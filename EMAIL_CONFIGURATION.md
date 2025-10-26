# Email Configuration for OTP System

## 🎯 Currently: OTP is NOT being sent to actual emails

Right now, OTPs are only:
- ✅ Displayed in terminal/console
- ✅ Shown in app toast (testing mode)
- ❌ NOT sent to real email addresses

## 📧 How to Enable Real Email Sending

### Option 1: Gmail (Easiest for Testing)

#### Step 1: Get Gmail App Password

1. **Go to your Google Account**: https://myaccount.google.com/
2. **Enable 2-Step Verification**:
   - Security → 2-Step Verification → Turn On
3. **Create App Password**:
   - Security → 2-Step Verification → App passwords
   - Select "Mail" and "Other (Custom name)"
   - Enter "ReliefNet" as the name
   - Click "Generate"
   - **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)

#### Step 2: Update server.js

Open `server/server.js` and find this section (around line 27):

```javascript
const emailTransporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER || 'your-email@gmail.com',  // ← Replace this
    pass: process.env.EMAIL_PASSWORD || 'your-app-password'  // ← Replace this
  }
});
```

**Replace with your credentials:**
```javascript
const emailTransporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-actual-email@gmail.com',      // Your Gmail address
    pass: 'abcd efgh ijkl mnop'                // Your 16-char App Password
  }
});
```

#### Step 3: Restart Server

```bash
cd server
node server.js
```

#### Step 4: Test!

1. Open app
2. Select Patient → OTP
3. Enter a **real email address** (yours or test email)
4. Click "Send OTP"
5. **Check your email inbox!** 📬

---

### Option 2: Environment Variables (Production Best Practice)

Create a `.env` file in the `server` folder:

```env
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
NODE_ENV=production
```

Install dotenv:
```bash
npm install dotenv
```

Add to top of `server.js`:
```javascript
require('dotenv').config();
```

Now credentials are loaded from `.env` file!

---

### Option 3: SendGrid (Professional Email Service)

#### Why SendGrid?
- ✅ Better deliverability
- ✅ No Gmail limits (Gmail = 500 emails/day)
- ✅ Professional "from" address
- ✅ Email analytics
- ✅ Free tier: 100 emails/day

#### Setup Steps:

1. **Sign up**: https://sendgrid.com/
2. **Get API Key**:
   - Settings → API Keys → Create API Key
   - Give it a name: "ReliefNet OTP"
   - Copy the API key

3. **Install SendGrid**:
```bash
npm install @sendgrid/mail
```

4. **Update server.js**:

Replace the nodemailer section with:

```javascript
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(process.env.SENDGRID_API_KEY || 'your-sendgrid-api-key');

async function sendOTPEmail(email, otp) {
  const msg = {
    to: email,
    from: 'noreply@reliefnet.com', // Use your verified sender
    subject: 'Your ReliefNet Login Code',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2>Your Login Code</h2>
        <div style="background: #f0f0f0; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px;">
          ${otp}
        </div>
        <p>This code expires in 5 minutes.</p>
        <p>If you didn't request this, please ignore this email.</p>
      </div>
    `
  };

  try {
    await sgMail.send(msg);
    console.log(`✅ OTP email sent to ${email}`);
    return true;
  } catch (error) {
    console.error('SendGrid error:', error);
    return false;
  }
}
```

---

## 🔧 Troubleshooting

### "Authentication failed" with Gmail
- ❌ **Don't use your regular Gmail password**
- ✅ **Use App Password** (16 characters)
- ✅ Enable 2-Step Verification first

### "Less secure apps" error
- Gmail no longer supports "less secure apps"
- **Must use App Password**

### Emails going to spam
- Add SPF/DKIM records to your domain
- Use SendGrid instead of Gmail
- Ask recipients to whitelist your email

### Rate limits
- **Gmail**: 500 emails/day
- **SendGrid Free**: 100 emails/day
- **SendGrid Paid**: Up to 100,000+/day

---

## 🧪 Testing Email Delivery

### Test with your own email:
```javascript
// In app
Email: your-real-email@gmail.com
Click "Send OTP"
→ Check your inbox!
```

### Test with temp email services:
- https://temp-mail.org/
- https://10minutemail.com/
- https://guerrillamail.com/

Enter temp email → Send OTP → Check temp inbox

---

## 📝 Current Status

### ✅ What Works Now:
- OTP generation
- OTP validation
- OTP expiry (5 minutes)
- Console logging of OTP
- Toast message with OTP (testing)

### ⚠️ What Needs Configuration:
- **Email credentials** (Gmail or SendGrid)
- **Real email sending** (currently disabled)

### 🔜 To Enable Real Emails:
1. Get Gmail App Password OR SendGrid API key
2. Update `server.js` with credentials
3. Restart server
4. Test with real email!

---

## 🚀 Quick Start (Gmail)

**1 minute setup:**

```javascript
// server.js - Line ~27
const emailTransporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'yourname@gmail.com',           // ← Your Gmail
    pass: 'xxxx xxxx xxxx xxxx'           // ← Your 16-char App Password
  }
});
```

**Done!** Restart server and test! 📧

---

## 💡 Recommended Approach

### For Testing (Now):
- ✅ Use Gmail with App Password
- ✅ Keep `testOtp` in response for debugging
- ✅ Log OTP to console as backup

### For Production (Later):
- ✅ Use SendGrid or AWS SES
- ✅ Remove `testOtp` from response
- ✅ Use Redis for OTP storage
- ✅ Add rate limiting
- ✅ Monitor email deliverability

---

**Need Help?** Check the logs:
```bash
cd server
node server.js
# Look for: ✅ OTP email sent to xxx
# Or:      ❌ Failed to send OTP email
```
