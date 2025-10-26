package com.sentrive.reliefnet.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PhonePe Payment Helper
 * 
 * Handles PhonePe payment integration with browser-based payment flow.
 * Since we're using the payment URL approach, we don't need the PhonePe SDK.
 */
class PhonePeHelper(private val activity: ComponentActivity) {
    
    companion object {
        const val PHONEPE_PACKAGE = "com.phonepe.app"
    }
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    
    /**
     * Check if PhonePe app is installed
     */
    fun isPhonePeInstalled(): Boolean {
        return try {
            activity.packageManager.getPackageInfo(PHONEPE_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Open payment URL in browser or PhonePe app
     * 
     * @param paymentUrl The payment URL received from backend
     * @param merchantTransactionId Transaction ID for tracking
     */
    fun openPaymentUrl(paymentUrl: String, merchantTransactionId: String) {
        try {
            _paymentState.value = PaymentState.Processing(merchantTransactionId)
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            
            // Try to open in PhonePe app if installed
            if (isPhonePeInstalled()) {
                intent.setPackage(PHONEPE_PACKAGE)
            }
            
            activity.startActivity(intent)
        } catch (e: Exception) {
            _paymentState.value = PaymentState.Error("Failed to open payment: ${e.message}")
        }
    }
    
    /**
     * Handle deep link callback from payment
     * Call this from your activity's onNewIntent
     */
    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        
        when {
            uri.toString().contains("payment/success") -> {
                val txnId = uri.getQueryParameter("txnId")
                _paymentState.value = PaymentState.Success(txnId)
            }
            uri.toString().contains("payment/failed") -> {
                val txnId = uri.getQueryParameter("txnId")
                _paymentState.value = PaymentState.Failed(txnId, "Payment failed")
            }
            uri.toString().contains("payment/error") -> {
                _paymentState.value = PaymentState.Error("Payment error occurred")
            }
        }
    }
    
    /**
     * Reset payment state
     */
    fun reset() {
        _paymentState.value = PaymentState.Idle
    }
    
    /**
     * Payment state sealed class
     */
    sealed class PaymentState {
        object Idle : PaymentState()
        data class Processing(val merchantTransactionId: String) : PaymentState()
        data class Success(val merchantTransactionId: String?) : PaymentState()
        data class Failed(val merchantTransactionId: String?, val reason: String) : PaymentState()
        data class Error(val message: String) : PaymentState()
    }
}

/**
 * Extension function to open payment URL easily
 */
fun Context.openPhonePePayment(paymentUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
    startActivity(intent)
}
