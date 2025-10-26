# PhonePe Payment Integration - Quick Reference

## 🚀 Quick Start

### Backend Setup (3 steps)
1. Add to `.env`:
```env
PHONEPE_MERCHANT_ID=your_merchant_id
PHONEPE_SALT_KEY=your_salt_key
PHONEPE_SALT_INDEX=1
PHONEPE_ENV=test
APP_BASE_URL=http://localhost:5000
```

2. Dependencies (already added):
```bash
npm install crypto axios
```

3. Start server:
```bash
cd server
npm start
```

### Android Setup (4 steps)
1. Add to `build.gradle.kts`:
```kotlin
implementation("phonepe.intentsdk.android:IntentSDK:2.4.1")
```

2. Create `PhonePeHelper.kt` (see PHONEPE_INTEGRATION.md)

3. Update `ApiModels.kt` and `ApiService.kt`

4. Integrate in booking UI

---

## 📡 API Endpoints

### 1. Create Payment Order
```http
POST /api/payments/create-order
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 500,
  "doctorId": "6504f1234567890",
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
  "paymentUrl": "https://api.phonepe.com/..."
}
```

### 2. Check Payment Status
```http
POST /api/payments/check-status
Authorization: Bearer <token>
Content-Type: application/json

{
  "merchantTransactionId": "TXN_1705734000_abc12345"
}
```

**Response**:
```json
{
  "success": true,
  "status": "COMPLETED",
  "transactionId": "PHONEPE_TXN_123",
  "amount": 500
}
```

### 3. Confirm Payment & Create Booking
```http
POST /api/payments/confirm
Authorization: Bearer <token>
Content-Type: application/json

{
  "merchantTransactionId": "TXN_1705734000_abc12345",
  "professionalId": "6504f1234567890",
  "date": "2024-01-20",
  "time": "10:00 AM",
  "duration": 60,
  "type": "consultation",
  "notes": "Patient notes"
}
```

---

## 💰 Payment Split

```
Total: ₹500
├── Doctor: ₹450 (90%)
└── Platform: ₹50 (10%)
    ├── Gateway fee: ₹7.50 (1.5%)
    └── Net profit: ₹42.50 (8.5%)
```

---

## 🔄 Payment Flow (Simple)

```
1. User clicks "Book & Pay"
2. App → Backend: create-order
3. Backend → PhonePe: Create order
4. PhonePe → Backend: Payment URL
5. Backend → App: Payment URL
6. App opens payment page
7. User completes payment
8. PhonePe → Backend: Webhook (updates DB)
9. PhonePe → Browser: Redirect
10. Browser → App: Deep link
11. App → Backend: check-status
12. App → Backend: confirm (creates booking)
13. App shows success ✅
```

---

## 🐛 Testing

### Sandbox Credentials
- **URL**: `https://api-preprod.phonepe.com/apis/pg-sandbox`
- **Get from**: PhonePe Business Dashboard

### Test Cards
```
Card: 4242 4242 4242 4242
CVV: 123
Expiry: 12/25
OTP: 123456
```

### Test UPI
```
success@ybl  → Payment SUCCESS
failure@ybl  → Payment FAILED
```

### Test Webhook (ngrok)
```bash
ngrok http 5000
# Update webhook URL in PhonePe dashboard
```

---

## ⚠️ Common Issues

| Issue | Fix |
|-------|-----|
| Checksum mismatch | Verify SALT_KEY, check string concatenation |
| Webhook not received | Use ngrok for local, check firewall |
| No booking created | Check `/api/payments/confirm` logs |
| Duplicate payment | Verify unique index on merchantTransactionId |

---

## 📊 Status Codes

| Status | Action |
|--------|--------|
| `PENDING` | Wait for completion |
| `SUCCESS` / `COMPLETED` | Create booking ✅ |
| `FAILED` | Show error, retry |
| `CANCELLED` | Allow retry |
| `TIMEOUT` | Check status & retry |

---

## 🎯 Next Steps

### Immediate (Android)
- [ ] Add PhonePe SDK
- [ ] Create PhonePeHelper.kt
- [ ] Update API models
- [ ] Test in sandbox

### Before Production
- [ ] Get merchant account
- [ ] Get production credentials
- [ ] Configure webhook URL
- [ ] Implement doctor payouts
- [ ] Test with real money

---

## 📚 Full Documentation

| File | Purpose |
|------|---------|
| `PHONEPE_INTEGRATION.md` | Complete guide (600+ lines) |
| `CONVERSATION_SUMMARY.md` | Project history |
| `PAYMENT_FLOW_DIAGRAM.md` | Visual flows |

---

## 🔗 Links

- **PhonePe Docs**: https://developer.phonepe.com/docs
- **Business Portal**: https://business.phonepe.com/
- **Support**: support@phonepe.com

---

**Status**: Backend ✅ Complete | Android ⚠️ Pending | Payouts ⚠️ TODO
