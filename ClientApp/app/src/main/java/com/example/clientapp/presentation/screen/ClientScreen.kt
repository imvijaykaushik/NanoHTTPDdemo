package com.example.clientapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.clientapp.presentation.components.ErrorBanner
import com.example.clientapp.presentation.components.ResponseCard
import com.example.clientapp.presentation.components.StatusIndicator
import com.example.clientapp.presentation.intent.ClientIntent
import com.example.clientapp.presentation.state.ClientUiState
import com.example.clientapp.presentation.state.displayMessage
import com.example.clientapp.presentation.theme.ClientAppTheme
import com.example.clientapp.presentation.viewmodel.ClientViewModel

/**
 * Connects the ViewModel to the stateless [ClientContent].
 *
 * Splitting the two means the layout can be previewed and tested without Hilt
 * or a real ViewModel.
 */
@Composable
fun ClientScreen(
    modifier: Modifier = Modifier,
    viewModel: ClientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClientContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/**
 * Renders state and emits intents. It holds no business logic and no mutable
 * state of its own - every change goes out as a [ClientIntent] and comes back
 * as a new [ClientUiState].
 */
@Composable
fun ClientContent(
    state: ClientUiState,
    onIntent: (ClientIntent) -> Unit,
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
            text = "Client App",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        OutlinedTextField(
            value = state.serverAddress,
            onValueChange = { onIntent(ClientIntent.ServerAddressChanged(it)) },
            label = { Text("Server IP") },
            placeholder = { Text("192.168.1.30") },
            supportingText = { Text("Use 127.0.0.1 when the Server app is on this device") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.port,
            onValueChange = { onIntent(ClientIntent.PortChanged(it)) },
            label = { Text("Port") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onIntent(ClientIntent.Connect) },
            enabled = state.canConnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusIndicator(
                active = state.isConnected,
                label = if (state.isConnected) "Connected" else "Not connected",
            )
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = state.message,
            onValueChange = { onIntent(ClientIntent.MessageChanged(it)) },
            label = { Text("Message") },
            placeholder = { Text("Hello Server") },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onIntent(ClientIntent.SendMessage(state.message)) },
            enabled = state.canSendMessage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send Message")
        }

        state.error?.let { error ->
            ErrorBanner(
                message = error.displayMessage,
                onDismiss = { onIntent(ClientIntent.DismissError) },
            )
        }

        ResponseCard(response = state.response)
    }
}

@Preview(showBackground = true)
@Composable
private fun ClientContentConnectedPreview() {
    ClientAppTheme {
        ClientContent(
            state = ClientUiState(
                serverAddress = "192.168.1.30",
                message = "Hello Server",
                response = "Hello from Server!",
                isConnected = true,
            ),
            onIntent = {},
        )
    }
}
