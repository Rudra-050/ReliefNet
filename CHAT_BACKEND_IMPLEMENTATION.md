# Chat System Backend Implementation

## ✅ Implementation Complete

Real-time chat system with WebSocket (Socket.IO) and REST API support.

---

## 📊 Database Models

### 1. Message Model (`models/Message.js`)

Stores individual chat messages:

```javascript
{
  _id: ObjectId,
  conversationId: String,          // Format: "patient_id:doctor_id"
  senderId: ObjectId,              // User who sent the message
  senderType: "patient" | "doctor",
  receiverId: ObjectId,            // User who receives the message
  receiverType: "patient" | "doctor",
  messageType: "text" | "voice" | "image",
  content: String,                 // Message text
  voiceUrl: String,                // URL for voice messages
  imageUrl: String,                // URL for image messages
  isRead: Boolean,                 // Read status
  readAt: Date,                    // When message was read
  createdAt: Date,                 // Auto-generated timestamp
  updatedAt: Date                  // Auto-generated timestamp
}
```

**Indexes:**
- `conversationId` + `createdAt` (compound, for efficient message fetching)
- `senderId` + `receiverId` (for user-specific queries)

### 2. Conversation Model (`models/Conversation.js`)

Tracks active chats between patients and doctors:

```javascript
{
  _id: ObjectId,
  conversationId: String,          // Unique: "smaller_id:larger_id"
  patientId: ObjectId,             // Reference to Patient
  doctorId: ObjectId,              // Reference to Doctor
  patientName: String,
  doctorName: String,
  lastMessage: String,             // Preview of last message
  lastMessageTime: Date,
  lastMessageSender: "patient" | "doctor",
  unreadCountPatient: Number,      // Unread count for patient
  unreadCountDoctor: Number,       // Unread count for doctor
  isActive: Boolean,
  createdAt: Date,
  updatedAt: Date
}
```

**Indexes:**
- `conversationId` (unique)
- `patientId` + `doctorId` (compound)
- `lastMessageTime` (for sorting conversations)

---

## 🔌 Socket.IO Events

### Client → Server Events:

#### 1. `chat:send-message`
Send a new message in real-time.

**Payload:**
```javascript
{
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  senderId: "60d5ec49f1b2c72b8c8e4f1a",
  senderType: "patient",
  receiverId: "60d5ec49f1b2c72b8c8e4f1b",
  receiverType: "doctor",
  messageType: "text",
  content: "Hello Doctor!",
  voiceUrl: null,    // Optional
  imageUrl: null     // Optional
}
```

**Server Response:**
- Emits `chat:message-sent` to sender (confirmation)
- Emits `chat:new-message` to receiver (if online)

#### 2. `chat:mark-read`
Mark messages as read.

**Payload:**
```javascript
{
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  userId: "60d5ec49f1b2c72b8c8e4f1a",
  userType: "patient"
}
```

**Server Response:**
- Emits `chat:marked-read` with conversationId

#### 3. `chat:typing`
Show typing indicator to other user.

**Payload:**
```javascript
{
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  userId: "60d5ec49f1b2c72b8c8e4f1a",
  userType: "patient",
  isTyping: true
}
```

**Server Response:**
- Emits `chat:user-typing` to other user in conversation

### Server → Client Events:

#### 1. `chat:message-sent`
Confirmation that message was saved to database.

**Payload:**
```javascript
{
  _id: "60d5ec49f1b2c72b8c8e4f1c",
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  senderId: "60d5ec49f1b2c72b8c8e4f1a",
  senderType: "patient",
  receiverId: "60d5ec49f1b2c72b8c8e4f1b",
  receiverType: "doctor",
  messageType: "text",
  content: "Hello Doctor!",
  isRead: false,
  createdAt: "2025-10-19T06:30:00.000Z"
}
```

#### 2. `chat:new-message`
New message received from another user.

**Payload:** Same as `chat:message-sent`

#### 3. `chat:marked-read`
Confirmation that messages were marked as read.

**Payload:**
```javascript
{
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b"
}
```

#### 4. `chat:user-typing`
Other user is typing.

**Payload:**
```javascript
{
  conversationId: "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  userId: "60d5ec49f1b2c72b8c8e4f1b",
  userType: "doctor",
  isTyping: true
}
```

#### 5. `chat:error`
Error occurred during chat operation.

**Payload:**
```javascript
{
  message: "Failed to send message"
}
```

---

## 🌐 REST API Endpoints

### 1. GET `/api/chat/conversations/:userType/:userId`
Get all conversations for a user.

**Authentication:** Required (JWT)

**Example Request:**
```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:5000/api/chat/conversations/patient/60d5ec49f1b2c72b8c8e4f1a
```

