# 🧪 Video/Audio Call Testing Guide

## Quick Test Setup

### Step 1: Start Backend
```bash
cd server
node server.js
```
**Expected Output:**
```
Server running on port 5000
MongoDB connected
Socket.IO initialized
```

### Step 2: Install App on 2 Devices
```bash
cd Reliefnet-android
.\gradlew.bat installDebug
```

---

## Test 1: Video Call (Patient → Doctor)

### Device Setup
- **Emulator 1:** Login as Patient
  - Patient ID: `687910a4748f95606960a4ca`
- **Emulator 2:** Login as Doctor  
  - Doctor ID: `68f4827e3174500e31a5a00f`

### Steps
```
PATIENT (Emulator 1)              DOCTOR (Emulator 2)
────────────────────              ────────────────────

1. Open doctor chat screen
2. Tap video call button 📹  →   
                                  3. Incoming call overlay appears
                                     "Incoming Video Call"
                                     [Accept] [Decline]
                                  
                            ←     4. Tap "Accept"
                                  
5. Screen shows "Connecting..."   5. Screen shows "Connecting..."
6. Patient's camera activates     6. Doctor's camera activates
7. Doctor's video appears   ↔    7. Patient's video appears
8. Small self preview (top-right) 8. Small self preview (top-right)

CONTROLS TEST:
9. Tap mute button 🎤       →    10. Patient's audio stops
11. Tap unmute button 🎤    →    12. Audio resumes
13. Tap camera switch 🔄    →    14. Camera flips (front→back)
15. Tap camera switch again →    16. Camera flips (back→front)
17. Tap end call button 📞  →    18. Call ends, back to chat

✅ PASS if:
- Video appears on both sides
- Audio works both ways
- Mute button turns red when muted
- Camera switches between front/back
- Call ends cleanly
```

---

## Test 2: Audio Call (Doctor → Patient)

### Steps
```
DOCTOR (Emulator 1)               PATIENT (Emulator 2)
───────────────────               ───────────────────

1. Open patient chat screen
2. Tap audio call button 📞  →   
                                  3. Incoming call alert
                                     "Incoming Audio Call"
                                     [Accept] [Decline]
                                  
                            ←     4. Tap "Accept"
                                  
5. Black screen appears          5. Black screen appears
6. "Connecting..." overlay       6. "Connecting..." overlay
7. Connected - can talk     ↔    7. Connected - can talk

AUDIO TEST:
8. Say "Hello"              →    9. Hears "Hello"
10. Tap mute button 🎤      →    11. Doctor's audio stops
                            ←    12. Say "Can you hear me?"
13. Hears nothing (muted)   
14. Tap unmute button 🎤    →    15. Audio resumes
16. Tap end call            →    17. Call ends

✅ PASS if:
- Audio works both directions
- No video rendering (black screen)
- Mute button works correctly
- Call ends cleanly
```

---

## Test 3: Decline Incoming Call

### Steps
```
PATIENT                           DOCTOR
───────                           ──────

1. Tap video call button     →   2. Incoming call appears
                                  
                             ←    3. Tap "Decline" button
                                  
4. Call doesn't connect
5. Stays on chat screen          5. Back to chat screen

✅ PASS if:
- Call doesn't connect
- Both users back to normal screen
- No errors in Logcat
```

---

## Test 4: Call While Offline

### Steps
```
PATIENT                           DOCTOR
───────                           ──────

1. Doctor is offline (app closed)
2. Tap video call button     →   
3. See "Calling..." indefinitely
4. Tap end call button       

✅ PASS if:
- Doesn't crash
- "Calling..." shows correctly
- Can end call manually
```

---

## Common Test Scenarios

### Scenario A: Simultaneous Calls
```
Patient A calls Doctor    →    Doctor accepts
Patient B calls Doctor    →    Doctor busy (optional: reject or queue)
```

### Scenario B: Network Change During Call
```
1. Start video call
2. Switch Wi-Fi to mobile data
3. Verify call continues (may reconnect)
```

### Scenario C: Background App During Call
```
1. Start video call
2. Press home button (app background)
3. Verify audio continues
4. Open app again
5. Verify video resumes
```

---

## Verification Checklist

### Before Starting Tests
- [ ] Backend server running on port 5000
- [ ] MongoDB connected
- [ ] Socket.IO initialized
- [ ] App installed on both devices
- [ ] Camera permission granted
- [ ] Microphone permission granted
- [ ] Both devices on same network (or good internet)

