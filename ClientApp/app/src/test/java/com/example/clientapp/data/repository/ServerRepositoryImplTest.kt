package com.example.clientapp.data.repository

import com.example.clientapp.core.NoOpLogger
import com.example.clientapp.data.remote.HostSelectionInterceptor
import com.example.clientapp.data.remote.ServerApi
import com.example.clientapp.domain.model.NetworkError
import com.example.clientapp.domain.model.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Exercises the repository over real HTTP against MockWebServer, so the
 * Retrofit wiring, JSON parsing, and exception-to-domain-error mapping are all
 * covered rather than mocked away.
 */
class ServerRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ServerRepositoryImpl
    private lateinit var address: ServerAddress

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val interceptor = HostSelectionInterceptor()
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ServerApi::class.java)

        repository = ServerRepositoryImpl(
            api = api,
            hostSelectionInterceptor = interceptor,
            // A real dispatcher, not a test one: these tests perform genuine
            // blocking socket I/O against MockWebServer, which a virtual-time
            // test scheduler would not drive.
            ioDispatcher = Dispatchers.IO,
            logger = NoOpLogger(),
        )
        address = ServerAddress(mockWebServer.hostName, mockWebServer.port)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `healthCheck parses a successful response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"OK","message":"Server is running"}"""),
        )

        val health = repository.healthCheck(address).getOrThrow()

        assertEquals("OK", health.status)
        assertEquals("Server is running", health.message)
        assertEquals("/health", mockWebServer.takeRequest().path)
    }

    @Test
    fun `sendMessage posts the message and parses the reply`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"Hello from Server!","received":"Hello Server"}"""),
        )

        val reply = repository.sendMessage(address, "Hello Server").getOrThrow()

        assertEquals("Hello from Server!", reply.message)
        assertEquals("Hello Server", reply.received)

        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/message", recorded.path)
        assertEquals("""{"message":"Hello Server"}""", recorded.body.readUtf8())
    }

    @Test
    fun `maps a 500 response to an Http error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val error = repository.sendMessage(address, "Hello").exceptionOrNull()

        assertEquals(NetworkError.Http(500), error)
    }

    @Test
    fun `maps a 404 response to an Http error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

        val error = repository.healthCheck(address).exceptionOrNull()

        assertEquals(NetworkError.Http(404), error)
    }

    @Test
    fun `maps an unparseable body to MalformedResponse`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                // "status" is missing, so deserialization must fail.
                .setBody("""{"unexpected":"shape"}"""),
        )

        val error = repository.healthCheck(address).exceptionOrNull()

        assertEquals(NetworkError.MalformedResponse, error)
    }

    @Test
    fun `maps a stalled connection to Timeout`() = runTest {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = repository.healthCheck(address).exceptionOrNull()

        assertEquals(NetworkError.Timeout, error)
    }

    @Test
    fun `maps a closed port to ConnectionRefused`() = runTest {
        // Bind a port, then release it, so nothing is listening but the port is
        // known to be free - this is what a stopped Server app looks like.
        val freePort = ServerSocket(0).use { it.localPort }
        val deadAddress = ServerAddress("127.0.0.1", freePort)

        val error = repository.healthCheck(deadAddress).exceptionOrNull()

        assertTrue(
            "expected ConnectionRefused but was $error",
            error is NetworkError.ConnectionRefused,
        )
    }
}
