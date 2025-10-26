@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun DoctorDashboardScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val doctorName = TokenManager.getUserName(context) ?: "Doctor"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navHostController = navHostController) {
                scope.launch { drawerState.close() }
            }
        }
    ) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Doctor Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            painter = painterResource(R.drawable.menu),
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // Notification bell icon
                    IconButton(onClick = { navHostController.navigate("Notifications") }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on dashboard */ },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navHostController.navigate("DoctorChats") },
                    icon = { Icon(Icons.Default.Chat, "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navHostController.navigate("DoctorSessions") },
                    icon = { Icon(Icons.Default.CalendarToday, "Sessions") },
                    label = { Text("Sessions") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        // Navigate only to DoctorAccountProfile when profile icon is clicked
                        navHostController.navigate("DoctorAccountProfile")
                    },
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(R.drawable.bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome, Dr. $doctorName",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Text(
                    text = "Your professional dashboard",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(32.dp))

                // Quick action cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Chat,
                        label = "Chats",
                        onClick = { navHostController.navigate("DoctorChats") }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Feedback,
                        label = "Feedback",
                        onClick = { navHostController.navigate("DoctorFeedback") }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Payment,
                        label = "Payments",
                        onClick = { navHostController.navigate("DoctorPayments") }
                    )
                    QuickActionCard(
                        icon = Icons.Default.Help,
                        label = "Help",
                        onClick = { navHostController.navigate("DoctorHelp") }
                    )
                }

                // Add button to create doctor session
                IconButton(onClick = { navHostController.navigate("DoctorSessionCreation") }) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Add Session",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
