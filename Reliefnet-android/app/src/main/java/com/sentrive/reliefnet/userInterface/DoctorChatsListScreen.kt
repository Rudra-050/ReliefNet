@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.DoctorChatSummary
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import com.sentrive.reliefnet.userInterface.components.DoctorBottomNavigationBar
import kotlinx.coroutines.launch

@Composable
fun DoctorChatsListScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DoctorChatSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token.isNullOrBlank()) {
                    error = "Not authenticated. Please login again."
                    loading = false
                    return@launch
                }
                
                println("DoctorChatsListScreen: Fetching chats with token: ${token.take(20)}...")
                val resp = RetrofitClient.apiService.getDoctorChats("Bearer $token")
                
                if (resp.isSuccessful) {
                    val body = resp.body()
                    println("DoctorChatsListScreen: Success! Conversations count: ${body?.conversations?.size}")
                    items = body?.conversations ?: emptyList()
                    if (items.isEmpty()) {
                        error = "No patient conversations yet. Chats will appear here when patients message you."
                    }
                } else {
                    val errorBody = resp.errorBody()?.string()
                    val errorMsg = "Failed to load chats (${resp.code()}): ${resp.message()}\n${errorBody ?: ""}"
                    println("DoctorChatsListScreen: Error - $errorMsg")
                    error = errorMsg
                }
            } catch (e: Exception) { 
                val errorMsg = "Network error: ${e.message ?: e.javaClass.simpleName}"
                println("DoctorChatsListScreen: Exception - $errorMsg")
                e.printStackTrace()
                error = errorMsg
            }
            loading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            nav?.let { controller ->
                AppDrawer(navHostController = controller) {
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Chats", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            painter = painterResource(R.drawable.menu),
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DoctorPrimary
                )
            ) 
        },
        bottomBar = {
            nav?.let { controller ->
                DoctorBottomNavigationBar(
                    navController = controller,
                    currentRoute = "DoctorChats"
                )
            }
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DoctorPrimary)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚠️ Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    val token = TokenManager.getToken(context)
                                    if (token.isNullOrBlank()) {
                                        error = "Not authenticated. Please login again."
                                        loading = false
                                        return@launch
                                    }
                                    val resp = RetrofitClient.apiService.getDoctorChats("Bearer $token")
                                    if (resp.isSuccessful) {
                                        items = resp.body()?.conversations ?: emptyList()
                                        if (items.isEmpty()) {
                                            error = "No patient conversations yet."
                                        }
                                    } else {
                                        error = "Failed (${resp.code()}): ${resp.message()}\n${resp.errorBody()?.string() ?: ""}"
                                    }
                                } catch (e: Exception) {
                                    error = "Network error: ${e.message ?: e.javaClass.simpleName}"
                                }
                                loading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorPrimary)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                else -> {
                    Text("Patient Chats", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                "No conversations yet\nPatient chats will appear here",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items.forEach { c ->
                            ListItem(
                                headlineContent = { Text(c.patientName ?: "Patient") },
                                supportingContent = { Text(c.lastMessage ?: "No messages") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nav?.navigate("DoctorChatScreen") }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
    }
}
