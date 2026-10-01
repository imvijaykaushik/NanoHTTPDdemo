package com.example.serverapp.presentation.state

import com.example.serverapp.domain.model.RequestLog

/** The complete, immutable description of the server screen. */
data class ServerUiState(
    val port: String = "8080",
    val isRunning: Boolean = false,
    val isLoading: Boolean = false,
    val requestCount: Int = 0,
    val lastMessage: String? = null,
    val lastRequest: RequestLog? = null,
    val localIpAddress: String? = null,
    val error: UiError? = null,
) {
    val canStart: Boolean get() = !isLoading && !isRunning
    val canStop: Boolean get() = !isLoading && isRunning

    /** What the client on another device should enter, e.g. "192.168.1.30:8080". */
    val clientHint: String?
        get() = localIpAddress?.let { "$it:$port" }
}
