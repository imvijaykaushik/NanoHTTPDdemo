package com.example.serverapp.data.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.serverapp.core.Logger
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.repository.HttpServerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the embedded HTTP server so it keeps serving while the user is in the
 * Client app.
 *
 * Why a service at all: a plain backgrounded activity can be killed at any time
 * under memory pressure, which would silently drop the listening socket. A
 * foreground service tells Android the work is user-visible, and the persistent
 * notification is the trade-off Android requires in return.
 *
 * The service is the single owner of the server lifecycle - the ViewModel sends
 * it intents rather than calling the repository directly, so there is exactly
 * one place that starts and stops the listener.
 */
@AndroidEntryPoint
class HttpServerService : Service() {

    @Inject
    lateinit var serverRepository: HttpServerRepository

    @Inject
    lateinit var notifications: ServerNotifications

    @Inject
    lateinit var logger: Logger

    /**
     * Tied to the service, not to any screen, so an in-flight start/stop is not
     * cancelled when the user navigates away.
     */
    private val serviceScope = CoroutineScope(SupervisorJob())

    /** Never bound; commands arrive through [onStartCommand]. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications.createChannel()
        observeStatusForNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                startServer(port)
            }

            ACTION_STOP -> stopServer()

            else -> {
                // Restarted by the system with a null intent and nothing to do.
                logger.w("SERVER service started with no action; stopping")
                stopSelf()
            }
        }

        // NOT_STICKY: if the process dies we do not want Android recreating the
        // service with a null intent, because the chosen port would be unknown.
        return START_NOT_STICKY
    }

    private fun startServer(port: Int) {
        serviceScope.launch {
            // Promote to foreground *before* binding the socket, so the service
            // cannot be killed mid-start. Android requires startForeground()
            // within a few seconds of the service being started.
            promoteToForeground(notifications.build(port, requestCount = 0))

            serverRepository.start(ServerConfig(port))
                .onSuccess { logger.d("SERVER service started on port $port") }
                .onFailure { throwable ->
                    // The port was busy or invalid, so there is nothing to keep
                    // alive. Errors reach the UI through the repository result;
                    // the service just stands down.
                    logger.w("SERVER service failed to start: ${throwable.message}")
                    stopForegroundCompat()
                    stopSelf()
                }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            serverRepository.stop()
            logger.d("SERVER service stopping")
            stopForegroundCompat()
            stopSelf()
        }
    }

    /**
     * Keeps the notification's request count current while the server runs.
     *
     * Only updates an already-foreground service; it never promotes one, so a
     * status emission cannot resurrect a stopped server.
     */
    private fun observeStatusForNotification() {
        serviceScope.launch {
            serverRepository.status
                // The first emission is the pre-start state; startServer posts
                // the initial notification itself.
                .drop(1)
                .distinctUntilChanged { old, new ->
                    old.isRunning == new.isRunning && old.requestCount == new.requestCount
                }
                .collect { status ->
                    val port = status.port
                    if (status.isRunning && port != null) {
                        NotificationManagerCompat.from(this@HttpServerService).notify(
                            ServerNotifications.NOTIFICATION_ID,
                            notifications.build(port, status.requestCount),
                        )
                    }
                }
        }
    }

    private fun promoteToForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // From Android 10 the type must be restated here, matching the
            // android:foregroundServiceType declared in the manifest.
            startForeground(
                ServerNotifications.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(ServerNotifications.NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Last line of defence: if the service is destroyed by the system
        // without ACTION_STOP, the socket must still be released. The repository
        // runs this in NonCancellable, so cancelling the scope cannot abort it.
        if (serverRepository.status.value.isRunning) {
            logger.w("SERVER service destroyed while running; releasing port")
            kotlinx.coroutines.runBlocking { serverRepository.stop() }
        }
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.serverapp.action.START"
        const val ACTION_STOP = "com.example.serverapp.action.STOP"
        private const val EXTRA_PORT = "port"
        private const val DEFAULT_PORT = 8080

        /** Starts the server on [port] via the foreground service. */
        fun start(context: Context, port: Int) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            // startForegroundService is required from Android 8 when the app is
            // not in the foreground; the service must then call startForeground.
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stops the server and the service. */
        fun stop(context: Context) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
