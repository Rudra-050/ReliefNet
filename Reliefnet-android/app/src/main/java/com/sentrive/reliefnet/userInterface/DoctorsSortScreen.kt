package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.alegreyaFontFamily
import com.sentrive.reliefnet.ui.theme.alegreyaSansFontFamily
import com.sentrive.reliefnet.ui.theme.inriaSerifFontFamily
import com.sentrive.reliefnet.ui.theme.interFontFamily
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(navHostController: NavHostController) {

    val configuration = LocalConfiguration.current
    val screeWidth  = configuration.screenWidthDp
    val mentalSupportCardWidth = screeWidth - 10
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(navHostController = navHostController) {
                scope.launch { drawerState.close() }
            }
        }
    ) {
    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFCF5DBD),
                                Color(0xFFDC8BEF),
                                Color(0xFFF3C6F7)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navHostController.navigate("Home") }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_2),
                        contentDescription = "Back",
                        modifier = Modifier.size(26.dp),
                        tint = Color.Black
                    )
                }
                Text(
                    "Relief Net",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = inriaSerifFontFamily,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        painter = painterResource(R.drawable.menu),
                        contentDescription = "Menu",
                        modifier = Modifier.size(26.dp),
                        tint = Color.Black
                    )
                }
            }
        },
        bottomBar = {
            MainBottomBar(navHostController)
        }, modifier = Modifier.padding(top = 30.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // Pills row
           PillsRow()

//            Mental health Support Card
            MentalHealthSupportCard(navHostController,0xFFFFD6F7,mentalSupportCardWidth.dp)

            //Lined Tab
            LinedTab()

            //filter Tab
            FilterTab()



            //Doctor's Card
            DoctorCard()

        }
    }
    }
}

@Composable
fun PillsRow(){
    val pills = listOf("Mental Health", "Home Nurse", "Postpartum Care")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFDD45C2),
                        Color(0xFF7B34DD)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pills) { pill ->
            Button(
                onClick = { /* Handle selection */ },
                shape = RoundedCornerShape(4.dp), // rounded corners
                border = BorderStroke(1.dp, Color.White), // outline color white
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White // text color
                )
            ) {
                Text(
                    text = pill,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MentalHealthSupportCard(navHostController: NavHostController,color:Long,width: Dp){
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = {
                navHostController.navigate("MentalHealthSupport") {
                    launchSingleTop = true
                }
            }),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(width)
                .height(126.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(color) // Light pink background
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mental Health Support",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 25.sp,
                            fontFamily = alegreyaFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connect with trusted psychologists & counselors in your preferred language",
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = alegreyaSansFontFamily,

                            ),
                        color = Color.DarkGray
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.arrow1), // Replace with actual arrow icon
                    contentDescription = "Arrow",
                    tint = Color.Black,
                    modifier = Modifier.size(51.dp, 10.dp)
                )
            }
        }
    }
}

@Composable
fun LinedTab(){
    Box(
        modifier = Modifier
            .padding(top = 25.dp, start = 7.dp)

            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val tabs = listOf("Psychologist", "Therapist", "Psychiatrist")
        var selectedTabIndex by remember { mutableStateOf(0) }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    selectedContentColor = Color.Blue,
                    unselectedContentColor = Color.Gray,
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FilterTab(){
    Box(Modifier.padding(top = 8.dp)){
        val filters = listOf("All Professionals", "Near Me", "Top Rated", "Price")
        var selectedIndex by remember { mutableStateOf(0) }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filters) { index, label ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = {
                        Text(
                            text = label,
                            color = if (selectedIndex == index) Color(0xFF6200EA) else Color.Black
                        )
                    },
                    leadingIcon = {
                        val iconColor = if (selectedIndex == index) Color(0xFF6200EA) else Color.Black
                        when (label) {
                            "All Professionals" -> Icon(painterResource(R.drawable.allprofessionals), contentDescription = null, Modifier.size(16.dp),tint = iconColor)
                            "Near Me" -> Icon(painterResource(R.drawable.location), contentDescription = null, Modifier.size(16.dp),tint = iconColor)
                            "Top Rated" -> Icon(painterResource(R.drawable.star1), contentDescription = null, Modifier.size(16.dp),tint = iconColor)
                            "Price" -> Icon(painterResource(R.drawable.cash), contentDescription = null, Modifier.size(16.dp),tint = iconColor)
                            else -> null
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEDE7F6), // light purple
                        containerColor = Color.White,
                        selectedLabelColor = Color(0xFF6200EA)
                    ),
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}

data class Doctor(val name: String,val type :String , val experience :Int,val price:Int,val rating: Float,val reviews : Int,val imageRes : Int)

@Composable
fun DoctorCard() {
    val doctors = listOf(
        Doctor("Dr. Priya Sharma", "Clinical Psychologist", 8, 1200, 4.9F, 124, R.drawable.applelogo),
        Doctor("Dr. Priya Sharma", "Clinical Psychologist", 8, 1200, 4.9F, 124, R.drawable.applelogo)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally // ✅ Align cards in center
    ) {
        items(doctors) { doctor ->
            Column(
                modifier = Modifier
                    .width(342.dp) // ✅ Set consistent card width
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Column {
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(doctor.imageRes),
                                contentDescription = "Doctor Profile"
                            )
                        }
                        Text(
                            "${doctor.experience} +years",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = alegreyaSansFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text("Experince")
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            doctor.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 18.sp,
                                fontFamily = alegreyaSansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            doctor.type,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = alegreyaSansFontFamily,
                                fontWeight = FontWeight.Normal
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.star),
                                contentDescription = "rating",
                                modifier = Modifier.size(26.dp), // ⬅ original size kept
                                tint = Color(0xFFFFD700)
                            )
                            Text("${doctor.rating}/session (${doctor.reviews} reviews)")
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "₹ ${doctor.price}/session",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = alegreyaSansFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text("Starting At")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Text("Book Session")
                    }

                    Spacer(Modifier.width(16.dp))

                    Card(
                        modifier = Modifier.width(30.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp)
                                .background(Color.White),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.message),
                                contentDescription = "message",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable(onClick = {}),
                                tint = Color.Blue
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    DoctorsSortScreen()
//}
