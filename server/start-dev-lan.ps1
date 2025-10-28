# Start ReliefNet server for LAN testing (physical devices)
# Binds server to 0.0.0.0 so devices on the same network can connect

$env:HOST = "0.0.0.0"
Write-Host "HOST is set to 0.0.0.0 (LAN mode). Starting server..." -ForegroundColor Cyan
npm run dev
