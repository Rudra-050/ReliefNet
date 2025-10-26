# ✅ Railway Deployment Checklist

Complete this checklist before and after deploying to Railway.

## 📋 Pre-Deployment Checklist

### 1. Code Preparation

- [ ] All code committed to Git
- [ ] `.gitignore` excludes sensitive files
- [ ] `package.json` has `"start": "node server.js"`
- [ ] No hardcoded credentials in code
- [ ] CORS configured for production domains

### 2. Configuration Files

- [ ] `railway.json` exists in server directory
- [ ] `Procfile` exists in server directory
- [ ] `.env.example` updated with all variables
- [ ] `.dockerignore` excludes unnecessary files

### 3. Database Setup

- [ ] MongoDB Atlas cluster created
- [ ] Database user created with password
- [ ] Connection string copied
- [ ] **CRITICAL:** Network Access whitelist includes `0.0.0.0/0`
- [ ] Database name decided (e.g., `reliefnet`)

### 4. SendGrid Email Setup

- [ ] SendGrid account created
- [ ] API Key generated (starts with `SG.`)
- [ ] Sender email verified
- [ ] Sender email confirmed working
- [ ] API key copied and secured

### 5. Firebase Setup

- [ ] Firebase project created
- [ ] Service account JSON downloaded
- [ ] Firebase Admin SDK enabled
- [ ] Run `node prepare-railway.js` to extract credentials
- [ ] Firebase variables ready to copy

### 6. Payment Gateway (If Using)

- [ ] Razorpay account created
- [ ] Test API keys obtained
- [ ] Production API keys obtained
- [ ] Webhook setup prepared

### 7. Security

- [ ] JWT secret generated (run `prepare-railway.js`)
- [ ] Strong passwords used for all services
- [ ] No credentials in Git history
- [ ] Rate limiting configured
- [ ] Helmet security middleware enabled

---

## 🚀 Deployment Steps

### 1. Railway Account Setup

- [ ] Railway account created at https://railway.app
- [ ] GitHub account connected
- [ ] Railway authorized to access repositories

### 2. Project Creation

- [ ] New project created in Railway
- [ ] Repository connected
- [ ] Correct branch selected (usually `main`)
- [ ] Root directory set to `server` (if needed)

### 3. Environment Variables

Copy all variables from `prepare-railway.js` output:

- [ ] `NODE_ENV=production`
- [ ] `PORT=8080` (or let Railway auto-set)
- [ ] `MONGODB_URI` (full connection string)
- [ ] `SENDGRID_API_KEY`
- [ ] `SENDGRID_FROM_EMAIL`
- [ ] `SENDGRID_REPLY_TO`
- [ ] `JWT_SECRET`
- [ ] `FIREBASE_PROJECT_ID`
- [ ] `FIREBASE_PRIVATE_KEY`
- [ ] `FIREBASE_CLIENT_EMAIL`
- [ ] `ALLOWED_ORIGINS` (production domains)
- [ ] `RAZORPAY_KEY_ID` (if using payments)
- [ ] `RAZORPAY_KEY_SECRET` (if using payments)

### 4. First Deployment

- [ ] Deployment initiated (automatic or manual)
- [ ] Build logs checked for errors
- [ ] Deployment completed successfully
- [ ] No errors in deployment logs

### 5. Domain Configuration

- [ ] Railway domain generated
- [ ] Custom domain added (optional)
- [ ] DNS records updated (if custom domain)
- [ ] SSL certificate issued (automatic)

---

## 🧪 Post-Deployment Testing

### 1. Basic Health Checks

Test these endpoints:

- [ ] `GET /` → Returns "ReliefNet API is running"
- [ ] `GET /health` → Returns health status JSON
- [ ] Response time < 2 seconds
- [ ] HTTPS working (no certificate errors)

### 2. Authentication Endpoints

- [ ] `POST /auth/send-otp` → Sends OTP email
- [ ] `POST /auth/verify-otp` → Verifies OTP
- [ ] `POST /auth/google-login` → Google OAuth works
- [ ] JWT token generated successfully
- [ ] Email delivery confirmed in inbox

### 3. Protected Routes

- [ ] Doctor routes require authentication
- [ ] Patient routes require authentication
- [ ] Unauthorized requests return 401
- [ ] Invalid tokens rejected

### 4. Real-time Features

- [ ] Socket.io connection established
- [ ] Chat messages sent/received
- [ ] Typing indicators work
- [ ] Online status updates
- [ ] Notifications delivered

