package com.example.clientapp.domain.usecase

import com.example.clientapp.domain.model.NetworkError
import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerMessage
import com.example.clientapp.domain.model.ValidationError
import com.example.clientapp.domain.repository.ServerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    private val repository = mockk<ServerRepository>()
    private val useCase = SendMessageUseCase(repository)

    @Test
    fun `sends message successfully`() = runTest {
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.success(ServerMessage("Hello from Server!", "Hello Server"))

        val result = useCase("192.168.1.30", "8080", "Hello Server")

        assertEquals(
            ServerMessage("Hello from Server!", "Hello Server"),
            result.getOrThrow(),
        )
        coVerify(exactly = 1) {
            repository.sendMessage(ServerAddress("192.168.1.30", 8080), "Hello Server")
        }
    }

    @Test
    fun `trims the message before sending`() = runTest {
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.success(ServerMessage("Hello from Server!", "padded"))

        useCase("127.0.0.1", "8080", "  padded  ")

        coVerify { repository.sendMessage(any(), "padded") }
    }

    @Test
    fun `rejects a blank message without calling the repository`() = runTest {
        val result = useCase("127.0.0.1", "8080", "   ")

        assertEquals(SendMessageUseCase.EmptyMessageError, result.exceptionOrNull())
        coVerify(exactly = 0) { repository.sendMessage(any(), any()) }
    }

    @Test
    fun `rejects an invalid address without calling the repository`() = runTest {
        val result = useCase("not a host", "8080", "Hello")

        assertTrue(result.exceptionOrNull() is ValidationError.InvalidHost)
        coVerify(exactly = 0) { repository.sendMessage(any(), any()) }
    }

    @Test
    fun `propagates a repository failure`() = runTest {
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.failure(NetworkError.ConnectionRefused)

        val result = useCase("127.0.0.1", "8080", "Hello")

        assertEquals(NetworkError.ConnectionRefused, result.exceptionOrNull())
    }
}
