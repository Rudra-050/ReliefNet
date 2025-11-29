@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.Booking
import com.sentrive.reliefnet.network.models.toDisplayString
import com.sentrive.reliefnet.network.models.getColor
import com.sentrive.reliefnet.ui.theme.*
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Personal Information Screen
@Composable
fun PersonalInformationScreen(navHostController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.sentrive.reliefnet.viewmodel.UserProfileViewModel>()
    val userProfileState = userProfileViewModel.userProfile.collectAsState()
    val user = userProfileState.value
    
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(user) {
        user?.let {
            name = it.name
            email = it.email
            phone = it.phone ?: ""
            location = it.location ?: ""
        }
    }

    // Image picker for profile photo
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val token = TokenManager.getToken(context)
                    if (!token.isNullOrBlank()) {
                        // Read bytes from selected image
                        val input = context.contentResolver.openInputStream(uri)
                        val bytes = input?.readBytes()
                        input?.close()
                        if (bytes != null) {
                            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData(
                                name = "file",
                                filename = "profile_${System.currentTimeMillis()}.jpg",
                                body = requestBody
                            )
                            // Set token for interceptor
                            RetrofitClient.authToken = token
                            val upload = RetrofitClient.apiService.uploadPhoto(part, "Bearer $token")
                            if (upload.isSuccessful) {
                                val url = upload.body()?.url
                                if (!url.isNullOrBlank()) {
                                    val update = RetrofitClient.apiService.updatePatientProfile(
                                        mapOf("photoUrl" to url)
                                    )
                                    if (update.isSuccessful) {
                                        userProfileViewModel.fetchUserProfile(context)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Personal Information",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = inriaSerifFontFamily,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PatientPrimary
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = { MainBottomBar(navHostController) }
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                Box(
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = user?.photoUrl ?: "",
                        contentDescription = "Profile Photo",
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.profile_pic),
                        error = painterResource(R.drawable.profile_pic)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    border = BorderStroke(1.dp, PatientPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PatientPrimary)
                ) {
                    Text("Change Photo")
                }
                
                Spacer(Modifier.height(24.dp))

                if (isEditing) {
                    // Edit Mode
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                loading = true
                                scope.launch {
                                    try {
                                        val token = TokenManager.getToken(context)
                                        if (!token.isNullOrBlank()) {
                                            // Set token for interceptor
                                            RetrofitClient.authToken = token
                                            val body = mapOf(
                                                "name" to name,
                                                "phone" to phone,
                                                "location" to location
                                            )
                                            val resp = RetrofitClient.apiService.updatePatientProfile(body)
                                            if (resp.isSuccessful) {
                                                userProfileViewModel.fetchUserProfile(context)
                                                isEditing = false
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = PatientPrimary)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                } else {
                    // View Mode
                    InfoCard("Name", name)
                    Spacer(Modifier.height(12.dp))
                    InfoCard("Email", email)
                    Spacer(Modifier.height(12.dp))
                    InfoCard("Phone", phone.ifBlank { "Not provided" })
                    Spacer(Modifier.height(12.dp))
                    InfoCard("Location", location.ifBlank { "Not provided" })
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PatientPrimary)
                    ) {
                        Text("Edit Profile", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(label: String, value: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PatientPrimary),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontFamily = alegreyaSansFontFamily,
                    color = Color.Gray
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontFamily = mitrFontFamily,
                    fontWeight = FontWeight.W500,
                    color = Color.Black
                )
            )
        }
    }
}

// Payment History Screen
@Composable
fun PaymentHistoryScreen(navHostController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var payments by remember { mutableStateOf<List<com.sentrive.reliefnet.network.models.PaymentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrBlank()) {
                    error = "Not authenticated"
                } else {
                    RetrofitClient.authToken = token
                    val resp = RetrofitClient.apiService.getPatientPayments("Bearer $token")
                    if (resp.isSuccessful) {
                        payments = resp.body()?.payments ?: emptyList()
                    } else error = resp.message()
                }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Payment History",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = inriaSerifFontFamily,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PatientPrimary
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = { MainBottomBar(navHostController) }
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PatientPrimary)
                        }
                    }
                    error != null -> {
                        Text("Error: $error", color = Color.Red)
                    }
                    payments.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No payment history yet", color = Color.Gray)
                        }
                    }
                    else -> {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            payments.forEach { payment ->
                                PaymentCard(payment)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentCard(payment: com.sentrive.reliefnet.network.models.PaymentItem) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PatientBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "₹${payment.amount}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 20.sp,
                        fontFamily = inriaSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    payment.status,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontFamily = mitrFontFamily,
                        color = if (payment.status == "succeeded") PatientAccent else Color(0xFFFFA726)
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                payment.createdAt,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontFamily = alegreyaSansFontFamily,
                    color = Color.Gray
                )
            )
        }
    }
}

