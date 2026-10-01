package com.example.clientapp.presentation.state

/**
 * Errors in a form the UI can render.
 *
 * Separate from the domain's NetworkError so that presentation owns the
 * human-readable wording, and so validation problems and transport problems
 * can be shown through one channel.
 */
sealed interface UiError {
    data object ServerUnavailable : UiError
    data object ConnectionRefused : UiError
    data object Timeout : UiError
    data object NoNetwork : UiError
    data object MalformedResponse : UiError
    data class HttpError(val code: Int) : UiError
    data class Validation(val message: String) : UiError
    data class Unknown(val message: String) : UiError
}

/** Wording shown to the user. Kept here so it is easy to review in one place. */
val UiError.displayMessage: String
    get() = when (this) {
        UiError.ServerUnavailable ->
            "Server unreachable. Check the IP address and that both devices are on the same Wi-Fi."
        UiError.ConnectionRefused ->
            "Connection refused. Is the Server app running and started on that port?"
        UiError.Timeout ->
            "Connection timed out. The server did not respond in time."
        UiError.NoNetwork ->
            "No network available. Turn on Wi-Fi and try again."
        UiError.MalformedResponse ->
            "The server sent a response the app could not understand."
        is UiError.HttpError -> when (code) {
            404 -> "Endpoint not found (404). Is the Server app up to date?"
            405 -> "Method not allowed (405)."
            in 500..599 -> "The server reported an internal error ($code)."
            else -> "Request failed with HTTP $code."
        }
        is UiError.Validation -> message
        is UiError.Unknown -> message
    }
