package com.sentrive.reliefnet.repository

import android.util.Log
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.ChatMessage
import com.sentrive.reliefnet.network.models.Conversation
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Repository for chat-related API calls
 */
class ChatRepository {
    private val apiService = RetrofitClient.apiService
    private val TAG = "ChatRepository"
    
    /**
     * Get all conversations for a user
     */
    suspend fun getConversations(
        userType: String,
        userId: String,
        token: String
    ): Result<List<Conversation>> {
        return try {
            RetrofitClient.authToken = token
            val response = apiService.getConversations(userType, userId, "Bearer $token")
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.conversations)
            } else {
                Result.failure(Exception("Failed to fetch conversations: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching conversations", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all messages in a conversation
     */
    suspend fun getMessages(
        conversationId: String,
        token: String
    ): Result<List<ChatMessage>> {
        return try {
            RetrofitClient.authToken = token
            val response = apiService.getMessages(conversationId, "Bearer $token")
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.messages)
            } else {
                Result.failure(Exception("Failed to fetch messages: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload voice message file
     */
    suspend fun uploadVoiceMessage(
        audioFile: File,
        token: String
    ): Result<String> {
        return try {
            val requestBody = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
            
            val response = apiService.uploadVoiceMessage(filePart, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception("Failed to upload voice message: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading voice message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload image file
     */
    suspend fun uploadImage(
        imageFile: File,
        token: String
    ): Result<String> {
        return try {
            val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", imageFile.name, requestBody)
            
            val response = apiService.uploadImage(filePart, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }
}
