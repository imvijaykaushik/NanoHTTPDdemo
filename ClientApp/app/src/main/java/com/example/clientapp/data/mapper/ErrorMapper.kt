package com.example.clientapp.data.mapper

import com.example.clientapp.domain.model.NetworkError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Translates the exceptions Retrofit/OkHttp throw into [NetworkError].
 *
 * Centralised here so both repository methods report failures identically, and
 * so the mapping can be unit-tested without touching the network.
 */
internal fun Throwable.toNetworkError(): NetworkError = when (this) {
    // A typed 2xx response that failed to deserialize.
    is SerializationException -> NetworkError.MalformedResponse

    // Non-2xx status.
    is HttpException -> NetworkError.Http(code())

    // Timed out waiting for the peer.
    is SocketTimeoutException -> NetworkError.Timeout

    // Nothing accepting connections on that port (server app not started).
    is ConnectException -> NetworkError.ConnectionRefused

    // Host could not be resolved.
    is UnknownHostException -> NetworkError.ServerUnavailable

    // Host is not routable, e.g. wrong subnet.
    is NoRouteToHostException -> NetworkError.ServerUnavailable

    // Remaining I/O problems: inspect the message for the common cases OkHttp
    // reports as plain IOException.
    is IOException -> fromIoMessage(message)

    else -> NetworkError.Unknown(message ?: this::class.java.simpleName)
}

private fun fromIoMessage(message: String?): NetworkError {
    val text = message?.lowercase().orEmpty()
    return when {
        "timeout" in text || "timed out" in text -> NetworkError.Timeout
        "econnrefused" in text || "connection refused" in text -> NetworkError.ConnectionRefused
        "unable to resolve host" in text -> NetworkError.ServerUnavailable
        "network is unreachable" in text || "enetunreach" in text -> NetworkError.NoNetwork
        "connection reset" in text -> NetworkError.ServerUnavailable
        // OkHttp reuses pooled keep-alive connections. If the server stopped
        // since the last call, the reused socket dies mid-request and surfaces
        // as this message rather than as a ConnectException. Treat it as the
        // server having gone away, which is what actually happened.
        "unexpected end of stream" in text -> NetworkError.ServerUnavailable
        "socket closed" in text -> NetworkError.ServerUnavailable
        "software caused connection abort" in text -> NetworkError.ServerUnavailable
        "failed to connect" in text -> NetworkError.ConnectionRefused
        else -> NetworkError.Unknown(message ?: "Network error")
    }
}
