# ReliefNet – App and Flow Summary Guide

This guide summarizes the core systems in the ReliefNet Android app and shows how users move between screens. Each step references the actual route name, composable, and file path so you can jump into code quickly.

- App: Android, Jetpack Compose, Navigation
- Package root: `Reliefnet-android/app/src/main/java/com/sentrive/reliefnet`
- Main navigation: `userInterface` and `navigation/Navigation.kt`

---

## Table of Contents

- Overview and Entry Points
- Patient Flows
  - User type selection / Onboarding
  - Patient login
  - Discover doctors
  - Book appointment (sessions)
  - Payments (PhonePe)
  - Payment status + Booking creation
  - My bookings
  - Patient chat
  - Notifications
  - Profile and personal information
- Doctor Flows
  - Doctor login/registration
  - Dashboard + bottom nav
  - Manage sessions (create/edit)
  - Doctor chats
  - Profile and payout history
  - Help and support
- Services and Wellness
  - Services screen
  - Mental health support (user type)
  - Resources, groups, emergency
- Realtime and Calls
  - Relie chat and patient chat
  - Video/audio calls
- Screen Reference Index (Routes → Composable → File)
- How to build and run
- Related docs

---

## Overview and Entry Points

- Splash → Navigation host
  - Route: `"SplashScreen"`
  - Composable: `SplashScreen`
  - File: `userInterface/SplashScreen.kt` (referenced in `navigation/Navigation.kt`)

- Home (patient homepage)
  - Route: `"Home"`
  - Composable: `HomePage`
  - File: `userInterface/HomeScreen.kt`

- Global Navigation Host
  - File: `navigation/Navigation.kt`
  - Contains all route → composable bindings (patients and doctors)

---

## Patient Flows

### 1) User type selection / Onboarding
- Route: `"UserTypeSelection"`
- Composable: `UserTypeSelectionScreen`
- File: `userInterface/UserTypeSelectionScreen.kt`
- Actions:
  - Patient → navigates to `"LoginScreen"`
  - Professional → navigates to `"ProfessionalLoginScreen"`

### 2) Patient login
- Route: `"LoginScreen"`
- Composable: `LoginScreen`
- File: `userInterface/Login.kt` (imported in `Navigation.kt` as `LoginScreen`)
- Optional auth enhancements (see docs):
  - OTP, Google sign-in, email (SendGrid) – check docs listed below.

### 3) Discover doctors (doctor list)
- Entry paths:
  - Home → Services → Therapy Sessions → `"DiscoverScreen"`
  - Bottom nav → Discover → `"DiscoverScreen"`
  - Mental Health Support → user type card → `"DiscoverScreen"`
- Route: `"DiscoverScreen"`
- Composable: `DiscoverScreen`
- File: `userInterface/DoctorsSortScreen.kt`
- Fetches doctor list via `ReliefNetRepository.getDoctors()` (by specialty), shows categories and filters.

### 4) Book appointment (sessions)
- From doctor card/button:
  - Route: `"booking/{doctorId}"`
  - Composable: `BookingScreen`
  - File: `ui/booking/BookingScreen.kt`
- Behavior:
  - Loads doctor details and the doctor's available sessions via `GET /api/doctor/sessions?doctorId={id}` (only `status=available` slots are shown).
  - Patient selects date and time slot, appointment type (Online/In-Person), and adds symptoms/notes.
  - On proceed, navigates to `PaymentScreen` with all booking context in the route parameters.

Detailed flow (screen-by-screen):
1. `DiscoverScreen` (doctor list) → tap "Book Session" → `booking/{doctorId}`
2. `BookingScreen` shows
   - Doctor summary (name, specialty, price)
   - Date picker and time slots (from sessions API)
   - Inputs: Appointment type, symptoms, notes
   - CTA: "Proceed to Payment"
3. CTA → navigates to `payment-screen/{doctorId}/{date}/{startTime}/{endTime}/{amount}/{appointmentType}/{symptoms}/{notes}`

Backend endpoints involved:
- `GET /api/doctor/sessions?doctorId={id}` → list available sessions
- (After payment) `POST /api/bookings` → creates booking record

Session object (simplified):
```json
{
  "id": "sess_123",
  "doctorId": "doc_456",
  "date": "2025-10-31",
  "startTime": "14:00",
  "endTime": "15:00",
  "status": "available|booked|cancelled"
}
```

Common edge cases handled:
- No sessions available → UI shows a friendly message to try another date/doctor
- Network issues → errors cleaned to human-readable messages (see `DoctorsSortScreen.kt`)
- Token/user not present → `TokenManager` returns empty values; ensure login precedes booking

