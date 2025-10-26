# SendGrid Setup Guide for ReliefNet OTP

## ✅ SendGrid Installed Successfully!

SendGrid is now configured in your server. Just need to get your API key!

---

## 🚀 **Quick Setup (5 Minutes)**

### **Step 1: Sign Up for SendGrid**

1. **Go to:** https://signup.sendgrid.com/
2. **Fill in details:**
   - Email: Your email address
   - Password: Create a strong password
   - Company: ReliefNet (or your name)
   - Website: reliefnet.com (or leave blank)
3. **Click "Create Account"**
4. **Verify your email** (check inbox/spam)

---

### **Step 2: Verify Single Sender (Required!)**

SendGrid requires a verified sender email before sending:

1. **After login, go to:** https://app.sendgrid.com/settings/sender_auth/senders
2. **Click "Create New Sender"**
3. **Fill in the form:**
   ```
   From Name: ReliefNet
   From Email Address: noreply@gmail.com (use your Gmail)
   Reply To: your-email@gmail.com
   Company Address: Your address
   City: Your city
   State: Your state
   Zip Code: Your zip
   Country: India (or your country)
   Nickname: ReliefNet OTP
   ```
4. **Click "Create"**
5. **Check your email inbox** for verification link
6. **Click the verification link**
7. **✅ Sender verified!**

---

### **Step 3: Get API Key**

1. **Go to:** https://app.sendgrid.com/settings/api_keys
2. **Click "Create API Key"**
3. **Settings:**
   - Name: `ReliefNet OTP System`
   - API Key Permissions: **Full Access** (recommended)
   - Or select: **Restricted Access** → Mail Send → Full Access
4. **Click "Create & View"**
5. **📋 COPY THE API KEY!** (Starts with `SG.`)
   - ⚠️ **IMPORTANT:** You can only see this ONCE!
   - Example: `SG.abcdefg1234567890.xyz`
6. **Save it somewhere safe!**

---

### **Step 4: Update server.js**

Now we need to add your API key to the code.

**Find this line in `server.js` (around line 18):**
```javascript
sgMail.setApiKey(process.env.SENDGRID_API_KEY || 'YOUR_SENDGRID_API_KEY');
```

**Replace with your actual API key:**
```javascript
sgMail.setApiKey('SG.your-actual-api-key-here');
```

**And this line (around line 31):**
```javascript
from: process.env.SENDGRID_FROM_EMAIL || 'noreply@reliefnet.com',
```

**Replace with your verified sender email:**
```javascript
from: 'noreply@gmail.com',  // The email you verified in Step 2
```

---

### **Step 5: Test It!**

1. **Start the server:**
```powershell
cd "C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\server"
node server.js
```

2. **Open ReliefNet app**
3. **Select Patient → OTP**
4. **Enter your email address**
5. **Click "Send OTP"**
6. **✅ Check your inbox!** (Also check spam folder)

---

## 📧 **Expected Email Format:**

You'll receive an email that looks like:

```
Subject: Your ReliefNet Login Code

🏥 ReliefNet
Your Mental Health Companion

Your Login Code
━━━━━━━━━━━━━━━━
   123456
━━━━━━━━━━━━━━━━

⚠️ Important:
• This code expires in 5 minutes
• Do not share this code with anyone
• ReliefNet staff will never ask for your OTP
```

---

## 🔧 **What I Changed in server.js:**

1. ❌ Removed: `nodemailer` and Gmail configuration
2. ✅ Added: `@sendgrid/mail` 
3. ✅ Updated: `sendOTPEmail()` function to use SendGrid
4. ✅ Added: Better error logging for debugging

---

## ⚠️ **Important Notes:**

### **Free Tier Limits:**
- ✅ 100 emails per day (forever free)
- ✅ More than enough for testing
- ✅ Can upgrade later for unlimited

### **Sender Verification:**
- ⚠️ **MUST verify sender email** before sending
- ⚠️ Emails will FAIL if sender not verified
- ✅ Only takes 2 minutes to verify

### **API Key Security:**
- ⚠️ **Never commit API key to GitHub!**
- ✅ Use environment variables for production
- ✅ Keep API key private

---

## 🐛 **Troubleshooting:**

### **Error: "The from email does not match a verified Sender Identity"**
- ❌ Sender email not verified
- ✅ Go to Step 2 and verify your sender email
- ✅ Make sure `from:` email matches verified email

### **Error: "Unauthorized"**
- ❌ Invalid API key
- ✅ Check if you copied the full API key (starts with `SG.`)
- ✅ Make sure no extra spaces
- ✅ Create a new API key if needed

### **Email goes to spam:**
- ✅ Normal for first few emails
- ✅ Ask recipient to mark as "Not Spam"
- ✅ Future emails will go to inbox

### **No email received:**
- ✅ Check spam/junk folder
- ✅ Check server console for errors
- ✅ Verify sender email is verified in SendGrid
- ✅ Check SendGrid dashboard → Activity for delivery status

---

## 📊 **Monitor Emails:**

**SendGrid Dashboard:** https://app.sendgrid.com/

- See all sent emails
- Delivery status
- Open rates
- Click rates
- Error logs

---

## 🎯 **Next Steps:**

1. ✅ Sign up for SendGrid
2. ✅ Verify sender email
3. ✅ Get API key
4. ✅ Update `server.js` with API key and sender email
5. ✅ Restart server
6. ✅ Test with real email!

---

## 💬 **Need Help?**

If you get stuck, let me know:
1. Which step you're on
2. Any error messages
3. Screenshot of the issue

**SendGrid Support:** https://support.sendgrid.com/

---

## ✅ **Checklist:**

- [ ] Signed up for SendGrid
- [ ] Verified email address
- [ ] Created Single Sender
- [ ] Verified sender email (check inbox!)
- [ ] Created API Key
- [ ] Copied API Key (starts with `SG.`)
- [ ] Updated `server.js` with API key (line 18)
- [ ] Updated `server.js` with sender email (line 31)
- [ ] Restarted server
- [ ] Tested OTP sending
- [ ] Received email successfully! 🎉

---

**Once you have your SendGrid API key, tell me and I'll help you update the code!** 🚀
