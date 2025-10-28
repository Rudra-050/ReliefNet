# 🌐 Using ngrok for Remote Backend Access

This guide shows you how to use ngrok to access your local backend from your debug APK anywhere in the world, without needing to be on the same network as your PC.

## 📋 Prerequisites

1. **ngrok account** (free): https://dashboard.ngrok.com/signup
2. **ngrok installed**: https://ngrok.com/download

## 🚀 Quick Setup

### Step 1: Install ngrok

**Option A: Download manually**
1. Go to https://ngrok.com/download
2. Download for Windows
3. Extract `ngrok.exe` to the `server` folder (or add to PATH)

**Option B: Using Chocolatey**
```powershell
choco install ngrok
```

### Step 2: Setup ngrok Authentication

1. Sign up at https://dashboard.ngrok.com/signup
2. Copy your authtoken from https://dashboard.ngrok.com/get-started/your-authtoken
3. Run in terminal:
```powershell
ngrok config add-authtoken YOUR_AUTH_TOKEN_HERE
```

### Step 3: Start Backend with ngrok

```powershell
cd server
npm run dev:ngrok
```

This will:
- Start your backend on `localhost:5000`
- Create an ngrok tunnel
- Show you a public HTTPS URL (e.g., `https://abc123.ngrok.io`)

### Step 4: Configure Android App

1. Copy the ngrok HTTPS URL from the terminal (e.g., `https://abc123.ngrok.io`)
2. Open `Reliefnet-android/gradle.properties`
3. Update the `DEV_NGROK_URL` line:
```properties
DEV_NGROK_URL=https://abc123.ngrok.io
```
   **Important:** No trailing slash!

### Step 5: Build & Test

```powershell
cd ..\Reliefnet-android
.\gradlew assembleDebug
```

Install the APK on your device and test from anywhere! 🎉

## 🔄 URL Priority Order

The debug build uses this priority:
1. **DEV_NGROK_URL** (if set) → Remote access via ngrok
2. **DEV_HOST_IP** (if set) → Local network (LAN)
3. **Default** → Emulator loopback (10.0.2.2)

## 📝 Configuration Examples

### For ngrok (remote access from anywhere):
```properties
DEV_NGROK_URL=https://abc123.ngrok.io
DEV_HOST_IP=192.168.29.232
```
Result: Uses `https://abc123.ngrok.io/`

### For LAN (same Wi-Fi network):
```properties
DEV_NGROK_URL=
DEV_HOST_IP=192.168.29.232
```
Result: Uses `http://192.168.29.232:5000/`

### For Emulator only:
```properties
DEV_NGROK_URL=
DEV_HOST_IP=
```
Result: Uses `http://10.0.2.2:5000/`

## 🛠️ Manual ngrok Usage

If you prefer to run ngrok separately:

```powershell
# Terminal 1: Start backend
cd server
npm run dev

# Terminal 2: Start ngrok
ngrok http 5000
```

Then copy the HTTPS URL to `gradle.properties`.

## ⚠️ Important Notes

1. **Free ngrok limitations:**
   - URL changes every time you restart ngrok
   - Limited to 40 connections/minute
   - Session expires after 2 hours

2. **Security:**
   - Your backend is publicly accessible while ngrok is running
   - Don't commit ngrok URLs to git
   - Stop ngrok when not testing

3. **URL Format:**
   - ✅ Correct: `https://abc123.ngrok.io`
   - ❌ Wrong: `https://abc123.ngrok.io/`
   - ❌ Wrong: `http://abc123.ngrok.io`

## 🎯 Use Cases

- **Test on physical device** without being on same Wi-Fi
- **Share app with testers** remotely
- **Test from mobile data** (not just Wi-Fi)
- **Debug real-world scenarios** outside your network

## 📊 Backend Options Comparison

| Method | Command | Accessible From | URL Example |
|--------|---------|----------------|-------------|
| Local only | `npm run dev` | Emulator only | `http://10.0.2.2:5000/` |
| LAN | `npm run dev:lan` | Same Wi-Fi | `http://192.168.29.232:5000/` |
| **ngrok** | `npm run dev:ngrok` | **Anywhere** | `https://abc123.ngrok.io/` |
| Railway | N/A (cloud) | Anywhere | `https://reliefnet-production-e119.up.railway.app/` |

## 🔍 Troubleshooting

### ngrok not found
- Make sure ngrok.exe is in the server folder or in your PATH
- Try running `ngrok version` to verify installation

### Connection refused
- Ensure backend is running before ngrok starts
- Check if port 5000 is already in use

### Invalid authtoken
- Run: `ngrok config add-authtoken YOUR_TOKEN`
- Get your token from: https://dashboard.ngrok.com/get-started/your-authtoken

### APK still using old URL
- Clean and rebuild: `.\gradlew clean assembleDebug`
- Verify `gradle.properties` has correct DEV_NGROK_URL
- Check the URL has no trailing slash

## 💡 Tips

- **Keep the ngrok terminal open** while testing
- **Save your authtoken** - you only need to set it once
- **Use ngrok's web interface** at http://localhost:4040 to inspect requests
- **Update gradle.properties** each time ngrok restarts (free plan gives new URL)

---

Happy testing! 🚀
