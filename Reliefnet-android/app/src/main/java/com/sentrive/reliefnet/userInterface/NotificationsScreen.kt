package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.network.models.Notification
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Fetch notifications on load
    LaunchedEffect(Unit) {
        val token = TokenManager.getToken(context)
        
        if (token != null) {
            repository.getNotifications(token)
                .onSuccess { response ->
                    notifications = response.notifications.sortedByDescending { it.createdAt }
                    isLoading = false
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to load notifications"
                    isLoading = false
                }
        } else {
            errorMessage = "Please login first"
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PatientPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PatientPrimary
                    )
                }
                
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navHostController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
                
                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You'll see notifications here when you have them",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    // Mark as read
                                    scope.launch {
                                        val token = TokenManager.getToken(context)
                                        if (token != null && !notification.isRead) {
                                            repository.markNotificationAsRead(notification.id, token)
                                        }
                                    }
                                    
                                    // Navigate based on notification type
                                    when (notification.type) {
                                        "booking" -> {
                                            val userType = TokenManager.getUserType(context)
                                            if (userType == "doctor") {
                                                navHostController.navigate("DoctorSessions")
                                            } else {
                                                navHostController.navigate("YourBookings")
                                            }
                                        }
                                        "payment" -> {
                                            val userType = TokenManager.getUserType(context)
                                            if (userType == "doctor") {
                                                navHostController.navigate("DoctorPayments")
                                            } else {
                                                navHostController.navigate("PaymentHistory")
                                            }
                                        }
                                        "session" -> {
                                            val userType = TokenManager.getUserType(context)
                                            if (userType == "doctor") {
                                                navHostController.navigate("DoctorSessions")
                                            } else {
                                                navHostController.navigate("YourBookings")
                                            }
                                        }
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (!notification.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon based on notification type
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when (notification.type) {
                        "booking" -> MaterialTheme.colorScheme.primary
                        "payment" -> Color(0xFF4CAF50)
                        "session" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (notification.type) {
                    "booking" -> Icons.Default.CalendarToday
                    "payment" -> Icons.Default.Payment
                    "session" -> Icons.Default.Event
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatTimestamp(notification.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(timestamp)
        
        if (date != null) {
            val now = Date()
            val diff = now.time - date.time
            
            when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000} minutes ago"
                diff < 86400000 -> "${diff / 3600000} hours ago"
                diff < 604800000 -> "${diff / 86400000} days ago"
                else -> {
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } else {
            "Unknown time"
        }
    } catch (e: Exception) {
        "Unknown time"
    }
}
