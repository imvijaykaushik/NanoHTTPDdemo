package com.example.serverapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.serverapp.presentation.components.ErrorBanner
import com.example.serverapp.presentation.components.InfoRow
import com.example.serverapp.presentation.components.StatusIndicator
import com.example.serverapp.presentation.intent.ServerIntent
import com.example.serverapp.presentation.state.ServerUiState
import com.example.serverapp.presentation.state.displayMessage
import com.example.serverapp.presentation.theme.ServerAppTheme
import com.example.serverapp.presentation.viewmodel.ServerViewModel

/** Connects the ViewModel to the stateless [ServerContent]. */
@Composable
fun ServerScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServerContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/** Renders state and emits intents; holds no logic of its own. */
@Composable
fun ServerContent(
    state: ServerUiState,
    onIntent: (ServerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Server App",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        OutlinedTextField(
            value = state.port,
            onValueChange = { onIntent(ServerIntent.PortChanged(it)) },
            label = { Text("Port") },
            // The port cannot change while bound, so the field locks when running.
            enabled = !state.isRunning && !state.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            supportingText = { Text("Use 1024-65535") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onIntent(ServerIntent.StartServer) },
                enabled = state.canStart,
                modifier = Modifier.weight(1f),
            ) {
                Text("Start Server")
            }
            OutlinedButton(
                onClick = { onIntent(ServerIntent.StopServer) },
                enabled = state.canStop,
                modifier = Modifier.weight(1f),
            ) {
                Text("Stop Server")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusIndicator(
                active = state.isRunning,
                label = if (state.isRunning) "Server Running" else "Server Stopped",
            )
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        state.error?.let { error ->
            ErrorBanner(
                message = error.displayMessage,
                onDismiss = { onIntent(ServerIntent.DismissError) },
            )
        }

        HorizontalDivider()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoRow(label = "Requests received", value = state.requestCount.toString())
                InfoRow(label = "Last message", value = state.lastMessage ?: "-")
                state.lastRequest?.let { request ->
                    InfoRow(
                        label = "Last request",
                        value = "${request.method} ${request.path} -> ${request.statusCode}",
                    )
                }
            }
        }

        // Shown so the user can read off exactly what to type into the client
        // running on another device.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Connect from another device",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = state.clientHint ?: "Not connected to Wi-Fi",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "On this device, the client can use 127.0.0.1:${state.port}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerContentRunningPreview() {
    ServerAppTheme {
        ServerContent(
            state = ServerUiState(
                isRunning = true,
                requestCount = 5,
                lastMessage = "Hello Server",
                localIpAddress = "192.168.1.30",
            ),
            onIntent = {},
        )
    }
}
