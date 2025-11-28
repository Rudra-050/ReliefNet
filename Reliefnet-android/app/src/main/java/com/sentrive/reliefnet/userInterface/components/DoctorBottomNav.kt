package com.sentrive.reliefnet.userInterface.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.ui.theme.*

@Composable
fun DoctorBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    NavigationBar(
        containerColor = DoctorBackground,
        contentColor = DoctorPrimary
    ) {
        NavigationBarItem(
            selected = currentRoute == "DoctorDashboard",
            onClick = { 
                if (currentRoute != "DoctorDashboard") {
                    navController.navigate("DoctorDashboard") {
                        popUpTo("DoctorDashboard") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "DoctorChats",
            onClick = { 
                if (currentRoute != "DoctorChats") {
                    navController.navigate("DoctorChats") {
                        popUpTo("DoctorDashboard") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Chat, "Chats") },
            label = { Text("Chats") }
        )
        NavigationBarItem(
            selected = currentRoute == "DoctorSessions",
            onClick = { 
                if (currentRoute != "DoctorSessions") {
                    navController.navigate("DoctorSessions") {
                        popUpTo("DoctorDashboard") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.CalendarToday, "Sessions") },
            label = { Text("Sessions") }
        )
        NavigationBarItem(
            selected = currentRoute == "DoctorAccountProfile",
            onClick = { 
                if (currentRoute != "DoctorAccountProfile") {
                    navController.navigate("DoctorAccountProfile") {
                        popUpTo("DoctorDashboard") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile") }
        )
    }
}
