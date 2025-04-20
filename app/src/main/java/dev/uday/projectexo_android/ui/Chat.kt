package dev.uday.projectexo_android.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import dev.uday.projectexo_android.net.handlers.ImageHandler
import dev.uday.projectexo_android.net.handlers.MsgHandler
import dev.uday.projectexo_android.utils.NotificationHelper
import dev.uday.projectexo_android.utils.SoundManager
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

object Chat {
    data class ChatMessage(
        val sender: String,
        val content: String = "",
        val isPrivate: Boolean,
        val isOutgoing: Boolean = false,
        val timestamp: Long = System.currentTimeMillis(),
        val imageData: ByteArray? = null // Add this for image support
    )

    // Store messages by chat channel (user or "general")
    val messages = mutableStateMapOf<String, List<ChatMessage>>()
    private val onlineUsers = mutableStateListOf<String>()
    val unreadCounts = mutableStateMapOf<String, Int>()

    @SuppressLint("PrivateApi")
    fun receiveMessage(sender: String, message: String, isPrivate: Boolean) {
        // Get application context
        val context = getApplicationContext()

        // Play sound if the app is in foreground
        context?.let {
            SoundManager.getInstance(it).playMessageReceived()

            // Show notification if not on the same chat
            val chatKey = if (isPrivate) sender else "general"
            if (currentSelectedChat.value != chatKey || !isAppInForeground(it)) {
                NotificationHelper.showMessageNotification(it, sender, message, isPrivate)
            }
        }

        // Existing message handling code
        val chatKey = if (isPrivate) sender else "general"
        val currentMessages = messages.getOrDefault(chatKey, emptyList())
        messages[chatKey] = currentMessages + ChatMessage(
            sender = sender,
            content = message,
            isPrivate = isPrivate,
            isOutgoing = false,
            timestamp = System.currentTimeMillis()
        )

        // Increment unread message count if not the current chat
        if (currentSelectedChat.value != chatKey) {
            unreadCounts[chatKey] = unreadCounts.getOrDefault(chatKey, 0) + 1
        }
    }

    // Check if app is in foreground
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    // Add this to track the current selected chat
    val currentSelectedChat = mutableStateOf("general")

    // Add method to mark messages as read
    fun markAsRead(chatKey: String) {
        unreadCounts[chatKey] = 0
    }

    fun updateOnlineUsers(users: List<String>) {
        onlineUsers.clear()
        // Add AI to the online users list
        onlineUsers.add("AI")
        onlineUsers.addAll(users)
        onlineUsers.remove("")
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
        val context = LocalContext.current

        // Image picker launcher
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val imageBytes = getImageBytes(context, it)
                imageBytes?.let { bytes ->
                    ImageHandler.sendImage(bytes, selectedChat.value)
                }
            }
        }

        // Current messages - extract outside composable functions for better performance
        val currentMessages = messages[selectedChat.value] ?: emptyList()

        LaunchedEffect(currentMessages.size, selectedChat.value) {
            if (currentMessages.isNotEmpty()) {
                listState.animateScrollToItem(
                    index = 0,
                    scrollOffset = 0,
                )
                markAsRead(selectedChat.value)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
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
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("General")
                                    // Show badge if there are unread messages
                                    val unreadCount = unreadCounts["general"] ?: 0
                                    if (unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                color = MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    }
                                }
                            },
                            selected = selectedChat.value == "general",
                            onClick = {
                                selectedChat.value = "general"
                                markAsRead("general")
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
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(user)
                                                // Show badge if there are unread messages
                                                val unreadCount = unreadCounts[user] ?: 0
                                                if (unreadCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.error
                                                    ) {
                                                        Text(
                                                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                            color = MaterialTheme.colorScheme.onError
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        selected = selectedChat.value == user,
                                        onClick = {
                                            selectedChat.value = user
                                            if (!messages.containsKey(user)) {
                                                messages[user] = emptyList()
                                            }
                                            markAsRead(user)
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
                            Text(
                                text = "Logout",
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFB00020),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable(onClick = onLogout),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Image attachment button
                            IconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Attach Image",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        state = listState,
                        reverseLayout = true, // Ensures the newest messages are at the bottom
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            bottom = 8.dp, // Add padding for keyboard
                            top = 8.dp
                        )
                    ) {
                        itemsIndexed(
                            items = currentMessages.reversed(), // Reverse the list to show the newest messages at the bottom
                            key = { index, message -> "${message.timestamp}-${message.sender}-${index}" }
                        ) { _, message ->
                            val messageKey =
                                remember(message.timestamp, message.content, message.imageData) {
                                    "${message.timestamp}-${message.content}-${message.imageData?.size}"
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

    // Helper function to convert Uri to ByteArray
    private fun getImageBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            // Compress with quality 80 to reduce size
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Composable
    private fun MessageBubble(message: ChatMessage) {

        val context = LocalContext.current
        var showSaveDialog by remember { mutableStateOf(false) }
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
                    if (message.imageData != null) {
                        // Display image with long press to save
                        Box {
                            Image(
                                bitmap = BitmapFactory.decodeByteArray(
                                    message.imageData, 0, message.imageData.size
                                ).asImageBitmap(),
                                contentDescription = "Image Message",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = { showSaveDialog = true })
                            )

                            if (showSaveDialog) {
                                SaveImageDialog(
                                    onDismiss = { showSaveDialog = false },
                                    onSave = {
                                        val timestamp = System.currentTimeMillis()
                                        val fileName = "EXO_${message.sender}_${timestamp}.jpg"
                                        val result =
                                            saveImageToDCIM(context, message.imageData, fileName)

                                        // Show toast based on result
                                        val toastMessage = if (result) {
                                            "Image saved to DCIM/EXO/$fileName"
                                        } else {
                                            "Failed to save image"
                                        }
                                        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT)
                                            .show()
                                        showSaveDialog = false
                                    }
                                )
                            }
                        }
                    } else {
                        // Display text

                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )

                    }
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

    @Composable
    private fun SaveImageDialog(onDismiss: () -> Unit, onSave: () -> Unit) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Save Image") },
            text = { Text("Save this image to DCIM/EXO?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onSave) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    fun sendMsg(messageInput: String, selectedChat: MutableState<String>) {
        if (selectedChat.value != "AI") {
            if (messageInput.isNotBlank()) {
                MsgHandler.sendMessage(messageInput, selectedChat.value)

                // Get context and play sound
                getApplicationContext()?.let { context ->
                    SoundManager.getInstance(context).playMessageSent()
                }

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

            // Get context and play sound
            getApplicationContext()?.let { context ->
                SoundManager.getInstance(context).playMessageSent()
            }
        }
    }

    // Helper method to get application context
    @SuppressLint("PrivateApi")
    private fun getApplicationContext(): Context? {
        return try {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Context
        } catch (e: Exception) {
            null
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

    private fun parseMarkdownToAnnotatedString(
        markdown: String,
        defaultColor: Color
    ): AnnotatedString {
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
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = Color.DarkGray.copy(alpha = 0.2f)
                                )
                            ) {
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

    private fun saveImageToDCIM(
        context: Context,
        imageBytes: ByteArray,
        imageName: String
    ): Boolean {
        return try {
            // Use MediaStore for Android 10 and above
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/EXO")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(imageBytes)
                }
                true
            } == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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