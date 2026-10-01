package com.example.clientapp.domain.model

/**
 * A validated host + port pair.
 *
 * Instances can only be produced through [ServerAddress.parse], so anything
 * downstream of validation can trust the values. This is what stops an invalid
 * IP or port from reaching the network layer.
 */
data class ServerAddress(
    val host: String,
    val port: Int,
) {
    val baseUrl: String get() = "http://$host:$port/"

    companion object {
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65535

        /**
         * Validates raw UI text. Returns a [ValidationError] rather than throwing,
         * so the ViewModel can surface the problem through state.
         */
        fun parse(host: String, port: String): Result<ServerAddress> {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) {
                return Result.failure(ValidationError.EmptyHost)
            }
            if (!isPlausibleHost(trimmedHost)) {
                return Result.failure(ValidationError.InvalidHost(trimmedHost))
            }
            val parsedPort = port.trim().toIntOrNull()
                ?: return Result.failure(ValidationError.InvalidPort(port.trim()))
            if (parsedPort !in MIN_PORT..MAX_PORT) {
                return Result.failure(ValidationError.InvalidPort(port.trim()))
            }
            return Result.success(ServerAddress(trimmedHost, parsedPort))
        }

        /**
         * Accepts either a dotted IPv4 address or a hostname such as "localhost".
         * Deliberately simple: the real check is whether the connection succeeds.
         */
        private fun isPlausibleHost(host: String): Boolean {
            if (host.any { it.isWhitespace() }) return false
            val looksNumeric = host.all { it.isDigit() || it == '.' }
            return if (looksNumeric) isValidIpv4(host) else isValidHostname(host)
        }

        private fun isValidIpv4(host: String): Boolean {
            val parts = host.split('.')
            if (parts.size != 4) return false
            return parts.all { part ->
                part.isNotEmpty() &&
                    part.length <= 3 &&
                    part.all(Char::isDigit) &&
                    (part.toIntOrNull() ?: -1) in 0..255
            }
        }

        private fun isValidHostname(host: String): Boolean =
            host.length <= 253 &&
                host.split('.').all { label ->
                    label.isNotEmpty() &&
                        label.length <= 63 &&
                        label.all { it.isLetterOrDigit() || it == '-' } &&
                        !label.startsWith('-') &&
                        !label.endsWith('-')
                }
    }
}

/** Input problems detected before any network call is made. */
sealed class ValidationError(message: String) : Exception(message) {
    data object EmptyHost : ValidationError("Server IP address is required")
    data class InvalidHost(val value: String) : ValidationError("\"$value\" is not a valid IP address or hostname")
    data class InvalidPort(val value: String) : ValidationError("\"$value\" is not a valid port (expected 1-65535)")
}
