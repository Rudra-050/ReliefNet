# PhonePe Payment Flow - Visual Diagrams

## 1. High-Level Payment Flow

```
┌─────────────────┐
│  Patient App    │
│  (Android)      │
└────────┬────────┘
         │
         │ 1. Select doctor & slot
         │
         ▼
┌─────────────────┐
│   Book & Pay    │
│     Button      │
└────────┬────────┘
         │
         │ 2. POST /api/payments/create-order
         │    {amount: 500, doctorId, date, time}
         │
         ▼
┌─────────────────────────────────────────┐
│          ReliefNet Backend              │
│  ┌─────────────────────────────────┐   │
│  │ Calculate Split:                │   │
│  │ • Doctor: ₹450 (90%)           │   │
│  │ • Platform: ₹50 (10%)          │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ Generate:                        │   │
│  │ • merchantTransactionId          │   │
│  │ • HMAC SHA256 checksum           │   │
│  └─────────────────────────────────┘   │
└────────┬────────────────────────────────┘
         │
         │ 3. Create PhonePe order
         │    POST https://api.phonepe.com/pg/v1/pay
         │
         ▼
┌─────────────────┐
│  PhonePe API    │
│                 │
│  Returns:       │
│  • Payment URL  │
│  • Order ID     │
└────────┬────────┘
         │
         │ 4. Return payment URL
         │    {paymentUrl, merchantTransactionId, ...}
         │
         ▼
┌─────────────────┐
│  Patient App    │
│                 │
│  Opens payment  │
│  URL in browser │
│  or WebView     │
└────────┬────────┘
         │
         │ 5. User completes payment
         │    (UPI / Card / Netbanking)
         │
         ▼
┌─────────────────────────────────────────┐
│         PhonePe Payment Page            │
│  ┌─────────────────────────────────┐   │
│  │ Select Payment Method:           │   │
│  │ • UPI (PhonePe, GPay, Paytm)    │   │
│  │ • Credit/Debit Card              │   │
│  │ • Netbanking                     │   │
│  │ • Wallets                        │   │
│  └─────────────────────────────────┘   │
└────────┬────────────────────────────────┘
         │
         │ 6. Payment processed
         │
         ├──────────────────┬──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
    ┌────────┐      ┌────────────┐    ┌──────────┐
    │SUCCESS │      │  FAILED    │    │CANCELLED │
    └────┬───┘      └─────┬──────┘    └────┬─────┘
         │                │                 │
         │                └─────────┬───────┘
         │                          │
         │ 7a. Webhook              │ 7b. Webhook
         │     (async)              │     (async)
         ▼                          ▼
┌─────────────────────────────────────────┐
│     POST /api/payments/webhook          │
│  ┌─────────────────────────────────┐   │
│  │ 1. Verify signature             │   │
│  │ 2. Decode payload               │   │
│  │ 3. Update payment status        │   │
│  │ 4. If SUCCESS:                  │   │
│  │    • Create/update booking      │   │
│  │    • Mark as confirmed          │   │
│  │    • TODO: Trigger payout       │   │
│  │    • Send notifications         │   │
│  └─────────────────────────────────┘   │
└────────┬────────────────────────────────┘
         │
         │ 8. Browser redirect
         │    POST /api/payments/callback
         │
         ▼
┌─────────────────┐
│  Deep Link      │
│  Redirect       │
│                 │
│ reliefnet://    │
│ payment/        │
│ success         │
└────────┬────────┘
         │
         │ 9. App receives callback
         │
         ▼
┌─────────────────┐
│  Patient App    │
│                 │
│ 10. Verify      │
│     payment     │
└────────┬────────┘
         │
         │ 11. POST /api/payments/check-status
         │     {merchantTransactionId}
         │
         ▼
┌─────────────────┐
│  Backend        │
│  Checks PhonePe │
│  Status API     │
└────────┬────────┘
         │
         │ 12. Return status
         │     {status: "COMPLETED", amount, ...}
         │
         ▼
┌─────────────────┐
│  Patient App    │
│                 │
│ 13. Confirm &   │
│     Create      │
│     Booking     │
└────────┬────────┘
         │
         │ 14. POST /api/payments/confirm
         │     {merchantTransactionId, professionalId, date, time}
         │
         ▼
┌─────────────────────────────────────────┐
│          Backend Processing             │
│  ┌─────────────────────────────────┐   │
│  │ 1. Verify payment COMPLETED     │   │
│  │ 2. Check for duplicates         │   │
│  │ 3. Check slot availability      │   │
│  │ 4. Create booking record        │   │
│  │ 5. Update payment record        │   │
│  │ 6. Send notifications           │   │
│  └─────────────────────────────────┘   │
└────────┬────────────────────────────────┘
         │
         │ 15. Return booking details
         │     {booking, payment}
         │
         ▼
┌─────────────────┐
│  Patient App    │
│                 │
│  Show Success   │
│  Screen with    │
│  Booking        │
│  Details        │
└─────────────────┘
```

