package com.example.serverapp.data.repository

import com.example.serverapp.core.Logger
import com.example.serverapp.data.http.EmbeddedHttpServer
import com.example.serverapp.data.http.RouteHandler
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.model.ServerError
import com.example.serverapp.domain.model.ServerStatus
import com.example.serverapp.domain.repository.HttpServerRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.BindException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the embedded server's lifecycle.
 *
 * Start and stop are serialised with a [Mutex] so that rapid taps cannot leave
 * two listeners bound or leak one that is never stopped.
 */
@Singleton
class HttpServerRepositoryImpl @Inject constructor(
    private val routeHandler: RouteHandler,
    private val statusHolder: ServerStatusHolder,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : HttpServerRepository {

    private val lifecycleMutex = Mutex()

    /** Non-null exactly while a listener is bound. */
    private var server: EmbeddedHttpServer? = null

    override val status: StateFlow<ServerStatus> = statusHolder.status

    override suspend fun start(config: ServerConfig): Result<Unit> =
        // NonCancellable: if the caller's scope dies midway through binding we
        // must still finish, otherwise the socket would leak with no handle to it.
        withContext(ioDispatcher + NonCancellable) {
            lifecycleMutex.withLock {
                if (server != null) {
                    return@withLock Result.failure(ServerError.AlreadyRunning)
                }

                val instance = EmbeddedHttpServer(config.port, routeHandler, logger)
                try {
                    // false = do not use a daemon thread, so the listener is not
                    // torn down arbitrarily by the JVM on the last user thread exit.
                    instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                    server = instance
                    statusHolder.onStarted(config.port)
                    logger.d("SERVER started on port ${config.port}")
                    Result.success(Unit)
                } catch (e: BindException) {
                    instance.stop()
                    logger.w("SERVER port ${config.port} already in use")
                    failStart(ServerError.PortInUse(config.port))
                } catch (e: IOException) {
                    instance.stop()
                    // Android also surfaces a busy port as a plain IOException
                    // ("Address already in use"), so inspect the message.
                    val message = e.message.orEmpty()
                    if ("EADDRINUSE" in message || "Address already in use" in message) {
                        logger.w("SERVER port ${config.port} already in use")
                        failStart(ServerError.PortInUse(config.port))
                    } else {
                        logger.e("SERVER failed to start", e)
                        failStart(ServerError.StartFailed(message.ifEmpty { "I/O error" }))
                    }
                }
            }
        }

    /**
     * Records the failure in the status flow *and* returns it.
     *
     * Both paths matter: the service calls start() and has no UI, so the error
     * has to travel through the flow to be seen.
     */
    private fun failStart(error: ServerError): Result<Unit> {
        statusHolder.onStartFailed(error)
        return Result.failure(error)
    }

    override fun clearStartError() {
        statusHolder.clearStartError()
    }

    override suspend fun stop(): Result<Unit> =
        withContext(ioDispatcher + NonCancellable) {
            lifecycleMutex.withLock {
                val instance = server
                    ?: return@withLock Result.failure(ServerError.NotRunning)

                // stop() closes the listening socket and joins the worker threads.
                instance.stop()
                server = null
                statusHolder.onStopped()
                logger.d("SERVER stopped")
                Result.success(Unit)
            }
        }
}
