package com.sentrive.reliefnet.userInterface

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.ui.theme.interFontFamily
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.launch


@Composable
fun DoctorRegistrationScreen(navHostController: NavHostController? = null){


    Box(Modifier.fillMaxSize()){
        Image(painter = painterResource(R.drawable.bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Column (Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally){
            LogoWithText("Healthcare Professional Portal")
            Spacer(Modifier.height(16.dp))
            RegistrationBox(navHostController)
        }
    }
}

@Composable
fun RegistrationBox(navHostController: NavHostController? = null){
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var medicalId by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedSpecialization by remember { mutableStateOf("Counselor") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMedicalIdDialog by remember { mutableStateOf(false) }
    var generatedMedicalId by remember { mutableStateOf<String?>(null) }

    val fontSize = 12.dp
    val doctorSpecializations = listOf(
        "Psychologist",
        "Therapist",
        "Psychiatrist",
        "Counselor",
        "Clinical Psychologist",
        "Mental Health Specialist"
    )

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    var columnWidth =screenWidth - 20

    // Handle registration
    fun handleRegistration() {
        // Clear previous error
        errorMessage = null
        
        // Validation
        when {
            firstName.isBlank() || lastName.isBlank() -> {
                errorMessage = "Please enter your full name"
                return
            }
            email.isBlank() -> {
                errorMessage = "Please enter your email"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errorMessage = "Please enter a valid email address"
                return
            }
            password.isBlank() -> {
                errorMessage = "Please enter a password"
                return
            }
            password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                return
            }
            phoneNumber.isBlank() -> {
                errorMessage = "Please enter your phone number"
                return
            }
            phoneNumber.length < 10 -> {
                errorMessage = "Please enter a valid phone number"
                return
            }
            selectedSpecialization == "Selected Specialization" || selectedSpecialization.isBlank() -> {
                errorMessage = "Please select your specialization"
                return
            }
            hospitalName.isBlank() -> {
                errorMessage = "Please enter your hospital/institute name"
                return
            }
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                repository.registerDoctor(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phoneNumber,
                    specialization = selectedSpecialization,
                    hospital = hospitalName
                )
                    .onSuccess { response ->
                        isLoading = false
                        
                        // Save token and user info
                        TokenManager.saveToken(context, response.token)
                        // Set global interceptor token for repository calls
                        com.sentrive.reliefnet.network.RetrofitClient.authToken = response.token
                        
                        if (response.doctor != null) {
                            val doctor = response.doctor
                            // Use id if available, otherwise use email as fallback
                            val doctorId = doctor.id ?: doctor.email
                            TokenManager.saveUserInfo(
                                context,
                                userId = doctorId,
                                userType = "Doctor",
                                name = doctor.name,
                                email = doctor.email,
                                photoUrl = doctor.photoUrl
                            )
                            
                            // Check if Medical ID was generated
                            if (!doctor.medicalId.isNullOrBlank()) {
                                generatedMedicalId = doctor.medicalId
                                showMedicalIdDialog = true
                            } else {
                                // No medical ID, navigate directly
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                navHostController?.navigate("DoctorDashboard") {
                                    popUpTo("DoctorRegistrationScreen") { inclusive = true }
                                }
                            }
                        } else {
                            // No doctor data in response
                            errorMessage = "Registration succeeded but doctor data is missing"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    .onFailure { error ->
                        isLoading = false
                        errorMessage = error.message ?: "Registration failed"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Unexpected error occurred"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }


    Column(Modifier.width(columnWidth.dp),
    horizontalAlignment = Alignment.CenterHorizontally) {
    Box(){
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Text("Create Professional Account",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                ))
            Text("Join Our Network of Healthcare Professionals",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = Color(0xFFECECEC)
                ))
            Text("Personal Information",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF80B2)
                ))
            HorizontalDivider(modifier = Modifier,
                color = Color.LightGray)

            Spacer(Modifier.height(10.dp))

            Row (verticalAlignment = Alignment.CenterVertically,){
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {firstName = it },
                    placeholder = {Text("First Name",
                        style = MaterialTheme.typography.titleSmall.copy(

                            fontSize = 14.sp,
                            fontFamily = interFontFamily
                        ),
                        color = Color(0xFF9E9E9E))
                    },
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = {lastName = it },
                    placeholder = {Text("Last Name",

                        style = MaterialTheme.typography.titleSmall.copy(

                            fontSize = 14.sp,
                            fontFamily = interFontFamily
                        ),
                        color = Color(0xFF9E9E9E))
                    },
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(value = email,
                    onValueChange = {email = it},
                    placeholder = {Text("Professional Email",


                        style = MaterialTheme.typography.titleSmall.copy(

                            fontSize = 14.sp,
                            fontFamily = interFontFamily
                        ),
                        color = Color(0xFF9E9E9E))},
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            "Password (min 6 characters)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontFamily = interFontFamily
                            ),
                            color = Color(0xFF9E9E9E)
                        )
                    },
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(value = phoneNumber,
                    onValueChange = {phoneNumber = it},
                    placeholder = {Text("Phone Number",


                        style = MaterialTheme.typography.titleSmall.copy(

                            fontSize = 14.sp,
                            fontFamily = interFontFamily
                        ),
                        color = Color(0xFF9E9E9E))},
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ))
            }

            Spacer(Modifier.height(14.dp))
            Text("Professional Information",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF80B2)
                ))
            HorizontalDivider(modifier = Modifier,
                color = Color.LightGray)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)) // Apply clipping first
                    .background(Color.White) // Then apply background within the clipped area
                    .padding(horizontal = 16.dp), // Optional: Add horizontal padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedSpecialization,
                    style = MaterialTheme.typography.titleSmall.copy(

                        fontSize = 14.sp,
                        fontFamily = interFontFamily
                    ),
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .weight(1f)
                )

                Box(Modifier.size(24.dp)) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            painter = painterResource(R.drawable.dropdown),
                            contentDescription = "Drop Down",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        doctorSpecializations.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    // Handle selection
                                    selectedSpecialization = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width( 8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(value = hospitalName,
                    onValueChange = {hospitalName = it},
                    placeholder = {Text("Hospital/ Institute Name",
                        style = MaterialTheme.typography.titleSmall.copy(

                            fontSize = 14.sp,
                            fontFamily = interFontFamily
                        ),
                        color = Color(0xFF9E9E9E))},
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ))
            }

            Spacer(Modifier.height(6.dp))
            
            // Error Message
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(6.dp))
            }
            
            OutlinedButton(
                onClick = { handleRegistration() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB6D4)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "Register",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.W500,
                            color = Color.Black
                        )
                    )
                }
            }

        }
    }
    
    // Medical ID Dialog - shows after successful registration
    if (showMedicalIdDialog && generatedMedicalId != null) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing without clicking button */ },
            title = {
                Text(
                    "🎉 Registration Successful!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2196F3)
                )
            },
            text = {
                val clipboard = LocalClipboardManager.current
                Column {
                    Text(
                        "Your Medical ID has been generated:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "MEDICAL ID",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                generatedMedicalId!!,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "⚠️ IMPORTANT: Please save this Medical ID. You will need it to log in to your account.",
                        fontSize = 13.sp,
                        color = Color(0xFFFF6F00),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(generatedMedicalId!!))
                            Toast.makeText(context, "Medical ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F2FD)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Medical ID", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showMedicalIdDialog = false
                        isLoading = false
                        Toast.makeText(context, "Medical ID: $generatedMedicalId", Toast.LENGTH_LONG).show()
                        navHostController?.navigate("DoctorDashboard") {
                            popUpTo("DoctorRegistrationScreen") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I've Saved My Medical ID", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
}
@Preview
@Composable
fun Show(){
   DoctorRegistrationScreen()
}
