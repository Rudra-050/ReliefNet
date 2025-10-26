# PhonePe Payment Gateway Integration

## Overview
Complete PhonePe Payment Gateway integration for ReliefNet's marketplace model with automatic doctor payouts and platform commission split (90% to doctor, 10% platform fee).

## Why PhonePe?
- **Lowest Fees**: 1.5-1.8% (vs Razorpay 2%, Cashfree 1.95%)
- **Largest User Base**: 450M+ users in India
- **UPI First**: Best UPI integration in the market
- **Split Payments**: Supports marketplace/vendor payouts
- **Fast Settlement**: T+1 or same-day settlement available

## Backend Implementation (COMPLETED)

### 1. Environment Variables (.env)
Add these to your `.env` file:
```env
# PhonePe Configuration
PHONEPE_MERCHANT_ID=your_merchant_id_here
PHONEPE_SALT_KEY=your_salt_key_here
PHONEPE_SALT_INDEX=1
PHONEPE_ENV=test  # Use 'production' for live

# App Configuration
APP_BASE_URL=http://localhost:5000  # Change for production
```

### 2. Dependencies Added
```javascript
const crypto = require('crypto');  // For signature verification
const axios = require('axios');    // For PhonePe API calls
```

### 3. Payment Model Schema
Updated Payment schema with PhonePe fields:
```javascript
{
  phonepeTransactionId: { type: String, required: true, unique: true },
  phonepeMerchantTransactionId: { type: String, required: true },
  status: { 
    type: String, 
    enum: ['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  paymentMethod: { type: String, default: 'phonepe' },
  doctorAmount: Number,      // 90% of total
  platformFee: Number,       // 10% of total
  paymentInstrument: String, // UPI, CARD, NETBANKING
  responseCode: String
}
```

### 4. API Endpoints Implemented

#### a) Create Payment Order
**Endpoint**: `POST /api/payments/create-order`
**Auth**: Required (JWT)
**Body**:
```json
{
  "amount": 500,
  "doctorId": "doctor_id_here",
  "appointmentDate": "2024-01-20",
  "appointmentTime": "10:00 AM"
}
```
**Response**:
```json
{
  "success": true,
  "merchantTransactionId": "TXN_1705734000_abc12345",
  "amount": 500,
  "doctorAmount": 450,
  "platformFee": 50,
  "paymentUrl": "https://api.phonepe.com/apis/hermes/pg/v1/pay/...",
  "phonepeData": { ... }
}
```

**Flow**:
1. Validates amount and user authentication
2. Retrieves patient and doctor details
3. Generates unique `merchantTransactionId`
4. Calculates split: 90% doctor, 10% platform
5. Creates PhonePe payment payload
6. Generates HMAC SHA256 checksum
7. Calls PhonePe API to initiate payment
8. Returns payment URL for Android app

#### b) Check Payment Status
**Endpoint**: `POST /api/payments/check-status`
**Auth**: Required (JWT)
**Body**:
```json
{
  "merchantTransactionId": "TXN_1705734000_abc12345"
}
```
**Response**:
```json
{
  "success": true,
  "status": "COMPLETED",
  "transactionId": "PHONEPE_TXN_ID",
  "amount": 500,
  "paymentInstrument": "UPI",
  "responseCode": "SUCCESS"
}
```

**Flow**:
1. Takes merchantTransactionId from app
2. Generates checksum for status API
3. Calls PhonePe status check API
4. Returns payment status (COMPLETED/FAILED/PENDING)

#### c) Payment Webhook (Callback)
**Endpoint**: `POST /api/payments/webhook`
**Auth**: None (PhonePe signature verification)
**Body** (from PhonePe):
```json
{
  "response": "base64_encoded_payload",
  "X-VERIFY": "checksum###salt_index"
}
```

**Flow**:
1. Receives webhook from PhonePe
2. Verifies checksum (HMAC SHA256)
3. Decodes base64 response
4. Updates/creates Payment record
5. If payment SUCCESS:
   - Updates booking status to 'confirmed'
   - Marks payment as paid
   - Triggers doctor payout (TODO)
   - Sends notifications

**Security**: Webhook signature is verified to ensure it came from PhonePe.

#### d) Payment Redirect Callback
**Endpoint**: `POST /api/payments/callback`
**Auth**: None
**Body**:
```json
{
  "merchantTransactionId": "TXN_1705734000_abc12345",
  "code": "PAYMENT_SUCCESS"
}
```

**Flow**:
1. Receives redirect after payment
2. Creates deep link for Android app
3. Redirects to `reliefnet://payment/success` or `reliefnet://payment/failed`

