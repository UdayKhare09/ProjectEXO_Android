package dev.uday.projectexo_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.uday.projectexo_android.net.ClientSocket
import dev.uday.projectexo_android.net.handlers.MsgHandler
import kotlinx.coroutines.launch

object Chat {
    // Chat message data class
    // Modify the ChatMessage data class to support images
    // First, update the ChatMessage data class to include timestamp
    data class ChatMessage(
        val sender: String,
        val content: String,
        val isPrivate: Boolean,
        val isOutgoing: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Store messages by chat channel (user or "general")
    val messages = mutableStateMapOf<String, List<ChatMessage>>()
    private val onlineUsers = mutableStateListOf<String>()

    fun receiveMessage(sender: String, message: String, isPrivate: Boolean) {
        val chatKey = if (isPrivate) sender else "general"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        messages[chatKey] = currentMessages + ChatMessage(
            sender = sender,
            content = message,
            isPrivate = isPrivate,
            isOutgoing = false,
            timestamp = System.currentTimeMillis()
        )
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(onLogout: () -> Unit, onBack: () -> Unit) {
        var messageInput by remember { mutableStateOf("") }
        val selectedChat = remember { mutableStateOf("general") }
        val listState = rememberLazyListState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Current messages - extract outside composable functions for better performance
        val currentMessages = messages[selectedChat.value] ?: emptyList()

        // Only scroll to bottom on new message or chat change
        LaunchedEffect(currentMessages.size, selectedChat.value) {
            if (currentMessages.isNotEmpty()) {
                // Use animateScrollToItem with custom duration for smoother scrolling
                listState.animateScrollToItem(
                    index = 0,
                    scrollOffset = 0
                )
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                // Rest of drawer content remains the same
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Chats",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                        NavigationDrawerItem(
                            label = { Text("General") },
                            selected = selectedChat.value == "general",
                            onClick = {
                                selectedChat.value = "general"
                                scope.launch { drawerState.close() }
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Online Users",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyColumn {
                            items(onlineUsers) { user ->
                                if (user != ClientSocket.username) {
                                    NavigationDrawerItem(
                                        label = { Text(user) },
                                        selected = selectedChat.value == user,
                                        onClick = {
                                            selectedChat.value = user
                                            if (!messages.containsKey(user)) {
                                                messages[user] = emptyList()
                                            }
                                            scope.launch { drawerState.close() }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (selectedChat.value == "general") "Group Chat"
                                else selectedChat.value
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB00020),
                                )
                            ){
                                Text(
                                    text = "Logout",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message") },
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            FloatingActionButton(
                                onClick = {
                                    if (messageInput.isNotBlank()) {
                                        sendMsg(messageInput, selectedChat)
                                        messageInput = ""
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message"
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Optimized LazyColumn
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true,
                        contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
                    ) {
                        itemsIndexed(
                            items = currentMessages.asReversed(),
                            // Use stable keys for better performance
                            key = { index, message -> "${message.timestamp}-${message.sender}-${index}" }
                        ) { _, message ->
                            // Use key to prevent unnecessary recompositions
                            val messageKey = remember(message.timestamp, message.content) {
                                "${message.timestamp}-${message.content}"
                            }

                            key(messageKey) {
                                MessageBubble(message = message)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MessageBubble(message: ChatMessage) {
        // Colors and shapes calculated once and remembered
        val bubbleShape = remember(message.isOutgoing) {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
            )
        }

        val backgroundColor = when {
            message.isOutgoing -> MaterialTheme.colorScheme.primary
            message.isPrivate -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }

        val textColor = when {
            message.isOutgoing -> MaterialTheme.colorScheme.onPrimary
            message.isPrivate -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }

        val timestampColor = when {
            message.isOutgoing -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            message.isPrivate -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        }

        // Format timestamp once
        val formattedTime = remember(message.timestamp) {
            formatTimestamp(message.timestamp)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                if (!message.isOutgoing) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.sender == "AI") {
                    val parsedMarkdown = remember(message.content) {
                        parseMarkdownToAnnotatedString(message.content, textColor)
                    }

                    Text(
                        text = parsedMarkdown,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                // Add timestamp
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End),
                    color = timestampColor
                )
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
                    isOutgoing = true,
                    timestamp = System.currentTimeMillis()
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
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )
        messages[chatKey] = currentMessages + newMessage
    }

    // Helper function to format timestamp
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun parseMarkdownToAnnotatedString(markdown: String, defaultColor: Color): AnnotatedString {
        // Cache results for identical inputs to avoid redundant parsing
        return buildAnnotatedString {
            withStyle(SpanStyle(color = defaultColor)) {
                val lines = markdown.split('\n')

                lines.forEachIndexed { index, line ->
                    when {
                        line.startsWith("# ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                                append(line.substring(2))
                            }
                        }
                        line.startsWith("## ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                                append(line.substring(3))
                            }
                        }
                        line.startsWith("### ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                                append(line.substring(4))
                            }
                        }
                        line.startsWith("```") -> {
                            withStyle(SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.DarkGray.copy(alpha = 0.2f)
                            )) {
                                append(line.substring(3))
                            }
                        }
                        else -> append(parseLine(line))
                    }

                    if (index < lines.size - 1) {
                        append('\n')
                    }
                }
            }
        }
    }

    private fun parseLine(line: String): AnnotatedString {
        return buildAnnotatedString {
            when {
                line.contains("**") -> {
                    val parts = line.split("**")
                    for (i in parts.indices) {
                        if (i % 2 == 1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(parts[i])
                            }
                        } else {
                            append(parts[i])
                        }
                    }
                }
                line.contains("*") -> {
                    val parts = line.split("*")
                    for (i in parts.indices) {
                        if (i % 2 == 1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(parts[i])
                            }
                        } else {
                            append(parts[i])
                        }
                    }
                }
                line.startsWith("- ") -> append("• ${line.substring(2)}")
                else -> append(line)
            }
        }
    }
}

@Composable
@Preview
fun ChatPreview() {
    MaterialTheme {
        Chat.ChatScreen(onLogout = {}, onBack = {})
    }
}