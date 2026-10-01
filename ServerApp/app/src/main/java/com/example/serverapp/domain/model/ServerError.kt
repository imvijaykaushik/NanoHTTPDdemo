package com.example.serverapp.domain.model

/** Failures the server can report, in domain terms. */
sealed class ServerError(message: String) : Exception(message) {

    data class InvalidPort(val value: String) :
        ServerError("\"$value\" is not a valid port number")

    data class PortOutOfRange(val value: Int) :
        ServerError("Port $value is out of range. Use 1024-65535.")

    /** Another process (or a previous run) already holds the port. */
    data class PortInUse(val port: Int) :
        ServerError("Port $port is already in use")

    data object AlreadyRunning : ServerError("Server is already running")

    data object NotRunning : ServerError("Server is not running")

    data class StartFailed(val detail: String) :
        ServerError("Could not start server: $detail")
}
