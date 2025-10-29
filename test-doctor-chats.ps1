# Test Doctor Chats Endpoint
# This script tests the /api/doctor/chats endpoint

Write-Host "`n=== Doctor Chats Endpoint Test ===" -ForegroundColor Cyan

# First, login as a doctor to get a token
Write-Host "`n1. Testing doctor login to get authentication token..." -ForegroundColor Yellow

$loginBody = @{
    medicalId = "DOC12345"  # Replace with actual doctor medical ID
    password = "doctor123"   # Replace with actual doctor password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/api/doctor/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"
    
    Write-Host "✓ Login successful!" -ForegroundColor Green
    Write-Host "Doctor: $($loginResponse.doctor.name)" -ForegroundColor White
    $token = $loginResponse.token
} catch {
    Write-Host "✗ Login failed: $_" -ForegroundColor Red
    Write-Host "`nPlease update the script with valid doctor credentials:" -ForegroundColor Yellow
    Write-Host "  - medicalId: The doctor's medical ID" -ForegroundColor Yellow
    Write-Host "  - password: The doctor's password" -ForegroundColor Yellow
    exit 1
}

# Test the chats endpoint
Write-Host "`n2. Testing GET /api/doctor/chats endpoint..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    $chatsResponse = Invoke-RestMethod -Uri "https://reliefnet-production-e119.up.railway.app/api/doctor/chats" `
        -Method GET `
        -Headers $headers
    
    Write-Host "✓ Chats endpoint successful!" -ForegroundColor Green
    Write-Host "`nResponse:" -ForegroundColor Cyan
    Write-Host "Success: $($chatsResponse.success)" -ForegroundColor White
    Write-Host "Number of conversations: $($chatsResponse.conversations.Count)" -ForegroundColor White
    
    if ($chatsResponse.conversations.Count -gt 0) {
        Write-Host "`nFirst few conversations:" -ForegroundColor Cyan
        $chatsResponse.conversations | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - Patient: $($_.patientName)" -ForegroundColor White
            Write-Host "    Last Message: $($_.lastMessage)" -ForegroundColor Gray
            Write-Host "    Unread: $($_.unreadCountDoctor)" -ForegroundColor Gray
            Write-Host ""
        }
    } else {
        Write-Host "`n⚠ No conversations found for this doctor." -ForegroundColor Yellow
        Write-Host "This is expected if no patients have messaged this doctor yet." -ForegroundColor Gray
    }
    
    Write-Host "`nFull Response JSON:" -ForegroundColor Cyan
    $chatsResponse | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "✗ Failed to fetch chats: $_" -ForegroundColor Red
    Write-Host "`nError Details:" -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "`nServer Response:" -ForegroundColor Yellow
        Write-Host $responseBody -ForegroundColor Red
    }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
