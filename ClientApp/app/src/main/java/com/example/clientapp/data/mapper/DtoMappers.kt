package com.example.clientapp.data.mapper

import com.example.clientapp.data.dto.HealthResponseDto
import com.example.clientapp.data.dto.MessageResponseDto
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.model.ServerMessage

/** DTO -> domain. Keeps wire types out of the UI. */
internal fun HealthResponseDto.toDomain() = ServerHealth(
    status = status,
    message = message,
)

internal fun MessageResponseDto.toDomain() = ServerMessage(
    message = message,
    received = received,
)
