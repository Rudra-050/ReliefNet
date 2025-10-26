# 🧪 Real-Time Chat Testing Guide

## Quick Start Testing

### Step 1: Start Backend Server
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

### Step 2: Build Android App
```bash
cd Reliefnet-android
.\gradlew.bat assembleDebug
```

**Expected:** `BUILD SUCCESSFUL`

### Step 3: Install on Device(s)
```bash
.\gradlew.bat installDebug
```

## Testing Scenarios

### Scenario 1: Patient to Doctor Chat (Same Emulator)

**Not ideal but possible for quick check:**

1. Login as Patient
2. Navigate to doctor's profile
3. Tap "Chat" button
4. Send message: "Hello doctor!"
5. Logout
6. Login as Doctor
7. Navigate to patient chat
8. You should see the message!

### Scenario 2: Real-Time Testing (Two Emulators) ⭐ RECOMMENDED

**Setup:**
1. Start Emulator 1: 
   - Tools → AVD Manager → Start first emulator
2. Start Emulator 2:
   - Start second emulator
3. Install app on both:
   ```bash
   .\gradlew.bat installDebug
   ```

**Test Flow:**
```
Emulator 1 (Patient)          Emulator 2 (Doctor)
────────────────────          ───────────────────
1. Login as Patient   ───►    1. Login as Doctor
   - ID: 687910a...              - ID: 68f4827e...
   
2. Open Chat Screen   ───►    2. Open Chat Screen
   (Connection: ✅)              (Connection: ✅)
   
3. Type "Hello" ─────►        3. See "Patient is typing..."
   
4. Send message ─────►        4. Message appears instantly!
                                  Text: "Hello"
                                  Time: "10:30 AM"
                                  
5. See "Doctor is typing..." ◄─── 5. Type "Hi, how can I help?"
   
6. Message appears ◄──────    6. Send message
   Text: "Hi, how..."
   
✅ SUCCESS!
```

### Scenario 3: Physical Device Testing

**Important Setup Changes:**

1. **Find Your Computer's IP Address:**
   ```bash
   # Windows
   ipconfig
   # Look for "IPv4 Address" like: 192.168.1.100
   
   # Mac/Linux
   ifconfig
   # Look for "inet" like: 192.168.1.100
   ```

2. **Update ChatViewModel.kt:**
   ```kotlin
   // Line ~57
   private const val SERVER_URL = "http://192.168.1.100:5000"
   // ^^^^^^^^^^^^^^^ Replace with YOUR IP
   ```

3. **Rebuild & Install:**
   ```bash
   .\gradlew.bat assembleDebug installDebug
   ```

4. **Test:**
   - Emulator: Patient (still uses 10.0.2.2)
   - Phone: Doctor (uses your IP)
   - Both should connect!

## What to Verify ✅

### Connection
- [ ] Top bar shows "Active Now" (green dot)
- [ ] Socket.IO connects (check Logcat: "Socket connected")

### Message Sending
- [ ] Type message and hit send
- [ ] Message appears in your screen immediately
- [ ] Message has timestamp (e.g., "10:30 AM")
- [ ] Message appears on other screen within 1 second

### Typing Indicators
- [ ] Start typing in patient screen
- [ ] "Patient is typing..." appears on doctor screen
- [ ] Disappears after 3 seconds
- [ ] Same test reversed (doctor → patient)

### Message History
- [ ] Close app and reopen
- [ ] Previous messages load from database
- [ ] New messages appear in real-time

### UI Design
- [ ] Patient screen: Purple gradient top bar ✅
- [ ] Doctor screen: White top bar ✅
- [ ] Message bubbles look good (rounded corners)
- [ ] Time stamps visible below messages
- [ ] Profile pictures show for received messages

### Navigation
- [ ] Audio call button opens video call screen (audio mode)
- [ ] Video call button opens video call screen (video mode)
- [ ] Back button navigates back

## Common Issues & Fixes

### Issue 1: "Connecting..." Never Changes to "Active Now"

**Possible Causes:**
- Backend not running
- Wrong SERVER_URL
- Firewall blocking port 5000

**Fix:**
```bash
# 1. Check backend is running
cd server
node server.js

# 2. Check Logcat for errors
adb logcat | grep ChatViewModel

# 3. Test backend directly
curl http://localhost:5000
# Should return something (not error)
```

### Issue 2: Messages Don't Appear on Other Screen

**Possible Causes:**
- Socket.IO not connected
- User not registered
- Wrong userId/userType

**Fix:**
```kotlin
// Check Logcat for:
"Socket connected"
"User registered: patient:687910..."
"Message sent: Hello"
"Received message: Hello"

// If not seeing these, check:
1. viewModel.connect() is called in LaunchedEffect
2. Correct userId and userType passed
3. Backend Socket.IO listeners are working
```

### Issue 3: Typing Indicator Never Appears

**Possible Causes:**
- Not emitting `chat:typing` event
- Wrong receiverId

**Fix:**
```kotlin
// Verify in code:
onMessageChange = { text ->
    messageText = text
    if (text.isNotEmpty()) {
        viewModel.sendTypingIndicator(conversationId, doctorId)
        // ^^^^^ Make sure doctorId is correct!
    }
}
```

