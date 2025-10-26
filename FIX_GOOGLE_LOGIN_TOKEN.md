# Fix Applied: Backend JWT Token Storage

## 🐛 Issue Found

After re-login with Google, the profile card was showing:
- ❌ Default person icon (gray placeholder)
- ❌ "Welcome to ReliefNet!" (not personalized)
- ✅ Google photo visible in top-right corner (from Firebase)

**Root Cause:** `GoogleAuthActivity` was only authenticating with Firebase but **not sending the token to backend** to get the JWT token and save it.

**Error in logs:**
```
E UserProfileViewModel: No token found
```

## ✅ Fix Applied

Updated `GoogleAuthActivity.kt` to:

### 1. Added Backend API Call
After Firebase authentication succeeds, now calls:
- `POST /api/auth/google` with `idToken` and `photoUrl`
- Receives JWT token and user profile from backend
- Saves JWT token using `TokenManager.saveToken()`
- Saves user info using `TokenManager.saveUserInfo()`

### 2. Updated Imports
```kotlin
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.utils.TokenManager
import kotlinx.coroutines.withContext
```

### 3. New Function Added
```kotlin
private fun authenticateWithBackend(idToken: String, photoUrl: String?) {
    // Calls backend API
    // Gets JWT token
    // Saves token and user info
    // Logs success/failure
}
```

## 🔄 New Flow

### Before (Broken):
1. User taps "Continue with Google"
2. Google Sign-In → Get ID token
3. Firebase Auth → Success
4. Activity finishes
5. ❌ **No JWT token saved**
6. ❌ **UserProfileViewModel can't fetch profile**
7. ❌ **Shows default placeholder**

### After (Fixed):
1. User taps "Continue with Google"
2. Google Sign-In → Get ID token + photo URL
3. Firebase Auth → Success
4. **✅ Call backend `/api/auth/google`**
5. **✅ Backend verifies token, creates/updates user**
6. **✅ Backend returns JWT token + user profile**
7. **✅ Save JWT token to SharedPreferences**
8. **✅ Save user info (email, name, userType)**
9. Activity finishes
10. **✅ UserProfileViewModel fetches profile with token**
11. **✅ Profile card shows photo, name, email**

## 📝 Changes Made

### File: `GoogleAuthActivity.kt`

**Added:**
- Backend authentication call after Firebase success
- JWT token storage
- User info storage
- Error handling for API calls
- Detailed logging

**Modified:**
- `onSuccess` callback now calls `authenticateWithBackend()`
- Added `authenticateWithBackend()` function

## 🧪 Testing After Fix

### Expected Behavior:

1. **Clear app data:**
   ```
   adb shell pm clear com.sentrive.reliefnet
   ```

2. **Open app and login with Google**

3. **Check logs:**
   ```powershell
   adb logcat | Select-String -Pattern "GoogleAuth|UserProfile"
   ```

4. **Should see:**
   ```
   D GoogleAuthActivity: Firebase sign-in successful
   D GoogleAuthActivity: Sending token to backend...
   D GoogleAuthActivity: Backend auth successful, token saved
   D GoogleAuthActivity: User: [Your Name], Photo: https://lh3.googleusercontent.com/...
   D UserProfileViewModel: User profile loaded: [Name], Photo: [URL]
   ```

5. **Profile card should show:**
   - ✅ Your actual Google profile photo (circular)
   - ✅ "Welcome, [Your Name]!"
   - ✅ Your email address
   - ✅ No more "No token found" errors

## 📱 Next Steps

1. **Rebuild app** (currently building...)
2. **Install:** `.\gradlew installDebug`
3. **Clear app data again:** `adb shell pm clear com.sentrive.reliefnet`
4. **Login with Google**
5. **Verify profile card displays correctly**

## 🔍 Verification Checklist

After rebuild and re-login:

- [ ] No "No token found" errors in logcat
- [ ] See "Backend auth successful" in logs
- [ ] Profile card shows your photo
- [ ] Greeting says "Welcome, [YourName]!"
- [ ] Email is displayed
- [ ] Photo is circular with border
- [ ] No app crashes

## 📊 API Flow

```
Android App                 Backend
    |                          |
    |--- POST /api/auth/google --->|
    |    {idToken, photoUrl}       |
    |                              |
    |                      Verify ID token
    |                      Find/update user
    |                      Save photoUrl
    |                              |
    |<--- JWT + User Profile  -----|
    |    {token, user}             |
    |                          |
Save to TokenManager        
Save user info              
    |                          |
    |--- GET /api/patient/profile --->|
    |    Authorization: Bearer JWT    |
    |                                  |
    |<--- User with photoUrl  ---------|
    |                          |
Display profile card        
```

## 🎯 Summary

**Problem:** JWT token wasn't being saved after Google login  
**Solution:** Added backend API call to exchange Google token for JWT  
**Result:** Profile card will now display user photo and info correctly  

**Status:** ✅ Fix applied, rebuilding app now

---

**Build Status:** In progress  
**Next:** Install and test
