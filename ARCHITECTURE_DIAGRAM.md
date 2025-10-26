# ReliefNet - Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          RELIEFNET ANDROID APP                       │
│                         (Kotlin + Jetpack Compose)                   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
        ┌───────────────────────────┴───────────────────────────┐
        │                                                       │
        ▼                                                       ▼
┌───────────────────┐                                 ┌─────────────────┐
│   UI LAYER        │                                 │  REAL-TIME      │
│  (Composables)    │                                 │  (Socket.IO)    │
├───────────────────┤                                 ├─────────────────┤
│ • LoginScreen     │                                 │ • Chat          │
│ • HomeScreen      │                                 │ • Video Calls   │
│ • BookingScreen   │                                 │ • Notifications │
│ • DoctorProfile   │                                 │ • Live Updates  │
│ • ChatScreen      │                                 └─────────────────┘
└───────────────────┘                                         │
        │                                                     │
        │                                                     │
        ▼                                                     │
┌───────────────────┐                                         │
│  VIEWMODEL        │                                         │
│  (State Mgmt)     │                                         │
└───────────────────┘                                         │
        │                                                     │
        │                                                     │
        ▼                                                     │
┌───────────────────────────────────────────────────────┐     │
│              REPOSITORY LAYER                          │     │
│         (ReliefNetRepository.kt)                       │     │
│                                                        │     │
│  • Business Logic                                     │     │
│  • Data Transformation                                │     │
│  • Error Handling                                     │     │
└───────────────────────────────────────────────────────┘     │
        │                                                     │
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────┐                   │
│         NETWORK LAYER                   │                   │
├─────────────────────────────────────────┤                   │
│  • RetrofitClient (HTTP)                │                   │
│  • SocketManager (WebSocket) ◄──────────┘                   │
│  • ApiService (Endpoints)               │                   
│  • ApiConfig (URLs)                     │                   
└─────────────────────────────────────────┘                   
        │                                                     
        │ HTTP/HTTPS + WebSocket                             
        ▼                                                     
═══════════════════════════════════════════════════════════
        │                    INTERNET                        
        ▼                                                     
═══════════════════════════════════════════════════════════
        │                                                     
        ▼                                                     
┌─────────────────────────────────────────────────────────┐
│              NODE.JS BACKEND SERVER                      │
│              (Express.js + Socket.IO)                    │
│              Port: 5000                                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  REST API Endpoints                                     │
│  ├── /api/register                                      │
│  ├── /api/login                                         │
│  ├── /api/doctors                                       │
│  ├── /api/sessions                                      │
│  └── /api/notifications                                 │
│                                                          │
│  Socket.IO Events                                       │
│  ├── connection                                         │
│  ├── register                                           │
│  ├── call:initiate                                      │
│  ├── call:offer                                         │
│  └── call:answer                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
        │                                                     
        │                                                     
        ▼                                                     
┌─────────────────────────────────────────────────────────┐
│              MONGODB ATLAS                               │
│              (Cloud Database)                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Collections:                                           │
│  ├── users (Patients)                                   │
│  ├── doctors                                            │
│  ├── sessions (Bookings)                                │
│  ├── notifications                                      │
│  └── payments                                           │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow Example: Login

```
1. User enters email/password
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
2. LoginScreen (Composable)
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
3. ViewModel calls Repository
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
4. Repository.loginPatient()
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
5. RetrofitClient sends HTTP POST
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
6. Node.js /api/login endpoint
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
7. MongoDB query for user
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
8. bcrypt password verification
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
9. JWT token generated
   ┗━━━━━━━━━━━━━━━━━━━━━┓
                         ▼
10. Response sent back
    ┗━━━━━━━━━━━━━━━━━━━━┓
                          ▼
11. Repository receives AuthResponse
    ┗━━━━━━━━━━━━━━━━━━━━┓
                          ▼
12. ViewModel updates state
    ┗━━━━━━━━━━━━━━━━━━━━┓
                          ▼
13. UI updates (navigate to home)
```

---

## 🔄 Data Flow Example: Real-time Chat

