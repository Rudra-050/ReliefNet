package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.ui.theme.alegreyaFontFamily
import android.content.Intent
import android.net.Uri

data class EmergencyContact(
    val name: String,
    val description: String,
    val number: String,
    val backgroundColor: Color,
    val available: String = "24/7"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    
    val emergencyContacts = listOf(
        EmergencyContact(
            name = "National Suicide Prevention Lifeline",
            description = "Free, confidential support for people in distress",
            number = "988",
            backgroundColor = Color(0xFFEF5350)
        ),
        EmergencyContact(
            name = "Crisis Text Line",
            description = "Text HOME to connect with a Crisis Counselor",
            number = "741741",
            backgroundColor = Color(0xFFEC407A)
        ),
        EmergencyContact(
            name = "SAMHSA National Helpline",
            description = "Treatment referral and information service",
            number = "1-800-662-4357",
            backgroundColor = Color(0xFFAB47BC)
        ),
        EmergencyContact(
            name = "Disaster Distress Helpline",
            description = "Crisis counseling for natural disasters",
            number = "1-800-985-5990",
            backgroundColor = Color(0xFF5C6BC0)
        ),
        EmergencyContact(
            name = "Veterans Crisis Line",
            description = "Support for veterans and their families",
            number = "988 (Press 1)",
            backgroundColor = Color(0xFF42A5F5)
        ),
        EmergencyContact(
            name = "National Domestic Violence Hotline",
            description = "Support for domestic violence victims",
            number = "1-800-799-7233",
            backgroundColor = Color(0xFF26A69A)
        ),
        EmergencyContact(
            name = "Emergency Services",
            description = "Police, Fire, Medical emergencies",
            number = "911",
            backgroundColor = Color(0xFFFF5722),
            available = "Emergency Only"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Emergency Contact",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W600
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEF9A9A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                MainBottomBar(navHostController)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
        ) {
            // Important Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ Important Notice",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                            color = Color(0xFFE65100)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If you're experiencing a life-threatening emergency, please call 911 immediately. These helplines provide support but are not emergency services.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF424242)
                        )
                    )
                }
            }

            // Emergency Contacts List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(emergencyContacts.size) { index ->
                    EmergencyContactCard(
                        contact = emergencyContacts[index],
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${emergencyContacts[index].number}")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCall() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Contact info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF616161)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = contact.backgroundColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contact.number,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                            color = contact.backgroundColor
                        )
                    )
                }
                Text(
                    text = "Available: ${contact.available}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                )
            }

            // Right side - Call button
            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = contact.backgroundColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color.White
                )
            }
        }
    }
}
