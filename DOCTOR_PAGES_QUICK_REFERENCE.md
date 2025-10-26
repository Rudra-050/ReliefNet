# Doctor Pages Quick Reference Guide

## 🚀 Quick Start

### 1. Start the Backend Server
```bash
cd server
node server.js
```
Server should be running on `http://localhost:5000`

### 2. Build Android App
```bash
cd Reliefnet-android
.\gradlew.bat assembleDebug
```

### 3. Install on Device/Emulator
```bash
.\gradlew.bat installDebug
```
Or use Android Studio to run the app.

## 🔐 Test Credentials

### Doctor Account
- **Medical ID**: `RN-515340`
- **Password**: `Test@123`

### Patient Account (if needed)
- Check existing patient accounts in database

## 📱 Doctor App Flow

### Login Flow
1. Open app → Splash → User Type Selection
2. Tap "Professional Login"
3. Enter Medical ID and Password
4. **Auto-redirect** to Doctor Dashboard ✨

### Dashboard Navigation
Bottom navigation bar provides quick access to:
- **Home**: Dashboard with quick action cards
- **Chats**: Patient conversations list
- **Sessions**: Appointment history
- **Profile**: Doctor information and settings

### Quick Actions (Dashboard Cards)
- **Chats** → View patient conversations
- **Feedback** → See patient reviews and ratings
- **Payments** → Payment transaction history
- **Help** → Support and FAQs

### Profile Menu
From Profile screen, access:
- **Edit Profile** → Update name, specialization, bio
- **Sessions** → View appointment history
- **Feedbacks** → Patient reviews
- **Payment History** → Transaction records
- **Help & Support** → Contact information

## 🔄 Navigation Map

```
DoctorDashboard (Bottom Nav: Home)
├── DoctorChats (Bottom Nav: Chats)
│   └── DoctorChatScreen (existing)
├── DoctorSessions (Bottom Nav: Sessions)
├── DoctorProfile (Bottom Nav: Profile)
│   ├── EditDoctorProfile
│   ├── DoctorFeedback
│   ├── DoctorPayments
│   └── DoctorHelp
└── Quick Actions
    ├── DoctorChats
    ├── DoctorFeedback
    ├── DoctorPayments
    └── DoctorHelp
```

## 🧪 Testing Checklist

### ✅ Authentication
- [ ] Doctor can login with medical ID
- [ ] App saves token and user info
- [ ] Auto-redirects to Doctor Dashboard on reopen
- [ ] Logout clears token (implement if needed)

### ✅ Dashboard
- [ ] Doctor name displays correctly
- [ ] Bottom navigation works (all 4 tabs)
- [ ] Quick action cards navigate correctly
- [ ] Background displays properly

### ✅ Chats
- [ ] Chats list loads (empty or with data)
- [ ] Can navigate to individual chat
- [ ] Error handling works

### ✅ Sessions
- [ ] Sessions list loads
- [ ] Shows session details (date, time, status)
- [ ] Empty state displays when no sessions
- [ ] Error handling works

### ✅ Profile
- [ ] Profile loads with doctor information
- [ ] All navigation buttons work
- [ ] Edit Profile loads current data
- [ ] Edit Profile saves changes
- [ ] Back navigation works

### ✅ Feedback
- [ ] Feedback list loads
- [ ] Shows average rating and count
- [ ] Individual feedback cards display
- [ ] Empty state handles no feedback

### ✅ Payments
- [ ] Payment list loads
- [ ] Shows total paid and pending
- [ ] Payment details display correctly
- [ ] Empty state handles no payments

### ✅ Help & Support
- [ ] FAQ displays
- [ ] Contact info visible

## 🐛 Troubleshooting

### Issue: "Not authenticated" error
**Solution**: Ensure doctor is logged in. Check TokenManager has valid token.

### Issue: Empty lists everywhere
**Reason**: No data seeded for test doctor yet.
**Solution**: This is expected for new account. See data seeding section below.

### Issue: Backend not responding
**Check**:
1. Server is running on port 5000
2. No other process using port 5000
3. Check server logs for errors
4. Verify ApiConfig.BASE_URL in Android app

### Issue: Build fails
**Solution**: Clean build cache
```bash
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

## 📊 Seeding Test Data (Optional)

### Create Test Session
```javascript
// In MongoDB or via POST /api/sessions
{
  "doctorId": "DOCTOR_ID_FROM_LOGIN",
  "patientId": "EXISTING_PATIENT_ID",
  "sessionDate": "2025-10-25",
  "sessionTime": "10:00 AM",
  "status": "scheduled",
  "notes": "Initial consultation"
}
```

### Create Test Feedback
```javascript
// Insert into feedback collection
{
  "doctorId": "DOCTOR_ID_FROM_LOGIN",
  "patientId": "EXISTING_PATIENT_ID",
  "patientName": "Test Patient",
  "rating": 5,
  "comment": "Excellent service!",
  "createdAt": new Date()
}
```

### Create Test Payment
```javascript
// Insert into payments collection (check your Payment model)
{
  "doctorId": "DOCTOR_ID_FROM_LOGIN",
  "amount": 500,
  "status": "completed",
  "patientName": "Test Patient",
  "createdAt": new Date()
}
```

## 🔍 API Endpoints Reference

### Doctor Profile
```
GET  /api/doctor/profile
PUT  /api/doctor/profile
Headers: Authorization: Bearer <JWT>
```

### Doctor Chats
```
GET  /api/doctor/chats
Headers: Authorization: Bearer <JWT>
Response: { success, conversations: [...] }
```

### Doctor Feedback
```
GET  /api/doctor/feedback
Headers: Authorization: Bearer <JWT>
Response: { success, averageRating, count, feedback: [...] }
```

### Doctor Payments
```
GET  /api/doctor/payments
Headers: Authorization: Bearer <JWT>
Response: { success, totalPaid, totalPending, payments: [...] }
```

### Doctor Sessions
```
GET  /api/sessions?doctorId=<ID>
Headers: Authorization: Bearer <JWT>
Response: { success, data: [...] }
```

## 📝 Notes

### Current Behavior
- All screens handle empty data gracefully
- Loading indicators shown during API calls
- Error messages displayed for failed requests
- Navigation is fully wired and functional

### Known UI Items
- Patient names in sessions show as IDs (can be enhanced with patient lookup)
- Chat tap navigates to generic chat screen (can pass conversation params)
- Bottom nav icons have minor deprecation warnings (cosmetic only)

## 🎯 Success Indicators

✅ **Working Correctly** if you see:
- Doctor Dashboard loads after login
- Bottom navigation switches between tabs
- All screens show loading then data/empty state
- No app crashes
- Network requests visible in server logs

🎉 **Ready for Demo** when:
- Can login as doctor
- Navigate to all pages
- Edit profile saves changes
- (Optionally) Sample data displays in lists

---

**Need Help?** Check:
- `DOCTOR_PAGES_IMPLEMENTATION_COMPLETE.md` for technical details
- Server logs for backend errors
- Logcat in Android Studio for client errors
- Network tab to verify API calls
