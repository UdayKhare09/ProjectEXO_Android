package dev.uday.projectexo_android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient {
    companion object {
        private const val TAG = "SocketClient"
        private const val SOCKET_TIMEOUT = 5000 // 5 seconds
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)
            writer = PrintWriter(socket?.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
            Log.d(TAG, "Connected to $host:$port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            close()
            false
        }
    }

    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            writer?.println(message)
            Log.d(TAG, "Message sent: $message")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            false
        }
    }

    suspend fun receiveMessage(): String? = withContext(Dispatchers.IO) {
        try {
            val message = reader?.readLine()
            Log.d(TAG, "Message received: $message")
            message
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive message: ${e.message}")
            null
        }
    }

    fun close() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
            Log.d(TAG, "Socket connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        } finally {
            socket = null
            writer = null
            reader = null
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true && !socket?.isClosed!!
}