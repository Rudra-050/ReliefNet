package com.sentrive.reliefnet.network

import com.sentrive.reliefnet.network.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication Endpoints
    @POST("api/patient/signup")
    suspend fun registerPatient(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("api/doctors/register")
    suspend fun registerDoctor(@Body request: DoctorRegistrationRequest): Response<AuthResponse>
    
    @POST("api/patient/login")
    suspend fun loginPatient(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("api/doctor/login")
    suspend fun loginDoctor(@Body request: Map<String, String>): Response<AuthResponse>
    
    // OTP Login Endpoints
    @POST("api/auth/send-otp")
    suspend fun sendOTP(@Body request: Map<String, String>): Response<OTPResponse>
    
    @POST("api/auth/verify-otp")
    suspend fun verifyOTP(@Body request: Map<String, String>): Response<AuthResponse>
    
    // Doctor Endpoints
    @GET("api/doctors")
    suspend fun getDoctors(
        @Query("specialty") specialty: String? = null,
        @Query("location") location: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 10
    ): Response<ApiResponse<List<Doctor>>>
    
    @GET("api/doctors/{id}")
    suspend fun getDoctorById(@Path("id") doctorId: String): Response<ApiResponse<Doctor>>
    
    @PUT("api/doctors/{id}")
    suspend fun updateDoctor(
        @Path("id") doctorId: String,
        @Body doctor: Doctor,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Doctor>>

    // Doctor session management (doctor creates availability)
    @POST("api/doctor/sessions")
    suspend fun createDoctorSession(
        @Body request: com.sentrive.reliefnet.network.models.CreateDoctorSessionRequest,
        @Header("Authorization") token: String
    ): Response<ApiResponse<com.sentrive.reliefnet.network.models.Session>>
    
    // Patient Endpoints
    @GET("api/patients/{id}")
    suspend fun getPatientById(@Path("id") patientId: String): Response<ApiResponse<User>>
    
    @PUT("api/patients/{id}")
    suspend fun updatePatient(
        @Path("id") patientId: String,
        @Body user: User,
        @Header("Authorization") token: String
    ): Response<ApiResponse<User>>
    
    // Session Endpoints
    @POST("api/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Session>>
    
    @GET("api/doctor/sessions")
    suspend fun getSessions(
        @Query("patientId") patientId: String? = null,
        @Query("doctorId") doctorId: String? = null,
        @Query("status") status: String? = null,
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<Session>>>
    
    @GET("api/sessions/{id}")
    suspend fun getSessionById(
        @Path("id") sessionId: String,
        @Header("Authorization") token: String
    ): Response<Session>
    
    @PUT("api/sessions/{id}")
    suspend fun updateSession(
        @Path("id") sessionId: String,
        @Body session: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Session>>
    
    @DELETE("api/sessions/{id}")
    suspend fun cancelSession(
        @Path("id") sessionId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>
    
    // Notification Endpoints
    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<ApiResponse<NotificationsResponse>>
    
    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") notificationId: String,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Notification>>
    
    // Upload Endpoint
    @Multipart
    @POST("api/upload")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<UploadResponse>
    
    // Payment Endpoints
    @POST("api/payments/create-order")
    suspend fun createPaymentOrder(
        @Body request: CreatePaymentOrderRequest,
        @Header("Authorization") token: String
    ): Response<PaymentOrderResponse>
    
    @POST("api/payments/verify")
    suspend fun verifyPayment(
        @Body request: VerifyPaymentRequest,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>
    
    // PhonePe Payment Endpoints
    @POST("api/payments/create-order")
    suspend fun createPhonePeOrder(
        @Body request: CreatePaymentOrderPhonePeRequest,
        @Header("Authorization") token: String
    ): Response<CreatePaymentOrderPhonePeResponse>
    
    @POST("api/payments/check-status")
    suspend fun checkPaymentStatus(
        @Body request: CheckPaymentStatusRequest,
        @Header("Authorization") token: String
    ): Response<CheckPaymentStatusResponse>
    
    @POST("api/payments/confirm")
    suspend fun confirmPayment(
        @Body request: ConfirmPaymentRequest,
        @Header("Authorization") token: String
    ): Response<ConfirmPaymentResponse>

    // Doctor pages
    @GET("api/doctor/profile")
    suspend fun getDoctorProfile(
        @Header("Authorization") token: String
    ): Response<DoctorProfile>

    @PUT("api/doctor/profile")
    suspend fun updateDoctorProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Authorization") token: String
    ): Response<DoctorProfile>

    @GET("api/doctor/chats")
    suspend fun getDoctorChats(
        @Header("Authorization") token: String
    ): Response<DoctorChatsResponse>

    @GET("api/doctor/feedback")
    suspend fun getDoctorFeedback(
        @Header("Authorization") token: String
    ): Response<DoctorFeedbackResponse>

    @GET("api/doctor/payments")
    suspend fun getDoctorPayments(
        @Header("Authorization") token: String
    ): Response<DoctorPaymentsResponse>

    // Patient Profile Endpoints
    @GET("api/patient/profile")
    suspend fun getPatientProfile(): Response<User>

    @PUT("api/patient/profile")
    suspend fun updatePatientProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<User>

    // Patient Payments
    @GET("api/patient/payments")
    suspend fun getPatientPayments(
        @Header("Authorization") token: String
    ): Response<PatientPaymentsResponse>

    // Patient Bookings (Legacy endpoint - use getPatientBookings from booking endpoints below)
    @GET("api/patient/bookings")
    suspend fun getPatientBookingsLegacy(
        @Header("Authorization") token: String
    ): Response<BookingListResponse>

    // Google Auth
    @POST("api/auth/google")
    suspend fun authenticateWithGoogle(
        @Body body: Map<String, String>
    ): Response<AuthResponse>

    // Device token registration for push notifications
    @POST("api/notifications/register-token")
    suspend fun registerDeviceToken(
        @Body body: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>
    
    // ==================== CHAT ENDPOINTS ====================
    
    /**
     * Get all conversations for a user
     */
    @GET("api/chat/conversations/{userType}/{userId}")
    suspend fun getConversations(
        @Path("userType") userType: String,
        @Path("userId") userId: String,
        @Header("Authorization") token: String
    ): Response<ConversationsResponse>
    
    /**
     * Get all messages in a conversation
     */
    @GET("api/chat/messages/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Header("Authorization") token: String
    ): Response<MessagesResponse>
    
    /**
     * Upload voice message file
     */
    @Multipart
    @POST("api/chat/upload-voice")
    suspend fun uploadVoiceMessage(
        @Part file: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<UploadResponse>
    
    /**
     * Upload image file
     */
    @Multipart
    @POST("api/chat/upload-image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<UploadResponse>
    
    // ============================================
    // Booking & Availability Endpoints
    // ============================================
    
    /**
     * Set doctor availability for a specific date
     */
    @POST("api/doctor/availability")
    suspend fun setDoctorAvailability(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String
    ): Response<ApiResponse<DoctorAvailability>>
    
    /**
     * Get doctor's availability for a date range
     */
    @GET("api/doctor/{doctorId}/availability")
    suspend fun getDoctorAvailability(
        @Path("doctorId") doctorId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<AvailabilityListResponse>
    
    /**
     * Get only available (not booked) slots for a specific date
     */
    @GET("api/doctor/{doctorId}/available-slots")
    suspend fun getAvailableSlots(
        @Path("doctorId") doctorId: String,
        @Query("date") date: String
    ): Response<AvailableSlotsResponse>
    
    /**
     * Create a new booking
     */
    @POST("api/bookings")
    suspend fun createBooking(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String
    ): Response<BookingResponse>
    
    /**
     * Confirm a booking
     */
    @POST("api/bookings/{id}/confirm")
    suspend fun confirmBooking(
        @Path("id") bookingId: String,
        @Body request: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<BookingResponse>
    
    /**
     * Cancel a booking
     */
    @POST("api/bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") bookingId: String,
        @Body request: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<BookingResponse>
    
    /**
     * Get booking status
     */
    @GET("api/bookings/{id}/status")
    suspend fun getBookingStatus(
        @Path("id") bookingId: String
    ): Response<BookingStatusResponse>
    
    /**
     * Get patient's bookings
     */
    @GET("api/patient/{patientId}/bookings")
    suspend fun getPatientBookings(
        @Path("patientId") patientId: String,
        @Query("status") status: String? = null,
        @Header("Authorization") token: String
    ): Response<BookingListResponse>
    
    /**
     * Get doctor's bookings
     */
    @GET("api/doctor/{doctorId}/bookings")
    suspend fun getDoctorBookings(
        @Path("doctorId") doctorId: String,
        @Query("status") status: String? = null,
        @Query("date") date: String? = null,
        @Header("Authorization") token: String
    ): Response<BookingListResponse>
    
    /**
     * Get upcoming bookings
     */
    @GET("api/bookings/upcoming")
    suspend fun getUpcomingBookings(): Response<BookingListResponse>
}

