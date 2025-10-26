package com.sentrive.reliefnet.network.models

import com.google.gson.annotations.SerializedName

// Auth Models
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val location: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User? = null,
    val doctor: Doctor? = null,
    val message: String? = null,
    val success: Boolean? = null
)

data class OTPResponse(
    val success: Boolean,
    val message: String,
    val testOtp: String? = null // For testing only
)

// User Models
data class User(
    @SerializedName("_id")
    val id: String? = null,
    val email: String,
    val name: String,
    val location: String? = null,
    val photoUrl: String? = null,
    val phone: String? = null
)

// Doctor Models
data class Doctor(
    @SerializedName("_id")
    val id: String? = null,
    val email: String,
    val name: String,
    val medicalId: String? = null,
    val specialization: String? = null,
    val specialty: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val photoUrl: String? = null,
    val title: String? = null,
    val category: String? = null,
    val description: String? = null,
    val experience: String? = "0 years",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val price: Double = 0.0,
    val priceUnit: String? = "/session",
    val availability: String? = "Available",
    val statusColor: String? = "#34d399",
    val certifications: String? = null,
    val education: String? = null,
    val hospital: String? = null,
    val phoneNumber: String? = null,
    val serviceAreas: List<String>? = null,
    val languages: String? = "English",
    val specializations: List<String>? = null,
    val coordinates: Coordinates? = null,
    val distance: Double? = 0.0,
    val isVerified: Boolean = false,
    val isActive: Boolean = true
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class DoctorRegistrationRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialization: String,
    val hospital: String
)

// Session Models
data class Session(
    @SerializedName("_id")
    val id: String,
    val patientId: String,
    val doctorId: String,
    val sessionDate: String,
    val sessionTime: String,
    val duration: Int? = 60,
    val status: String = "scheduled", // scheduled, completed, cancelled
    val notes: String? = null,
    val paymentStatus: String? = "pending",
    val paymentId: String? = null,
    val createdAt: String? = null
)

data class CreateSessionRequest(
    val patientId: String,
    val doctorId: String,
    val sessionDate: String,
    val sessionTime: String,
    val duration: Int? = 60,
    val notes: String? = null
)

// Notification Models
data class Notification(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val userType: String,
    val message: String,
    val type: String,
    val data: Map<String, Any?>? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

data class NotificationsResponse(
    val notifications: List<Notification>
)

// Payment Models
data class CreatePaymentOrderRequest(
    val amount: Double,
    val currency: String = "INR",
    val receipt: String,
    val notes: Map<String, String>? = null
)

data class PaymentOrderResponse(
    val id: String,
    val amount: Double,
    val currency: String,
    val receipt: String
)

// --- Doctor specific models ---

data class DoctorProfile(
    val id: String,
    val name: String,
    val email: String,
    val specialty: String? = null,
    val specialization: String? = null,
    val medicalId: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val userType: String? = null
)

data class DoctorChatSummary(
    val conversationId: String,
    val patientId: String,
    val patientName: String?,
    val lastMessage: String?,
    val lastMessageTime: String?,
    val unreadCountDoctor: Int?
)

data class DoctorFeedbackItem(
    val _id: String,
    val patientId: String,
    val patientName: String?,
    val rating: Double,
    val comment: String?,
    val createdAt: String
)

data class DoctorPaymentsResponse(
    val success: Boolean,
    val totalPaid: Double,
    val totalPending: Double,
    val payments: List<DoctorPaymentItem>
)

data class DoctorPaymentItem(
    val _id: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val createdAt: String,
    val receiptUrl: String? = null
)

data class DoctorChatsResponse(
    val success: Boolean,
    val conversations: List<DoctorChatSummary>
)

data class DoctorFeedbackResponse(
    val success: Boolean,
    val averageRating: Double,
    val count: Int,
    val feedback: List<DoctorFeedbackItem>
)

// PhonePe Payment Models
data class CreatePaymentOrderPhonePeRequest(
    val amount: Double,
    val doctorId: String,
    val appointmentDate: String,
    val appointmentTime: String
)

data class CreatePaymentOrderPhonePeResponse(
    val success: Boolean,
    val merchantTransactionId: String,
    val amount: Double,
    val doctorAmount: Double,
    val platformFee: Double,
    val paymentUrl: String,
    val phonepeData: PhonePeData? = null
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
    val paymentInstrument: String? = null,
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
    val payment: PaymentItemPhonePe
)

data class PaymentItemPhonePe(
    @SerializedName("_id")
    val id: String,
    val amount: Double,
    val doctorAmount: Double,
    val platformFee: Double,
    val status: String,
    val transactionId: String
)

// Legacy Google Pay model (keeping for backward compatibility)
data class VerifyPaymentRequest(
    val googlePayTransactionId: String,
    val orderId: String,
    val professionalId: String,
    val date: String,
    val time: String,
    val duration: Int? = null,
    val type: String? = null,
    val notes: String? = null,
    val sessionId: String? = null,
    val amount: Double
)

// Patient specific models
data class PaymentItem(
    @SerializedName("_id")
    val id: String,
    val amount: Double,
    val currency: String = "INR",
    val status: String,
    val createdAt: String,
    val googlePayTransactionId: String? = null,
    val receiptUrl: String? = null
)

data class PatientPaymentsResponse(
    val success: Boolean,
    val payments: List<PaymentItem>
)

// Booking model moved to BookingModels.kt for comprehensive booking system
// Use BookingListResponse from BookingModels.kt instead of PatientBookingsResponse

// Generic Response
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

// Upload Response
data class UploadResponse(
    val url: String,
    val filename: String
)
