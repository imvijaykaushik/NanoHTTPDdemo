package com.example.serverapp.presentation.viewmodel

import app.cash.turbine.test
import com.example.serverapp.core.NetworkAddressProvider
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.model.ServerError
import com.example.serverapp.domain.model.ServerStatus
import com.example.serverapp.domain.repository.HttpServerRepository
import com.example.serverapp.domain.repository.ServerController
import com.example.serverapp.domain.usecase.StartServerUseCase
import com.example.serverapp.domain.usecase.StopServerUseCase
import com.example.serverapp.presentation.intent.ServerIntent
import com.example.serverapp.presentation.state.UiError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The server now runs in a foreground service, so starting is asynchronous:
 * the use case only dispatches a request, and the outcome arrives through the
 * status flow. These tests drive that flow explicitly, which is what the real
 * service does when it binds (or fails to bind) the socket.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerViewModelTest {

    /** Stands in for the foreground service. */
    private class FakeServerController(
        private val statusFlow: MutableStateFlow<ServerStatus>,
    ) : ServerController {
        var startedWith: ServerConfig? = null
        var stopRequested = false

        /** Set to simulate a port that cannot be bound. */
        var failWith: ServerError? = null

        override fun requestStart(config: ServerConfig) {
            startedWith = config
            // Mirror the service: bind, then publish the outcome.
            statusFlow.value = failWith
                ?.let { ServerStatus(isRunning = false, startError = it) }
                ?: ServerStatus(isRunning = true, port = config.port)
        }

        override fun requestStop() {
            stopRequested = true
            statusFlow.value = ServerStatus(isRunning = false)
        }
    }

    private val statusFlow = MutableStateFlow(ServerStatus())
    private val controller = FakeServerController(statusFlow)
    private val repository = mockk<HttpServerRepository>(relaxed = true)
    private val addressProvider = mockk<NetworkAddressProvider>()
    private val dispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ServerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.status } returns statusFlow as StateFlow<ServerStatus>
        every { addressProvider.localIpAddress() } returns "192.168.1.30"
        viewModel = ServerViewModel(
            startServer = StartServerUseCase(controller, repository),
            stopServer = StopServerUseCase(controller),
            serverRepository = repository,
            networkAddressProvider = addressProvider,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to port 8080 and stopped`() {
        assertEquals("8080", viewModel.state.value.port)
        assertFalse(viewModel.state.value.isRunning)
    }

    @Test
    fun `start goes Loading then Running`() = runTest {
        viewModel.state.test {
            assertFalse(awaitItem().isLoading) // initial

            viewModel.onIntent(ServerIntent.StartServer)
            assertTrue(awaitItem().isLoading)

            dispatcher.scheduler.advanceUntilIdle()

            val running = expectMostRecentItem()
            assertTrue(running.isRunning)
            assertFalse(running.isLoading)
            assertEquals(null, running.error)
        }
        assertEquals(ServerConfig(8080), controller.startedWith)
    }

    @Test
    fun `the server is hosted by the service, not bound directly`() = runTest {
        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()

        // The ViewModel must delegate to the controller (the service), so the
        // listener outlives this screen.
        assertEquals(ServerConfig(8080), controller.startedWith)
        io.mockk.coVerify(exactly = 0) { repository.start(any()) }
    }

    @Test
    fun `a busy port reported asynchronously surfaces as an error`() = runTest {
        controller.failWith = ServerError.PortInUse(8080)

        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(UiError.PortInUse(8080), state.error)
        assertFalse(state.isRunning)
        // Loading must clear even though the start request itself "succeeded".
        assertFalse(state.isLoading)
    }

    @Test
    fun `an async start failure is consumed so it is not shown twice`() = runTest {
        controller.failWith = ServerError.PortInUse(8080)

        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()

        io.mockk.verify(atLeast = 1) { repository.clearStartError() }
    }

    @Test
    fun `an invalid port never reaches the controller`() = runTest {
        viewModel.onIntent(ServerIntent.PortChanged("80"))
        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.error is UiError.InvalidPort)
        assertEquals(null, controller.startedWith)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `starting while already running is rejected`() = runTest {
        statusFlow.value = ServerStatus(isRunning = true, port = 8080)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiError.AlreadyRunning, viewModel.state.value.error)
    }

    @Test
    fun `stop clears the running state`() = runTest {
        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isRunning)

        viewModel.onIntent(ServerIntent.StopServer)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(controller.stopRequested)
        assertFalse(viewModel.state.value.isRunning)
    }

    @Test
    fun `request counts from the status flow reach the state`() = runTest {
        statusFlow.value = ServerStatus(
            isRunning = true,
            port = 8080,
            requestCount = 5,
            lastMessage = "Hello Server",
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, viewModel.state.value.requestCount)
        assertEquals("Hello Server", viewModel.state.value.lastMessage)
    }

    @Test
    fun `the client hint shows the LAN address and port`() {
        assertEquals("192.168.1.30:8080", viewModel.state.value.clientHint)
    }

    @Test
    fun `the port field ignores non-digits`() {
        viewModel.onIntent(ServerIntent.PortChanged("90a0b0"))

        assertEquals("9000", viewModel.state.value.port)
    }

    @Test
    fun `dismissing the error clears it`() = runTest {
        viewModel.onIntent(ServerIntent.PortChanged("80"))
        viewModel.onIntent(ServerIntent.StartServer)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.error != null)

        viewModel.onIntent(ServerIntent.DismissError)

        assertEquals(null, viewModel.state.value.error)
    }
}
