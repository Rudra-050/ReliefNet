@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.DoctorFeedbackItem
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun DoctorFeedbackScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DoctorFeedbackItem>>(emptyList()) }
    var avg by remember { mutableStateOf(0.0) }
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
                    val resp = RetrofitClient.apiService.getDoctorFeedback("Bearer $token")
                    if (resp.isSuccessful) {
                        resp.body()?.let { avg = it.averageRating; items = it.feedback }
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
            title = { Text("Patient Feedback") },
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
            else items.forEach { f ->
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(f.patientName ?: "Anonymous", style = MaterialTheme.typography.titleMedium)
                        Text("★ ${f.rating}")
                        if (!f.comment.isNullOrBlank()) Text("\"${f.comment}\"")
                    }
                }
            }
        }
    }
    }
}
