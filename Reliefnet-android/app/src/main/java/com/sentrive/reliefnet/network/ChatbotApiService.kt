package com.sentrive.reliefnet.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val user_id: String,
    val message: String
)

data class ChatResponse(
    val reply: String
)

interface ChatbotApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>
}
