package com.sentrive.reliefnet.userInterface

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DoctorProfileScreen(navHostController: NavHostController) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, bottom = 20.dp)
    ) {
        Image(painterResource(R.drawable.bg),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)

        Scaffold(
            containerColor = Color.Transparent,

            topBar = {
                TopBar(navHostController)
            },
            modifier = Modifier.fillMaxSize()

        ) {
            Box(
                modifier = Modifier
                    .padding(top = 72.dp)
                    .background(Color.White)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Doctor Image
                    Image(
                        painter = painterResource(id = R.drawable.doc_image),
                        contentDescription = "Doctor Image",
                        modifier = Modifier
                            .size(width = 183.dp, height = 275.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Doctor Name
                    Text(
                        text = "Dr. Rahul Verma",
                        fontFamily = oswaldFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )

                    // Title
                    Text(
                        text = "Therapist & Counsellor",
                        fontFamily = oswaldFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // About Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PatientPrimary, shape = RoundedCornerShape(6.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "About Doctor",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "★ 4.9 (280 Reviews)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "A doctor is someone who is experienced and certified to practice medicine to help maintain or restore physical and mental health.",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Icons
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ActionIcon(R.drawable.video_call)
                        ActionIcon(R.drawable.phone_call)
                        ActionIcon(R.drawable.message_doc)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Availability
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE",
                            fontFamily = poppinsFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ActionIcon(iconResId: Int) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .background(color = PatientPrimary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

    }
}

//@Preview(showSystemUi = true)
//@Composable
//fun DoctorProfilePreview() {
//    val navController = rememberNavController()
//  DoctorProfileScreen(navController)
//}
//@Preview(showSystemUi = true)
//@Composable
//fun p(){
//    val navController = rememberNavController()
//    MentalHealthSupport(navController)
//}
