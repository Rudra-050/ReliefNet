# 🚀 ngrok Quick Start

## 1️⃣ Install & Setup (One-time)
```powershell
# Download from: https://ngrok.com/download
# Extract ngrok.exe to server folder

# Get auth token from: https://dashboard.ngrok.com/get-started/your-authtoken
ngrok config add-authtoken YOUR_AUTH_TOKEN_HERE
```

## 2️⃣ Start Backend with ngrok
```powershell
cd server
npm run dev:ngrok
```

## 3️⃣ Copy the ngrok URL
Look for output like:
```
Forwarding https://abc123.ngrok.io -> http://localhost:5000
```

## 4️⃣ Update gradle.properties
```properties
DEV_NGROK_URL=https://abc123.ngrok.io
```
⚠️ **No trailing slash!**

## 5️⃣ Build & Install
```powershell
cd ..\Reliefnet-android
.\gradlew assembleDebug
# Install APK on device and test from anywhere!
```

---

## 📋 Quick Commands

| What | Command |
|------|---------|
| Start with ngrok | `npm run dev:ngrok` |
| Start with LAN | `npm run dev:lan` |
| Start local only | `npm run dev` |
| View ngrok inspector | Open http://localhost:4040 |

## 🎯 Current Backend URLs

- **Debug (ngrok)**: Check `DEV_NGROK_URL` in gradle.properties
- **Debug (LAN)**: `http://192.168.29.232:5000/`
- **Release**: `https://reliefnet-production-e119.up.railway.app/`

---

See [NGROK_SETUP_GUIDE.md](NGROK_SETUP_GUIDE.md) for detailed instructions.