#### e) Confirm Payment and Create Booking
**Endpoint**: `POST /api/payments/confirm`
**Auth**: Required (JWT)
**Body**:
```json
{
  "merchantTransactionId": "TXN_1705734000_abc12345",
  "professionalId": "doctor_id",
  "date": "2024-01-20",
  "time": "10:00 AM",
  "duration": 60,
  "type": "consultation",
  "notes": "Patient notes"
}
```
**Response**:
```json
{
  "message": "Payment and booking successful",
  "booking": { ... },
  "payment": {
    "_id": "payment_id",
    "amount": 500,
    "doctorAmount": 450,
    "platformFee": 50,
    "status": "SUCCESS",
    "transactionId": "PHONEPE_TXN_ID"
  }
}
```

**Flow**:
1. Validates input and authentication
2. Checks payment status with PhonePe API
3. Verifies payment is COMPLETED
4. Checks for duplicate transactions
5. Validates no booking conflicts
6. Calculates payment split (90/10)
7. Creates booking record
8. Creates/updates payment record
9. Sends notifications to doctor and patient
10. Returns booking and payment details

### 5. Helper Functions

#### Generate Checksum
```javascript
function generatePhonePeChecksum(payload, endpoint) {
  const string = payload + endpoint + PHONEPE_SALT_KEY;
  const sha256 = crypto.createHash('sha256').update(string).digest('hex');
  return sha256 + '###' + PHONEPE_SALT_INDEX;
}
```

#### Verify Checksum
```javascript
function verifyPhonePeChecksum(checksum, payload) {
  const [receivedChecksum, saltIndex] = checksum.split('###');
  const string = payload + PHONEPE_SALT_KEY;
  const calculatedChecksum = crypto.createHash('sha256').update(string).digest('hex');
  return receivedChecksum === calculatedChecksum;
}
```

## Android Implementation (PENDING)

### 1. Add PhonePe SDK to build.gradle.kts
```kotlin
dependencies {
    // PhonePe Payment Gateway SDK
    implementation("phonepe.intentsdk.android:IntentSDK:2.4.1")
    
    // Required dependencies
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. Update AndroidManifest.xml
```xml
<manifest>
    <queries>
        <!-- PhonePe App -->
        <package android:name="com.phonepe.app" />
    </queries>
    
    <application>
        <!-- Deep link for payment callback -->
        <activity android:name=".PaymentActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="reliefnet"
                    android:host="payment" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 3. Create PhonePeHelper.kt
```kotlin
package com.example.reliefnet.utils

import android.app.Activity
import android.content.Intent
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import org.json.JSONObject

class PhonePeHelper(private val activity: Activity) {
    
    companion object {
        const val REQUEST_CODE_PHONEPE = 1001
        private const val MERCHANT_ID = "YOUR_MERCHANT_ID"
        private const val APP_ID = "YOUR_APP_ID"
    }

    fun initialize(): Boolean {
        return try {
            // Use PhonePeEnvironment.PRODUCTION for live
            PhonePe.init(
                activity.applicationContext,
                PhonePeEnvironment.SANDBOX,
                MERCHANT_ID,
                APP_ID
            )
            true
        } catch (e: PhonePeInitException) {
            e.printStackTrace()
            false
        }
    }

    fun startPayment(
        amount: Long,
        merchantTransactionId: String,
        redirectUrl: String,
        callbackUrl: String
    ) {
        try {
            val paymentRequest = JSONObject().apply {
                put("merchantId", MERCHANT_ID)
                put("merchantTransactionId", merchantTransactionId)
                put("amount", amount)
                put("mobileNumber", "9999999999") // Get from user
                put("callbackUrl", callbackUrl)
                put("redirectUrl", redirectUrl)
            }

            val base64Body = android.util.Base64.encodeToString(
                paymentRequest.toString().toByteArray(),
                android.util.Base64.NO_WRAP
            )

            PhonePe.getImplicitIntent(
                activity,
                base64Body,
                "checksum_from_server" // Get from backend
            )?.let { intent ->
                activity.startActivityForResult(intent, REQUEST_CODE_PHONEPE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handlePaymentResult(requestCode: Int, resultCode: Int, data: Intent?): PaymentResult {
        if (requestCode == REQUEST_CODE_PHONEPE) {
            return when (resultCode) {
                Activity.RESULT_OK -> {
                    val merchantTransactionId = data?.getStringExtra("transactionId")
                    PaymentResult.Success(merchantTransactionId)
                }
                Activity.RESULT_CANCELED -> PaymentResult.Cancelled
                else -> PaymentResult.Failed("Payment failed")
            }
        }
        return PaymentResult.Failed("Invalid request")
    }

    sealed class PaymentResult {
        data class Success(val transactionId: String?) : PaymentResult()
        object Cancelled : PaymentResult()
        data class Failed(val message: String) : PaymentResult()
    }
}
```

