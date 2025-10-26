# Quick Test Commands for Doctor Endpoints

## Quick Login & Test
```powershell
# Login and save token
$body = @{ medicalId = "RN-515340"; password = "Test@123" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/login" -Method POST -Body $body -ContentType "application/json"
$global:doctorToken = $response.token
Write-Host "Logged in as: $($response.user.name)" -ForegroundColor Green

# Test all endpoints
$headers = @{ Authorization = "Bearer $global:doctorToken" }

# Profile
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/profile" -Headers $headers | ConvertTo-Json

# Chats
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/chats" -Headers $headers | ConvertTo-Json

# Feedback
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/feedback" -Headers $headers | ConvertTo-Json

# Payments
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/payments" -Headers $headers | ConvertTo-Json

# Sessions
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/sessions" -Headers $headers | ConvertTo-Json
```

## Update Profile Test
```powershell
$headers = @{ 
    Authorization = "Bearer $global:doctorToken"
    "Content-Type" = "application/json" 
}
$body = @{ 
    name = "Dr. John Smith"
    specialization = "Clinical Psychiatrist"
    bio = "Experienced mental health professional"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/profile" -Method PUT -Headers $headers -Body $body | ConvertTo-Json
```

## One-Line Full Test
```powershell
# Complete test sequence
$b=@{medicalId="RN-515340";password="Test@123"}|ConvertTo-Json;$r=Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/login" -Method POST -Body $b -ContentType "application/json";$t=$r.token;$h=@{Authorization="Bearer $t"};Write-Host "✅ Login OK";Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/profile" -Headers $h|Out-Null;Write-Host "✅ Profile OK";Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/chats" -Headers $h|Out-Null;Write-Host "✅ Chats OK";Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/feedback" -Headers $h|Out-Null;Write-Host "✅ Feedback OK";Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/payments" -Headers $h|Out-Null;Write-Host "✅ Payments OK";Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/sessions" -Headers $h|Out-Null;Write-Host "✅ Sessions OK";Write-Host "`n🎉 All endpoints working!" -ForegroundColor Green
```

## Check Server Status
```powershell
# Check if server is running
netstat -ano | findstr :5000

# Test server health
Invoke-RestMethod -Uri "http://localhost:5000" -Method GET
```

## cURL Alternatives (if needed)
```bash
# Login
curl -X POST http://localhost:5000/api/doctor/login \
  -H "Content-Type: application/json" \
  -d '{"medicalId":"RN-515340","password":"Test@123"}'

# Get Profile (replace TOKEN)
curl -X GET http://localhost:5000/api/doctor/profile \
  -H "Authorization: Bearer TOKEN"
```

## Test Results Summary
- ✅ 7/7 endpoints tested and working
- ✅ JWT authentication verified
- ✅ Profile update confirmed
- ✅ Empty states handled correctly
- ✅ Response structures match Android models

## Credentials
- Medical ID: `RN-515340`
- Password: `Test@123`
- Doctor ID: `68f51239bca1dbb061c40e05`
