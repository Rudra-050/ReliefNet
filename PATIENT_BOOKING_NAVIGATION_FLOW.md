# Patient Booking Navigation Flow Analysis

## Overview
This document analyzes how patients navigate from the HomeScreen to book appointments with doctors in the ReliefNet app.

## Current Navigation Paths

### Path 1: HomeScreen → ServicesScreen → "Therapy Sessions" (BROKEN ❌)

**Flow:**
```
HomeScreen.kt (line 166)
  ↓ Click "Services" button
  → ServicesScreen.kt
    ↓ Click "Therapy Sessions" card (line 47)
    → Navigate to "BookingMain" route
      ❌ ISSUE: "BookingMain" route is NOT defined in Navigation.kt
      ❌ This navigation will fail!
```

**Code References:**

1. **HomeScreen.kt** - Line 166: Navigate to ServicesScreen
```kotlin
.clickable { navHostController.navigate("ServicesScreen") }
```

2. **ServicesScreen.kt** - Line 47: "Therapy Sessions" service
```kotlin
Service(
    name = "Therapy Sessions",
    description = "Book individual or group therapy sessions",
    backgroundColor = Color(0xFF80CBC4),
    imageRes = R.drawable.couple1,
    route = "BookingMain"  // ❌ This route doesn't exist!
)
```

3. **Navigation.kt** - Missing route:
```kotlin
// ❌ NO composable("BookingMain") { ... } definition found!
```

---

### Path 2: HomeScreen → MentalHealthSupport → User Type Selection (INCOMPLETE ⚠️)

**Flow:**
```
HomeScreen.kt (line 344)
  ↓ Click "Mental Health Support" button
  → MentalHealthSupport screen (UsersTypeCard.kt)
    → Shows 4 user type cards:
      - Individual
      - Child
      - Teen
      - Couple
    ⚠️ ISSUE: Cards have NO click functionality
    ⚠️ Should navigate to doctor list but doesn't
```

**Code References:**

1. **HomeScreen.kt** - Line 344:
```kotlin
.clickable { navHostController.navigate("MentalHealthSupport") }
```

2. **UsersTypeCard.kt** - Lines 100-142: UserCard function
```kotlin
@Composable
fun UserCard(userType: UserType, cardWidth : Dp){
    // ... Card UI ...
    // ⚠️ NO .clickable() modifier
    // ⚠️ NO navigation to doctor list
}
```

---

### Path 3: HomeScreen → BookingMain Component (VIEW ONLY - NOT FOR NEW BOOKINGS ℹ️)

**Flow:**
```
HomeScreen.kt (line 192)
  → BookingMain component is displayed (NOT clickable for new bookings)
  → Shows existing patient bookings
    ↓ Click on existing booking (line 464 or 615)
    → Navigate to "BookingScreen" route
      → Shows booking details (NOT for creating new bookings)
```

**Code References:**

1. **HomeScreen.kt** - Line 192: Display existing bookings
```kotlin
BookingMain(279.dp, 269.dp, navHostController)
```

2. **HomeScreen.kt** - Line 535: BookingMain function
```kotlin
@Composable
fun BookingMain(width: Dp, height: Dp, navHostController: NavHostController) {
    // Displays existing patient bookings
    // Fetches from GET /api/bookings?patientId=X
}
```

3. **HomeScreen.kt** - Lines 464, 615: Navigate to booking details
```kotlin
.clickable(onClick = {navHostController.navigate("BookingScreen")})
```

**Note:** This is for VIEWING existing bookings, not creating new ones!

---

## The Working Flow (Hidden/Undiscovered)

### DoctorsSortScreen → BookingScreen (WORKS ✅ but not accessible!)

**Flow:**
```
DoctorsSortScreen.kt
  → Shows list of doctors
  → Fetches from GET /api/doctors API
    ↓ Click "Book Now" button (lines 513, 602)
    → Navigate to "booking/{doctorId}" route
      → Navigation.kt (line 163): composable("booking/{doctorId}")
        → BookingScreen.kt
          → Fetches doctor sessions: GET /api/doctor/sessions?doctorId=X
          → Patient selects time slot
          → Patient enters symptoms, notes
          → Patient proceeds to payment
            → PhonePe payment integration
            → POST /api/bookings (creates booking)
```