### 4. Update ApiModels.kt
```kotlin
// Payment Request Models
data class CreatePaymentOrderRequest(
    val amount: Double,
    val doctorId: String,
    val appointmentDate: String,
    val appointmentTime: String
)

data class CreatePaymentOrderResponse(
    val success: Boolean,
    val merchantTransactionId: String,
    val amount: Double,
    val doctorAmount: Double,
    val platformFee: Double,
    val paymentUrl: String,
    val phonepeData: PhonePeData?
)

data class PhonePeData(
    val instrumentResponse: InstrumentResponse
)

data class InstrumentResponse(
    val redirectInfo: RedirectInfo
)

data class RedirectInfo(
    val url: String
)

data class CheckPaymentStatusRequest(
    val merchantTransactionId: String
)

data class CheckPaymentStatusResponse(
    val success: Boolean,
    val status: String,
    val transactionId: String,
    val amount: Double,
    val paymentInstrument: String?,
    val responseCode: String
)

data class ConfirmPaymentRequest(
    val merchantTransactionId: String,
    val professionalId: String,
    val date: String,
    val time: String,
    val duration: Int = 60,
    val type: String = "consultation",
    val notes: String? = null
)

data class ConfirmPaymentResponse(
    val message: String,
    val booking: BookingItem,
    val payment: PaymentItem
)

data class PaymentItem(
    val _id: String,
    val amount: Double,
    val doctorAmount: Double,
    val platformFee: Double,
    val status: String,
    val transactionId: String
)
```

### 5. Update ApiService.kt
```kotlin
interface ApiService {
    @POST("payments/create-order")
    suspend fun createPaymentOrder(
        @Header("Authorization") token: String,
        @Body request: CreatePaymentOrderRequest
    ): Response<CreatePaymentOrderResponse>

    @POST("payments/check-status")
    suspend fun checkPaymentStatus(
        @Header("Authorization") token: String,
        @Body request: CheckPaymentStatusRequest
    ): Response<CheckPaymentStatusResponse>

    @POST("payments/confirm")
    suspend fun confirmPayment(
        @Header("Authorization") token: String,
        @Body request: ConfirmPaymentRequest
    ): Response<ConfirmPaymentResponse>
}
```

### 6. Usage in BookingActivity/Screen
```kotlin
class BookingActivity : ComponentActivity() {
    private lateinit var phonePeHelper: PhonePeHelper
    private var currentMerchantTxnId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        phonePeHelper = PhonePeHelper(this)
        phonePeHelper.initialize()
        
        setContent {
            BookingScreen(
                onPayClick = { amount, doctorId, date, time ->
                    initiatePayment(amount, doctorId, date, time)
                }
            )
        }
    }

    private fun initiatePayment(
        amount: Double,
        doctorId: String,
        date: String,
        time: String
    ) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createPaymentOrder(
                    token = "Bearer $authToken",
                    request = CreatePaymentOrderRequest(
                        amount = amount,
                        doctorId = doctorId,
                        appointmentDate = date,
                        appointmentTime = time
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val orderData = response.body()!!
                    currentMerchantTxnId = orderData.merchantTransactionId
                    
                    // Open payment URL in browser or WebView
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(orderData.paymentUrl))
                    startActivity(intent)
                    
                    // Alternative: Use PhonePe SDK
                    // phonePeHelper.startPayment(...)
                } else {
                    showError("Failed to create payment order")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        val result = phonePeHelper.handlePaymentResult(requestCode, resultCode, data)
        when (result) {
            is PhonePeHelper.PaymentResult.Success -> {
                verifyAndConfirmPayment(currentMerchantTxnId ?: "")
            }
            is PhonePeHelper.PaymentResult.Cancelled -> {
                showError("Payment cancelled by user")
            }
            is PhonePeHelper.PaymentResult.Failed -> {
                showError(result.message)
            }
        }
    }

    private fun verifyAndConfirmPayment(merchantTxnId: String) {
        lifecycleScope.launch {
            try {
                // Check status first
                val statusResponse = RetrofitClient.apiService.checkPaymentStatus(
                    token = "Bearer $authToken",
                    request = CheckPaymentStatusRequest(merchantTxnId)
                )

                if (statusResponse.body()?.status == "COMPLETED") {
                    // Confirm and create booking
                    val confirmResponse = RetrofitClient.apiService.confirmPayment(
                        token = "Bearer $authToken",
                        request = ConfirmPaymentRequest(
                            merchantTransactionId = merchantTxnId,
                            professionalId = doctorId,
                            date = selectedDate,
                            time = selectedTime
                        )
                    )

                    if (confirmResponse.isSuccessful) {
                        showSuccess("Booking confirmed!")
                        navigateToBookingDetails(confirmResponse.body()?.booking?._id)
                    }
                } else {
                    showError("Payment not completed")
                }
            } catch (e: Exception) {
                showError("Verification error: ${e.message}")
            }
        }
    }
}
```

