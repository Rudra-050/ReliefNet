# How to Update Your Profile Photo

## ✅ Current Status

Your app is working correctly! The circular placeholder you're seeing means:
- ✅ Profile card is displaying
- ✅ UI is working properly
- ✅ Just need to update the photo URL in your user account

## 🎯 Quick Fix: Re-login with Google

Since you logged in **before** the photo URL feature was added, your account doesn't have the photo yet. Here's the easiest solution:

### Step 1: Clear App Data (Fastest Method)

**On Android Device/Emulator:**
1. Long press the ReliefNet app icon
2. Tap "App info"
3. Tap "Storage & cache"
4. Tap "Clear storage" or "Clear data"
5. Confirm

**OR via ADB:**
```powershell
adb shell pm clear com.sentrive.reliefnet
```

### Step 2: Re-login

1. Open the app (you'll see login screen again)
2. Tap "Continue with Google"
3. Select your Google account
4. Grant permissions
5. **Your photo will now be fetched and saved!**

## Alternative: Force Logout in App

If you want to add a logout button, I can add one to your app. For now, clearing app data is the quickest way.

## What Happens on Re-login?

When you login again with the updated code:

1. Google provides your ID token + profile picture URL
2. Backend receives: `idToken`, `photoUrl`
3. Backend calls: `admin.auth().verifyIdToken(idToken)`
4. Token contains: `email`, `name`, `picture`
5. Your existing user is found by email
6. User record is updated with photo URL
7. JWT token is returned with complete profile
8. App displays your photo!

## Verify It's Working

After re-login, check the logs:

```powershell
adb logcat | Select-String -Pattern "GoogleAuth|UserProfile|photoUrl"
```

You should see:
- ✅ `Google Sign-In - Photo: https://lh3.googleusercontent.com/...`
- ✅ `User profile loaded: [Name], Photo: [URL]`

## Manual Update (If You Don't Want to Re-login)

If you know your Google photo URL, you can update it manually:

### Get Your Token

In your app code, add this temporarily to log your token:
```kotlin
val token = TokenManager.getToken(context)
Log.d("TOKEN", "Current token: $token")
```

### Update Profile via API

```powershell
# Replace YOUR_TOKEN and YOUR_PHOTO_URL
curl -X PUT http://localhost:5000/api/patient/profile `
  -H "Authorization: Bearer YOUR_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{\"photoUrl\":\"https://lh3.googleusercontent.com/YOUR_PHOTO\"}'
```

### Restart App

Force close and reopen the app to see the updated photo.

## Why This Happened

The photo feature was added **after** you first logged in, so:
- ❌ Old login: Didn't capture photo URL
- ✅ New login: Captures photo URL automatically

From now on, **all new logins will automatically save photos!**

## Test It's Working

After re-login:

1. **Check the dashboard**
   - [ ] Profile card shows
   - [ ] Circular photo (not placeholder icon)
   - [ ] Your actual Google profile picture
   - [ ] "Welcome, [Name]!" greeting
   - [ ] Your email

2. **Check the logs**
   ```powershell
   adb logcat -d | Select-String -Pattern "User profile loaded"
   ```
   Should show: `Photo: https://lh3.googleusercontent.com/...`

3. **Check backend response**
   You can manually call the API to verify:
   ```powershell
   # Use your JWT token
   curl http://localhost:5000/api/patient/profile `
     -H "Authorization: Bearer YOUR_TOKEN"
   ```
   Response should include `"photoUrl": "https://..."`

## 🎉 Summary

**Current State:**
- ✅ Implementation is working
- ✅ UI is displaying correctly
- ✅ Just need to refresh your account data

**Quick Fix:**
1. Clear app data: `adb shell pm clear com.sentrive.reliefnet`
2. Open app and login with Google again
3. Photo will now be saved and displayed!

**From now on:**
- ✅ Every new Google login saves photo automatically
- ✅ Profile updates work
- ✅ Photo persists across app restarts

---

**Ready?** Clear app data and login again to see your photo! 🚀
