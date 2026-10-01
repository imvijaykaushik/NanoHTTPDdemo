package com.example.serverapp.domain.usecase

import com.example.serverapp.domain.model.HealthStatus
import javax.inject.Inject

/**
 * Business logic for `GET /health`.
 *
 * Lives in domain rather than in the HTTP layer, so the route handler only
 * translates between HTTP and the domain - the reply itself is defined here
 * and is testable without a socket.
 */
class HandleHealthCheckUseCase @Inject constructor() {
    operator fun invoke(): HealthStatus = HealthStatus(
        status = "OK",
        message = "Server is running",
    )
}
