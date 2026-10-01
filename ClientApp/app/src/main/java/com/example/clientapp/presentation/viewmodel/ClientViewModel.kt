package com.example.clientapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clientapp.domain.usecase.CheckServerHealthUseCase
import com.example.clientapp.domain.usecase.SendMessageUseCase
import com.example.clientapp.presentation.intent.ClientIntent
import com.example.clientapp.presentation.state.ClientUiState
import com.example.clientapp.presentation.state.isTransport
import com.example.clientapp.presentation.state.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Turns intents into state.
 *
 * Note what is absent: no Retrofit, no OkHttp, no DTOs. The ViewModel only
 * calls use cases and folds their [Result] into [ClientUiState].
 */
@HiltViewModel
class ClientViewModel @Inject constructor(
    private val checkServerHealth: CheckServerHealthUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ClientUiState())
    val state: StateFlow<ClientUiState> = _state.asStateFlow()

    /** The single entry point from the UI. */
    fun onIntent(intent: ClientIntent) {
        when (intent) {
            is ClientIntent.ServerAddressChanged -> _state.update {
                // Changing the target invalidates the previous health check.
                it.copy(serverAddress = intent.value, isConnected = false, error = null)
            }

            is ClientIntent.PortChanged -> _state.update {
                it.copy(port = intent.value.filter(Char::isDigit), isConnected = false, error = null)
            }

            is ClientIntent.MessageChanged -> _state.update {
                it.copy(message = intent.value, error = null)
            }

            ClientIntent.Connect -> connect()

            is ClientIntent.SendMessage -> send(intent.message)

            ClientIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun connect() {
        val current = _state.value
        if (current.isLoading) return

        // Loading -> (Success | Error): the transition asserted in the tests.
        _state.update { it.copy(isLoading = true, error = null, response = "") }

        viewModelScope.launch {
            checkServerHealth(current.serverAddress, current.port).fold(
                onSuccess = { health ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true,
                            response = "${health.status}: ${health.message}",
                            error = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isConnected = false,
                            error = throwable.toUiError(),
                        )
                    }
                },
            )
        }
    }

    private fun send(message: String) {
        val current = _state.value
        if (current.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            sendMessage(current.serverAddress, current.port, message).fold(
                onSuccess = { serverMessage ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true,
                            response = serverMessage.message,
                            error = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    val uiError = throwable.toUiError()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            // A transport failure means the server is no longer
                            // reachable, so drop the connected flag. A validation
                            // error says nothing about the server, so keep it.
                            isConnected = if (uiError.isTransport()) false else it.isConnected,
                            error = uiError,
                        )
                    }
                },
            )
        }
    }
}
