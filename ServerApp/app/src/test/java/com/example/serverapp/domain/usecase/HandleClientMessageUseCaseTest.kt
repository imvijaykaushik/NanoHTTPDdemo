package com.example.serverapp.domain.usecase

import com.example.serverapp.domain.model.RequestLog
import com.example.serverapp.domain.repository.MessageRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class HandleClientMessageUseCaseTest {

    private val recorded = mutableListOf<String>()
    private val messageRepository = object : MessageRepository {
        override fun recordMessage(message: String) { recorded += message }
        override fun recordRequest(log: RequestLog) = Unit
    }
    private val useCase = HandleClientMessageUseCase(messageRepository)

    @Test
    fun `builds the expected reply and echoes the received text`() {
        val reply = useCase("Hello Server").getOrThrow()

        assertEquals("Hello from Server!", reply.message)
        assertEquals("Hello Server", reply.received)
    }

    @Test
    fun `records the received message for the UI`() {
        useCase("Hello Server")

        assertEquals(listOf("Hello Server"), recorded)
    }

    @Test
    fun `rejects a blank message without recording it`() {
        val error = useCase("   ").exceptionOrNull()

        assertEquals(HandleClientMessageUseCase.BlankMessage, error)
        assertEquals(emptyList<String>(), recorded)
    }

    @Test
    fun `does not touch the repository when the message is blank`() {
        val strict = mockk<MessageRepository>()
        val useCaseWithMock = HandleClientMessageUseCase(strict)

        useCaseWithMock("")

        verify(exactly = 0) { strict.recordMessage(any()) }
    }
}
