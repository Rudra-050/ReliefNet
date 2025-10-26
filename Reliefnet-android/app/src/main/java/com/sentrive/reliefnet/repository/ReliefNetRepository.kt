package com.sentrive.reliefnet.repository

import android.util.Log
import com.sentrive.reliefnet.network.RetrofitClient
import com.sentrive.reliefnet.network.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ReliefNetRepository {
    
    private val apiService = RetrofitClient.apiService
    private val TAG = "ReliefNetRepository"
    
    // Authentication
    suspend fun registerPatient(email: String, password: String, name: String, location: String? = null): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(email, password, name, location)
                val response = apiService.registerPatient(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering patient", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun loginPatient(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = apiService.loginPatient(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in patient", e)
                Result.failure(e)
            }
        }
    }
    
    // OTP Login Methods
    suspend fun sendOTP(email: String): Result<OTPResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("email" to email)
                val response = apiService.sendOTP(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to send OTP"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending OTP", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun verifyOTP(email: String, otp: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "email" to email,
                    "otp" to otp
                )
                val response = apiService.verifyOTP(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Invalid OTP"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying OTP", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun registerDoctor(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        specialization: String,
        hospital: String
    ): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DoctorRegistrationRequest(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    specialization = specialization,
                    hospital = hospital
                )
                val response = apiService.registerDoctor(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Doctor registration failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering doctor", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun loginDoctor(medicalId: String, password: String, email: String? = null): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Create a custom request with medicalId and optionally email
                val request = mutableMapOf(
                    "medicalId" to medicalId,
                    "password" to password
                )
                if (!email.isNullOrBlank()) {
                    request["email"] = email
                }
                val response = apiService.loginDoctor(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Doctor login failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in doctor", e)
                Result.failure(e)
            }
        }
    }
    
    // Doctors
    suspend fun getDoctors(
        specialty: String? = null,
        location: String? = null,
        category: String? = null,
        page: Int = 1,
        limit: Int = 10
    ): Result<List<Doctor>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDoctors(specialty, location, category, page, limit)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch doctors"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching doctors", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getDoctorById(doctorId: String): Result<Doctor> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDoctorById(doctorId)
                
                if (response.isSuccessful && response.body() != null && response.body()!!.data != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Doctor not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching doctor", e)
                Result.failure(e)
            }
        }
    }
    
    // Sessions (Bookings)
    suspend fun createSession(
        patientId: String,
        doctorId: String,
        sessionDate: String,
        sessionTime: String,
        duration: Int = 60,
        notes: String? = null,
        token: String
    ): Result<Session> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateSessionRequest(patientId, doctorId, sessionDate, sessionTime, duration, notes)
                val response = apiService.createSession(request, "Bearer $token")
                
                if (response.isSuccessful && response.body() != null && response.body()!!.data != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create session"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating session", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getSessions(
        patientId: String? = null,
        doctorId: String? = null,
        status: String? = null,
        token: String
    ): Result<List<Session>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSessions(patientId, doctorId, status, "Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch sessions"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sessions", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun cancelSession(sessionId: String, token: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelSession(sessionId, "Bearer $token")
                
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to cancel session"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling session", e)
                Result.failure(e)
            }
        }
    }
    
    // Notifications
    suspend fun getNotifications(token: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getNotifications("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data?.notifications ?: emptyList())
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch notifications"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching notifications", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun markNotificationAsRead(notificationId: String, token: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.markNotificationAsRead(notificationId, "Bearer $token")
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
                Result.failure(e)
            }
        }
    }
    
    // ==================== PHONEPE PAYMENT FUNCTIONS ====================
    
    suspend fun createPhonePeOrder(request: CreatePaymentOrderPhonePeRequest) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.createPhonePeOrder(request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PhonePe order", e)
            throw e
        }
    }
    
    suspend fun checkPaymentStatus(request: CheckPaymentStatusRequest) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.checkPaymentStatus(request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking payment status", e)
            throw e
        }
    }
    
    suspend fun confirmPayment(request: ConfirmPaymentRequest) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.confirmPayment(request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming payment", e)
            throw e
        }
    }
    
    // ==================== BOOKING & AVAILABILITY FUNCTIONS ====================
    
    suspend fun getDoctorAvailability(
        doctorId: String,
        startDate: String,
        endDate: String
    ) = withContext(Dispatchers.IO) {
        try {
            apiService.getDoctorAvailability(doctorId, startDate, endDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doctor availability", e)
            throw e
        }
    }
    
    suspend fun getAvailableSlots(
        doctorId: String,
        date: String
    ) = withContext(Dispatchers.IO) {
        try {
            apiService.getAvailableSlots(doctorId, date)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available slots", e)
            throw e
        }
    }
    
    suspend fun createBooking(
        request: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.createBooking(request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking", e)
            throw e
        }
    }
    
    suspend fun confirmBooking(
        bookingId: String,
        request: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.confirmBooking(bookingId, request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming booking", e)
            throw e
        }
    }
    
    suspend fun cancelBooking(
        bookingId: String,
        request: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.cancelBooking(bookingId, request, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking", e)
            throw e
        }
    }
    
    suspend fun getBookingStatus(bookingId: String) = withContext(Dispatchers.IO) {
        try {
            apiService.getBookingStatus(bookingId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking status", e)
            throw e
        }
    }
    
    suspend fun getPatientBookings(
        patientId: String,
        status: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.getPatientBookings(patientId, status, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient bookings", e)
            throw e
        }
    }
    
    suspend fun getDoctorBookings(
        doctorId: String,
        status: String? = null,
        date: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
            apiService.getDoctorBookings(doctorId, status, date, "Bearer $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doctor bookings", e)
            throw e
        }
    }
    
    // Doctor Availability Slot Management
    suspend fun addDoctorAvailabilitySlot(doctorId: String, date: Date, hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val request = mapOf(
            "doctorId" to doctorId,
            "date" to sdf.format(date),
            "hour" to hour,
            "minute" to minute
        )
        apiService.setDoctorAvailability(request, "Bearer $token")
    }

    suspend fun editDoctorAvailabilitySlot(doctorId: String, slotId: String, date: Date, hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val request = mapOf(
            "doctorId" to doctorId,
            "slotId" to slotId,
            "date" to sdf.format(date),
            "hour" to hour,
            "minute" to minute
        )
        apiService.setDoctorAvailability(request, "Bearer $token")
    }

    suspend fun deleteDoctorAvailabilitySlot(doctorId: String, slotId: String, date: Date) = withContext(Dispatchers.IO) {
        val token = RetrofitClient.authToken ?: throw Exception("Not authenticated")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val request = mapOf(
            "doctorId" to doctorId,
            "slotId" to slotId,
            "date" to sdf.format(date),
            "delete" to true
        )
        apiService.setDoctorAvailability(request, "Bearer $token")
    }
}
