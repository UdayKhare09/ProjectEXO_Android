package dev.uday.projectexo_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.uday.projectexo_android.ui.theme.ProjectEXO_AndroidTheme
import dev.uday.projectexo_android.viewmodel.ConnectionState
import dev.uday.projectexo_android.viewmodel.NetworkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectEXO_AndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SocketClientApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SocketClientApp(modifier: Modifier = Modifier) {
    val viewModel: NetworkViewModel = viewModel()
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var host by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("8080") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection status
        ConnectionStatusIndicator(connectionState)

        // Connection settings
        ConnectionSettings(
            host = host,
            port = port,
            onHostChange = { host = it },
            onPortChange = { port = it },
            onConnectClick = {
                viewModel.connectToSocket(host, port.toIntOrNull() ?: 8080)
            },
            onDisconnectClick = { viewModel.disconnect() },
            isConnected = connectionState is ConnectionState.Connected
        )

        // Messages
        MessagesList(messages = messages)

        // Send message
        SendMessageBar(
            message = message,
            onMessageChange = { message = it },
            onSendClick = {
                if (message.isNotEmpty()) {
                    viewModel.sendMessage(message)
                    message = ""
                }
            },
            isConnected = connectionState is ConnectionState.Connected
        )
    }
}

@Composable
fun ConnectionStatusIndicator(state: ConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
                is ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
                is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Text(
            text = when (state) {
                is ConnectionState.Connected -> "Connected"
                is ConnectionState.Connecting -> "Connecting..."
                is ConnectionState.Disconnected -> "Disconnected"
                is ConnectionState.Error -> "Error: ${state.message}"
            },
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ConnectionSettings(
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    isConnected: Boolean
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected
            )

            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                enabled = !isConnected
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isConnected
                ) {
                    Text("Connect")
                }

                Button(
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(1f),
                    enabled = isConnected
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun MessagesList(messages: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Text(message)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun SendMessageBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            label = { Text("Message") },
            enabled = isConnected,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendClick() })
        )

        IconButton(
            onClick = onSendClick,
            enabled = isConnected && message.isNotEmpty()
        ) {
            Icon(Icons.Filled.Send, contentDescription = "Send")
        }
    }
}