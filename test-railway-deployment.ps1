# Railway Deployment Test Script
# Run this after deploying to Railway to verify everything works

param(
    [Parameter(Mandatory=$true)]
    [string]$RailwayUrl
)

# Remove trailing slash if present
$RailwayUrl = $RailwayUrl.TrimEnd('/')

Write-Host "🧪 Testing Railway Deployment" -ForegroundColor Cyan
Write-Host "URL: $RailwayUrl" -ForegroundColor Yellow
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""

$passed = 0
$failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Body = @{},
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "Testing: $Name" -ForegroundColor White -NoNewline
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            ContentType = "application/json"
            TimeoutSec = 30
        }
        
        if ($Body.Count -gt 0) {
            $params.Body = ($Body | ConvertTo-Json)
        }
        
        $response = Invoke-WebRequest @params -UseBasicParsing
        
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host " ✅ PASS" -ForegroundColor Green
            Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Gray
            
            if ($response.Content) {
                $content = $response.Content
                if ($content.Length -gt 100) {
                    $content = $content.Substring(0, 100) + "..."
                }
                Write-Host "   Response: $content" -ForegroundColor Gray
            }
            
            $script:passed++
            return $true
        } else {
            Write-Host " ❌ FAIL" -ForegroundColor Red
            Write-Host "   Expected: $ExpectedStatus, Got: $($response.StatusCode)" -ForegroundColor Red
            $script:failed++
            return $false
        }
    } catch {
        Write-Host " ❌ FAIL" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
        return $false
    }
    
    Write-Host ""
}

# Test 1: Basic Health Check
Write-Host "`n1️⃣  Basic Health Checks" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray

Test-Endpoint -Name "Root Endpoint" -Url "$RailwayUrl/"
Test-Endpoint -Name "Health Check" -Url "$RailwayUrl/health"

# Test 2: Authentication Endpoints
Write-Host "`n2️⃣  Authentication Endpoints" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray

# Test OTP send (will fail without valid email, but should return proper error)
$testEmail = "test@example.com"
Write-Host "`nNote: Testing with dummy email - expect validation error" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "$RailwayUrl/auth/send-otp" `
        -Method POST `
        -ContentType "application/json" `
        -Body (@{email = $testEmail} | ConvertTo-Json) `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    Write-Host "Send OTP Endpoint: ✅ Reachable" -ForegroundColor Green
    $passed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400 -or $_.Exception.Response.StatusCode.value__ -eq 500) {
        Write-Host "Send OTP Endpoint: ✅ Reachable (returned expected error)" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "Send OTP Endpoint: ❌ FAIL - $($_.Exception.Message)" -ForegroundColor Red
        $failed++
    }
}

# Test 3: Protected Routes (should return 401)
Write-Host "`n3️⃣  Protected Routes (Should Require Auth)" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray

$protectedEndpoints = @(
    "/api/doctors",
    "/api/patient/profile",
    "/api/bookings"
)

foreach ($endpoint in $protectedEndpoints) {
    try {
        $response = Invoke-WebRequest -Uri "$RailwayUrl$endpoint" `
            -Method GET `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue
        
        Write-Host "$endpoint : ❌ Should require auth but didn't" -ForegroundColor Red
        $failed++
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "$endpoint : ✅ Correctly protected" -ForegroundColor Green
            $passed++
        } else {
            Write-Host "$endpoint : ⚠️  Unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
            $failed++
        }
    }
}

# Test 4: HTTPS and Security Headers
Write-Host "`n4️⃣  Security Checks" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray

try {
    $response = Invoke-WebRequest -Uri "$RailwayUrl/" -Method GET -UseBasicParsing
    
    # Check HTTPS
    if ($RailwayUrl -like "https://*") {
        Write-Host "HTTPS Enabled: ✅ PASS" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "HTTPS Enabled: ❌ FAIL - Using HTTP" -ForegroundColor Red
        $failed++
    }
    
    # Check security headers
    $headers = $response.Headers
    
    if ($headers["X-Content-Type-Options"]) {
        Write-Host "X-Content-Type-Options: ✅ Present" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "X-Content-Type-Options: ⚠️  Missing (Helmet might not be configured)" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Security Check: ❌ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $failed++
}

# Test 5: Response Time
Write-Host "`n5️⃣  Performance Check" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $response = Invoke-WebRequest -Uri "$RailwayUrl/" -Method GET -UseBasicParsing
    $stopwatch.Stop()
    
    $responseTime = $stopwatch.ElapsedMilliseconds
    Write-Host "Response Time: $responseTime ms" -ForegroundColor $(if ($responseTime -lt 2000) { "Green" } else { "Yellow" })
    
    if ($responseTime -lt 2000) {
        Write-Host "Performance: ✅ Good (<2s)" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "Performance: ⚠️  Slow (>2s)" -ForegroundColor Yellow
        $failed++
    }
} catch {
    Write-Host "Performance Test: ❌ FAIL" -ForegroundColor Red
    $failed++
}

# Summary
Write-Host "`n" + ("=" * 60) -ForegroundColor Gray
Write-Host "📊 Test Summary" -ForegroundColor Cyan
Write-Host ("-" * 60) -ForegroundColor Gray
Write-Host "Total Tests: $($passed + $failed)" -ForegroundColor White
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red

$successRate = [math]::Round(($passed / ($passed + $failed)) * 100, 2)
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })

Write-Host "`n" + ("=" * 60) -ForegroundColor Gray

if ($failed -eq 0) {
    Write-Host "🎉 All tests passed! Your deployment is ready!" -ForegroundColor Green
} elseif ($successRate -ge 70) {
    Write-Host "⚠️  Most tests passed, but some issues need attention." -ForegroundColor Yellow
    Write-Host "Check the Railway logs for more details." -ForegroundColor Yellow
} else {
    Write-Host "❌ Multiple tests failed. Check your configuration:" -ForegroundColor Red
    Write-Host "  1. Verify environment variables in Railway" -ForegroundColor Yellow
    Write-Host "  2. Check Railway deployment logs" -ForegroundColor Yellow
    Write-Host "  3. Ensure MongoDB whitelist includes 0.0.0.0/0" -ForegroundColor Yellow
}

Write-Host "`n📋 Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Check Railway logs: https://railway.app" -ForegroundColor White
Write-Host "  2. Test with real email for OTP" -ForegroundColor White
Write-Host "  3. Update Android app with this URL: $RailwayUrl" -ForegroundColor White
Write-Host "  4. Monitor logs for any errors" -ForegroundColor White

Write-Host "`n✅ Deployment URL: $RailwayUrl" -ForegroundColor Green
Write-Host ""
