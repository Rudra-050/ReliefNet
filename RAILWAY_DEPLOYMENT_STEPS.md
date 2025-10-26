# Railway Deployment Guide

## Prerequisites ✅

- [x] Railway config files in `server/`:
  - `railway.json` (build & healthcheck)
  - `Procfile` (start command)
  - `.env.production.example` (env var template)
- [x] `/health` endpoint in `server.js`
- [x] GitHub repo pushed: https://github.com/Rudra-050/ReliefNet

---

## Option 1: Deploy from GitHub (Recommended)

### Step 1: Create Railway Project

1. Go to [Railway](https://railway.app)
2. Click **New Project** → **Deploy from GitHub repo**
3. Authorize GitHub and select **Rudra-050/ReliefNet**
4. Railway will detect your repo and ask for the service directory

### Step 2: Configure Service

1. **Root Directory:** `/server`
   - Railway will auto-detect `railway.json` and use Nixpacks
2. **Service Name:** `reliefnet-api` (or your choice)
3. Click **Deploy**

### Step 3: Set Environment Variables

Go to your Railway project → **Variables** tab and add:

```bash
NODE_ENV=production
MONGODB_URI=mongodb+srv://rudranshbhatt2005:tiger%401358@relief.sb1iuve.mongodb.net/reliefnet?retryWrites=true&w=majority
SENDGRID_API_KEY=<your_real_sendgrid_key>
SENDGRID_FROM_EMAIL=rudransh.bhatt120960@marwadiuniversity.ac.in
SENDGRID_REPLY_TO=rudransh.bhatt120960@marwadiuniversity.ac.in
JWT_SECRET=<generate_from_prepare-railway.js>
FIREBASE_PROJECT_ID=reliefnet-fc110
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-fbsvc@reliefnet-fc110.iam.gserviceaccount.com
FIREBASE_SERVICE_ACCOUNT_BASE64=<base64_from_prepare-railway.js>
ALLOWED_ORIGINS=capacitor://localhost,http://localhost,https://localhost
RAZORPAY_KEY_ID=<if_using_razorpay>
RAZORPAY_KEY_SECRET=<if_using_razorpay>
RATE_LIMIT_MAX=100
SESSION_TIMEOUT_HOURS=24
```

**Note:** Railway auto-sets `PORT`; no need to define it.

### Step 4: Generate Secrets (if not already done)

From your local `server/` directory:

```powershell
cd server
node prepare-railway.js
```

Copy the output values for `JWT_SECRET` and `FIREBASE_SERVICE_ACCOUNT_BASE64`.

### Step 5: Whitelist Railway IPs in MongoDB Atlas

1. Go to [MongoDB Atlas](https://cloud.mongodb.com/) → Network Access
2. Click **Add IP Address**
3. Choose **Allow Access from Anywhere** (`0.0.0.0/0`) or add specific Railway IP ranges
4. Save

### Step 6: Deploy & Monitor

1. Railway will auto-deploy on every push to `main` (if you keep GitHub integration enabled)
2. Check deployment logs in Railway dashboard
3. Once deployed, Railway provides a public URL (e.g., `https://reliefnet-api-production.up.railway.app`)

### Step 7: Test Health Endpoint

```powershell
curl https://your-railway-url.railway.app/health
```

Expected response:
```json
{
  "status": "ok",
  "db": "connected",
  "time": "2025-10-26T..."
}
```

---

## Option 2: Deploy via Railway CLI

### Install Railway CLI

```powershell
npm install -g @railway/cli
```

### Login

```powershell
railway login
```

### Initialize Project

```powershell
cd server
railway init
```

Follow prompts to create a new project or link an existing one.

### Set Variables

You can set variables via CLI:

```powershell
railway variables set MONGODB_URI="mongodb+srv://..."
railway variables set SENDGRID_API_KEY="SG...."
railway variables set JWT_SECRET="your_secret"
# ... etc for all vars in .env.production.example
```

Or bulk-upload from a local file (create `.env.railway` with production values):

```powershell
railway variables set --env-file .env.railway
```

### Deploy

```powershell
railway up
```

Railway will build and deploy your service. Monitor logs:

```powershell
railway logs
```

### Get Deployment URL

```powershell
railway domain
```

---

## Post-Deployment Checklist

- [ ] Health endpoint returns `{"status":"ok","db":"connected"}`
- [ ] Test an authenticated endpoint (e.g., `/api/auth/register`)
- [ ] Verify SendGrid emails are sent (test OTP login)
- [ ] Confirm Firebase auth works (test Google login)
- [ ] Check MongoDB Atlas → Metrics for new connections
- [ ] Update Android app `BASE_URL` to Railway production URL
- [ ] Test Socket.io real-time chat from Android app
- [ ] Set up custom domain (optional) in Railway → Settings → Domains

---

## GitHub Actions Auto-Deploy (Optional)

The repo includes `.github/workflows/deploy-railway.yml`. To enable:

1. Get Railway API Token: Railway → Account → Tokens → Create
2. Add to GitHub: Repo → Settings → Secrets → New secret
   - Name: `RAILWAY_TOKEN`
   - Value: `<your_railway_token>`
3. (Optional) Add `RAILWAY_SERVICE` secret with your service name if you have multiple services

Now every push to `main` that touches `server/` will auto-deploy via GitHub Actions.

---

## Troubleshooting

### Build Fails

- Check Railway logs for errors
- Ensure `package.json` has all dependencies (not devDependencies)
- Verify `railway.json` buildCommand is correct

### Health Check Fails

- Confirm `/health` endpoint exists in `server.js`
- Check `railway.json` healthcheckPath matches (`/health`)
- Review deployment logs for server startup errors

### Database Connection Issues

- Verify `MONGODB_URI` is correct and URL-encoded
- Check MongoDB Atlas Network Access whitelist includes Railway IPs
- Test connection string locally first

### SendGrid Emails Not Sending

- Verify `SENDGRID_API_KEY` is correct and has Mail Send permissions
- Check `SENDGRID_FROM_EMAIL` is verified in SendGrid dashboard
- Review Railway logs for SendGrid API errors

### Firebase Auth Fails

- Ensure `FIREBASE_SERVICE_ACCOUNT_BASE64` is correctly set
- Or use individual vars: `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`
- Verify Firebase project ID matches

---

## Monitoring & Scaling

- **Logs:** Railway dashboard → your service → Logs
- **Metrics:** Dashboard shows CPU, memory, network usage
- **Restart:** Click "Restart" in service settings if needed
- **Scaling:** Railway auto-scales based on usage; configure in Settings → Resources

---

## Security Best Practices

- Rotate `JWT_SECRET` periodically
- Use least-privilege MongoDB user (read/write to `reliefnet` DB only)
- Scope SendGrid API key to Mail Send only
- Enable Railway's built-in DDoS protection
- Consider adding rate limiting (already configured with `express-rate-limit`)
- Set `ALLOWED_ORIGINS` to only trusted domains in production

---

## Next Steps

1. Deploy using one of the methods above
2. Test all endpoints with `test-railway-deployment.ps1` (update URL in script)
3. Update Android app backend URL to Railway production
4. Set up a custom domain if needed
5. Configure monitoring/alerts in Railway

**Ready to deploy!** 🚀

If you encounter issues, check `RAILWAY_SETUP_GUIDE.md` for detailed configuration or run diagnostics with `test-railway-deployment.ps1`.
