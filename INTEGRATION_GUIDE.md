# ReliefNet - Frontend & Backend Integration Guide

## 🎉 Setup Complete!

Your Android app is now connected to the Node.js backend server!

---

## 📋 What Was Done

### 1. Backend Server (Node.js)
- ✅ Server running on `http://localhost:5000`
- ✅ MongoDB connected
- ✅ Socket.IO enabled for real-time communication
- ✅ REST API endpoints ready

### 2. Android Frontend
- ✅ Added Retrofit for HTTP calls
- ✅ Added Socket.IO client for real-time features
- ✅ Created network layer with API services
- ✅ Added internet permissions
- ✅ Created repository pattern for API calls
- ✅ Added example usage code

---

## 🌐 Network Configuration

### Important: Update IP Address for Physical Device

Currently configured for **Android Emulator**: `10.0.2.2:5000`

**If using a physical device:**
1. Find your computer's IP address:
   - Windows: Open Command Prompt → Type `ipconfig` → Look for "IPv4 Address"
   - Example: `192.168.1.100`

2. Update `ApiConfig.kt`:
```kotlin
private const val LOCAL_IP = "192.168.1.100" // Your computer's IP
```

3. Make sure your phone and computer are on the **same WiFi network**

---

## 📁 Project Structure

```
app/src/main/java/com/sentrive/reliefnet/
├── network/
│   ├── ApiConfig.kt              # API URLs and endpoints
│   ├── ApiService.kt             # Retrofit API interface
│   ├── RetrofitClient.kt         # Retrofit singleton
│   ├── SocketManager.kt          # Socket.IO manager
│   └── models/
│       └── ApiModels.kt          # Data models
├── repository/
│   └── ReliefNetRepository.kt    # Repository layer
├── examples/
│   └── ExampleApiUsage.kt        # Usage examples
└── userInterface/
    └── [Your existing screens]
```

---

## 🚀 API Endpoints Available

### Authentication
- `POST /api/register` - Register patient
- `POST /api/doctors/register` - Register doctor
- `POST /api/login` - Patient login
- `POST /api/doctors/login` - Doctor login

### Doctors
- `GET /api/doctors` - Get all doctors (with filters)
- `GET /api/doctors/{id}` - Get doctor by ID
- `PUT /api/doctors/{id}` - Update doctor profile

### Sessions (Bookings)
- `POST /api/sessions` - Create new session
- `GET /api/sessions` - Get sessions (filter by patient/doctor)
- `PUT /api/sessions/{id}` - Update session
- `DELETE /api/sessions/{id}` - Cancel session

### Notifications
- `GET /api/notifications` - Get user notifications
- `PUT /api/notifications/{id}/read` - Mark as read

### Uploads
- `POST /api/upload` - Upload profile photo

### Payments
- `POST /api/payments/create-order` - Create Razorpay order
- `POST /api/payments/verify` - Verify payment

---

## 💻 How to Use in Your Screens

### Example 1: Login Screen

```kotlin
@Composable
fun LoginScreen(navController: NavController) {
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    
                    repository.loginPatient(email, password)
                        .onSuccess { response ->
                            // Save token to SharedPreferences
                            // Save user data
                            navController.navigate("home")
                        }
                        .onFailure { error ->
                            errorMessage = error.message
                        }
                    
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Login")
        }
        
        errorMessage?.let {
            Text(it, color = Color.Red)
        }
    }
}
```

### Example 2: Fetch Doctors

```kotlin
@Composable
fun DoctorsListScreen() {
    val repository = remember { ReliefNetRepository() }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isLoading = true
        repository.getDoctors(specialty = "Psychologist")
            .onSuccess { doctorList ->
                doctors = doctorList
            }
            .onFailure { error ->
                // Handle error
            }
        isLoading = false
    }
    
    LazyColumn {
        items(doctors) { doctor ->
            DoctorCard(doctor)
        }
    }
}
```

