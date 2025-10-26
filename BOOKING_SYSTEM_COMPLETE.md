# 📅 Appointment Booking & Reminder System - COMPLETE

## ✅ What's Implemented

### **Backend (Node.js + MongoDB)**

#### 1. **Database Models**

##### Booking Model (`/models/Booking.js`)
```javascript
{
  // Patient info
  patientId, patientName, patientEmail, patientPhone,
  
  // Doctor info
  doctorId, doctorName, doctorEmail,
  
  // Appointment details
  appointmentDate, appointmentTime, duration, sessionType,
  
  // Status tracking
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed' | 'no-show',
  paymentStatus: 'pending' | 'paid' | 'refunded' | 'failed',
  
  // Reminders
  reminderSent24h: Boolean,
  reminderSent1h: Boolean,
  
  // Other fields
  reasonForVisit, symptoms, notes, doctorNotes, amount, currency
}
```

##### DoctorAvailability Model (`/models/DoctorAvailability.js`)
```javascript
{
  doctorId, doctorName,
  date, dayOfWeek,
  
  // Time slots array
  slots: [{
    startTime: "09:00",
    endTime: "10:00",
    isBooked: false,
    bookingId: ObjectId or null
  }],
  
  slotDuration: 60, // minutes
  isAvailable: true,
  notes
}
```

---

#### 2. **Calendar Availability Endpoints**

##### **POST** `/api/doctor/availability`
Set doctor's availability for a specific date

**Request:**
```json
{
  "doctorId": "6754abc...",
  "date": "2025-11-15",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00" },
    { "startTime": "10:00", "endTime": "11:00" },
    { "startTime": "14:00", "endTime": "15:00" }
  ],
  "slotDuration": 60,
  "notes": "Morning and afternoon slots"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Availability set successfully",
  "availability": { ... }
}
```

---

##### **GET** `/api/doctor/:doctorId/availability?startDate=2025-11-01&endDate=2025-11-30`
Get doctor's availability for a date range

**Response:**
```json
{
  "success": true,
  "availability": [
    {
      "doctorId": "...",
      "date": "2025-11-15",
      "slots": [ ... ],
      "slotDuration": 60
    }
  ]
}
```

---

##### **GET** `/api/doctor/:doctorId/available-slots?date=2025-11-15`
Get only AVAILABLE (not booked) slots for a specific date

**Response:**
```json
{
  "success": true,
  "date": "2025-11-15",
  "availableSlots": [
    { "startTime": "09:00", "endTime": "10:00", "isBooked": false },
    { "startTime": "14:00", "endTime": "15:00", "isBooked": false }
  ],
  "slotDuration": 60
}
```

---

#### 3. **Booking Endpoints**

##### **POST** `/api/bookings`
Create a new booking (requires authentication)

**Request:**
```json
{
  "patientId": "...",
  "patientName": "John Doe",
  "patientEmail": "john@example.com",
  "patientPhone": "9876543210",
  
  "doctorId": "...",
  "doctorName": "Dr. Smith",
  "doctorEmail": "smith@reliefnet.com",
  
  "appointmentDate": "2025-11-15",
  "appointmentTime": "09:00",
  "duration": 60,
  "sessionType": "consultation",
  "amount": 1000,
  
  "reasonForVisit": "Anxiety management",
  "symptoms": "Feeling anxious recently"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking created successfully",
  "booking": { ... }
}
```

**What Happens:**
1. Checks if slot is still available
2. Creates booking with `status: 'pending'`
3. Marks slot as booked in DoctorAvailability
4. Returns booking details

---

##### **POST** `/api/bookings/:id/confirm`
Confirm a booking (requires authentication)

**Request:**
```json
{
  "paymentId": "razorpay_xyz123",
  "confirmedBy": "patient"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking confirmed successfully",
  "booking": { ... }
}
```

**What Happens:**
1. Updates booking status to `confirmed`
2. Updates payment status to `paid` (if paymentId provided)
3. Sends **confirmation email** to patient
4. Sends **push notification** to patient
5. Returns updated booking

**Confirmation Email Includes:**
- Doctor name
- Date and time
- Duration and session type
- Amount paid
- Important reminders about 24h and 1h notifications

---

##### **POST** `/api/bookings/:id/cancel`
Cancel a booking (requires authentication)

**Request:**
```json
{
  "cancelledBy": "patient",
  "cancellationReason": "Schedule conflict"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "booking": { ... }
}
```

**What Happens:**
1. Updates booking status to `cancelled`
2. Frees up the time slot in DoctorAvailability
3. Sends **cancellation notification** to doctor
4. Returns updated booking

---

##### **GET** `/api/bookings/:id/status`
Get booking status

**Response:**
```json
{
  "success": true,
  "status": "confirmed",
  "paymentStatus": "paid",
  "booking": { ... }
}
```

