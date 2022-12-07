package com.keylesspalace.tusky.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import at.connyduck.calladapter.networkresult.NetworkResultCallAdapterFactory
import at.connyduck.calladapter.networkresult.map
import com.google.gson.Gson
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.settings.PrefKeys.DOMAIN
import com.keylesspalace.tusky.util.getNonNullString
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager @Inject constructor(
    private val accountManager: AccountManager,
    private val cacheDir: File,
    private val preferences: SharedPreferences,
    private val gson: Gson
) {

    private val accessTokenInterceptor = Interceptor { chain ->
        val token = accountManager.activeAccount?.accessToken
        if (chain.request().url.host == apiDomain && token != null) {
            chain.proceed(chain.request().newBuilder().header(
                "Authorization",
                "Bearer %s".format(token)
            ).build())
        } else chain.proceed(chain.request())
    }
    private val retrofit: Retrofit
        get() =
            Retrofit.Builder().baseUrl(apiBaseUrl)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addCallAdapterFactory(NetworkResultCallAdapterFactory.create())
                .build()

    var apiDomain = preferences.getNonNullString(DOMAIN, MastodonApi.PLACEHOLDER_DOMAIN)
        set(value) {
            preferences.edit().putString(DOMAIN, value).apply()
            field = value
        }

    private val apiBaseUrl get() = "https://${apiDomain}"

    val mastodonApi get() = retrofit.create<MastodonApi>()
    val api get() = mastodonApi

    val mediaUploaderApi: MediaUploadApi
        get() {
            val longTimeOutOkHttpClient = getClient().newBuilder()
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .build()

            return retrofit.newBuilder()
                .client(longTimeOutOkHttpClient)
                .build()
                .create()
        }

    fun getClient(): OkHttpClient {
        val httpProxyEnabled = preferences.getBoolean(PrefKeys.HTTP_PROXY_ENABLED, false)
        val httpServer = preferences.getNonNullString(PrefKeys.HTTP_PROXY_SERVER, "")
        val httpPort = preferences.getNonNullString(PrefKeys.HTTP_PROXY_PORT, "-1").toIntOrNull() ?: -1
        val cacheSize = 25 * 1024 * 1024L // 25 MiB
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                /**
                 * Add a custom User-Agent that contains Tusky, Android and OkHttp Version to all requests
                 * Example:
                 * User-Agent: Tusky/1.1.2 Android/5.0.2 OkHttp/4.9.0
                 * */
                val requestWithUserAgent = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Tusky/${BuildConfig.VERSION_NAME} Android/${Build.VERSION.RELEASE} OkHttp/${OkHttp.VERSION}"
                    )
                    .build()
                chain.proceed(requestWithUserAgent)
            }
            .addInterceptor(accessTokenInterceptor)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, cacheSize))

        if (httpProxyEnabled && httpServer.isNotEmpty() && httpPort > 0 && httpPort < 65535) {
            val address = InetSocketAddress.createUnresolved(httpServer, httpPort)
            builder.proxy(Proxy(Proxy.Type.HTTP, address))
        }
        return builder
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }
}
