package com.example.serverapp.data.dto

import kotlinx.serialization.Serializable

/** Body of `GET /health`. */
@Serializable
data class HealthResponseDto(
    val status: String,
    val message: String,
)

/** Body of `POST /message`. */
@Serializable
data class MessageResponseDto(
    val message: String,
    val received: String,
)

/** Uniform error body, so clients can always expect JSON. */
@Serializable
data class ErrorResponseDto(
    val error: String,
)
