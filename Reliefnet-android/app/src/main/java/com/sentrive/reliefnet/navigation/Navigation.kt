package com.sentrive.reliefnet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.ui.booking.BookingScreen
import com.sentrive.reliefnet.ui.booking.MyBookingsScreen
import com.sentrive.reliefnet.ui.booking.PaymentStatusScreen
import com.sentrive.reliefnet.userInterface.BookingScreenRecord
import com.sentrive.reliefnet.userInterface.DiscoverScreen
import com.sentrive.reliefnet.userInterface.DoctorChatScreen
import com.sentrive.reliefnet.userInterface.*
import com.sentrive.reliefnet.userInterface.DoctorRegistrationScreen
import com.sentrive.reliefnet.userInterface.HomePage
import com.sentrive.reliefnet.userInterface.LinearProgress
import com.sentrive.reliefnet.userInterface.LoginScreen
import com.sentrive.reliefnet.userInterface.PatientChatScreen
import com.sentrive.reliefnet.userInterface.ProfileScreen
import com.sentrive.reliefnet.userInterface.RelieScreen
import com.sentrive.reliefnet.userInterface.SplashScreen
import com.sentrive.reliefnet.userInterface.MentalHealthSupport
import com.sentrive.reliefnet.userInterface.ProfessionalLoginScreen
import com.sentrive.reliefnet.userInterface.RelieChat
import com.sentrive.reliefnet.userInterface.VideoCallScreen
import com.sentrive.reliefnet.userInterface.UserTypeSelectionScreen
import com.sentrive.reliefnet.viewmodel.BookingViewModel
import com.sentrive.reliefnet.viewmodel.BookingViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.sentrive.reliefnet.utils.TokenManager