### Example 3: Book a Session

```kotlin
fun bookSession(
    patientId: String,
    doctorId: String,
    date: String,
    time: String,
    token: String
) {
    val repository = ReliefNetRepository()
    
    scope.launch {
        repository.createSession(
            patientId = patientId,
            doctorId = doctorId,
            sessionDate = date,
            sessionTime = time,
            duration = 60,
            notes = "First session",
            token = token
        )
            .onSuccess { session ->
                // Show success message
                // Navigate to booking confirmation
            }
            .onFailure { error ->
                // Show error message
            }
    }
}
```

### Example 4: Real-time Chat with Socket.IO

```kotlin
@Composable
fun ChatScreen(userId: String, userType: String) {
    LaunchedEffect(Unit) {
        // Connect to socket
        val socket = SocketManager.connect()
        
        // Register user
        SocketManager.registerUser(userId, userType)
        
        // Listen for incoming calls
        socket?.on("call:incoming") { args ->
            // Show incoming call UI
        }
        
        // Disconnect when leaving screen
        DisposableEffect(Unit) {
            onDispose {
                SocketManager.disconnect()
            }
        }
    }
}
```

---

## 🔐 Authentication Flow

1. **Login/Register** → Get token from API
2. **Save token** to SharedPreferences or DataStore
3. **Use token** in all authenticated requests:
```kotlin
repository.getSessions(
    patientId = "user123",
    token = "your-jwt-token"
)
```

---

## 🧪 Testing the Connection

### Test 1: Check Server is Running
Open browser: `http://localhost:5000` (should show a response)

### Test 2: Test Registration
Use the example code in `ExampleApiUsage.kt` or create a test screen

### Test 3: Test Doctor Listing
```kotlin
// In any screen
LaunchedEffect(Unit) {
    ReliefNetRepository().getDoctors()
        .onSuccess { doctors ->
            Log.d("API_TEST", "Fetched ${doctors.size} doctors")
        }
        .onFailure { error ->
            Log.e("API_TEST", "Error: ${error.message}")
        }
}
```

---

## 📱 Dependencies Added

```gradle
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Gson for JSON
implementation("com.google.code.gson:gson:2.10.1")

// Socket.IO
implementation("io.socket:socket.io-client:2.1.0")

// Coil for images
implementation("io.coil-kt:coil-compose:2.5.0")
```

---

## 🔧 Troubleshooting

### Issue: "Unable to connect to server"
**Solution:**
1. Check if backend server is running
2. For physical device, use your computer's IP instead of `10.0.2.2`
3. Ensure firewall allows port 5000

### Issue: "Cleartext traffic not permitted"
**Solution:** Already fixed! Added `android:usesCleartextTraffic="true"` in manifest

### Issue: "Connection timeout"
**Solution:** 
- Check network connectivity
- Verify server is accessible from device/emulator
- Check if MongoDB is connected

---

## 📚 Next Steps

1. **Update your existing screens** to use the API:
   - Replace mock data with real API calls
   - Add loading states
   - Handle errors gracefully

2. **Implement authentication**:
   - Create login/register screens
   - Save JWT tokens
   - Add token to API headers

3. **Add image upload**:
   - Use `uploadPhoto()` for profile pictures
   - Display images with Coil library

4. **Implement real-time features**:
   - Video/Audio calls using Socket.IO
   - Real-time chat
   - Live notifications

---

## 🎯 Key Files to Modify

1. **loginScreen.kt** - Add actual login API call
2. **DoctorRegistration.kt** - Add doctor registration API
3. **HomeScreen.kt** - Fetch real doctor data
4. **BookingScreen.kt** - Create actual bookings
5. **DoctorChatScreen.kt** - Connect to Socket.IO

---

## 📞 Support

Check the example file for complete usage patterns:
`app/src/main/java/com/sentrive/reliefnet/examples/ExampleApiUsage.kt`

---

**Happy Coding! 🚀**
