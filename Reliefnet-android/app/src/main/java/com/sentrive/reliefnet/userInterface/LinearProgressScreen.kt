package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import androidx.compose.ui.platform.LocalContext
import com.sentrive.reliefnet.utils.TokenManager
import com.sentrive.reliefnet.ui.theme.*
import androidx.compose.ui.graphics.Color

@Composable
fun LinearProgress(navHostController: NavHostController){
    val context = LocalContext.current


    Box(Modifier.fillMaxSize()
        ){
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
            Row(Modifier.width(134.dp)) {
                LinearProgressIndicator(
                    color = PatientPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1200)
            val isLoggedIn = TokenManager.isLoggedIn(context)
            val userType = TokenManager.getUserType(context)
            val destination = when {
                isLoggedIn && userType.equals("Doctor", ignoreCase = true) -> "DoctorDashboard"
                isLoggedIn -> "Home"
                else -> "UserTypeSelection" // align with docs; will fallback in nav if not present
            }
            navHostController.navigate(destination){
                popUpTo("LinerarProgress") { inclusive = true }
            }
        }

    }
}
//@Preview
//@Composable
//fun show(){
//   LoginScreen()
//}
