package dev.uday.projectexo_android.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.uday.projectexo_android.net.ClientSocket
import dev.uday.projectexo_android.net.ServerBroadcastReceiver

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverIP by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerList by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val serverList = remember { mutableStateListOf<ServerBroadcastReceiver.ServerInfo>() }
    var selectedServer by remember { mutableStateOf<ServerBroadcastReceiver.ServerInfo?>(null) }

    DisposableEffect(Unit) {
        val receiver = ServerBroadcastReceiver()
        val thread = Thread(receiver)
        thread.start()

        val updateThread = Thread {
            while (true) {
                try {
                    // Update the server list on the UI thread
                    Handler(Looper.getMainLooper()).post {
                        serverList.clear()
                        serverList.addAll(ServerBroadcastReceiver.availableServers.values)
                    }
                    Thread.sleep(1000) // Update every second
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        updateThread.isDaemon = true
        updateThread.start()

        onDispose {
            receiver.stop()
            updateThread.interrupt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ProjectEXO",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Server Selection",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Server selection button
                Button(
                    onClick = { showServerList = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedServer?.toString() ?: "Select Server"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual server entry fields
                if (selectedServer == null) {
                    OutlinedTextField(
                        value = serverIP,
                        onValueChange = { serverIP = it },
                        label = { Text("Server IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Server Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                } else {
                    TextButton(
                        onClick = { selectedServer = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enter Manually")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Login Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Login fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isLoading && username.isNotBlank() && password.isNotBlank() &&
                                (selectedServer != null || (serverIP.isNotBlank() && serverPort.isNotBlank()))) {
                                attemptLogin(
                                    username, password,
                                    selectedServer?.ipAddress ?: serverIP,
                                    selectedServer?.port ?: serverPort,
                                    onLoadingChange = { isLoading = it },
                                    onErrorChange = { errorMessage = it },
                                    onLoginSuccess = onLoginSuccess
                                )
                            }
                        }
                    )
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (!isLoading && username.isNotBlank() && password.isNotBlank() &&
                            (selectedServer != null || (serverIP.isNotBlank() && serverPort.isNotBlank()))) {
                            attemptLogin(
                                username, password,
                                selectedServer?.ipAddress ?: serverIP,
                                selectedServer?.port ?: serverPort,
                                onLoadingChange = { isLoading = it },
                                onErrorChange = { errorMessage = it },
                                onLoginSuccess = onLoginSuccess
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank() &&
                            (selectedServer != null || (serverIP.isNotBlank() && serverPort.isNotBlank()))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Login")
                    }
                }
            }
        }
    }

    // Server selection dialog
    if (showServerList) {
        AlertDialog(
            onDismissRequest = { showServerList = false },
            title = { Text("Available Servers") },
            text = {
                LazyColumn {
                    if (serverList.isEmpty()) {
                        item {
                            Text(
                                "No servers found. Please make sure the server is running and broadcasting.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(serverList) { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedServer = server
                                        showServerList = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = server.toString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (serverList.indexOf(server) < serverList.size - 1) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showServerList = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun attemptLogin(
    username: String,
    password: String,
    serverIP: String,
    serverPort: String,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoginSuccess: () -> Unit
) {
    onLoadingChange(true)
    onErrorChange(null)

    // Run login attempt in a background thread to avoid blocking UI
    Thread {
        try {
            val success = ClientSocket.init(username, password, serverIP, serverPort)

            // Return to main thread to update UI
            Handler(Looper.getMainLooper()).post {
                onLoadingChange(false)
                if (success) {
                    onLoginSuccess()
                } else {
                    onErrorChange("Login failed. Please check your credentials and server details.")
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onLoadingChange(false)
                onErrorChange("Error: ${e.message}")
            }
        }
    }.start()
}