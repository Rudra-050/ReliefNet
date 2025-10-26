# Google Login Testing Guide

## ⚠️ Important Note
**Google Sign-In cannot be tested from terminal** - it requires the Android app running on a physical device or emulator with Google Play Services.

## Your Configuration Status

✅ **Google Services Configured:**
- Project ID: `reliefnet-fc110`
- Package Name: `com.sentrive.reliefnet`
- OAuth Client IDs: Configured (3 clients)
- Firebase API Key: `AIzaSyDDCvRKMC6Vak6ZJaWty7xFwOzGja93I0s`

✅ **Implementation Files:**
- `GoogleAuthActivity.kt` - Credential Manager implementation
- `FirebaseAuthManager.kt` - Firebase Google Auth provider
- `loginScreen.kt` - UI with Google Sign-In button

## How to Test Google Login

### Method 1: Run on Physical Device (Recommended)

1. **Build and Install APK:**
   ```powershell
   cd C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android
   .\gradlew assembleDebug
   ```

2. **Connect Android Device via USB:**
   - Enable Developer Options on your device
   - Enable USB Debugging
   - Connect device to PC

3. **Install APK:**
   ```powershell
   .\gradlew installDebug
   ```

4. **Test Google Login:**
   - Open ReliefNet app on device
   - Tap "Continue with Google" button
   - Select your Google account
   - Grant permissions
   - Check if login succeeds and user is redirected

### Method 2: Run on Android Emulator

1. **Start Android Emulator:**
   - Open Android Studio
   - Click "Device Manager"
   - Start an emulator with Google Play Services

2. **Build and Run:**
   ```powershell
   cd C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android
   .\gradlew installDebug
   ```

3. **Test Login:**
   - Open app on emulator
   - Tap "Continue with Google"
   - Sign in with test Google account

### Method 3: Check Configuration (Terminal)

While you can't test the actual login from terminal, you can verify the configuration:

```powershell
# 1. Check if google-services.json is valid
cd C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android\app
Get-Content google-services.json | ConvertFrom-Json | Select-Object -ExpandProperty project_info

# 2. Verify build configuration
cd ..
.\gradlew dependencies | Select-String -Pattern "google|firebase|credential"

# 3. Check if GoogleAuthActivity is compiled
.\gradlew compileDebugKotlin

# 4. Verify SHA-1 certificate fingerprint (matches Firebase Console)
cd app
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String -Pattern "SHA1"
```

## Expected Google Login Flow

1. **User taps "Continue with Google"**
   - `loginScreen.kt` launches `GoogleAuthActivity`

2. **Credential Manager requests Google account**
   - Android Credential Manager shows account picker
   - User selects Google account

3. **Firebase authenticates with Google ID token**
   - `FirebaseAuthManager.signInWithGoogle()` called
   - Firebase verifies token and creates user session

4. **Backend registration/login**
   - App sends Google email to backend `/api/user/google-auth`
   - Backend creates/updates user record
   - JWT token returned and stored

5. **Navigation to dashboard**
   - User redirected to PatientDashboard or DoctorDashboard based on role

## Troubleshooting

### Common Issues:

1. **"Developer Error" or "Sign-in failed"**
   - SHA-1 fingerprint not added to Firebase Console
   - Solution: Add debug SHA-1 to Firebase project settings

2. **"API key not valid"**
   - google-services.json not properly placed in app/ directory
   - Solution: Ensure file is in correct location and sync Gradle

3. **"Google Play Services not available"**
   - Emulator doesn't have Play Services
   - Solution: Use emulator with Google Play Store image

4. **Backend error after Google login**
   - Backend not running or endpoint missing
   - Solution: Ensure server is running on port 5000

## Backend Endpoint for Google Auth

Your backend should have this endpoint (verify it exists):

```javascript
// POST /api/user/google-auth
// Body: { "email": "user@gmail.com", "name": "User Name" }
// Returns: { "token": "jwt_token", "user": {...} }
```

## Next Steps for Testing

1. ✅ Build the APK: `.\gradlew assembleDebug`
2. ✅ Install on device: `.\gradlew installDebug`
3. ✅ Open app and test Google login
4. ✅ Check logcat for errors: `adb logcat | Select-String -Pattern "GoogleAuth|Firebase"`
5. ✅ Verify backend receives request
6. ✅ Confirm user is redirected to dashboard

## Monitor Logs During Testing

```powershell
# Real-time app logs
adb logcat -s GoogleAuthActivity:* FirebaseAuthManager:*

# All ReliefNet logs
adb logcat | Select-String -Pattern "reliefnet"

# Google Sign-In specific logs
adb logcat | Select-String -Pattern "GoogleAuth|CredentialManager"
```

---

**Summary:** Google login requires running the app on a device/emulator. Use the commands above to build, install, and monitor the app during testing.
