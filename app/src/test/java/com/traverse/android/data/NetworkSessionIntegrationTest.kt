package com.traverse.android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NetworkSessionIntegrationTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var service: NetworkService
    private lateinit var tokenStore: TestAuthTokenStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer().also { it.start() }
        tokenStore = TestAuthTokenStore()
        service = NetworkService(context, server.url("/api/").toString(), tokenStore)
        check(tokenStore.saveToken("integration-test-token"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun billingAndLogoutRequestsUseAuthenticatedApiAndClearToken() = runBlocking {
        assertEquals("integration-test-token", tokenStore.getToken())

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"isSubscriptionActive":true,"activeUntil":"2026-10-01T00:00:00.000Z","planName":"Traverse Pro","canCancel":true,"cancellationScheduled":false}"""
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"cancelAt":"2026-10-01T00:00:00.000Z"}"""
            )
        )
        server.enqueue(MockResponse().setResponseCode(204))

        val billing = service.getBillingStatus()
        assertTrue(billing is NetworkResult.Success)
        assertEquals("Traverse Pro", (billing as NetworkResult.Success).data.planName)
        val billingRequest = server.takeRequest()
        assertEquals("GET", billingRequest.method)
        assertEquals("/api/subscription/status", billingRequest.path)
        assertEquals("Bearer integration-test-token", billingRequest.getHeader("Authorization"))

        val cancellation = service.cancelSubscription()
        assertTrue(cancellation is NetworkResult.Success)
        val cancellationRequest = server.takeRequest()
        assertEquals("POST", cancellationRequest.method)
        assertEquals("/api/subscription/cancel", cancellationRequest.path)
        assertEquals("Bearer integration-test-token", cancellationRequest.getHeader("Authorization"))

        assertTrue(service.logout() is NetworkResult.Success)
        val logoutRequest = server.takeRequest()
        assertEquals("POST", logoutRequest.method)
        assertEquals("/api/auth/logout", logoutRequest.path)
        assertEquals("Bearer integration-test-token", logoutRequest.getHeader("Authorization"))
        assertEquals(null, tokenStore.getToken())
    }

    private class TestAuthTokenStore : AuthTokenStore {
        private var token: String? = null

        override fun saveToken(token: String): Boolean {
            this.token = token
            return true
        }

        override fun getToken(): String? = token

        override fun deleteToken() {
            token = null
        }

        override fun isAuthenticated(): Boolean = !token.isNullOrBlank()
    }
}
