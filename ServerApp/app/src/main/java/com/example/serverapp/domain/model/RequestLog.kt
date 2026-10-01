package com.example.serverapp.domain.model

/** One handled request, for display in the UI. */
data class RequestLog(
    val method: String,
    val path: String,
    val statusCode: Int,
)

/** Observable snapshot of the running server. */
data class ServerStatus(
    val isRunning: Boolean = false,
    val port: Int? = null,
    val requestCount: Int = 0,
    val lastMessage: String? = null,
    val lastRequest: RequestLog? = null,
    /**
     * Why the last start attempt failed, or null if none did.
     *
     * Needed because the server is bound inside a foreground service: the
     * request to start returns immediately, so a bind failure (busy port) can
     * only reach the UI through this flow.
     */
    val startError: ServerError? = null,
)
