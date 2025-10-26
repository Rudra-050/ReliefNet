# Quick Chatbot Backend Diagnostic Script
# This script helps identify the exact issue with your chatbot backend

Write-Host ""
Write-Host "================================================" -ForegroundColor Magenta
Write-Host "  CHATBOT BACKEND DIAGNOSTIC TOOL" -ForegroundColor Magenta
Write-Host "================================================" -ForegroundColor Magenta
Write-Host ""

$BACKEND_URL = "https://relie-backend-zq69.onrender.com"

# Test 1: Check if backend is online
Write-Host "[STEP 1] Checking if backend is online..." -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri $BACKEND_URL -TimeoutSec 30
    Write-Host "✓ Backend is ONLINE (Status: $($health.StatusCode))" -ForegroundColor Green
    Write-Host "  Response: $($health.Content)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Backend is OFFLINE" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "ACTION REQUIRED: Your Render service may be down." -ForegroundColor Yellow
    Write-Host "Go to: https://dashboard.render.com and check your service status." -ForegroundColor Yellow
    exit
}

Write-Host ""

# Test 2: Send a test message
Write-Host "[STEP 2] Sending test message to chatbot..." -ForegroundColor Yellow
$testMessage = "Hello, I need help with anxiety"
$testBody = @{
    message = $testMessage
    user_id = "diagnostic_user_$(Get-Random -Maximum 9999)"
} | ConvertTo-Json

Write-Host "  Sending: '$testMessage'" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$BACKEND_URL/chat" -Method Post -Body $testBody -ContentType "application/json" -TimeoutSec 60
    $botResponse = $response.response
    
    Write-Host "  Bot Response: $botResponse" -ForegroundColor White
    
    # Analyze the response
    if ($botResponse -like "*couldn't connect*" -or $botResponse -like "*try again later*" -or $botResponse -like "*⚠*") {
        Write-Host ""
        Write-Host "✗ ISSUE DETECTED: Backend returning fallback message" -ForegroundColor Red
        Write-Host ""
        Write-Host "DIAGNOSIS:" -ForegroundColor Yellow
        Write-Host "The Flask API is working, but the AI model is not connected." -ForegroundColor White
        Write-Host ""
        Write-Host "MOST LIKELY CAUSES:" -ForegroundColor Yellow
        Write-Host "1. Missing API Key" -ForegroundColor Cyan
        Write-Host "   → AI service (OpenAI/Gemini) API key not configured in Render" -ForegroundColor Gray
        Write-Host ""
        Write-Host "2. Invalid API Key" -ForegroundColor Cyan
        Write-Host "   → API key is set but expired or incorrect" -ForegroundColor Gray
        Write-Host ""
        Write-Host "3. API Quota Exceeded" -ForegroundColor Cyan
        Write-Host "   → Your OpenAI/Gemini account ran out of credits" -ForegroundColor Gray
        Write-Host ""
        Write-Host "RECOMMENDED ACTIONS:" -ForegroundColor Yellow
        Write-Host "───────────────────────────────────────────────" -ForegroundColor Gray
        Write-Host "1. Go to Render Dashboard:" -ForegroundColor Green
        Write-Host "   https://dashboard.render.com" -ForegroundColor Blue
        Write-Host ""
        Write-Host "2. Click on your 'relie-backend' service" -ForegroundColor Green
        Write-Host ""
        Write-Host "3. Check the LOGS tab for errors like:" -ForegroundColor Green
        Write-Host "   • 'API key not found'" -ForegroundColor Gray
        Write-Host "   • 'Authentication failed'" -ForegroundColor Gray
        Write-Host "   • 'Quota exceeded'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "4. Check the ENVIRONMENT tab for:" -ForegroundColor Green
        Write-Host "   • OPENAI_API_KEY (for ChatGPT)" -ForegroundColor Gray
        Write-Host "   • GEMINI_API_KEY (for Google Gemini)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "5. If missing, add the API key:" -ForegroundColor Green
        Write-Host "   a) Get key from: https://platform.openai.com/api-keys" -ForegroundColor Gray
        Write-Host "   b) Add to Render Environment variables" -ForegroundColor Gray
        Write-Host "   c) Wait for automatic redeployment (2-3 min)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "See CHATBOT_BACKEND_FIX.md for detailed instructions" -ForegroundColor Magenta
        
    } else {
        Write-Host ""
        Write-Host "✓ SUCCESS: AI model is responding correctly!" -ForegroundColor Green
        Write-Host "  Your chatbot backend is fully configured and working." -ForegroundColor Gray
    }
    
} catch {
    Write-Host "✗ Error calling chat endpoint" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Magenta
Write-Host ""