**Code References:**

1. **DoctorsSortScreen.kt** - Lines 513, 602:
```kotlin
navHostController.navigate("Booking/$doctorId")
// Note: Route is "Booking" with capital B
```

2. **Navigation.kt** - Line 163: Booking route definition
```kotlin
composable(
    route = "booking/{doctorId}",  // lowercase "booking"
    arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
) { backStackEntry ->
    val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
    // ... BookingScreen implementation ...
}
```

3. **Navigation.kt** - Line 152: Alternative IntegratedBooking route
```kotlin
composable("Booking/{doctorId}") { backStackEntry ->
    val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
    IntegratedBookingScreen(navHostController, doctorId)
}
```

**⚠️ ISSUE:** There's a case-sensitivity mismatch:
- DoctorsSortScreen navigates to `"Booking/$doctorId"` (capital B)
- Main booking route is `"booking/{doctorId}"` (lowercase b)
- But there IS an IntegratedBooking route for capital B at line 152

---

## Critical Issues Found

### 1. Missing Route: "BookingMain" ❌
- **Where:** ServicesScreen → "Therapy Sessions" service
- **Issue:** Route "BookingMain" is not defined in Navigation.kt
- **Impact:** Patients clicking "Therapy Sessions" will get navigation error
- **Fix Needed:** Either:
  - Register "BookingMain" route in Navigation.kt, OR
  - Change ServicesScreen route to a valid one (e.g., doctor list)

### 2. DoctorsSortScreen Not in Navigation.kt ❌
- **Issue:** DoctorsSortScreen.kt exists and has complete booking flow
- **Problem:** This screen is NOT registered in Navigation.kt
- **Impact:** There's no way to navigate TO this screen!
- **Fix Needed:** Add route to Navigation.kt:
  ```kotlin
  composable("DoctorsList") { DoctorsSortScreen(navHostController) }
  ```

### 3. UserCard Has No Click Action ⚠️
- **Where:** MentalHealthSupport screen
- **Issue:** User type cards (Individual, Child, Teen, Couple) are not clickable
- **Impact:** Dead end - patients can't proceed to doctor selection
- **Fix Needed:** Add click handler to navigate to doctor list:
  ```kotlin
  Card(
      modifier = Modifier
          .clickable { navHostController.navigate("DoctorsList") }
          // ... rest of card
  )
  ```

### 4. Route Case Sensitivity Mismatch ⚠️
- **Issue:** DoctorsSortScreen navigates to `"Booking/$doctorId"` (capital B)
- **Available:** Two routes exist:
  - `"booking/{doctorId}"` (lowercase) - Main route at line 163
  - `"Booking/{doctorId}"` (capital) - IntegratedBooking at line 152
- **Impact:** Works by accident (IntegratedBooking catches it)
- **Fix Needed:** Standardize to lowercase for consistency

---

## Recommended Complete Patient Booking Flow

### Proposed Solution 1: Fix ServicesScreen Route

**Change ServicesScreen.kt "Therapy Sessions" to navigate to DoctorsList:**

```kotlin
Service(
    name = "Therapy Sessions",
    description = "Book individual or group therapy sessions",
    backgroundColor = Color(0xFF80CBC4),
    imageRes = R.drawable.couple1,
    route = "DoctorsList"  // ✅ Change from "BookingMain"
)
```

**Add DoctorsList route in Navigation.kt:**

```kotlin
composable("DoctorsList") { 
    DoctorsSortScreen(navHostController) 
}
```

**Complete Flow:**
```
HomeScreen
  ↓ Click "Services"
ServicesScreen
  ↓ Click "Therapy Sessions"
DoctorsSortScreen (Doctor List)
  ↓ Click doctor's "Book Now"
BookingScreen (with doctorId)
  ↓ Select time slot, enter details
PaymentScreen (PhonePe)
  ↓ Payment success
Booking Created ✅
```

---

### Proposed Solution 2: Fix MentalHealthSupport Cards

**Add navigation to UserCard in UsersTypeCard.kt:**

