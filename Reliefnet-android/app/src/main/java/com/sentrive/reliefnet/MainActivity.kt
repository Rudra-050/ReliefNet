package com.sentrive.reliefnet

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sentrive.reliefnet.navigation.Navigation
import com.sentrive.reliefnet.ui.theme.ReliefNetTheme
import com.sentrive.reliefnet.userInterface.HomePage
import com.sentrive.reliefnet.notifications.ReliefFirebaseMessagingService
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.network.RetrofitClient
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    
    private var pendingDeepLink by mutableStateOf<Uri?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        enableEdgeToEdge(
            SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        
        // Check for initial deep link
        intent?.data?.let { uri ->
            Log.d("MainActivity", "Initial deep link: $uri")
            pendingDeepLink = uri
        }
        
        setContent {
            ReliefNetTheme {
                Surface (modifier = Modifier.fillMaxSize()) {
                    Greeting(
                        deepLinkUri = pendingDeepLink,
                        onDeepLinkHandled = { pendingDeepLink = null }
                    )
                }
            }
        }

        // Initialize global auth token from persisted token if available
        try {
            val saved = TokenManager.getToken(applicationContext)
            if (!saved.isNullOrBlank()) {
                RetrofitClient.authToken = saved
            }
        } catch (_: Exception) { }

        // Best-effort ensure FCM token is registered after app starts and if user is logged in
        ReliefFirebaseMessagingService.ensureTokenRegistered(this)

        // Request runtime notification permission on Android 13+
        requestNotificationPermissionIfNeeded()
    }
    
    // Handle deep links when app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        intent.data?.let { uri ->
            Log.d("MainActivity", "New intent deep link: $uri")
            pendingDeepLink = uri
        }
    }
}


@Composable
fun Greeting(
    deepLinkUri: Uri? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Handle deep link navigation
    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { uri ->
            handleDeepLink(uri, navController)
            onDeepLinkHandled()
        }
    }
    
    Navigation(navController)
}

/**
 * Handle deep link navigation
 */
fun handleDeepLink(uri: Uri, navController: NavController) {
    Log.d("MainActivity", "Handling deep link: $uri")
    Log.d("MainActivity", "Host: ${uri.host}, Path: ${uri.path}")
    
    when (uri.host) {
        "payment" -> handlePaymentDeepLink(uri, navController)
        // Add other deep link handlers here
        else -> Log.w("MainActivity", "Unknown deep link host: ${uri.host}")
    }
}

/**
 * Handle payment deep links from PhonePe
 */
fun handlePaymentDeepLink(uri: Uri, navController: NavController) {
    val path = uri.path
    val transactionId = uri.getQueryParameter("transactionId")
    val doctorId = uri.getQueryParameter("doctorId")
    val date = uri.getQueryParameter("date")
    val time = uri.getQueryParameter("time")
    
    Log.d("MainActivity", "Payment deep link - Path: $path")
    Log.d("MainActivity", "Transaction ID: $transactionId")
    Log.d("MainActivity", "Doctor ID: $doctorId")
    Log.d("MainActivity", "Date: $date, Time: $time")
    
    // Validate required parameters
    if (transactionId.isNullOrEmpty() || doctorId.isNullOrEmpty() || 
        date.isNullOrEmpty() || time.isNullOrEmpty()) {
        Log.e("MainActivity", "Missing required parameters in deep link")
        return
    }
    
    when (path) {
        "/success", "/failed", "/error" -> {
            // Navigate to payment status screen
            navController.navigate(
                "payment_status/$transactionId/$doctorId/$date/$time"
            ) {
                // Clear back stack up to home to prevent going back to booking screen
                popUpTo("home") { 
                    inclusive = false 
                }
                // Avoid multiple copies of the same destination
                launchSingleTop = true
            }
        }
        else -> Log.w("MainActivity", "Unknown payment path: $path")
    }
}

private const val REQ_POST_NOTIFICATIONS = 1001

private fun ComponentActivity.requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS
            )
        }
    }
}

