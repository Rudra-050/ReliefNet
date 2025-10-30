package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.cantataOneFontFamily
import com.sentrive.reliefnet.ui.theme.inriaSerifFontFamily


@Composable
fun SplashScreen(navHostController: NavHostController) {

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

        // Foreground content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LogoWithText("Bridging Care, Compassion, and Connection")
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            navHostController.navigate("LinerarProgress"){
                popUpTo("SplashScreen") { inclusive =true }
            }
        }
    }
}
@Composable
fun LogoWithText(logoText: String){

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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

        // ReliefNet Title (matching the image style)
        Text(
            text = "ReliefNet",
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
            text = logoText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = cantataOneFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center
        )
    }
}

