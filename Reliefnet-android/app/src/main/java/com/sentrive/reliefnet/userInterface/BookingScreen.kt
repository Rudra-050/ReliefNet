package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import android.graphics.Typeface
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.network.models.Session
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(navHostController: NavHostController, doctorId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()

    var doctor by remember { mutableStateOf<Doctor?>(null) }
    var availableSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var bookingId by remember { mutableStateOf<String?>(null) }

    // Fetch doctor details and available sessions
    LaunchedEffect(doctorId) {
        repository.getDoctorById(doctorId)
            .onSuccess { doc -> doctor = doc }
            .onFailure { error -> errorMessage = error.message ?: "Failed to load doctor" }
        repository.getSessions(doctorId = doctorId, status = "available", token = TokenManager.getToken(context) ?: "")
            .onSuccess { sessions ->
                availableSessions = sessions.sortedBy { "${it.sessionDate} ${it.sessionTime}" }
                isLoading = false
            }
            .onFailure { error ->
                errorMessage = error.message ?: "Failed to load sessions"
                isLoading = false
            }
    }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PatientPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PatientPrimary)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                doctor?.let { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PatientBackground),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = doc.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                AndroidView(factory = { context ->
                                    TextView(context).apply {
                                        text = "Dr. ${doc.name}"
                                        textSize = 22f
                                        setTypeface(null, Typeface.BOLD)
                                    }
                                })
                                Spacer(modifier = Modifier.height(4.dp))
                                AndroidView(factory = { context ->
                                    TextView(context).apply {
                                        text = doc.specialization
                                        textSize = 16f
                                    }
                                })
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.star_on),
                                        contentDescription = "Rating",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFC107)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AndroidView(factory = { context ->
                                        TextView(context).apply {
                                            text = "${doc.rating ?: 4.5} (${doc.reviewCount ?: 120}+ reviews)"
                                            textSize = 14f
                                        }
                                    })
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AndroidView(factory = { context ->
                                    TextView(context).apply {
                                        text = "₹${doc.price.toInt()} per session"
                                        textSize = 18f
                                        setTypeface(null, Typeface.BOLD)
                                        setTextColor(android.graphics.Color.parseColor("#6200EE"))
                                    }
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = "Select Time Slot", style = MaterialTheme.typography.titleMedium)
                if (availableSessions.isEmpty()) {
                    Text(text = "No available slots", style = MaterialTheme.typography.bodyMedium)
                } else {
                    availableSessions.forEach { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedSession?.id == session.id) PatientPrimary else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = { selectedSession = session }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${session.sessionDate} ${session.sessionTime}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selectedSession?.id == session.id) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Duration: ${session.duration ?: 60} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedSession?.id == session.id) Color.White.copy(alpha = 0.9f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Any specific requirements?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        selectedSession?.let { session ->
                            scope.launch {
                                val token = TokenManager.getToken(context) ?: ""
                                val patientId = TokenManager.getUserId(context) ?: ""
                                repository.createSession(
                                    patientId = patientId,
                                    doctorId = doctorId,
                                    sessionDate = session.sessionDate,
                                    sessionTime = session.sessionTime,
                                    duration = session.duration ?: 60,
                                    notes = notes,
                                    token = token
                                )
                                    .onSuccess { booking ->
                                        bookingId = booking.id
                                        showSuccessDialog = true
                                    }
                                    .onFailure { error ->
                                        errorMessage = error.message ?: "Booking failed"
                                    }
                            }
                        }
                    },
                    enabled = selectedSession != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PatientPrimary)
                ) {
                    Text(text = "Confirm Booking", color = Color.White)
                }
            }
        }
    }
}