@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.DoctorProfile
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import com.sentrive.reliefnet.userInterface.components.DoctorBottomNavigationBar
import kotlinx.coroutines.launch

@Composable
fun DoctorAccountProfileScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var profile by remember { mutableStateOf<DoctorProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrBlank()) {
                    error = "Not authenticated"
                } else {
                    RetrofitClient.authToken = token
                    val resp = RetrofitClient.apiService.getDoctorProfile("Bearer $token")
                    if (resp.isSuccessful) profile = resp.body() else error = resp.message()
                }
            } catch (e: Exception) { error = e.message }
        }
    }

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Profile", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DoctorPrimary
                )
            ) 
        },
        bottomBar = {
            nav?.let { controller ->
                DoctorBottomNavigationBar(
                    navController = controller,
                    currentRoute = "DoctorAccountProfile"
                )
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (profile == null && error == null) CircularProgressIndicator(color = DoctorPrimary)
            error?.let { Text("Error: $it", color = Color.Red) }
            profile?.let { d ->
                Text(d.name, style = MaterialTheme.typography.headlineSmall)
                Text(d.specialization ?: d.specialty ?: "")
                Text(d.location ?: "")
                Spacer(Modifier.height(8.dp))
                // Medical ID row with copy to clipboard
                val clipboard = LocalClipboardManager.current
                if (!d.medicalId.isNullOrBlank()) {
                    Text("Medical ID", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.medicalId ?: "")
                        TextButton(onClick = { clipboard.setText(AnnotatedString(d.medicalId ?: "")) }) {
                            Text("Copy")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { nav?.navigate("EditDoctorProfile") },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) { Text("Edit Profile", color = Color.White) }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { nav?.navigate("DoctorSessions") },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) { Text("Sessions", color = Color.White) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav?.navigate("DoctorFeedback") },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) { Text("Feedbacks", color = Color.White) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav?.navigate("DoctorPayments") },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) { Text("Payment History", color = Color.White) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav?.navigate("DoctorHelp") },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) { Text("Help & Support", color = Color.White) }
            }
        }
    }
}

@Composable
fun EditDoctorProfileScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = TokenManager.getToken(context)
            if (!token.isNullOrBlank()) {
                RetrofitClient.authToken = token
                val resp = RetrofitClient.apiService.getDoctorProfile("Bearer $token")
                resp.body()?.let { name = it.name; specialization = it.specialization ?: it.specialty ?: ""; bio = it.bio ?: "" }
            }
        }
    }

    Scaffold(topBar = { 
        CenterAlignedTopAppBar(
            title = { Text("Edit Profile", color = Color.White) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = DoctorPrimary
            )
        ) 
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(specialization, { specialization = it }, label = { Text("Specialization") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(bio, { bio = it }, label = { Text("About") }, modifier = Modifier.fillMaxWidth(), minLines = 3, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
            Spacer(Modifier.height(16.dp))
            Button(enabled = !loading, onClick = {
                loading = true
                scope.launch {
                    try {
                        val token = TokenManager.getToken(context)
                        if (!token.isNullOrBlank()) {
                            RetrofitClient.authToken = token
                            val body = mapOf("name" to name, "specialization" to specialization, "bio" to bio)
                            val resp = RetrofitClient.apiService.updateDoctorProfile(body, "Bearer $token")
                            if (resp.isSuccessful) nav?.popBackStack() else loading = false
                        } else loading = false
                    } catch (_: Exception) { loading = false }
                }
            }) { Text("Save") }
        }
    }
}

@Composable
fun DoctorSessionsScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var bookings by remember { mutableStateOf<List<com.sentrive.reliefnet.network.models.Booking>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var cancelingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                val doctorId = TokenManager.getUserId(context)
                if (token.isNullOrBlank() || doctorId.isNullOrBlank()) {
                    error = "Not authenticated"
                } else {
                    val resp = RetrofitClient.apiService.getDoctorBookings(
                        doctorId = doctorId,
                        token = "Bearer $token"
                    )
                    if (resp.isSuccessful) {
                        bookings = resp.body()?.bookings ?: emptyList()
                    } else error = resp.message()
                }
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Bookings", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DoctorPrimary
                )
            ) 
        },
        bottomBar = {
            nav?.let { controller ->
                DoctorBottomNavigationBar(
                    navController = controller,
                    currentRoute = "DoctorSessions"
                )
            }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            if (loading) CircularProgressIndicator(color = DoctorPrimary)
            else if (error != null) Text("Error: $error")
            else if (bookings.isEmpty()) Text("No bookings yet")
            else bookings.forEach { booking ->
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Patient: ${booking.patientName}", style = MaterialTheme.typography.titleMedium)
                        Text("Status: ${booking.status}")
                        Text("Date: ${booking.appointmentDate} at ${booking.appointmentTime}")
                        Text("Duration: ${booking.duration} min")
                        if (!booking.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Notes: ${booking.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // Join Call button (doctor initiates)
                            Button(
                                onClick = {
                                    val selfId = TokenManager.getUserId(context)
                                    if (!selfId.isNullOrBlank()) {
                                        nav?.navigate("VideoCallScreen/${'$'}selfId/${'$'}{booking.patientId}/true/video")
                                    }
                                }
                            ) {
                                Text("Join Call")
                            }

                            // Audio Call button
                            Button(
                                onClick = {
                                    val selfId = TokenManager.getUserId(context)
                                    if (!selfId.isNullOrBlank()) {
                                        nav?.navigate("VideoCallScreen/${'$'}selfId/${'$'}{booking.patientId}/true/audio")
                                    }
                                }
                            ) {
                                Text("Audio Call")
                            }

                            // Cancel button
                            Button(
                                enabled = cancelingId != booking.id && (booking.status == com.sentrive.reliefnet.network.models.BookingStatus.PENDING || booking.status == com.sentrive.reliefnet.network.models.BookingStatus.CONFIRMED),
                                onClick = {
                                    cancelingId = booking.id
                                    scope.launch {
                                        try {
                                            val token = TokenManager.getToken(context)
                                            if (!token.isNullOrBlank()) {
                                                RetrofitClient.authToken = token
                                            }
                                            val req = mapOf("cancelledBy" to "doctor")
                                            val resp = RetrofitClient.apiService.cancelBooking(booking.id, req, "Bearer $token")
                                            if (resp.isSuccessful) {
                                                val doctorId = TokenManager.getUserId(context)
                                                val refresh = RetrofitClient.apiService.getDoctorBookings(doctorId = doctorId ?: "", token = "Bearer $token")
                                                if (refresh.isSuccessful) bookings = refresh.body()?.bookings ?: emptyList() else error = refresh.message()
                                            } else error = resp.message()
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                        cancelingId = null
                                    }
                                }) {
                                if (cancelingId == booking.id) CircularProgressIndicator(Modifier.size(18.dp), color = DoctorPrimary)
                                else Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorHelpSupportScreen() {
    Scaffold(topBar = { 
        CenterAlignedTopAppBar(
            title = { Text("Help & Support", color = Color.White) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = DoctorPrimary
            )
        ) 
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Text("Frequently Asked Questions")
            Spacer(Modifier.height(12.dp))
            Text("Contact: support@nirogsetu.com\n+91 98765 43210")
        }
    }
}