---

## 2. Split Payment Breakdown

```
┌────────────────────────────────────────────────┐
│         Patient Pays ₹500                      │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│         PhonePe Payment Gateway                │
│                                                │
│         Receives: ₹500                         │
│         Gateway Fee: ₹7.50 (1.5%)              │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│         ReliefNet Backend                      │
│         Calculates Split                       │
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │  Total Amount: ₹500.00                   │ │
│  │                                          │ │
│  │  Platform Fee (10%): ₹50.00             │ │
│  │  Doctor Amount (90%): ₹450.00           │ │
│  └──────────────────────────────────────────┘ │
└────────────┬────────────────┬──────────────────┘
             │                │
             │                │
    ┌────────▼─────┐   ┌─────▼──────────┐
    │  Platform    │   │  Doctor        │
    │  Account     │   │  Account       │
    │              │   │                │
    │  Gets:       │   │  Gets:         │
    │  ₹50.00      │   │  ₹450.00       │
    │              │   │                │
    │  Minus:      │   │  (Direct       │
    │  Gateway fee │   │   transfer)    │
    │  ₹7.50       │   │                │
    │              │   │                │
    │  Net: ₹42.50 │   │  Net: ₹450.00  │
    └──────────────┘   └────────────────┘
```

### Breakdown Table

| Item | Amount | Percentage |
|------|--------|------------|
| Patient Pays | ₹500.00 | 100% |
| **PhonePe Gateway Fee** | *₹7.50* | *1.5%* |
| Doctor Receives | **₹450.00** | **90%** |
| Platform Gross | **₹50.00** | **10%** |
| Platform Net (after gateway fee) | ₹42.50 | 8.5% |

---

## 3. Webhook vs Callback Flow

```
┌──────────────────────────────────────────────────────────┐
│                 After Payment Completion                 │
└────────────────────────┬─────────────────────────────────┘
                         │
                         │ PhonePe sends 2 signals
                         │
        ┌────────────────┴────────────────┐
        │                                 │
        ▼                                 ▼
┌──────────────────┐            ┌──────────────────┐
│    WEBHOOK       │            │    CALLBACK      │
│  (Server-Server) │            │  (Browser Redir) │
└────────┬─────────┘            └────────┬─────────┘
         │                               │
         │ RELIABLE                      │ USER-FACING
         │ INSTANT                       │ SLOW
         │ SECURE                        │ CAN FAIL
         │                               │
         ▼                               ▼
┌──────────────────┐            ┌──────────────────┐
│  Purpose:        │            │  Purpose:        │
│  • Update DB     │            │  • Show UI       │
│  • Create record │            │  • Feedback      │
│  • Send email    │            │  • Navigation    │
│  • Trigger payout│            │                  │
└──────────────────┘            └──────────────────┘
         │                               │
         │                               │
         ▼                               ▼
┌──────────────────┐            ┌──────────────────┐
│  Processing:     │            │  Processing:     │
│  1. Verify sig   │            │  1. Create deep  │
│  2. Decode JSON  │            │     link         │
│  3. Update DB    │            │  2. Redirect app │
│  4. Return 200   │            │  3. Show screen  │
└──────────────────┘            └──────────────────┘
         │                               │
         │                               │
         └───────────┬───────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   Both Complete       │
         │   Booking Confirmed   │
         │   User Notified       │
         └───────────────────────┘
```

### When Each Fires

| Event | Webhook | Callback |
|-------|---------|----------|
| Payment SUCCESS | ✅ Fires immediately | ✅ After redirect delay |
| Payment FAILED | ✅ Fires immediately | ✅ After redirect delay |
| User closes browser | ✅ Still fires | ❌ May not fire |
| Network issues | ✅ Retries | ❌ Lost |
| Update database | ✅ **Use this** | ❌ Don't use |
| Show UI feedback | ❌ Not for this | ✅ **Use this** |

**Best Practice**: 
- Use **webhook** for all critical operations (DB updates, bookings)
- Use **callback** only for UI feedback and navigation

---

## 4. State Machine - Payment Status

