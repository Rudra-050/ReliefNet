package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.*

@Composable
fun BookingScreenRecord(navHostController: NavHostController) {

    Box(Modifier.fillMaxSize()
        .padding(top = 30.dp)) {
        Image(
            painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopBar(navHostController)
            },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            bottomBar = {
                MainBottomBar(navHostController
                )
            }
            // modifier = Modifier.background(Color.Transparent)
        ) { paddingValues ->
            Box() {
                Column(
                    Modifier
                        .padding(paddingValues)
                        .background(Color.White)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AdvancedBookingScreen(navHostController)
                    }
                }
            }
        }
    }

data class AdvancedBooking(
    val title: String,
    val name: String,
    val status: String,        // "Confirmed", "Pending"
    val statusColor: Color,
    val dateTime: String,
    val callType: String
)

@Composable
fun AdvancedBookingScreen(navHostController: NavHostController) {
    val bookings = listOf(
        AdvancedBooking(
            title = "Therapy Session",
            name = "Dr. Priya Sharma",
            status = "Confirmed",
            statusColor = Color(0xFF4CAF50),
            dateTime = "May 15, 2025, 10:00 AM",
            callType = "Video Call"
        ),
        AdvancedBooking(
            title = "Home Nurse Visit",
            name = "Anita Desai",
            status = "Pending",
            statusColor = Color(0xFFFFF59D),
            dateTime = "May 18, 2025, 4:30 PM",
            callType = "Home Visit"
        )
    )

    var selectedTab by remember { mutableStateOf("Past") }

    Spacer(Modifier.height(12.dp))
    // Tabs
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F0F0)) // Light grey
            .padding(top = 2.dp, start = 2.dp, end = 2.dp, bottom = 2.dp)
            .width(374.dp)
    ) {
        listOf("Upcoming", "Past").forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedTab = tab }
                    .background(if (isSelected) Color.White else Color(0xFFF0F0F0))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) PatientPrimary else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bookings.size) { index ->
            val booking = bookings[index]
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* handle click */ }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = booking.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = booking.name,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(booking.statusColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = booking.status,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.calender),
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(booking.dateTime, color = Color.Gray, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.message),
                            contentDescription = "Call Type",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(booking.callType, color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { /* Join / Directions */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PatientPrimary)
                        ) {
                            Text("Join / Directions", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { /* Delete */ },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.trash),
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun d(){
//    BookingScreenRecord()
//}