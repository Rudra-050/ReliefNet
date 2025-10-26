package com.sentrive.reliefnet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingViewModel(private val repository: ReliefNetRepository) : ViewModel() {
    
    companion object {
        private const val TAG = "BookingViewModel"
    }
    
    // ==================== STATE FLOWS ====================
    
    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()
    
    private val _availabilityState = MutableStateFlow<AvailabilityState>(AvailabilityState.Loading)
    val availabilityState: StateFlow<AvailabilityState> = _availabilityState.asStateFlow()
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    
    private val _bookingsList = MutableStateFlow<List<Booking>>(emptyList())
    val bookingsList: StateFlow<List<Booking>> = _bookingsList.asStateFlow()
    
    private val _doctorState = MutableStateFlow<DoctorState>(DoctorState.Loading)
    val doctorState: StateFlow<DoctorState> = _doctorState.asStateFlow()
    
    // ==================== DOCTOR FUNCTIONS ====================
    
    /**
     * Load doctor details by ID
     */
    fun loadDoctorDetails(doctorId: String) {
        viewModelScope.launch {
            try {
                _doctorState.value = DoctorState.Loading
                
                val result = repository.getDoctorById(doctorId)
                
                result.onSuccess { doctor ->
                    _doctorState.value = DoctorState.Success(doctor)
                }.onFailure { e ->
                    _doctorState.value = DoctorState.Error(e.message ?: "Failed to load doctor details")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading doctor details", e)
                _doctorState.value = DoctorState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    // ==================== AVAILABILITY FUNCTIONS ====================
    
    /**
     * Load available time slots for a doctor on a specific date
     */
    fun loadAvailableSlots(doctorId: String, date: String) {
        viewModelScope.launch {
            try {
                _availabilityState.value = AvailabilityState.Loading
                
                val response = repository.getAvailableSlots(doctorId, date)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _availabilityState.value = AvailabilityState.Success(
                            date = body.date,
                            slots = body.availableSlots,
                            slotDuration = body.slotDuration
                        )
                    } else {
                        _availabilityState.value = AvailabilityState.Error("No slots available")
                    }
                } else {
                    _availabilityState.value = AvailabilityState.Error(
                        response.message() ?: "Failed to load slots"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading slots", e)
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Load doctor's availability for a date range
     */
    fun loadDoctorAvailability(doctorId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _availabilityState.value = AvailabilityState.Loading
                
                val response = repository.getDoctorAvailability(doctorId, startDate, endDate)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _availabilityState.value = AvailabilityState.AvailabilityList(
                            availabilities = body.availability
                        )
                    } else {
                        _availabilityState.value = AvailabilityState.Error("No availability found")
                    }
                } else {
                    _availabilityState.value = AvailabilityState.Error(
                        response.message() ?: "Failed to load availability"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading availability", e)
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    // ==================== PAYMENT FUNCTIONS ====================
    
    /**
     * Create PhonePe payment order
     */
    fun createPaymentOrder(
        amount: Double,
        doctorId: String,
        appointmentDate: String,
        appointmentTime: String
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Processing
                
                val request = CreatePaymentOrderPhonePeRequest(
                    amount = amount,
                    doctorId = doctorId,
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime
                )
                
                val response = repository.createPhonePeOrder(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _paymentState.value = PaymentState.OrderCreated(
                            merchantTransactionId = body.merchantTransactionId,
                            paymentUrl = body.paymentUrl,
                            amount = body.amount,
                            doctorAmount = body.doctorAmount,
                            platformFee = body.platformFee
                        )
                    } else {
                        _paymentState.value = PaymentState.Error("Failed to create payment order")
                    }
                } else {
                    _paymentState.value = PaymentState.Error(
                        response.message() ?: "Payment order creation failed"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payment order", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Check payment status
     */
    fun checkPaymentStatus(merchantTransactionId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Verifying
                
                val request = CheckPaymentStatusRequest(merchantTransactionId)
                val response = repository.checkPaymentStatus(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        when (body.status) {
                            "COMPLETED" -> {
                                _paymentState.value = PaymentState.PaymentSuccess(
                                    merchantTransactionId = merchantTransactionId,
                                    transactionId = body.transactionId,
                                    amount = body.amount
                                )
                            }
                            "FAILED" -> {
                                _paymentState.value = PaymentState.PaymentFailed(
                                    "Payment failed: ${body.responseCode}"
                                )
                            }
                            "PENDING" -> {
                                _paymentState.value = PaymentState.Verifying
                            }
                            else -> {
                                _paymentState.value = PaymentState.Error("Unknown payment status")
                            }
                        }
                    } else {
                        _paymentState.value = PaymentState.Error("Failed to check payment status")
                    }
                } else {
                    _paymentState.value = PaymentState.Error(
                        response.message() ?: "Status check failed"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking payment status", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Confirm payment and create booking
     */
    fun confirmPaymentAndBooking(
        merchantTransactionId: String,
        professionalId: String,
        date: String,
        time: String,
        duration: Int = 60,
        type: String = "consultation",
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                _bookingState.value = BookingState.Creating
                
                val request = ConfirmPaymentRequest(
                    merchantTransactionId = merchantTransactionId,
                    professionalId = professionalId,
                    date = date,
                    time = time,
                    duration = duration,
                    type = type,
                    notes = notes
                )
                
                val response = repository.confirmPayment(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _bookingState.value = BookingState.Success(
                        booking = body.booking,
                        message = body.message
                    )
                    _paymentState.value = PaymentState.BookingConfirmed(body.booking)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    _bookingState.value = BookingState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming payment", e)
                _bookingState.value = BookingState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    // ==================== BOOKING FUNCTIONS ====================
    
    /**
     * Load patient's bookings
     */
    fun loadPatientBookings(patientId: String, status: String? = null) {
        viewModelScope.launch {
            try {
                _bookingState.value = BookingState.Loading
                
                val response = repository.getPatientBookings(patientId, status)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _bookingsList.value = body.bookings
                        _bookingState.value = BookingState.BookingsLoaded(body.bookings)
                    } else {
                        _bookingState.value = BookingState.Error("Failed to load bookings")
                    }
                } else {
                    _bookingState.value = BookingState.Error(
                        response.message() ?: "Failed to load bookings"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bookings", e)
                _bookingState.value = BookingState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Cancel a booking
     */
    fun cancelBooking(
        bookingId: String,
        cancelledBy: String = "patient",
        cancellationReason: String? = null
    ) {
        viewModelScope.launch {
            try {
                _bookingState.value = BookingState.Cancelling
                
                val request = BookingRequestBuilder.cancelBooking(
                    cancelledBy = cancelledBy,
                    cancellationReason = cancellationReason
                )
                
                val response = repository.cancelBooking(bookingId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _bookingState.value = BookingState.Cancelled(body.booking)
                    } else {
                        _bookingState.value = BookingState.Error(body.message)
                    }
                } else {
                    _bookingState.value = BookingState.Error(
                        response.message() ?: "Failed to cancel booking"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling booking", e)
                _bookingState.value = BookingState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Get booking status
     */
    fun getBookingStatus(bookingId: String) {
        viewModelScope.launch {
            try {
                val response = repository.getBookingStatus(bookingId)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _bookingState.value = BookingState.StatusUpdated(body.booking)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting booking status", e)
            }
        }
    }
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Reset states
     */
    fun resetBookingState() {
        _bookingState.value = BookingState.Idle
    }
    
    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
    
    fun resetAvailabilityState() {
        _availabilityState.value = AvailabilityState.Loading
    }
    
    /**
     * Format date for API
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Format time for API
     */
    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }
    
    // ==================== STATE CLASSES ====================
    
    sealed class BookingState {
        object Idle : BookingState()
        object Loading : BookingState()
        object Creating : BookingState()
        object Cancelling : BookingState()
        data class Success(val booking: Booking, val message: String) : BookingState()
        data class BookingsLoaded(val bookings: List<Booking>) : BookingState()
        data class Cancelled(val booking: Booking) : BookingState()
        data class StatusUpdated(val booking: Booking) : BookingState()
        data class Error(val message: String) : BookingState()
    }
    
    sealed class AvailabilityState {
        object Loading : AvailabilityState()
        data class Success(
            val date: String,
            val slots: List<TimeSlot>,
            val slotDuration: Int
        ) : AvailabilityState()
        data class AvailabilityList(
            val availabilities: List<DoctorAvailability>
        ) : AvailabilityState()
        data class Error(val message: String) : AvailabilityState()
    }
    
    sealed class PaymentState {
        object Idle : PaymentState()
        object Processing : PaymentState()
        object Verifying : PaymentState()
        data class OrderCreated(
            val merchantTransactionId: String,
            val paymentUrl: String,
            val amount: Double,
            val doctorAmount: Double,
            val platformFee: Double
        ) : PaymentState()
        data class PaymentSuccess(
            val merchantTransactionId: String,
            val transactionId: String,
            val amount: Double
        ) : PaymentState()
        data class PaymentFailed(val reason: String) : PaymentState()
        data class BookingConfirmed(val booking: Booking) : PaymentState()
        data class Error(val message: String) : PaymentState()
    }
    
    sealed class DoctorState {
        object Loading : DoctorState()
        data class Success(val doctor: Doctor) : DoctorState()
        data class Error(val message: String) : DoctorState()
    }
}
