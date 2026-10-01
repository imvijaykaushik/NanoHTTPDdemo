package com.example.clientapp.domain.repository

import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.model.ServerMessage

/**
 * The domain's view of the remote server.
 *
 * Declared here (in domain) but implemented in data - this is what inverts the
 * dependency so that domain never points at data. Returning [Result] keeps
 * failures explicit instead of relying on thrown exceptions.
 */
interface ServerRepository {

    suspend fun healthCheck(address: ServerAddress): Result<ServerHealth>

    suspend fun sendMessage(address: ServerAddress, message: String): Result<ServerMessage>
}