// Your Bookings Screen
@Composable
fun YourBookingsScreen(navHostController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrBlank()) {
                    error = "Not authenticated"
                } else {
                    RetrofitClient.authToken = token
                    val resp = RetrofitClient.apiService.getPatientBookingsLegacy("Bearer $token")
                    if (resp.isSuccessful) {
                        bookings = resp.body()?.bookings ?: emptyList()
                    } else error = resp.message()
                }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Your Bookings",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = inriaSerifFontFamily,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = { MainBottomBar(navHostController) }
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PatientPrimary)
                        }
                    }
                    error != null -> {
                        Text("Error: $error", color = Color.Red)
                    }
                    bookings.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No bookings yet", color = Color.Gray)
                        }
                    }
                    else -> {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            bookings.forEach { booking ->
                                BookingCard(booking)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PatientBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    booking.doctorName ?: "Professional",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp,
                        fontFamily = inriaSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    booking.status.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontFamily = mitrFontFamily,
                        color = Color(booking.status.getColor())
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Date: ${booking.appointmentDate}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = mitrFontFamily,
                    color = Color.Black
                )
            )
            Text(
                "Time: ${booking.appointmentTime}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = mitrFontFamily,
                    color = Color.Black
                )
            )
            if (!booking.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Notes: ${booking.notes}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontFamily = alegreyaSansFontFamily,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}

// Help & Support Screen
@Composable
fun HelpSupportScreen(navHostController: NavHostController) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Help & Support",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = inriaSerifFontFamily,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = { MainBottomBar(navHostController) }
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Frequently Asked Questions",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 20.sp,
                        fontFamily = inriaSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                FAQCard("How do I book a session?", "Navigate to the Doctors section, select a professional, and choose your preferred time slot.")
                Spacer(Modifier.height(12.dp))
                
                FAQCard("How do I cancel a booking?", "Go to 'Your Bookings', select the booking you want to cancel, and click the cancel button.")
                Spacer(Modifier.height(12.dp))
                
                FAQCard("How do I contact a doctor?", "You can chat with your booked doctor through the Chat section.")
                Spacer(Modifier.height(12.dp))
                
                FAQCard("What payment methods are accepted?", "We accept UPI payments through Google Pay, PhonePe, Paytm, and all major UPI apps.")
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    "Contact Us",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 20.sp,
                        fontFamily = inriaSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB3D6))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Email: support@nirogsetu.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Phone: +91 98765 43210",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Available: 24/7",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    "Crisis Helplines",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 20.sp,
                        fontFamily = inriaSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "🆘 If you're in crisis, please call:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "National Suicide Prevention: 988",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Emergency Services: 911",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mental Health Helpline: 1-800-273-8255",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontFamily = mitrFontFamily,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FAQCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontFamily = mitrFontFamily,
                        fontWeight = FontWeight.W500,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "−" else "+", fontSize = 24.sp)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    answer,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontFamily = alegreyaSansFontFamily,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}
