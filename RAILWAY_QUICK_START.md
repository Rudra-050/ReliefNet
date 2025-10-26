# 🚄 Railway Quick Start

Fast track to deploying ReliefNet backend on Railway.

## ⚡ Quick Setup (5 Minutes)

### 1. Prepare Your Configuration

Run the preparation script:
```bash
cd server
node prepare-railway.js
```

This will generate:
- JWT secret
- Firebase configuration
- Validation checklist

### 2. Sign Up for Railway

1. Go to: https://railway.app
2. Sign in with GitHub
3. Authorize Railway

### 3. Deploy

1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Choose your repository
4. Set root directory to `server` (if needed)

### 4. Add Environment Variables

Go to **Variables** tab and add:

**Copy from `prepare-railway.js` output:**
```env
NODE_ENV=production
MONGODB_URI=your_mongodb_connection_string
SENDGRID_API_KEY=your_sendgrid_key
SENDGRID_FROM_EMAIL=your_verified_email
JWT_SECRET=generated_from_script
FIREBASE_PROJECT_ID=from_script_output
FIREBASE_PRIVATE_KEY=from_script_output
FIREBASE_CLIENT_EMAIL=from_script_output
```

### 5. MongoDB Atlas Setup

**CRITICAL:** Whitelist Railway IP:

1. Go to MongoDB Atlas → **Network Access**
2. Click **"Add IP Address"**
3. Enter: `0.0.0.0/0`
4. Click **"Confirm"**

### 6. Get Your URL

1. Go to **Settings** → **Networking**
2. Click **"Generate Domain"**
3. Copy your URL: `https://your-app.railway.app`

### 7. Test Deployment

```bash
# Health check
curl https://your-app.railway.app/health

# API check
curl https://your-app.railway.app/
```

## 🔥 Update Android App

In your Android app, update the API base URL:

```kotlin
const val BASE_URL = "https://your-app.railway.app/"
```

## ✅ Done!

Your backend is live on Railway! 🎉

For detailed instructions, see: **RAILWAY_SETUP_GUIDE.md**

## 🐛 Common Issues

**Issue:** MongoDB connection failed
**Fix:** Add `0.0.0.0/0` to MongoDB Network Access whitelist

**Issue:** Firebase auth errors
**Fix:** Check all three Firebase variables are set correctly

**Issue:** Build failed
**Fix:** Ensure `package.json` has `"start": "node server.js"`

## 📊 Monitor Your App

View logs:
1. Railway Dashboard → Your Project
2. Click **Deployments**
3. View real-time logs

## 💰 Costs

Railway Free Tier:
- $5 credit/month
- Perfect for development and small apps
- Upgrade to paid plan for production scale

---

**Need help?** Check the full guide in `RAILWAY_SETUP_GUIDE.md`
