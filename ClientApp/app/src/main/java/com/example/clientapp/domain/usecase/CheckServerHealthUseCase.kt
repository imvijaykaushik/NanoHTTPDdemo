package com.example.clientapp.domain.usecase

import com.example.clientapp.domain.model.ServerAddress
import com.example.clientapp.domain.model.ServerHealth
import com.example.clientapp.domain.repository.ServerRepository
import javax.inject.Inject

/**
 * "Connect" in the UI means: validate the address, then probe GET /health.
 *
 * HTTP is stateless, so there is no long-lived connection to establish. A
 * successful health check is what "connected" actually means here.
 */
class CheckServerHealthUseCase @Inject constructor(
    private val repository: ServerRepository,
) {
    suspend operator fun invoke(host: String, port: String): Result<ServerHealth> =
        ServerAddress.parse(host, port).fold(
            onSuccess = { repository.healthCheck(it) },
            onFailure = { Result.failure(it) },
        )
}
