package com.example.serverapp.data.repository

import com.example.serverapp.domain.model.RequestLog
import com.example.serverapp.domain.model.ServerError
import com.example.serverapp.domain.model.ServerStatus
import com.example.serverapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for server status.
 *
 * Shared between the HTTP threads (which record requests) and the UI (which
 * observes). [MutableStateFlow.update] is atomic, so concurrent requests cannot
 * lose an increment.
 */
@Singleton
class ServerStatusHolder @Inject constructor() : MessageRepository {

    private val _status = MutableStateFlow(ServerStatus())
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    override fun recordMessage(message: String) {
        _status.update { it.copy(lastMessage = message) }
    }

    override fun recordRequest(log: RequestLog) {
        _status.update {
            it.copy(requestCount = it.requestCount + 1, lastRequest = log)
        }
    }

    fun onStarted(port: Int) {
        // Counters reset on each start, so the UI reflects the current session.
        // A successful start also clears any previous failure.
        _status.value = ServerStatus(isRunning = true, port = port)
    }

    fun onStopped() {
        _status.update { it.copy(isRunning = false, port = null) }
    }

    /**
     * Publishes a failed start attempt. The server is bound asynchronously in a
     * foreground service, so this flow is how the UI learns the port was busy.
     */
    fun onStartFailed(error: ServerError) {
        _status.update { it.copy(isRunning = false, port = null, startError = error) }
    }

    /** Clears a consumed failure so it is not shown twice. */
    fun clearStartError() {
        _status.update { it.copy(startError = null) }
    }
}
