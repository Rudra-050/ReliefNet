package com.sentrive.reliefnet.network.models

import com.google.gson.annotations.SerializedName

// ============================================
// Booking Models
// ============================================

data class TimeSlot(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("isBooked") val isBooked: Boolean = false,
    @SerializedName("bookingId") val bookingId: String? = null,
    @SerializedName("_id") val id: String? = null
)

data class DoctorAvailability(
    @SerializedName("_id") val id: String,
    @SerializedName("doctorId") val doctorId: String,
    @SerializedName("doctorName") val doctorName: String,
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String? = null,
    @SerializedName("slots") val slots: List<TimeSlot>,
    @SerializedName("isAvailable") val isAvailable: Boolean = true,
    @SerializedName("slotDuration") val slotDuration: Int = 60,
    @SerializedName("breakStart") val breakStart: String? = null,
    @SerializedName("breakEnd") val breakEnd: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class Booking(
    @SerializedName("_id") val id: String,
    @SerializedName("patientId") val patientId: String,
    @SerializedName("patientName") val patientName: String,
    @SerializedName("patientEmail") val patientEmail: String,
    @SerializedName("patientPhone") val patientPhone: String? = null,
    
    @SerializedName("doctorId") val doctorId: String,
    @SerializedName("doctorName") val doctorName: String,
    @SerializedName("doctorEmail") val doctorEmail: String,
    
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("availabilitySlotId") val availabilitySlotId: String? = null,
    
    @SerializedName("appointmentDate") val appointmentDate: String,
    @SerializedName("appointmentTime") val appointmentTime: String,
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("sessionType") val sessionType: String = "consultation",
    
    @SerializedName("status") val status: BookingStatus = BookingStatus.PENDING,
    @SerializedName("paymentId") val paymentId: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String = "INR",
    
    @SerializedName("reasonForVisit") val reasonForVisit: String? = null,
    @SerializedName("symptoms") val symptoms: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("doctorNotes") val doctorNotes: String? = null,
    
    @SerializedName("reminderSent24h") val reminderSent24h: Boolean = false,
    @SerializedName("reminderSent1h") val reminderSent1h: Boolean = false,
    
    @SerializedName("cancelledBy") val cancelledBy: String? = null,
    @SerializedName("cancellationReason") val cancellationReason: String? = null,
    @SerializedName("cancelledAt") val cancelledAt: String? = null,
    
    @SerializedName("confirmedAt") val confirmedAt: String? = null,
    @SerializedName("confirmedBy") val confirmedBy: String? = null,
    
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

// Type alias for compatibility with payment APIs
typealias BookingItem = Booking

enum class BookingStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("confirmed") CONFIRMED,
    @SerializedName("cancelled") CANCELLED,
    @SerializedName("completed") COMPLETED,
    @SerializedName("no-show") NO_SHOW
}

enum class PaymentStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("paid") PAID,
    @SerializedName("refunded") REFUNDED,
    @SerializedName("failed") FAILED
}

// ============================================
// Response Models
// ============================================

data class AvailabilityListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("availability") val availability: List<DoctorAvailability>
)

data class AvailableSlotsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("date") val date: String,
    @SerializedName("availableSlots") val availableSlots: List<TimeSlot>,
    @SerializedName("slotDuration") val slotDuration: Int
)

data class BookingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("booking") val booking: Booking
)

data class BookingStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("paymentStatus") val paymentStatus: String,
    @SerializedName("booking") val booking: Booking
)

data class BookingListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("bookings") val bookings: List<Booking>
)

// ============================================
// Request Helper Functions
// ============================================

object BookingRequestBuilder {
    fun createBooking(
        patientId: String,
        patientName: String,
        patientEmail: String,
        patientPhone: String?,
        doctorId: String,
        doctorName: String,
        doctorEmail: String,
        appointmentDate: String,
        appointmentTime: String,
        duration: Int = 60,
        sessionType: String = "consultation",
        amount: Double,
        reasonForVisit: String? = null,
        symptoms: String? = null,
        notes: String? = null
    ): Map<String, Any?> {
        return mapOf(
            "patientId" to patientId,
            "patientName" to patientName,
            "patientEmail" to patientEmail,
            "patientPhone" to patientPhone,
            "doctorId" to doctorId,
            "doctorName" to doctorName,
            "doctorEmail" to doctorEmail,
            "appointmentDate" to appointmentDate,
            "appointmentTime" to appointmentTime,
            "duration" to duration,
            "sessionType" to sessionType,
            "amount" to amount,
            "reasonForVisit" to reasonForVisit,
            "symptoms" to symptoms,
            "notes" to notes
        )
    }
    
    /**
     * Helper function to map UI appointmentType to backend sessionType
     */
    fun mapAppointmentTypeToSessionType(appointmentType: String): String {
        return when (appointmentType) {
            "Online Consultation" -> "consultation"
            "In-Person Visit" -> "consultation"
            "Home Visit" -> "home-visit"
            else -> "consultation"
        }
    }
    
    fun setAvailability(
        doctorId: String,
        date: String,
        slots: List<Map<String, String>>,
        slotDuration: Int = 60,
        notes: String? = null
    ): Map<String, Any?> {
        return mapOf(
            "doctorId" to doctorId,
            "date" to date,
            "slots" to slots,
            "slotDuration" to slotDuration,
            "notes" to notes
        )
    }
    
    fun confirmBooking(
        paymentId: String? = null,
        confirmedBy: String = "patient"
    ): Map<String, String> {
        val map = mutableMapOf("confirmedBy" to confirmedBy)
        paymentId?.let { map["paymentId"] = it }
        return map
    }
    
    fun cancelBooking(
        cancelledBy: String = "patient",
        cancellationReason: String? = null
    ): Map<String, String> {
        val map = mutableMapOf("cancelledBy" to cancelledBy)
        cancellationReason?.let { map["cancellationReason"] = it }
        return map
    }
}

// ============================================
// UI Helper Extensions
// ============================================

fun BookingStatus.toDisplayString(): String {
    return when (this) {
        BookingStatus.PENDING -> "Pending"
        BookingStatus.CONFIRMED -> "Confirmed"
        BookingStatus.CANCELLED -> "Cancelled"
        BookingStatus.COMPLETED -> "Completed"
        BookingStatus.NO_SHOW -> "No Show"
    }
}

fun BookingStatus.getColor(): Long {
    return when (this) {
        BookingStatus.PENDING -> 0xFFFFA726 // Orange
        BookingStatus.CONFIRMED -> 0xFF66BB6A // Green
        BookingStatus.CANCELLED -> 0xFFEF5350 // Red
        BookingStatus.COMPLETED -> 0xFF42A5F5 // Blue
        BookingStatus.NO_SHOW -> 0xFF9E9E9E // Grey
    }
}

fun PaymentStatus.toDisplayString(): String {
    return when (this) {
        PaymentStatus.PENDING -> "Pending"
        PaymentStatus.PAID -> "Paid"
        PaymentStatus.REFUNDED -> "Refunded"
        PaymentStatus.FAILED -> "Failed"
    }
}

fun PaymentStatus.getColor(): Long {
    return when (this) {
        PaymentStatus.PENDING -> 0xFFFFA726 // Orange
        PaymentStatus.PAID -> 0xFF66BB6A // Green
        PaymentStatus.REFUNDED -> 0xFF42A5F5 // Blue
        PaymentStatus.FAILED -> 0xFFEF5350 // Red
    }
}