## Payment Flow

### Complete User Journey
1. **Patient selects doctor and time slot**
2. **Patient clicks "Book Appointment" button**
3. **App calls** `POST /api/payments/create-order`:
   - Backend generates merchantTransactionId
   - Calculates split (₹450 doctor + ₹50 platform from ₹500)
   - Creates PhonePe order with checksum
   - Returns payment URL
4. **App opens payment URL** in browser/WebView or uses PhonePe SDK
5. **Patient completes payment** on PhonePe interface (UPI/Card/Netbanking)
6. **PhonePe sends webhook** to `POST /api/payments/webhook`:
   - Backend verifies checksum
   - Updates payment status to SUCCESS
   - Updates booking status to confirmed
7. **PhonePe redirects browser** to `POST /api/payments/callback`:
   - Backend creates deep link
   - Redirects to app via `reliefnet://payment/success`
8. **App receives callback** via deep link or activity result
9. **App calls** `POST /api/payments/check-status` to verify
10. **App calls** `POST /api/payments/confirm` to create booking
11. **Success screen shown** with booking details

## Split Payment Architecture

### Current Implementation
```
Total Payment: ₹500
├── Doctor Amount: ₹450 (90%)
└── Platform Fee: ₹50 (10%)
```

### Payment Split Calculation
```javascript
const platformFeePercent = 0.10; // 10%
const platformFee = Math.round(amount * platformFeePercent * 100) / 100;
const doctorAmount = amount - platformFee;
```

### Doctor Payout (TODO)
**Not yet implemented. Required for production:**

1. **Collect Doctor Bank Details**:
   - Bank account number
   - IFSC code
   - PAN card number
   - Account holder name

2. **Create DoctorPayout Model**:
```javascript
const DoctorPayoutSchema = new mongoose.Schema({
  doctorId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  paymentId: { type: mongoose.Schema.Types.ObjectId, ref: 'Payment', required: true },
  amount: { type: Number, required: true },
  status: { 
    type: String, 
    enum: ['PENDING', 'SUCCESS', 'FAILED'],
    default: 'PENDING'
  },
  phonepePayoutId: String,
  accountNumber: String,
  ifscCode: String,
  settlementDate: Date,
  createdAt: { type: Date, default: Date.now }
});
```

3. **Implement Payout Function**:
```javascript
async function processDoctorPayout(doctorId, amount, merchantTransactionId) {
  try {
    // Get doctor bank details
    const doctor = await Doctor.findById(doctorId);
    if (!doctor.bankAccount || !doctor.ifscCode) {
      throw new Error('Doctor bank details not found');
    }

    // Create payout request to PhonePe
    const payoutPayload = {
      merchantId: PHONEPE_MERCHANT_ID,
      merchantTransactionId: `PAYOUT_${Date.now()}`,
      amount: Math.round(amount * 100), // Convert to paise
      beneficiaryVPA: doctor.upiId, // Or account details
      // ... other fields
    };

    // Generate checksum and call PhonePe payout API
    // ... implementation

    // Save payout record
    const payout = new DoctorPayout({
      doctorId,
      amount,
      status: 'PENDING'
    });
    await payout.save();

    return payout;
  } catch (error) {
    console.error('Payout failed:', error);
    throw error;
  }
}
```

## Testing

