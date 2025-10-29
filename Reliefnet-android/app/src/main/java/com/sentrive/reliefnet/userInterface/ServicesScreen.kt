package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.alegreyaFontFamily

// Data class for Service
data class Service(
    val name: String,
    val description: String,
    val backgroundColor: Color,
    val imageRes: Int,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(navHostController: NavHostController) {
    val services = listOf(
        Service(
            name = "Mental Health Support",
            description = "Connect with certified mental health professionals",
            backgroundColor = Color(0xFFB39DDB),
            imageRes = R.drawable.individual1,
            route = "MentalHealthSupport"
        ),
        Service(
            name = "Emergency Contact",
            description = "24/7 crisis helpline and emergency support",
            backgroundColor = Color(0xFFEF9A9A),
            imageRes = R.drawable.child1,
            route = "Home" // Placeholder - can be implemented later
        ),
        Service(
            name = "Therapy Sessions",
            description = "Book individual or group therapy sessions",
            backgroundColor = Color(0xFF80CBC4),
            imageRes = R.drawable.couple1,
            route = "BookingMain"
        ),
        Service(
            name = "Wellness Resources",
            description = "Articles, videos, and self-help guides",
            backgroundColor = Color(0xFFFFF59D),
            imageRes = R.drawable.teen1,
            route = "Home" // Placeholder - can be implemented later
        ),
        Service(
            name = "Support Groups",
            description = "Join community support groups and forums",
            backgroundColor = Color(0xFFCE93D8),
            imageRes = R.drawable.individual1,
            route = "Home" // Placeholder - can be implemented later
        ),
        Service(
            name = "Chat with Doctors",
            description = "Real-time chat with healthcare professionals",
            backgroundColor = Color(0xFF90CAF9),
            imageRes = R.drawable.child1,
            route = "DiscoverScreen"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Our Services",
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
                    containerColor = Color(0xFFFAD6FF),
                    titleContentColor = Color(0xFF1A1A1A)
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
                .background(Color.White)
        ) {
            // Subtitle
            Text(
                text = "Choose from our comprehensive healthcare services",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = alegreyaFontFamily,
                    fontSize = 16.sp,
                    color = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Services Grid
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val cardWidth = screenWidth - 32

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(services.size) { index ->
                    ServiceCard(
                        service = services[index],
                        cardWidth = cardWidth.dp,
                        onClick = {
                            navHostController.navigate(services[index].route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: Service,
    cardWidth: Dp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = service.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W600,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
            
            Image(
                painter = painterResource(id = service.imageRes),
                contentDescription = service.name,
                modifier = Modifier
                    .size(80.dp)
                    .padding(start = 8.dp)
            )
        }
    }
}
