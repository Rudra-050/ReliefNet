# 🧪 Testing Guide - PhonePe Payment Integration

## Quick Testing Steps

### 1. Start Backend Server
```bash
cd server
npm start
```

Verify server is running:
```bash
curl http://localhost:3000/api/health
```

### 2. Build and Install Android App
```bash
cd Reliefnet-android
./gradlew installDebug
```

Or in Android Studio: **Run > Run 'app'**

### 3. Test Deep Link Configuration
```bash
# Verify deep links are registered
adb shell dumpsys package com.sentrive.reliefnet | grep -A 20 "Deep Links"

# Test success deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/success?transactionId=TEST123&doctorId=DOC001&date=2024-10-25&time=10:00%20AM"

# Test failed deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/failed?transactionId=TEST123&reason=Cancelled"
```

### 4. Test Complete Booking Flow

#### Step 1: Navigate to Booking Screen
- Open app
- Find a doctor
- Click "Book Appointment"
- Should navigate to BookingScreen

#### Step 2: Select Date and Time
- Scroll through dates (14-day calendar)
- Select a date
- Time slots should load automatically
- Select a time slot

#### Step 3: Fill Booking Details
- Enter reason for visit
- Enter symptoms (optional)
- Review price: ₹500 total

#### Step 4: Proceed to Payment
- Click "Proceed to Payment"
- Confirmation dialog appears
- Click "Confirm Payment"
- Payment order created
- PhonePe payment URL opens in browser

#### Step 5: Complete Payment (Sandbox)
- Use PhonePe sandbox credentials
- Complete payment
- Browser redirects to deep link

#### Step 6: Automatic Verification
- App opens via deep link
- PaymentStatusScreen shows
- "Verifying Payment" indicator
- Payment verified automatically
- "Creating Booking" indicator
- Booking created
- Success screen displayed

#### Step 7: Verify Booking
- Navigate to "My Bookings"
- Booking should appear in "Upcoming" tab
- Details should match

### 5. Test Payment Scenarios

#### Success Scenario
```bash
# Simulate successful payment callback
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/success?transactionId=TX$(date +%s)&doctorId=DOC001&date=2024-10-25&time=14:00"
```

Expected:
- ✅ App opens to PaymentStatusScreen
- ✅ Shows "Verifying Payment"
- ✅ Shows "Payment Successful" with green checkmark
- ✅ Shows "Creating Booking"
- ✅ Shows "Booking Confirmed" with booking details
- ✅ Navigates to My Bookings after 1.5 seconds

#### Failed Scenario
```bash
# Simulate failed payment
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/failed?transactionId=TX$(date +%s)&reason=Insufficient%20Balance"
```

Expected:
- ✅ App opens to PaymentStatusScreen
- ✅ Shows "Payment Failed" with red icon
- ✅ Shows failure reason
- ✅ Shows "Try Again" button
- ✅ Returns to home after 2 seconds

#### Error Scenario
```bash
# Simulate error
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/error?transactionId=TX$(date +%s)&error=Network%20Timeout"
```

Expected:
- ✅ App opens to PaymentStatusScreen
- ✅ Shows "Something Went Wrong" with error icon
- ✅ Shows error message
- ✅ Shows "Go Back" button

### 6. Test My Bookings Screen

#### Navigate to My Bookings
```kotlin
// From any screen
navController.navigate("my_bookings")
```

#### Test Features:
- [ ] Three tabs visible: Upcoming, Completed, Cancelled
- [ ] Switch between tabs
- [ ] Booking cards display correctly
- [ ] Status badges show correct colors
- [ ] Doctor name and specialization visible
- [ ] Date, time, amount displayed
- [ ] "Cancel" button visible for upcoming bookings
- [ ] Cancel confirmation dialog works
- [ ] Empty states show for empty tabs

### 7. Monitor Logs

#### View All App Logs
```bash
adb logcat | grep ReliefNet
```

#### View Deep Link Logs
```bash
adb logcat | grep MainActivity
```

#### View Payment Logs
```bash
adb logcat | grep -E "PhonePe|Payment|Booking"
```

#### View ViewModel Logs
```bash
adb logcat | grep BookingViewModel
```

### 8. Backend Verification

#### Check Payment Order Created
```bash
curl -X POST http://localhost:3000/api/payments/phonepe/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "amount": 500,
    "doctorId": "DOC001",
    "appointmentDate": "2024-10-25",
    "appointmentTime": "10:00 AM"
  }'
```

