# How to Access Doctor/Professional Pages in ReliefNet

## 🔧 Fixed Issue
The navigation was incorrectly mapping `DoctorRegistrationScreen` to a test screen. This has been **fixed**!

## ✅ Available Doctor Pages

### 1. **Doctor Registration Screen**
- **Route**: `DoctorRegistrationScreen`
- **Purpose**: Register as a mental health professional
- **Features**:
  - First Name, Last Name
  - Email, Phone, Medical ID
  - Specialization (dropdown with mental health specialties)
  - Hospital/Clinic
  - Password
  - Real API integration with backend (`POST /api/doctors/register`)

### 2. **Professional Login Screen**
- **Route**: `ProfessionalLoginScreen`
- **Purpose**: Login for doctors/therapists

### 3. **Doctor Chat Screen**
- **Route**: `DoctorChatScreen`
- **Purpose**: Chat interface for doctors

### 4. **Doctor Profile Screen**
- **Accessible from**: Doctor listings in HomeScreen
- **Purpose**: View doctor details

## 🚀 How to Navigate to Doctor Pages

Since there's no UI button to access these pages yet, you have **3 options**:

### Option 1: Add Navigation Button to LoginScreen

Add a button in `loginScreen.kt` to navigate to doctor registration:

```kotlin
// Add this button after the patient login section
TextButton(onClick = { navHostController.navigate("DoctorRegistrationScreen") }) {
    Text("Register as a Professional", color = Color.White)
}
```

### Option 2: Temporary Test Navigation

Modify `LinearProgressScreen.kt` to navigate directly to doctor registration for testing:

```kotlin
LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(2000)
    navHostController.navigate("DoctorRegistrationScreen"){ // Change from LoginScreen
        popUpTo("LinerarProgress") { inclusive = true }
    }
}
```

### Option 3: Create User Type Selection Screen

Create a new screen after `LinearProgress` that asks:
- **"Are you a Patient?"** → Navigate to `LoginScreen`
- **"Are you a Professional?"** → Navigate to `ProfessionalLoginScreen`

## 📁 Doctor-Related Files

| File | Purpose | Status |
|------|---------|--------|
| `DoctorRegistration.kt` | Doctor sign-up form | ✅ Backend integrated |
| `ProfessionalsLogin.kt` | Doctor login | ⚠️ Needs backend integration |
| `DoctorChatScreen.kt` | Doctor chat interface | 📝 UI only |
| `Doctor'sProfile.kt` | Doctor profile view | 📝 UI only |
| `DoctorsSortScreen.kt` | Doctor filtering/sorting | 📝 UI only |

## 🔗 Backend Endpoints Available

```
POST /api/doctors/register  - Register new doctor
POST /api/doctors/login     - Doctor login
GET  /api/doctors          - Get all doctors
GET  /api/doctors/{id}      - Get doctor by ID
PUT  /api/doctors/{id}      - Update doctor info
```

## 🎯 Next Steps

1. **Add "Register as Professional" button** to LoginScreen
2. **Integrate ProfessionalLoginScreen** with backend API
3. **Create User Type Selection Screen** for better UX
4. **Test doctor registration** with backend

## 🐛 Current Limitation

**Google Sign-In** won't work until the other intern adds your SHA-1 fingerprint to Firebase:
```
SHA1: 0D:6A:7E:73:D0:F2:F5:5B:43:71:2A:83:DB:65:C4:F8:99:3C:D4:16
```

But **email/password registration and login work perfectly** for both patients and doctors! 🎉
