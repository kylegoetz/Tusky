package com.keylesspalace.tusky.network

import android.content.SharedPreferences
import com.google.gson.Gson
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.settings.PrefKeys.DOMAIN
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

class ConnectionManagerTests {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var accountManager: AccountManager
    private lateinit var gson: Gson

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setup() {
        accountManager = mock()
        gson = mock()
    }

    @Test
    fun `setting api base url stores in preferences in domain key`() {
        val domain = "testdomain"
        val mockEditor: SharedPreferences.Editor = mock {
            on { putString(anyString(), anyString()) } doReturn this.mock
        }
        sharedPreferences = mock {
            on { edit() } doReturn mockEditor
        }
        connectionManager = ConnectionManagerImpl(accountManager, File(""), sharedPreferences, gson)
        connectionManager.apiDomain = domain
        verify(mockEditor).putString(DOMAIN, domain)
        verify(mockEditor).apply()
    }

    @Test
    fun `if proxy enabled, use proxy settings`() {
        val domain = "testdomain"
        val port = 5
        sharedPreferences = mock {
            on { getBoolean(eq(PrefKeys.HTTP_PROXY_ENABLED), anyBoolean()) } doReturn true
            on { getString(eq(PrefKeys.HTTP_PROXY_SERVER), anyString()) } doReturn domain
            on { getString(eq(PrefKeys.HTTP_PROXY_PORT), anyString()) } doReturn "$port"
        }
        connectionManager = ConnectionManagerImpl(accountManager, File(""), sharedPreferences, gson)
        assertEquals("$domain:$port", connectionManager.getClient().proxy?.address().toString())
    }
}
