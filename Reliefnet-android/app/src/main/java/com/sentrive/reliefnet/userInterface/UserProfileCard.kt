package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sentrive.reliefnet.ui.theme.*
import com.sentrive.reliefnet.viewmodel.UserProfileViewModel

/**
 * User Profile Card - Displays user information from Google/OTP login
 * Shows: Profile photo, name, email, location
 */
@Composable
fun UserProfileCard(
    viewModel: UserProfileViewModel = viewModel(),
    width: Dp,
    bgColor: Long = 0xFFFAD6FF
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Fetch profile on first composition
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile(context)
    }

    Box(modifier = Modifier.padding(top = 15.dp)) {
        Card(
            modifier = Modifier
                .width(width)
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(bgColor)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PatientPrimary)
                }
            } else {
                // User Info - No photo, just text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Name
                    Text(
                        text = if (userProfile?.name?.isNotEmpty() == true) {
                            "Welcome, ${userProfile?.name?.split(" ")?.firstOrNull() ?: "User"}!"
                        } else {
                            "Welcome to NirogSetu!"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = alegreyaFontFamily
                        ),
                        color = Color(0xFF2D1B4E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    userProfile?.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontFamily = alegreyaSansFontFamily
                            ),
                            color = Color(0xFF5A4570)
                        )
                    }

                    // Location (if available)
                    userProfile?.location?.let { location ->
                        if (location.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 $location",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontFamily = alegreyaSansFontFamily
                                ),
                                color = Color(0xFF7A6590)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Profile Photo Component - Shows user photo or default icon
 */
@Composable
fun ProfilePhoto(
    photoUrl: String?,
    size: Dp = 60.dp
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(photoUrl)
            .crossfade(true)
            .build()
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Color(0xFFE0D4F7), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PatientPrimary
                )
            }
            else -> {
                // Default icon when no photo or error
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Profile",
                    modifier = Modifier.size(size * 0.6f),
                    tint = Color(0xFFB8A6D9)
                )
            }
        }
    }
}

/**
 * Simple Welcome Card (fallback for when profile isn't loaded)
 */
@Composable
fun SimpleWelcomeCard(
    userName: String? = null,
    width: Dp,
    bgColor: Long = 0xFFFAD6FF
) {
    Box(modifier = Modifier.padding(top = 15.dp)) {
        Card(
            modifier = Modifier
                .width(width)
                .height(97.dp),
            colors = CardDefaults.cardColors(containerColor = Color(bgColor))
        ) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Row {
                    Text(
                        text = if (userName != null) "Welcome, $userName!" else "Welcome to NirogSetu",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = alegreyaFontFamily
                        )
                    )
                }
                Text(
                    "Bridging Care, Compassion and ",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = alegreyaSansFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp
                    )
                )
                Text(
                    "connection",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = alegreyaSansFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}