---

##### **GET** `/api/patient/:patientId/bookings?status=confirmed`
Get patient's bookings

**Response:**
```json
{
  "success": true,
  "bookings": [ ... ]
}
```

**Query Parameters:**
- `status` (optional): Filter by status (pending, confirmed, cancelled, completed)

---

##### **GET** `/api/doctor/:doctorId/bookings?date=2025-11-15&status=confirmed`
Get doctor's bookings

**Response:**
```json
{
  "success": true,
  "bookings": [ ... ]
}
```

**Query Parameters:**
- `status` (optional): Filter by status
- `date` (optional): Filter by specific date

---

##### **GET** `/api/bookings/upcoming`
Get upcoming bookings in next 24 hours (for reminders)

**Response:**
```json
{
  "success": true,
  "bookings": [ ... ]
}
```

---

#### 4. **Reminder Notification System** 🔔

##### **Automated Reminder Job**
Runs every **15 minutes** to check for upcoming appointments

##### **24-Hour Reminder**
Triggered when appointment is 23-25 hours away

**Sends:**
1. **Email** with full appointment details
2. **Push Notification** to patient's device

**Email Content:**
```
🔔 Appointment Reminder

Dear [Patient Name],

This is a friendly reminder about your appointment tomorrow:
- Doctor: [Doctor Name]
- Date: [Date]
- Time: [Time]
- Duration: [Duration] minutes

Please be ready 5 minutes before your scheduled time.

Best regards,
ReliefNet Team
```

**Marks:** `reminderSent24h = true`

---

##### **1-Hour Reminder**
Triggered when appointment is 50-70 minutes away

**Sends:**
- **Push Notification** only (shorter notice)

**Notification:**
```
Title: "Appointment in 1 Hour"
Message: "Your appointment with [Doctor] starts in 1 hour at [Time]. Please be ready!"
```

**Marks:** `reminderSent1h = true`

---

##### **Reminder Job Configuration**
```javascript
// Runs every 15 minutes
setInterval(sendAppointmentReminders, 15 * 60 * 1000);

// Also runs 5 seconds after server startup
setTimeout(sendAppointmentReminders, 5000);
```

---

## 📊 **Complete Booking Flow**

### **Scenario: Patient Books an Appointment**

#### **Step 1: Doctor Sets Availability**
```
Doctor → Sets availability for Nov 15, 2025
POST /api/doctor/availability
{
  "date": "2025-11-15",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00" },
    { "startTime": "10:00", "endTime": "11:00" }
  ]
}
```

#### **Step 2: Patient Views Available Slots**
```
Patient → Checks available slots
GET /api/doctor/DOC123/available-slots?date=2025-11-15

Response:
{
  "availableSlots": [
    { "startTime": "09:00", "endTime": "10:00", "isBooked": false },
    { "startTime": "10:00", "endTime": "11:00", "isBooked": false }
  ]
}
```

#### **Step 3: Patient Creates Booking**
```
Patient → Books 09:00 slot
POST /api/bookings
{
  "appointmentDate": "2025-11-15",
  "appointmentTime": "09:00",
  ...
}

Result:
- Booking created with status: 'pending'
- Slot marked as booked
```

#### **Step 4: Patient Confirms Payment**
```
Patient → Completes payment
POST /api/bookings/BOOKING123/confirm
{
  "paymentId": "razorpay_xyz"
}

Result:
- Status updated to 'confirmed'
- Confirmation email sent ✅
- Push notification sent ✅
```

#### **Step 5: 24h Before Appointment** (Nov 14, 09:00)
```
Reminder Job Runs:
- Finds bookings 23-25 hours away
- Sends email reminder ✅
- Sends push notification ✅
- Marks reminderSent24h = true
```

#### **Step 6: 1h Before Appointment** (Nov 15, 08:00)
```
Reminder Job Runs:
- Finds bookings 50-70 minutes away
- Sends push notification ✅
- Marks reminderSent1h = true
```

#### **Step 7: Appointment Time** (Nov 15, 09:00)
```
Patient & Doctor:
- Join session via app
- Complete consultation
- Doctor updates booking status to 'completed'
```

---

## 🎯 **Android Integration TODO**

### **1. Doctor Availability Screen**
```kotlin
@Composable
fun DoctorAvailabilityScreen() {
  // Calendar to select date
  // Time slot picker (09:00, 10:00, 11:00, etc.)
  // Add/Remove slot buttons
  // Save availability button
  
  // API Call: POST /api/doctor/availability
}
```

### **2. Enhanced Booking Screen**
```kotlin
@Composable
fun BookingScreen1() {
  // Calendar to select date
  // Load available slots from API
  // Display only available (not booked) slots
  // Slot selection UI
  // Booking details form
  
  // API Call: GET /api/doctor/:id/available-slots
  // API Call: POST /api/bookings
}
```

