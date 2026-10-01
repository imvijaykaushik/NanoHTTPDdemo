package com.example.serverapp.domain.usecase

import com.example.serverapp.domain.repository.ServerController
import javax.inject.Inject

/**
 * Asks the host to shut the server down and release the port.
 *
 * Dispatching to the service, rather than stopping the socket directly, means
 * the service also tears itself down and removes its notification.
 */
class StopServerUseCase @Inject constructor(
    private val controller: ServerController,
) {
    suspend operator fun invoke(): Result<Unit> {
        controller.requestStop()
        return Result.success(Unit)
    }
}
