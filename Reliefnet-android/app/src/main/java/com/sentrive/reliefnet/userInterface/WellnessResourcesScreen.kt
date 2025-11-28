package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.ui.theme.*

data class WellnessResource(
    val title: String,
    val description: String,
    val category: String,
    val duration: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessResourcesScreen(navHostController: NavHostController) {
    var selectedCategory by remember { mutableStateOf("All") }
    
    val categories = listOf("All", "Articles", "Videos", "Meditation", "Self-Help")
    
    val resources = listOf(
        WellnessResource(
            title = "Understanding Anxiety",
            description = "Learn about anxiety disorders and coping strategies",
            category = "Articles",
            duration = "5 min read",
            icon = Icons.Default.Article,
            backgroundColor = Color(0xFF64B5F6)
        ),
        WellnessResource(
            title = "Guided Meditation for Sleep",
            description = "Relaxing meditation to help you fall asleep peacefully",
            category = "Meditation",
            duration = "15 min",
            icon = Icons.Default.SelfImprovement,
            backgroundColor = Color(0xFF81C784)
        ),
        WellnessResource(
            title = "Managing Depression",
            description = "Expert advice on recognizing and managing depression",
            category = "Videos",
            duration = "12 min watch",
            icon = Icons.Default.PlayCircle,
            backgroundColor = Color(0xFFBA68C8)
        ),
        WellnessResource(
            title = "Stress Reduction Techniques",
            description = "Practical techniques to reduce daily stress",
            category = "Self-Help",
            duration = "8 min read",
            icon = Icons.Default.FavoriteBorder,
            backgroundColor = Color(0xFFFFB74D)
        ),
        WellnessResource(
            title = "Breathing Exercises",
            description = "Simple breathing exercises for anxiety relief",
            category = "Meditation",
            duration = "10 min",
            icon = Icons.Default.SelfImprovement,
            backgroundColor = Color(0xFF4DB6AC)
        ),
        WellnessResource(
            title = "Building Resilience",
            description = "How to develop emotional resilience in difficult times",
            category = "Articles",
            duration = "7 min read",
            icon = Icons.Default.Article,
            backgroundColor = Color(0xFFE57373)
        ),
        WellnessResource(
            title = "Mindfulness Meditation",
            description = "Introduction to mindfulness and present-moment awareness",
            category = "Videos",
            duration = "20 min watch",
            icon = Icons.Default.PlayCircle,
            backgroundColor = Color(0xFF9575CD)
        ),
        WellnessResource(
            title = "Self-Care Checklist",
            description = "Daily self-care activities for better mental health",
            category = "Self-Help",
            duration = "Quick reference",
            icon = Icons.Default.FavoriteBorder,
            backgroundColor = Color(0xFFF06292)
        ),
        WellnessResource(
            title = "Cognitive Behavioral Therapy Basics",
            description = "Understanding CBT and how it can help you",
            category = "Articles",
            duration = "10 min read",
            icon = Icons.Default.Article,
            backgroundColor = Color(0xFF4FC3F7)
        ),
        WellnessResource(
            title = "Body Scan Meditation",
            description = "Progressive relaxation technique for stress relief",
            category = "Meditation",
            duration = "25 min",
            icon = Icons.Default.SelfImprovement,
            backgroundColor = Color(0xFFAED581)
        )
    )

    val filteredResources = if (selectedCategory == "All") {
        resources
    } else {
        resources.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wellness Resources",
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
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PatientPrimary,
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
            // Category Filter
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = alegreyaFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W600
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = alegreyaFontFamily
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Resources List
                items(filteredResources.size) { index ->
                    WellnessResourceCard(
                        resource = filteredResources[index],
                        onClick = {
                            // Could navigate to detail screen or open content
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WellnessResourceCard(
    resource: WellnessResource,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = resource.backgroundColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resource.icon,
                    contentDescription = resource.category,
                    tint = resource.backgroundColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resource.description,
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
                    Text(
                        text = resource.category,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 12.sp,
                            color = resource.backgroundColor,
                            fontWeight = FontWeight.W500
                        )
                    )
                    Text(
                        text = " • ${resource.duration}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    )
                }
            }
        }
    }
}
