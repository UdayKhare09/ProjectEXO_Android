package dev.uday.projectexo_android.net

import dev.uday.projectexo_android.net.handlers.PacketHandler
import java.io.*
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ClientSocket {
    companion object {
        private var socket: Socket? = null
        private var dataInputStream: DataInputStream? = null
        private var dataOutputStream: DataOutputStream? = null
        private var serverPublicKey: PublicKey? = null
        private var keyPair: KeyPair? = null
        private var cipher: Cipher? = null

        var username: String = ""
        private var password: String = ""
        var onlineUsers = mutableListOf<String>()

        @Volatile
        private var isConnected = false

        fun init(username: String, password: String, host: String, port: String): Boolean {
            this.username = username
            this.password = password

            try {
                println("ClientSocket initializing...")
                // Create socket with timeout
                socket = Socket(host, port.toInt())
                socket?.soTimeout = 5000 // Initial timeout for setup operations

                dataInputStream = DataInputStream(BufferedInputStream(socket?.getInputStream()))
                dataOutputStream = DataOutputStream(BufferedOutputStream(socket?.getOutputStream()))

                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

                println("ClientSocket initialized with host: $host and port: $port")

                // Exchange keys first
                if (!exchangeKeys()) {
                    println("Key exchange failed")
                    disconnect()
                    return false
                }

                // Then authenticate
                if (!auth()) {
                    println("Authentication failed")
                    disconnect()
                    return false
                }

                isConnected = true

                // For normal operation, use longer timeout or none
                socket?.soTimeout = 0

                // Start packet receiver in a new thread
                Thread {
                    startReceivingPacket()
                }.start()

                return true
            } catch (e: Exception) {
                println("Error during initialization: ${e.message}")
                e.printStackTrace()
                disconnect()
                return false
            }
        }

        private fun exchangeKeys(): Boolean {
            try {
                // Generate key pair
                val keyGen = KeyPairGenerator.getInstance("RSA")
                keyGen.initialize(2048)
                keyPair = keyGen.generateKeyPair()

                // Receive server's public key
                val serverKeyLength = dataInputStream?.readInt() ?: return false
                val serverKeyBytes = ByteArray(serverKeyLength)
                dataInputStream?.readFully(serverKeyBytes)
                println("Size of public key: $serverKeyLength")

                val keyFactory = KeyFactory.getInstance("RSA")
                serverPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(serverKeyBytes))
                println("Server public key received")

                // Send client's public key
                val clientPublicKeyBytes = keyPair?.public?.encoded ?: return false
                dataOutputStream?.writeInt(clientPublicKeyBytes.size)
                dataOutputStream?.write(clientPublicKeyBytes)
                dataOutputStream?.flush()

                return true
            } catch (e: Exception) {
                println("Error during key exchange: ${e.message}")
                e.printStackTrace()
                return false
            }
        }

        private fun auth(): Boolean {
            try {
                // Encrypt and send username
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val encryptedUsername = cipher?.doFinal(username.toByteArray()) ?: return false
                dataOutputStream?.writeInt(encryptedUsername.size)
                dataOutputStream?.write(encryptedUsername)

                // Encrypt and send password
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val encryptedPassword = cipher?.doFinal(password.toByteArray()) ?: return false
                dataOutputStream?.writeInt(encryptedPassword.size)
                dataOutputStream?.write(encryptedPassword)
                dataOutputStream?.flush()

                // Set a specific timeout for auth response
                socket?.soTimeout = 5000

                // Get response
                val response = dataInputStream?.readInt() ?: -1

                println("Error during authentication: $response")

                return when (response) {
                    1 -> true // Success
                    0 -> false // User not registered
                    2 -> false // Wrong password
                    3 -> false // Already logged in
                    else -> false
                }
            } catch (e: Exception) {
                println("Error during authentication: ${e.message}")
                e.printStackTrace()
                return false
            }
        }

        fun sendPacket(packet: ByteArray) {
            if (!isConnected) {
                println("Cannot send packet: Not connected")
                return
            }

            try {
                // Based on server implementation, we need to encrypt and chunk data
                val chunkSize = 240 // Server expects this size for RSA-2048
                val totalChunks = Math.ceil(packet.size.toDouble() / chunkSize).toInt()

                // Prepare and send header
                cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val header = "SIZE:${packet.size};CHUNKS:$totalChunks".toByteArray()
                val encryptedHeader = cipher?.doFinal(header) ?: return

                synchronized(dataOutputStream!!) {
                    dataOutputStream?.writeInt(encryptedHeader.size)
                    dataOutputStream?.write(encryptedHeader)
                    dataOutputStream?.flush()

                    // Send chunks
                    for (i in 0 until totalChunks) {
                        val start = i * chunkSize
                        val end = Math.min(packet.size, start + chunkSize)
                        val chunk = ByteArray(end - start)
                        System.arraycopy(packet, start, chunk, 0, end - start)

                        cipher?.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                        val encryptedChunk = cipher?.doFinal(chunk) ?: continue

                        dataOutputStream?.writeInt(encryptedChunk.size)
                        dataOutputStream?.write(encryptedChunk)
                        dataOutputStream?.flush()
                    }
                }
            } catch (e: Exception) {
                println("Error sending packet: ${e.message}")
                e.printStackTrace()

                // Check if connection is lost
                if (e is IOException) {
                    disconnect()
                }
            }
        }

        private fun startReceivingPacket() {
            println("Listening for packets")
            try {
                while (isConnected) {
                    try {
                        // Read the header chunk
                        val headerSize = dataInputStream?.readInt() ?: break
                        val encryptedHeader = ByteArray(headerSize)
                        dataInputStream?.readFully(encryptedHeader)

                        cipher?.init(Cipher.DECRYPT_MODE, keyPair?.private)
                        val headerBytes = cipher?.doFinal(encryptedHeader) ?: continue
                        val headerString = String(headerBytes)

                        // Parse header
                        val totalSize = headerString.split(";")[0].split(":")[1].toInt()
                        val totalChunks = headerString.split(";")[1].split(":")[1].toInt()

                        // Read all chunks
                        val completePacket = ByteArrayOutputStream()
                        for (i in 0 until totalChunks) {
                            val chunkSize = dataInputStream?.readInt() ?: break
                            val encryptedChunk = ByteArray(chunkSize)
                            dataInputStream?.readFully(encryptedChunk)

                            cipher?.init(Cipher.DECRYPT_MODE, keyPair?.private)
                            val decryptedChunk = cipher?.doFinal(encryptedChunk) ?: continue
                            completePacket.write(decryptedChunk)
                        }

                        val decryptedBytes = completePacket.toByteArray()
                        // Process the packet
                        PacketHandler.handlePacket(decryptedBytes)
                    } catch (e: EOFException) {
                        println("Connection closed by server")
                        break
                    } catch (e: Exception) {
                        println("Error receiving packet: ${e.message}")
                        e.printStackTrace()

                        if (e is IOException) {
                            break
                        }
                    }
                }
            } finally {
                disconnect()
            }
        }

        fun disconnect() {
            try {
                isConnected = false
                dataInputStream?.close()
                dataOutputStream?.close()
                socket?.close()
                println("Disconnected from server")
            } catch (e: Exception) {
                println("Error during disconnect: ${e.message}")
            }
        }
    }
}