package com.sentrive.reliefnet.network

import com.sentrive.reliefnet.BuildConfig

object ApiConfig {
    // Use BuildConfig.BASE_URL configured per build type (debug/release)
    // Debug: http://10.0.2.2:5000/
    // Release: https://api.yourdomain.com/
    val BASE_URL: String = BuildConfig.BASE_URL

    // Socket.IO URL follows base; if using HTTPS/WSS in prod, update accordingly
    val SOCKET_URL: String = BASE_URL.removeSuffix("/")

    object Endpoints {
        const val REGISTER_PATIENT = "api/patient/signup"
        const val REGISTER_DOCTOR = "api/doctors/register"
        const val LOGIN_PATIENT = "api/patient/login"
        const val LOGIN_DOCTOR = "api/doctors/login"
        const val GET_DOCTORS = "api/doctors"
        const val GET_DOCTOR_BY_ID = "api/doctors/{id}"
        const val UPDATE_PATIENT = "api/patients/{id}"
        const val UPDATE_DOCTOR = "api/doctors/{id}"
        const val CREATE_SESSION = "api/sessions"
        const val GET_SESSIONS = "api/sessions"
        const val UPDATE_SESSION = "api/sessions/{id}"
        const val GET_NOTIFICATIONS = "api/notifications"
        const val MARK_NOTIFICATION_READ = "api/notifications/{id}/read"
        const val UPLOAD_PHOTO = "api/upload"
        const val CREATE_PAYMENT_ORDER = "api/payments/create-order"
        const val VERIFY_PAYMENT = "api/payments/verify"
    }
}
