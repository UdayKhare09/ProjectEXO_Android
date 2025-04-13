package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.uday.projectexo_android.net.ClientSocket
import dev.uday.projectexo_android.net.handlers.MsgHandler

object Chat {
    // Chat message data class
    // Modify the ChatMessage data class to support images
    data class ChatMessage(
        val sender: String,
        val content: String,
        val isPrivate: Boolean,
        val isOutgoing: Boolean = false,
    )

    // Store messages by chat channel (user or "general")
    val messages = mutableStateMapOf<String, List<ChatMessage>>()
    private val onlineUsers = mutableStateListOf<String>()

    // Currently selected chat (default is "general")
    private val currentChat = mutableStateOf("general")

    fun receiveMessage(sender: String, message: String, isPrivate: Boolean) {
        val chatKey = if (isPrivate) sender else "general"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        messages[chatKey] = currentMessages + ChatMessage(sender, message, isPrivate, false)
    }


    fun updateOnlineUsers(users: List<String>) {
        onlineUsers.clear()
        // Add AI to the online users list
        onlineUsers.add("AI")
        onlineUsers.addAll(users)
        // Ensure general channel exists
        if (!messages.containsKey("general")) {
            messages["general"] = emptyList()
        }
    }

    @Composable
    fun ChatScreen(onLogout: () -> Unit, onBack: () -> Unit) {
        var messageInput by remember { mutableStateOf("") }
        val selectedChat = remember { currentChat }
        val chatMessages = messages[selectedChat.value] ?: emptyList()

        val listState = rememberLazyListState()

        LaunchedEffect(chatMessages.size) {
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }

        // File chooser dialog

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header remains unchanged...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Back")
                }

                // Title
                Text(
                    text = "Secure Chat",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Logout button
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Logout")
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // Online users panel remains unchanged...
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // General chat option
                    ChatListItem(
                        name = "general",
                        isSelected = selectedChat.value == "general",
                        onClick = { selectedChat.value = "general" }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Online Users",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn {
                        // Display online users except the current user
                        items(onlineUsers.filter { it != ClientSocket.username }) { user ->
                            ChatListItem(
                                name = user,
                                isSelected = selectedChat.value == user,
                                onClick = {
                                    selectedChat.value = user
                                    // Load chat history for the selected user
                                    if (!messages.containsKey(user)) {
                                        messages[user] = emptyList()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // ui.Chat panel
                Column(
                    modifier = Modifier.weight(3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    // ui.Chat header and messages remain unchanged...
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedChat.value == "general") "General Chat" else "Chat with ${selectedChat.value}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ui.Chat messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(chatMessages) { message ->
                            MessageBubble(message)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Modified input field area with attachment button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))

                        // Text input field
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    sendMsg(messageInput, selectedChat)
                                    messageInput = "" // Clear input after sending
                                }
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Send button
                        Button(
                            onClick = {
                                sendMsg(messageInput, selectedChat)
                                messageInput = "" // Clear input after sending
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }

    private fun sendMsg(messageInput: String, selectedChat: MutableState<String>) {
        if (selectedChat.value != "AI") {
            if (messageInput.isNotBlank()) {
                MsgHandler.sendMessage(messageInput, selectedChat.value)

                // Add outgoing message to local state
                val chatKey = selectedChat.value
                val currentMessages = messages.getOrDefault(chatKey, emptyList())
                val newMessage = ChatMessage(
                    sender = "Me",
                    content = messageInput,
                    isPrivate = chatKey != "general",
                    isOutgoing = true
                )
                messages[chatKey] = currentMessages + newMessage
            }
        } else {
            sendAIMessage(messageInput)
        }
    }

    private fun sendAIMessage(messageInput: String) {
        val packetType = 9.toByte()
        val msgType = 1.toByte()

        val messageBytes = messageInput.toByteArray()
        val packet = ByteArray(2 + messageBytes.size)
        packet[0] = packetType
        packet[1] = msgType
        System.arraycopy(messageBytes, 0, packet, 2, messageBytes.size)
        ClientSocket.sendPacket(packet)

        // Add outgoing message to local state
        val chatKey = "AI"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        val newMessage = ChatMessage(
            sender = "Me",
            content = messageInput,
            isPrivate = true,
            isOutgoing = true
        )
        messages[chatKey] = currentMessages + newMessage
    }


    @Composable
    private fun ChatListItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    private fun MessageBubble(message: ChatMessage) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (message.isOutgoing)
                            MaterialTheme.colorScheme.primary
                        else if (message.isPrivate)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                    .padding(12.dp)
            ) {
                if (!message.isOutgoing) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isPrivate)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }


                    if (message.sender == "AI") {
                        MarkdownText(
                            content = message.content,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isOutgoing)
                                MaterialTheme.colorScheme.onPrimary
                            else if (message.isPrivate)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

            }
        }
    }

    @Composable
    private fun MarkdownText(content: String, color: Color) {
        val parsedMarkdown = remember(content) {
            parseMarkdownToAnnotatedString(content, color)
        }

        Text(
            text = parsedMarkdown,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    private fun parseMarkdownToAnnotatedString(markdown: String, defaultColor: Color): AnnotatedString {
        // A simple markdown parser for basic formatting
        return buildAnnotatedString {
            withStyle(SpanStyle(color = defaultColor)) {
                // Process lines to handle different markdown elements
                val lines = markdown.split("\n")

                lines.forEachIndexed { index, line ->
                    // Headers
                    if (line.startsWith("# ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                            append(line.substring(2))
                        }
                    } else if (line.startsWith("## ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append(line.substring(3))
                        }
                    } else if (line.startsWith("### ")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            append(line.substring(4))
                        }
                    }
                    // Code block
                    else if (line.startsWith("```")) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.DarkGray.copy(alpha = 0.2f)
                            )
                        ) {
                            append(line.substring(3))
                        }
                    }
                    // Bold
                    else if (line.contains("**")) {
                        val parts = line.split("**")
                        for (i in parts.indices) {
                            if (i % 2 == 1) { // Odd indices are inside ** markers
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(parts[i])
                                }
                            } else {
                                append(parts[i])
                            }
                        }
                    }
                    // Italic
                    else if (line.contains("*")) {
                        val parts = line.split("*")
                        for (i in parts.indices) {
                            if (i % 2 == 1) { // Odd indices are inside * markers
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(parts[i])
                                }
                            } else {
                                append(parts[i])
                            }
                        }
                    }
                    // Bullet points
                    else if (line.startsWith("- ")) {
                        append("• ${line.substring(2)}")
                    }
                    // Regular text
                    else {
                        append(line)
                    }

                    // Add newline if not the last line
                    if (index < lines.size - 1) {
                        append("\n")
                    }
                }
            }
        }
    }
}