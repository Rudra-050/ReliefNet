# AndroidManifest.xml Configuration for PhonePe Deep Links

## Required Changes

Add these intent filters to your `MainActivity` in `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Existing permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:name=".ReliefNetApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.ReliefNet">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ReliefNet"
            android:windowSoftInputMode="adjustResize">
            
            <!-- Existing launcher intent filter -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- ========================================== -->
            <!-- ADD THIS SECTION FOR PHONEPE DEEP LINKS   -->
            <!-- ========================================== -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                
                <!-- Payment Success Deep Link -->
                <data
                    android:scheme="reliefnet"
                    android:host="payment"
                    android:pathPrefix="/success" />
                
                <!-- Payment Failed Deep Link -->
                <data
                    android:scheme="reliefnet"
                    android:host="payment"
                    android:pathPrefix="/failed" />
                
                <!-- Payment Error Deep Link -->
                <data
                    android:scheme="reliefnet"
                    android:host="payment"
                    android:pathPrefix="/error" />
            </intent-filter>
            <!-- ========================================== -->
            
        </activity>
        
        <!-- Other activities -->
        
    </application>
    
</manifest>
```

## Deep Link URL Format

The PhonePe payment callback will use these URLs:

### Success
```
reliefnet://payment/success?transactionId=TX123&doctorId=DOC123&date=2024-06-15&time=10:00
```

### Failed
```
reliefnet://payment/failed?transactionId=TX123&reason=User%20cancelled
```

### Error
```
reliefnet://payment/error?transactionId=TX123&error=Payment%20timeout
```

## Testing Deep Links

### Test with ADB (Android Debug Bridge)

```bash
# Test success callback
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/success?transactionId=TX12345&doctorId=DOC123&date=2024-06-15&time=10:00"

# Test failed callback
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/failed?transactionId=TX12345&reason=Cancelled"

# Test error callback
adb shell am start -W -a android.intent.action.VIEW \
  -d "reliefnet://payment/error?transactionId=TX12345&error=Timeout"
```

### Test in Browser (after installing app)

1. Open browser on your Android device
2. Type in address bar: `reliefnet://payment/success?transactionId=TEST123&doctorId=DOC123&date=2024-06-15&time=10:00`
3. Press Enter
4. Android should ask to open ReliefNet app
5. App should navigate to PaymentStatusScreen

## MainActivity Deep Link Handling

Add this code to your MainActivity.kt:

```kotlin
package com.sentrive.reliefnet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            navController = rememberNavController()
            
            // Your existing UI setup
            ReliefNetApp(navController = navController)
            
            // Handle initial deep link
            LaunchedEffect(Unit) {
                intent?.data?.let { uri ->
                    handleDeepLink(uri)
                }
            }
        }
    }
    
    // Handle deep links when app is already running
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        intent?.data?.let { uri ->
            handleDeepLink(uri)
        }
    }
    
    private fun handleDeepLink(uri: Uri) {
        when (uri.host) {
            "payment" -> handlePaymentDeepLink(uri)
            // Add other deep link handlers here
        }
    }
    
    private fun handlePaymentDeepLink(uri: Uri) {
        val path = uri.path
        val transactionId = uri.getQueryParameter("transactionId") ?: return
        val doctorId = uri.getQueryParameter("doctorId") ?: return
        val date = uri.getQueryParameter("date") ?: return
        val time = uri.getQueryParameter("time") ?: return
        
        when (path) {
            "/success", "/failed", "/error" -> {
                // Navigate to payment status screen
                navController.navigate(
                    "payment_status/$transactionId/$doctorId/$date/$time"
                ) {
                    // Clear back stack up to home
                    popUpTo("home") { 
                        inclusive = false 
                    }
                }
            }
        }
    }
}
```

## Verification Steps

After adding the intent filters:

1. **Build and Install App**
   ```bash
   ./gradlew installDebug
   ```

2. **Verify Manifest**
   ```bash
   # Check if deep links are registered
   adb shell dumpsys package com.sentrive.reliefnet | grep -A 10 "Deep Links"
   ```

3. **Test Deep Link**
   ```bash
   adb shell am start -W -a android.intent.action.VIEW \
     -d "reliefnet://payment/success?transactionId=TEST&doctorId=DOC1&date=2024-06-15&time=10:00"
   ```

4. **Check Logs**
   ```bash
   adb logcat | grep -i "reliefnet"
   ```

## Common Issues

### Issue 1: Deep link not opening app
**Solution**: Make sure `android:exported="true"` is set on MainActivity

### Issue 2: App opens but doesn't navigate
**Solution**: Check handleDeepLink() implementation in MainActivity

### Issue 3: Query parameters are null
**Solution**: Verify PhonePe callback URL includes all required parameters

### Issue 4: Multiple instances of app open
**Solution**: Add `android:launchMode="singleTask"` to MainActivity

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    ...>
```

## Backend Configuration

Make sure your backend returns the correct deep link URLs in the payment response:

```javascript
// In server.js - PhonePe create order endpoint
const callbackUrl = 'https://your-backend.com/api/payments/phonepe/callback';
const redirectUrl = `reliefnet://payment/success?transactionId=${merchantTransactionId}&doctorId=${doctorId}&date=${appointmentDate}&time=${appointmentTime}`;
```

## Security Considerations

1. **Validate Parameters**: Always validate deep link parameters in your app
2. **Use HTTPS**: Backend should use HTTPS for callback URLs
3. **Verify Transactions**: Always verify payment status with backend, don't trust deep link alone
4. **Handle Errors**: Properly handle missing or invalid parameters

## Complete Example Flow

1. User clicks "Proceed to Payment" in BookingScreen
2. App creates payment order via API
3. Backend returns payment URL with deep link callback
4. App opens payment URL in browser/PhonePe app
5. User completes payment in PhonePe
6. PhonePe sends webhook to backend (updates DB)
7. PhonePe redirects browser to deep link: `reliefnet://payment/success?...`
8. Android opens ReliefNet app with deep link
9. MainActivity.onNewIntent() receives the deep link
10. handleDeepLink() extracts parameters
11. App navigates to PaymentStatusScreen
12. PaymentStatusScreen verifies payment with backend
13. If verified, creates booking and shows success

## Next Steps

1. ✅ Add intent filter to AndroidManifest.xml
2. ✅ Add deep link handling to MainActivity
3. ✅ Test with ADB commands
4. ✅ Test actual payment flow in sandbox
5. ✅ Verify navigation works correctly
6. ✅ Test on physical device
7. ✅ Deploy to production

---

**Last Updated**: June 2024
**Status**: Ready for Implementation
**Estimated Time**: 10 minutes
