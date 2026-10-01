package com.example.serverapp.domain.model

/**
 * A validated port to listen on.
 *
 * Only constructible via [parse], so the HTTP layer can trust the value.
 */
data class ServerConfig(
    val port: Int,
) {
    companion object {
        // Ports below 1024 are privileged and cannot be bound by a normal
        // Android app, so they are rejected up front with a clear message.
        private const val MIN_USER_PORT = 1024
        private const val MAX_PORT = 65535

        fun parse(port: String): Result<ServerConfig> {
            val parsed = port.trim().toIntOrNull()
                ?: return Result.failure(ServerError.InvalidPort(port.trim()))
            if (parsed !in MIN_USER_PORT..MAX_PORT) {
                return Result.failure(ServerError.PortOutOfRange(parsed))
            }
            return Result.success(ServerConfig(parsed))
        }
    }
}