**Response:**
```json
{
  "success": true,
  "conversations": [
    {
      "_id": "60d5ec49f1b2c72b8c8e4f1d",
      "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
      "patientId": "60d5ec49f1b2c72b8c8e4f1a",
      "doctorId": "60d5ec49f1b2c72b8c8e4f1b",
      "patientName": "Rudransh Bhatt",
      "doctorName": "Dr. Rahul Verma",
      "lastMessage": "Thank you doctor",
      "lastMessageTime": "2025-10-19T06:35:00.000Z",
      "lastMessageSender": "patient",
      "unreadCountPatient": 0,
      "unreadCountDoctor": 2,
      "isActive": true
    }
  ],
  "count": 1
}
```

### 2. GET `/api/chat/messages/:conversationId`
Get messages for a specific conversation.

**Authentication:** Required (JWT)

**Query Parameters:**
- `limit` (optional, default: 50) - Number of messages to fetch
- `before` (optional) - ISO date string for pagination

**Example Request:**
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:5000/api/chat/messages/60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b?limit=20"
```

**Response:**
```json
{
  "success": true,
  "messages": [
    {
      "_id": "60d5ec49f1b2c72b8c8e4f1c",
      "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
      "senderId": "60d5ec49f1b2c72b8c8e4f1a",
      "senderType": "patient",
      "receiverId": "60d5ec49f1b2c72b8c8e4f1b",
      "receiverType": "doctor",
      "messageType": "text",
      "content": "Hello Doctor!",
      "isRead": true,
      "readAt": "2025-10-19T06:31:00.000Z",
      "createdAt": "2025-10-19T06:30:00.000Z"
    }
  ],
  "count": 1
}
```

### 3. POST `/api/chat/send-message`
Send a message via REST API (alternative to Socket.IO).

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  "senderId": "60d5ec49f1b2c72b8c8e4f1a",
  "senderType": "patient",
  "receiverId": "60d5ec49f1b2c72b8c8e4f1b",
  "receiverType": "doctor",
  "messageType": "text",
  "content": "Hello Doctor!",
  "voiceUrl": null,
  "imageUrl": null
}
```

**Response:**
```json
{
  "success": true,
  "message": {
    "_id": "60d5ec49f1b2c72b8c8e4f1c",
    "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
    "senderId": "60d5ec49f1b2c72b8c8e4f1a",
    "senderType": "patient",
    "receiverId": "60d5ec49f1b2c72b8c8e4f1b",
    "receiverType": "doctor",
    "messageType": "text",
    "content": "Hello Doctor!",
    "isRead": false,
    "createdAt": "2025-10-19T06:30:00.000Z"
  }
}
```

### 4. POST `/api/chat/mark-read`
Mark messages as read via REST API.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
  "userId": "60d5ec49f1b2c72b8c8e4f1a",
  "userType": "patient"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Messages marked as read"
}
```

### 5. POST `/api/chat/create-conversation`
Create or get existing conversation between patient and doctor.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "patientId": "60d5ec49f1b2c72b8c8e4f1a",
  "doctorId": "60d5ec49f1b2c72b8c8e4f1b",
  "patientName": "Rudransh Bhatt",
  "doctorName": "Dr. Rahul Verma"
}
```

**Response:**
```json
{
  "success": true,
  "conversation": {
    "_id": "60d5ec49f1b2c72b8c8e4f1d",
    "conversationId": "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
    "patientId": "60d5ec49f1b2c72b8c8e4f1a",
    "doctorId": "60d5ec49f1b2c72b8c8e4f1b",
    "patientName": "Rudransh Bhatt",
    "doctorName": "Dr. Rahul Verma",
    "unreadCountPatient": 0,
    "unreadCountDoctor": 0,
    "isActive": true,
    "createdAt": "2025-10-19T06:30:00.000Z"
  }
}
```

---

## 🔧 How It Works

### Message Flow:

1. **User Types Message**
   - Android app captures text input
   - Emits `chat:typing` event (optional, for typing indicator)

2. **User Sends Message**
   - App emits `chat:send-message` via Socket.IO
   - OR calls POST `/api/chat/send-message` REST API

3. **Server Processes Message**
   - Saves message to MongoDB (`messages` collection)
   - Updates conversation (`conversations` collection)
   - Increments unread count for receiver

4. **Server Delivers Message**
   - Emits `chat:message-sent` to sender (confirmation)
   - Emits `chat:new-message` to receiver (if online via Socket.IO)
   - If receiver offline, message waits in database

5. **Receiver Views Message**
   - Fetches messages via GET `/api/chat/messages/:conversationId`
   - Marks as read via `chat:mark-read` or POST `/api/chat/mark-read`
   - Unread count decreases

### Conversation Flow:

1. **Patient Finds Doctor**
   - Browses doctor list in DiscoverScreen

