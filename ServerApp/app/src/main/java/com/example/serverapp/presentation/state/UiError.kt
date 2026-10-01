package com.example.serverapp.presentation.state

import com.example.serverapp.domain.model.ServerError

/** Errors in a form the UI can render. */
sealed interface UiError {
    data class InvalidPort(val detail: String) : UiError
    data class PortInUse(val port: Int) : UiError
    data object AlreadyRunning : UiError
    data object NotRunning : UiError
    data class StartFailed(val detail: String) : UiError
    data class Unknown(val message: String) : UiError
}

/** Wording shown to the user. */
val UiError.displayMessage: String
    get() = when (this) {
        is UiError.InvalidPort -> detail
        is UiError.PortInUse ->
            "Port $port is already in use. Stop the other app using it, or pick a different port."
        UiError.AlreadyRunning -> "The server is already running."
        UiError.NotRunning -> "The server is not running."
        is UiError.StartFailed -> "Could not start the server: $detail"
        is UiError.Unknown -> message
    }

/** Domain failure -> presentation error. */
fun Throwable.toUiError(): UiError = when (this) {
    is ServerError.InvalidPort -> UiError.InvalidPort(message ?: "Invalid port")
    is ServerError.PortOutOfRange -> UiError.InvalidPort(message ?: "Invalid port")
    is ServerError.PortInUse -> UiError.PortInUse(port)
    ServerError.AlreadyRunning -> UiError.AlreadyRunning
    ServerError.NotRunning -> UiError.NotRunning
    is ServerError.StartFailed -> UiError.StartFailed(detail)
    else -> UiError.Unknown(message ?: "Something went wrong")
}
