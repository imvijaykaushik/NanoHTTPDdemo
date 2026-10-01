package com.example.clientapp.domain.usecase

import com.example.clientapp.domain.model.NetworkError
import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.model.ValidationError
import com.example.clientapp.domain.repository.ServerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckServerHealthUseCaseTest {

    private val repository = mockk<ServerRepository>()
    private val useCase = CheckServerHealthUseCase(repository)

    @Test
    fun `returns health when the server responds`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))

        val result = useCase("192.168.1.30", "8080")

        assertEquals(ServerHealth("OK", "Server is running"), result.getOrThrow())
        coVerify { repository.healthCheck(ServerAddress("192.168.1.30", 8080)) }
    }

    @Test
    fun `reports connection refused when the server is stopped`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.failure(NetworkError.ConnectionRefused)

        val result = useCase("127.0.0.1", "8080")

        assertEquals(NetworkError.ConnectionRefused, result.exceptionOrNull())
    }

    @Test
    fun `does not hit the network for an invalid port`() = runTest {
        val result = useCase("127.0.0.1", "0")

        assertTrue(result.exceptionOrNull() is ValidationError.InvalidPort)
        coVerify(exactly = 0) { repository.healthCheck(any()) }
    }
}
