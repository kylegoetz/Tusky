package com.keylesspalace.tusky.components.login

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import at.connyduck.calladapter.networkresult.NetworkResult
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.AppCredentials
import com.keylesspalace.tusky.network.ConnectionManager
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class LoginActivityTests {
    @Mock private lateinit var connectionManager: ConnectionManager
    @Mock private lateinit var mastodonApi: MastodonApi
    lateinit var rule: ActivityScenarioRule<LoginActivity>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(runBlocking { mastodonApi.authenticateApp(anyString(), anyString(), anyString(), anyString()) })
            .thenReturn(NetworkResult.success(AppCredentials("", "")))
        `when`(connectionManager.mastodonApi).thenReturn(mastodonApi)
    }

    @Test
    fun whenClickLoginStoresDomainInConnectionManager() {
        val domainName = "foo"
        launchActivity<LoginActivity>().use {
            it.onActivity { activity ->
                activity.connectionManager = connectionManager
            }
            onView(withId(R.id.domainEditText)).perform(typeText(domainName), closeSoftKeyboard())
            onView(withId(R.id.loginButton)).perform(click())
        }
        verify(connectionManager).apiDomain = domainName
    }

    @Test
    fun whenClickLoginThenCallsApiAuthenticate() {
        launchActivity<LoginActivity>().use {
            it.onActivity { a ->
                a.connectionManager = connectionManager
            }
            onView(withId(R.id.domainEditText)).perform(typeText("foo"), closeSoftKeyboard())
            onView(withId(R.id.loginButton)).perform(click())
            runBlocking {
                verify(mastodonApi).authenticateApp(anyString(), anyString(), anyString(), anyString())
            }
        }
    }

    companion object {
        private const val TAG = "LoginActivityTests"
    }
}
