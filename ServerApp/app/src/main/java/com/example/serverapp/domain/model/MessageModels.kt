package com.example.serverapp.domain.model

/** The server's reply to a client message. */
data class MessageReply(
    val message: String,
    val received: String,
)

/** The server's own health description. */
data class HealthStatus(
    val status: String,
    val message: String,
)
