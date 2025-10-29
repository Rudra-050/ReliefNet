package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.poppinsFontFamily
import com.sentrive.reliefnet.viewmodel.ChatViewModel
import com.sentrive.reliefnet.viewmodel.ChatUiState

// Data class for chat messages (kept for UI compatibility)
data class ChatMessageUI(
    val text: String,
    val isMine: Boolean,
    val time: String,
    val isVoiceMessage: Boolean = false
)

@Composable
fun PatientChatScreen(
    navController: NavHostController? = null,
    patientId: String = "687910a4748f95606960a4ca", // Default for testing
    doctorId: String = "68f4827e3174500e31a5a00f", // Default for testing
    doctorName: String = "Dr. Rahul Verma",
    viewModel: ChatViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val conversationId = remember { "${patientId}:${doctorId}" }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var messageText by remember { mutableStateOf("") }

    // Connect to socket when screen loads
    LaunchedEffect(Unit) {
        viewModel.connect(patientId, "patient")
        val token = com.sentrive.reliefnet.utils.TokenManager.getToken(context) ?: ""
        if (token.isNotEmpty()) {
            viewModel.loadMessagesForConversation(conversationId, token)
        }
    }

    // Disconnect when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    // Convert ViewModel messages to UI messages
    val uiMessages = remember(messages) {
        messages.map { msg ->
            ChatMessageUI(
                text = msg.content,
                isMine = msg.senderId == patientId,
                time = formatTime(msg.sentAt),
                isVoiceMessage = msg.messageType == "audio"
            )
        }
    }

    Scaffold(
        topBar = { 
            PatientChatTopBar(
                doctorName = doctorName,
                isOnline = uiState is ChatUiState.Connected,
                onAudioCall = {
                    navController?.navigate("VideoCallScreen/${patientId}/${doctorId}/true/audio")
                },
                onVideoCall = {
                    navController?.navigate("VideoCallScreen/${patientId}/${doctorId}/true/video")
                }
            ) 
        },
        bottomBar = { 
            PatientChatBottomBar(
                message = messageText,
                onMessageChange = { 
                    messageText = it
                    // Send typing indicator
                    if (it.isNotEmpty()) {
                        viewModel.sendTypingIndicator(conversationId, doctorId)
                    }
                },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(
                            conversationId = conversationId,
                            receiverId = doctorId,
                            receiverType = "doctor",
                            content = messageText
                        )
                        messageText = ""
                    }
                },
                isConnected = uiState is ChatUiState.Connected
            ) 
        },
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PatientChatMessages(
                messages = uiMessages,
                isTyping = isTyping
            )
        }
    }
}

@Composable
fun PatientChatTopBar(
    doctorName: String,
    isOnline: Boolean,
    onAudioCall: () -> Unit = {},
    onVideoCall: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2)
                    )
                )
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = { /* Navigate back */ }) {
            Icon(
                painter = painterResource(R.drawable.doc_back_arrow),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Doctor profile picture
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Image(
                painter = painterResource(R.drawable.doc_image),
                contentDescription = "Doctor Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        // Doctor info
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                doctorName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                    color = Color.White
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isOnline) "Active Now" else "Offline",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = poppinsFontFamily,
                        fontWeight = FontWeight.W400,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Audio call button
        IconButton(onClick = onAudioCall) {
            Icon(
                painter = painterResource(R.drawable.doc_audio_call),
                contentDescription = "Audio Call",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Video call button
        IconButton(onClick = onVideoCall) {
            Icon(
                painter = painterResource(R.drawable.doc_video_call),
                contentDescription = "Video Call",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PatientChatMessages(
    messages: List<ChatMessageUI>,
    isTyping: Boolean = false
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            PatientMessageBubble(message)
        }
        
        // Show typing indicator
        if (isTyping) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Doctor is typing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PatientMessageBubble(message: ChatMessageUI) {
    val bubbleColor = if (message.isMine) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF667EEA),
                Color(0xFF764BA2)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color.White
            )
        )
    }
    val textColor = if (message.isMine) Color.White else Color(0xFF2D2D2D)
    val timeColor = if (message.isMine) Color.White.copy(alpha = 0.8f) else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Doctor's profile picture for received messages
        if (!message.isMine) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                Image(
                    painter = painterResource(R.drawable.doc_image),
                    contentDescription = "Doctor Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble and timestamp
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            // Chat bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (message.isMine) 20.dp else 4.dp,
                            topEnd = if (message.isMine) 4.dp else 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (message.isVoiceMessage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = "Voice Message",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Voice message",
                            color = textColor,
                            fontSize = 14.sp,
                            fontFamily = poppinsFontFamily,
                            fontWeight = FontWeight.W400
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontFamily = poppinsFontFamily,
                        fontWeight = FontWeight.W400
                    )
                }
            }

            // Timestamp
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                text = message.time,
                color = timeColor,
                fontSize = 11.sp,
                fontFamily = poppinsFontFamily,
                fontWeight = FontWeight.W300,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun PatientChatBottomBar(
    message: String = "",
    onMessageChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    isConnected: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Message input field
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            placeholder = {
                Text(
                    if (isConnected) "Type a message..." else "Connecting...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = poppinsFontFamily,
                        fontWeight = FontWeight.W400,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            },
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF667EEA),
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color(0xFF667EEA),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            enabled = isConnected,
            trailingIcon = {
                IconButton(
                    onClick = onSendClick,
                    enabled = message.isNotBlank() && isConnected
                ) {
                    Icon(
                        painter = painterResource(R.drawable.send_icon),
                        contentDescription = "Send",
                        tint = if (message.isNotBlank() && isConnected) Color(0xFF667EEA) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        Spacer(Modifier.width(12.dp))

        // Voice message button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667EEA),
                            Color(0xFF764BA2)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { /* Record voice message */ }) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = "Voice Message",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Helper function to format timestamp
private fun formatTime(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return String.format("%02d:%02d %s", displayHour, minute, amPm)
}

@Preview(showSystemUi = true)
@Composable
fun PreviewPatientChatScreen() {
    PatientChatScreen()
}
