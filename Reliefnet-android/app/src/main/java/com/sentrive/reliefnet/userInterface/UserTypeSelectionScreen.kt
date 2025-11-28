package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.inriaSerifFontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock

@Composable
fun UserTypeSelectionScreen(navHostController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo with gradient background circle (matching the image)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFE1BEE7),
                                Color(0xFFBA68C8),
                                Color(0xFF9C27B0)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Name (matching the image style)
            Text(
                text = "NirogSetu",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = inriaSerifFontFamily,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline (matching the image style)
            Text(
                text = "Bridging Care, Compassion, and Connection",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Title
            Text(
                text = "Login Portal",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Patient Button (enhanced)
            UserTypeCard(
                title = "Patient",
                subtitle = "Continue as a patient",
                description = "Find professionals and book secure sessions",
                leadingEmoji = "👤",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFB3E5FC), Color(0xFF81D4FA))
                ),
                onClick = {
                    navHostController.navigate("LoginScreen") {
                        popUpTo("UserTypeSelection") { inclusive = true }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Professional Button (enhanced)
            UserTypeCard(
                title = "Healthcare Professional",
                subtitle = "Continue as a provider",
                description = "Manage sessions, patients, and earnings",
                leadingEmoji = "🏥",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8))
                ),
                onClick = {
                    navHostController.navigate("ProfessionalLoginScreen") {
                        popUpTo("UserTypeSelection") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun UserTypeCard(
    title: String,
    subtitle: String,
    description: String,
    leadingEmoji: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading emblem with gradient ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(gradient)
                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leadingEmoji,
                    fontSize = 30.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secure • Encrypted • Private",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // Trailing chevron
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF374151)
                )
            }
        }
    }
}