```
User A sends message
        │
        ▼
SocketManager.emit("message:send")
        │
        ▼
Socket.IO Client ──────►  Node.js Socket.IO Server
                                    │
                                    │
                    ┌───────────────┴─────────────┐
                    │                             │
                    ▼                             ▼
            Save to MongoDB              Find User B's socket
                    │                             │
                    │                             ▼
                    │              socket.emit("message:received")
                    │                             │
                    │                             │
                    │                             ▼
                    │                     User B's Socket.IO Client
                    │                             │
                    │                             ▼
                    │                   User B receives message
                    │                             │
                    └─────────────────────────────┘
```

---

## 📱 File Structure

```
ReliefNet/
│
├── server/                          # Backend (Node.js)
│   ├── server.js                    # Main server file
│   ├── models/
│   │   ├── Doctor.js
│   │   ├── Patient.js
│   │   └── Session.js
│   └── package.json
│
└── Reliefnet-android/               # Frontend (Android)
    └── app/src/main/java/com/sentrive/reliefnet/
        │
        ├── network/                 # 🌐 Network Layer
        │   ├── ApiConfig.kt         # URLs & Endpoints
        │   ├── ApiService.kt        # Retrofit Interface
        │   ├── RetrofitClient.kt    # HTTP Client
        │   ├── SocketManager.kt     # Socket.IO Client
        │   └── models/
        │       └── ApiModels.kt     # Data Classes
        │
        ├── repository/              # 💼 Business Logic
        │   └── ReliefNetRepository.kt
        │
        ├── userInterface/           # 🎨 UI Screens
        │   ├── loginScreen.kt
        │   ├── HomeScreen.kt
        │   ├── BookingScreen.kt
        │   ├── DoctorProfile.kt
        │   └── DoctorChatScreen.kt
        │
        ├── examples/                # 📚 Usage Examples
        │   └── ExampleApiUsage.kt
        │
        └── testing/                 # 🧪 Testing
            └── ApiTestScreen.kt
```

---

## 🔐 Authentication Flow

```
┌─────────────┐
│   Start     │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  User Opens App │
└──────┬──────────┘
       │
       ▼
   ┌─────────────┐
   │ Has Token?  │
   └──┬──────┬───┘
      │      │
     Yes    No
      │      │
      │      ▼
      │  ┌────────────┐
      │  │Login Screen│
      │  └──────┬─────┘
      │         │
      │         ▼
      │  ┌──────────────────┐
      │  │ Enter Credentials│
      │  └──────┬───────────┘
      │         │
      │         ▼
      │  ┌─────────────────────┐
      │  │POST /api/login      │
      │  └──────┬──────────────┘
      │         │
      │         ▼
      │  ┌─────────────────┐
      │  │Server validates │
      │  └──────┬──────────┘
      │         │
      │         ▼
      │  ┌─────────────────┐
      │  │Returns JWT Token│
      │  └──────┬──────────┘
      │         │
      │         ▼
      │  ┌─────────────────┐
      │  │ Save Token      │
      │  │ (SharedPrefs)   │
      │  └──────┬──────────┘
      │         │
      └─────────┘
             │
             ▼
      ┌────────────┐
      │ Home Screen│
      └────────────┘
             │
             ▼
   ┌─────────────────────┐
   │ All API calls use   │
   │ Authorization:      │
   │ Bearer <token>      │
   └─────────────────────┘
```

---

## 🎯 Integration Points

### 1. Login/Register Screens
- Replace mock auth with `ReliefNetRepository`
- Save JWT token
- Navigate on success

### 2. Doctor Listing
- Fetch from `/api/doctors`
- Filter by specialty, location
- Display in LazyColumn

### 3. Booking System
- Create session via `/api/sessions`
- Display user's bookings
- Allow cancellation

### 4. Chat & Calls
- Connect to Socket.IO
- Register user
- Handle real-time events

### 5. Profile Management
- Upload photos via `/api/upload`
- Update user data
- Display in UI

---

**This architecture is production-ready and scalable! 🚀**
