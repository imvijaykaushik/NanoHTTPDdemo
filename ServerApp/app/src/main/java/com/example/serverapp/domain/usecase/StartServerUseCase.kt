package com.example.serverapp.domain.usecase

import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.model.ServerError
import com.example.serverapp.domain.repository.HttpServerRepository
import com.example.serverapp.domain.repository.ServerController
import javax.inject.Inject

/**
 * Validates the port, then asks the [ServerController] to host the server.
 *
 * The socket is bound inside the foreground service rather than here, so the
 * listener is owned by a component that outlives the current screen. Validation
 * still happens up front, so an invalid port never starts a service at all.
 */
class StartServerUseCase @Inject constructor(
    private val controller: ServerController,
    private val repository: HttpServerRepository,
) {
    suspend operator fun invoke(port: String): Result<Unit> =
        ServerConfig.parse(port).fold(
            onSuccess = { config ->
                if (repository.status.value.isRunning) {
                    return Result.failure(ServerError.AlreadyRunning)
                }
                controller.requestStart(config)
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
}
