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
data class DoctorChatMessageUI(
    val text: String,
    val isMine: Boolean,
    val time: String,
    val isVoiceMessage: Boolean = false
)

@Composable
fun DoctorChatScreen(
    navController: NavHostController? = null,
    doctorId: String = "68f4827e3174500e31a5a00f", // Default for testing
    patientId: String = "687910a4748f95606960a4ca", // Default for testing
    patientName: String = "Rudra Bhatt",
    viewModel: ChatViewModel = viewModel()
){
    val context = androidx.compose.ui.platform.LocalContext.current
    val conversationId = remember { "${patientId}:${doctorId}" }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var messageText by remember { mutableStateOf("") }

    // Connect to socket when screen loads
    LaunchedEffect(Unit) {
        viewModel.connect(doctorId, "doctor")
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
            DoctorChatMessageUI(
                text = msg.content,
                isMine = msg.senderId == doctorId,
                time = formatDoctorTime(msg.sentAt),
                isVoiceMessage = msg.messageType == "audio"
            )
        }
    }

    Scaffold(
        topBar = {
            DoctorTopBar(
                patientName = patientName,
                isOnline = uiState is ChatUiState.Connected,
                onAudioCall = {
                    navController?.navigate("VideoCallScreen/${doctorId}/${patientId}/true/audio")
                },
                onVideoCall = {
                    navController?.navigate("VideoCallScreen/${doctorId}/${patientId}/true/video")
                }
            )
        },
        bottomBar = {
            DoctorChatBottomBar(
                message = messageText,
                onMessageChange = { 
                    messageText = it
                    // Send typing indicator
                    if (it.isNotEmpty()) {
                        viewModel.sendTypingIndicator(conversationId, patientId)
                    }
                },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(
                            conversationId = conversationId,
                            receiverId = patientId,
                            receiverType = "patient",
                            content = messageText
                        )
                        messageText = ""
                    }
                },
                isConnected = uiState is ChatUiState.Connected
            )
        },
        modifier = Modifier.statusBarsPadding()
            .navigationBarsPadding()
    ) { paddingValues ->
        Column(Modifier.fillMaxSize()
            .padding(paddingValues)) {
            DoctorChats(
                messages = uiMessages,
                isTyping = isTyping
            )
        }
    }
}
@Composable
fun DoctorTopBar(
    patientName: String,
    isOnline: Boolean,
    onAudioCall: () -> Unit = {},
    onVideoCall: () -> Unit = {}
){
    Row (Modifier
        .fillMaxWidth()
        .background(Color.White),
        verticalAlignment = Alignment.CenterVertically){
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(R.drawable.doc_back_arrow),
                contentDescription = "Back Arrow"
            )
        }
        Box(Modifier
            .size(49.dp, 45.dp)
            .clip(CircleShape)){
            Image(painter = painterResource(R.drawable.doc_image),
                contentDescription = "Patient Profile Pic",
                contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(12.dp))

        val statusText = if (isOnline) "Active Now" else "Offline"
        val color = if (isOnline) Color.Green else Color.LightGray
        Column(horizontalAlignment = Alignment.Start) {
            Row { Text(patientName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 18.sp,
                    color = Color.Black
                )) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text(statusText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 10.sp,
                    color = Color.LightGray
                ))
            Spacer(Modifier.width(4.dp))
                Box(Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color))
            }
        }
    Spacer(Modifier.weight(1f))
    IconButton(onClick = onAudioCall){
        Icon(
            painter = painterResource(R.drawable.doc_audio_call),
            contentDescription = "Audio Call",
            modifier = Modifier.size(26.dp, 18.dp)
        )
    }
    Spacer(Modifier.width(20.dp))
    IconButton(onClick = onVideoCall) {
        Icon(
            painter = painterResource(R.drawable.doc_video_call),
            contentDescription = "Video Call",
            modifier = Modifier
                .size(26.dp, 18.dp)
                .padding(end = 4.dp)
        )
    }
    }
}


@Composable
fun DoctorChats(
    messages: List<DoctorChatMessageUI>,
    isTyping: Boolean = false
){
    LazyColumn(
        modifier = Modifier.background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { msg ->
            DoctorMessageBubble(msg)
        }
        
        // Show typing indicator
        if (isTyping) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Patient is typing...",
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
fun DoctorMessageBubble(message: DoctorChatMessageUI){
    val bubbleColor = if (message.isMine) Color(0xFF8C8A9F) else Color.LightGray
    val textColor = if (message.isMine) Color.Black else Color(0xFF383737)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ){
        // Receiver's profile pic
        if (!message.isMine) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ){
                Image(painterResource(R.drawable.doc_image),
                    contentDescription = "DoctorProfile",
                    contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        // Message + timestamp row
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // For sender → time first, then bubble
            if (message.isMine) {
                Text(
                    text = message.time,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)

                )
            }

            // Chat bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (message.isMine) 16.dp else 4.dp,
                            topEnd = if (message.isMine) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }

            // For receiver → bubble first, then time
            if (!message.isMine) {
                Text(
                    text = message.time,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }

}
@Composable
fun DoctorChatBottomBar(
    message: String = "",
    onMessageChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    isConnected: Boolean = false
){
   Row(Modifier.fillMaxWidth()
       .background(Color.White)
       .padding(all = 13.dp),
       verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            placeholder = {
                Text(
                    if (isConnected) "Send Message" else "Connecting...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = poppinsFontFamily,
                        fontWeight = FontWeight.W400,
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                )
            },
            modifier = Modifier.background(Color.White),
            shape = RoundedCornerShape(16.dp),
            enabled = isConnected,
            trailingIcon = {
                IconButton(
                    onClick = onSendClick,
                    enabled = message.isNotBlank() && isConnected
                ) {
                    Icon(
                        painterResource(R.drawable.send_icon),
                        contentDescription = "Send",
                        Modifier.size(19.dp),
                        tint = if (message.isNotBlank() && isConnected) Color.Black else Color.LightGray
                    )
                }
            }
        )
       Spacer(Modifier.weight(1f))
       Box(Modifier.size(50.dp)
           .clip(CircleShape)

           .background(brush =
               Brush.linearGradient(
                   listOf(Color(0xFF49467E),
                       Color(0xFF007665),
                       Color(0xF22C2871)
                   )
               )


           ),
           contentAlignment = Alignment.Center){
           Icon(painterResource(R.drawable.mic_doc),
               contentDescription = "Mic",
               tint = Color.White)
       }
    }
}

// Helper function to format timestamp
private fun formatDoctorTime(timestamp: Long): String {
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
fun show(){
   // DoctorTopBar("Gautam Sah",false)
    DoctorChatScreen()
}