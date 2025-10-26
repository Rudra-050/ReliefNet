# Google Cloud Run Production Setup

Deploy your ReliefNet backend on Google Cloud Run with auto HTTPS and custom domains.

## What you'll get
- **Free tier**: 2M requests/month, 360k vCPU-seconds/month, 180k GiB-seconds/month
- Auto HTTPS on `*.run.app` subdomain
- Custom domain support with auto TLS
- Auto-scales to zero (no cost when idle)
- Global CDN and load balancing

---

## Prerequisites

1. Google account
2. Credit/debit card (required for GCP, but you won't be charged if you stay in free tier)
3. Docker installed locally (we'll create a Dockerfile)

---

## Step 1: Create Dockerfile

In your `server/` folder, create a `Dockerfile`:

```dockerfile
# Use official Node.js LTS runtime
FROM node:20-slim

# Create app directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (production only)
RUN npm ci --only=production

# Copy app source
COPY . .

# Expose port (Cloud Run sets PORT env var automatically)
EXPOSE 8080

# Start server
CMD ["node", "server.js"]
```

**Important**: Cloud Run sets `PORT` environment variable automatically. Your `server.js` already uses `process.env.PORT || 5000`, so this will work.

---

## Step 2: Create .dockerignore

In `server/` folder, create `.dockerignore`:

```
node_modules
npm-debug.log
.env
.git
.gitignore
*.md
uploads/*
test*.js
test*.ps1
serviceAccountKey.json
```

---

## Step 3: Test Docker locally (optional but recommended)

From `server/` folder:

```powershell
# Build image
docker build -t reliefnet-server .

# Test locally (map port 8080)
docker run -p 8080:8080 -e PORT=8080 reliefnet-server

# Test in another terminal
curl http://localhost:8080/health
```

Stop with `Ctrl+C` when done testing.

---

## Step 4: Set up Google Cloud

### 4.1 Create GCP project

1. Go to https://console.cloud.google.com
2. Create new project: **ReliefNet** (or any name)
3. Note your **Project ID** (e.g., `reliefnet-123456`)

### 4.2 Enable required APIs

In Cloud Console:
- Search for "Cloud Run API" → Enable
- Search for "Artifact Registry API" → Enable (for storing Docker images)
- Search for "Cloud Build API" → Enable (optional, for auto-builds)

### 4.3 Install gcloud CLI

Download from: https://cloud.google.com/sdk/docs/install

After install, run:

```powershell
# Initialize gcloud
gcloud init

# Login
gcloud auth login

# Set project
gcloud config set project YOUR_PROJECT_ID
```

---

## Step 5: Deploy to Cloud Run

### Option A: Deploy from local Docker image (easiest)

From `server/` folder:

```powershell
# Deploy directly (gcloud builds the image and deploys)
gcloud run deploy reliefnet-api `
  --source . `
  --region us-central1 `
  --platform managed `
  --allow-unauthenticated `
  --set-env-vars "NODE_ENV=production"

# You'll be prompted:
# - Service name: reliefnet-api
# - Region: us-central1 (or choose closest to your users)
# - Allow unauthenticated: Yes
```

This command:
- Builds the Docker image from current folder
- Pushes it to Artifact Registry
- Deploys to Cloud Run
- Gives you a public HTTPS URL like `https://reliefnet-api-xxxxx-uc.a.run.app`

### Option B: Build and push manually (more control)

```powershell
# Set variables
$PROJECT_ID = "YOUR_PROJECT_ID"
$REGION = "us-central1"

# Build and push to Artifact Registry
gcloud builds submit --tag gcr.io/$PROJECT_ID/reliefnet-api

# Deploy
gcloud run deploy reliefnet-api `
  --image gcr.io/$PROJECT_ID/reliefnet-api `
  --platform managed `
  --region $REGION `
  --allow-unauthenticated
```

---

## Step 6: Set environment variables

In Cloud Console → Cloud Run → reliefnet-api → Edit & Deploy New Revision:

**Required**:
- `NODE_ENV` = `production`
- `APP_BASE_URL` = `https://reliefnet-api-xxxxx-uc.a.run.app` (your Cloud Run URL)
- `JWT_SECRET` = (long random string)
- `MONGODB_URI` = (your Atlas connection string)

**Email (if using)**:
- `SENDGRID_API_KEY`
- `SENDGRID_FROM_EMAIL`
- `SENDGRID_REPLY_TO`

**PhonePe (if payments)**:
- `PHONEPE_ENV` = `production` or `sandbox`
- `PHONEPE_MERCHANT_ID`
- `PHONEPE_SALT_KEY`
- `PHONEPE_SALT_INDEX` = `1`

**Firebase (for serviceAccountKey.json)**:

You have 2 options:

**Option 1**: Use Google Cloud's built-in service account (easiest)
- Cloud Run automatically provides credentials
- No need to upload serviceAccountKey.json
- Update `server.js` to use application default credentials

**Option 2**: Store as env var
- Copy your `serviceAccountKey.json` content
- Add env var `FIREBASE_SERVICE_ACCOUNT` = (paste full JSON)
- Update `server.js` to load from env var

Click **Deploy** after adding env vars.

---

## Step 7: Verify deployment

```powershell
# Get service URL
gcloud run services describe reliefnet-api --region us-central1 --format="value(status.url)"

# Test health endpoint
curl https://reliefnet-api-xxxxx-uc.a.run.app/health
```

Expected response:
```json
{"status":"ok","db":"connected","time":"2025-10-26T..."}
```

---

## Step 8: Add custom domain (optional)

### 8.1 In Cloud Run Console:

1. Cloud Run → reliefnet-api → **Manage Custom Domains**
2. Click **Add Mapping**
3. Select your domain or add a new verified domain
4. Choose subdomain: `api.yourdomain.com`
5. Cloud Run shows DNS records to add

### 8.2 In your DNS provider:

Add the records exactly as shown. Usually:
- **Type**: `CNAME`
- **Name**: `api`
- **Value**: `ghs.googlehosted.com`

Or for root domain:
- **Type**: `A`
- **Values**: (IP addresses provided by Cloud Run)

### 8.3 Wait for verification

- DNS propagation: 5-30 minutes
- TLS certificate issuance: automatic after DNS is correct
- Cloud Run will show "Active" when ready

### 8.4 Update APP_BASE_URL

After custom domain is active, update env var:
- `APP_BASE_URL` = `https://api.yourdomain.com`

---

## Step 9: Update Android release BASE_URL

In `Reliefnet-android/app/build.gradle.kts`, update release build type:

```kotlin
release {
    buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com/\"")
    // Or use Cloud Run default URL:
    // buildConfigField("String", "BASE_URL", "\"https://reliefnet-api-xxxxx-uc.a.run.app/\"")
    
    isMinifyEnabled = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

Build release APK:

```powershell
cd ..\Reliefnet-android
.\gradlew :app:assembleRelease
```

---

## Step 10: Configure Firebase service account (if needed)

If you're using Option 2 (env var for Firebase):

Update `server/server.js`:

```javascript
// Replace this:
const serviceAccount = require('./serviceAccountKey.json');

// With this:
let serviceAccount;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
} else {
  serviceAccount = require('./serviceAccountKey.json');
}

// Initialize Firebase Admin (rest stays the same)
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}
```

---

## Cloud Run-specific optimizations

### Allow MongoDB Atlas connections

In MongoDB Atlas:
- Network Access → Add IP Address
- Add `0.0.0.0/0` (allow all) OR use Google Cloud's IP ranges
- Cloud Run uses dynamic IPs, so you need to allow broad range

### Increase instance resources (if needed)

If you hit free tier limits or need more power:

```powershell
gcloud run services update reliefnet-api `
  --region us-central1 `
  --memory 512Mi `
  --cpu 1 `
  --min-instances 0 `
  --max-instances 10
```

