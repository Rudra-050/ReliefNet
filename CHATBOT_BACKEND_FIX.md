# Chatbot Backend Configuration Fix Guide

## Current Issue
The chatbot API is **ONLINE** but returning a fallback message:
```
⚠️ I couldn't connect to the support assistant right now. Please try again later.
```

This means the Flask API is working, but the AI model integration is not configured properly.

---

## Solution Steps

### Step 1: Access Render Dashboard
1. Go to: **https://dashboard.render.com**
2. Log in with your account
3. Find and click on **"relie-backend"** service

### Step 2: Check the Logs
1. Click on the **"Logs"** tab
2. Look for error messages related to:
   - `API key`
   - `OpenAI`
   - `Gemini`
   - `Connection failed`
   - `Authentication`
3. **Note down any error messages you see**

### Step 3: Check Environment Variables
1. Click on the **"Environment"** tab in your Render service
2. Check if the following environment variables are set:

#### For OpenAI (ChatGPT):
```
OPENAI_API_KEY = sk-xxxxxxxxxxxxxxxxxxxxx
MODEL_NAME = gpt-3.5-turbo  (or gpt-4)
```

#### For Google Gemini:
```
GEMINI_API_KEY = AIzaSyxxxxxxxxxxxxxxxxxxxxx
MODEL_NAME = gemini-pro
```

#### For Hugging Face:
```
HUGGINGFACE_API_KEY = hf_xxxxxxxxxxxxxxxxxxxxx
MODEL_NAME = <model-name>
```

### Step 4: Add Missing API Key

If the API key is missing:

1. **Get an API Key:**
   
   **For OpenAI:**
   - Go to: https://platform.openai.com/api-keys
   - Click "Create new secret key"
   - Copy the key (starts with `sk-`)
   
   **For Google Gemini:**
   - Go to: https://makersuite.google.com/app/apikey
   - Click "Create API Key"
   - Copy the key (starts with `AIza`)

2. **Add to Render:**
   - In Render dashboard → Environment tab
   - Click "Add Environment Variable"
   - Key: `OPENAI_API_KEY` (or `GEMINI_API_KEY`)
   - Value: Paste your API key
   - Click "Save Changes"
   - **Render will automatically redeploy your service**

### Step 5: Verify the Fix

After Render finishes redeploying (wait 2-3 minutes):

**Run this PowerShell test:**
```powershell
cd "C:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet"

$body = @{ 
    message = "Hello, I'm feeling anxious" 
    user_id = "test_user_123" 
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "https://relie-backend-zq69.onrender.com/chat" -Method Post -Body $body -ContentType "application/json"

Write-Host "User: Hello, I'm feeling anxious" -ForegroundColor Cyan
Write-Host "Bot: $($response.response)" -ForegroundColor Green
```

**Expected result:** You should now see a helpful AI response instead of the fallback message.

---

## Alternative: Check Backend Source Code

If you have access to the Flask backend source code:

1. Look for the file (usually `app.py` or `main.py`)
2. Check the `/chat` endpoint
3. Look for:
   - `try/except` blocks that catch API errors
   - Environment variable usage: `os.getenv("OPENAI_API_KEY")`
   - API client initialization
4. Verify the API key is being loaded correctly

---

## Common Issues and Solutions

### Issue: "Invalid API Key"
**Solution:** 
- Verify the API key is correct (no extra spaces)
- Check if the API key has been revoked
- Create a new API key

### Issue: "Quota Exceeded"
**Solution:**
- Check your OpenAI/Gemini billing page
- Add credits to your account
- Or use a free-tier alternative like Hugging Face

### Issue: "Model not found"
**Solution:**
- Check the `MODEL_NAME` environment variable
- Use: `gpt-3.5-turbo` for OpenAI or `gemini-pro` for Gemini

---

## Testing Checklist

After fixing:
- [ ] Render deployment shows "Live"
- [ ] Logs show no errors
- [ ] PowerShell test returns AI response (not fallback)
- [ ] Android app chatbot works (RelieScreen → RelieChat)

---

## Need Help?

If you're still getting the fallback message:
1. Share the **Render logs** (last 50 lines)
2. Share the **environment variables** you've set (without the actual API key values)
3. Mention which AI service you're trying to use (OpenAI/Gemini/Other)

---

**Note:** Your Android app implementation is correct. Once the backend is configured with a valid API key, the chatbot will work perfectly! 🎉
