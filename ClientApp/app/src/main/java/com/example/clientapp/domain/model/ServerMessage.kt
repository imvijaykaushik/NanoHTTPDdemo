package com.example.clientapp.domain.model

/**
 * The server's reply to a posted message.
 *
 * Domain models are plain Kotlin: no Retrofit, no Android, no serialization
 * annotations. The data layer maps DTOs onto these types, which keeps the
 * presentation layer independent of the wire format.
 */
data class ServerMessage(
    val message: String,
    val received: String,
)