### 5. File Uploads

- [ ] Profile photo upload works
- [ ] File size limits enforced
- [ ] File types validated
- [ ] Images accessible via URL

### 6. Database Operations

- [ ] Users can register
- [ ] Users can login
- [ ] Data persists after restart
- [ ] Queries respond quickly (< 500ms)
- [ ] No connection errors in logs

### 7. Email Delivery

- [ ] OTP emails arrive within 1 minute
- [ ] Emails not in spam folder
- [ ] Email formatting correct
- [ ] All email links work

---

## 📱 Android App Integration

### 1. Update API Configuration

In your Android app (e.g., `ApiService.kt`):

```kotlin
object ApiConfig {
    const val BASE_URL = "https://your-railway-url.railway.app/"
}
```

- [ ] Base URL updated
- [ ] HTTPS used (not HTTP)
- [ ] Trailing slash included

### 2. Network Security

- [ ] `network_security_config.xml` allows HTTPS
- [ ] Certificate validation enabled
- [ ] No cleartext traffic to production

### 3. Test From Android

- [ ] Login works from app
- [ ] OTP received and verified
- [ ] Google login works
- [ ] Chat messages send/receive
- [ ] Notifications arrive
- [ ] Profile updates work
- [ ] Doctor booking works
- [ ] Payments process (if implemented)

---

## 📊 Monitoring & Maintenance

### 1. Set Up Monitoring

- [ ] Railway dashboard bookmarked
- [ ] Email alerts configured
- [ ] Deployment notifications enabled
- [ ] Error tracking setup (optional: Sentry)

### 2. View Logs Regularly

- [ ] Check logs daily (first week)
- [ ] Monitor error rates
- [ ] Watch memory usage
- [ ] Track response times

### 3. Database Maintenance

- [ ] MongoDB Atlas monitoring enabled
- [ ] Backup schedule configured
- [ ] Index optimization checked
- [ ] Connection pool sized appropriately

### 4. Cost Management

- [ ] Railway usage tracked
- [ ] Billing alerts set up
- [ ] Resource limits configured
- [ ] Optimization opportunities identified

---

## 🔒 Security Hardening

### 1. Production Security

- [ ] All API keys rotated from test keys
- [ ] Environment variables double-checked
- [ ] No default passwords in use
- [ ] Rate limiting active
- [ ] Helmet middleware configured

### 2. CORS Configuration

- [ ] Only production domains allowed
- [ ] Wildcard origins removed
- [ ] Credentials properly handled
- [ ] Preflight requests working

### 3. Authentication

- [ ] JWT expiry set appropriately
- [ ] Refresh tokens implemented (if needed)
- [ ] Password hashing verified
- [ ] Session management secure

---

## 📄 Documentation

### 1. Update Documentation

- [ ] Production URL documented
- [ ] Environment variables listed
- [ ] API endpoints documented
- [ ] Deployment process documented

### 2. Team Knowledge

- [ ] Team knows how to view logs
- [ ] Rollback process documented
- [ ] Emergency contacts listed
- [ ] On-call rotation set (if applicable)

---

## 🎉 Final Verification

### Before Announcing Launch

- [ ] All checklist items completed
- [ ] All tests passing
- [ ] No critical errors in logs
- [ ] Performance acceptable
- [ ] Backup and recovery tested
- [ ] Monitoring alerts working
- [ ] Documentation up to date
- [ ] Team briefed on new deployment

### Launch Criteria

- [ ] **Uptime:** 99%+ for 24 hours
- [ ] **Response Time:** < 2s for API calls
- [ ] **Error Rate:** < 1% of requests
- [ ] **Email Delivery:** > 95% success rate
- [ ] **Database:** No connection errors
- [ ] **Websockets:** Stable connections

---

## 🆘 Emergency Contacts

**Railway Support:**
- Discord: https://discord.gg/railway
- Email: team@railway.app

**MongoDB Atlas Support:**
- Support Portal: https://support.mongodb.com

**SendGrid Support:**
- Support Portal: https://support.sendgrid.com

---

## 📝 Notes Section

Use this space to record deployment-specific information:

**Deployment Date:** _______________

**Railway Project URL:** _______________

**Production API URL:** _______________

**MongoDB Cluster:** _______________

**SendGrid Account:** _______________

**Firebase Project:** _______________

**Custom Domain:** _______________

**Team Members with Access:**
- _______________
- _______________
- _______________

---

**Last Updated:** October 26, 2025
**Status:** Ready for Deployment ✅
