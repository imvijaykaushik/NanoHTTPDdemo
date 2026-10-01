package com.example.serverapp.contract

import com.example.serverapp.core.NoOpLogger
import com.example.serverapp.data.http.RouteHandler
import com.example.serverapp.data.repository.HttpServerRepositoryImpl
import com.example.serverapp.data.repository.ServerStatusHolder
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.usecase.HandleClientMessageUseCase
import com.example.serverapp.domain.usecase.HandleHealthCheckUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Pins the wire contract documented in the README.
 *
 * Runs the real embedded server and asserts the exact JSON shape, so a change
 * that silently renames a field fails here rather than in the client app.
 */
class ApiContractTest {

    private lateinit var repository: HttpServerRepositoryImpl
    private var port = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() = runBlocking {
        val statusHolder = ServerStatusHolder()
        repository = HttpServerRepositoryImpl(
            routeHandler = RouteHandler(
                handleHealthCheck = HandleHealthCheckUseCase(),
                handleClientMessage = HandleClientMessageUseCase(statusHolder),
                messageRepository = statusHolder,
                json = Json { ignoreUnknownKeys = true },
                logger = NoOpLogger(),
            ),
            statusHolder = statusHolder,
            ioDispatcher = Dispatchers.IO,
            logger = NoOpLogger(),
        )
        repeat(5) {
            port = ServerSocket(0).use { socket -> socket.localPort }
            if (repository.start(ServerConfig(port)).isSuccess) return@runBlocking
        }
        error("could not bind a free port")
    }

    @After
    fun tearDown() {
        runBlocking { repository.stop() }
    }

    @Test
    fun `GET health matches the documented contract`() = runTest {
        val request = Request.Builder().url("http://127.0.0.1:$port/health").get().build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = Json.parseToJsonElement(response.body!!.string())
            assertEquals("OK", body.jsonObjectValue("status"))
            assertEquals("Server is running", body.jsonObjectValue("message"))
        }
    }

    @Test
    fun `POST message matches the documented contract`() = runTest {
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/message")
            .post("""{"message":"Hello Server"}""".toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = Json.parseToJsonElement(response.body!!.string())
            assertEquals("Hello from Server!", body.jsonObjectValue("message"))
            assertEquals("Hello Server", body.jsonObjectValue("received"))
        }
    }

    @Test
    fun `responses are sent as JSON`() = runTest {
        val request = Request.Builder().url("http://127.0.0.1:$port/health").get().build()

        client.newCall(request).execute().use { response ->
            assertEquals("application/json", response.header("Content-Type"))
        }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectValue(key: String): String =
        (this as kotlinx.serialization.json.JsonObject).getValue(key).jsonPrimitive.content
}
