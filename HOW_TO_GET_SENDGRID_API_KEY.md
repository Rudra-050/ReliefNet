# How to Get SendGrid API Key - Step by Step

## 🚀 **Complete Guide (5 Minutes)**

---

## **Step 1: Sign Up for SendGrid**

### **Go to SendGrid website:**
🔗 **https://signup.sendgrid.com/**

### **Fill in the form:**
```
First Name: Your first name
Last Name: Your last name
Email: your-email@gmail.com (any email you use)
Password: Create a strong password (8+ characters)
```

### **Company Information:**
```
Company Name: ReliefNet (or your company name)
Company Website: reliefnet.com (or leave blank if you don't have one)
```

### **Additional Information:**
```
Role: Developer
Company Size: 1-100
Country: India (or your country)
Do you send email: Yes
```

### **Click "Create Account"**

✅ **You'll get a verification email - click the link to verify!**

---

## **Step 2: Complete Setup Wizard**

After signing in, SendGrid shows a setup wizard:

### **Choose Integration:**
- Select: **"Web API"** (not SMTP)
- Click "Next"

### **Choose Language:**
- Select: **"Node.js"**
- Click "Next"

---

## **Step 3: Create API Key**

### **Option A: During Setup Wizard**

The wizard will show you:
```javascript
// Example code with API key
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey('SG.XXXXXX.YYYYYY');
```

**Copy the API key** (starts with `SG.`)

### **Option B: From Settings (Manual)**

If you skipped the wizard:

1. **Go to Settings** (left sidebar)
2. Click **"API Keys"**
   - URL: https://app.sendgrid.com/settings/api_keys
3. Click **"Create API Key"** (blue button)
4. **Enter details:**
   ```
   API Key Name: ReliefNet OTP System
   ```
5. **Choose permissions:**
   - Select: **"Full Access"** (easiest)
   - Or: **"Restricted Access"** → Check only "Mail Send" → Full Access
6. Click **"Create & View"**
7. **📋 COPY THE API KEY!**
   - Looks like: `SG.abcd1234efgh5678.ijkl9012mnop3456`
   - ⚠️ **You can only see this ONCE!**
   - Save it somewhere safe!

---

## **Step 4: Verify Sender Identity (REQUIRED!)**

⚠️ **SendGrid won't send emails without a verified sender!**

### **Go to Sender Authentication:**
🔗 https://app.sendgrid.com/settings/sender_auth/senders

### **Click "Create New Sender"**

### **Fill in the form:**
```
From Name: ReliefNet
From Email Address: your-email@gmail.com (your actual email)
Reply To: your-email@gmail.com (same email)
Company Address: Your address (123 Main St)
Company City: Your city (Mumbai)
State/Province: Your state (Maharashtra)
Zip/Postal Code: Your zip (400001)
Country: India (or your country)
Nickname: ReliefNet OTP Sender
```

### **Click "Create"**

### **Check your email inbox!**
- You'll receive: "Please verify your sender identity"
- **Click the verification link**
- ✅ **Sender verified!**

---

## **Step 5: Copy Your Information**

Now you have:

### **API Key:**
```
SG.abcd1234efgh5678.ijkl9012mnop3456
```
(Your actual key will be longer)

### **Verified Sender Email:**
```
your-email@gmail.com
```
(The email you just verified)

---

## **Step 6: Tell Me Your Details**

Once you have:
1. ✅ SendGrid API Key (starts with `SG.`)
2. ✅ Verified sender email

**Tell me:**
```
API Key: SG.your-key-here
Sender Email: your-email@gmail.com
```

**And I'll update server.js for you in 30 seconds!** 🚀

---

## 📸 **Visual Guide:**

### **What You'll See:**

1. **Sign Up Page:**
```
┌─────────────────────────────────┐
│  Create your free account       │
│                                  │
│  Email: ___________________     │
│  Password: ________________     │
│                                  │
│  [Create Account]               │
└─────────────────────────────────┘
```

2. **Dashboard After Login:**
```
┌─────────────────────────────────┐
│  SendGrid Dashboard             │
│                                  │
│  ← Settings                     │
│    - API Keys                   │
│    - Sender Authentication      │
│                                  │
└─────────────────────────────────┘
```

3. **API Key Creation:**
```
┌─────────────────────────────────┐
│  Create API Key                 │
│                                  │
│  Name: ReliefNet OTP System     │
│  Permissions: ● Full Access     │
│                                  │
│  [Create & View]                │
└─────────────────────────────────┘
```

4. **API Key Display (COPY THIS!):**
```
┌─────────────────────────────────┐
│  Your API Key:                  │
│                                  │
│  SG.abcd1234.efgh5678           │
│  [Copy]                         │
│                                  │
│  ⚠️ This will only show once!   │
└─────────────────────────────────┘
```

5. **Sender Verification Email:**
```
┌─────────────────────────────────┐
│  From: SendGrid                 │
│  Subject: Verify your sender    │
│                                  │
│  Click here to verify:          │
│  [Verify Sender Identity]       │
└─────────────────────────────────┘
```

---

## ⚠️ **Important Reminders:**

### **API Key:**
- ✅ Shows only ONCE - copy immediately!
- ✅ Starts with `SG.`
- ✅ Keep it private (like a password)
- ✅ If you lose it, create a new one

### **Sender Verification:**
- ⚠️ **MUST verify before sending emails!**
- ⚠️ Check spam folder if email doesn't arrive
- ⚠️ Use the SAME email in server.js

### **Free Tier:**
- ✅ 100 emails per day (forever free)
- ✅ No credit card required
- ✅ Perfect for testing

---

## 🐛 **Troubleshooting:**

### **"I don't see Create API Key button"**
- Check you're logged in
- Go directly to: https://app.sendgrid.com/settings/api_keys
- Look for blue "Create API Key" button

### **"I closed the API key popup!"**
- ❌ Can't see it again
- ✅ Create a new API key (you can have multiple)

### **"Verification email not received"**
- Check spam/junk folder
- Wait 5 minutes
- Click "Resend Verification Email"

### **"Can I use free tier?"**
- ✅ Yes! 100 emails/day
- ✅ No credit card needed
- ✅ No expiration

---

## 📝 **Quick Checklist:**

- [ ] Signed up at SendGrid
- [ ] Verified email address
- [ ] Logged into dashboard
- [ ] Created API Key
- [ ] **COPIED API KEY** (starts with `SG.`)
- [ ] Created Sender Identity
- [ ] Verified sender email (clicked link in email)
- [ ] Ready to give API key + sender email

---

## 🎯 **What Happens Next:**

Once you give me your:
1. API Key (`SG.xxxxx`)
2. Verified sender email

**I will:**
1. Update `server.js` line 18 (API key)
2. Update `server.js` line 31 (sender email)
3. You restart server
4. ✅ **OTP emails start working!**

---

## 🔗 **Quick Links:**

- **Sign Up:** https://signup.sendgrid.com/
- **Login:** https://app.sendgrid.com/login
- **API Keys:** https://app.sendgrid.com/settings/api_keys
- **Sender Auth:** https://app.sendgrid.com/settings/sender_auth/senders
- **Help Center:** https://docs.sendgrid.com/

---

## 💬 **Need Help?**

If you get stuck at any step:
1. Tell me which step number
2. Send a screenshot if possible
3. Copy any error messages

**I'm here to help!** 🚀

---

**Ready?** Go to: https://signup.sendgrid.com/ and start! 🎉
