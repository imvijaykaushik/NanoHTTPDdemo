package com.example.serverapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigTest {

    @Test
    fun `accepts a valid user port`() {
        assertEquals(8080, ServerConfig.parse("8080").getOrThrow().port)
    }

    @Test
    fun `accepts the range boundaries`() {
        assertTrue(ServerConfig.parse("1024").isSuccess)
        assertTrue(ServerConfig.parse("65535").isSuccess)
    }

    @Test
    fun `rejects a privileged port that Android cannot bind`() {
        val error = ServerConfig.parse("80").exceptionOrNull()
        assertTrue(error is ServerError.PortOutOfRange)
    }

    @Test
    fun `rejects a port above the valid range`() {
        val error = ServerConfig.parse("70000").exceptionOrNull()
        assertTrue(error is ServerError.PortOutOfRange)
    }

    @Test
    fun `rejects non-numeric input`() {
        val error = ServerConfig.parse("abc").exceptionOrNull()
        assertTrue(error is ServerError.InvalidPort)
    }

    @Test
    fun `trims whitespace`() {
        assertEquals(9000, ServerConfig.parse("  9000 ").getOrThrow().port)
    }
}
