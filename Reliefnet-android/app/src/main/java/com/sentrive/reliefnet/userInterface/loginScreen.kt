package com.sentrive.reliefnet.userInterface

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.auth.GoogleAuthActivity
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.ui.theme.interFontFamily
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navHostController: NavHostController){
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()

    // 🛡️ Ensure Firebase is initialized
    LaunchedEffect(Unit) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var medicalId by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoginMode by remember { mutableStateOf(true) } // true = login, false = register
    var name by remember { mutableStateOf("") }
    val loginSuccess = remember { mutableStateOf(false) }
    var userType by remember { mutableStateOf("patient") } // "patient" or "doctor"
    var loginMethod by remember { mutableStateOf("password") } // "password" or "otp"
    var otpSent by remember { mutableStateOf(false) }
    var testOtp by remember { mutableStateOf<String?>(null) } // For testing

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn(context)) {
            val savedType = TokenManager.getUserType(context)
            val destination = if (savedType.equals("Doctor", ignoreCase = true)) "DoctorDashboard" else "Home"
            navHostController.navigate(destination) {
                popUpTo("LoginScreen") { inclusive = true }
            }
        }
    }

    // 🛡️ Avoid direct call until initialization
    val currentUser = remember {
        try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: IllegalStateException) {
            null
        }
    }

    if (currentUser != null) {
        LaunchedEffect(Unit) {
            val savedType = TokenManager.getUserType(context)
            val destination = if (savedType.equals("Doctor", ignoreCase = true)) "DoctorDashboard" else "Home"
            navHostController.navigate(destination) {
                popUpTo("LoginScreen") { inclusive = true }
            }
        }
        return
    }

    // 2️⃣ Launcher for GoogleAuthActivity
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loginSuccess.value = true
        }
    }
    LaunchedEffect(loginSuccess.value) {
        if (loginSuccess.value) {
            navHostController.navigate("Home") {
                popUpTo("LoginScreen") { inclusive = true }
            }
        }
    }

    // Handle OTP send
    fun handleSendOTP() {
        if (email.isBlank()) {
            errorMessage = "Please enter your email"
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            repository.sendOTP(email)
                .onSuccess { response ->
                    otpSent = true
                    testOtp = response.testOtp // For testing only
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    if (testOtp != null) {
                        Toast.makeText(context, "Test OTP: $testOtp", Toast.LENGTH_LONG).show()
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to send OTP"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            
            isLoading = false
        }
    }
    
    // Handle OTP verification
    fun handleVerifyOTP() {
        if (otpCode.isBlank()) {
            errorMessage = "Please enter the OTP code"
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            repository.verifyOTP(email, otpCode)
                .onSuccess { response ->
                    // Save token and user info
                    TokenManager.saveToken(context, response.token)
                    // Set global interceptor token for repository calls
                    com.sentrive.reliefnet.network.RetrofitClient.authToken = response.token
                    response.user?.let { user ->
                        val userId = user.id ?: user.email
                        TokenManager.saveUserInfo(
                            context,
                            userId = userId,
                            userType = "User",
                            name = user.name,
                            email = user.email,
                            photoUrl = user.photoUrl
                        )
                    }
                    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                    val dest = if (TokenManager.getUserType(context).equals("Doctor", true)) "DoctorDashboard" else "Home"
                    navHostController.navigate(dest) {
                        popUpTo("LoginScreen") { inclusive = true }
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Invalid OTP"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            
            isLoading = false
        }
    }

    // Handle login/register
    fun handleAuth() {
        // Validation based on user type and mode
        if (userType == "doctor" && isLoginMode) {
            // Doctor login requires medicalId and password
            if (medicalId.isBlank() || password.isBlank()) {
                errorMessage = "Please fill in Medical ID and password"
                return
            }
        } else if (userType == "patient") {
            // Patient login/register requires email and password
            if (email.isBlank() || password.isBlank()) {
                errorMessage = "Please fill in all fields"
                return
            }
            
            if (!isLoginMode && name.isBlank()) {
                errorMessage = "Please enter your name"
                return
            }
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            if (isLoginMode) {
                // Login based on user type
                val result = if (userType == "patient") {
                    repository.loginPatient(email, password)
                } else {
                    repository.loginDoctor(medicalId, password)
                }
                
                result.onSuccess { response ->
                        // Save token and user info
                        TokenManager.saveToken(context, response.token)
                        val savedType = if (userType == "patient") "User" else "Doctor"
                        // Prefer user object; if absent, fallback to doctor object
                        val savedFromUser = response.user?.let { user ->
                            val userId = user.id ?: user.email
                            TokenManager.saveUserInfo(
                                context,
                                userId = userId,
                                userType = savedType,
                                name = user.name,
                                email = user.email,
                                photoUrl = user.photoUrl
                            )
                            true
                        } ?: false
                        if (!savedFromUser) {
                            response.doctor?.let { doc ->
                                val docId = doc.id ?: doc.email
                                TokenManager.saveUserInfo(
                                    context,
                                    userId = docId,
                                    userType = savedType,
                                    name = doc.name,
                                    email = doc.email,
                                    photoUrl = doc.photoUrl
                                )
                            }
                        }
                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                        val dest = if (TokenManager.getUserType(context).equals("Doctor", true)) "DoctorDashboard" else "Home"
                        navHostController.navigate(dest) {
                            popUpTo("LoginScreen") { inclusive = true }
                        }
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Login failed"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
            } else {
                // Register - only patients can register here
                if (userType == "doctor") {
                    // Redirect doctors to doctor registration
                    navHostController.navigate("DoctorRegistrationScreen")
                    isLoading = false
                    return@launch
                }
                
                // Patient registration
                repository.registerPatient(email, password, name)
                    .onSuccess { response ->
                        // Save token and user info
                        TokenManager.saveToken(context, response.token)
                // Set global interceptor token for repository calls
                com.sentrive.reliefnet.network.RetrofitClient.authToken = response.token
                        // Set global interceptor token for repository calls
                        com.sentrive.reliefnet.network.RetrofitClient.authToken = response.token
                        response.user?.let { user ->
                            val userId = user.id ?: user.email
                            TokenManager.saveUserInfo(
                                context,
                                userId = userId,
                                userType = "User",
                                name = user.name,
                                email = user.email,
                                photoUrl = user.photoUrl
                            )
                        }
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                        val dest = if (TokenManager.getUserType(context).equals("Doctor", true)) "DoctorDashboard" else "Home"
                        navHostController.navigate(dest) {
                            popUpTo("LoginScreen") { inclusive = true }
                        }
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Registration failed"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
            }

            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize()
        ){
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column (Modifier.fillMaxWidth()
            , horizontalAlignment = Alignment.CenterHorizontally){

            //Logo and Text function
            LogoWithText("Bridging Care, Compassion, and Connection")

            // User Type Selection Buttons
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .width(300.dp)
                    .height(50.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                // Patient Login Button
                Button(
                    onClick = { userType = "patient" },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userType == "patient") Color.Cyan else Color.White.copy(alpha = 0.7f),
                        contentColor = if (userType == "patient") Color.Black else Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Patient",
                        fontWeight = if (userType == "patient") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                // Doctor Login Button
                Button(
                    onClick = { userType = "doctor" },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userType == "doctor") Color.Cyan else Color.White.copy(alpha = 0.7f),
                        contentColor = if (userType == "doctor") Color.Black else Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Doctor",
                        fontWeight = if (userType == "doctor") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }

            // Login Method Toggle (Password or OTP) - Only for Patient Login
            if (isLoginMode && userType == "patient") {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .width(300.dp)
                        .height(45.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Password Login Button
                    Button(
                        onClick = { 
                            loginMethod = "password"
                            otpSent = false
                            otpCode = ""
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loginMethod == "password") Color(0xFF9C27B0) else Color.White.copy(alpha = 0.7f),
                            contentColor = if (loginMethod == "password") Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Password",
                            fontWeight = if (loginMethod == "password") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }

                    // OTP Login Button
                    Button(
                        onClick = { 
                            loginMethod = "otp"
                            otpSent = false
                            otpCode = ""
                            password = ""
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loginMethod == "otp") Color(0xFF9C27B0) else Color.White.copy(alpha = 0.7f),
                            contentColor = if (loginMethod == "otp") Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "OTP",
                            fontWeight = if (loginMethod == "otp") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column (Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally){

                //Login Screen Upper Text
                Text(if (isLoginMode) "Sign In" else if (userType == "doctor") "Doctor Registration" else "Create an Account",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = interFontFamily,
                    fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isLoginMode) "Enter your credentials to continue" 
                    else if (userType == "doctor") "Please use Doctor Registration form" 
                    else "Enter your details to Sign Up",
                    fontFamily = interFontFamily,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))

                // Name field (only for patient registration)
                if (!isLoginMode && userType == "patient") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text(
                                "Full Name",
                                fontFamily = interFontFamily,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(300.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                //Email Input TextField (only for patient, hide for doctor)
                if (userType == "patient" && (isLoginMode || !isLoginMode)) {
                    OutlinedTextField(value = email,
                        onValueChange = {email = it},
                        placeholder = {Text("email@domain.com",
                            fontFamily = interFontFamily,
                            fontSize = 15.sp,
                            color = Color.Gray)},
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.White,
                            unfocusedContainerColor = Color.White,
                       ),shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(300.dp),
                    )

                    Spacer(Modifier.height(12.dp))
                }

                // Medical ID field (only for doctor login)
                if (isLoginMode && userType == "doctor") {
                    OutlinedTextField(
                        value = medicalId,
                        onValueChange = { medicalId = it },
                        placeholder = {
                            Text(
                                "Medical ID",
                                fontFamily = interFontFamily,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(300.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Password Input TextField (show when password login or patient registration, hide for OTP login)
                if ((isLoginMode && loginMethod == "password") || (!isLoginMode && userType == "patient")) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text(
                                "Password",
                                fontFamily = interFontFamily,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(300.dp),
                )

                    Spacer(Modifier.height(12.dp))
                }

                // OTP Code Input (show only when OTP method selected and OTP has been sent)
                if (isLoginMode && userType == "patient" && loginMethod == "otp" && otpSent) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        placeholder = {
                            Text(
                                "Enter 6-digit OTP",
                                fontFamily = interFontFamily,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(300.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Show message for doctor registration
                if (!isLoginMode && userType == "doctor") {
                    Button(
                        onClick = { navHostController.navigate("DoctorRegistrationScreen") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Cyan
                        ),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Text("Go to Doctor Registration", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    // Error Message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 50.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    //Continue Button (context-aware: Send OTP, Verify OTP, or Sign In)
                    Button(
                        onClick = { 
                            when {
                                // OTP Login Flow
                                isLoginMode && userType == "patient" && loginMethod == "otp" && !otpSent -> handleSendOTP()
                                isLoginMode && userType == "patient" && loginMethod == "otp" && otpSent -> handleVerifyOTP()
                                // Regular password login/register
                                else -> handleAuth()
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Magenta
                        ),
                        modifier = Modifier.width(300.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                when {
                                    isLoginMode && userType == "patient" && loginMethod == "otp" && !otpSent -> "Send OTP"
                                    isLoginMode && userType == "patient" && loginMethod == "otp" && otpSent -> "Verify OTP"
                                    isLoginMode && userType == "doctor" -> "Doctor Sign In"
                                    isLoginMode -> "Patient Sign In"
                                    else -> "Sign Up"
                                },
                                fontFamily = interFontFamily,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    // Resend OTP button (only show when OTP sent)
                    if (isLoginMode && userType == "patient" && loginMethod == "otp" && otpSent) {
                        Button(
                            onClick = { 
                                otpCode = ""
                                handleSendOTP()
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.width(300.dp)
                        ) {
                            Text(
                                "Resend OTP",
                                fontFamily = interFontFamily,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Toggle between login and register
                    Button(
                        onClick = { 
                            isLoginMode = !isLoginMode
                            errorMessage = null
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Text(
                            if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                            fontFamily = interFontFamily,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                //Divider
               Row(Modifier.width(300.dp),
                   verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color.Black
                    )
                   Text("or",
                       fontFamily = interFontFamily,
                       color = Color.Gray,
                       fontSize = 12.sp,
                       modifier = Modifier.padding(horizontal = 16.dp))
                   HorizontalDivider(Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color.Black
                    )
                }

                Spacer(Modifier.height(16.dp))
                //Google Login
                Button(onClick = {
                    val intent = Intent(context, GoogleAuthActivity::class.java)
                    launcher.launch(intent)
                },
                    modifier = Modifier.width(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black

                    )) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                            ){
                        Image(
                            painter = painterResource(R.drawable.googlelogo),
                            contentDescription = "googleLogin",
                                    modifier = Modifier.height(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Google",fontFamily = interFontFamily,)
                    }
                }
                Spacer(Modifier.height(8.dp))
                //Apple Login
                Button(onClick = {},
                    modifier = Modifier.width(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.applelogo),
                            contentDescription = "googleLogin",
                            modifier = Modifier.height(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Apple",fontFamily = interFontFamily,)
                    }
                }
            }
        }


        }
    }
