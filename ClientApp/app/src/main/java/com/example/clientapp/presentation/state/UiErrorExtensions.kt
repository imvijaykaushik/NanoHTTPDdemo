package com.example.clientapp.presentation.state

/**
 * True when the error came from the network rather than from input validation.
 * Used to decide whether a failure should clear the "connected" indicator.
 */
fun UiError.isTransport(): Boolean = when (this) {
    UiError.ServerUnavailable,
    UiError.ConnectionRefused,
    UiError.Timeout,
    UiError.NoNetwork,
    -> true
    UiError.MalformedResponse,
    is UiError.HttpError,
    is UiError.Validation,
    is UiError.Unknown,
    -> false
}