### 5) Payments (PhonePe)
- Route: `"payment-screen/{doctorId}/{date}/{startTime}/{endTime}/{amount}/{appointmentType}/{symptoms}/{notes}"`
- Composable: `PaymentScreen`
- File: `userInterface/PaymentScreen.kt` (referenced in `Navigation.kt`)
- Initiates PhonePe payment; on completion, deep links to payment status.

Payment flow specifics:
1. `PaymentScreen` renders a summary and a PhonePe pay button
2. Launches PhonePe intent → user completes UPI
3. PhonePe returns to app via deep link → `payment_status/{...}` route

### 6) Payment status + Booking creation
- Route: `"payment_status/{transactionId}/{doctorId}/{date}/{time}/{endTime}/{appointmentType}/{symptoms}/{notes}"`
- Composable: `PaymentStatusScreen`
- File: `ui/booking/PaymentStatusScreen.kt`
- Verifies payment and creates booking via `POST /api/bookings`.
- On success: navigates to `"YourBookings"`.

Booking creation (simplified request):
```json
{
  "patientId": "pat_abc",
  "professionalId": "doc_456",
  "appointmentDate": "2025-10-31",
  "appointmentTime": "14:00",
  "appointmentEndTime": "15:00",
  "appointmentType": "Online Consultation",
  "amount": 500,
  "symptoms": "Anxiety",
  "notes": "First consultation",
  "paymentStatus": "completed"
}
```

States and navigation:
- Success → `YourBookings`
- Failure → pop back to `BookingScreen` or `Home`

### 7) My bookings
- Routes:
  - `"YourBookings"` → `YourBookingsIntegratedScreen` (file: `userInterface/YourBookingsIntegratedScreen.kt`)
  - `"my_bookings"` → `MyBookingsScreen` (file: `ui/booking/MyBookingsScreen.kt`)
- Shows upcoming and past bookings.
 - From here, users can open details, join a call (if implemented), or cancel (if supported by backend).

### 8) Patient chat
- Route: `"PatientChatScreen"`
- Composable: `PatientChatScreen`
- File: `userInterface/PatientChatScreen.kt`
- Real-time chat with backend integration (see chat docs for server routes).

Chat flow (high-level):
1. Conversation is established between patient and doctor (either from booking or ad-hoc chat)
2. Messages are sent/received via the chat backend (websocket/HTTP polling depending on current implementation)
3. Each message includes senderId, receiverId, content, and timestamp
4. Optional: push notifications for new messages

### 9) Notifications
- Route: `"Notifications"`
- Composable: `NotificationsScreen`
- File: `userInterface/Notifications.kt`

### 10) Profile and personal information
- Routes:
  - `"ProfileScreen"` → `ProfileScreen` (`userInterface/Profile.kt`)
  - `"PersonalInformation"` → `PersonalInformationScreen` (`userInterface/ProfilePages.kt`)
  - `"PaymentHistory"` → `PaymentHistoryScreen` (`userInterface/ProfilePages.kt`)

---

## Doctor Flows

### 1) Doctor login/registration
- Routes:
  - `"ProfessionalLoginScreen"` → `ProfessionalLoginScreen` (`userInterface/ProfessionalsLogin.kt`)
  - `"DoctorRegistrationScreen"` → `DoctorRegistrationScreen` (`userInterface/DoctorRegistration.kt`)

### 2) Dashboard + bottom nav (doctor)
- Route: `"DoctorDashboard"`
- Composable: `DoctorDashboardScreen`
- File: `userInterface/DoctorDashboard.kt`
- Bottom nav for doctors is consistent across doctor screens (Home, Chats, Sessions, Profile).

### 3) Manage sessions (create/edit)
- Routes:
  - `"DoctorSessions"` → `DoctorSessionsScreen` (`userInterface/DoctorProfileScreens.kt`)
  - `"DoctorSessionCreation"` → `DoctorSessionCreationScreen` (`userInterface/DoctorProfileScreens.kt`)
  - `"EditSession/{sessionId}"` → `EditSessionScreen` (`userInterface/DoctorProfileScreens.kt`)
  - `"doctor_availability/{doctorId}"` → `DoctorAvailabilityScreen` (`ui/doctor/DoctorAvailabilityScreen.kt`)
- Behavior:
  - Create available slots (status: "available").
  - Patients see these slots on `BookingScreen`.

