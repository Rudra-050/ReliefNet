# 🚄 Railway Deployment Guide for ReliefNet Backend

Complete guide to deploy your ReliefNet backend to Railway for production use.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Configuration](#configuration)
4. [Deployment](#deployment)
5. [Environment Variables](#environment-variables)
6. [Testing](#testing)
7. [Monitoring](#monitoring)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Prerequisites

Before deploying to Railway, ensure you have:

- ✅ **GitHub Account** (for code repository)
- ✅ **Railway Account** (sign up at https://railway.app)
- ✅ **MongoDB Atlas** database ready
- ✅ **SendGrid API Key** configured
- ✅ **Firebase Service Account** JSON file
- ✅ All environment variables prepared

---

## 🚀 Initial Setup

### Step 1: Push Your Code to GitHub

If not already done:

```bash
cd server
git init
git add .
git commit -m "Initial commit for Railway deployment"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/reliefnet-backend.git
git push -u origin main
```

### Step 2: Create Railway Account

1. Go to https://railway.app
2. Click **"Start a New Project"**
3. Sign in with **GitHub**
4. Authorize Railway to access your repositories

### Step 3: Create New Project

1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Choose your **reliefnet-backend** repository
4. Railway will auto-detect it's a Node.js project

---

## ⚙️ Configuration

### 1. Create `railway.json` (Optional but Recommended)

Create this file in your `server` directory:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "npm install"
  },
  "deploy": {
    "startCommand": "node server.js",
    "healthcheckPath": "/",
    "healthcheckTimeout": 100,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

### 2. Update `.dockerignore`

Ensure your `.dockerignore` excludes unnecessary files:

```
node_modules
npm-debug.log
.env
.env.local
.git
.gitignore
*.md
.DS_Store
uploads/*
!uploads/.gitkeep
test-*.ps1
test*.js
serviceAccountKey.json.json
.vscode
```

### 3. Create `Procfile` (Alternative to railway.json)

Create `Procfile` in server directory:

```
web: node server.js
```

---

## 🔐 Environment Variables

### Set Environment Variables in Railway

1. Go to your Railway project
2. Click on your service
3. Go to **"Variables"** tab
4. Add these variables:

#### Required Variables:

```env
# Node Environment
NODE_ENV=production

# Server Port (Railway sets this automatically, but you can override)
PORT=8080

# MongoDB Atlas
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/reliefnet?retryWrites=true&w=majority

# SendGrid Email
SENDGRID_API_KEY=SG.your_sendgrid_api_key_here
SENDGRID_FROM_EMAIL=your-verified-sender@example.com
SENDGRID_REPLY_TO=your-verified-sender@example.com

# JWT Secret (generate a strong secret)
JWT_SECRET=your_super_secret_jwt_key_here_make_it_long_and_random

# Firebase Admin SDK (see below for service account)
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_PRIVATE_KEY=your-firebase-private-key
FIREBASE_CLIENT_EMAIL=your-firebase-client-email

# CORS Origins (your Android app and web frontend)
ALLOWED_ORIGINS=https://yourfrontend.com,capacitor://localhost,http://localhost

# Razorpay (if using payment)
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
```

### 🔥 Firebase Service Account Setup

**Option 1: Using Individual Environment Variables** (Recommended for Railway)

Instead of uploading the JSON file, extract values from `serviceAccountKey.json.json`:

```json
{
  "project_id": "YOUR_PROJECT_ID",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@YOUR_PROJECT_ID.iam.gserviceaccount.com"
}
```

Add to Railway variables:
- `FIREBASE_PROJECT_ID` = `YOUR_PROJECT_ID`
- `FIREBASE_PRIVATE_KEY` = The full private key (include `-----BEGIN PRIVATE KEY-----` and `-----END PRIVATE KEY-----`)
- `FIREBASE_CLIENT_EMAIL` = The service account email

**Option 2: Using Base64 Encoded JSON** (Alternative)

1. Encode your service account file:
```bash
# On Linux/Mac:
base64 serviceAccountKey.json.json

# On Windows PowerShell:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("serviceAccountKey.json.json"))
```

2. Add to Railway as:
```env
FIREBASE_SERVICE_ACCOUNT_BASE64=your_base64_encoded_json
```

---

## 📦 Update server.js for Production

### Modify Firebase Initialization

Update your `server.js` to handle both local and production environments:

```javascript
// Firebase Admin SDK initialization
let firebaseApp;
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
    // Production: Use base64 encoded service account
    const serviceAccount = JSON.parse(
      Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, 'base64').toString('utf-8')
    );
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  } else if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
    // Production: Use individual environment variables
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL
      })
    });
  } else {
    // Local development: Use service account file
    const serviceAccount = require('./serviceAccountKey.json');
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  }
  console.log('✅ Firebase Admin initialized');
} catch (error) {
  console.error('❌ Firebase initialization error:', error);
  process.exit(1);
}
```

### Update CORS Configuration

```javascript
const allowedOrigins = process.env.ALLOWED_ORIGINS 
  ? process.env.ALLOWED_ORIGINS.split(',')
  : ['http://localhost:3000', 'capacitor://localhost'];

app.use((req, res, next) => {
  const origin = req.headers.origin;
  if (allowedOrigins.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  
  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});
```

---

## 🚢 Deployment Steps

### Deploy from GitHub

1. **Connect Repository**
   - In Railway dashboard, click **"New Project"**
   - Select **"Deploy from GitHub repo"**
   - Choose your repository
   - Select the branch (usually `main`)

2. **Configure Root Directory**
   - If your server is in a subdirectory, set the root path:
   - Go to **Settings** → **Service Settings**
   - Set **Root Directory** to `server`

3. **Set Environment Variables**
   - Go to **Variables** tab
   - Add all environment variables listed above

4. **Deploy**
   - Railway automatically deploys on every push to main
   - Monitor deployment in the **Deployments** tab

### Manual Deployment (Railway CLI)

Install Railway CLI:

```bash
npm install -g @railway/cli
```

Deploy:

```bash
cd server
railway login
railway init
railway up
```

---

## 🌐 Get Your Production URL

1. Go to your Railway project
2. Click on your service
3. Go to **Settings** → **Networking**
4. Click **"Generate Domain"**
5. Railway will provide a URL like: `https://reliefnet-backend-production.up.railway.app`

### Custom Domain (Optional)

1. Go to **Settings** → **Networking**
2. Click **"Custom Domain"**
3. Add your domain (e.g., `api.reliefnet.com`)
4. Update DNS records as instructed

---

## 🧪 Testing Your Deployment

### 1. Test Basic Health Check

```bash
curl https://your-railway-url.up.railway.app/
```

Expected response: `ReliefNet API is running`

### 2. Test OTP Endpoint

```bash
curl -X POST https://your-railway-url.up.railway.app/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

### 3. Test From Android App

Update your Android app's base URL:

**In your Android project** (e.g., `ApiService.kt` or `Constants.kt`):

```kotlin
object ApiConfig {
    const val BASE_URL = "https://your-railway-url.up.railway.app/"
}
```

---

## 📊 Monitoring & Logs

### View Logs

1. Go to Railway dashboard
2. Click on your service
3. Click **"Deployments"**
4. Click on active deployment
5. View real-time logs

### Set Up Logging

Add structured logging to your app:

```javascript
// Add at the top of server.js
const logger = {
  info: (msg, data = {}) => console.log(JSON.stringify({ level: 'info', message: msg, ...data, timestamp: new Date().toISOString() })),
  error: (msg, error = {}) => console.error(JSON.stringify({ level: 'error', message: msg, error: error.message, stack: error.stack, timestamp: new Date().toISOString() })),
  warn: (msg, data = {}) => console.warn(JSON.stringify({ level: 'warn', message: msg, ...data, timestamp: new Date().toISOString() }))
};

// Use throughout your code:
logger.info('Server started', { port: PORT });
logger.error('Database connection failed', error);
```

---

## 🔧 Production Optimizations

### 1. Add Health Check Endpoint

```javascript
app.get('/health', (req, res) => {
  const health = {
    uptime: process.uptime(),
    status: 'OK',
    timestamp: Date.now(),
    mongodb: mongoose.connection.readyState === 1 ? 'Connected' : 'Disconnected',
    environment: process.env.NODE_ENV
  };
  res.json(health);
});
```

### 2. Add Graceful Shutdown

```javascript
// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down gracefully');
  server.close(() => {
    console.log('HTTP server closed');
    mongoose.connection.close(false, () => {
      console.log('MongoDB connection closed');
      process.exit(0);
    });
  });
});
```

### 3. Update Rate Limiting for Production

```javascript
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: process.env.NODE_ENV === 'production' ? 100 : 1000,
  message: 'Too many requests from this IP, please try again later.'
});
```

---

## 🐛 Troubleshooting

### Issue 1: "Application Failed to Start"

**Check:**
- Logs in Railway dashboard
- Environment variables are set correctly
- `package.json` has correct start script

**Solution:**
```json
{
  "scripts": {
    "start": "node server.js"
  }
}
```

### Issue 2: MongoDB Connection Failed

**Check:**
- MongoDB Atlas IP whitelist (add `0.0.0.0/0` for Railway)
- Connection string is correct
- Database user has correct permissions

### Issue 3: Firebase Auth Errors

**Check:**
- Private key includes `\n` newlines
- All three Firebase variables are set
- Service account has correct permissions

### Issue 4: Port Binding Error

**Railway automatically sets PORT**, ensure your code uses it:

```javascript
const PORT = process.env.PORT || 5000;
```

### Issue 5: File Upload Issues

**Railway uses ephemeral storage**. For persistent uploads:

**Option 1:** Use Firebase Storage or AWS S3
**Option 2:** Configure Railway volume (Settings → Volumes)

---

## 💰 Pricing & Resource Management

### Free Tier Limits:
- **$5 credit/month** (free for hobby projects)
- **500 hours** of usage
- **1GB** memory per service

### Monitor Usage:
1. Dashboard → **Usage**
2. Track monthly credits
3. Set up billing alerts

### Optimize Costs:
- Use environment-based configs
- Implement caching
- Optimize database queries

---

## 🔄 CI/CD Pipeline

### Automatic Deployments

Railway automatically deploys when you push to GitHub:

1. **Push to GitHub:**
```bash
git add .
git commit -m "Update feature"
git push origin main
```

2. **Railway automatically:**
   - Detects the push
   - Builds the application
   - Runs tests (if configured)
   - Deploys to production

### Rollback

If a deployment fails:
1. Go to **Deployments**
2. Click on a previous successful deployment
3. Click **"Redeploy"**

---

## ✅ Final Checklist

Before going live:

- [ ] All environment variables configured
- [ ] MongoDB Atlas IP whitelist includes `0.0.0.0/0`
- [ ] SendGrid sender email verified
- [ ] Firebase service account configured
- [ ] Custom domain configured (optional)
- [ ] Health check endpoint working
- [ ] Android app updated with production URL
- [ ] CORS origins include production domains
- [ ] Rate limiting configured
- [ ] Logging implemented
- [ ] Error handling tested
- [ ] Database backups configured
- [ ] Monitoring alerts set up

---

## 📱 Update Android App

After deployment, update your Android app:

**1. Update API Base URL:**

In `ApiService.kt` or equivalent:

```kotlin
object ApiConstants {
    private const val IS_PRODUCTION = true
    
    const val BASE_URL = if (IS_PRODUCTION) {
        "https://your-railway-url.up.railway.app/"
    } else {
        "http://10.0.2.2:5000/" // Local development
    }
}
```

**2. Update Network Security Config (if needed):**

In `android/app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

**3. Rebuild Release APK:**

```bash
cd Reliefnet-android
./gradlew assembleRelease
```

---

## 🎉 Success!

Your ReliefNet backend is now deployed on Railway!

**Your API is available at:**
`https://your-railway-url.up.railway.app`

### Next Steps:

1. Test all endpoints from your Android app
2. Monitor logs for any errors
3. Set up monitoring and alerts
4. Configure database backups
5. Implement analytics

---

## 📞 Support & Resources

- **Railway Docs:** https://docs.railway.app
- **Railway Discord:** https://discord.gg/railway
- **MongoDB Atlas:** https://www.mongodb.com/docs/atlas
- **SendGrid Docs:** https://docs.sendgrid.com

---

**Created for ReliefNet Project**
Last Updated: October 26, 2025
