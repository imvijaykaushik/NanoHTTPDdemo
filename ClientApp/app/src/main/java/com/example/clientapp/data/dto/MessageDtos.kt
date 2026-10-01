package com.example.clientapp.data.dto

import kotlinx.serialization.Serializable

/** Request body for `POST /message`. */
@Serializable
data class MessageRequestDto(
    val message: String,
)

/** Response body for `POST /message`. */
@Serializable
data class MessageResponseDto(
    val message: String,
    val received: String,
)