### Video Call Tests
- [ ] Call initiates successfully
- [ ] Incoming call alert appears
- [ ] Accept button works
- [ ] Decline button works
- [ ] Video appears on both sides
- [ ] Audio works both directions
- [ ] Mute button works
- [ ] Unmute button works
- [ ] Camera switch works
- [ ] End call button works
- [ ] Call state displays correctly

### Audio Call Tests
- [ ] Call initiates successfully
- [ ] Incoming call alert appears
- [ ] No video rendering (black screen)
- [ ] Audio works both directions
- [ ] Mute button works
- [ ] End call button works

---

## Debugging

### Check Logcat for CallViewModel
```bash
adb logcat | grep CallViewModel
```

**Expected Logs:**
```
CallViewModel: Socket connected
CallViewModel: User registered: patient:687910a4...
CallViewModel: Call initiated: 68f4827e... (doctor) - video
CallViewModel: Incoming call: 687910a4... video
CallViewModel: Camera switched to front
CallViewModel: Audio muted
CallViewModel: Audio unmuted
```

### Check Backend Console
**Expected Logs:**
```
Socket connected: <socketId>
User registered: patient:687910a4748f95606960a4ca
Call initiated: patient→doctor video
Broadcasting call:incoming to doctor
WebRTC offer received
WebRTC answer received
ICE candidate exchanged
```

---

## Performance Benchmarks

| Metric | Expected | How to Measure |
|--------|----------|----------------|
| Call Connect Time | < 3s | Time from tap to video |
| Video Latency | < 500ms | Wave hand, see on other screen |
| Audio Latency | < 200ms | Speak, hear on other screen |
| Mute Response | < 100ms | Tap mute, audio stops |
| Camera Switch | < 1s | Tap switch, camera flips |

---

## Troubleshooting

### Problem: "Connecting..." Never Changes

**Possible Causes:**
- Backend not running
- Socket.IO not connected
- ICE candidates failing
- Network blocked

**Debug Steps:**
1. Check backend console for logs
2. Check Logcat for "Socket connected"
3. Verify STUN servers reachable:
   ```bash
   ping stun.l.google.com
   ```

### Problem: No Video Appears

**Possible Causes:**
- Camera permission denied
- WebRTC error
- Remote video not added to stream

**Debug Steps:**
1. Check permissions: Settings → Apps → ReliefNet → Permissions
2. Check Logcat for WebRTC errors
3. Verify `remoteStream` has video track

### Problem: No Audio

**Possible Causes:**
- Microphone permission denied
- Audio track not enabled
- Muted by mistake
- Device volume too low

**Debug Steps:**
1. Check microphone permission
2. Verify mute button is NOT red
3. Check device volume slider
4. Check Logcat for audio track errors

---

## Success Criteria

### ✅ Test Passes If:
- Video appears within 3 seconds
- Audio works both directions
- Mute button toggles correctly (red when muted)
- Camera switch flips camera
- Call ends cleanly (no crashes)
- "Connecting..." changes to "Connected"
- No errors in Logcat

### ❌ Test Fails If:
- Call doesn't connect after 10 seconds
- Video doesn't appear
- Audio doesn't work
- Mute button doesn't work
- Camera doesn't switch
- App crashes
- Errors in Logcat

---

## Quick Commands

### Start Backend
```bash
cd server && node server.js
```

### Build App
```bash
cd Reliefnet-android && .\gradlew.bat assembleDebug
```

### Install App
```bash
.\gradlew.bat installDebug
```

### View Logs
```bash
adb logcat | grep CallViewModel
```

### Clear App Data (Fresh Test)
```bash
adb shell pm clear com.sentrive.reliefnet
```

---

## Testing Matrix

| Test Case | Patient→Doctor | Doctor→Patient | Video | Audio | Status |
|-----------|---------------|----------------|-------|-------|--------|
| Initiate Call | ✅ | ✅ | ✅ | ✅ | Ready |
| Accept Call | ✅ | ✅ | ✅ | ✅ | Ready |
| Decline Call | ✅ | ✅ | ✅ | ✅ | Ready |
| Mute Audio | ✅ | ✅ | ✅ | ✅ | Ready |
| Unmute Audio | ✅ | ✅ | ✅ | ✅ | Ready |
| Switch Camera | ✅ | ✅ | ✅ | N/A | Ready |
| End Call | ✅ | ✅ | ✅ | ✅ | Ready |

---

**Ready to test? Start with Test 1: Video Call!** 📹🚀

