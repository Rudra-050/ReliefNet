# ReliefNet API - Quick Reference Card

## 🔌 Server Status
- **URL:** `http://localhost:5000`
- **Socket.IO:** `http://localhost:5000`
- **Status:** ✅ Running

## 📱 Android Configuration
- **Emulator:** `10.0.2.2:5000`
- **Physical Device:** Update IP in `ApiConfig.kt`

---

## 🚀 Quick Usage Examples

### 1. Login
```kotlin
repository.loginPatient("email@test.com", "password")
    .onSuccess { response ->
        val token = response.token  // Save this!
        val user = response.user
    }
```

### 2. Get Doctors
```kotlin
repository.getDoctors(specialty = "Psychologist")
    .onSuccess { doctors ->
        // Display list
    }
```

### 3. Book Session
```kotlin
repository.createSession(
    patientId = userId,
    doctorId = doctorId,
    sessionDate = "2025-10-25",
    sessionTime = "14:00",
    token = authToken
)
```

### 4. Socket.IO (Real-time)
```kotlin
SocketManager.connect()
SocketManager.registerUser(userId, "User")

socket?.on("call:incoming") { /* Handle */ }
```

---

## 📦 Key Files

| File | Purpose |
|------|---------|
| `ApiConfig.kt` | URLs & endpoints |
| `ApiService.kt` | All API methods |
| `RetrofitClient.kt` | HTTP client |
| `SocketManager.kt` | WebSocket client |
| `ReliefNetRepository.kt` | Business logic |
| `ApiModels.kt` | Data models |
| `ExampleApiUsage.kt` | Copy-paste examples |
| `ApiTestScreen.kt` | Test connection |

---

## 🔑 Common Patterns

### With Loading State
```kotlin
var isLoading by mutableStateOf(false)
var error by mutableStateOf<String?>(null)

isLoading = true
repository.getDoctors()
    .onSuccess { doctors -> /* ... */ }
    .onFailure { error = it.message }
isLoading = false
```

### With Authentication
```kotlin
repository.createSession(..., token = "Bearer $authToken")
```

### Error Handling
```kotlin
.onFailure { error ->
    when {
        error.message?.contains("401") == true -> logout()
        error.message?.contains("network") == true -> showRetry()
        else -> showError(error.message)
    }
}
```

---

## 🛠️ Troubleshooting

| Problem | Solution |
|---------|----------|
| Can't connect | Check server is running |
| 401 Unauthorized | Need valid auth token |
| 404 Not Found | Check endpoint spelling |
| Timeout | Verify IP address (physical device) |
| Cleartext error | Already fixed in manifest |

---

## 📞 API Endpoints Cheatsheet

```
POST   /api/register                    # User register
POST   /api/login                       # User login
POST   /api/doctors/register            # Doctor register
POST   /api/doctors/login               # Doctor login
GET    /api/doctors                     # List doctors
GET    /api/doctors/:id                 # Get doctor
POST   /api/sessions                    # Book session
GET    /api/sessions                    # Get bookings
GET    /api/notifications               # Get notifications
POST   /api/upload                      # Upload photo
POST   /api/payments/create-order       # Payment
```

---

## ✅ Integration Steps

1. ✅ Backend running on port 5000
2. ✅ Network layer created in Android
3. ✅ Permissions added to manifest
4. ✅ Dependencies added to build.gradle
5. ⏭️ Update UI screens to use API
6. ⏭️ Implement authentication flow
7. ⏭️ Add real-time features

---

**Ready to build! 🎉**
