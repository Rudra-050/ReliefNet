@file:OptIn(ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.Session
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditSessionScreen(
    sessionId: String,
    nav: NavHostController? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var session by remember { mutableStateOf<Session?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sessionDate by remember { mutableStateOf("") }
    var sessionTime by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(60) }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                val doctorId = TokenManager.getUserId(context)
                if (token.isNullOrBlank() || doctorId.isNullOrBlank()) {
                    error = "Not authenticated"
                } else {
                    RetrofitClient.authToken = token
                    val resp = RetrofitClient.apiService.getSessionById(sessionId, "Bearer $token")
                    if (resp.isSuccessful) {
                        session = resp.body()
                        session?.let {
                            sessionDate = it.sessionDate
                            sessionTime = it.sessionTime
                            duration = it.duration ?: 60
                            notes = it.notes ?: ""
                        }
                    } else error = resp.message()
                }
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Edit Session", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DoctorPrimary
                )
            ) 
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (loading) CircularProgressIndicator(color = DoctorPrimary)
            else if (error != null) Text("Error: $error")
            else session?.let {
                OutlinedTextField(sessionDate, { sessionDate = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sessionTime, { sessionTime = it }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(duration.toString(), { d -> duration = d.toIntOrNull() ?: 60 }, label = { Text("Duration (min)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary),
                    onClick = {
                    saving = true
                    scope.launch {
                        try {
                            val token = TokenManager.getToken(context)
                            RetrofitClient.authToken = token
                            val updateBody = mapOf(
                                "sessionDate" to sessionDate,
                                "sessionTime" to sessionTime,
                                "duration" to duration,
                                "notes" to notes
                            )
                            val resp = RetrofitClient.apiService.updateSession(sessionId, updateBody, "Bearer $token")
                            if (resp.isSuccessful) nav?.popBackStack() else error = resp.message()
                        } catch (e: Exception) { error = e.message }
                        saving = false
                    }
                }) { Text("Save", color = Color.White) }
            }
        }
    }
}
