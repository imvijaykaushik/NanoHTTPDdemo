package com.example.serverapp.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.serverapp.presentation.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the persistent notification that a foreground service must display.
 *
 * Split out from [HttpServerService] so the service deals only with lifecycle
 * and this class deals only with presentation.
 */
@Singleton
class ServerNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Notification channels are required from Android 8 (API 26). Creating one
     * that already exists is a no-op, so this is safe to call repeatedly.
     */
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "HTTP Server",
            // LOW: the notification must be visible, but it should not make a
            // sound or vibrate every time the server starts.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while the embedded HTTP server is running"
            setShowBadge(false)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    /** Notification shown while the server is listening on [port]. */
    fun build(port: Int, requestCount: Int): Notification {
        // Tapping the notification reopens the app rather than starting a second
        // task, so the user returns to the existing screen.
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // A Stop action is essential: with stopWithTask="false" the service
        // outlives the app being swiped away, so the notification must always
        // offer a way to shut the server down.
        val stopServer = PendingIntent.getService(
            context,
            REQUEST_STOP,
            Intent(context, HttpServerService::class.java).apply {
                action = HttpServerService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Server running on port $port")
            .setContentText(
                if (requestCount == 0) {
                    "Waiting for requests"
                } else {
                    "$requestCount request${if (requestCount == 1) "" else "s"} handled"
                },
            )
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopServer)
            // Ongoing + no timestamp: this reflects current state, not an event.
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "http_server"
        private const val REQUEST_OPEN_APP = 0
        private const val REQUEST_STOP = 1
    }
}