### **3. Booking Confirmation Dialog**
```kotlin
@Composable
fun BookingConfirmationDialog() {
  // Show booking details
  // Payment integration (Razorpay)
  // Confirm button
  
  // API Call: POST /api/bookings/:id/confirm
}
```

### **4. Booking Status Badges**
```kotlin
@Composable
fun BookingCard() {
  // Status badge (Pending, Confirmed, Cancelled)
  // Color coding (Yellow, Green, Red)
  // Cancel button (if pending/confirmed)
  
  // API Call: POST /api/bookings/:id/cancel
}
```

### **5. ApiService.kt Additions**
```kotlin
interface ApiService {
  // Availability
  @POST("api/doctor/availability")
  suspend fun setDoctorAvailability(...)
  
  @GET("api/doctor/{doctorId}/available-slots")
  suspend fun getAvailableSlots(...)
  
  // Bookings
  @POST("api/bookings")
  suspend fun createBooking(...)
  
  @POST("api/bookings/{id}/confirm")
  suspend fun confirmBooking(...)
  
  @POST("api/bookings/{id}/cancel")
  suspend fun cancelBooking(...)
  
  @GET("api/bookings/{id}/status")
  suspend fun getBookingStatus(...)
}
```

---

## 🧪 **Testing Guide**

### **Test 1: Set Doctor Availability**
```powershell
$headers = @{ "Authorization" = "Bearer YOUR_JWT_TOKEN" }
$body = @{
  doctorId = "DOC_ID"
  date = "2025-11-15"
  slots = @(
    @{ startTime = "09:00"; endTime = "10:00" }
    @{ startTime = "10:00"; endTime = "11:00" }
  )
  slotDuration = 60
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/availability" `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body
```

### **Test 2: View Available Slots**
```powershell
Invoke-RestMethod -Uri "http://localhost:5000/api/doctor/DOC_ID/available-slots?date=2025-11-15"
```

### **Test 3: Create Booking**
```powershell
$headers = @{ "Authorization" = "Bearer YOUR_JWT_TOKEN" }
$body = @{
  patientId = "PATIENT_ID"
  patientName = "John Doe"
  patientEmail = "john@example.com"
  doctorId = "DOC_ID"
  doctorName = "Dr. Smith"
  doctorEmail = "smith@reliefnet.com"
  appointmentDate = "2025-11-15"
  appointmentTime = "09:00"
  duration = 60
  sessionType = "consultation"
  amount = 1000
  reasonForVisit = "Anxiety management"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5000/api/bookings" `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body
```

### **Test 4: Confirm Booking**
```powershell
$headers = @{ "Authorization" = "Bearer YOUR_JWT_TOKEN" }
$body = @{
  paymentId = "razorpay_test123"
  confirmedBy = "patient"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5000/api/bookings/BOOKING_ID/confirm" `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body
```

**Expected:**
- Confirmation email received ✅
- Push notification received ✅
- Booking status updated to 'confirmed'

### **Test 5: Check Reminders (Manual)**
```powershell
# Get upcoming bookings
Invoke-RestMethod -Uri "http://localhost:5000/api/bookings/upcoming"

# Check if reminders sent
Invoke-RestMethod -Uri "http://localhost:5000/api/bookings/BOOKING_ID/status"

# Look for:
# reminderSent24h: true
# reminderSent1h: true
```

---

## 📈 **System Features**

### ✅ **Completed Features**
- [x] DoctorAvailability schema with time slots
- [x] Booking schema with status tracking
- [x] Set doctor availability endpoint
- [x] Get availability by date range
- [x] Get available (unbooked) slots only
- [x] Create booking with slot validation
- [x] Confirm booking with email notification
- [x] Cancel booking with slot release
- [x] Get booking status
- [x] Get patient bookings
- [x] Get doctor bookings
- [x] 24-hour email + push reminder
- [x] 1-hour push notification reminder
- [x] Automated reminder cron job (every 15 min)
- [x] Beautiful confirmation email template
- [x] Integration with existing notification system

### 🔲 **Pending (Android)**
- [ ] Doctor availability management UI
- [ ] Calendar view for date selection
- [ ] Available slots display
- [ ] Booking confirmation dialog
- [ ] Status badges (pending/confirmed/cancelled)
- [ ] Cancel booking button
- [ ] Razorpay payment integration
- [ ] ApiService endpoint definitions

---

## 🚀 **Ready for Production**

**Backend is 100% complete and tested!**

- ✅ All endpoints functional
- ✅ Email notifications working
- ✅ Push notifications integrated
- ✅ Reminder system active (15-minute intervals)
- ✅ Database models created
- ✅ Server running on port 5000

**Next Step:** Android UI implementation

---

*Last Updated: October 22, 2025*
*Status: ✅ BACKEND COMPLETE - ANDROID UI PENDING*
