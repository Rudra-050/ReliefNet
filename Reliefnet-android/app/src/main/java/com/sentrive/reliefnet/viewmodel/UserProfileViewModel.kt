package com.sentrive.reliefnet.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.User
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val TAG = "UserProfileViewModel"

    /**
     * Fetch patient profile from backend using stored JWT token
     */
    fun fetchUserProfile(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrEmpty()) {
                    _error.value = "Not authenticated"
                    Log.e(TAG, "No token found")
                    _isLoading.value = false
                    return@launch
                }

                val response = RetrofitClient.apiService.getPatientProfile("Bearer $token")
                
                if (response.isSuccessful) {
                    val user = response.body()
                    _userProfile.value = user
                    Log.d(TAG, "User profile loaded: ${user?.name}, Photo: ${user?.photoUrl}")
                } else {
                    _error.value = "Failed to load profile: ${response.code()}"
                    Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e(TAG, "Exception fetching profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update patient profile
     */
    fun updateUserProfile(context: Context, updates: Map<String, Any?>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrEmpty()) {
                    _error.value = "Not authenticated"
                    _isLoading.value = false
                    return@launch
                }

                val response = RetrofitClient.apiService.updatePatientProfile(updates, "Bearer $token")
                
                if (response.isSuccessful) {
                    val user = response.body()
                    _userProfile.value = user
                    Log.d(TAG, "User profile updated: ${user?.name}")
                } else {
                    _error.value = "Failed to update profile: ${response.code()}"
                    Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e(TAG, "Exception updating profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear user profile (for logout)
     */
    fun clearProfile() {
        _userProfile.value = null
        _error.value = null
    }
}
