package com.example.clientapp.presentation.state

/**
 * The complete, immutable description of the client screen.
 *
 * Compose renders this and nothing else. Because it is a data class, each
 * update produces a new instance, which makes state transitions easy to assert
 * in tests.
 */
data class ClientUiState(
    // 127.0.0.1 is the useful default: it works immediately when both APKs are
    // installed on the same device.
    val serverAddress: String = "127.0.0.1",
    val port: String = "8080",
    val message: String = "",
    val response: String = "",
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiError? = null,
) {
    /** Guards the Connect button: no duplicate in-flight requests. */
    val canConnect: Boolean get() = !isLoading

    /** Sending requires a successful health check and a non-blank message. */
    val canSendMessage: Boolean get() = !isLoading && isConnected && message.isNotBlank()
}
