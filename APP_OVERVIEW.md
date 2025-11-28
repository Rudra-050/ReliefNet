# ReliefNet - App Overview

## What is ReliefNet?

ReliefNet is a comprehensive telemedicine platform connecting patients with healthcare professionals for remote consultations, mental health support, and medical services.

## Key Features

### For Patients 👤
- **Easy Registration & Login**
  - Email/password authentication
  - Google Sign-In integration
  - OTP-based verification

- **Doctor Discovery**
  - Browse qualified healthcare professionals
  - View detailed doctor profiles with specializations
  - Check availability and ratings

- **Appointment Booking**
  - Book sessions with preferred doctors
  - View upcoming and past appointments
  - Reschedule or cancel bookings
  - Real-time booking notifications

- **Video & Audio Consultations**
  - High-quality video calls using WebRTC
  - Audio-only call option for low bandwidth
  - In-call controls (mute, camera toggle, end call)

- **Real-time Chat**
  - Text messaging with doctors
  - Instant notifications for new messages
  - Message history and conversation threads

- **AI Chatbot Assistant**
  - 24/7 medical information support
  - Symptom checker
  - Health guidance and recommendations

- **Secure Payments**
  - Multiple payment options (PhonePe, Google Pay)
  - Transparent pricing
  - Transaction history

- **Profile Management**
  - Update personal information
  - Upload profile photo
  - View booking history

### For Doctors 👨‍⚕️
- **Professional Registration**
  - Medical ID verification
  - Specialty and qualification details
  - Profile customization

- **Session Management**
  - View all patient bookings
  - Accept or decline appointments
  - Manage availability schedule

- **Patient Communication**
  - Video/audio consultations
  - Chat with patients
  - Session notes and follow-ups

- **Notifications**
  - Real-time booking alerts via push notifications
  - In-app notification center
  - Session reminders

- **Cancellation Management**
  - Cancel sessions with reason tracking
  - Automatic slot management

## Technical Highlights

### Mobile App (Android)
- **Framework**: Jetpack Compose with Material3 design
- **Architecture**: MVVM pattern with Repository layer
- **Navigation**: Compose Navigation with type-safe routes
- **Real-time**: Socket.IO for chat and call signaling
- **Media**: WebRTC for high-quality video/audio calls
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Payments**: PhonePe SDK integration

### Backend Server
- **Runtime**: Node.js with Express.js
- **Database**: MongoDB with Mongoose ODM
- **Real-time**: Socket.IO for WebRTC signaling
- **Email**: SendGrid for OTP and notifications
- **Authentication**: JWT tokens with secure session management
- **Cloud**: Railway deployment

## User Journey

### Patient Flow
1. **Sign Up** → Choose "Patient" → Register/Login
2. **Browse Doctors** → View profiles → Select doctor
3. **Book Session** → Choose time slot → Make payment
4. **Get Notified** → Receive booking confirmation
5. **Join Call** → At appointment time → Video/Audio consultation
6. **Chat** → Follow-up questions → Real-time messaging
7. **Complete** → End session → Rate doctor

### Doctor Flow
1. **Sign Up** → Choose "Professional" → Register with Medical ID
2. **Set Profile** → Add specialization → Set availability
3. **Receive Booking** → Push notification → View session details
4. **Manage Sessions** → Accept/decline → Prepare for consultation
5. **Join Call** → At appointment time → Conduct consultation
6. **Follow-up** → Chat with patient → Provide recommendations
7. **Complete** → End session → Update records

## Security & Privacy

- End-to-end encrypted communications
- Secure JWT-based authentication
- HIPAA-compliant data handling
- Protected health information (PHI) safeguards
- Secure payment processing
- Regular security audits

## Platform Support

- **Android**: 8.0 (API 26) and above
- **Backend**: Cloud-hosted on Railway
- **Database**: MongoDB Atlas (cloud)
- **Real-time**: WebSocket connections via Socket.IO

## Getting Started

### For Patients
1. Download ReliefNet app from Play Store
2. Sign up with email or Google account
3. Complete your profile
4. Browse and book your first appointment

### For Doctors
1. Download ReliefNet app from Play Store
2. Register as a healthcare professional
3. Provide medical credentials for verification
4. Set up your profile and availability
5. Start accepting patient appointments

## Support

For technical support or inquiries:
- Email: support@reliefnet.com
- In-app help center
- FAQ section

---

**Version**: 1.0.0  
**Last Updated**: October 2025  
**Platform**: Android Mobile Application
