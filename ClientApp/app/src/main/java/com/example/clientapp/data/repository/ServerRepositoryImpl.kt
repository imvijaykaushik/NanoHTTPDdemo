package com.example.clientapp.data.repository

import com.example.clientapp.core.Logger
import com.example.clientapp.data.dto.MessageRequestDto
import com.example.clientapp.data.mapper.toDomain
import com.example.clientapp.data.mapper.toNetworkError
import com.example.clientapp.data.remote.HostSelectionInterceptor
import com.example.clientapp.data.remote.ServerApi
import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.model.ServerMessage
import com.example.clientapp.domain.repository.ServerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The only class that knows Retrofit exists.
 *
 * Responsibilities: point the shared OkHttp client at the requested address,
 * run the call off the main thread, map DTOs to domain models, and convert
 * failures into [com.example.clientapp.domain.model.NetworkError].
 */
class ServerRepositoryImpl @Inject constructor(
    private val api: ServerApi,
    private val hostSelectionInterceptor: HostSelectionInterceptor,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : ServerRepository {

    override suspend fun healthCheck(address: ServerAddress): Result<ServerHealth> =
        withContext(ioDispatcher) {
            hostSelectionInterceptor.setAddress(address)
            logger.d("CLIENT -> GET ${address.baseUrl}health")
            runCatching { api.health() }
                .onSuccess { logger.d("CLIENT <- 200 status=${it.status}") }
                .map { it.toDomain() }
                .recoverCatching { throw it.toNetworkError() }
                .onFailure { logger.w("CLIENT <- failed: ${it.message}") }
        }

    override suspend fun sendMessage(
        address: ServerAddress,
        message: String,
    ): Result<ServerMessage> = withContext(ioDispatcher) {
        hostSelectionInterceptor.setAddress(address)
        logger.d("CLIENT -> POST ${address.baseUrl}message")
        runCatching { api.sendMessage(MessageRequestDto(message)) }
            .onSuccess { logger.d("CLIENT <- 200") }
            .map { it.toDomain() }
            .recoverCatching { throw it.toNetworkError() }
            .onFailure { logger.w("CLIENT <- failed: ${it.message}") }
    }
}
