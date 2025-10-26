package com.sentrive.reliefnet.auth

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleAuthActivity: ComponentActivity() {

    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private lateinit var credentialManager: CredentialManager
    private val TAG = "GoogleAuthActivity"// Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {    // Called when Activity is created
        super.onCreate(savedInstanceState)
        firebaseAuthManager = FirebaseAuthManager(this) // Init FirebaseAuth manager
        credentialManager = CredentialManager.create(this)  // Init Credential Manager

        // Start Google Sign-In immediately (instead of waiting for button click)
        startGoogleSignIn()
    }

    private fun startGoogleSignIn(){
        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(getString(R.string.default_web_client_id))
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(false)
            .build()

// Create the Credential Manager request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
Log.d("Credential","$request")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(this@GoogleAuthActivity, request)

                //Debug
                Log.d("Result","$result")
                handleSignIn(result.credential) // Handle credential after success
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed: ${e.localizedMessage}")
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Log user details
            Log.d(TAG, "Google Sign-In - Name: ${googleIdTokenCredential.displayName}")
            Log.d(TAG, "Google Sign-In - Email: ${googleIdTokenCredential.id}")
            Log.d(TAG, "Google Sign-In - Photo: ${googleIdTokenCredential.profilePictureUri}")

            // Sign in to Firebase with using the token
            firebaseAuthManager.firebaseAuthWithGoogle(
                idToken = googleIdTokenCredential.idToken,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                onSuccess = { firebaseUser ->
                    Log.d(TAG, "Firebase sign-in successful")
                    // Get Firebase ID token
                    firebaseUser.getIdToken(false).addOnSuccessListener { result ->
                        val firebaseIdToken = result.token
                        if (firebaseIdToken != null) {
                            // Now send Firebase token to backend and get JWT
                            authenticateWithBackend(
                                firebaseIdToken,
                                googleIdTokenCredential.profilePictureUri?.toString()
                            )
                        } else {
                            Log.e(TAG, "Failed to get Firebase ID token")
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get Firebase ID token", exception)
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Firebase sign-in failed", exception)
                    setResult(RESULT_CANCELED)
                    finish()
                }
            )
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Authenticate with backend and get JWT token
     */
    private fun authenticateWithBackend(idToken: String, photoUrl: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Sending token to backend...")
                
                val requestBody = mutableMapOf<String, String>("idToken" to idToken)
                photoUrl?.let { requestBody["photoUrl"] = it }
                
                val response = RetrofitClient.apiService.authenticateWithGoogle(requestBody)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val token = authResponse?.token
                        
                        if (!token.isNullOrEmpty()) {
                            // Save JWT token
                            TokenManager.saveToken(this@GoogleAuthActivity, token)
                            // Set global interceptor token for repository calls
                            RetrofitClient.authToken = token
                            
                            // Save user info
                            authResponse.user?.let { user ->
                                    val userId = user.id ?: user.email
                                    TokenManager.saveUserInfo(
                                        context = this@GoogleAuthActivity,
                                        userId = userId,
                                        userType = "User",
                                        name = user.name,
                                        email = user.email,
                                        photoUrl = user.photoUrl
                                    )
                            }
                            
                            Log.d(TAG, "Backend auth successful, token saved")
                            Log.d(TAG, "User: ${authResponse.user?.name}, Photo: ${authResponse.user?.photoUrl}")
                            
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Log.e(TAG, "No token received from backend")
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    } else {
                        Log.e(TAG, "Backend auth failed: ${response.code()} - ${response.message()}")
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backend auth error", e)
                withContext(Dispatchers.Main) {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

}