End-to-end sessions → booking linkage:
1. Doctor creates sessions → `status=available`
2. Patient on `BookingScreen` pulls `GET /api/doctor/sessions?doctorId=...`
3. Patient selects a slot, pays, and app creates booking → the chosen session becomes effectively booked (status should flip to `booked` server-side)
4. Both sides can now see the appointment in their respective views (doctor may see it under sessions or appointments; patient under bookings)

### 4) Doctor chats
- Routes:
  - `"DoctorChats"` → `DoctorChatsListScreen` (`userInterface/DoctorChatsListScreen.kt`)
  - `"DoctorChatScreen"` → `DoctorChatScreen` (`userInterface/DoctorChat.kt`)
 - From `DoctorsSortScreen` doctor cards, the small message icon can navigate to `doctor_chat/{doctorId}` when available.

### 5) Profile and payout history
- Routes:
  - `"DoctorProfile"` → `DoctorProfileScreen` (public profile, patient-facing)
  - `"DoctorAccountProfile"` → `DoctorAccountProfileScreen` (doctor-facing)
  - `"DoctorPayments"` → `DoctorPaymentHistoryScreen`
  - All in `userInterface/DoctorProfileScreens.kt`
 - Doctors can manage personal details, rates, bio, and see payment history summaries.

### 6) Help and support
- Route: `"DoctorHelp"`
- Composable: `DoctorHelpSupportScreen`
- File: `userInterface/DoctorProfileScreens.kt`

---

## Services and Wellness

### Services screen
- Route: `"ServicesScreen"`
- Composable: `ServicesScreen`
- File: `userInterface/ServicesScreen.kt`
- "Therapy Sessions" navigates to `"DiscoverScreen"` (doctor list).
 - Other entries (Chat, Wellness Resources, Support Groups) route to their respective screens for quick access.

### Mental health support
- Route: `"MentalHealthSupport"`
- Composable: `MentalHealthSupport`
- File: `userInterface/UsersTypeCard.kt`
- User type cards navigate to `"DiscoverScreen"`.
 - Cards include: Individual, Child, Teen, Couple. Each now clickable and uses the patient bottom nav.

### Wellness resources, groups, emergency
- `"WellnessResourcesScreen"` → `WellnessResourcesScreen` (`userInterface/WellnessResourcesScreen.kt`)
- `"SupportGroupsScreen"` → `SupportGroupsScreen` (`userInterface/SupportGroupsScreen.kt`)
- `"EmergencyContactScreen"` → `EmergencyContactScreen` (`userInterface/EmergencyContactScreen.kt`)

---

## Realtime and Calls

### Relie chat and patient chat
- `"RelieChat"` → `RelieChat` (`userInterface/RelieChat.kt`)
- `"RelieScreen"` → `RelieScreen` (`userInterface/RelieScreen.kt`)
- `"PatientChatScreen"` → `PatientChatScreen` (`userInterface/PatientChatScreen.kt`)
 - Entry points can be from doctor list, booking details, or dedicated chat menu.

### Video/audio calls
- Route: `"VideoCallScreen/{selfId}/{peerId}/{isCaller}/{callType}"`
- Composable: `VideoCallScreen`
- File: `userInterface/VideoCall.kt`
 - Calls can be initiated from a confirmed booking or an active chat. `callType` supports `video` or `audio`.
 - Parameters:
   - `selfId` = current user id (from `TokenManager`)
   - `peerId` = target user id (doctor/patient)
   - `isCaller` = `true` if initiating the call
   - `callType` = `video|audio`

---

## Screen Reference Index (Routes → Composable → File)

