package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import com.sentrive.reliefnet.ui.theme.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.ui.theme.aboretoFontFamily
import com.sentrive.reliefnet.ui.theme.alegreyaFontFamily

// Define a data class for UserType
data class UserType(val name: String, val backgroundColor: Color, val imageRes: Int)
@Composable
fun MentalHealthSupport(navHostController: NavHostController) {
    // List of user types
    val userTypes = listOf(
        UserType("Individual", Color(0xFFE3D7FF), R.drawable.individual1),
        UserType("Child", Color(0xFFFFF6CC), R.drawable.child1),
        UserType("Teen", Color(0xFFD8FCD2), R.drawable.teen1),
        UserType("Couple", Color(0xFFFFD5D5), R.drawable.couple1)
    )

    Scaffold(
        bottomBar = {
            BottomAppBar {
                MainBottomBar(navHostController
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Mental Health Support",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = alegreyaFontFamily,
                        fontSize = 30.sp
                    )
                )
            }

            //Calculating the actual width of the device then providing dynamic padding as required
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val cardWidth = screenWidth - 10
            // Cards
//            userTypes.forEach { userType ->
//                //User Card
//                UserCard(userType,cardWidth.dp)
//            }
            LazyColumn {
                userTypes.forEach { userType ->
                    item {
                        UserCard(userType, cardWidth.dp, navHostController)
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(userType: UserType, cardWidth: Dp, navHostController: NavHostController){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(cardWidth)
                .height(139.dp)
                .clickable { 
                    // Navigate to doctor list when user type is clicked
                    navHostController.navigate("DiscoverScreen")
                },
            colors = CardDefaults.cardColors(containerColor = userType.backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = userType.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = aboretoFontFamily,
                        fontSize = 30.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 58.dp, start = 10.dp)
                )
                Image(
                    painter = painterResource(id = userType.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(118.dp)
                        .height(139.dp)
                        .padding(end = 14.dp)
                )
            }
        }
    }
}
@Composable
fun MainBottomBar(navHostController: NavHostController) {
    BottomAppBar(
        containerColor = PatientBackground,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(icon = R.drawable.home_fill, label = "Home", onClick = { navHostController.navigate("Home"){ launchSingleTop = true } })
            BottomBarItem(icon = R.drawable.discover, label = "Nav", onClick = { navHostController.navigate("DiscoverScreen"){ launchSingleTop = true } })
            BottomBarItem(icon = R.drawable.vector, label = "Cart", onClick = { navHostController.navigate("YourBookings"){ launchSingleTop = true }})
            BottomBarItem(icon = R.drawable.bell, label = "Notification", onClick = { navHostController.navigate("Notifications"){ launchSingleTop = true } })
            BottomBarItem(icon = R.drawable.person, label = "Account", onClick = { navHostController.navigate("ProfileScreen"){ launchSingleTop = true } })
        }
    }
}

@Composable
fun BottomBarItem(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable{onClick()}
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = PatientPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    UserTypeScreen()
//}
