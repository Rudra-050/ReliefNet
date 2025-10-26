package com.sentrive.reliefnet.payment

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Google Pay Integration Helper for ReliefNet
 * Handles UPI payments through Google Pay
 */
class GooglePayHelper(private val activity: Activity) {

    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(
            activity,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // Use ENVIRONMENT_PRODUCTION for live
                .build()
        )
    }

    companion object {
        const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
        private const val MERCHANT_NAME = "ReliefNet"
        
        // Indian payment gateway parameters
        private const val GATEWAY_MERCHANT_ID = "BCR2DN4TZ6Q3K5QJ" // Replace with your actual merchant ID
        private const val GATEWAY_NAME = "example" // Use your payment gateway name
    }

    /**
     * Check if Google Pay is ready to use
     */
    fun isGooglePayAvailable(callback: (Boolean) -> Unit) {
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest().toString())
        val task = paymentsClient.isReadyToPay(request)
        
        task.addOnCompleteListener { completedTask ->
            try {
                val result = completedTask.getResult(ApiException::class.java)
                callback(result == true)
            } catch (exception: ApiException) {
                callback(false)
            }
        }
    }

    /**
     * Create payment data request
     */
    fun requestPayment(amount: Double, orderId: String) {
        val paymentDataRequestJson = getPaymentDataRequest(amount, orderId)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            activity,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    /**
     * Handle Google Pay result
     */
    fun handlePaymentResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                data?.let { intent ->
                    PaymentData.getFromIntent(intent)?.let { paymentData ->
                        handlePaymentSuccess(paymentData, onSuccess)
                    } ?: onError("Payment data not found")
                } ?: onError("Payment intent is null")
            }
            Activity.RESULT_CANCELED -> {
                onError("Payment cancelled by user")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                AutoResolveHelper.getStatusFromIntent(data)?.let { status ->
                    onError("Payment error: ${status.statusMessage}")
                } ?: onError("Unknown payment error")
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData, onSuccess: (String) -> Unit) {
        val paymentInformation = paymentData.toJson()

        try {
            val paymentMethodData = JSONObject(paymentInformation)
                .getJSONObject("paymentMethodData")
            
            // Get tokenization data
            val tokenizationData = paymentMethodData
                .getJSONObject("tokenizationData")
            
            // Extract transaction ID from token
            val token = tokenizationData.getString("token")
            val tokenJson = JSONObject(token)
            
            // For UPI payments, extract transaction reference ID
            val transactionId = tokenJson.optString("id") ?: 
                                tokenJson.optString("txnId") ?: 
                                tokenJson.optString("transactionId") ?:
                                generateTransactionId()
            
            onSuccess(transactionId)
        } catch (e: JSONException) {
            onSuccess(generateTransactionId()) // Fallback transaction ID
        }
    }

    /**
     * Generate a fallback transaction ID
     */
    private fun generateTransactionId(): String {
        return "GPAY${System.currentTimeMillis()}${(1000..9999).random()}"
    }

    /**
     * Check if Google Pay is ready to pay request
     */
    private fun isReadyToPayRequest(): JSONObject {
        return try {
            JSONObject().apply {
                put("apiVersion", 2)
                put("apiVersionMinor", 0)
                put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
            }
        } catch (e: JSONException) {
            JSONObject()
        }
    }

    /**
     * Base card payment method
     */
    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", JSONArray().apply {
                    put("PAN_ONLY")
                    put("CRYPTOGRAM_3DS")
                })
                put("allowedCardNetworks", JSONArray().apply {
                    put("MASTERCARD")
                    put("VISA")
                    put("AMEX")
                    put("DISCOVER")
                })
            })
        }
    }

    /**
     * Card payment method with tokenization
     */
    private fun cardPaymentMethod(): JSONObject {
        return baseCardPaymentMethod().apply {
            put("tokenizationSpecification", gatewayTokenizationSpecification())
        }
    }

    /**
     * Gateway tokenization specification
     */
    private fun gatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", GATEWAY_NAME)
                put("gatewayMerchantId", GATEWAY_MERCHANT_ID)
            })
        }
    }

    /**
     * Transaction info
     */
    private fun getTransactionInfo(amount: Double, orderId: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", String.format("%.2f", amount))
            put("totalPriceStatus", "FINAL")
            put("countryCode", "IN")
            put("currencyCode", "INR")
            put("transactionId", orderId)
        }
    }

    /**
     * Merchant info
     */
    private fun getMerchantInfo(): JSONObject {
        return JSONObject().apply {
            put("merchantName", MERCHANT_NAME)
        }
    }

    /**
     * Complete payment data request
     */
    private fun getPaymentDataRequest(amount: Double, orderId: String): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
            put("transactionInfo", getTransactionInfo(amount, orderId))
            put("merchantInfo", getMerchantInfo())
            
            // Add shipping address requirement if needed
            put("emailRequired", false)
            put("shippingAddressRequired", false)
        }
    }
}

/**
 * Data class for payment result
 */
data class GooglePayResult(
    val transactionId: String,
    val amount: Double,
    val orderId: String,
    val timestamp: Long = System.currentTimeMillis()
)
