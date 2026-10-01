package com.example.serverapp.data.http

import com.example.serverapp.core.NoOpLogger
import com.example.serverapp.data.repository.ServerStatusHolder
import com.example.serverapp.domain.usecase.HandleClientMessageUseCase
import com.example.serverapp.domain.usecase.HandleHealthCheckUseCase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Routing is tested without a socket: [RouteHandler] takes an [HttpRequest] and
 * returns an [HttpReply], so the JSON contract can be asserted directly.
 */
class RouteHandlerTest {

    private lateinit var statusHolder: ServerStatusHolder
    private lateinit var handler: RouteHandler

    @Before
    fun setUp() {
        statusHolder = ServerStatusHolder()
        handler = RouteHandler(
            handleHealthCheck = HandleHealthCheckUseCase(),
            handleClientMessage = HandleClientMessageUseCase(statusHolder),
            messageRepository = statusHolder,
            json = Json { ignoreUnknownKeys = true },
            logger = NoOpLogger(),
        )
    }

    @Test
    fun `GET health returns the documented JSON`() {
        val reply = handler.handle(HttpRequest("GET", "/health", ""))

        assertEquals(200, reply.statusCode)
        assertEquals("""{"status":"OK","message":"Server is running"}""", reply.body)
        assertEquals("application/json", reply.contentType)
    }

    @Test
    fun `POST message returns the documented JSON`() {
        val reply = handler.handle(
            HttpRequest("POST", "/message", """{"message":"Hello Server"}"""),
        )

        assertEquals(200, reply.statusCode)
        assertEquals(
            """{"message":"Hello from Server!","received":"Hello Server"}""",
            reply.body,
        )
    }

    @Test
    fun `POST message records the message for the UI`() {
        handler.handle(HttpRequest("POST", "/message", """{"message":"Hello Server"}"""))

        assertEquals("Hello Server", statusHolder.status.value.lastMessage)
    }

    @Test
    fun `every handled request increments the counter`() {
        handler.handle(HttpRequest("GET", "/health", ""))
        handler.handle(HttpRequest("GET", "/health", ""))
        handler.handle(HttpRequest("GET", "/nope", ""))

        assertEquals(3, statusHolder.status.value.requestCount)
    }

    @Test
    fun `a malformed body is a 400, not a crash`() {
        val reply = handler.handle(HttpRequest("POST", "/message", "not json at all"))

        assertEquals(400, reply.statusCode)
        assertTrue(reply.body.contains("error"))
    }

    @Test
    fun `a body missing the message field is a 400`() {
        val reply = handler.handle(HttpRequest("POST", "/message", """{"wrong":"field"}"""))

        assertEquals(400, reply.statusCode)
    }

    @Test
    fun `a blank message is a 400`() {
        val reply = handler.handle(HttpRequest("POST", "/message", """{"message":"   "}"""))

        assertEquals(400, reply.statusCode)
    }

    @Test
    fun `an unknown path is a 404`() {
        val reply = handler.handle(HttpRequest("GET", "/unknown", ""))

        assertEquals(404, reply.statusCode)
    }

    @Test
    fun `a known path with the wrong method is a 405`() {
        assertEquals(405, handler.handle(HttpRequest("POST", "/health", "")).statusCode)
        assertEquals(405, handler.handle(HttpRequest("GET", "/message", "")).statusCode)
    }

    @Test
    fun `a trailing slash still routes`() {
        assertEquals(200, handler.handle(HttpRequest("GET", "/health/", "")).statusCode)
    }

    @Test
    fun `the last request is recorded with its status code`() {
        handler.handle(HttpRequest("GET", "/unknown", ""))

        val last = statusHolder.status.value.lastRequest
        assertEquals("GET", last?.method)
        assertEquals("/unknown", last?.path)
        assertEquals(404, last?.statusCode)
    }
}
