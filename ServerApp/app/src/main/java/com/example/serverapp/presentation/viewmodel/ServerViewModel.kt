package com.example.serverapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serverapp.core.NetworkAddressProvider
import com.example.serverapp.domain.repository.HttpServerRepository
import com.example.serverapp.domain.usecase.StartServerUseCase
import com.example.serverapp.domain.usecase.StopServerUseCase
import com.example.serverapp.presentation.intent.ServerIntent
import com.example.serverapp.presentation.state.ServerUiState
import com.example.serverapp.presentation.state.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Turns intents into state, and mirrors the running server's status into the UI.
 */
@HiltViewModel
class ServerViewModel @Inject constructor(
    private val startServer: StartServerUseCase,
    private val stopServer: StopServerUseCase,
    private val serverRepository: HttpServerRepository,
    private val networkAddressProvider: NetworkAddressProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    init {
        observeServerStatus()
        resolveLocalIp()
    }

    /**
     * Requests arrive on HTTP worker threads and are published through the
     * repository's StateFlow; collecting it keeps the counters live without the
     * UI polling.
     */
    private fun observeServerStatus() {
        viewModelScope.launch {
            serverRepository.status.collect { status ->
                _state.update {
                    it.copy(
                        isRunning = status.isRunning,
                        requestCount = status.requestCount,
                        lastMessage = status.lastMessage,
                        lastRequest = status.lastRequest,
                        // The service binds the socket asynchronously, so a
                        // busy port surfaces here rather than from the start
                        // call. Clear loading once the outcome is known either way.
                        isLoading = when {
                            status.isRunning -> false
                            status.startError != null -> false
                            else -> it.isLoading
                        },
                        error = status.startError?.toUiError() ?: it.error,
                    )
                }
                // Consume the failure so it is not re-applied on later emissions.
                if (status.startError != null) {
                    serverRepository.clearStartError()
                }
            }
        }
    }

    private fun resolveLocalIp() {
        _state.update { it.copy(localIpAddress = networkAddressProvider.localIpAddress()) }
    }

    fun onIntent(intent: ServerIntent) {
        when (intent) {
            is ServerIntent.PortChanged -> _state.update {
                it.copy(port = intent.value.filter(Char::isDigit), error = null)
            }

            ServerIntent.StartServer -> start()
            ServerIntent.StopServer -> stop()
            ServerIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun start() {
        val current = _state.value
        if (current.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            startServer(current.port).fold(
                // Loading is deliberately left on: this only means the service
                // was asked to start. isRunning (or a startError) arrives via
                // the status flow, which is the single source of truth.
                onSuccess = { },
                onFailure = { throwable ->
                    // Validation failed, so no service was started.
                    _state.update { it.copy(isLoading = false, error = throwable.toUiError()) }
                },
            )
            // Re-read the IP: it may have been unavailable before Wi-Fi connected.
            resolveLocalIp()
        }
    }

    private fun stop() {
        if (_state.value.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            stopServer().fold(
                onSuccess = { _state.update { it.copy(isLoading = false, error = null) } },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, error = throwable.toUiError()) }
                },
            )
        }
    }
}
