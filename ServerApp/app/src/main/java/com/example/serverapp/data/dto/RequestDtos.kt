package com.example.serverapp.data.dto

import kotlinx.serialization.Serializable

/** Expected request body of `POST /message`. */
@Serializable
data class MessageRequestDto(
    val message: String,
)
