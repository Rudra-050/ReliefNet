package com.sentrive.reliefnet.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class FirebaseAuthManager(private val activity: Activity) {

    private val TAG = "FirebaseAuthManager"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun firebaseAuthWithGoogle(
        idToken: String,
        photoUrl: String? = null,
        onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "signInWithCredential:success → ${user?.email}")
                    Log.d(TAG, "User display name: ${user?.displayName}")
                    Log.d(TAG, "User photo URL: ${user?.photoUrl ?: photoUrl}")
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure(Exception("Firebase user is null"))
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    onFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    fun getCurrentUser() = auth.currentUser
}
