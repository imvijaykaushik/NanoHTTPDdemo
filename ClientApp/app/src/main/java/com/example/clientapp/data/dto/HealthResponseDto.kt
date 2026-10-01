package com.example.clientapp.data.dto

import kotlinx.serialization.Serializable

/** Wire shape of `GET /health`. Never leaves the data layer. */
@Serializable
data class HealthResponseDto(
    val status: String,
    val message: String,
)
