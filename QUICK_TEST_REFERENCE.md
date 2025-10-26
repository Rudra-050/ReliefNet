# Quick Test Reference Card

## ✅ Current Status
- **Backend**: All endpoints working ✅
- **Android App**: Built & installed ✅
- **Device**: Pixel_7a_2(AVD) ✅
- **Server**: Running on port 5000 ✅

## 🎯 Quick Test (2 minutes)

1. Open app on device
2. Login with Google
3. Look for profile card on dashboard
4. Verify:
   - [ ] Photo loads
   - [ ] Name shows
   - [ ] Email displays
   - [ ] No crashes

## 📱 What You Should See

```
┌─────────────────────────────────────┐
│  ┌────┐                             │
│  │    │  Welcome, John!             │
│  │ 📷 │  john@gmail.com             │
│  │    │  📍 New York                │
│  └────┘                             │
└─────────────────────────────────────┘
```

## 🐛 If Something Goes Wrong

### No Photo?
- Check internet connection
- Look at logcat for Coil errors
- Verify photoUrl in backend response

### No Profile Card?
- Check if HomePage is using UserProfileCard
- Look for ViewModel errors in logs
- Verify token is stored

### App Crashes?
- Check logcat: `adb logcat | Select-String "AndroidRuntime"`
- Look for build errors
- Clean and rebuild

## 📊 Backend Endpoints Working

✅ `/api/patient/profile` (GET) - Returns user with photoUrl  
✅ `/api/patient/profile` (PUT) - Updates profile  
✅ `/api/auth/google` (POST) - Google auth with photo  
✅ JWT authentication - Active

## 🔍 Monitor App in Real-Time

```powershell
# Watch all app activity
adb logcat | Select-String -Pattern "reliefnet|sentrive"

# Watch profile loading
adb logcat | Select-String -Pattern "UserProfile|Coil"

# Watch network requests
adb logcat | Select-String -Pattern "Retrofit"
```

## 📞 Get Help

1. Check `TESTING_GUIDE_PATIENT_PROFILE.md` for detailed steps
2. Check `PATIENT_PROFILE_IMPLEMENTATION.md` for technical details
3. Look at logcat output for errors
4. Verify server is running: `curl http://localhost:5000/api/patient/profile`

## 🎉 Success Looks Like

- ✅ App opens without crash
- ✅ Google login works
- ✅ Dashboard loads
- ✅ Profile card shows with your photo
- ✅ Greeting is personalized
- ✅ Email is correct
- ✅ Smooth loading animation

---

**Ready?** Open the app and test it now! 🚀
