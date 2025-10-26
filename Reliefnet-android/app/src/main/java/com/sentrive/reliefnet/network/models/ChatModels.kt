package com.sentrive.reliefnet.network.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat message data model matching backend schema
 */
data class ChatMessage(
    val _id: String = "",
    val conversationId: String,
    val senderId: String,
    val senderType: String, // "patient" or "doctor"
    val receiverId: String,
    val receiverType: String, // "patient" or "doctor"
    val messageType: String = "text", // "text", "voice", "image"
    val content: String,
    val voiceUrl: String? = null,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String? = null
) {
    /**
     * Check if this message was sent by the current user
     */
    fun isMine(currentUserId: String): Boolean = senderId == currentUserId
    
    /**
     * Get formatted time for display (e.g., "10:30 AM")
     */
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(createdAt)
            
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            // Fallback to just showing the time part
            createdAt.substringAfter("T").substringBefore(".").let {
                val parts = it.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toInt()
                    val minute = parts[1]
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                    "$displayHour:$minute $amPm"
                } else ""
            }
        }
    }
    
    /**
     * Check if this is a voice message
     */
    fun isVoiceMessage(): Boolean = messageType == "voice"
    
    /**
     * Check if this is an image message
     */
    fun isImageMessage(): Boolean = messageType == "image"
}

/**
 * Conversation data model
 */
data class Conversation(
    val _id: String = "",
    val conversationId: String,
    val patientId: String,
    val doctorId: String,
    val patientName: String? = null,
    val doctorName: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val lastMessageSender: String? = null, // "patient" or "doctor"
    val unreadCountPatient: Int = 0,
    val unreadCountDoctor: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    /**
     * Get unread count for specific user type
     */
    fun getUnreadCount(userType: String): Int {
        return if (userType == "patient") unreadCountPatient else unreadCountDoctor
    }
    
    /**
     * Get the other user's name
     */
    fun getOtherUserName(userType: String): String {
        return if (userType == "patient") doctorName ?: "Doctor" else patientName ?: "Patient"
    }
}

/**
 * API Response for messages list
 */
data class MessagesResponse(
    val success: Boolean,
    val messages: List<ChatMessage>,
    val count: Int
)

/**
 * API Response for conversations list
 */
data class ConversationsResponse(
    val success: Boolean,
    val conversations: List<Conversation>,
    val count: Int
)
