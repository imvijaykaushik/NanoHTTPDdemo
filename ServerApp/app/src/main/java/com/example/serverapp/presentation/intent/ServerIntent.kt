package com.example.serverapp.presentation.intent

/** Everything the user can do on the server screen. */
sealed interface ServerIntent {
    data class PortChanged(val value: String) : ServerIntent
    data object StartServer : ServerIntent
    data object StopServer : ServerIntent
    data object DismissError : ServerIntent
}
