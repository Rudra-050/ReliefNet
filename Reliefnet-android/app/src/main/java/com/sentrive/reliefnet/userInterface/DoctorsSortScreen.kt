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
import androidx.compose.runtime.LaunchedEffect
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
import com.sentrive.reliefnet.repository.ReliefNetRepository
import androidx.compose.ui.platform.LocalContext
import com.sentrive.reliefnet.network.models.Doctor as ApiDoctor

@Composable
fun DiscoverScreen(navHostController: NavHostController) {

    val configuration = LocalConfiguration.current
    val screeWidth  = configuration.screenWidthDp
    val mentalSupportCardWidth = screeWidth - 10
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    
    // State for doctors
    var doctors by remember { mutableStateOf<List<ApiDoctor>>(emptyList()) }
    var isLoadingDoctors by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Psychologist") }
    
    // Fetch doctors when screen loads or category changes
    LaunchedEffect(selectedCategory) {
        isLoadingDoctors = true
        errorMessage = null
        try {
            val result = repository.getDoctors(
                specialty = selectedCategory,
                limit = 50
            )
            result.onSuccess { doctorList ->
                doctors = doctorList
                isLoadingDoctors = false
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to load doctors"
                isLoadingDoctors = false
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "An error occurred"
            isLoadingDoctors = false
        }
    }

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
           PillsRow(selectedCategory) { category ->
               selectedCategory = category
           }

//            Mental health Support Card
            MentalHealthSupportCard(navHostController,0xFFFFD6F7,mentalSupportCardWidth.dp)

            //Lined Tab
            LinedTab(selectedCategory) { category ->
                selectedCategory = category
            }

            //filter Tab
            FilterTab()



            //Doctor's Card
            DoctorCard(
                doctors = doctors,
                isLoading = isLoadingDoctors,
                errorMessage = errorMessage,
                navHostController = navHostController
            )

        }
    }
    }
}

@Composable
fun PillsRow(selectedPill: String, onPillSelected: (String) -> Unit){
    val pills = listOf("Psychologist", "Therapist", "Psychiatrist")
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
                onClick = { onPillSelected(pill) },
                shape = RoundedCornerShape(4.dp), // rounded corners
                border = BorderStroke(1.dp, Color.White), // outline color white
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = if (pill == selectedPill) Color.White.copy(alpha = 0.3f) else Color.Transparent
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
fun LinedTab(selectedTab: String, onTabSelected: (String) -> Unit){
    Box(
        modifier = Modifier
            .padding(top = 25.dp, start = 7.dp)

            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val tabs = listOf("Psychologist", "Therapist", "Psychiatrist")
        var selectedTabIndex by remember { mutableStateOf(tabs.indexOf(selectedTab).takeIf { it >= 0 } ?: 0) }

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
                    onClick = { 
                        selectedTabIndex = index
                        onTabSelected(title)
                    },
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

@Composable
fun DoctorCard(
    doctors: List<ApiDoctor>,
    isLoading: Boolean,
    errorMessage: String?,
    navHostController: NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 25.dp)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Please check your connection and try again",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            doctors.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No doctors found",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try selecting a different category",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(doctors) { doctor ->
                        DoctorCardItem(doctor = doctor, navHostController = navHostController)
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorCardItem(doctor: ApiDoctor, navHostController: NavHostController) {
    Column(
        modifier = Modifier
            .width(342.dp)
            .padding(bottom = 16.dp)
            .clickable {
                doctor.id?.let { doctorId ->
                    navHostController.navigate("Booking/$doctorId")
                }
            }
    ) {
        Row(Modifier.fillMaxWidth()) {
            Column {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!doctor.photoUrl.isNullOrBlank()) {
                        // TODO: Load image from URL using Coil or Glide
                        Text(
                            text = doctor.name.firstOrNull()?.toString() ?: "D",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    } else {
                        Text(
                            text = doctor.name.firstOrNull()?.toString() ?: "D",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${doctor.experience ?: "0 years"}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = alegreyaSansFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text("Experience")
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
                    doctor.specialty ?: doctor.specialization ?: "Healthcare Professional",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = alegreyaSansFontFamily,
                        fontWeight = FontWeight.Normal
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = "rating",
                        modifier = Modifier.size(26.dp),
                        tint = Color(0xFFFFD700)
                    )
                    Text("${doctor.rating}/session (${doctor.reviewCount} reviews)")
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "₹ ${doctor.price.toInt()}${doctor.priceUnit ?: "/session"}",
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
                onClick = {
                    doctor.id?.let { doctorId ->
                        navHostController.navigate("Booking/$doctorId")
                    }
                },
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
                            .clickable {
                                doctor.id?.let { doctorId ->
                                    navHostController.navigate("doctor_chat/$doctorId")
                                }
                            },
                        tint = Color.Blue
                    )
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    DoctorsSortScreen()
//}
