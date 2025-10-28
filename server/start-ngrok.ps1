# Start backend with ngrok tunnel
# This allows your debug APK to access the backend from anywhere

Write-Host "🚀 Starting ReliefNet Backend with ngrok tunnel..." -ForegroundColor Cyan
Write-Host ""

# Check if ngrok is installed
if (-not (Get-Command ngrok -ErrorAction SilentlyContinue)) {
    Write-Host "❌ ngrok not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install ngrok:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://ngrok.com/download" -ForegroundColor Yellow
    Write-Host "2. Extract ngrok.exe to this folder OR add to PATH" -ForegroundColor Yellow
    Write-Host "3. Sign up at https://dashboard.ngrok.com/signup" -ForegroundColor Yellow
    Write-Host "4. Run: ngrok config add-authtoken YOUR_TOKEN" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Start the backend in background
Write-Host "📦 Starting backend server..." -ForegroundColor Green
$backendProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "npm run dev" -PassThru -WindowStyle Normal

# Wait for backend to start
Write-Host "⏳ Waiting for backend to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Start ngrok tunnel
Write-Host "🌐 Starting ngrok tunnel on port 5000..." -ForegroundColor Green
Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Your backend will be accessible via ngrok URL" -ForegroundColor White
Write-Host "  Copy the HTTPS URL and update DEV_NGROK_URL in gradle.properties" -ForegroundColor White
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Start ngrok (this will block)
ngrok http 5000

# Cleanup when ngrok stops
Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
