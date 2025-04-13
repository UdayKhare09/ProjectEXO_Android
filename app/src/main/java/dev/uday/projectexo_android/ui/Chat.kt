package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(onLogout: () -> Unit, onBack: () -> Unit) {
        var messageInput by remember { mutableStateOf("") }
        val selectedChat = remember { mutableStateOf("general") }
        val listState = rememberLazyListState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Auto-scroll to bottom when new messages arrive
        val currentMessages = messages[selectedChat.value] ?: emptyList()
        LaunchedEffect(currentMessages.size) {
            if (currentMessages.isNotEmpty()) {
                listState.animateScrollToItem(currentMessages.size - 1)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
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
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Button(onClick = onLogout) {
                                Text(
                                    text = "Logout",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
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
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Message"
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(currentMessages) { message ->
                        MessageBubble(message)
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
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