package com.sentrive.reliefnet.testing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.repository.ReliefNetRepository
import kotlinx.coroutines.launch

/**
 * Simple test screen to verify API connection
 * Add this to your navigation to test the connection
 * 
 * Usage: Navigate to this screen to test if backend is working
 */
@Composable
fun ApiTestScreen() {
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var resultColor by remember { mutableStateOf(Color.Black) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "API Connection Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResult = "Testing connection..."
                    resultColor = Color.Gray
                    
                    repository.getDoctors()
                        .onSuccess { doctorList ->
                            doctors = doctorList
                            testResult = "✅ SUCCESS! Connected to backend.\nFetched ${doctorList.size} doctors"
                            resultColor = Color.Green
                        }
                        .onFailure { error ->
                            testResult = "❌ FAILED: ${error.message}\n\n" +
                                    "Possible issues:\n" +
                                    "1. Backend server not running\n" +
                                    "2. Wrong IP address (check ApiConfig.kt)\n" +
                                    "3. Network connectivity issue"
                            resultColor = Color.Red
                        }
                    
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Testing..." else "Test Backend Connection")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        }
        
        testResult?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = resultColor.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = resultColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (doctors.isNotEmpty()) {
            Text("Doctors from Backend:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(doctors) { doctor ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = doctor.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = doctor.specialization ?: "No specialization",
                                style = MaterialTheme.typography.bodySmall
                            )
                            doctor.rating.let { rating ->
                                Text(
                                    text = "⭐ $rating (${doctor.reviewCount} reviews)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Backend Server: http://localhost:5000\n" +
                   "Make sure the Node.js server is running!",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
