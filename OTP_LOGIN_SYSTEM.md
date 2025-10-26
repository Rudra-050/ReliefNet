# OTP-Based Email Login System for Patients

## Overview
Patients can now log in using a **One-Time Password (OTP)** sent to their email address, providing a passwordless authentication option alongside traditional password-based login.

## Features Implemented

### 1. **Dual Login Methods**
- **Password Login**: Traditional email + password authentication
- **OTP Login**: Email-based OTP verification (passwordless)

### 2. **User Interface**
- Patient/Doctor toggle buttons
- Password/OTP login method selector (Patient only)
- Contextual button text:
  - "Send OTP" → sends OTP to email
  - "Verify OTP" → validates the entered code
  - "Resend OTP" → requests new OTP

### 3. **Security Features**
- OTPs expire after **5 minutes**
- 6-digit random OTP codes
- OTPs stored in-memory (testServer) or can use Redis in production
- One-time use: OTP deleted after successful verification

## Backend Implementation

### API Endpoints

#### 1. Send OTP
```http
POST /api/auth/send-otp
Content-Type: application/json

{
  "email": "patient@example.com"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "OTP sent to your email",
  "testOtp": "123456"  // FOR TESTING ONLY - Remove in production
}
```

**Response (Error - No Account):**
```json
{
  "success": false,
  "message": "No account found with this email. Please register first."
}
```

#### 2. Verify OTP
```http
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "patient@example.com",
  "otp": "123456"
}
```

**Response (Success):**
```json
{
  "success": true,
  "token": "jwt-token-here",
  "user": {
    "_id": "user-id",
    "email": "patient@example.com",
    "name": "Patient Name",
    "location": null,
    "role": "patient"
  },
  "message": "Login successful via OTP"
}
```

**Response (Error - Invalid OTP):**
```json
{
  "success": false,
  "message": "Invalid OTP. Please try again."
}
```

**Response (Error - Expired OTP):**
```json
{
  "success": false,
  "message": "OTP has expired. Please request a new OTP."
}
```

### Backend Files Modified

#### `server/testServer.js`
- Added in-memory OTP storage: `const otpStore = new Map()`
- Added `generateOTP()` helper function
- Implemented `/api/auth/send-otp` endpoint
- Implemented `/api/auth/verify-otp` endpoint
- OTPs logged to console for testing

#### `server/server.js`
- Same endpoints added for production use
- Validates patient exists before sending OTP
- TODO: Integrate with email service (nodemailer/SendGrid)

## Android Implementation

### Repository Methods

#### `ReliefNetRepository.kt`
```kotlin
suspend fun sendOTP(email: String): Result<OTPResponse>
suspend fun verifyOTP(email: String, otp: String): Result<AuthResponse>
```

### API Service

#### `ApiService.kt`
```kotlin
@POST("api/auth/send-otp")
suspend fun sendOTP(@Body request: Map<String, String>): Response<OTPResponse>

@POST("api/auth/verify-otp")
suspend fun verifyOTP(@Body request: Map<String, String>): Response<AuthResponse>
```

### Data Models

#### `ApiModels.kt`
```kotlin
data class OTPResponse(
    val success: Boolean,
    val message: String,
    val testOtp: String? = null  // For testing only
)

data class AuthResponse(
    val token: String,
    val user: User? = null,
    val doctor: Doctor? = null,
    val message: String? = null,
    val success: Boolean? = null
)
```

### UI Components

#### `loginScreen.kt`

**State Variables:**
```kotlin
var loginMethod by remember { mutableStateOf("password") }  // "password" or "otp"
var otpSent by remember { mutableStateOf(false) }
var otpCode by remember { mutableStateOf("") }
var testOtp by remember { mutableStateOf<String?>(null) }
```

**Handler Functions:**
```kotlin
fun handleSendOTP() { ... }      // Sends OTP to email
fun handleVerifyOTP() { ... }    // Verifies OTP code
```

**UI Elements:**
1. **Login Method Toggle**: Password/OTP buttons (Patient login only)
2. **Email Field**: Always visible for OTP login
3. **OTP Code Field**: Appears after OTP is sent
4. **Dynamic Button**: 
   - Shows "Send OTP" when OTP not sent
   - Shows "Verify OTP" after OTP sent
5. **Resend OTP Button**: Appears after OTP sent

## User Flow

### OTP Login Flow

1. **User Opens App**
   - Sees Patient/Doctor toggle
   - Selects "Patient"

2. **Select OTP Login**
   - Clicks "OTP" button (instead of "Password")
   - Password field disappears
   - Email field remains visible

3. **Enter Email**
   - User enters email: `patient@example.com`
   - Clicks "Send OTP" button

4. **OTP Sent**
   - Backend generates 6-digit OTP
   - OTP stored with 5-minute expiry
   - User sees: "OTP sent to your email"
   - (Testing: OTP shown in Toast)

