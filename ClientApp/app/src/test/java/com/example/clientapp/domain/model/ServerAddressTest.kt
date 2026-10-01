package com.example.clientapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validation happens before any socket is opened, so it is worth pinning down. */
class ServerAddressTest {

    @Test
    fun `accepts a valid IPv4 address and port`() {
        val result = ServerAddress.parse("192.168.1.30", "8080")

        val address = result.getOrThrow()
        assertEquals("192.168.1.30", address.host)
        assertEquals(8080, address.port)
        assertEquals("http://192.168.1.30:8080/", address.baseUrl)
    }

    @Test
    fun `accepts loopback for same-device use`() {
        assertTrue(ServerAddress.parse("127.0.0.1", "8080").isSuccess)
    }

    @Test
    fun `accepts a hostname`() {
        assertTrue(ServerAddress.parse("localhost", "8080").isSuccess)
    }

    @Test
    fun `rejects an empty host`() {
        val error = ServerAddress.parse("  ", "8080").exceptionOrNull()
        assertTrue(error is ValidationError.EmptyHost)
    }

    @Test
    fun `rejects an octet above 255`() {
        val error = ServerAddress.parse("192.168.1.999", "8080").exceptionOrNull()
        assertTrue(error is ValidationError.InvalidHost)
    }

    @Test
    fun `rejects an incomplete IPv4 address`() {
        val error = ServerAddress.parse("192.168.1", "8080").exceptionOrNull()
        assertTrue(error is ValidationError.InvalidHost)
    }

    @Test
    fun `rejects a non-numeric port`() {
        val error = ServerAddress.parse("127.0.0.1", "abc").exceptionOrNull()
        assertTrue(error is ValidationError.InvalidPort)
    }

    @Test
    fun `rejects a port above the valid range`() {
        val error = ServerAddress.parse("127.0.0.1", "70000").exceptionOrNull()
        assertTrue(error is ValidationError.InvalidPort)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val address = ServerAddress.parse("  10.0.0.5  ", " 9000 ").getOrThrow()
        assertEquals("10.0.0.5", address.host)
        assertEquals(9000, address.port)
    }
}