2. **Initiate Chat**
   - Clicks "Chat" button
   - App calls POST `/api/chat/create-conversation`
   - Gets or creates conversationId

3. **Navigate to Chat Screen**
   - Opens PatientChatScreen with conversationId
   - Connects to Socket.IO with userId and userType
   - Fetches existing messages

4. **Real-time Communication**
   - Both users send/receive messages via Socket.IO
   - Messages saved to database
   - Offline messages delivered when user comes online

---

## 🔐 Security Features

### Implemented:
- ✅ JWT authentication required for all API endpoints
- ✅ User registration via Socket.IO (`socket.on('register')`)
- ✅ User-specific socket connections (stored in `connectedUsers`)
- ✅ Message validation (sender/receiver IDs)
- ✅ Conversation ownership verification

### Recommended:
- 🔲 Validate user permissions (patient can only chat with their assigned doctors)
- 🔲 Rate limiting on message sending (prevent spam)
- 🔲 Message content sanitization (XSS prevention)
- 🔲 File upload validation for voice/image messages
- 🔲 Encrypt sensitive message content
- 🔲 Add message reporting/blocking features

---

## 📝 Usage Examples

### Socket.IO Connection (Android):

```kotlin
// Add Socket.IO dependency to build.gradle.kts
implementation("io.socket:socket.io-client:2.1.0")

// Connect to server
val socket = IO.socket("http://localhost:5000")
socket.connect()

// Register user
socket.emit("register", JSONObject().apply {
    put("userId", "60d5ec49f1b2c72b8c8e4f1a")
    put("userType", "patient")
})

// Send message
socket.emit("chat:send-message", JSONObject().apply {
    put("conversationId", "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b")
    put("senderId", "60d5ec49f1b2c72b8c8e4f1a")
    put("senderType", "patient")
    put("receiverId", "60d5ec49f1b2c72b8c8e4f1b")
    put("receiverType", "doctor")
    put("messageType", "text")
    put("content", "Hello Doctor!")
})

// Listen for new messages
socket.on("chat:new-message") { args ->
    val message = args[0] as JSONObject
    // Update UI with new message
}

// Listen for message sent confirmation
socket.on("chat:message-sent") { args ->
    val message = args[0] as JSONObject
    // Update UI to show message sent
}
```

### REST API Call (Android):

```kotlin
// Using Retrofit
interface ChatApi {
    @GET("api/chat/messages/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 50,
        @Header("Authorization") token: String
    ): Response<MessagesResponse>
    
    @POST("api/chat/send-message")
    suspend fun sendMessage(
        @Body message: SendMessageRequest,
        @Header("Authorization") token: String
    ): Response<MessageResponse>
}

// Fetch messages
val response = chatApi.getMessages(
    conversationId = "60d5ec49f1b2c72b8c8e4f1a:60d5ec49f1b2c72b8c8e4f1b",
    limit = 50,
    token = "Bearer $jwtToken"
)
```

---

## 🧪 Testing

### Test Scenarios:

1. **Create Conversation**
   ```bash
   curl -X POST http://localhost:5000/api/chat/create-conversation \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{
       "patientId": "68f482a12e77f11a7af70d0e",
       "doctorId": "68f4827e3174500e31a5a00f",
       "patientName": "Rudransh Bhatt",
       "doctorName": "Test Kumar"
     }'
   ```

2. **Send Message**
   ```bash
   curl -X POST http://localhost:5000/api/chat/send-message \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{
       "conversationId": "68f482a12e77f11a7af70d0e:68f4827e3174500e31a5a00f",
       "senderId": "68f482a12e77f11a7af70d0e",
       "senderType": "patient",
       "receiverId": "68f4827e3174500e31a5a00f",
       "receiverType": "doctor",
       "content": "Hello Doctor!"
     }'
   ```

3. **Get Messages**
   ```bash
   curl -H "Authorization: Bearer <token>" \
     http://localhost:5000/api/chat/messages/68f482a12e77f11a7af70d0e:68f4827e3174500e31a5a00f
   ```

---

## 📊 Status: BACKEND COMPLETE ✅

**What's Ready:**
- ✅ Message model with MongoDB
- ✅ Conversation model with MongoDB
- ✅ Socket.IO real-time events
- ✅ REST API endpoints
- ✅ Message persistence
- ✅ Unread count tracking
- ✅ Typing indicators
- ✅ Read receipts
- ✅ Conversation management

**Next Steps:**
1. Integrate Socket.IO in Android app
2. Create chat repository/view model
3. Update UI to use real backend data
4. Add voice message recording/playback
5. Add image upload functionality
6. Test real-time messaging between devices

---

*Backend implementation complete. Ready for Android integration!* 🚀
