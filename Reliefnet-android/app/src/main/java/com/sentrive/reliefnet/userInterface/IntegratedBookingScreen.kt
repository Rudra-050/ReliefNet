package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import android.graphics.Typeface
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.sentrive.reliefnet.network.models.BookingRequestBuilder
import com.sentrive.reliefnet.network.models.CreatePaymentOrderPhonePeRequest
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.network.models.TimeSlot
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.utils.openPhonePePayment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedBookingScreen(
    navHostController: NavHostController,
    doctorId: String
) {
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var doctor by remember { mutableStateOf<Doctor?>(null) }
    var availableSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }
    var selectedSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var bookingId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(currentDateString()) }
    var isBooking by remember { mutableStateOf(false) }
    
    // Fetch doctor details and available slots for selected date
    LaunchedEffect(doctorId, selectedDate) {
        // Fetch doctor
        repository.getDoctorById(doctorId)
            .onSuccess { doc ->
                doctor = doc
            }
            .onFailure { error ->
                errorMessage = error.message ?: "Failed to load doctor"
            }
        
        // Fetch available slots for this doctor and date
        try {
            isLoading = true
            val resp = repository.getAvailableSlots(doctorId, selectedDate)
            if (resp.isSuccessful) {
                val body = resp.body()
                availableSlots = body?.availableSlots?.filter { !it.isBooked }?.sortedBy { it.startTime } ?: emptyList()
                if (availableSlots.isEmpty()) {
                    selectedSlot = null
                }
                errorMessage = null
            } else {
                errorMessage = resp.errorBody()?.string() ?: "Failed to load available slots"
                availableSlots = emptyList()
                selectedSlot = null
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load available slots"
            availableSlots = emptyList()
            selectedSlot = null
        } finally {
            isLoading = false
        }
    }
    
    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "✅ Booking Confirmed!") },
            text = {
                Column {
                    Text(text = "Your session has been booked successfully")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (bookingId != null) {
                        Text(text = "Booking ID: #${bookingId?.take(8)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navHostController.navigate("YourBookings") {
                        popUpTo("Home") { inclusive = false }
                    }
                }) {
                    Text(text = "View My Bookings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navHostController.popBackStack()
                }) {
                    Text(text = "Done")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navHostController.popBackStack() }) {
                        Text(text = "Go Back")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Doctor info
                item {
                    doctor?.let { doc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = doc.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    AndroidView(factory = { context ->
                                        TextView(context).apply {
                                            text = "Dr. ${doc.name}"
                                            textSize = 18f
                                            setTypeface(null, Typeface.BOLD)
                                        }
                                    })
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AndroidView(factory = { context ->
                                        TextView(context).apply {
                                            text = doc.specialization
                                            textSize = 14f
                                        }
                                    })
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AndroidView(factory = { context ->
                                        TextView(context).apply {
                                            text = "₹${doc.price.toInt()} per session"
                                            textSize = 16f
                                            setTypeface(null, Typeface.BOLD)
                                            setTextColor(android.graphics.Color.parseColor("#6200EE"))
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
                
                // Date selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Time Slot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedDate = shiftDate(selectedDate, -1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                            }
                            Text(text = formatDate(selectedDate), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { selectedDate = shiftDate(selectedDate, 1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                            }
                        }
                    }
                }

                // Available time slots
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (availableSlots.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "No available slots", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Try a different day", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    items(availableSlots) { slot ->
                        SlotCard(
                            date = selectedDate,
                            slot = slot,
                            isSelected = (selectedSlot?.startTime == slot.startTime && selectedSlot?.endTime == slot.endTime),
                            onClick = { selectedSlot = slot }
                        )
                    }
                }
                
                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Any specific requirements?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                // Book button
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                isBooking = true
                                try {
                                    val doc = doctor
                                    if (doc == null || selectedSlot == null) {
                                        errorMessage = "Missing required information"
                                        isBooking = false
                                        return@launch
                                    }
                                    
                                    // Create PhonePe payment order
                                    val paymentRequest = CreatePaymentOrderPhonePeRequest(
                                        amount = doc.price,
                                        doctorId = doc.id ?: "",
                                        appointmentDate = selectedDate,
                                        appointmentTime = selectedSlot!!.startTime
                                    )
                                    
                                    val paymentResponse = repository.createPhonePeOrder(paymentRequest)
                                    if (paymentResponse.isSuccessful && paymentResponse.body() != null) {
                                        val paymentBody = paymentResponse.body()!!
                                        val paymentUrl = paymentBody.paymentUrl
                                        
                                        // Store transaction ID for later confirmation
                                        bookingId = paymentBody.merchantTransactionId
                                        
                                        // Launch PhonePe payment
                                        context.openPhonePePayment(paymentUrl)
                                        
                                        // Navigate to payment status screen
                                        navHostController.navigate(
                                            "payment-status/${paymentBody.merchantTransactionId}/${doc.id}/$selectedDate/${selectedSlot!!.startTime}/${notes.ifBlank { "none" }}"
                                        )
                                    } else {
                                        errorMessage = paymentResponse.errorBody()?.string() ?: "Failed to create payment order"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to initiate payment"
                                } finally {
                                    isBooking = false
                                }
                            }
                        },
                        enabled = selectedSlot != null && !isBooking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBooking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pay ${selectedSlot?.startTime ?: ""} - ₹" + (doctor?.price?.toInt()?.toString() ?: "500"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlotCard(
    date: String,
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(text = formatDate(date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "${slot.startTime} - ${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
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

private fun currentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

private fun shiftDate(date: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d = sdf.parse(date) ?: Date()
        val cal = Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, days) }
        sdf.format(cal.time)
    } catch (e: Exception) {
        currentDateString()
    }
}
