package com.sentrive.reliefnet.ui.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.network.models.TimeSlot
import com.sentrive.reliefnet.viewmodel.BookingViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    doctor: Doctor,
    patientId: String,
    patientName: String,
    patientEmail: String,
    patientPhone: String?,
    onBookingSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = viewModel()
) {
    val context = LocalContext.current
    
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var reasonForVisit by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    val availabilityState by viewModel.availabilityState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    
    // Load available slots when date changes
    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            val formattedDate = viewModel.formatDate(date)
            viewModel.loadAvailableSlots(doctor.id ?: "", formattedDate)
        }
    }
    
    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (val state = paymentState) {
            is BookingViewModel.PaymentState.OrderCreated -> {
                // Open payment URL in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.paymentUrl))
                context.startActivity(intent)
            }
            is BookingViewModel.PaymentState.BookingConfirmed -> {
                // Navigate to success screen
                onBookingSuccess(state.booking.id)
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Doctor Info Card
            item {
                DoctorInfoCard(doctor)
            }
            
            // Date Selection
            item {
                Text(
                    "Select Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                DateSelectionRow(
                    selectedDate = selectedDate,
                    onDateSelected = { 
                        selectedDate = it
                        selectedTimeSlot = null // Reset time slot
                    }
                )
            }
            
            // Time Slot Selection
            if (selectedDate != null) {
                item {
                    Text(
                        "Select Time Slot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    when (val state = availabilityState) {
                        is BookingViewModel.AvailabilityState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is BookingViewModel.AvailabilityState.Success -> {
                            if (state.slots.isEmpty()) {
                                NoSlotsAvailableCard()
                            } else {
                                TimeSlotGrid(
                                    slots = state.slots,
                                    selectedSlot = selectedTimeSlot,
                                    onSlotSelected = { selectedTimeSlot = it }
                                )
                            }
                        }
                        is BookingViewModel.AvailabilityState.Error -> {
                            ErrorCard(state.message)
                        }
                        else -> {}
                    }
                }
            }
            
            // Booking Details
            if (selectedTimeSlot != null) {
                item {
                    Text(
                        "Booking Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BookingDetailsForm(
                        reasonForVisit = reasonForVisit,
                        onReasonChange = { reasonForVisit = it },
                        symptoms = symptoms,
                        onSymptomsChange = { symptoms = it }
                    )
                }
                
                // Price Summary
                item {
                    PriceSummaryCard(
                        consultationFee = doctor.price,
                        platformFee = doctor.price * 0.10
                    )
                }
                
                // Book & Pay Button
                item {
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Proceed to Payment - ₹${doctor.price.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    // Payment Confirmation Dialog
    if (showPaymentDialog) {
        PaymentConfirmationDialog(
            doctor = doctor,
            selectedDate = selectedDate!!,
            selectedTimeSlot = selectedTimeSlot!!,
            amount = doctor.price,
            onConfirm = {
                val formattedDate = viewModel.formatDate(selectedDate!!)
                viewModel.createPaymentOrder(
                    amount = doctor.price,
                    doctorId = doctor.id ?: "",
                    appointmentDate = formattedDate,
                    appointmentTime = selectedTimeSlot!!.startTime
                )
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false }
        )
    }
    
    // Loading Dialog
    when {
        paymentState is BookingViewModel.PaymentState.Processing -> {
            LoadingDialog("Creating payment order...")
        }
        paymentState is BookingViewModel.PaymentState.Verifying -> {
            LoadingDialog("Verifying payment...")
        }
        bookingState is BookingViewModel.BookingState.Creating -> {
            LoadingDialog("Creating booking...")
        }
    }
    
    // Error Snackbar
    when {
        paymentState is BookingViewModel.PaymentState.Error -> {
            val error = (paymentState as BookingViewModel.PaymentState.Error).message
            LaunchedEffect(error) {
                // Show error snackbar
            }
        }
        bookingState is BookingViewModel.BookingState.Error -> {
            val error = (bookingState as BookingViewModel.BookingState.Error).message
            LaunchedEffect(error) {
                // Show error snackbar
            }
        }
    }
}

@Composable
fun DoctorInfoCard(doctor: Doctor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Doctor Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doctor.name.first().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Doctor Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = doctor.specialization ?: "General Physician",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${doctor.rating} (${doctor.reviewCount} reviews)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelectionRow(
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    val dates = remember {
        (0..13).map {
            calendar.time.also {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(dates) { date ->
            DateCard(
                date = date,
                isSelected = selectedDate?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) ==
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                } ?: false,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DateCard(
    date: Date,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .width(70.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayFormat.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateFormat.format(date),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = monthFormat.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TimeSlotGrid(
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        slots.chunked(3).forEach { rowSlots ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowSlots.forEach { slot ->
                    TimeSlotChip(
                        slot = slot,
                        isSelected = selectedSlot == slot,
                        onClick = { onSlotSelected(slot) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty spaces
                repeat(3 - rowSlots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TimeSlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = slot.startTime,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = modifier.height(40.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun NoSlotsAvailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "No time slots available for this date. Please select another date.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsForm(
    reasonForVisit: String,
    onReasonChange: (String) -> Unit,
    symptoms: String,
    onSymptomsChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = reasonForVisit,
            onValueChange = onReasonChange,
            label = { Text("Reason for Visit") },
            placeholder = { Text("E.g., Regular checkup, Fever, etc.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
            }
        )
        
        OutlinedTextField(
            value = symptoms,
            onValueChange = onSymptomsChange,
            label = { Text("Symptoms (Optional)") },
            placeholder = { Text("Describe your symptoms...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            leadingIcon = {
                Icon(Icons.Default.MedicalServices, contentDescription = null)
            }
        )
    }
}

@Composable
fun PriceSummaryCard(
    consultationFee: Double,
    platformFee: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Payment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            PriceRow("Consultation Fee", consultationFee)
            PriceRow("Platform Fee (10%)", platformFee, isSubtle = true)
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${consultationFee.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                "Doctor receives ₹${(consultationFee * 0.9).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PriceRow(
    label: String,
    amount: Double,
    isSubtle: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSubtle) 
                MaterialTheme.colorScheme.onSurfaceVariant 
            else 
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            "₹${amount.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSubtle) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@Composable
fun PaymentConfirmationDialog(
    doctor: Doctor,
    selectedDate: Date,
    selectedTimeSlot: TimeSlot,
    amount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Confirm Booking",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "You are booking an appointment with:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    doctor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                InfoRow(Icons.Default.CalendarToday, dateFormat.format(selectedDate))
                InfoRow(Icons.Default.Schedule, selectedTimeSlot.startTime)
                InfoRow(Icons.Default.Payment, "₹${amount.toInt()}")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "You will be redirected to PhonePe payment gateway to complete the payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Proceed to Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(message) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
