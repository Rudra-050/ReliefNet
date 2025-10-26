# Role-Based Authentication Testing Guide

## Prerequisites
- Server running on `http://localhost:5000`
- MongoDB connected
- SendGrid configured (for OTP emails)

## Test 1: Patient OTP Login Flow

### Step 1: Send OTP
```bash
curl -X POST http://localhost:5000/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"patient@example.com\",\"name\":\"Test Patient\"}"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "OTP sent to your email"
}
```

### Step 2: Check Email for OTP
- Check inbox for patient@example.com
- Note the 6-digit OTP code

### Step 3: Verify OTP
```bash
curl -X POST http://localhost:5000/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"patient@example.com\",\"otp\":\"123456\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "...",
    "email": "patient@example.com",
    "name": "Test Patient",
    "userType": "User"
  }
}
```

**✅ Verify: `userType` should be `"User"` (Patient)**

---

## Test 2: Doctor Login with Medical ID

### Step 1: Create Test Doctor (if not exists)
```bash
curl -X POST http://localhost:5000/api/doctor/register \
  -H "Content-Type: application/json" \
  -d "{
    \"name\":\"Dr. Test\",
    \"email\":\"doctor@example.com\",
    \"medicalId\":\"DOC123456\",
    \"password\":\"TestDoctor@123\",
    \"specialization\":\"Psychiatrist\"
  }"
```

### Step 2: Login with Medical ID
```bash
curl -X POST http://localhost:5000/api/doctor/login \
  -H "Content-Type: application/json" \
  -d "{\"medicalId\":\"DOC123456\",\"password\":\"TestDoctor@123\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "...",
    "name": "Dr. Test",
    "email": "doctor@example.com",
    "medicalId": "DOC123456",
    "specialization": "Psychiatrist",
    "userType": "Doctor"
  }
}
```

**✅ Verify: `userType` should be `"Doctor"`**

---

## Test 3: Doctor Login with Email/Password

```bash
curl -X POST http://localhost:5000/api/doctor/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"doctor@example.com\",\"password\":\"TestDoctor@123\"}"
```

**Expected Response:** Same as above with `userType: "Doctor"`

---

## Test 4: Decode JWT Token

Use this online tool or script to decode the JWT token:
- https://jwt.io/

**Token Payload Should Contain:**
```json
{
  "userId": "...",
  "userType": "Doctor" | "User",
  "iat": 1234567890,
  "exp": 1234571490
}
```

---

## Test 5: Verify Auto-Routing Logic

### In Android App (LinearProgressScreen.kt):

```kotlin
// Check token and userType from SharedPreferences
val token = TokenManager.getToken()
val userType = TokenManager.getUserType()

// Routing logic:
if (token != null) {
    if (userType == "Doctor") {
        // ✅ Navigate to DoctorDashboard
        navController.navigate("DoctorDashboard")
    } else {
        // ✅ Navigate to Home (Patient dashboard)
        navController.navigate("Home")
    }
} else {
    // ✅ Navigate to UserTypeSelection
    navController.navigate("UserTypeSelection")
}
```

---

## Test 6: End-to-End Manual Testing on Android

### Test Case 1: Doctor Auto-Redirect
1. Install app: `.\gradlew.bat installDebug`
2. Open app
3. Login as doctor (medical ID: DOC123456)
4. **Verify:** App shows DoctorDashboard
5. Force close app (swipe from recent apps)
6. Reopen app
7. **Verify:** App automatically redirects to DoctorDashboard (skips login)

### Test Case 2: Patient Auto-Redirect
1. Logout from doctor account
2. Login as patient (OTP or password)
3. **Verify:** App shows Home screen (patient dashboard)
4. Force close app
5. Reopen app
6. **Verify:** App automatically redirects to Home (skips login)

### Test Case 3: No Token (Fresh Install)
1. Clear app data: Settings → Apps → ReliefNet → Clear Data
2. Open app
3. **Verify:** App shows UserTypeSelection screen

---

## PowerShell Test Commands

### Test Patient OTP Send
```powershell
$body = @{
    email = "patient@example.com"
    name = "Test Patient"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5000/api/auth/send-otp" -Method Post -Body $body -ContentType "application/json"
```

### Test Doctor Login
```powershell
$body = @{
    medicalId = "DOC123456"
    password = "TestDoctor@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/login" -Method Post -Body $body -ContentType "application/json"

# Display token
Write-Host "Token: $($response.token)"
Write-Host "UserType: $($response.user.userType)"

# Decode JWT payload (base64)
$parts = $response.token.Split('.')
$payload = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($parts[1]))
Write-Host "Decoded Payload: $payload"
```

---

## Expected Outcomes

### ✅ Success Criteria:
1. Patient OTP login returns token with `userType: "User"`
2. Doctor medical-id login returns token with `userType: "Doctor"`
3. Doctor email/password login returns token with `userType: "Doctor"`
4. JWT tokens contain `userId`, `userType`, and `exp` fields
5. Android app auto-redirects based on saved `userType`:
   - Doctor → DoctorDashboard
   - Patient → Home
   - No token → UserTypeSelection

### ❌ Failure Indicators:
- Token missing `userType` field
- Wrong `userType` value (should be "Doctor" or "User")
- App doesn't auto-redirect on reopen
- App redirects to wrong dashboard
- Token not persisted in SharedPreferences

---

## Troubleshooting

### Issue: OTP not received
- Check SendGrid API key in server/.env
- Verify sender email is verified in SendGrid dashboard
- Check server logs for email sending errors

### Issue: Doctor login fails
- Verify doctor exists in MongoDB `doctors` collection
- Check password hash matches (use bcrypt to compare)
- Ensure medicalId is unique and correct

### Issue: App doesn't auto-redirect
- Check TokenManager.getToken() returns non-null value
- Verify TokenManager.getUserType() returns "Doctor" or "User"
- Check LinearProgressScreen.kt routing logic
- Clear app data and re-login

### Issue: Wrong dashboard shown
- Debug print `userType` value in LinearProgressScreen
- Verify token payload contains correct `userType`
- Check if token is being overwritten during login

---

## Quick Test Script (Node.js)

Run the comprehensive test suite:
```bash
cd server
node testRoleBasedAuth.js
```

This will test:
- Patient OTP flow
- Doctor medical-id login
- Doctor email/password login
- Token verification
- Auto-routing logic simulation

---

## Security Checks

### Token Expiry Test
1. Get a valid token
2. Wait 61 minutes (token expires after 1 hour)
3. Try API request with expired token
4. **Verify:** Should return 401 Unauthorized

### Invalid Token Test
```bash
curl -X GET http://localhost:5000/api/protected-endpoint \
  -H "Authorization: Bearer invalid.token.here"
```
**Verify:** Should return 401 or 403

---

## Database Verification

### Check User in MongoDB
```javascript
// Connect to MongoDB and run:
db.patients.findOne({ email: "patient@example.com" })
db.doctors.findOne({ medicalId: "DOC123456" })
```

### Verify Token in LoginSessions
```javascript
db.loginsessions.find({ userId: "user_id_here" }).sort({ loginTime: -1 })
```

---

## Next Steps

After successful endpoint testing:
1. Run Android app on emulator/device
2. Test actual user workflows
3. Monitor server logs during testing
4. Test edge cases (network failure, token expiry, etc.)
5. Performance testing with multiple concurrent logins
