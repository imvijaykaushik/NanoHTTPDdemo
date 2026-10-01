package com.example.clientapp.domain.model

/**
 * Transport failures, expressed in domain terms.
 *
 * The data layer translates Retrofit/OkHttp/IO exceptions into these so that
 * nothing above the repository has to know about `SocketTimeoutException` or
 * `HttpException`.
 */
sealed class NetworkError(message: String) : Exception(message) {

    /** Nothing is listening at that host:port - typically the server app is stopped. */
    data object ConnectionRefused : NetworkError("Connection refused")

    /** The host did not answer in time. */
    data object Timeout : NetworkError("Connection timed out")

    /** The host name or address could not be resolved / routed. */
    data object ServerUnavailable : NetworkError("Server unreachable")

    /** The device has no usable network. */
    data object NoNetwork : NetworkError("No network available")

    /** A response arrived, but with a non-2xx status. */
    data class Http(val code: Int) : NetworkError("HTTP error $code")

    /** A 2xx response arrived but the body could not be parsed. */
    data object MalformedResponse : NetworkError("Malformed response from server")

    /** Anything not covered above. */
    data class Unknown(val detail: String) : NetworkError(detail)
}
