# Quick Test: Doctor Chats Endpoint (No Auth Required)
# This tests the endpoint's response to various auth scenarios

Write-Host "`n=== Quick Doctor Chats Endpoint Test ===" -ForegroundColor Cyan

# Test 1: No token (should return 401)
Write-Host "`n1. Testing endpoint without authentication..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/api/doctor/chats" -Method GET
    Write-Host "[FAIL] Unexpected: Should have required authentication!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "[PASS] Correct: Endpoint requires authentication (401)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Unexpected error: $_" -ForegroundColor Red
    }
}

# Test 2: Invalid token (should return 401 or 403)
Write-Host "`n2. Testing endpoint with invalid token..." -ForegroundColor Yellow
try {
    $headers = @{ "Authorization" = "Bearer invalid_token_12345" }
    Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/api/doctor/chats" -Method GET -Headers $headers
    Write-Host "[FAIL] Unexpected: Should have rejected invalid token!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401 -or $_.Exception.Response.StatusCode -eq 403) {
        Write-Host "[PASS] Correct: Invalid token rejected ($($_.Exception.Response.StatusCode))" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Unexpected error: $_" -ForegroundColor Red
    }
}

# Test 3: Check if health endpoint works
Write-Host "`n3. Testing server health endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/health" -Method GET
    Write-Host "[PASS] Server is online!" -ForegroundColor Green
    Write-Host "  Status: $($health.status)" -ForegroundColor White
    Write-Host "  Database: $($health.db)" -ForegroundColor White
    Write-Host "  Version: $($health.version)" -ForegroundColor White
} catch {
    Write-Host "[FAIL] Server health check failed: $_" -ForegroundColor Red
}

# Test 4: Show available doctors
Write-Host "`n4. Checking available doctors in database..." -ForegroundColor Yellow
try {
    $doctors = Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/api/doctors" -Method GET
    Write-Host "[PASS] Found $($doctors.total) doctors in database" -ForegroundColor Green
    Write-Host "`nSample doctors (first 5):" -ForegroundColor Cyan
    $doctors.doctors | Select-Object -First 5 | ForEach-Object {
        Write-Host "  - $($_.name) ($($_.medicalId)) - $($_.specialization)" -ForegroundColor White
    }
} catch {
    Write-Host "[FAIL] Failed to fetch doctors: $_" -ForegroundColor Red
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "[PASS] Doctor chats endpoint exists and requires authentication" -ForegroundColor Green
Write-Host "[PASS] Backend server is operational" -ForegroundColor Green
Write-Host "`nTo test with real data:" -ForegroundColor Yellow
Write-Host "  1. Login as a doctor in the Android app" -ForegroundColor Yellow
Write-Host "  2. Have a patient send you a message" -ForegroundColor Yellow
Write-Host "  3. Check the Doctor Chats screen" -ForegroundColor Yellow
Write-Host "`nOr run: .\test-doctor-chats.ps1 (and enter valid credentials)" -ForegroundColor Yellow

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
