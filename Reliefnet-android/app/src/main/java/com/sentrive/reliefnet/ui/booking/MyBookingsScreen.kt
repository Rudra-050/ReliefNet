package com.sentrive.reliefnet.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentrive.reliefnet.network.models.Booking
import com.sentrive.reliefnet.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * My Bookings Screen
 * Shows list of user's bookings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    patientId: String,
    onBookingClick: (String) -> Unit, // Navigate to booking details
    onBack: () -> Unit,
    viewModel: BookingViewModel = viewModel()
) {
    val bookingState by viewModel.bookingState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Upcoming", "Completed", "Cancelled")
    val statusFilters = listOf("confirmed", "completed", "cancelled")
    
    var showCancelDialog by remember { mutableStateOf(false) }
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    
    // Load bookings
    LaunchedEffect(selectedTab) {
        viewModel.loadPatientBookings(patientId, statusFilters[selectedTab])
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Appointments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Bookings List
            when (val state = bookingState) {
                is BookingViewModel.BookingState.Loading -> {
                    MyBookingsLoadingContent()
                }
                is BookingViewModel.BookingState.BookingsLoaded -> {
                    if (state.bookings.isEmpty()) {
                        EmptyBookingsContent(selectedTab)
                    } else {
                        BookingsList(
                            bookings = state.bookings,
                            onBookingClick = onBookingClick,
                            onCancelClick = { booking ->
                                selectedBooking = booking
                                showCancelDialog = true
                            },
                            showCancelButton = selectedTab == 0 // Only for upcoming
                        )
                    }
                }
                is BookingViewModel.BookingState.Error -> {
                    MyBookingsErrorContent(
                        message = state.message,
                        onRetry = {
                            viewModel.loadPatientBookings(patientId, statusFilters[selectedTab])
                        }
                    )
                }
                else -> {}
            }
        }
    }
    
    // Cancel Confirmation Dialog
    if (showCancelDialog && selectedBooking != null) {
        CancelBookingDialog(
            booking = selectedBooking!!,
            onConfirm = { reason ->
                viewModel.cancelBooking(selectedBooking!!.id, reason)
                showCancelDialog = false
                selectedBooking = null
            },
            onDismiss = {
                showCancelDialog = false
                selectedBooking = null
            }
        )
    }
}

@Composable
fun BookingsList(
    bookings: List<Booking>,
    onBookingClick: (String) -> Unit,
    onCancelClick: (Booking) -> Unit,
    showCancelButton: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bookings) { booking ->
            BookingCard(
                booking = booking,
                onClick = { onBookingClick(booking.id) },
                onCancelClick = { onCancelClick(booking) },
                showCancelButton = showCancelButton
            )
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    onClick: () -> Unit,
    onCancelClick: () -> Unit,
    showCancelButton: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Doctor name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        booking.doctorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        booking.sessionType ?: "Consultation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusBadge(status = booking.status.name.lowercase())
            }
            
            Divider()
            
            // Appointment Details
            InfoRowWithIcon(
                icon = Icons.Default.CalendarToday,
                label = "Date",
                value = formatDate(booking.appointmentDate)
            )
            
            InfoRowWithIcon(
                icon = Icons.Default.AccessTime,
                label = "Time",
                value = booking.appointmentTime
            )
            
            InfoRowWithIcon(
                icon = Icons.Default.AttachMoney,
                label = "Amount",
                value = "₹${booking.amount.toInt()}"
            )
            
            if (!booking.reasonForVisit.isNullOrEmpty()) {
                InfoRowWithIcon(
                    icon = Icons.Default.Description,
                    label = "Reason",
                    value = booking.reasonForVisit
                )
            }
            
            // Action Buttons
            if (showCancelButton && booking.status.name.lowercase() == "confirmed") {
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Details")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status.lowercase()) {
        "confirmed" -> Pair(Color(0xFF4CAF50), "Confirmed")
        "pending" -> Pair(Color(0xFFFFA000), "Pending")
        "completed" -> Pair(Color(0xFF2196F3), "Completed")
        "cancelled" -> Pair(Color(0xFFF44336), "Cancelled")
        else -> Pair(Color.Gray, status)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun InfoRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyBookingsContent(selectedTab: Int) {
    val message = when (selectedTab) {
        0 -> "No upcoming appointments"
        1 -> "No completed appointments"
        2 -> "No cancelled appointments"
        else -> "No appointments"
    }
    
    val description = when (selectedTab) {
        0 -> "Book an appointment with a doctor to get started"
        1 -> "Your completed appointments will appear here"
        2 -> "Your cancelled appointments will appear here"
        else -> ""
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MyBookingsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MyBookingsErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Error Loading Bookings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun CancelBookingDialog(
    booking: Booking,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var cancelReason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Appointment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Are you sure you want to cancel your appointment with ${booking.doctorName}?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = cancelReason,
                    onValueChange = { cancelReason = it },
                    label = { Text("Reason (Optional)") },
                    placeholder = { Text("Why are you cancelling?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Text(
                    "Note: Refund will be processed within 5-7 business days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cancelReason) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Appointment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Appointment")
            }
        }
    )
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
