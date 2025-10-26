package com.sentrive.reliefnet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentrive.reliefnet.network.ChatRequest
import com.sentrive.reliefnet.network.ChatbotClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BotMessage(
    val text: String,
    val isMine: Boolean,
    val time: String = getCurrentTime()
)

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

class ChatbotViewModel : ViewModel() {
    
    private val _messages = MutableStateFlow<List<BotMessage>>(emptyList())
    val messages: StateFlow<List<BotMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val TAG = "ChatbotViewModel"
    
    // Generate a unique user ID that persists during the session
    private val userId: String = UUID.randomUUID().toString()
    
    init {
        // Add welcome message
        _messages.value = listOf(
            BotMessage(
                text = "👋 Hi! I'm Relie, your emotional support assistant. I'm here to listen anytime 💙",
                isMine = false
            )
        )
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        // Add user message
        val userMessage = BotMessage(text = message, isMine = true)
        _messages.value = _messages.value + userMessage
        
        // Call chatbot API
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = ChatbotClient.chatbotService.sendMessage(
                    ChatRequest(user_id = userId, message = message)
                )
                
                if (response.isSuccessful) {
                    val botReply = response.body()?.reply ?: "I'm sorry, I didn't understand that."
                    val botMessage = BotMessage(text = botReply, isMine = false)
                    _messages.value = _messages.value + botMessage
                    Log.d(TAG, "Bot reply: $botReply")
                } else {
                    _error.value = "Failed to get response: ${response.code()}"
                    Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                    addErrorMessage()
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e(TAG, "Exception sending message", e)
                addErrorMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun addErrorMessage() {
        val errorMsg = BotMessage(
            text = "I'm having trouble connecting right now. Please try again in a moment.",
            isMine = false
        )
        _messages.value = _messages.value + errorMsg
    }
    
    fun clearMessages() {
        _messages.value = listOf(
            BotMessage(
                text = "👋 Hi! I'm Relie, your emotional support assistant. I'm here to listen anytime 💙",
                isMine = false
            )
        )
    }
}