```
┌───────────────┐
│   PENDING     │ ← Initial state when order created
└───────┬───────┘
        │
        │ User completes payment
        │
        ├─────────────┬─────────────┬──────────────┐
        │             │             │              │
        ▼             ▼             ▼              ▼
┌───────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  SUCCESS  │  │  FAILED  │  │CANCELLED │  │ TIMEOUT  │
└─────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
      │             │              │             │
      │             │              │             │
      ▼             ▼              ▼             ▼
┌───────────────────────────────────────────────────────┐
│              What Happens Next                        │
├───────────────┬───────────────┬─────────────┬─────────┤
│ SUCCESS:      │ FAILED:       │ CANCELLED:  │TIMEOUT: │
│ • Create      │ • No booking  │ • No        │• Retry  │
│   booking     │ • Show error  │   booking   │• Or     │
│ • Confirm     │ • Offer retry │ • Refund if │  fail   │
│   slot        │               │   debited   │         │
│ • Notify      │               │             │         │
│   doctor      │               │             │         │
│ • Send email  │               │             │         │
│ • Trigger     │               │             │         │
│   payout      │               │             │         │
└───────────────┴───────────────┴─────────────┴─────────┘
```

---

## 5. Security Flow - Checksum Verification

```
┌────────────────────────────────────────────────────────┐
│         PhonePe API Request (from Backend)             │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 1: Prepare data
                         ▼
┌────────────────────────────────────────────────────────┐
│  Payment Data (JSON):                                  │
│  {                                                     │
│    merchantId: "M123",                                │
│    merchantTransactionId: "TXN_123",                  │
│    amount: 50000,  // in paise                        │
│    ...                                                │
│  }                                                     │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 2: Convert to Base64
                         ▼
┌────────────────────────────────────────────────────────┐
│  Base64 Payload:                                       │
│  "eyJtZXJjaGFudElkIjoiTTEyMyIsIm1lcmNoYW50VHJhbnN..." │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 3: Create checksum string
                         ▼
┌────────────────────────────────────────────────────────┐
│  String to Hash:                                       │
│  base64Payload + endpoint + SALT_KEY                  │
│                                                        │
│  Example:                                              │
│  "eyJtZXJjaGF...+/pg/v1/pay+YOUR_SALT_KEY_HERE"     │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 4: Generate SHA256 hash
                         ▼
┌────────────────────────────────────────────────────────┐
│  SHA256 Hash:                                          │
│  "a1b2c3d4e5f6...xyz789"                              │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 5: Append salt index
                         ▼
┌────────────────────────────────────────────────────────┐
│  Final Checksum:                                       │
│  "a1b2c3d4e5f6...xyz789###1"                          │
└────────────────────────┬───────────────────────────────┘
                         │
                         │ Step 6: Send to PhonePe
                         ▼
┌────────────────────────────────────────────────────────┐
│  HTTP Request:                                         │
│  POST https://api.phonepe.com/pg/v1/pay               │
│  Headers:                                              │
│    Content-Type: application/json                     │
│    X-VERIFY: a1b2c3d4e5f6...xyz789###1               │
│  Body:                                                 │
│    { "request": "eyJtZXJjaGF..." }                    │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────┐
│              PhonePe Verifies Checksum                 │
│                                                        │
│  1. Extract base64 payload from request                │
│  2. Extract checksum from X-VERIFY header              │
│  3. Reconstruct string:                                │
│     payload + endpoint + THEIR_COPY_OF_SALT_KEY       │
│  4. Generate SHA256 hash                               │
│  5. Compare with received checksum                     │
│  6. If match → Process payment ✅                      │
│     If not match → Reject (401) ❌                     │
└────────────────────────────────────────────────────────┘
```

### Same Process for Webhook Verification (Reverse)

```
PhonePe sends webhook:
├── Header: X-VERIFY: "hash###1"
├── Body: { response: "base64_encoded_data" }
│
Backend verifies:
├── Extract base64 response from body
├── Decode to get transaction data
├── Reconstruct: response + SALT_KEY
├── Generate SHA256 hash
├── Compare with X-VERIFY hash
└── If match → Trust data ✅
    If not → Reject (suspicious) ❌
```

---

## 6. Error Handling Flow

