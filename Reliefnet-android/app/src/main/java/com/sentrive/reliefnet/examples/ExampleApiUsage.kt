package com.sentrive.reliefnet.examples

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentrive.reliefnet.network.SocketManager
import com.sentrive.reliefnet.network.models.Doctor
import com.sentrive.reliefnet.repository.ReliefNetRepository
import kotlinx.coroutines.launch

/**
 * Example ViewModel showing how to use the ReliefNet API
 * 
 * Usage in your actual ViewModels:
 * 1. Create a ViewModel extending this class or copy the patterns
 * 2. Call the suspend functions from your Composable screens
 * 3. Update UI based on the state
 */
class ExampleApiUsageViewModel : ViewModel() {
    
    private val repository = ReliefNetRepository()
    
    // Example states
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var doctors by mutableStateOf<List<Doctor>>(emptyList())
        private set
    
    var authToken by mutableStateOf<String?>(null)
        private set
    
    // Example 1: Register a patient
    fun registerPatient(email: String, password: String, name: String, location: String? = null) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.registerPatient(email, password, name, location)
                .onSuccess { response ->
                    authToken = response.token
                    // Save token to SharedPreferences or DataStore
                    // Navigate to home screen
                    println("Registration successful! Token: ${response.token}")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Registration failed"
                    println("Registration error: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 2: Login a patient
    fun loginPatient(email: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.loginPatient(email, password)
                .onSuccess { response ->
                    authToken = response.token
                    // Save token and user data
                    // Navigate to home screen
                    println("Login successful! Token: ${response.token}")
                    println("User: ${response.user}")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Login failed"
                    println("Login error: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 3: Register a doctor
    fun registerDoctor(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        specialization: String,
        hospital: String
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.registerDoctor(
                email, password, firstName, lastName, phone, specialization, hospital
            )
                .onSuccess { response ->
                    authToken = response.token
                    println("Doctor registration successful! Token: ${response.token}")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Doctor registration failed"
                    println("Doctor registration error: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 4: Get all doctors (with optional filters)
    fun fetchDoctors(specialty: String? = null, location: String? = null, category: String? = null) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.getDoctors(specialty, location, category)
                .onSuccess { doctorList ->
                    doctors = doctorList
                    println("Fetched ${doctorList.size} doctors")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to fetch doctors"
                    println("Error fetching doctors: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 5: Get doctor by ID
    fun fetchDoctorById(doctorId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.getDoctorById(doctorId)
                .onSuccess { doctor ->
                    println("Fetched doctor: ${doctor.name}")
                    println("Specialization: ${doctor.specialization}")
                    println("Rating: ${doctor.rating}")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Doctor not found"
                    println("Error fetching doctor: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 6: Create a session (booking)
    fun bookSession(
        patientId: String,
        doctorId: String,
        sessionDate: String,
        sessionTime: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            if (authToken == null) {
                errorMessage = "Please login first"
                return@launch
            }
            
            isLoading = true
            errorMessage = null
            
            repository.createSession(
                patientId, doctorId, sessionDate, sessionTime, 60, notes, authToken!!
            )
                .onSuccess { session ->
                    println("Session booked successfully!")
                    println("Session ID: ${session.id}")
                    println("Date: ${session.sessionDate} at ${session.sessionTime}")
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to book session"
                    println("Booking error: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 7: Get user's sessions
    fun fetchMySessions(patientId: String) {
        viewModelScope.launch {
            if (authToken == null) {
                errorMessage = "Please login first"
                return@launch
            }
            
            isLoading = true
            errorMessage = null
            
            repository.getSessions(patientId = patientId, token = authToken!!)
                .onSuccess { sessions ->
                    println("Fetched ${sessions.size} sessions")
                    sessions.forEach { session ->
                        println("Session on ${session.sessionDate} at ${session.sessionTime} - Status: ${session.status}")
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to fetch sessions"
                    println("Error fetching sessions: ${error.message}")
                }
            
            isLoading = false
        }
    }
    
    // Example 8: Connect to Socket.IO for real-time features
    fun connectToSocket(userId: String, userType: String) {
        viewModelScope.launch {
            val socket = SocketManager.connect()
            
            if (socket != null) {
                // Register user for real-time notifications
                SocketManager.registerUser(userId, userType)
                
                // Listen for incoming calls
                socket.on("call:incoming") { args ->
                    println("Incoming call from: ${args[0]}")
                    // Show incoming call UI
                }
                
                // Listen for call end
                socket.on("call:end") { args ->
                    println("Call ended by: ${args[0]}")
                    // Update UI
                }
                
                println("Socket.IO connected successfully!")
            } else {
                println("Failed to connect to Socket.IO")
            }
        }
    }
    
    // Example 9: Get notifications
    fun fetchNotifications() {
        viewModelScope.launch {
            if (authToken == null) {
                errorMessage = "Please login first"
                return@launch
            }
            
            repository.getNotifications(authToken!!)
                .onSuccess { notifications ->
                    println("Fetched ${notifications.size} notifications")
                    notifications.forEach { notification ->
                        println("${notification.type}: ${notification.message} - Read: ${notification.isRead}")
                    }
                }
                .onFailure { error ->
                    println("Error fetching notifications: ${error.message}")
                }
        }
    }
}

/* 
 * EXAMPLE USAGE IN A COMPOSABLE SCREEN:
 * 
 * @Composable
 * fun LoginScreen() {
 *     val viewModel = remember { ExampleApiUsageViewModel() }
 *     var email by remember { mutableStateOf("") }
 *     var password by remember { mutableStateOf("") }
 *     
 *     Column {
 *         TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
 *         TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
 *         
 *         Button(onClick = { viewModel.loginPatient(email, password) }) {
 *             Text("Login")
 *         }
 *         
 *         if (viewModel.isLoading) {
 *             CircularProgressIndicator()
 *         }
 *         
 *         viewModel.errorMessage?.let { error ->
 *             Text(error, color = Color.Red)
 *         }
 *     }
 * }
 * 
 * EXAMPLE: Fetching doctors with filter
 * 
 * @Composable
 * fun DoctorsListScreen() {
 *     val viewModel = remember { ExampleApiUsageViewModel() }
 *     
 *     LaunchedEffect(Unit) {
 *         viewModel.fetchDoctors(specialty = "Psychologist")
 *     }
 *     
 *     LazyColumn {
 *         items(viewModel.doctors) { doctor ->
 *             DoctorCard(doctor)
 *         }
 *     }
 * }
 */
