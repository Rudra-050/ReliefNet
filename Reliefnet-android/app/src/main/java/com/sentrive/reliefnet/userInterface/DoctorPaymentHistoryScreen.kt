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
import com.sentrive.reliefnet.network.models.DoctorPaymentItem
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun DoctorPaymentHistoryScreen(nav: NavHostController? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var payments by remember { mutableStateOf<List<DoctorPaymentItem>>(emptyList()) }
    var totalPaid by remember { mutableStateOf(0.0) }
    var totalPending by remember { mutableStateOf(0.0) }
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
                    val resp = RetrofitClient.apiService.getDoctorPayments("Bearer $token")
                    if (resp.isSuccessful) {
                        resp.body()?.let { payments = it.payments; totalPaid = it.totalPaid; totalPending = it.totalPending }
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
            title = { Text("Payment History") },
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
                payments.forEach { pay ->
                    ListItem(
                        headlineContent = { Text("₹${pay.amount} • ${pay.status}") },
                        supportingContent = { Text(pay.createdAt) }
                    )
                    HorizontalDivider()
                }
                Spacer(Modifier.height(16.dp))
                Text("Total Paid: ₹$totalPaid")
                Text("Total Pending: ₹$totalPending")
            }
        }
    }
    }
}