@Composable
fun Navigation(navHostController: NavHostController){
    NavHost(navHostController,"SplashScreen"){
        composable("Home") { HomePage(navHostController) }
        composable("LoginScreen") { LoginScreen(navHostController) }
        composable("LinerarProgress") { LinearProgress(navHostController) }
        
        // Notifications (shared by patients and doctors)
        composable("Notifications") { NotificationsScreen(navHostController) }
        
        composable("DoctorDashboard") { DoctorDashboardScreen(navHostController) }
    composable("DoctorChats") { DoctorChatsListScreen(navHostController) }
    composable("DoctorFeedback") { DoctorFeedbackScreen(navHostController) }
    composable("DoctorPayments") { DoctorPaymentHistoryScreen(navHostController) }
    // Public profile (patient-facing)
    composable("DoctorProfile") { DoctorProfileScreen(navHostController) }
    // Doctor account profile (doctor-facing)
    composable("DoctorAccountProfile") { DoctorAccountProfileScreen(navHostController) }
    composable("EditDoctorProfile") { EditDoctorProfileScreen(navHostController) }
    composable("DoctorSessions") { DoctorSessionsScreen(navHostController) }
    composable("DoctorHelp") { DoctorHelpSupportScreen() }
    composable(
        route = "doctor_availability/{doctorId}",
        arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
        com.sentrive.reliefnet.ui.doctor.DoctorAvailabilityScreen(
            doctorId = doctorId,
            onBack = { navHostController.popBackStack() }
        )
    }
        composable("DiscoverScreen") { DiscoverScreen(navHostController) }
        composable("ProfileScreen") { ProfileScreen(navHostController) }
        
        // Patient Profile Pages
        composable("PersonalInformation") { PersonalInformationScreen(navHostController) }
        composable("PaymentHistory") { PaymentHistoryScreen(navHostController) }
        composable("YourBookings") { YourBookingsIntegratedScreen(navHostController) }
        composable("HelpSupport") { HelpSupportScreen(navHostController) }
        
        // Integrated Booking Screen with doctor ID parameter
        composable("IntegratedBooking/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            IntegratedBookingScreen(navHostController, doctorId)
        }
        
        // Payment Screen
        composable("payment-screen/{doctorId}/{date}/{startTime}/{endTime}/{amount}/{appointmentType}/{symptoms}/{notes}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
            val endTime = backStackEntry.arguments?.getString("endTime") ?: ""
            val amount = backStackEntry.arguments?.getString("amount") ?: "0"
            val appointmentType = backStackEntry.arguments?.getString("appointmentType") ?: "Online Consultation"
            val symptoms = backStackEntry.arguments?.getString("symptoms") ?: "None"
            val notes = backStackEntry.arguments?.getString("notes") ?: "None"
            PaymentScreen(navHostController, doctorId, date, startTime, endTime, amount, appointmentType, symptoms, notes)
        }
        
        composable("SplashScreen") { SplashScreen(navHostController) }
        composable("MentalHealthSupport") { MentalHealthSupport(navHostController) }
        composable("ServicesScreen") { ServicesScreen(navHostController) }
        composable("EmergencyContactScreen") { EmergencyContactScreen(navHostController) }
        composable("WellnessResourcesScreen") { WellnessResourcesScreen(navHostController) }
        composable("SupportGroupsScreen") { SupportGroupsScreen(navHostController) }
        composable("BookingScreenRecord") { BookingScreenRecord(navHostController) }
        composable("RelieScreen") { RelieScreen(navHostController) }
        composable("AdvanceBooking") { BookingScreenRecord(navHostController) }
        
        // User Type Selection
        composable("UserTypeSelection") { UserTypeSelectionScreen(navHostController) }
        
        // Doctor/Professional routes
        composable("DoctorRegistrationScreen") { DoctorRegistrationScreen(navHostController) }
        composable("ProfessionalLoginScreen") { ProfessionalLoginScreen(navHostController) }
        composable("DoctorChatScreen") { DoctorChatScreen() }
        
        // Patient routes
        composable("PatientChatScreen") { PatientChatScreen() }
        
        // Test routes
        composable("RelieChat") { RelieChat(navHostController) }

        // Video/Audio Call route
        composable(
            "VideoCallScreen/{selfId}/{peerId}/{isCaller}/{callType}",
        ) { backStackEntry ->
            val selfId = backStackEntry.arguments?.getString("selfId") ?: ""
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val isCaller = backStackEntry.arguments?.getString("isCaller") == "true"
            val callType = backStackEntry.arguments?.getString("callType") ?: "video"
            VideoCallScreen(
                selfId = selfId,
                peerId = peerId,
                isCaller = isCaller,
                callType = callType
            )
        }
        
        // Booking Screen with doctor ID parameter (use integrated booking)
        composable("Booking/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            IntegratedBookingScreen(navHostController, doctorId)
        }
        
        // Doctor session creation screen
        composable("DoctorSessionCreation") { DoctorSessionCreationScreen(navHostController) }
        
        // Edit session screen
        composable("EditSession/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            EditSessionScreen(sessionId, navHostController)
        }
        
        // ========== PhonePe Payment & Booking Routes ==========
        
        // New Booking Screen with PhonePe integration
        composable(
            route = "booking/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            
            val repository = ReliefNetRepository()
            val viewModelFactory = BookingViewModelFactory(repository)
            val viewModel: BookingViewModel = viewModel(factory = viewModelFactory)
            
            // Load actual doctor details from API
            LaunchedEffect(doctorId) {
                viewModel.loadDoctorDetails(doctorId)
            }
            
            val doctorState by viewModel.doctorState.collectAsState()
            
            when (val state = doctorState) {
                is BookingViewModel.DoctorState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is BookingViewModel.DoctorState.Success -> {
                    val context = LocalContext.current
                    val patientId = TokenManager.getUserId(context) ?: ""
                    val patientName = TokenManager.getUserName(context) ?: ""
                    val patientEmail = TokenManager.getUserEmail(context) ?: ""
                    val patientPhone: String? = null // Phone not stored in TokenManager
                    
                    BookingScreen(
                        doctor = state.doctor,
                        patientId = patientId,
                        patientName = patientName,
                        patientEmail = patientEmail,
                        patientPhone = patientPhone,
                        onBack = { navHostController.popBackStack() },
                        onBookingSuccess = { bookingId ->
                            navHostController.navigate("YourBookings") {
                                popUpTo("home") { inclusive = false }
                            }
                        },
                        viewModel = viewModel
                    )
                }
                is BookingViewModel.DoctorState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navHostController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }
        
        // Payment Status Screen (handles PhonePe deep link callback)
        composable(
            route = "payment_status/{transactionId}/{doctorId}/{date}/{time}/{endTime}/{appointmentType}/{symptoms}/{notes}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType },
                navArgument("endTime") { type = NavType.StringType },
                navArgument("appointmentType") { type = NavType.StringType },
                navArgument("symptoms") { type = NavType.StringType },
                navArgument("notes") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val time = backStackEntry.arguments?.getString("time") ?: ""
            val endTime = backStackEntry.arguments?.getString("endTime") ?: ""
            val appointmentType = backStackEntry.arguments?.getString("appointmentType") ?: "Online Consultation"
            val symptoms = backStackEntry.arguments?.getString("symptoms") ?: ""
            val notes = backStackEntry.arguments?.getString("notes") ?: ""
            
            val repository = ReliefNetRepository()
            val viewModelFactory = BookingViewModelFactory(repository)
            val viewModel: BookingViewModel = viewModel(factory = viewModelFactory)
            
            PaymentStatusScreen(
                merchantTransactionId = transactionId,
                professionalId = doctorId,
                appointmentDate = date,
                appointmentTime = time,
                appointmentEndTime = endTime,
                appointmentType = appointmentType,
                symptoms = symptoms,
                notes = notes,
                onSuccess = { bookingId ->
                    // Navigate to booking details or my bookings
                    navHostController.navigate("YourBookings") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onFailed = {
                    // Go back to booking screen or home
                    navHostController.popBackStack("home", false)
                },
                onBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewModel
            )
        }
        
        // My Bookings Screen
        composable("my_bookings") {
            val context = LocalContext.current
            val patientId = TokenManager.getUserId(context) ?: ""
            
            val repository = ReliefNetRepository()
            val viewModelFactory = BookingViewModelFactory(repository)
            val viewModel: BookingViewModel = viewModel(factory = viewModelFactory)
            
            MyBookingsScreen(
                patientId = patientId,
                onBookingClick = { bookingId ->
                    // Navigate to booking details screen
                    // For now, navigating to existing bookings screen
                    navHostController.navigate("YourBookings")
                },
                onBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewModel
            )
        }
    }
}