Expected Response:
```json
{
  "merchantTransactionId": "TXABC123",
  "paymentUrl": "https://phonepe.com/pay?...",
  "amount": 500,
  "doctorAmount": 450,
  "platformFee": 50
}
```

#### Check Payment Status
```bash
curl -X POST http://localhost:3000/api/payments/phonepe/check-status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "merchantTransactionId": "TXABC123"
  }'
```

#### Check Booking Created
```bash
# Query database or use API
curl http://localhost:3000/api/bookings/patient/PATIENT_ID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 9. Database Verification

#### Check Bookings Table
```sql
-- In MongoDB or your database
db.bookings.find({ merchantTransactionId: "TXABC123" })
```

Should contain:
- Patient ID
- Doctor ID
- Appointment date and time
- Payment amount (500)
- Status: "confirmed"
- Transaction ID
- Created timestamp

#### Check Payments Table
```sql
db.payments.find({ merchantTransactionId: "TXABC123" })
```

Should contain:
- Transaction ID
- Amount: 500
- Doctor amount: 450
- Platform fee: 50
- Status: "success"
- Payment method: "phonepe"

### 10. Email Verification

Check if booking confirmation email was sent:
- Subject: "Booking Confirmation"
- Contains: Doctor name, date, time, amount
- Contains: Booking ID
- Contains: Platform fee breakdown

### 11. Push Notification Verification

Check if push notification was sent:
- Title: "Booking Confirmed"
- Body: Doctor name and appointment details
- Should appear on device

---

## Automated Testing Checklist

### Unit Tests
```bash
cd Reliefnet-android
./gradlew test
```

- [ ] ViewModel state transitions
- [ ] Repository function responses
- [ ] Payment helper functionality
- [ ] Data model serialization

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

- [ ] API endpoint calls
- [ ] Payment flow end-to-end
- [ ] Booking creation
- [ ] Cancellation

### UI Tests
```bash
./gradlew connectedDebugAndroidTest
```

- [ ] Date selection
- [ ] Time slot selection
- [ ] Form validation
- [ ] Navigation

---

## Performance Testing

### Load Testing
```bash
# Test concurrent booking requests
for i in {1..10}; do
  curl -X POST http://localhost:3000/api/payments/phonepe/create-order \
    -H "Authorization: Bearer TOKEN" \
    -d '{"amount":500,"doctorId":"DOC001","appointmentDate":"2024-10-25","appointmentTime":"10:00"}' &
done
```

### Response Time Testing
```bash
# Measure API response times
time curl http://localhost:3000/api/payments/phonepe/create-order ...
```

Expected: < 1 second

---

## Troubleshooting

### Issue: Deep link not opening app
**Check**: 
```bash
adb shell dumpsys package com.sentrive.reliefnet | grep "Deep Links"
```
**Fix**: Verify AndroidManifest.xml intent filters

### Issue: Payment verification fails
**Check backend logs**: 
```bash
tail -f server/logs/app.log
```
**Fix**: Ensure webhook URL is accessible

### Issue: Booking not created
**Check**: Payment status in database
**Fix**: Verify payment status is "SUCCESS" before booking

### Issue: Navigation not working
**Check logs**: 
```bash
adb logcat | grep Navigation
```
**Fix**: Verify routes are registered in Navigation.kt

---

## Test Checklist Summary

- [ ] Backend server running
- [ ] Android app installed
- [ ] Deep links registered
- [ ] Deep link test successful
- [ ] Booking flow tested
- [ ] Payment creation tested
- [ ] Payment verification tested
- [ ] Booking creation verified
- [ ] My Bookings screen tested
- [ ] Email notification received
- [ ] Push notification received
- [ ] Database entries verified
- [ ] Error scenarios tested
- [ ] Logs reviewed
- [ ] Performance acceptable

---

## Production Testing

Before going live:

1. **Test with Real PhonePe Account**
   - Use production credentials
   - Test with actual ₹1 payment
   - Verify money is received

2. **Test on Multiple Devices**
   - Different Android versions
   - Different screen sizes
   - Different network conditions

3. **Test Edge Cases**
   - Slow network
   - No network
   - App closed during payment
   - Multiple rapid bookings
   - Simultaneous bookings for same slot

4. **Security Testing**
   - Test with expired auth tokens
   - Test with invalid transaction IDs
   - Test SQL injection attempts
   - Test rate limiting

5. **Monitor in Production**
   - Set up error tracking (Sentry, Firebase Crashlytics)
   - Monitor payment success rate
   - Track booking completion rate
   - Monitor API response times

---

**Ready to test? Follow the steps above and check off each item!** ✅

