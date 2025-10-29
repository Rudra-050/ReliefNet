# Doctor Session Flow Test
$baseUrl = "https://reliefnet-production-e119.up.railway.app"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Doctor Session Creation & Patient View" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# STEP 1: Doctor Login
Write-Host "STEP 1: Doctor Login" -ForegroundColor Yellow
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
    
    Write-Host "[PASS] Login successful!" -ForegroundColor Green
    Write-Host "Doctor ID: $doctorId" -ForegroundColor White
    Write-Host "Doctor Name: $doctorName" -ForegroundColor White
} catch {
    Write-Host "[FAIL] Login failed: $_" -ForegroundColor Red
    exit
}

# STEP 2: Doctor Creates a Session
Write-Host ""
Write-Host "STEP 2: Doctor Creates a Session" -ForegroundColor Yellow
$sessionDate = Read-Host "Enter session date"
$sessionTime = Read-Host "Enter session time"
$duration = Read-Host "Enter duration in minutes"
if ([string]::IsNullOrEmpty($duration)) { $duration = 60 }
$sessionType = Read-Host "Enter session type"

$sessionBody = @{
    date = $sessionDate
    time = $sessionTime
    duration = [int]$duration
    type = $sessionType
} | ConvertTo-Json

try {
    $headers = @{ Authorization = "Bearer $doctorToken" }
    $sessionResponse = Invoke-RestMethod -Uri "$baseUrl/api/doctor/sessions" -Method Post -Headers $headers -Body $sessionBody -ContentType "application/json"
    
    Write-Host "[PASS] Session created successfully!" -ForegroundColor Green
    Write-Host "Session ID: $($sessionResponse.data._id)" -ForegroundColor White
    Write-Host "Date: $($sessionResponse.data.date)" -ForegroundColor White
    Write-Host "Time: $($sessionResponse.data.time)" -ForegroundColor White
    Write-Host "Status: $($sessionResponse.data.status)" -ForegroundColor White
} catch {
    Write-Host "[FAIL] Session creation failed: $_" -ForegroundColor Red
    exit
}

# STEP 3: Patient Views Available Sessions
Write-Host ""
Write-Host "STEP 3: Patient Views Available Sessions" -ForegroundColor Yellow

try {
    $sessionsResponse = Invoke-RestMethod -Uri "$baseUrl/api/doctor/sessions?doctorId=$doctorId" -Method Get
    
    Write-Host "[PASS] Found $($sessionsResponse.sessions.Count) available sessions" -ForegroundColor Green
    
    foreach ($s in $sessionsResponse.sessions) {
        Write-Host ""
        Write-Host "  Session: $($s._id)" -ForegroundColor Cyan
        Write-Host "  Date: $($s.date)" -ForegroundColor White
        Write-Host "  Time: $($s.time)" -ForegroundColor White
        Write-Host "  Duration: $($s.duration) min" -ForegroundColor White
        Write-Host "  Type: $($s.type)" -ForegroundColor White
        Write-Host "  Status: $($s.status)" -ForegroundColor Green
    }
} catch {
    Write-Host "[FAIL] Failed to fetch sessions: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "How patients see this in the app:" -ForegroundColor Yellow
Write-Host "1. Patient selects 'Book Appointment'" -ForegroundColor White
Write-Host "2. Patient chooses Dr. $doctorName" -ForegroundColor White
Write-Host "3. App fetches available sessions" -ForegroundColor White
Write-Host "4. Patient sees time slots and books" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