### Issue 4: Message History Doesn't Load

**Possible Causes:**
- No token
- Backend API endpoint not working
- Conversation doesn't exist

**Fix:**
```bash
# 1. Check token in Logcat
"Token: eyJhbGciOiJIUzI1NiIs..."

# 2. Test API directly
curl -H "Authorization: Bearer <token>" \
  http://localhost:5000/api/chat/messages/687910...:68f4827...

# 3. Check MongoDB has messages
# In MongoDB Compass or CLI:
db.messages.find({ conversationId: "687910...:68f4827..." })
```

## Debugging Tips

### Enable Verbose Logging

Add to `ChatViewModel.kt`:
```kotlin
// In every function, add:
Log.d(TAG, "Function name - Details: $variable")

// Examples:
Log.d(TAG, "sendMessage - ConvId: $conversationId, Content: $content")
Log.d(TAG, "Received message - From: ${message.senderId}, Content: ${message.content}")
```

### Monitor Socket.IO Events

In `ChatViewModel.kt`:
```kotlin
// Add listener for all events
socket?.onAnyEvent { event, args ->
    Log.d(TAG, "Socket Event: $event, Args: ${args.contentToString()}")
}
```

### Check Backend Console

Backend should log:
```
New client connected: <socketId>
User registered: patient:687910...
Message saved: <messageId>
Broadcasting to: doctor:68f4827...
```

## Performance Testing

### Test 1: Message Latency
1. Have stopwatch ready
2. Send message from Patient
3. Time until it appears on Doctor screen
4. **Expected:** < 500ms

### Test 2: Connection Reliability
1. Start chat
2. Turn off Wi-Fi for 10 seconds
3. Turn back on
4. **Expected:** Reconnects automatically, messages sync

### Test 3: Message Ordering
1. Send 10 messages rapidly from Patient
2. **Expected:** All appear on Doctor screen in correct order

### Test 4: Concurrent Users
1. Login 3 patients, 3 doctors
2. Start 3 conversations simultaneously
3. Send messages in all chats
4. **Expected:** No message cross-over, all delivered correctly

## Success Criteria ✅

Your chat system is working if:
- [x] Messages appear in < 1 second on other device
- [x] Typing indicators work both ways
- [x] Connection status accurate ("Active Now" when connected)
- [x] Message history persists after app restart
- [x] No crashes or errors in Logcat
- [x] UI looks polished and professional
- [x] Audio/video call buttons navigate correctly

## Next Steps After Testing

### If Everything Works ✅
1. Celebrate! 🎉
2. Move to next system (voice messages, notifications, etc.)
3. Consider production hardening:
   - Add message retry logic
   - Implement offline queue
   - Add end-to-end encryption
   - Set up monitoring/analytics

### If Issues Found ❌
1. Check Logcat for errors
2. Verify backend console logs
3. Use debugging tips above
4. Check MongoDB for stored messages
5. Test Socket.IO connection separately

## Testing Checklist

**Before Starting:**
- [ ] Node.js installed and backend runs
- [ ] MongoDB running
- [ ] Android Studio setup
- [ ] Emulator(s) or physical device ready

**Backend:**
- [ ] Server starts without errors
- [ ] MongoDB connected
- [ ] Socket.IO initialized
- [ ] Port 5000 accessible

**Android:**
- [ ] App builds successfully
- [ ] App installs on device(s)
- [ ] No runtime crashes
- [ ] Logcat shows "Socket connected"

**Functionality:**
- [ ] Patient can send message
- [ ] Doctor receives message instantly
- [ ] Doctor can reply
- [ ] Patient receives reply instantly
- [ ] Typing indicators work both directions
- [ ] Connection status accurate
- [ ] Message history loads from DB
- [ ] Messages persist after app restart

**UI/UX:**
- [ ] Chat bubbles look professional
- [ ] Timestamps are readable
- [ ] Profile pictures display
- [ ] Colors match design (patient purple, doctor blue/white)
- [ ] Smooth scrolling
- [ ] Input field responsive
- [ ] Send button works

**Integration:**
- [ ] Audio call button navigates
- [ ] Video call button navigates
- [ ] Back button returns to previous screen

---

## 🎓 Pro Tips

1. **Use Logcat Filters:**
   ```
   ChatViewModel
   ```
   This will show only chat-related logs!

2. **Test on Real Wi-Fi:**
   Both devices on same network = most realistic test

3. **Monitor Backend Console:**
   Keep it visible while testing to see events in real-time

4. **Clear App Data Between Tests:**
   Settings → Apps → ReliefNet → Storage → Clear Data
   Ensures fresh state for each test

5. **Use MongoDB Compass:**
   Visually inspect messages collection to verify persistence

---

## 📞 Need Help?

Check these files:
1. `REALTIME_CHAT_COMPLETE.md` - Full system documentation
2. `server/server.js` (Lines 610-810) - Backend Socket.IO
3. `ChatViewModel.kt` - Android Socket.IO client
4. Logcat with filter "ChatViewModel"

Happy Testing! 🚀
