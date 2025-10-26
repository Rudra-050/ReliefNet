# 🚀 Chat System - Quick Reference

## Quick Commands

### Start Backend
```bash
cd server
node server.js
```

### Build Android
```bash
cd Reliefnet-android
.\gradlew.bat assembleDebug
```

### Install on Device
```bash
.\gradlew.bat installDebug
```

---

## Key Files

| File | Purpose | Lines |
|------|---------|-------|
| `ChatModels.kt` | Data classes | ~120 |
| `ChatRepository.kt` | API calls | ~103 |
| `ApiService.kt` | Retrofit endpoints | +40 |
| `SocketManager.kt` | Socket.IO functions | +50 |
| `ChatViewModel.kt` | Business logic | +70 |
| `PatientChatScreen.kt` | Patient UI | +30 |
| `DoctorChatScreen.kt` | Doctor UI | +30 |

---

## API Endpoints

```kotlin
GET  /api/chat/conversations/{userType}/{userId}
GET  /api/chat/messages/{conversationId}
POST /api/chat/upload-voice (multipart)
POST /api/chat/upload-image (multipart)
```

---

## Socket.IO Events

### Emit (Send)
```kotlin
socket.emit("register", { userId, userType })
socket.emit("chat:send-message", { conversationId, senderId, receiverId, content, ... })
socket.emit("chat:typing", { conversationId, senderId, receiverId })
socket.emit("chat:mark-read", { conversationId, messageIds })
```

### Listen (Receive)
```kotlin
socket.on("chat:new-message") // New message received
socket.on("chat:typing")      // Other user typing
socket.on("chat:delivered")   // Message delivered
socket.on("chat:read")        // Message read
```

---

## Testing Quick Start

### Two Emulators
```bash
# Terminal 1: Start Backend
cd server; node server.js

# Terminal 2: Install App
cd Reliefnet-android
.\gradlew.bat installDebug

# Emulator 1: Login as Patient → Open chat
# Emulator 2: Login as Doctor → Open chat
# Send messages between them!
```

### Physical Device
1. Find your IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
2. Update `ChatViewModel.kt`:
   ```kotlin
   private const val SERVER_URL = "http://192.168.1.XXX:5000"
   ```
3. Rebuild and install

---

## Common Issues

| Problem | Fix |
|---------|-----|
| "Connecting..." stuck | Check backend is running, verify SERVER_URL |
| Messages don't appear | Check Socket.IO connected (Logcat: "Socket connected") |
| Typing indicator missing | Verify `sendTypingIndicator()` called on text change |
| History doesn't load | Check token exists, backend API working |

---

## Debugging

### Logcat Filter
```
ChatViewModel
```

### Enable All Socket Events
```kotlin
socket?.onAnyEvent { event, args ->
    Log.d(TAG, "Event: $event, Args: ${args.contentToString()}")
}
```

### Check Backend Logs
Backend should show:
```
New client connected
User registered: patient:687910...
Message saved
Broadcasting to: doctor:68f4827...
```

---

## Code Snippets

### Send Message
```kotlin
viewModel.sendMessage(
    conversationId = "$patientId:$doctorId",
    receiverId = doctorId,
    receiverType = "doctor",
    content = messageText
)
```

### Load History
```kotlin
val token = TokenManager.getToken(context) ?: ""
viewModel.loadMessagesForConversation(conversationId, token)
```

### Display Messages
```kotlin
val messages by viewModel.messages.collectAsState()
val uiState by viewModel.uiState.collectAsState()

LazyColumn {
    items(messages) { message ->
        MessageBubble(message)
    }
}
```

---

## Architecture Layers

```
UI (Composable)
    ↓
ViewModel (Business Logic + Socket.IO)
    ↓
Repository (API Calls)
    ↓
Network (Retrofit + Socket.IO)
    ↓
Backend (Node.js + MongoDB)
```

---

## Connection Flow

```
1. Screen opens
2. LaunchedEffect runs
3. viewModel.connect(userId, userType)
4. Socket.IO connects to server
5. Emit "register" event
6. Server acknowledges
7. uiState = Connected
8. Load message history from API
9. Listen for real-time events
10. User can send/receive messages
```

---

## Build Verification

```bash
# Should see:
BUILD SUCCESSFUL in Xs
37 actionable tasks: 4 executed, 33 up-to-date
```

---

## Success Checklist

- [ ] Backend running (port 5000)
- [ ] MongoDB connected
- [ ] App builds successfully
- [ ] Socket connects (Logcat: "Socket connected")
- [ ] Messages send instantly (< 1s)
- [ ] Typing indicators work both ways
- [ ] "Active Now" shows when connected
- [ ] History loads from database

---

## Documentation

- **Full Guide:** `REALTIME_CHAT_COMPLETE.md`
- **Testing:** `CHAT_TESTING_GUIDE.md`
- **Summary:** `CHAT_SYSTEM_IMPLEMENTATION_SUMMARY.md`
- **This Card:** `CHAT_QUICK_REFERENCE.md`

---

## Status

✅ **Implementation: 100% Complete**  
✅ **Build: Successful**  
🧪 **Testing: Ready**  
🚀 **Deployment: Ready**

---

*Quick reference for ReliefNet Chat System*  
*For detailed docs, see REALTIME_CHAT_COMPLETE.md*
