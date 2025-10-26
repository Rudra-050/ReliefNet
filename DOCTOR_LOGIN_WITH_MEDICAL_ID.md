# Doctor Login with Medical ID

## Overview
Doctors now log in using their **Medical ID** instead of email, while patients continue to use email for authentication.

## Changes Made

### 1. Backend Updates

#### `server/server.js` - Doctor Login API
```javascript
// Doctor Login - Updated to accept Medical ID or Email
app.post('/api/doctor/login', async (req, res) => {
  const { email, password, medicalId } = req.body;
  
  // Allow login with either email or medicalId
  const doctor = medicalId 
    ? await Doctor.findOne({ medicalId })
    : await Doctor.findOne({ email });
    
  if (!doctor) return res.status(401).json({ message: 'Invalid credentials' });
  const valid = await bcrypt.compare(password, doctor.password);
  if (!valid) return res.status(401).json({ message: 'Invalid credentials' });
  const token = jwt.sign({ email: doctor.email, name: doctor.name, role: 'doctor' }, JWT_SECRET, { expiresIn: '1h' });
  res.json({ token, user: { ...doctor data } });
});
```

#### `server/testServer.js` - Mock Doctor Login API
```javascript
// Mock doctor login endpoint
app.post('/api/doctor/login', (req, res) => {
  const { email, password, medicalId } = req.body;
  
  // Accept either email or medicalId
  if ((email || medicalId) && password) {
    res.json({
      token: 'mock-jwt-doctor-token-' + Date.now(),
      user: {
        _id: 'mock-doctor-123',
        email: email || 'doctor@example.com',
        name: 'Dr. Test Professional',
        medicalId: medicalId || 'MD-12345',
        specialty: 'Mental Health',
        specialization: 'Psychologist',
        role: 'doctor'
      }
    });
  }
});
```

### 2. Android App Updates

#### `ReliefNetRepository.kt` - Updated Doctor Login Method
```kotlin
suspend fun loginDoctor(medicalId: String, password: String): Result<AuthResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val request = mapOf(
                "medicalId" to medicalId,
                "password" to password
            )
            val response = apiService.loginDoctor(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Doctor login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### `ApiService.kt` - Updated API Interface
```kotlin
@POST("api/doctor/login")
suspend fun loginDoctor(@Body request: Map<String, String>): Response<AuthResponse>
```

#### `loginScreen.kt` - UI and Logic Updates

**New State Variables:**
```kotlin
var medicalId by remember { mutableStateOf("") }
```

**Updated Validation:**
```kotlin
fun handleAuth() {
    if (userType == "doctor" && isLoginMode) {
        // Doctor login requires medicalId and password
        if (medicalId.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in Medical ID and password"
            return
        }
    } else if (userType == "patient") {
        // Patient validation
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
    }
}
```

**Updated Login Call:**
```kotlin
val result = if (userType == "patient") {
    repository.loginPatient(email, password)
} else {
    repository.loginDoctor(medicalId, password)  // Using medicalId now
}
```

**UI Changes:**
- Medical ID field shown **ONLY** when `userType == "doctor"` and `isLoginMode == true`
- Email field shown **ONLY** for patients
- Button text shows "Doctor Sign In" vs "Patient Sign In" for clarity

## How It Works

### Doctor Login Flow
1. User selects "Doctor" button on login screen
2. UI displays:
   - Medical ID field (instead of email)
   - Password field
   - "Doctor Sign In" button
3. User enters Medical ID and password
4. App calls `/api/doctor/login` with `{ medicalId: "MD-XXXXX", password: "..." }`
5. Backend searches for doctor by Medical ID
6. Returns JWT token and doctor profile data

### Patient Login Flow (Unchanged)
1. User selects "Patient" button (default)
2. UI displays:
   - Email field
   - Password field
   - "Patient Sign In" button
3. User enters email and password
4. App calls `/api/login` with `{ email: "...", password: "..." }`
5. Returns JWT token and patient profile data

## Testing

### Test Doctor Login
Use any of these test Medical IDs (mock data):
- `MD-12345`
- Any Medical ID with any password

### Verify in App
1. Open ReliefNet app
2. Click "Doctor" button
3. See "Medical ID" field appears (email field hidden)
4. Enter a Medical ID and password
5. Click "Doctor Sign In"
6. Should login successfully

## Backend Configuration

### Production Server (server.js)
- Searches MongoDB for doctor by `medicalId` field
- Falls back to email if medicalId not provided (backward compatibility)

### Test Server (testServer.js)  
- Running on `http://localhost:5000`
- Accepts any Medical ID for testing
- No database required

## Security Notes
- Medical IDs are **NOT encrypted** in transit (use HTTPS in production)
- Password is hashed with bcrypt before storage
- JWT tokens expire after 1 hour
- Medical ID should be unique per doctor in database

## Future Enhancements
- Add "Forgot Medical ID" feature for doctors
- Validate Medical ID format (e.g., must start with "MD-")
- Add two-factor authentication for doctor accounts
- Allow doctors to set custom PIN for quick login