5. **Enter OTP**
   - OTP code input field appears
   - User enters 6-digit code
   - Clicks "Verify OTP" button

6. **Verification**
   - ✅ **Success**: User logged in, navigates to Home
   - ❌ **Invalid OTP**: Error message shown
   - ⏰ **Expired**: "OTP has expired" message

7. **Resend OTP** (Optional)
   - User can click "Resend OTP"
   - New OTP generated and sent
   - Old OTP invalidated

## Testing Instructions

### Test Server (No Email Required)

1. **Start Test Server**
   ```bash
   cd server
   node testServer.js
   ```

2. **Open App**
   - Select "Patient"
   - Click "OTP" button

3. **Send OTP**
   - Enter any email: `test@example.com`
   - Click "Send OTP"
   - **Look at terminal** - OTP will be printed:
     ```
     📧 OTP for test@example.com: 123456 (expires in 5 minutes)
     ```
   - Also shown in Toast message (testing mode)

4. **Verify OTP**
   - Enter the OTP code from terminal/toast
   - Click "Verify OTP"
   - Should login successfully

### Production Server (With Email)

1. **Setup Email Service** (TODO)
   - Use nodemailer or SendGrid
   - Configure SMTP settings
   - Update `/api/auth/send-otp` to send real emails

2. **Remove Test OTP from Response**
   - Delete `testOtp` field from response
   - Remove console.log statements

3. **Use Redis for OTP Storage**
   - Replace `Map()` with Redis client
   - Set TTL (Time To Live) = 5 minutes
   ```javascript
   await redis.setex(`otp:${email}`, 300, otp);
   ```

## Security Considerations

### Current Implementation
- ✅ OTPs expire after 5 minutes
- ✅ One-time use (deleted after verification)
- ✅ 6-digit random codes (1 million combinations)
- ✅ Validates user exists before sending OTP

### Production Recommendations
1. **Rate Limiting**
   - Limit OTP requests: 3 attempts per email per hour
   - Prevent brute force attacks

2. **Email Verification**
   - Validate email format
   - Use verified email service (SendGrid, AWS SES)

3. **HTTPS Only**
   - Never send OTPs over HTTP
   - Use SSL/TLS certificates

4. **Redis for OTP Storage**
   - In-memory storage not scalable
   - Use Redis with automatic expiry

5. **Monitoring**
   - Log OTP request attempts
   - Alert on suspicious activity
   - Track success/failure rates

6. **IP-Based Throttling**
   - Limit requests per IP address
   - Prevent automated attacks

## Code Files Modified

### Backend
- ✅ `server/testServer.js` - Added OTP endpoints + in-memory storage
- ✅ `server/server.js` - Added OTP endpoints for production

### Android
- ✅ `ApiService.kt` - Added sendOTP() and verifyOTP()
- ✅ `ApiModels.kt` - Added OTPResponse model
- ✅ `ReliefNetRepository.kt` - Added OTP repository methods
- ✅ `loginScreen.kt` - Complete OTP UI flow

## Future Enhancements

### Email Integration
```javascript
const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-email@gmail.com',
    pass: 'your-app-password'
  }
});

async function sendOTPEmail(email, otp) {
  await transporter.sendMail({
    from: 'ReliefNet <noreply@reliefnet.com>',
    to: email,
    subject: 'Your ReliefNet Login OTP',
    html: `
      <h2>Your Login Code</h2>
      <p>Your OTP is: <strong style="font-size: 24px">${otp}</strong></p>
      <p>This code expires in 5 minutes.</p>
      <p>If you didn't request this, please ignore this email.</p>
    `
  });
}
```

### SMS OTP (Alternative)
- Use Twilio for SMS delivery
- Faster delivery than email
- Better for users without email access

### Biometric Login
- Fingerprint/Face ID after first OTP login
- Store encrypted token locally
- Fallback to OTP if biometric fails

## Troubleshooting

### "OTP not found"
- OTP expired (5 minutes)
- Email mismatch
- OTP already used

### "Failed to send OTP"
- Email doesn't exist in database
- Network connection issues
- Backend server not running

### OTP not received (Production)
- Check spam folder
- Verify email service configuration
- Check email service logs

## API Testing with cURL

### Send OTP
```bash
curl -X POST http://localhost:5000/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

### Verify OTP
```bash
curl -X POST http://localhost:5000/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "otp": "123456"}'
```

## Summary

✅ **Passwordless Login**: Patients can log in without remembering passwords
✅ **Enhanced Security**: Time-limited, one-time use OTPs
✅ **Better UX**: Simpler login process, especially for infrequent users
✅ **Dual Options**: Users can choose password OR OTP based on preference
✅ **Testing Ready**: Mock OTPs shown in console/toast for easy testing

The OTP login system is now **fully functional** and ready for testing! 🎉
