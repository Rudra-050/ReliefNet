package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.style.TextAlign
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
import com.sentrive.reliefnet.ui.theme.*
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
    var symptoms by remember { mutableStateOf("") }
    var appointmentType by remember { mutableStateOf("Online Consultation") }
    var isAppointmentTypeExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var bookingId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(currentDateString()) }
    var isBooking by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    val appointmentTypes = listOf("Online Consultation", "In-Person Visit", "Home Visit")
    
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
                title = { Text(text = "Book Appointment", color = Color.White) },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PatientPrimary)
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
                    Button(
                        onClick = { navHostController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = PatientPrimary)
                    ) {
                        Text(text = "Go Back", color = Color.White)
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
                // Select Date section
                item {
                    Text(
                        text = "Select Date",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Month navigation
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = { 
                                currentMonth = (currentMonth.clone() as Calendar).apply { 
                                    add(Calendar.MONTH, -1) 
                                }
                            }) {
                                Icon(Icons.Default.ChevronLeft, "Previous month", tint = Color(0xFF6200EE))
                            }
                            IconButton(onClick = { 
                                currentMonth = (currentMonth.clone() as Calendar).apply { 
                                    add(Calendar.MONTH, 1) 
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, "Next month", tint = Color(0xFF6200EE))
                            }
                        }
                    }
                }
                
                // Calendar grid
                item {
                    CalendarView(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                }
                
                // Select Time Slot section
                item {
                    Text(
                        text = "Select Time Slot",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Time slots grid
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
                    item {
                        val rows = (availableSlots.size / 2) + (availableSlots.size % 2)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height((rows * 70).dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableSlots) { slot ->
                                TimeSlotButton(
                                    slot = slot,
                                    isSelected = selectedSlot == slot,
                                    onClick = { selectedSlot = slot }
                                )
                            }
                        }
                    }
                }
                
                // Appointment Type
                item {
                    Text(
                        text = "Appointment Type",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = isAppointmentTypeExpanded,
                        onExpandedChange = { isAppointmentTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = appointmentType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAppointmentTypeExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF6200EE),
                                focusedLabelColor = Color(0xFF6200EE)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isAppointmentTypeExpanded,
                            onDismissRequest = { isAppointmentTypeExpanded = false }
                        ) {
                            appointmentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        appointmentType = type
                                        isAppointmentTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Symptoms / Reason for Visit
                item {
                    Text(
                        text = "Symptoms / Reason for Visit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        placeholder = { Text("e.g. Fever, Cough, Headache") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                
                // Additional Notes
                item {
                    Text(
                        text = "Additional Notes (Optional)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Any other details for the doctor") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                // Consultation Fee
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consultation Fee:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${doctor?.price?.toInt() ?: 0}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
                
                // Proceed to Payment button
                item {
                    Button(
                        onClick = {
                            // Navigate to payment screen instead of directly processing payment
                            val doc = doctor
                            if (doc != null && selectedSlot != null) {
                                navHostController.navigate(
                                    "payment-screen/${doc.id}/$selectedDate/${selectedSlot!!.startTime}/${selectedSlot!!.endTime}/${doc.price.toInt()}/$appointmentType/${symptoms.ifBlank { "None" }}/${notes.ifBlank { "None" }}"
                                )
                            }
                        },
                        enabled = selectedSlot != null && symptoms.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE)
                        )
                    ) {
                        Text(
                            text = "Proceed to Payment",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotButton(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFE8E8E8),
            contentColor = if (isSelected) Color.White else Color.Black
        )
    ) {
        Text(
            text = "${slot.startTime} -\n${slot.endTime}",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CalendarView(
    currentMonth: Calendar,
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    Column {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar dates
        val calendar = currentMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val weeks = mutableListOf<List<Int?>>()
        var currentWeek = MutableList<Int?>(7) { null }
        
        // Fill first week
        for (i in 0 until firstDayOfWeek) {
            currentWeek[i] = null
        }
        
        for (day in 1..daysInMonth) {
            val dayOfWeek = (firstDayOfWeek + day - 1) % 7
            currentWeek[dayOfWeek] = day
            
            if (dayOfWeek == 6 || day == daysInMonth) {
                weeks.add(currentWeek.toList())
                currentWeek = MutableList(7) { null }
            }
        }
        
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    if (day != null) {
                        val dateStr = String.format(
                            "%04d-%02d-%02d",
                            currentMonth.get(Calendar.YEAR),
                            currentMonth.get(Calendar.MONTH) + 1,
                            day
                        )
                        val isSelected = dateStr == selectedDate
                        val isPast = try {
                            val date = sdf.parse(dateStr)
                            date?.before(Date()) == true
                        } catch (e: Exception) {
                            false
                        }
                        
                        Card(
                            onClick = { if (!isPast) onDateSelected(dateStr) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> Color(0xFF6200EE)
                                    isPast -> Color.LightGray
                                    else -> Color.Transparent
                                }
                            ),
                            shape = RoundedCornerShape(if (isSelected) 50 else 0)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isPast -> Color.Gray
                                        else -> Color.Black
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
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
