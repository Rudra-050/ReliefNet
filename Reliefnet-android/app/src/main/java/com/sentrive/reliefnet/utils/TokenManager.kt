package com.sentrive.reliefnet.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * TokenManager - Handles JWT token storage and retrieval
 * Use this to save and access authentication tokens throughout the app
 */
object TokenManager {
    private const val PREF_NAME = "NirogSetuPrefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_TYPE = "user_type" // "User" or "Doctor"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHOTO_URL = "user_photo_url"
    private const val KEY_FCM_TOKEN = "fcm_token"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save authentication token
     */
    fun saveToken(context: Context, token: String) {
        getPreferences(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
    
    /**
     * Get authentication token
     */
    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * Save user information
     */
    fun saveUserInfo(
        context: Context,
        userId: String,
        userType: String,
        name: String,
        email: String,
        photoUrl: String? = null
    ) {
        getPreferences(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_TYPE, userType)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHOTO_URL, photoUrl)
            apply()
        }
    }
    
    /**
     * Get user ID
     */
    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_ID, null)
    }
    
    /**
     * Get user type
     */
    fun getUserType(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_TYPE, null)
    }
    
    /**
     * Get user name
     */
    fun getUserName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_NAME, null)
    }
    
    /**
     * Get user email
     */
    fun getUserEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    /**
     * Get user photo URL
     */
    fun getUserPhotoUrl(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_PHOTO_URL, null)
    }
    
    /**
     * Update user photo URL
     */
    fun saveUserPhotoUrl(context: Context, photoUrl: String?) {
        getPreferences(context).edit().putString(KEY_USER_PHOTO_URL, photoUrl).apply()
    }

    /**
     * Save/Retrieve FCM device token locally
     */
    fun saveFcmToken(context: Context, token: String) {
        getPreferences(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(context: Context): String? {
        return getPreferences(context).getString(KEY_FCM_TOKEN, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }
    
    /**
     * Clear all user data (logout)
     */
    fun clearUserData(context: Context) {
        getPreferences(context).edit().clear().apply()
        // Also sign out from Firebase
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Firebase not initialized, that's fine
        }
    }
}
