package com.example.serverapp.domain.repository

import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.model.ServerStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Controls the embedded HTTP server, without naming the library that
 * implements it. Declared in domain, implemented in data.
 */
interface HttpServerRepository {

    /** Live status, so the UI reflects request counts as they arrive. */
    val status: StateFlow<ServerStatus>

    suspend fun start(config: ServerConfig): Result<Unit>

    suspend fun stop(): Result<Unit>

    /** Clears a start failure once it has been shown to the user. */
    fun clearStartError()
}
