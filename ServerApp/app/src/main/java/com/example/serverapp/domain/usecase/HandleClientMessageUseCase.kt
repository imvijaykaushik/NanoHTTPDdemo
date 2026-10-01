package com.example.serverapp.domain.usecase

import com.example.serverapp.domain.model.MessageReply
import com.example.serverapp.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Business logic for `POST /message`: record what arrived, then build the reply.
 */
class HandleClientMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    operator fun invoke(incoming: String): Result<MessageReply> {
        if (incoming.isBlank()) {
            return Result.failure(BlankMessage)
        }
        messageRepository.recordMessage(incoming)
        return Result.success(
            MessageReply(
                message = "Hello from Server!",
                received = incoming,
            ),
        )
    }

    data object BlankMessage : Exception("Field \"message\" must not be empty")
}
