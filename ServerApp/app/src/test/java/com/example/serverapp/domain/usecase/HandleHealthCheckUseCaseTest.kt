package com.example.serverapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class HandleHealthCheckUseCaseTest {

    @Test
    fun `reports the documented health payload`() {
        val status = HandleHealthCheckUseCase()()

        assertEquals("OK", status.status)
        assertEquals("Server is running", status.message)
    }
}
