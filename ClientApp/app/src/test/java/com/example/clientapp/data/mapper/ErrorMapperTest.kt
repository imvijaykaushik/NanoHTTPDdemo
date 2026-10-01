package com.example.clientapp.data.mapper

import com.example.clientapp.domain.model.NetworkError
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The mapper is the only place transport exceptions become user-facing errors,
 * so each branch is pinned down here.
 */
class ErrorMapperTest {

    @Test
    fun `maps a connect failure to ConnectionRefused`() {
        assertEquals(
            NetworkError.ConnectionRefused,
            ConnectException("ECONNREFUSED").toNetworkError(),
        )
    }

    @Test
    fun `maps a socket timeout to Timeout`() {
        assertEquals(NetworkError.Timeout, SocketTimeoutException().toNetworkError())
    }

    @Test
    fun `maps an unresolvable host to ServerUnavailable`() {
        assertEquals(
            NetworkError.ServerUnavailable,
            UnknownHostException("nope").toNetworkError(),
        )
    }

    @Test
    fun `maps an unroutable host to ServerUnavailable`() {
        assertEquals(
            NetworkError.ServerUnavailable,
            NoRouteToHostException().toNetworkError(),
        )
    }

    @Test
    fun `maps a non-2xx response to Http with its code`() {
        val exception = HttpException(
            Response.error<Any>(503, "".toResponseBody("application/json".toMediaType())),
        )

        assertEquals(NetworkError.Http(503), exception.toNetworkError())
    }

    @Test
    fun `maps a deserialization failure to MalformedResponse`() {
        assertEquals(
            NetworkError.MalformedResponse,
            SerializationException("bad json").toNetworkError(),
        )
    }

    @Test
    fun `maps an unreachable network to NoNetwork`() {
        assertEquals(
            NetworkError.NoNetwork,
            IOException("Network is unreachable").toNetworkError(),
        )
    }

    /**
     * Regression test: OkHttp reuses pooled keep-alive connections, so calling a
     * server that has just stopped yields this message rather than a
     * ConnectException. It must not leak to the UI as a raw Unknown error.
     */
    @Test
    fun `maps a dead pooled connection to ServerUnavailable`() {
        assertEquals(
            NetworkError.ServerUnavailable,
            IOException("unexpected end of stream on http://127.0.0.1:8080/...")
                .toNetworkError(),
        )
    }

    @Test
    fun `maps a closed socket to ServerUnavailable`() {
        assertEquals(
            NetworkError.ServerUnavailable,
            IOException("Socket closed").toNetworkError(),
        )
    }

    @Test
    fun `maps an Android connect failure message to ConnectionRefused`() {
        assertEquals(
            NetworkError.ConnectionRefused,
            IOException("Failed to connect to /192.168.1.30:8080").toNetworkError(),
        )
    }

    @Test
    fun `falls back to Unknown for anything unrecognised`() {
        val error = IllegalStateException("something odd").toNetworkError()

        assertEquals(NetworkError.Unknown("something odd"), error)
    }
}
