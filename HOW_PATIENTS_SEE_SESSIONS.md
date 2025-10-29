# How Patients See Doctor Sessions

## Complete Flow Explanation

### 📋 **Overview**
When a doctor creates a session, it becomes immediately available for patients to view and book through the app. Here's how the complete flow works:

---

## 🔄 **The Complete Flow**

### **Step 1: Doctor Creates a Session**

**What Doctor Does:**
1. Doctor logs into the app
2. Doctor navigates to "Create Session" or "Add Session" button
3. Doctor fills in session details:
   - Date (e.g., 2025-11-05)
   - Time (e.g., 14:30)
   - Duration (e.g., 60 minutes)
   - Type (e.g., consultation, therapy, follow-up)
4. Doctor clicks "Create Session"

**What Happens in Backend:**
```
POST /api/doctor/sessions
Headers: Authorization: Bearer <doctor_token>
Body: {
  "date": "2025-11-05",
  "time": "14:30",
  "duration": 60,
  "type": "consultation"
}

Response: {
  "success": true,
  "data": {
    "_id": "session123",
    "doctor": "doctor_id",
    "date": "2025-11-05",
    "time": "14:30",
    "duration": 60,
    "type": "consultation",
    "status": "available"  // ← Important: Session is "available"
  }
}
```

---

### **Step 2: Patient Views Available Sessions**

**What Patient Does:**
1. Patient opens the app
2. Patient navigates to "Book Appointment" or "Find Doctor"
3. Patient selects a doctor from the list
4. **App automatically fetches available sessions for that doctor**
5. Patient sees available time slots displayed

**What Happens in Backend:**
```
GET /api/doctor/sessions?doctorId=<doctor_id>

This endpoint:
- Does NOT require authentication (patients can view without login)
- Filters sessions by doctor ID
- Only returns sessions with status="available"
- Sorts sessions by date and time

Response: {
  "sessions": [
    {
      "_id": "session123",
      "doctor": "doctor_id",
      "date": "2025-11-05",
      "time": "14:30",
      "duration": 60,
      "type": "consultation",
      "status": "available"
    },
    {
      "_id": "session124",
      "date": "2025-11-06",
      "time": "10:00",
      ...
    }
  ]
}
```

---

### **Step 3: Patient Books a Session**

**What Patient Does:**
1. Patient selects a time slot from the available sessions
2. Patient adds optional notes
3. Patient clicks "Book Appointment"

**What Happens in Backend:**
```
POST /api/bookings
Headers: Authorization: Bearer <patient_token>
Body: {
  "doctorId": "doctor_id",
  "sessionId": "session123",
  "appointmentDate": "2025-11-05",
  "appointmentTime": "14:30",
  "duration": 60,
  "notes": "First consultation"
}

This creates a booking and may update the session status
```

---

## 📱 **In the Android App**

### **Files Involved:**

1. **BookingScreen.kt** (Lines 46-60)
   ```kotlin
   // Fetch doctor details and available sessions
   LaunchedEffect(doctorId) {
       repository.getDoctorById(doctorId)
           .onSuccess { doc -> doctor = doc }
           .onFailure { error -> errorMessage = error.message }
       
       repository.getSessions(
           doctorId = doctorId, 
           status = "available", 
           token = TokenManager.getToken(context) ?: ""
       )
           .onSuccess { sessions ->
               availableSessions = sessions.sortedBy { 
                   "${it.sessionDate} ${it.sessionTime}" 
               }
               isLoading = false
           }
   }
   ```

2. **ReliefNetRepository.kt** (Lines 256-275)
   ```kotlin
   suspend fun getSessions(
       patientId: String? = null,
       doctorId: String? = null,
       status: String? = null,
       token: String
   ): Result<List<Session>> {
       return withContext(Dispatchers.IO) {
           val response = apiService.getSessions(
               patientId, doctorId, status, "Bearer $token"
           )
           if (response.isSuccessful && response.body() != null) {
               Result.success(response.body()!!.data ?: emptyList())
           } else {
               Result.failure(Exception("Failed to fetch sessions"))
           }
       }
   }
   ```

3. **ApiService.kt** (Lines 77-83)
   ```kotlin
   @GET("api/sessions")
   suspend fun getSessions(
       @Query("patientId") patientId: String? = null,
       @Query("doctorId") doctorId: String? = null,
       @Query("status") status: String? = null,
       @Header("Authorization") token: String
   ): Response<ApiResponse<List<Session>>>
   ```

---

## ⚠️ **Important Note: API Endpoint Mismatch**

**Issue Found:**
- Android app calls: `/api/sessions?doctorId=...`
- Backend endpoint is: `/api/doctor/sessions?doctorId=...`

**This needs to be fixed!** Either:

### Option 1: Update Android App (Recommended)
Change `ApiService.kt` line 77:
```kotlin
@GET("api/doctor/sessions")  // Changed from "api/sessions"
suspend fun getSessions(...)
```

### Option 2: Add Backend Route Alias
Add to `server.js`:
```javascript
app.get('/api/sessions', async (req, res) => {
  // Redirect to /api/doctor/sessions
  return app._router.handle(
    {...req, url: '/api/doctor/sessions' + req.url.slice(13)},
    res
  );
});
```

---

## 🔍 **Session Status Flow**

```
1. Doctor creates session → status: "available"
2. Patient books session  → status: "booked" (if booking updates session)
3. Session completed     → status: "completed"
4. Session cancelled     → status: "cancelled"
```

Only sessions with `status: "available"` are shown to patients!

---

## 🧪 **Testing Script**

Run `test-doctor-session.ps1` to test the complete flow:

```powershell
.\test-doctor-session.ps1
```

This script will:
1. Login as a doctor
2. Create a new session
3. Fetch available sessions (as a patient would)
4. Display all available sessions

---

## ✅ **Summary**

**How patients see doctor sessions:**

1. ✅ Doctor creates session → Session saved with `status="available"`
2. ✅ Patient opens booking screen for that doctor
3. ✅ App fetches: `GET /api/doctor/sessions?doctorId=<id>`
4. ✅ Backend returns only available sessions
5. ✅ Patient sees time slots and can book

**Current Status:** Backend is ready, but Android app needs API endpoint update!
