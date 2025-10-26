package com.sentrive.reliefnet.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReliefFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM new token: $token")
        // Try to register token if logged in; else store for later
        registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "ReliefNet"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "reliefnet_default"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ReliefNet Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    private fun registerToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = TokenManager.getToken(applicationContext)
                if (!auth.isNullOrBlank()) {
                    val resp = RetrofitClient.apiService.registerDeviceToken(
                        mapOf("token" to token, "platform" to "android"),
                        "Bearer $auth"
                    )
                    if (resp.isSuccessful) {
                        Log.d(TAG, "Token registered with backend")
                    } else {
                        Log.w(TAG, "Token registration failed: ${resp.code()} ${resp.message()}")
                    }
                } else {
                    // No auth yet; keep token in shared prefs to retry later
                    TokenManager.saveFcmToken(applicationContext, token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering token", e)
            }
        }
    }

    companion object {
        private const val TAG = "ReliefFCMService"
        fun ensureTokenRegistered(context: Context) {
            // If logged in, try to send cached token (if any)
            CoroutineScope(Dispatchers.IO).launch {
                val auth = TokenManager.getToken(context) ?: return@launch
                val token = TokenManager.getFcmToken(context)
                if (!token.isNullOrBlank()) {
                    try {
                        val resp = RetrofitClient.apiService.registerDeviceToken(
                            mapOf("token" to token, "platform" to "android"),
                            "Bearer $auth"
                        )
                        if (resp.isSuccessful) {
                            Log.d(TAG, "Token ensured/registered")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ensureTokenRegistered error", e)
                    }
                }
            }
        }
    }
}

