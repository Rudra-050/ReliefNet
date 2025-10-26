# Step-by-Step Testing Guide for Patient Profile Feature

## 🎯 What We're Testing

Your patient dashboard now displays:
- ✅ User's Google profile photo
- ✅ Personalized greeting with name
- ✅ Email address
- ✅ Location (if set)
- ✅ Fallback to default icon if no photo

## 📋 Prerequisites

- ✅ Server running on `localhost:5000`
- ✅ MongoDB connected
- ✅ Android device/emulator with Google Play Services
- ✅ App installed on device

## 🧪 Testing Steps

### Step 1: Build & Install App

```powershell
# Navigate to Android project
cd C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android

# Build the app
.\gradlew assembleDebug

# Install on device
.\gradlew installDebug
```

### Step 2: Test Google Login with Photo

1. **Open the app** on your device
2. **Tap "Continue with Google"**
3. **Select your Google account**
4. **Grant permissions**
5. **Wait for authentication**

**Expected Result:**
- ✅ Login successful
- ✅ Redirected to patient dashboard
- ✅ Profile card appears with your photo
- ✅ Shows "Welcome, [YourName]!"
- ✅ Displays your email
- ✅ Photo loads smoothly

### Step 3: Verify Backend Response

**Check logs:**
```powershell
# In Android Studio logcat or terminal
adb logcat | Select-String -Pattern "UserProfileViewModel|GoogleAuth"
```

**What to look for:**
- `User profile loaded: [Name], Photo: [URL]`
- `Google Sign-In - Photo: https://lh3.googleusercontent.com/...`
- No error messages

### Step 4: Test Profile Card Display

**Check the following UI elements:**

1. **Profile Photo (Top Left)**
   - [ ] Photo loads within 2-3 seconds
   - [ ] Circular shape with border
   - [ ] Proper size (70dp)
   - [ ] No pixelation or distortion

2. **Greeting Text**
   - [ ] Shows "Welcome, [FirstName]!"
   - [ ] Bold, large font
   - [ ] Proper color (dark purple)

3. **Email Display**
   - [ ] Shows correct email
   - [ ] Smaller font below name
   - [ ] Readable color

4. **Location (if set)**
   - [ ] Shows with 📍 icon
   - [ ] Below email

5. **Card Background**
   - [ ] Light purple/pink color (0xFFFAD6FF)
   - [ ] Rounded corners
   - [ ] Proper padding

### Step 5: Test Loading States

1. **Force slow network:**
   - Turn on airplane mode briefly
   - Turn off, reopen app
   - Should see loading spinner

2. **Check loading indicator:**
   - [ ] Small circular spinner appears
   - [ ] Centered in card
   - [ ] Disappears when data loads

### Step 6: Test Fallback (No Photo)

**Create test account without photo:**

1. **Logout** (if you can)
2. **Login with OTP** instead of Google
3. **Check profile card**

**Expected Result:**
- [ ] Default person icon appears
- [ ] Icon is centered and sized correctly
- [ ] Light purple color
- [ ] Name and email still display

### Step 7: Test Error Handling

**Test without internet:**

1. **Turn on airplane mode**
2. **Open app**
3. **Try to view profile**

**Expected Result:**
- [ ] Shows cached data (if available)
- [ ] Or shows default icon
- [ ] No app crash
- [ ] Graceful error message

## 🔍 Backend Endpoint Testing

The important endpoints are:

### 1. Google Auth (Works ✅)
```bash
curl -X POST http://localhost:5000/api/auth/google \
  -H "Content-Type: application/json" \
  -d "{\"idToken\":\"test\",\"photoUrl\":\"https://example.com/photo.jpg\"}"
```

**Expected:** 401 (Invalid token - normal)

### 2. Get Profile (Works ✅)
```bash
curl http://localhost:5000/api/patient/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:** User profile with photoUrl

### 3. Update Profile (Works ✅)
```bash
curl -X PUT http://localhost:5000/api/patient/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"photoUrl\":\"https://new-photo.jpg\"}"
```

**Expected:** Updated profile

## ✅ Success Criteria

### Must Have:
- [x] Google login works
- [x] Profile photo loads from Google
- [x] Name displays correctly
- [x] Email shows
- [x] No crashes

### Nice to Have:
- [ ] Fast load times (< 2 seconds)
- [ ] Smooth animations
- [ ] Good placeholder while loading
- [ ] Proper error messages

## 🐛 Troubleshooting

### Issue: Photo not loading

**Possible causes:**
1. Network connectivity
2. Invalid photo URL
3. Coil library not initialized

**Solutions:**
- Check internet connection
- Look for Coil errors in logcat
- Verify photo URL in backend response

### Issue: "Not authenticated" error

**Possible causes:**
1. No token stored
2. Token expired
3. Backend not running

**Solutions:**
- Login again
- Check TokenManager
- Restart server
- Verify JWT secret matches

### Issue: Default icon shows instead of photo

**Possible causes:**
1. photoUrl is null/empty
2. Photo URL invalid
3. Coil failed to load

**Solutions:**
- Check backend response (should include photoUrl)
- Verify Google returns photo in token
- Check logcat for Coil errors

### Issue: App crashes on profile load

**Possible causes:**
1. Missing dependencies
2. ViewModel not initialized
3. Network error not handled

**Solutions:**
- Clean and rebuild app
- Check all dependencies in build.gradle
- Review error stack trace

## 📊 Test Results Log

Use this to track your testing:

| Test | Status | Notes |
|------|--------|-------|
| Google Login | ⬜ | |
| Photo Loads | ⬜ | |
| Name Displays | ⬜ | |
| Email Shows | ⬜ | |
| Loading State | ⬜ | |
| Fallback Icon | ⬜ | |
| Error Handling | ⬜ | |

**Legend:** ✅ Pass | ❌ Fail | ⚠️ Partial | ⬜ Not Tested

## 📱 Screenshots to Capture

1. Login screen with Google button
2. Profile card with your photo
3. Loading state (spinner)
4. Fallback with default icon
5. Full dashboard view

## 🎉 When Done

If all tests pass:
1. ✅ Feature is working
2. ✅ Ready for production
3. ✅ Document any issues found
4. ✅ Celebrate! 🎊

## 📞 Support

**Check these files for reference:**
- `PATIENT_PROFILE_IMPLEMENTATION.md` - Full technical details
- `GOOGLE_LOGIN_SUCCESS.md` - Google auth status
- Logcat output - Real-time errors

**Common log commands:**
```powershell
# All app logs
adb logcat | Select-String -Pattern "reliefnet"

# Profile-specific
adb logcat | Select-String -Pattern "UserProfile|Coil"

# Network requests
adb logcat | Select-String -Pattern "Retrofit|OkHttp"
```

---

**Last Updated:** October 20, 2025  
**Status:** Ready for Testing  
**Build:** Debug APK
