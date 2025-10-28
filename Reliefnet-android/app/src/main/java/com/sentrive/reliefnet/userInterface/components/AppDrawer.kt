package com.sentrive.reliefnet.userInterface.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sentrive.reliefnet.utils.TokenManager

@Composable
fun AppDrawer(
    navHostController: NavHostController,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val name = TokenManager.getUserName(context) ?: "Guest"
    val userType = (TokenManager.getUserType(context) ?: "User").lowercase()
    val photoUrl = TokenManager.getUserPhotoUrl(context)

    ModalDrawerSheet {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEE5FF))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                // Fallback circle avatar with initial
                Row(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB39DDB)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "R",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = userType.replaceFirstChar { it.uppercase() }, color = Color.DarkGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Items
        NavigationDrawerItem(
            label = { Text("Profile") },
            selected = false,
            onClick = {
                val route = if (userType == "doctor") "DoctorAccountProfile" else "ProfileScreen"
                navHostController.navigate(route)
                onClose()
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Notifications") },
            selected = false,
            onClick = {
                navHostController.navigate("Notifications")
                onClose()
            },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Help & Support") },
            selected = false,
            onClick = {
                val route = if (userType == "doctor") "DoctorHelp" else "HelpSupport"
                navHostController.navigate(route)
                onClose()
            },
            icon = { Icon(Icons.Default.Help, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Services") },
            selected = false,
            onClick = {
                navHostController.navigate("DiscoverScreen")
                onClose()
            },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Divider(Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            onClick = {
                // Clear user data first
                TokenManager.clearUserData(context)
                // Close drawer
                onClose()
                // Navigate to UserTypeSelection and clear entire back stack
                navHostController.navigate("UserTypeSelection") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
