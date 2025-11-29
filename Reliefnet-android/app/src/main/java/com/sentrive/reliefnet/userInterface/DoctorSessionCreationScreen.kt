@file:OptIn(ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DoctorSessionCreationScreen(navHostController: NavHostController) {
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(60) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Available Session", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DoctorPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
                OutlinedTextField(
                    value = duration.toString(),
                    onValueChange = { d -> duration = d.toIntOrNull() ?: 60 },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Call backend to create doctor session
                        errorMessage = null
                        success = false
                        val token = TokenManager.getToken(context)
                        val userType = TokenManager.getUserType(context)
                        if (token.isNullOrBlank() || userType == null || !userType.equals("doctor", ignoreCase = true)) {
                            errorMessage = "You must be logged in as a doctor to create sessions"
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val result = repository.createDoctorSession(
                                date = date,
                                time = time,
                                duration = duration,
                                type = "consultation",
                                token = token
                            )
                            isLoading = false
                            result.onSuccess {
                                success = true
                                // Optionally navigate back to sessions list
                                navHostController?.popBackStack()
                            }.onFailure { err ->
                                // Show detailed server message when available
                                errorMessage = err.message ?: "Failed to create session"
                            }
                        }
                    },
                    enabled = date.isNotBlank() && time.isNotBlank() && duration > 0 && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                ) {
                    Text("Create Session", color = Color.White)
                }
                if (isLoading) CircularProgressIndicator(color = DoctorPrimary)
                if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                if (success) Text("Session created!", color = MaterialTheme.colorScheme.primary)
        }
    }
}