- Home → `HomePage` → `userInterface/HomeScreen.kt`
- UserTypeSelection → `UserTypeSelectionScreen` → `userInterface/UserTypeSelectionScreen.kt`
- LoginScreen → `LoginScreen` → `userInterface/Login.kt`
- ProfessionalLoginScreen → `ProfessionalLoginScreen` → `userInterface/ProfessionalsLogin.kt`
- DiscoverScreen → `DiscoverScreen` → `userInterface/DoctorsSortScreen.kt`
- booking/{doctorId} → `BookingScreen` → `ui/booking/BookingScreen.kt`
- payment-screen/{...} → `PaymentScreen` → `userInterface/PaymentScreen.kt`
- payment_status/{...} → `PaymentStatusScreen` → `ui/booking/PaymentStatusScreen.kt`
- YourBookings → `YourBookingsIntegratedScreen` → `userInterface/YourBookingsIntegratedScreen.kt`
- my_bookings → `MyBookingsScreen` → `ui/booking/MyBookingsScreen.kt`
- Notifications → `NotificationsScreen` → `userInterface/Notifications.kt`
- ProfileScreen → `ProfileScreen` → `userInterface/Profile.kt`
- PersonalInformation → `PersonalInformationScreen` → `userInterface/ProfilePages.kt`
- PaymentHistory → `PaymentHistoryScreen` → `userInterface/ProfilePages.kt`
- ServicesScreen → `ServicesScreen` → `userInterface/ServicesScreen.kt`
- MentalHealthSupport → `MentalHealthSupport` → `userInterface/UsersTypeCard.kt`
- SupportGroupsScreen → `SupportGroupsScreen` → `userInterface/SupportGroupsScreen.kt`
- WellnessResourcesScreen → `WellnessResourcesScreen` → `userInterface/WellnessResourcesScreen.kt`
- EmergencyContactScreen → `EmergencyContactScreen` → `userInterface/EmergencyContactScreen.kt`
- DoctorDashboard → `DoctorDashboardScreen` → `userInterface/DoctorDashboard.kt`
- DoctorChats → `DoctorChatsListScreen` → `userInterface/DoctorChatsListScreen.kt`
- DoctorProfile → `DoctorProfileScreen` → `userInterface/DoctorProfileScreens.kt`
- DoctorAccountProfile → `DoctorAccountProfileScreen` → `userInterface/DoctorProfileScreens.kt`
- DoctorSessions → `DoctorSessionsScreen` → `userInterface/DoctorProfileScreens.kt`
- DoctorPayments → `DoctorPaymentHistoryScreen` → `userInterface/DoctorProfileScreens.kt`
- DoctorHelp → `DoctorHelpSupportScreen` → `userInterface/DoctorProfileScreens.kt`
- DoctorRegistrationScreen → `DoctorRegistrationScreen` → `userInterface/DoctorRegistration.kt`
- DoctorSessionCreation → `DoctorSessionCreationScreen` → `userInterface/DoctorProfileScreens.kt`
- EditSession/{sessionId} → `EditSessionScreen` → `userInterface/DoctorProfileScreens.kt`
- doctor_availability/{doctorId} → `DoctorAvailabilityScreen` → `ui/doctor/DoctorAvailabilityScreen.kt`
- PatientChatScreen → `PatientChatScreen` → `userInterface/PatientChatScreen.kt`
- DoctorChatScreen → `DoctorChatScreen` → `userInterface/DoctorChat.kt`
- RelieChat → `RelieChat` → `userInterface/RelieChat.kt`
- RelieScreen → `RelieScreen` → `userInterface/RelieScreen.kt`
- VideoCallScreen/{...} → `VideoCallScreen` → `userInterface/VideoCall.kt`

---

## How to build and run

```powershell
cd "c:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android"
.\gradlew assembleDebug
```

Install the generated APK on your device/emulator, start the app, and use the flows above.

---

## Related docs (deep dives)

- Booking and sessions
  - `BOOKING_SYSTEM_COMPLETE.md`
  - `HOW_PATIENTS_SEE_SESSIONS.md`
  - `PHONEPE_QUICK_START.md`, `PHONEPE_INTEGRATION.md`, `GOOGLE_PAY_IMPLEMENTATION.md`
- Chat system
  - `CHAT_SYSTEM_IMPLEMENTATION.md`, `REALTIME_CHAT_IMPLEMENTATION_STATUS.md`, `REALTIME_CHAT_COMPLETE.md`
  - `CHAT_BACKEND_IMPLEMENTATION.md`, `CHAT_TESTING_GUIDE.md`
- Doctor pages
  - `DOCTOR_PAGES_IMPLEMENTATION_COMPLETE.md`, `DOCTOR_PAGES_COMPLETE_SUMMARY.md`
  - `DOCTOR_PAGES_QUICK_REFERENCE.md`
- Authentication & Email
  - `OTP_LOGIN_SYSTEM.md`, `OTP_EMAIL_READY.md`, `GMAIL_APP_PASSWORD_TROUBLESHOOTING.md`, `SENDGRID_SETUP_GUIDE.md`
- Deployment/Infra
  - `GOOGLE_CLOUD_RUN_SETUP.md`, `PRODUCTION_HTTPS_SETUP.md`
- Integration and status
  - `FRONTEND_BACKEND_INTEGRATION_SUMMARY.md`, `FINAL_INTEGRATION_SUMMARY.md`, `PROJECT_COMPLETE_SUMMARY.md`

---

If you want, I can add small screen thumbnails or flow diagrams directly in this guide—just share the images or confirm I should extract from the Screenshots folder.
