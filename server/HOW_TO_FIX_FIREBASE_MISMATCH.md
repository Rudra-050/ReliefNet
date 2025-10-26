# 🔥 How to Fix Firebase Project Mismatch

## Problem
The Android app is using Firebase project `reliefnet-fc110`, but the backend server has a service account key for a different project `relief-b84a3`. This causes authentication failures (401 Unauthorized).

## Solution
Download the correct service account key for the `reliefnet-fc110` project and replace the existing one.

## Steps

### 1. Go to Firebase Console
Visit: https://console.firebase.google.com/

### 2. Select the Correct Project
- Click on **reliefnet-fc110** project (NOT relief-b84a3)

### 3. Access Project Settings
- Click the **gear icon** ⚙️ next to "Project Overview"
- Select **Project settings**

### 4. Go to Service Accounts Tab
- Click on the **Service accounts** tab
- You should see "Firebase Admin SDK"

### 5. Generate New Private Key
- Click **Generate new private key** button
- A dialog will appear warning you to keep it secure
- Click **Generate key**
- A JSON file will be downloaded (e.g., `reliefnet-fc110-firebase-adminsdk-xxxxx.json`)

### 6. Replace the Service Account Key
- Rename the downloaded file to `serviceAccountKey.json`
- Replace the existing file at:
  ```
  ReliefNet/server/serviceAccountKey.json
  ```

### 7. Restart the Backend Server
```bash
cd server
# Kill the existing Node process first
# Then start the server
node server.js
```

### 8. Test Again
- Clear app data: `adb shell pm clear com.sentrive.reliefnet`
- Launch app and try Google Sign-In
- It should now work! ✅

## What Was Wrong?

**App Configuration:**
- Project ID: `reliefnet-fc110` (from `google-services.json`)
- Firebase tokens have audience: `reliefnet-fc110`

**Server Configuration (OLD):**
- Project ID: `relief-b84a3` (from `serviceAccountKey.json`)
- Firebase Admin SDK expects: `relief-b84a3`

**Result:** Token mismatch → 401 Unauthorized ❌

**Server Configuration (NEW):**
- Project ID: `reliefnet-fc110` (from new `serviceAccountKey.json`)
- Firebase Admin SDK expects: `reliefnet-fc110`

**Result:** Token match → Authentication successful ✅

## Note
Keep the `serviceAccountKey.json` file secure and never commit it to public repositories!
