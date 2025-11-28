package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sentrive.reliefnet.network.models.Booking
import com.sentrive.reliefnet.network.models.BookingStatus
import com.sentrive.reliefnet.network.models.PaymentStatus
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.network.models.toDisplayString
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import com.sentrive.reliefnet.userInterface.MainBottomBar
import androidx.compose.ui.res.painterResource
import com.sentrive.reliefnet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourBookingsIntegratedScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Fetch bookings
    LaunchedEffect(Unit) {
        val token = TokenManager.getToken(context)
        val userId = TokenManager.getUserId(context)
        
        if (token != null && userId != null) {
            try {
                val resp = repository.getPatientBookings(userId)
                if (resp.isSuccessful) {
                    val data = resp.body()?.bookings ?: emptyList()
                    bookings = data.sortedByDescending { it.appointmentDate }
                } else {
                    errorMessage = resp.message()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load bookings"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Please login first"
            isLoading = false
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navHostController = navHostController) {
                scope.launch { drawerState.close() }
            }
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Bookings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            painter = painterResource(R.drawable.menu),
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PatientPrimary
                )
            )
        },
        bottomBar = {
            MainBottomBar(navHostController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs for filtering
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Completed") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Cancelled") }
                )
            }
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PatientPrimary)
                    }
                }
                
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navHostController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                
                else -> {
                    val filteredBookings = when (selectedTab) {
                        0 -> bookings.filter { it.status == BookingStatus.PENDING || it.status == BookingStatus.CONFIRMED }
                        1 -> bookings.filter { it.status == BookingStatus.COMPLETED }
                        2 -> bookings.filter { it.status == BookingStatus.CANCELLED }
                        else -> bookings
                    }
                    
                    if (filteredBookings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No bookings yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { navHostController.navigate("Home") }) {
                                    Text("Browse Doctors")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredBookings) { booking ->
                                BookingCard(
                                    booking = booking,
                                    onCancelClick = {
                                        scope.launch {
                                            val token = TokenManager.getToken(context)
                                            if (token != null) {
                                                try {
                                                    val req = com.sentrive.reliefnet.network.models.BookingRequestBuilder.cancelBooking(
                                                        cancelledBy = "patient"
                                                    )
                                                    val resp = repository.cancelBooking(booking.id, req)
                                                    if (resp.isSuccessful) {
                                                        val userId = TokenManager.getUserId(context)
                                                        if (userId != null) {
                                                            val refreshed = repository.getPatientBookings(userId)
                                                            if (refreshed.isSuccessful) {
                                                                bookings = refreshed.body()?.bookings?.sortedByDescending { it.appointmentDate } ?: emptyList()
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) { }
                                            }
                                        }
                                    },
                                    onRescheduleClick = {
                                        // Navigate to booking screen with doctor ID to reschedule
                                        navHostController.navigate("Booking/${booking.doctorId}")
                                    },
                                    onJoinCallClick = {
                                        val selfId = TokenManager.getUserId(context)
                                        if (!selfId.isNullOrBlank()) {
                                            navHostController.navigate("VideoCallScreen/${'$'}selfId/${'$'}{booking.doctorId}/true/video")
                                        }
                                    },
                                    onAudioCallClick = {
                                        val selfId = TokenManager.getUserId(context)
                                        if (!selfId.isNullOrBlank()) {
                                            navHostController.navigate("VideoCallScreen/${'$'}selfId/${'$'}{booking.doctorId}/true/audio")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    onCancelClick: () -> Unit,
    onRescheduleClick: () -> Unit = {},
    onJoinCallClick: () -> Unit = {},
    onAudioCallClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${booking.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(status = booking.status)
            }
            
            HorizontalDivider()
            
            // Date & Time
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = formatDate(booking.appointmentDate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = "${booking.appointmentTime} • ${booking.duration} min",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Notes
            if (!booking.notes.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = booking.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Payment status
            if (booking.paymentStatus != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (booking.paymentStatus == PaymentStatus.PAID) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (booking.paymentStatus == PaymentStatus.PAID) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Payment: ${booking.paymentStatus.toDisplayString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Action buttons for upcoming bookings
            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAudioCallClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Audio Call")
                    }

                    Button(
                        onClick = onJoinCallClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join Call")
                    }
                    
                    OutlinedButton(
                        onClick = onRescheduleClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reschedule")
                    }
                    
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: BookingStatus) {
    val (backgroundColor, textColor, displayText) = when (status) {
        BookingStatus.PENDING, BookingStatus.CONFIRMED -> Triple(
            Color(0xFFE3F2FD),
            Color(0xFF1976D2),
            "Scheduled"
        )
        BookingStatus.COMPLETED -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF388E3C),
            "Completed"
        )
        BookingStatus.CANCELLED -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Cancelled"
        )
        BookingStatus.NO_SHOW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "No Show"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
