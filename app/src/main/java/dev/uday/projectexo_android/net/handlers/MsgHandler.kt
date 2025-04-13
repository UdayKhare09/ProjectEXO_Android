package dev.uday.projectexo_android.net.handlers

import ui.Chat
import dev.uday.projectexo_android.net.ClientSocket

class MsgHandler {
    companion object {
        fun handleMsgPacket(decryptedBytes: ByteArray) {
            val packetType = decryptedBytes[0]
            val packetData = decryptedBytes.copyOfRange(1, decryptedBytes.size)

            when (packetType) {
                1.toByte() -> handlePrivateMessage(packetData)
                0.toByte() -> handleGeneralMessage(packetData)
                else -> println("Unknown packet type: $packetType")
            }
        }

        private fun handlePrivateMessage(packetData: ByteArray) {
            val fullMessage = String(packetData).split("x1W1x")
            val sender = fullMessage[0]
            val message = fullMessage[1]
            println("Private message from $sender: $message")

            // Add to ui.Chat UI
            Chat.receiveMessage(sender, message, true)
        }

        private fun handleGeneralMessage(packetData: ByteArray) {
            val fullMessage = String(packetData).split("x1W1x")
            val sender = fullMessage[0]
            val message = fullMessage[1]
            println("General message from $sender: $message")

            // Add to ui.Chat UI
            if (sender != ClientSocket.username) {
                Chat.receiveMessage(sender, message, false)
            } else {
                // Ignore messages sent by the user themselves
                return
            }
        }

        fun sendMessage(message: String, recipient: String) {
            val packetType = 1.toByte()
            if (recipient.equals("general", ignoreCase = true)) {
                val msgType = 0.toByte()
                val messageBytes = message.toByteArray()
                val packet = ByteArray(2 + messageBytes.size)
                packet[0] = packetType
                packet[1] = msgType
                System.arraycopy(messageBytes, 0, packet, 2, messageBytes.size)
                ClientSocket.sendPacket(packet)
            } else {
                sendPrivateMessage(message, recipient)
            }
        }

        private fun sendPrivateMessage(message: String, recipient: String) {
            val packetType = 1.toByte()
            val msgType = 1.toByte()
            val msgContent = recipient+ "x1W1x" + message
            val msgContentBytes = msgContent.toByteArray()
            val packet = ByteArray(2 + msgContent.length)
            packet[0] = packetType
            packet[1] = msgType
            System.arraycopy(msgContentBytes, 0, packet, 2, msgContentBytes.size)
            ClientSocket.sendPacket(packet)
        }
    }
}