```kotlin
@Composable
fun UserCard(userType: UserType, cardWidth: Dp, navHostController: NavHostController){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(cardWidth)
                .height(139.dp)
                .clickable { 
                    // Navigate to doctor list filtered by user type
                    navHostController.navigate("DoctorsList")
                },  // ✅ ADD THIS
            colors = CardDefaults.cardColors(containerColor = userType.backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            // ... existing card content
        }
    }
}
```

**Complete Flow:**
```
HomeScreen
  ↓ Click "Mental Health Support"
MentalHealthSupport
  ↓ Click user type (Individual/Child/Teen/Couple)
DoctorsSortScreen (Doctor List)
  ↓ Click doctor's "Book Now"
BookingScreen (with doctorId)
  ↓ Select time slot, enter details
PaymentScreen (PhonePe)
  ↓ Payment success
Booking Created ✅
```

---

## API Integration Points

### 1. Fetch Doctors List
- **Endpoint:** `GET /api/doctors`
- **Used in:** DoctorsSortScreen.kt
- **Purpose:** Display available doctors to patients

### 2. Fetch Doctor Sessions
- **Endpoint:** `GET /api/doctor/sessions?doctorId={doctorId}`
- **Used in:** BookingScreen.kt (via BookingViewModel)
- **Purpose:** Show available time slots for selected doctor
- **Returns:** Only sessions with `status: "available"`

### 3. Create Booking
- **Endpoint:** `POST /api/bookings`
- **Used in:** PaymentStatusScreen.kt (after payment success)
- **Purpose:** Create patient booking after payment
- **Request Body:**
```json
{
  "patientId": "string",
  "professionalId": "string",
  "appointmentDate": "YYYY-MM-DD",
  "appointmentTime": "HH:mm",
  "appointmentEndTime": "HH:mm",
  "appointmentType": "Online Consultation|In-Person Visit",
  "status": "pending",
  "paymentStatus": "completed",
  "amount": number,
  "symptoms": "string",
  "notes": "string"
}
```

### 4. Fetch Patient Bookings
- **Endpoint:** `GET /api/bookings?patientId={patientId}`
- **Used in:** HomeScreen.kt (BookingMain component)
- **Purpose:** Display patient's existing bookings

---

## Summary

### What Works ✅
- DoctorsSortScreen has complete doctor list functionality
- BookingScreen successfully shows doctor sessions
- PhonePe payment integration works
- Booking creation API works
- Existing bookings display on HomeScreen

### What's Broken ❌
1. ServicesScreen → "Therapy Sessions" navigates to non-existent "BookingMain" route
2. DoctorsSortScreen is not registered in Navigation.kt (no way to reach it!)
3. MentalHealthSupport user type cards are not clickable
4. No clear path for patients to discover and book doctors

### Immediate Action Required 🔧
1. **Register DoctorsSortScreen in Navigation.kt** as "DoctorsList" route
2. **Fix ServicesScreen** "Therapy Sessions" route to navigate to "DoctorsList"
3. **Make UserCard clickable** in MentalHealthSupport screen
4. **Test complete flow:** HomeScreen → Services → Therapy Sessions → Doctor List → Book Now → Payment → Success

---

## Testing Commands

After fixes are applied, test the complete flow:

```powershell
# Build the app
cd "c:\Users\rudra\OneDrive\Pictures\Documents\OneDrive\Desktop\ReliefNet\Reliefnet-android"
./gradlew assembleDebug

# Manual testing steps:
# 1. Open app → Login as patient
# 2. Click "Services" on HomeScreen
# 3. Click "Therapy Sessions"
# 4. Should see doctor list (DoctorsSortScreen)
# 5. Click "Book Now" on any doctor
# 6. Should see available time slots
# 7. Select slot, enter details, proceed to payment
# 8. Complete payment
# 9. Verify booking created successfully
```

---

## Related Documentation
- [HOW_PATIENTS_SEE_SESSIONS.md](HOW_PATIENTS_SEE_SESSIONS.md) - Doctor session creation flow
- [BOOKING_SYSTEM_COMPLETE.md](BOOKING_SYSTEM_COMPLETE.md) - Booking system implementation
- [PHONEPE_QUICK_START.md](PHONEPE_QUICK_START.md) - Payment integration guide
