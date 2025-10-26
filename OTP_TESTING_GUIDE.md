# Quick Test Guide: OTP Login System

## 🎯 How to Test OTP Login

### Step 1: Open the App
- App is already running on your emulator
- Backend server running at `http://localhost:5000`

### Step 2: Navigate to Patient OTP Login
1. On login screen, you'll see two buttons at top:
   - **Patient** | **Doctor**
   - Click **"Patient"**

2. Below that, you'll see two more buttons:
   - **Password** | **OTP**
   - Click **"OTP"**

### Step 3: Enter Email and Send OTP
1. Email field will be visible (password field hidden)
2. Enter any email: `test@example.com`
3. Click **"Send OTP"** button

### Step 4: Get the OTP Code
**IMPORTANT**: The OTP will appear in TWO places (testing mode):

1. **In PowerShell Terminal** (where testServer.js is running):
   ```
   📧 OTP for test@example.com: 123456 (expires in 5 minutes)
   ```

2. **In App Toast Message**:
   - "OTP sent to your email"
   - "Test OTP: 123456"

### Step 5: Enter and Verify OTP
1. OTP input field will appear
2. Type the 6-digit code (e.g., `123456`)
3. Click **"Verify OTP"** button
4. ✅ You should be logged in and redirected to Home!

## 🧪 Test Scenarios

### ✅ Successful Login
```
Email: any@example.com
OTP: (use the one shown in terminal/toast)
Result: Login successful → Navigate to Home
```

### ❌ Invalid OTP
```
Email: test@example.com
OTP: 000000 (wrong code)
Result: "Invalid OTP. Please try again."
```

### ⏰ Expired OTP (wait 5+ minutes)
```
Email: test@example.com
OTP: (old code after 5 minutes)
Result: "OTP has expired. Please request a new OTP."
```

### 🔄 Resend OTP
```
1. Click "Send OTP"
2. Wait for OTP
3. Don't enter it
4. Click "Resend OTP" button
5. Old OTP is invalidated
6. New OTP appears in terminal
```

## 📱 UI Elements to Look For

### When OTP Method Selected:
- ✅ Password field HIDDEN
- ✅ Email field VISIBLE
- ✅ Button says "Send OTP"

### After OTP Sent:
- ✅ OTP code input field appears
- ✅ Button says "Verify OTP"
- ✅ "Resend OTP" button visible

### Back to Password Login:
- Click "Password" button
- Password field reappears
- OTP fields disappear

## 🐛 If Something Goes Wrong

### OTP not appearing in terminal
- Check if testServer.js is running
- Look for this line in terminal: `✅ TEST SERVER running at http://localhost:5000`

### "Failed to send OTP" error
- Backend might not be running
- Check network connection (emulator → localhost via 10.0.2.2)

### App crashes
- Check logcat: `adb logcat | grep ReliefNet`
- Look for error messages

## 🎉 Expected Behavior

1. **Patient Login** + **OTP** selected
2. Enter email → Click "Send OTP"
3. See OTP in terminal: `📧 OTP for xxx: 123456`
4. Enter OTP code
5. Click "Verify OTP"
6. ✅ **Login successful!**

## 📝 Notes

- OTPs are **6 digits** (e.g., 123456)
- OTPs expire in **5 minutes**
- Each OTP is **one-time use**
- Test OTP shown in toast (remove in production)
- Console logs help you see what's happening

## 🚀 Next Steps

Once OTP login works:
1. Test with **real email service** (nodemailer)
2. Add **rate limiting** (prevent spam)
3. Remove **testOtp** from API response
4. Add **email templates** with styling

---

**Happy Testing!** 🎊
