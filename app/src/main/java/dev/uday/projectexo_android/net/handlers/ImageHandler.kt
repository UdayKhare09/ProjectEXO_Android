package dev.uday.projectexo_android.net.handlers

import dev.uday.projectexo_android.net.ClientSocket
import dev.uday.projectexo_android.ui.Chat

class ImageHandler {
    companion object {
        fun handleImagePacket(packetData: ByteArray) {
            val packetType = packetData[0]
            val imageData = packetData.copyOfRange(1, packetData.size)
            println("Received image packet (type: $packetType, size: ${imageData.size} bytes)")

            when (packetType) {
                0.toByte() -> handleGeneralImage(imageData)
                1.toByte() -> handlePrivateImage(imageData)
                else -> println("Unknown image packet type: $packetType")
            }
        }

        private fun handlePrivateImage(imageData: ByteArray) {
            val sender = extractSender(imageData)
            val imageBytes = imageData.copyOfRange(30, imageData.size)

            // Add the image message directly to the messages map
            Chat.messages[sender] = Chat.messages[sender].orEmpty() + Chat.ChatMessage(
                sender = sender,
                isPrivate = true,
                imageData = imageBytes
            )
        }

        private fun handleGeneralImage(imageData: ByteArray) {
            val sender = extractSender(imageData)
            val imageBytes = imageData.copyOfRange(30, imageData.size)

            // Add the image message directly to the messages map
            Chat.messages["general"] = Chat.messages["general"].orEmpty() + Chat.ChatMessage(
                sender = sender,
                isPrivate = false,
                imageData = imageBytes
            )
        }

        private fun extractSender(imageData: ByteArray): String {
            // Find the position of the first null byte or take all 30 bytes
            val nullTerminatorPos = imageData.take(30).indexOfFirst { it == 0.toByte() }
            val senderLength = if (nullTerminatorPos >= 0) nullTerminatorPos else 30

            // Extract just the relevant bytes for the username
            return String(imageData, 0, senderLength).trim()
        }

        /**
         * Sends an image to either a specific recipient or to general chat
         *
         * @param imageData The raw image bytes to send
         * @param recipient The recipient username or "general" for general chat
         */
        fun sendImage(imageData: ByteArray, recipient: String) {
            if ("general" == recipient) {
                println("Sending general image")
                val packetData = ByteArray(imageData.size + 2)
                packetData[0] = 3.toByte() // PacketType for image
                packetData[1] = 0.toByte() // General message type
                System.arraycopy(imageData, 0, packetData, 2, imageData.size)
                ClientSocket.sendPacket(packetData)

                // Add outgoing image to local state
                val currentMessages = Chat.messages.getOrDefault("general", emptyList())
                val newMessage = Chat.ChatMessage(
                    sender = "Me",
                    isPrivate = false,
                    isOutgoing = true,
                    imageData = imageData,
                    timestamp = System.currentTimeMillis()
                )
                Chat.messages["general"] = currentMessages + newMessage

            } else {
                println("Sending private image to $recipient")
                val packetData = ByteArray(imageData.size + 32)
                packetData[0] = 3.toByte() // PacketType for image
                packetData[1] = 1.toByte() // Private message type

                // Add recipient name to header (padded to 30 bytes)
                val usernameBytes = recipient.toByteArray()
                System.arraycopy(usernameBytes, 0, packetData, 2, usernameBytes.size.coerceAtMost(30))

                // Null-padding for username field
                for (i in (usernameBytes.size + 2).coerceAtMost(32) until 32) {
                    packetData[i] = 0
                }

                System.arraycopy(imageData, 0, packetData, 32, imageData.size)
                ClientSocket.sendPacket(packetData)

                // Add outgoing image to local state
                val currentMessages = Chat.messages.getOrDefault(recipient, emptyList())
                val newMessage = Chat.ChatMessage(
                    sender = "Me",
                    isPrivate = true,
                    isOutgoing = true,
                    imageData = imageData,
                    timestamp = System.currentTimeMillis()
                )
                Chat.messages[recipient] = currentMessages + newMessage
            }
        }
    }
}