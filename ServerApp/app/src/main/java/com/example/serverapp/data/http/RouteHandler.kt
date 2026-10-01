package com.example.serverapp.data.http

import com.example.serverapp.core.Logger
import com.example.serverapp.data.dto.ErrorResponseDto
import com.example.serverapp.data.dto.HealthResponseDto
import com.example.serverapp.data.dto.MessageRequestDto
import com.example.serverapp.data.dto.MessageResponseDto
import com.example.serverapp.domain.model.RequestLog
import com.example.serverapp.domain.repository.MessageRepository
import com.example.serverapp.domain.usecase.HandleClientMessageUseCase
import com.example.serverapp.domain.usecase.HandleHealthCheckUseCase
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** A framework-independent HTTP reply: status, JSON body, content type. */
data class HttpReply(
    val statusCode: Int,
    val body: String,
    val contentType: String = "application/json",
)

/** The subset of an HTTP request the routes need. */
data class HttpRequest(
    val method: String,
    val path: String,
    val body: String,
)

/**
 * Maps requests to use cases and use-case results to JSON replies.
 *
 * Deliberately knows nothing about NanoHTTPD: it takes an [HttpRequest] and
 * returns an [HttpReply], which means the whole routing layer is unit-testable
 * without opening a socket.
 */
@Singleton
class RouteHandler @Inject constructor(
    private val handleHealthCheck: HandleHealthCheckUseCase,
    private val handleClientMessage: HandleClientMessageUseCase,
    private val messageRepository: MessageRepository,
    private val json: Json,
    private val logger: Logger,
) {

    fun handle(request: HttpRequest): HttpReply {
        logger.d("SERVER received ${request.method} ${request.path}")

        val reply = when {
            request.method == "GET" && request.path.matchesRoute(HEALTH) -> health()

            request.method == "POST" && request.path.matchesRoute(MESSAGE) -> message(request.body)

            // Right path, wrong verb: 405 is more useful to a client than 404.
            request.path.matchesRoute(HEALTH) || request.path.matchesRoute(MESSAGE) ->
                error(405, "Method ${request.method} not allowed for ${request.path}")

            else -> error(404, "No handler for ${request.path}")
        }

        logger.d("SERVER responding with ${reply.statusCode}")
        messageRepository.recordRequest(
            RequestLog(request.method, request.path, reply.statusCode),
        )
        return reply
    }

    private fun health(): HttpReply {
        val status = handleHealthCheck()
        return HttpReply(
            statusCode = 200,
            body = json.encodeToString(
                HealthResponseDto(status = status.status, message = status.message),
            ),
        )
    }

    private fun message(rawBody: String): HttpReply {
        // Parse first: a malformed body is a 400, not a 500.
        val incoming = runCatching {
            json.decodeFromString<MessageRequestDto>(rawBody).message
        }.getOrElse {
            return error(400, "Expected JSON body of the form {\"message\":\"...\"}")
        }

        return handleClientMessage(incoming).fold(
            onSuccess = { reply ->
                // Length only - the message text itself is not logged.
                logger.d("SERVER received message (${reply.received.length} chars)")
                HttpReply(
                    statusCode = 200,
                    body = json.encodeToString(
                        MessageResponseDto(message = reply.message, received = reply.received),
                    ),
                )
            },
            onFailure = { error(400, it.message ?: "Invalid message") },
        )
    }

    private fun error(code: Int, detail: String): HttpReply = HttpReply(
        statusCode = code,
        body = json.encodeToString(ErrorResponseDto(error = detail)),
    )

    /** Tolerates an optional trailing slash, e.g. both /health and /health/. */
    private fun String.matchesRoute(route: String): Boolean =
        this == route || this == "$route/"

    private companion object {
        const val HEALTH = "/health"
        const val MESSAGE = "/message"
    }
}
