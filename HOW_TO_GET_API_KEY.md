# How to Get OpenAI API Key

## Quick Steps

### 1. Go to OpenAI Platform
**URL**: https://platform.openai.com/api-keys

### 2. Sign In
- Use your OpenAI account
- If you don't have one, create it at https://platform.openai.com/signup

### 3. Create API Key
- Click **"+ Create new secret key"** button
- Give it a name (e.g., "ReliefNet Chatbot")
- Click **"Create secret key"**

### 4. Copy the Key
- ⚠️ **IMPORTANT**: Copy it immediately!
- It starts with `sk-proj-` or `sk-`
- You won't be able to see it again
- Example format: `sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 5. Add to Render
- Go to Render Dashboard: https://dashboard.render.com
- Click on `relie-backend` service
- Go to **Environment** tab
- Click **"Add Environment Variable"**
- Key: `OPENAI_API_KEY`
- Value: Paste your API key
- Click **"Save Changes"**

---

## Important Notes

### ⚠️ Billing
- OpenAI API is **NOT free** (though they give some free credits initially)
- You need to add a payment method at: https://platform.openai.com/account/billing
- Check your usage at: https://platform.openai.com/usage

### 💰 Free Tier / Credits
- New accounts get $5 free credits (valid for 3 months)
- After that, you pay as you go
- GPT-3.5-turbo is cheapest (~$0.002 per 1K tokens)
- GPT-4 is more expensive (~$0.03 per 1K tokens)

### 🔒 Security
- **NEVER** share your API key publicly
- **NEVER** commit it to GitHub
- Keep it only in Render's Environment Variables
- Rotate it if compromised

---

## Alternative: Use Google Gemini (Free Option)

If you don't want to pay for OpenAI, use Google Gemini instead:

### 1. Get Gemini API Key
**URL**: https://makersuite.google.com/app/apikey

### 2. Create Key
- Click **"Create API Key"**
- Select project or create new one
- Copy the key (starts with `AIza`)

### 3. Add to Render
- Key: `GEMINI_API_KEY` (instead of OPENAI_API_KEY)
- Value: Paste your Gemini key

### 4. Update Backend Code (if needed)
The backend code might need to be modified to use Gemini instead of OpenAI. Check with the backend developer or look at the Flask app code.

---

## Verify It's Working

After adding the key and waiting for Render to redeploy (2-3 minutes):

### Test from PowerShell:
```powershell
$body = @{ 
    message = "Hello, I need help" 
    user_id = "test_123" 
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "https://relie-backend-zq69.onrender.com/chat" -Method Post -Body $body -ContentType "application/json"

Write-Host "Bot: $($response.response)"
```

**Expected**: You should see an actual AI response, not the fallback message!

---

## Troubleshooting

### "Invalid API Key"
- Check if you copied the full key (including `sk-` prefix)
- Make sure there are no extra spaces
- Try creating a new key

### "Quota Exceeded"
- Check your OpenAI billing page
- Add payment method or credits
- Or switch to Gemini (free tier)

### Still showing fallback message
- Check Render logs for specific error
- Wait full 2-3 minutes after saving
- Verify environment variable name is exactly: `OPENAI_API_KEY`

---

## Cost Estimation

For a mental health chatbot with moderate usage:

**OpenAI GPT-3.5-turbo**:
- ~100 conversations/day = ~$1-2/month
- ~1000 conversations/day = ~$10-20/month

**Google Gemini**:
- Free tier: 60 requests/minute
- Good for development/testing
- May need paid tier for production

---

Good luck! 🚀
