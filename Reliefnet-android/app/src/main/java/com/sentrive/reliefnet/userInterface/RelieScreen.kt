package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import com.sentrive.reliefnet.R
import com.sentrive.reliefnet.viewmodel.ChatbotViewModel
import com.sentrive.reliefnet.userInterface.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun RelieScreen(
    navHostController: NavHostController? = null,
    chatbotViewModel: ChatbotViewModel = viewModel()
) {
    val message = remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            navHostController?.let { controller ->
                AppDrawer(navHostController = controller) {
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,

        )
        Spacer(Modifier.height(30.dp))
        Row (Modifier.fillMaxWidth()
            .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End){
            Image(
                painter = painterResource(R.drawable.menu),
                contentDescription = "Menu",
                modifier = Modifier.padding(end = 26.dp)
                    .size(26.dp)
                    .clickable {
                        scope.launch { drawerState.open() }
                    }

            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello there!!",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "How can I help you Today?",
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = message.value,
                onValueChange = { message.value = it },
                placeholder = { Text("Message...") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFB39DDB),
                    unfocusedBorderColor = Color.LightGray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Send button - prominent when there's text
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (message.value.isNotBlank()) Color(0xFFB39DDB) else Color.LightGray
                                )
                                .clickable(enabled = message.value.isNotBlank()) { 
                                    if (message.value.isNotBlank()) {
                                        // Send message to chatbot
                                        chatbotViewModel.sendMessage(message.value)
                                        message.value = ""
                                        // Navigate to RelieChat screen
                                        navHostController?.navigate("RelieChat")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.send_icon),
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            )
        }

    }
    }
}