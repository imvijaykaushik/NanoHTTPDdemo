package com.example.serverapp.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.serverapp.presentation.screen.ServerScreen
import com.example.serverapp.presentation.theme.ServerAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the Compose UI.
 *
 * Note what this no longer does: it does not stop the server in onDestroy. The
 * server lives in a foreground service now, so it deliberately outlives this
 * activity - that is what keeps it serving while the user is in the Client app.
 * Shutdown happens via the in-app Stop button or the notification's Stop action.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * The foreground-service notification is a runtime permission from Android
     * 13. The server runs either way, but without it the user gets no visible
     * indication that it is listening and no Stop action, so it is requested up
     * front. The result is intentionally ignored: denial must not block the app.
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationPermissionIfNeeded()
        setContent {
            ServerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ServerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun askForNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