### Test Mode Setup
1. **Sign up** on [PhonePe Business](https://business.phonepe.com/)
2. **Complete KYC** verification
3. **Get Sandbox Credentials**:
   - Merchant ID
   - Salt Key
   - Salt Index
4. **Add to .env** file
5. **Use sandbox URL**: `https://api-preprod.phonepe.com/apis/pg-sandbox`

### Test Cards (Sandbox)
```
Card Number: 4242 4242 4242 4242
CVV: Any 3 digits
Expiry: Any future date
OTP: 123456
```

### Test UPI
```
UPI ID: success@ybl (for successful payment)
UPI ID: failure@ybl (for failed payment)
```

### Webhook Testing
Use **ngrok** to expose local server for webhook testing:
```bash
ngrok http 5000
# Update PHONEPE webhook URL in dashboard with ngrok URL
```

## Security Checklist

### Backend Security
- ✅ Checksum verification on all PhonePe API calls
- ✅ Webhook signature verification
- ✅ Unique transaction ID generation
- ✅ Duplicate payment prevention
- ✅ Amount validation before processing
- ✅ HTTPS only in production
- ✅ Environment variables for sensitive keys
- ✅ Rate limiting on payment endpoints (TODO)

### Android Security
- ⚠️ Store API keys in `local.properties` (not in code)
- ⚠️ Use ProGuard to obfuscate code
- ⚠️ Validate payment response on backend (not just client)
- ⚠️ Handle deep links securely
- ⚠️ Add certificate pinning (TODO)

## Production Checklist

### Before Going Live
1. **Get Production Credentials**:
   - [ ] Complete business KYC on PhonePe
   - [ ] Submit business documents
   - [ ] Get production Merchant ID and Salt Key
   - [ ] Update .env with production credentials

2. **Update Environment**:
   - [ ] Change `PHONEPE_ENV=production` in .env
   - [ ] Update `PHONEPE_HOST_URL` to production URL
   - [ ] Update `APP_BASE_URL` to production domain

3. **Configure Webhooks**:
   - [ ] Add production webhook URL in PhonePe dashboard
   - [ ] Test webhook delivery
   - [ ] Verify webhook signature validation

4. **Doctor Payouts**:
   - [ ] Implement payout function
   - [ ] Collect doctor bank details
   - [ ] Test payout in sandbox
   - [ ] Setup automatic settlement schedule

5. **Testing**:
   - [ ] Test complete payment flow with real cards
   - [ ] Test webhook callbacks
   - [ ] Test payment status check
   - [ ] Test booking creation
   - [ ] Test error scenarios (failed payments, timeouts)

6. **Monitoring**:
   - [ ] Setup payment logging
   - [ ] Setup error alerts
   - [ ] Monitor webhook failures
   - [ ] Track split payment accuracy

## Common Issues & Solutions

### Issue 1: Checksum Mismatch
**Error**: "Invalid checksum" or signature verification failed
**Solution**: 
- Ensure Salt Key matches PhonePe dashboard
- Check Salt Index (usually 1)
- Verify payload encoding (base64)
- Check string concatenation order

### Issue 2: Webhook Not Received
**Error**: Webhook not hitting your server
**Solution**:
- Ensure webhook URL is publicly accessible (use ngrok for local)
- Check firewall settings
- Verify webhook URL in PhonePe dashboard
- Check server logs for rejected requests

### Issue 3: Payment Success but Booking Not Created
**Error**: Payment successful but booking creation fails
**Solution**:
- Check booking validation logic
- Verify doctor availability
- Check database connection
- Review error logs in `/api/payments/confirm`

### Issue 4: Duplicate Payments
**Error**: Same payment processed twice
**Solution**:
- Check unique index on `phonepeMerchantTransactionId`
- Verify duplicate check logic in confirm endpoint
- Use idempotency keys

## Next Steps

### Immediate (Required for MVP)
1. ✅ Backend payment endpoints (DONE)
2. ⚠️ Android PhonePe SDK integration
3. ⚠️ Payment UI screens
4. ⚠️ Test in sandbox environment

### Short Term (Required for Launch)
1. ⚠️ Doctor bank details collection
2. ⚠️ Doctor payout implementation
3. ⚠️ Production credentials setup
4. ⚠️ Complete testing with real transactions

### Long Term (Post Launch)
1. ⚠️ Doctor earnings dashboard
2. ⚠️ Refund handling
3. ⚠️ Failed payment retry mechanism
4. ⚠️ Payment analytics and reporting
5. ⚠️ Multiple payment methods (cards, wallets, etc.)

## Support & Documentation
- **PhonePe Docs**: https://developer.phonepe.com/docs
- **PhonePe Business**: https://business.phonepe.com/
- **PhonePe Support**: support@phonepe.com
- **Android SDK**: https://developer.phonepe.com/docs/android-sdk

---

**Status**: Backend implementation complete ✅ | Android integration pending ⚠️ | Doctor payouts TODO ⚠️

**Last Updated**: 2024-01-18