```
┌────────────────────────────────────────────────────────┐
│              Payment Request Initiated                 │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼
                 ┌───────────────┐
                 │  Try Payment  │
                 └───────┬───────┘
                         │
            ┌────────────┼────────────┐
            │            │            │
            ▼            ▼            ▼
    ┌───────────┐ ┌──────────┐ ┌──────────┐
    │ SUCCESS   │ │  ERROR   │ │ TIMEOUT  │
    └─────┬─────┘ └────┬─────┘ └────┬─────┘
          │            │             │
          │            ▼             │
          │    ┌──────────────┐     │
          │    │ Error Type?  │     │
          │    └──────┬───────┘     │
          │           │             │
          │    ┌──────┴──────┬──────┼──────┬──────────┐
          │    │             │      │      │          │
          │    ▼             ▼      ▼      ▼          ▼
          │ ┌─────┐  ┌─────────┐ ┌────┐ ┌────┐  ┌──────┐
          │ │400  │  │401/403  │ │500 │ │503 │  │Other │
          │ │Bad  │  │Auth     │ │Int │ │Down│  │Error │
          │ │Req  │  │Failed   │ │Err │ │    │  │      │
          │ └──┬──┘  └────┬────┘ └─┬──┘ └─┬──┘  └───┬──┘
          │    │          │        │      │         │
          │    ▼          ▼        ▼      ▼         ▼
          │ ┌────────────────────────────────────────────┐
          │ │          Error Handling                    │
          │ ├────────────────────────────────────────────┤
          │ │ 400: Show "Invalid amount/details"         │
          │ │      → Fix and retry                       │
          │ │                                            │
          │ │ 401: Show "Authentication failed"          │
          │ │      → Re-login                            │
          │ │                                            │
          │ │ 500: Show "Server error"                   │
          │ │      → Retry after 5 seconds               │
          │ │                                            │
          │ │ 503: Show "Service unavailable"            │
          │ │      → Retry with backoff                  │
          │ │                                            │
          │ │ Timeout: Show "Request timeout"            │
          │ │      → Check status & retry                │
          │ │                                            │
          │ │ Other: Show "Unknown error"                │
          │ │      → Contact support                     │
          │ └────────────────────────────────────────────┘
          │
          ▼
   ┌──────────────────┐
   │  Proceed with    │
   │  Booking         │
   └──────────────────┘
```

---

## 7. Timeline - Typical Payment Journey

```
Time    Event
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

T+0s    User clicks "Book & Pay"
        │
T+0.5s  App calls create-order API
        │
T+1s    Backend generates checksum
        │
T+1.5s  Backend calls PhonePe API
        │
T+2s    PhonePe returns payment URL
        │
T+2.5s  App opens payment page
        │
        ┌──── User fills payment details ────┐
        │                                     │
T+20s   │  User enters UPI PIN / Card OTP    │
        │                                     │
        └─────────────────────────────────────┘
        │
T+22s   PhonePe processes payment
        │
T+25s   ┌─── PhonePe sends WEBHOOK (async) ─┐
        │                                     │
        │   Backend receives webhook          │
        │   └─→ Verifies signature            │
        │   └─→ Updates payment status        │
        │   └─→ Creates booking               │
        │   └─→ Sends notifications           │
        │                                     │
T+26s   └─────────────────────────────────────┘
        │
T+27s   PhonePe redirects browser
        │
T+28s   App receives deep link callback
        │
T+29s   App calls check-status API
        │
T+30s   Backend returns COMPLETED status
        │
T+31s   App calls confirm API
        │
T+32s   Backend verifies & returns booking
        │
T+33s   App shows success screen
        │
        ✅ DONE - Total time: 33 seconds
```

### Parallel Processing (Webhook vs UI)

```
T+25s: Payment Success
       │
       ├────────────────────────┬─────────────────────────┐
       │                        │                         │
       ▼ WEBHOOK                ▼ CALLBACK                ▼ USER
   Server-to-Server         Browser Redirect          Still on
   (Reliable)               (User-facing)             Payment Page
       │                        │                         │
       ├─→ Verify signature     │                         │
       ├─→ Update DB            │                         │
       ├─→ Create booking       │                         │
       ├─→ Return 200           ├─→ Create deep link      │
       │                        ├─→ Redirect browser      │
       │                        │                         ├─→ See success
       │                        │                         │   message
       │                        │                         │
       └────────────────────────┴─────────────────────────┘
                                │
                            T+28s: All processes complete
                            Booking confirmed in DB
                            User sees success screen
```

---

## 8. Production Deployment Checklist Diagram

