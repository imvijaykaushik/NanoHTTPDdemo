package com.example.clientapp.presentation.viewmodel

import app.cash.turbine.test
import com.example.clientapp.domain.model.NetworkError
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.model.ServerMessage
import com.example.clientapp.domain.repository.ServerRepository
import com.example.clientapp.domain.usecase.CheckServerHealthUseCase
import com.example.clientapp.domain.usecase.SendMessageUseCase
import com.example.clientapp.presentation.intent.ClientIntent
import com.example.clientapp.presentation.state.UiError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Verifies the MVI transitions: an intent goes in, and a specific sequence of
 * states comes out. Turbine lets us assert the intermediate Loading state,
 * which a simple `state.value` read would miss.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientViewModelTest {

    private val repository = mockk<ServerRepository>()
    private val dispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ClientViewModel

    @Before
    fun setUp() {
        // viewModelScope uses Dispatchers.Main, which has no Android looper in
        // a JVM test, so it is replaced with a test dispatcher.
        Dispatchers.setMain(dispatcher)
        viewModel = ClientViewModel(
            checkServerHealth = CheckServerHealthUseCase(repository),
            sendMessage = SendMessageUseCase(repository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to loopback and port 8080`() {
        val state = viewModel.state.value

        assertEquals("127.0.0.1", state.serverAddress)
        assertEquals("8080", state.port)
        assertFalse(state.isConnected)
        assertFalse(state.isLoading)
    }

    @Test
    fun `connect goes Loading then Success`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))

        viewModel.state.test {
            assertFalse(awaitItem().isLoading) // initial

            viewModel.onIntent(ClientIntent.Connect)

            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertFalse(loading.isConnected)

            dispatcher.scheduler.advanceUntilIdle()

            val success = awaitItem()
            assertFalse(success.isLoading)
            assertTrue(success.isConnected)
            assertEquals("OK: Server is running", success.response)
            assertEquals(null, success.error)
        }
    }

    @Test
    fun `connect goes Loading then Error when the server is unavailable`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.failure(NetworkError.ConnectionRefused)

        viewModel.state.test {
            awaitItem() // initial

            viewModel.onIntent(ClientIntent.Connect)
            assertTrue(awaitItem().isLoading)

            dispatcher.scheduler.advanceUntilIdle()

            val failed = awaitItem()
            assertFalse(failed.isLoading)
            assertFalse(failed.isConnected)
            assertEquals(UiError.ConnectionRefused, failed.error)
        }
    }

    @Test
    fun `send message goes Loading then Success`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.success(ServerMessage("Hello from Server!", "Hello Server"))

        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(ClientIntent.MessageChanged("Hello Server"))

        viewModel.state.test {
            assertTrue(awaitItem().isConnected) // connected, not loading

            viewModel.onIntent(ClientIntent.SendMessage("Hello Server"))
            assertTrue(awaitItem().isLoading)

            dispatcher.scheduler.advanceUntilIdle()

            val success = awaitItem()
            assertFalse(success.isLoading)
            assertEquals("Hello from Server!", success.response)
        }
    }

    @Test
    fun `an HTTP error is surfaced with its status code`() = runTest {
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.failure(NetworkError.Http(404))

        viewModel.onIntent(ClientIntent.SendMessage("Hello"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiError.HttpError(404), viewModel.state.value.error)
    }

    @Test
    fun `a timeout clears the connected flag`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))
        coEvery { repository.sendMessage(any(), any()) } returns
            Result.failure(NetworkError.Timeout)

        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isConnected)

        viewModel.onIntent(ClientIntent.SendMessage("Hello"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiError.Timeout, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isConnected)
    }

    @Test
    fun `a blank message is rejected as a validation error and keeps the connection`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))

        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(ClientIntent.SendMessage("   "))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.error is UiError.Validation)
        // Validation says nothing about the server, so stay connected.
        assertTrue(viewModel.state.value.isConnected)
    }

    @Test
    fun `changing the address invalidates the previous connection`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))

        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isConnected)

        viewModel.onIntent(ClientIntent.ServerAddressChanged("10.0.0.1"))

        assertFalse(viewModel.state.value.isConnected)
    }

    @Test
    fun `the port field ignores non-digits`() {
        viewModel.onIntent(ClientIntent.PortChanged("80a8b0"))

        assertEquals("8080", viewModel.state.value.port)
    }

    @Test
    fun `dismissing the error clears it`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.failure(NetworkError.Timeout)

        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiError.Timeout, viewModel.state.value.error)

        viewModel.onIntent(ClientIntent.DismissError)

        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun `a second connect is ignored while one is in flight`() = runTest {
        coEvery { repository.healthCheck(any()) } returns
            Result.success(ServerHealth("OK", "Server is running"))

        viewModel.onIntent(ClientIntent.Connect)
        // Not advanced yet, so the first call is still running.
        viewModel.onIntent(ClientIntent.Connect)
        dispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify(exactly = 1) { repository.healthCheck(any()) }
    }
}
