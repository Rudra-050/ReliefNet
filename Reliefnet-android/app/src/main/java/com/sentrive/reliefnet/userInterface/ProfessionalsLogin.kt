package com.sentrive.reliefnet.userInterface

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
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
fun ProfessionalLoginScreen(navHostController: NavHostController? = null){
    Box(Modifier.fillMaxSize()){
        Image(painter = painterResource(R.drawable.bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Column (Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally){
            LogoWithText("Healthcare Professional Portal")
            Spacer(Modifier.height(20.dp))
            ProfessionalsLoginColumn(navHostController)
        }

    }
}
@Composable
fun ProfessionalsLoginColumn(navHostController: NavHostController? = null){
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var medicalId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    var columnWidth = screenWidth - 20
    
    // Handle login
    fun handleLogin() {
        // Validation
        when {
            email.isBlank() -> {
                errorMessage = "Please enter your email"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errorMessage = "Please enter a valid email address"
                return
            }
            medicalId.isBlank() -> {
                errorMessage = "Please enter your Medical ID"
                return
            }
            password.isBlank() -> {
                errorMessage = "Please enter your password"
                return
            }
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            repository.loginDoctor(
                medicalId = medicalId,
                password = password,
                email = email
            )
                .onSuccess { response ->
                    // Save token and user info
                    TokenManager.saveToken(context, response.token)
                    // Set global interceptor token for repository calls
                    com.sentrive.reliefnet.network.RetrofitClient.authToken = response.token
                    val doctor = response.doctor
                    if (doctor != null) {
                        // Use id if available, otherwise use email as fallback
                        val doctorId = doctor.id ?: doctor.email
                        TokenManager.saveUserInfo(
                            context,
                            userId = doctorId,
                            userType = "Doctor",
                            name = doctor.name,
                            email = doctor.email
                        )
                    } else {
                        // Some backends may return user instead of doctor on login
                        response.user?.let { u ->
                            val userId = u.id ?: u.email
                            TokenManager.saveUserInfo(
                                context,
                                userId = userId,
                                userType = "Doctor",
                                name = u.name,
                                email = u.email
                            )
                        }
                    }
                    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                    navHostController?.navigate("DoctorDashboard") {
                        popUpTo("ProfessionalLoginScreen") { inclusive = true }
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Login failed"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    isLoading = false
                }
        }
    }

    Column (Modifier.width(columnWidth.dp),
        horizontalAlignment = Alignment.CenterHorizontally){
        Box(){

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Doctor/Nurse Login",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                )
                Text(
                    "Access your professional dashboard",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.W400,
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = {
                            Text(
                                "Professional Email",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    fontFamily = interFontFamily
                                ),
                                color = Color(0xFF9E9E9E)
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedTextField(
                        value = medicalId,
                        onValueChange = { medicalId = it },
                        placeholder = {
                            Text(
                                "Medical ID",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    fontFamily = interFontFamily
                                ),
                                color = Color(0xFF9E9E9E)
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text(
                                "Password",
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
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                
                // Error Message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                OutlinedButton(
                    onClick = { handleLogin() },
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
                            "Sign in",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.W500,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Row {
                    Text("Don't have an account? ",
                        fontSize = 14.sp,
                        color = Color.White)
                            Text("Register here",
                                color = Color.Magenta,
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable(onClick = {
                                    navHostController?.navigate("DoctorRegistrationScreen")
                                }))
                }
                Row {
                    Text("Forgot Password?",
                        fontSize = 14.sp,
                        color = Color.Magenta,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(
                            onClick = {
                            }
                        ))
                }
                Row { Text("By signing in, you agree to our ",
                    fontSize = 14.sp,
                    color = Color.White)
                                    Text("Terms of Service",
                                        fontSize = 14.sp,
                                        color = Color.Magenta,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable(
                                            onClick = {
                                            }
                                        ))
                }
                Row { Text("and ",
                    fontSize = 14.sp,
                    color = Color.White)
                                    Text("Privacy Policy",
                                        fontSize = 14.sp,
                                        color = Color.Magenta,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable(
                                            onClick = {}
                                        ))
                }
            }
        }

    }
}
@Preview
@Composable
fun A(){
    ProfessionalLoginScreen()
}