```
┌─────────────────────────────────────────────────────────┐
│         Production Deployment Phases                    │
└────────────────────────┬────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   PHASE 1    │ │   PHASE 2    │ │   PHASE 3    │
│   Setup      │ │   Testing    │ │   Launch     │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼

┌──────────────────────────────────────────────────────┐
│ PHASE 1: Setup (Before Launch)                      │
├──────────────────────────────────────────────────────┤
│ ☐ Get PhonePe merchant account                      │
│   ├─→ Sign up on business.phonepe.com               │
│   ├─→ Complete KYC (PAN, business docs)             │
│   ├─→ Wait for approval (2-3 days)                  │
│   └─→ Get production credentials                    │
│                                                      │
│ ☐ Update environment variables                      │
│   ├─→ PHONEPE_ENV=production                        │
│   ├─→ PHONEPE_MERCHANT_ID=<prod_merchant_id>        │
│   ├─→ PHONEPE_SALT_KEY=<prod_salt_key>              │
│   └─→ APP_BASE_URL=https://api.reliefnet.com        │
│                                                      │
│ ☐ Configure production URLs                         │
│   ├─→ Add webhook: https://api.reliefnet.com/...    │
│   ├─→ Add callback: https://api.reliefnet.com/...   │
│   └─→ Add redirect URL in PhonePe dashboard         │
│                                                      │
│ ☐ Implement doctor payout system                    │
│   ├─→ Create DoctorPayout model                     │
│   ├─→ Collect doctor bank details                   │
│   ├─→ Implement payout API calls                    │
│   └─→ Test payout in sandbox                        │
│                                                      │
│ ☐ Setup monitoring & alerts                         │
│   ├─→ Payment success/failure logging               │
│   ├─→ Webhook failure alerts                        │
│   ├─→ Error tracking (Sentry/similar)               │
│   └─→ Transaction monitoring dashboard              │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ PHASE 2: Testing (Staging Environment)              │
├──────────────────────────────────────────────────────┤
│ ☐ Test payment flow with ₹1                         │
│   ├─→ Create order                                  │
│   ├─→ Complete payment                              │
│   ├─→ Verify webhook received                       │
│   ├─→ Verify booking created                        │
│   └─→ Verify notifications sent                     │
│                                                      │
│ ☐ Test all payment methods                          │
│   ├─→ UPI (PhonePe, GPay, Paytm, etc.)             │
│   ├─→ Credit cards (Visa, Mastercard, Rupay)       │
│   ├─→ Debit cards                                   │
│   └─→ Netbanking (major banks)                      │
│                                                      │
│ ☐ Test error scenarios                              │
│   ├─→ Payment failure                               │
│   ├─→ Payment timeout                               │
│   ├─→ User cancellation                             │
│   ├─→ Duplicate transaction attempt                 │
│   ├─→ Network interruption                          │
│   └─→ Server downtime                               │
│                                                      │
│ ☐ Test split payment accuracy                       │
│   ├─→ Verify doctor amount (90%)                    │
│   ├─→ Verify platform fee (10%)                     │
│   ├─→ Test rounding (₹499, ₹1, etc.)               │
│   └─→ Verify payout amounts                         │
│                                                      │
│ ☐ Load testing                                      │
│   ├─→ 10 concurrent payments                        │
│   ├─→ 50 concurrent payments                        │
│   └─→ Measure response times                        │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ PHASE 3: Launch (Production)                        │
├──────────────────────────────────────────────────────┤
│ ☐ Soft launch (limited users)                       │
│   ├─→ Enable for 10-20 beta users                   │
│   ├─→ Monitor for 1 week                            │
│   ├─→ Collect feedback                              │
│   └─→ Fix any issues                                │
│                                                      │
│ ☐ Full launch                                       │
│   ├─→ Enable for all users                          │
│   ├─→ Monitor success rate (target: >95%)           │
│   ├─→ Monitor average completion time               │
│   └─→ Track conversion rate                         │
│                                                      │
│ ☐ Post-launch monitoring (first month)              │
│   ├─→ Daily transaction volume                      │
│   ├─→ Payment success rate                          │
│   ├─→ Average transaction value                     │
│   ├─→ Doctor payout success rate                    │
│   ├─→ User complaints/issues                        │
│   └─→ Gateway fees vs projection                    │
│                                                      │
│ ☐ Optimization (ongoing)                            │
│   ├─→ Reduce average completion time                │
│   ├─→ Improve success rate                          │
│   ├─→ Add more payment methods                      │
│   └─→ Implement retry for failed payments           │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│             Success Metrics                          │
├──────────────────────────────────────────────────────┤
│ Target Values:                                       │
│ • Payment success rate: >95%                         │
│ • Average completion time: <35 seconds               │
│ • Webhook delivery: >99%                             │
│ • Doctor payout success: >98%                        │
│ • User satisfaction: >4.5/5                          │
└──────────────────────────────────────────────────────┘
```

---

**End of Payment Flow Diagrams**

These diagrams provide a visual understanding of:
1. Complete payment flow from user to booking
2. Split payment architecture
3. Webhook vs callback differences
4. Payment status state machine
5. Security checksum flow
6. Error handling
7. Typical timeline
8. Production deployment checklist

Use these diagrams as reference when implementing or debugging the payment system.
