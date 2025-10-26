@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.DoctorChatSummary
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.userInterface.components.AppDrawer
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
                    error = "Not authenticated"
                } else {
                    val resp = RetrofitClient.apiService.getDoctorChats("Bearer $token")
                    if (resp.isSuccessful) {
                        items = resp.body()?.conversations ?: emptyList()
                    } else error = resp.message()
                }
            } catch (e: Exception) { error = e.message }
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
    Scaffold(topBar = { 
        CenterAlignedTopAppBar(
            title = { Text("Chats") },
            navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        painter = painterResource(R.drawable.menu),
                        contentDescription = "Menu"
                    )
                }
            }
        ) 
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            if (loading) CircularProgressIndicator()
            else if (error != null) Text("Error: $error")
            else {
                Text("Patient Chats", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                items.forEach { c ->
                    ListItem(
                        headlineContent = { Text(c.patientName ?: "Patient") },
                        supportingContent = { Text(c.lastMessage ?: "") },
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
