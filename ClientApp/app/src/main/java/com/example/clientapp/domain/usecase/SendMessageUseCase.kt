package com.example.clientapp.domain.usecase

import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerMessage
import com.example.clientapp.domain.repository.ServerRepository
import javax.inject.Inject

/**
 * Validates the address and the message, then POSTs to /message.
 */
class SendMessageUseCase @Inject constructor(
    private val repository: ServerRepository,
) {
    suspend operator fun invoke(host: String, port: String, message: String): Result<ServerMessage> {
        if (message.isBlank()) {
            return Result.failure(EmptyMessageError)
        }
        return ServerAddress.parse(host, port).fold(
            onSuccess = { repository.sendMessage(it, message.trim()) },
            onFailure = { Result.failure(it) },
        )
    }

    data object EmptyMessageError : Exception("Message cannot be empty")
}
