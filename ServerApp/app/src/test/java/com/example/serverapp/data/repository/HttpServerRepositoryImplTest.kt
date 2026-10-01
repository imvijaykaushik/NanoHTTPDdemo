package com.example.serverapp.data.repository

import com.example.serverapp.core.NoOpLogger
import com.example.serverapp.data.http.RouteHandler
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.model.ServerError
import com.example.serverapp.domain.usecase.HandleClientMessageUseCase
import com.example.serverapp.domain.usecase.HandleHealthCheckUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Starts the real embedded server on a real port and drives it with real HTTP
 * requests. This is the test that proves the server actually listens, rather
 * than only that the routing logic is correct.
 */
class HttpServerRepositoryImplTest {

    private lateinit var statusHolder: ServerStatusHolder
    private lateinit var repository: HttpServerRepositoryImpl
    private var port: Int = 0

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        statusHolder = ServerStatusHolder()
        repository = HttpServerRepositoryImpl(
            routeHandler = RouteHandler(
                handleHealthCheck = HandleHealthCheckUseCase(),
                handleClientMessage = HandleClientMessageUseCase(statusHolder),
                messageRepository = statusHolder,
                json = Json { ignoreUnknownKeys = true },
                logger = NoOpLogger(),
            ),
            statusHolder = statusHolder,
            // Real dispatcher: the server does genuine blocking socket work.
            ioDispatcher = Dispatchers.IO,
            logger = NoOpLogger(),
        )
        port = freePort()
    }

    @After
    fun tearDown() {
        runBlocking { repository.stop() }
    }

    /**
     * Asks the OS for an unused port. The socket is closed before the embedded
     * server binds it, so the port can briefly linger in TIME_WAIT; [startServer]
     * retries with a fresh port if that happens.
     */
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    /**
     * Starts the server, retrying on a different port if the chosen one turns
     * out to be taken. Without this the suite is flaky, because a port that was
     * free a moment ago can still be in TIME_WAIT from an earlier test.
     */
    private suspend fun startServer() {
        repeat(RETRIES) {
            if (repository.start(ServerConfig(port)).isSuccess) return
            port = freePort()
        }
        repository.start(ServerConfig(port)).getOrThrow()
    }

    private fun get(path: String): Pair<Int, String> {
        val request = Request.Builder().url("http://127.0.0.1:$port$path").get().build()
        httpClient.newCall(request).execute().use { response ->
            return response.code to response.body!!.string()
        }
    }

    private fun post(path: String, body: String): Pair<Int, String> {
        val request = Request.Builder()
            .url("http://127.0.0.1:$port$path")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            return response.code to response.body!!.string()
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val RETRIES = 5
    }

    @Test
    fun `starts and serves GET health over real HTTP`() = runTest {
        startServer()

        val (code, body) = get("/health")

        assertEquals(200, code)
        assertEquals("""{"status":"OK","message":"Server is running"}""", body)
    }

    @Test
    fun `serves POST message over real HTTP`() = runTest {
        startServer()

        val (code, body) = post("/message", """{"message":"Hello Server"}""")

        assertEquals(200, code)
        assertEquals(
            """{"message":"Hello from Server!","received":"Hello Server"}""",
            body,
        )
    }

    @Test
    fun `publishes running status with the bound port`() = runTest {
        startServer()

        val status = repository.status.value
        assertTrue(status.isRunning)
        assertEquals(port, status.port)
    }

    @Test
    fun `counts served requests in the status flow`() = runTest {
        startServer()

        get("/health")
        post("/message", """{"message":"Hello"}""")

        assertEquals(2, repository.status.value.requestCount)
        assertEquals("Hello", repository.status.value.lastMessage)
    }

    @Test
    fun `returns 404 for an unknown path`() = runTest {
        startServer()

        assertEquals(404, get("/nope").first)
    }

    @Test
    fun `returns 400 for a malformed body`() = runTest {
        startServer()

        assertEquals(400, post("/message", "not json").first)
    }

    @Test
    fun `rejects a second start while already running`() = runTest {
        startServer()

        val error = repository.start(ServerConfig(port)).exceptionOrNull()

        assertEquals(ServerError.AlreadyRunning, error)
    }

    @Test
    fun `reports the port as in use when another socket holds it`() = runTest {
        ServerSocket(port).use {
            val error = repository.start(ServerConfig(port)).exceptionOrNull()

            assertTrue(
                "expected PortInUse but was $error",
                error is ServerError.PortInUse,
            )
        }
    }

    @Test
    fun `stop releases the port so it can be bound again`() = runTest {
        startServer()
        repository.stop().getOrThrow()

        assertFalse(repository.status.value.isRunning)
        // Binding again is the real proof the listener was closed.
        repository.start(ServerConfig(port)).getOrThrow()
        assertEquals(200, get("/health").first)
    }

    @Test
    fun `a busy port is published to the status flow, not just returned`() = runTest {
        // The foreground service calls start() and has no UI, so a bind failure
        // must reach the ViewModel through the status flow.
        ServerSocket(port).use {
            repository.start(ServerConfig(port))

            val published = repository.status.value.startError
            assertTrue(
                "expected PortInUse in the status flow but was $published",
                published is ServerError.PortInUse,
            )
        }
    }

    @Test
    fun `clearStartError removes a consumed failure`() = runTest {
        ServerSocket(port).use {
            repository.start(ServerConfig(port))
        }
        assertTrue(repository.status.value.startError != null)

        repository.clearStartError()

        assertEquals(null, repository.status.value.startError)
    }

    @Test
    fun `a successful start clears a previous failure`() = runTest {
        ServerSocket(port).use {
            repository.start(ServerConfig(port))
        }
        assertTrue(repository.status.value.startError != null)

        startServer()

        assertEquals(null, repository.status.value.startError)
        assertTrue(repository.status.value.isRunning)
    }

    @Test
    fun `stopping when not running is reported as an error`() = runTest {
        val error = repository.stop().exceptionOrNull()

        assertEquals(ServerError.NotRunning, error)
    }

    @Test
    fun `the server stops serving after stop`() = runTest {
        startServer()
        repository.stop().getOrThrow()

        val failed = runCatching { get("/health") }
        assertTrue("expected the request to fail once stopped", failed.isFailure)
    }
}
