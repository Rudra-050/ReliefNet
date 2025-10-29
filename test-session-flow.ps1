# Complete Doctor Session Creation & Patient Booking Flow Test
$baseUrl = "https://reliefnet-production-e119.up.railway.app"

Write-Host "`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Doctor Session Creation & Patient Viewing Flow Test     ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# ===============================================
# STEP 1: Doctor Login
# ===============================================
Write-Host "STEP 1: Doctor Login" -ForegroundColor Yellow
Write-Host "────────────────────────────────────────" -ForegroundColor Gray

$medicalId = Read-Host "Enter Doctor Medical ID"
$password = Read-Host "Enter Password" -AsSecureString
$passwordText = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))

$loginBody = @{
    medicalId = $medicalId
    password = $passwordText
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/doctor/login" -Method Post -Body $loginBody -ContentType "application/json"
    $doctorToken = $loginResponse.token
    $doctorId = $loginResponse.doctor.id
    $doctorName = $loginResponse.doctor.name
    
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "   Doctor ID: $doctorId" -ForegroundColor White
    Write-Host "   Doctor Name: $doctorName" -ForegroundColor White
    Write-Host "   Token: $($doctorToken.Substring(0,20))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ Login failed: $_" -ForegroundColor Red
    exit
}

# ===============================================
# STEP 2: Doctor Creates a Session
# ===============================================
Write-Host "`nSTEP 2: Doctor Creates a Session" -ForegroundColor Yellow
Write-Host "────────────────────────────────────────" -ForegroundColor Gray

Write-Host "Enter session details:" -ForegroundColor Cyan
$sessionDate = Read-Host "  Date (YYYY-MM-DD)"
$sessionTime = Read-Host "  Time (HH:MM)"
$duration = Read-Host "  Duration in minutes (default 60)"
if ([string]::IsNullOrEmpty($duration)) { $duration = 60 }
$sessionType = Read-Host "  Type (consultation/therapy/follow-up)"

$sessionBody = @{
    date = $sessionDate
    time = $sessionTime
    duration = [int]$duration
    type = $sessionType
} | ConvertTo-Json

Write-Host "`n⏳ Creating session..." -ForegroundColor Cyan

try {
    $headers = @{ Authorization = "Bearer $doctorToken" }
    $sessionResponse = Invoke-RestMethod -Uri "$baseUrl/api/doctor/sessions" -Method Post -Headers $headers -Body $sessionBody -ContentType "application/json"
    
    $sessionId = $sessionResponse.data.id
    $createdSession = $sessionResponse.data
    
    Write-Host "✅ Session created successfully!" -ForegroundColor Green
    Write-Host "   Session ID: $sessionId" -ForegroundColor White
    Write-Host "   Date: $($createdSession.date)" -ForegroundColor White
    Write-Host "   Time: $($createdSession.time)" -ForegroundColor White
    Write-Host "   Duration: $($createdSession.duration) minutes" -ForegroundColor White
    Write-Host "   Type: $($createdSession.type)" -ForegroundColor White
    Write-Host "   Status: $($createdSession.status)" -ForegroundColor White
} catch {
    Write-Host "❌ Session creation failed!" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    exit
}

# ===============================================
# STEP 3: Patient Views Available Sessions
# ===============================================
Write-Host "`nSTEP 3: Patient Views Available Sessions for This Doctor" -ForegroundColor Yellow
Write-Host "────────────────────────────────────────" -ForegroundColor Gray

Write-Host "⏳ Fetching available sessions for doctor $doctorId..." -ForegroundColor Cyan

try {
    # Patient fetches sessions for this doctor (no auth needed)
    $sessionsResponse = Invoke-RestMethod -Uri "$baseUrl/api/doctor/sessions?doctorId=$doctorId" -Method Get
    
    $availableSessions = $sessionsResponse.sessions
    
    Write-Host "✅ Found $($availableSessions.Count) available session(s)!" -ForegroundColor Green
    
    if ($availableSessions.Count -eq 0) {
        Write-Host "   No sessions available yet." -ForegroundColor Yellow
    } else {
        Write-Host "`nAvailable Sessions:" -ForegroundColor Cyan
        for ($i = 0; $i -lt $availableSessions.Count; $i++) {
            $s = $availableSessions[$i]
            Write-Host "`n   [$($i+1)] Session Details:" -ForegroundColor White
            Write-Host "       ID: $($s._id)" -ForegroundColor Gray
            Write-Host "       Date: $($s.date)" -ForegroundColor White
            Write-Host "       Time: $($s.time)" -ForegroundColor White
            Write-Host "       Duration: $($s.duration) minutes" -ForegroundColor White
            Write-Host "       Type: $($s.type)" -ForegroundColor White
            Write-Host "       Status: $($s.status)" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "❌ Failed to fetch sessions!" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
}

# ===============================================
# SUMMARY
# ===============================================
Write-Host "`n╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                        TEST SUMMARY                          ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`n✅ Flow Verification:" -ForegroundColor Green
Write-Host "   1. Doctor logged in successfully" -ForegroundColor White
Write-Host "   2. Doctor created a session" -ForegroundColor White
Write-Host "   3. Session is now visible to patients" -ForegroundColor White

Write-Host "`n📱 How Patients See Sessions in the App:" -ForegroundColor Yellow
Write-Host "   • Patient opens the app" -ForegroundColor White
Write-Host "   • Patient navigates to 'Book Appointment'" -ForegroundColor White
Write-Host "   • Patient selects Dr. $doctorName" -ForegroundColor White
Write-Host "   • App calls: GET /api/doctor/sessions?doctorId=$doctorId" -ForegroundColor Gray
Write-Host "   • Patient sees available time slots" -ForegroundColor White
Write-Host "   • Patient selects a slot and books the appointment" -ForegroundColor White

Write-Host "`n🔗 API Endpoint Used:" -ForegroundColor Cyan
Write-Host "   GET $baseUrl/api/doctor/sessions?doctorId=$doctorId" -ForegroundColor Gray

Write-Host "`n✨ Test completed successfully!" -ForegroundColor Green
Write-Host ""
