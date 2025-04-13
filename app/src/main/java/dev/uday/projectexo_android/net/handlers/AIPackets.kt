package dev.uday.projectexo_android.net.handlers

import ui.Chat

class AIPackets {
    companion object{
        fun handleAIPacket(packetData: ByteArray) {
            val packetType = packetData[0]
            val packetContent = packetData.copyOfRange(1, packetData.size)
            println("Received AI packet (type: $packetType, size: ${packetContent.size} bytes)")

            when (packetType) {
                1.toByte() -> handleAIMessage(packetContent)
                else -> println("Unknown AI packet type: $packetType")
            }
        }

        private fun handleAIMessage(packetContent: ByteArray) {
            Chat.receiveMessage("AI", String(packetContent), true)
        }
    }
}