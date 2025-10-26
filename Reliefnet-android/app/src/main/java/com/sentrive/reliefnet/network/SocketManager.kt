package com.sentrive.reliefnet.network

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketManager {
    
    private var socket: Socket? = null
    
    fun connect(): Socket? {
        try {
            if (socket == null || !socket!!.connected()) {
                val options = IO.Options().apply {
                    reconnection = true
                    reconnectionDelay = 1000
                    reconnectionAttempts = Int.MAX_VALUE
                }
                
                socket = IO.socket(ApiConfig.SOCKET_URL, options)
                socket?.connect()
            }
            return socket
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return null
        }
    }
    
    fun getSocket(): Socket? {
        if (socket == null || !socket!!.connected()) {
            connect()
        }
        return socket
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
    
    fun registerUser(userId: String, userType: String) {
        socket?.emit("register", mapOf(
            "userId" to userId,
            "userType" to userType
        ))
    }
    
    fun initiateCall(
        toUserId: String,
        toUserType: String,
        fromUserId: String,
        fromUserType: String,
        callType: String
    ) {
        socket?.emit("call:initiate", mapOf(
            "toUserId" to toUserId,
            "toUserType" to toUserType,
            "fromUserId" to fromUserId,
            "fromUserType" to fromUserType,
            "callType" to callType
        ))
    }
    
    fun sendOffer(
        toUserId: String,
        toUserType: String,
        offer: Any,
        fromUserId: String,
        fromUserType: String
    ) {
        socket?.emit("call:offer", mapOf(
            "toUserId" to toUserId,
            "toUserType" to toUserType,
            "offer" to offer,
            "fromUserId" to fromUserId,
            "fromUserType" to fromUserType
        ))
    }
    
    fun sendAnswer(
        toUserId: String,
        toUserType: String,
        answer: Any,
        fromUserId: String,
        fromUserType: String
    ) {
        socket?.emit("call:answer", mapOf(
            "toUserId" to toUserId,
            "toUserType" to toUserType,
            "answer" to answer,
            "fromUserId" to fromUserId,
            "fromUserType" to fromUserType
        ))
    }
    
    fun sendIceCandidate(
        toUserId: String,
        toUserType: String,
        candidate: Any,
        fromUserId: String,
        fromUserType: String
    ) {
        socket?.emit("call:ice-candidate", mapOf(
            "toUserId" to toUserId,
            "toUserType" to toUserType,
            "candidate" to candidate,
            "fromUserId" to fromUserId,
            "fromUserType" to fromUserType
        ))
    }
    
    fun endCall(
        toUserId: String,
        toUserType: String,
        fromUserId: String,
        fromUserType: String
    ) {
        socket?.emit("call:end", mapOf(
            "toUserId" to toUserId,
            "toUserType" to toUserType,
            "fromUserId" to fromUserId,
            "fromUserType" to fromUserType
        ))
    }
    
    // ==================== CHAT FUNCTIONS ====================
    
    /**
     * Send a chat message
     */
    fun sendChatMessage(
        conversationId: String,
        senderId: String,
        senderType: String,
        receiverId: String,
        receiverType: String,
        content: String,
        messageType: String = "text",
        voiceUrl: String? = null,
        imageUrl: String? = null
    ) {
        socket?.emit("chat:send-message", mapOf(
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderType" to senderType,
            "receiverId" to receiverId,
            "receiverType" to receiverType,
            "messageType" to messageType,
            "content" to content,
            "voiceUrl" to voiceUrl,
            "imageUrl" to imageUrl
        ))
    }
    
    /**
     * Send typing indicator
     */
    fun sendTypingIndicator(
        conversationId: String,
        userId: String,
        userType: String,
        isTyping: Boolean
    ) {
        socket?.emit("chat:typing", mapOf(
            "conversationId" to conversationId,
            "userId" to userId,
            "userType" to userType,
            "isTyping" to isTyping
        ))
    }
    
    /**
     * Mark messages as read
     */
    fun markMessagesAsRead(
        conversationId: String,
        userId: String,
        userType: String
    ) {
        socket?.emit("chat:mark-read", mapOf(
            "conversationId" to conversationId,
            "userId" to userId,
            "userType" to userType
        ))
    }
}
