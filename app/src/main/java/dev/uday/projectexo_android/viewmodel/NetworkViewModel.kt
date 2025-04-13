package dev.uday.projectexo_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.uday.projectexo_android.network.SocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkViewModel : ViewModel() {
    private val socketClient = SocketClient()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    fun connectToSocket(host: String, port: Int) {
        _connectionState.value = ConnectionState.Connecting
        viewModelScope.launch {
            val isConnected = socketClient.connect(host, port)
            _connectionState.value = if (isConnected) {
                startListening()
                ConnectionState.Connected
            } else {
                ConnectionState.Error("Failed to connect")
            }
        }
    }

    fun disconnect() {
        socketClient.close()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            if (socketClient.isConnected()) {
                val success = socketClient.sendMessage(message)
                if (!success) {
                    _connectionState.value = ConnectionState.Error("Failed to send message")
                }
            } else {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private fun startListening() {
        viewModelScope.launch {
            while (socketClient.isConnected()) {
                val message = socketClient.receiveMessage() ?: break
                _messages.value = _messages.value + message
            }

            // If we exit the loop, we're disconnected
            if (socketClient.isConnected()) {
                socketClient.close()
            }
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketClient.close()
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}