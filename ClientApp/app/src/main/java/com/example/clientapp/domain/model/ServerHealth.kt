package com.example.clientapp.domain.model

/** The server's answer to a health check. */
data class ServerHealth(
    val status: String,
    val message: String,
)
