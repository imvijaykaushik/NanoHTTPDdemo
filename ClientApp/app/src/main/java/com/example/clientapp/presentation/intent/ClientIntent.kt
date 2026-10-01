package com.example.clientapp.presentation.intent

/**
 * Every way the user can affect the screen.
 *
 * The UI sends intents and nothing else - it never calls a use case or mutates
 * state directly. Typing counts as an intent too, which is what keeps the
 * text fields driven by a single source of truth.
 */
sealed interface ClientIntent {
    data class ServerAddressChanged(val value: String) : ClientIntent
    data class PortChanged(val value: String) : ClientIntent
    data class MessageChanged(val value: String) : ClientIntent
    data object Connect : ClientIntent
    data class SendMessage(val message: String) : ClientIntent
    data object DismissError : ClientIntent
}