Free tier includes:
- 512Mi memory (default is 256Mi)
- 1 CPU
- 0 min instances (scales to zero when idle = no cost)

### View logs

```powershell
# Stream logs
gcloud run services logs tail reliefnet-api --region us-central1

# Or view in Cloud Console:
# Cloud Run → reliefnet-api → Logs
```

---

## Cost estimate (after free tier)

If you exceed free tier:
- **Requests**: $0.40 per million requests
- **CPU**: $0.00002400 per vCPU-second
- **Memory**: $0.00000250 per GiB-second
- **Networking**: $0.12 per GB egress

**Example**: 10M requests/month, avg 200ms response:
- Requests: ~$4
- CPU/Memory: ~$2-3
- **Total**: ~$6-7/month

Most small apps stay in free tier.

---

## Deployment checklist

- [ ] Dockerfile created in `server/`
- [ ] `.dockerignore` created
- [ ] Docker tested locally (optional)
- [ ] GCP project created and billing enabled
- [ ] APIs enabled (Cloud Run, Artifact Registry)
- [ ] gcloud CLI installed and authenticated
- [ ] Service deployed to Cloud Run
- [ ] Environment variables set (NODE_ENV, MONGODB_URI, JWT_SECRET, etc.)
- [ ] `/health` endpoint returns OK
- [ ] MongoDB Atlas allows Cloud Run IPs
- [ ] Custom domain added and DNS configured (optional)
- [ ] Android release BASE_URL updated
- [ ] Release APK tested on device

---

## Continuous deployment (optional)

Set up auto-deploy from GitHub:

1. Cloud Console → Cloud Build → Triggers
2. Connect repository
3. Create trigger:
   - Branch: `main`
   - Build config: Cloud Build configuration file
   - Location: `server/cloudbuild.yaml`

Create `server/cloudbuild.yaml`:

```yaml
steps:
  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/reliefnet-api', '.']
  
  # Push to Artifact Registry
  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/reliefnet-api']
  
  # Deploy to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args:
      - 'run'
      - 'deploy'
      - 'reliefnet-api'
      - '--image'
      - 'gcr.io/$PROJECT_ID/reliefnet-api'
      - '--region'
      - 'us-central1'
      - '--platform'
      - 'managed'

images:
  - 'gcr.io/$PROJECT_ID/reliefnet-api'
```

Now every push to `main` auto-deploys to Cloud Run.

---

## Troubleshooting

### "Permission denied" error
```powershell
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

### "Service account key not found"
Use env var approach (Step 10) or enable Application Default Credentials.

### "Cannot connect to MongoDB"
Add `0.0.0.0/0` to Atlas Network Access whitelist.

### Cold starts slow (1-2s delay)
This is normal for Cloud Run when scaling from zero. To eliminate:
```powershell
gcloud run services update reliefnet-api --min-instances 1 --region us-central1
```
**Note**: Keeping 1 instance always warm costs ~$10-15/month.

---

## Summary

You now have:
- ✅ Backend deployed on Cloud Run with auto HTTPS
- ✅ Free tier (2M requests/month)
- ✅ Auto-scaling (zero cost when idle)
- ✅ Custom domain support
- ✅ Global CDN
- ✅ Container-based (portable and reproducible)

Next steps:
1. Deploy using the commands above
2. Test `/health` endpoint
3. Update Android release BASE_URL
4. Build and test release APK
