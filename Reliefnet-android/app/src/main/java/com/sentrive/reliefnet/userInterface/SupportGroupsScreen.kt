package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.ui.theme.alegreyaFontFamily

data class SupportGroup(
    val name: String,
    val description: String,
    val members: Int,
    val type: String, // "Public" or "Private"
    val category: String,
    val meetingSchedule: String,
    val backgroundColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportGroupsScreen(navHostController: NavHostController) {
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filters = listOf("All", "Anxiety", "Depression", "Addiction", "Grief", "PTSD")
    
    val supportGroups = listOf(
        SupportGroup(
            name = "Anxiety Support Circle",
            description = "A safe space to share experiences and coping strategies for anxiety",
            members = 234,
            type = "Public",
            category = "Anxiety",
            meetingSchedule = "Every Monday, 7 PM",
            backgroundColor = Color(0xFF64B5F6)
        ),
        SupportGroup(
            name = "Depression Recovery Group",
            description = "Supporting each other through the journey of recovery",
            members = 189,
            type = "Public",
            category = "Depression",
            meetingSchedule = "Wednesdays, 6 PM",
            backgroundColor = Color(0xFF81C784)
        ),
        SupportGroup(
            name = "Addiction Recovery Community",
            description = "Share your recovery journey in a judgment-free environment",
            members = 156,
            type = "Private",
            category = "Addiction",
            meetingSchedule = "Daily, 8 PM",
            backgroundColor = Color(0xFFBA68C8)
        ),
        SupportGroup(
            name = "Grief Support Network",
            description = "Finding comfort and understanding during difficult times",
            members = 98,
            type = "Public",
            category = "Grief",
            meetingSchedule = "Thursdays, 5 PM",
            backgroundColor = Color(0xFFFFB74D)
        ),
        SupportGroup(
            name = "PTSD Warriors",
            description = "Veterans and trauma survivors supporting one another",
            members = 127,
            type = "Private",
            category = "PTSD",
            meetingSchedule = "Saturdays, 4 PM",
            backgroundColor = Color(0xFFE57373)
        ),
        SupportGroup(
            name = "Teen Mental Health Support",
            description = "Peer support for teenagers facing mental health challenges",
            members = 201,
            type = "Public",
            category = "Anxiety",
            meetingSchedule = "Fridays, 7 PM",
            backgroundColor = Color(0xFF4FC3F7)
        ),
        SupportGroup(
            name = "Parents of Struggling Teens",
            description = "Connect with other parents navigating teen mental health",
            members = 112,
            type = "Private",
            category = "Depression",
            meetingSchedule = "Tuesdays, 8 PM",
            backgroundColor = Color(0xFF9575CD)
        ),
        SupportGroup(
            name = "Workplace Stress Management",
            description = "Dealing with work-related stress and burnout",
            members = 176,
            type = "Public",
            category = "Anxiety",
            meetingSchedule = "Sundays, 6 PM",
            backgroundColor = Color(0xFFF06292)
        ),
        SupportGroup(
            name = "Eating Disorder Recovery",
            description = "Support for those recovering from eating disorders",
            members = 87,
            type = "Private",
            category = "Addiction",
            meetingSchedule = "Mondays, 6 PM",
            backgroundColor = Color(0xFF4DB6AC)
        ),
        SupportGroup(
            name = "Bipolar Support Alliance",
            description = "Understanding and managing bipolar disorder together",
            members = 143,
            type = "Public",
            category = "Depression",
            meetingSchedule = "Wednesdays, 7 PM",
            backgroundColor = Color(0xFFAED581)
        )
    )

    val filteredGroups = if (selectedFilter == "All") {
        supportGroups
    } else {
        supportGroups.filter { it.category == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Support Groups",
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
                    containerColor = Color(0xFFCE93D8),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Info Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "💙 Join a Supportive Community",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = alegreyaFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W600,
                                    color = Color(0xFF0277BD)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connect with others who understand what you're going through. Share experiences, find hope, and support one another.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = alegreyaFontFamily,
                                    fontSize = 14.sp,
                                    color = Color(0xFF424242)
                                )
                            )
                        }
                    }
                }

                // Filter Chips
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Filter by Category",
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
                            filters.take(3).forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = filter,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = alegreyaFontFamily
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filters.drop(3).forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = filter,
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

                // Support Groups List
                items(filteredGroups.size) { index ->
                    SupportGroupCard(
                        group = filteredGroups[index],
                        onClick = {
                            // Could navigate to group details or join screen
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
fun SupportGroupCard(
    group: SupportGroup,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = group.backgroundColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group",
                            tint = group.backgroundColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = alegreyaFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W600,
                                color = Color(0xFF1A1A1A)
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (group.type == "Public") Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = group.type,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${group.type} • ${group.members} members",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = alegreyaFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF616161)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${group.meetingSchedule}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 12.sp,
                        color = group.backgroundColor,
                        fontWeight = FontWeight.W500
                    )
                )
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = group.backgroundColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (group.type == "Public") "Join" else "Request",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = alegreyaFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600
                        )
                    )
                }
            }
        }
    }
}
