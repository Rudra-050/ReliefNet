package com.sentrive.reliefnet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentrive.reliefnet.repository.ChatRepository
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderType: String, // "patient" or "doctor"
    val receiverId: String,
    val receiverType: String,
    val messageType: String = "text", // text, image, audio, video, file
    val content: String,
    val mediaUrl: String? = null,
    val status: String = "sent", // sent, delivered, read
    val sentAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null
)

data class Conversation(
    val conversationId: String,
    val patientId: String,
    val doctorId: String,
    val patientName: String,
    val doctorName: String,
    val lastMessage: String?,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)

sealed class ChatUiState {
    object Disconnected : ChatUiState()
    object Connecting : ChatUiState()
    object Connected : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
        // Use Railway production URL - will work on all devices/emulator
        private const val SERVER_URL = "https://reliefnet-production-e119.up.railway.app"
    }

    private var socket: Socket? = null

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Disconnected)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private var currentUserId: String? = null
    private var currentUserType: String? = null

    init {
        initializeSocket()
    }

    private fun initializeSocket() {
        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
            }

            socket = IO.socket(SERVER_URL, options)
            setupSocketListeners()
            
            Log.d(TAG, "Socket initialized")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket initialization error", e)
            _uiState.value = ChatUiState.Error("Failed to connect: ${e.message}")
        }
    }

    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
                _uiState.value = ChatUiState.Connected
                
                // Register user if credentials available
                currentUserId?.let { userId ->
                    currentUserType?.let { userType ->
                        registerUser(userId, userType)
                    }
                }
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket disconnected")
                _uiState.value = ChatUiState.Disconnected
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connection error: ${args.contentToString()}")
                _uiState.value = ChatUiState.Error("Connection error")
            }

            on("chat:message") { args ->
                try {
                    val data = args[0] as JSONObject
                    val message = parseChatMessage(data)
                    
                    viewModelScope.launch {
                        val currentMessages = _messages.value.toMutableList()
                        currentMessages.add(message)
                        _messages.value = currentMessages
                    }
                    
                    Log.d(TAG, "Received message: ${message.content}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }

            on("chat:typing") { args ->
                try {
                    val data = args[0] as JSONObject
                    val senderId = data.getString("senderId")
                    
                    // Only show typing if it's from the other person
                    if (senderId != currentUserId) {
                        _isTyping.value = true
                        
                        // Auto-hide after 3 seconds
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(3000)
                            _isTyping.value = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling typing indicator", e)
                }
            }

            on("chat:delivered") { args ->
                try {
                    val data = args[0] as JSONObject
                    val messageId = data.getString("messageId")
                    
                    updateMessageStatus(messageId, "delivered")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling delivery confirmation", e)
                }
            }

            on("chat:read") { args ->
                try {
                    val data = args[0] as JSONObject
                    val messageIds = data.getJSONArray("messageIds")
                    
                    for (i in 0 until messageIds.length()) {
                        updateMessageStatus(messageIds.getString(i), "read")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling read confirmation", e)
                }
            }
        }
    }

    fun connect(userId: String, userType: String) {
        currentUserId = userId
        currentUserType = userType
        
        if (_uiState.value != ChatUiState.Connected) {
            _uiState.value = ChatUiState.Connecting
            socket?.connect()
            Log.d(TAG, "Connecting to socket as $userType:$userId")
        } else {
            registerUser(userId, userType)
        }
    }

    private fun registerUser(userId: String, userType: String) {
        val data = JSONObject().apply {
            put("userId", userId)
            put("userType", userType)
        }
        socket?.emit("register", data)
        Log.d(TAG, "User registered: $userType:$userId")
    }

    fun sendMessage(
        conversationId: String,
        receiverId: String,
        receiverType: String,
        content: String,
        messageType: String = "text"
    ) {
        val userId = currentUserId ?: return
        val userType = currentUserType ?: return

        val messageData = JSONObject().apply {
            put("conversationId", conversationId)
            put("senderId", userId)
            put("senderType", userType)
            put("receiverId", receiverId)
            put("receiverType", receiverType)
            put("messageType", messageType)
            put("content", content)
        }

        socket?.emit("chat:send", messageData)
        
        // Optimistically add message to UI
        val tempMessage = ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderId = userId,
            senderType = userType,
            receiverId = receiverId,
            receiverType = receiverType,
            messageType = messageType,
            content = content,
            status = "sent"
        )
        
        viewModelScope.launch {
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(tempMessage)
            _messages.value = currentMessages
        }
        
        Log.d(TAG, "Message sent: $content")
    }

    fun sendTypingIndicator(conversationId: String, receiverId: String) {
        val userId = currentUserId ?: return
        val userType = currentUserType ?: return

        val data = JSONObject().apply {
            put("conversationId", conversationId)
            put("senderId", userId)
            put("senderType", userType)
            put("receiverId", receiverId)
        }

        socket?.emit("chat:typing", data)
    }

    fun markAsRead(conversationId: String, messageIds: List<String>) {
        val userId = currentUserId ?: return
        val userType = currentUserType ?: return

        val data = JSONObject().apply {
            put("conversationId", conversationId)
            put("userId", userId)
            put("userType", userType)
            put("messageIds", org.json.JSONArray(messageIds))
        }

        socket?.emit("chat:read", data)
    }

    fun loadMessagesForConversation(conversationId: String, token: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.getMessages(conversationId, token)
                result.onSuccess { apiMessages ->
                    // Convert API messages to ViewModel messages
                    val vmMessages = apiMessages.map { apiMsg ->
                        ChatMessage(
                            id = apiMsg._id,
                            conversationId = apiMsg.conversationId,
                            senderId = apiMsg.senderId,
                            senderType = apiMsg.senderType,
                            receiverId = apiMsg.receiverId,
                            receiverType = apiMsg.receiverType,
                            messageType = apiMsg.messageType,
                            content = apiMsg.content,
                            mediaUrl = apiMsg.voiceUrl ?: apiMsg.imageUrl,
                            status = if (apiMsg.isRead) "read" else "delivered",
                            sentAt = parseTimestamp(apiMsg.createdAt)
                        )
                    }
                    _messages.value = vmMessages
                    Log.d(TAG, "Loaded ${vmMessages.size} messages for conversation $conversationId")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load messages", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages", e)
            }
        }
    }
    
    fun loadConversations(userType: String, userId: String, token: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.getConversations(userType, userId, token)
                result.onSuccess { apiConversations ->
                    // Convert API conversations to ViewModel conversations
                    val vmConversations = apiConversations.map { apiConv ->
                        Conversation(
                            conversationId = apiConv.conversationId,
                            patientId = apiConv.patientId,
                            doctorId = apiConv.doctorId,
                            patientName = apiConv.patientName ?: "Patient",
                            doctorName = apiConv.doctorName ?: "Doctor",
                            lastMessage = apiConv.lastMessage,
                            lastMessageTime = parseTimestamp(apiConv.lastMessageTime ?: ""),
                            unreadCount = apiConv.getUnreadCount(userType)
                        )
                    }
                    _conversations.value = vmConversations
                    Log.d(TAG, "Loaded ${vmConversations.size} conversations")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load conversations", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
            }
        }
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseChatMessage(json: JSONObject): ChatMessage {
        return ChatMessage(
            id = json.optString("_id", ""),
            conversationId = json.getString("conversationId"),
            senderId = json.getString("senderId"),
            senderType = json.getString("senderType"),
            receiverId = json.getString("receiverId"),
            receiverType = json.getString("receiverType"),
            messageType = json.optString("messageType", "text"),
            content = json.getString("content"),
            mediaUrl = json.optString("mediaUrl", null),
            status = json.optString("status", "sent"),
            sentAt = json.optLong("sentAt", System.currentTimeMillis()),
            deliveredAt = if (json.has("deliveredAt")) json.getLong("deliveredAt") else null,
            readAt = if (json.has("readAt")) json.getLong("readAt") else null
        )
    }

    private fun updateMessageStatus(messageId: String, newStatus: String) {
        viewModelScope.launch {
            val updatedMessages = _messages.value.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        status = newStatus,
                        deliveredAt = if (newStatus == "delivered") System.currentTimeMillis() else message.deliveredAt,
                        readAt = if (newStatus == "read") System.currentTimeMillis() else message.readAt
                    )
                } else {
                    message
                }
            }
            _messages.value = updatedMessages
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        Log.d(TAG, "Socket disconnected and listeners removed")
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
