package dev.uday.projectexo_android.net.handlers

import dev.uday.projectexo_android.ui.Chat
import dev.uday.projectexo_android.net.ClientSocket

class PacketHandler {
    companion object {
        fun handlePacket(decryptedBytes: ByteArray) {
            val packetType = decryptedBytes[0]
            val packetData = decryptedBytes.copyOfRange(1, decryptedBytes.size)
            println("Received packet (type: $packetType, size: ${packetData.size} bytes)")
            when (packetType) {
                0.toByte() -> handleBroadcastUserList(packetData)
                1.toByte() -> MsgHandler.handleMsgPacket(packetData)
                3.toByte() -> ImageHandler.handleImagePacket(packetData)
                9.toByte() -> AIPackets.handleAIPacket(packetData)
                else -> println("Unknown packet type: $packetType")
            }
        }

        private fun handleBroadcastUserList(packetData: ByteArray) {
            val userList = String(packetData)
            //split the user list by comma
            val users = userList.split(",")

            ClientSocket.onlineUsers = users.toMutableList()
            Chat.updateOnlineUsers(users)
            println(ClientSocket.onlineUsers)
        }
    }
}
