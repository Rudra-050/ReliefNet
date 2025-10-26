const express = require('express');
const app = express();
const http = require('http');
const server = http.createServer(app);

app.use(express.json());

// Enable CORS
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  next();
});

// In-memory OTP storage (for testing - use Redis in production)
const otpStore = new Map();

// Helper function to generate 6-digit OTP
function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Test endpoint - no MongoDB needed
app.get('/api/test', (req, res) => {
  res.json({
    success: true,
    message: 'Backend is working!',
    timestamp: new Date().toISOString()
  });
});

// Mock login endpoint for testing (no MongoDB)
app.post('/api/login', (req, res) => {
  const { email, password } = req.body;
  
  // Mock authentication
  if (email && password) {
    res.json({
      token: 'mock-jwt-token-' + Date.now(),
      user: {
        _id: 'mock-user-123',
        email: email,
        name: 'Test User',
        location: 'Test Location'
      },
      message: 'Login successful (mock)'
    });
  } else {
    res.status(400).json({
      message: 'Email and password required'
    });
  }
});

// OTP Login System for Patients

// Step 1: Send OTP to email
app.post('/api/auth/send-otp', (req, res) => {
  const { email } = req.body;
  
  if (!email) {
    return res.status(400).json({
      success: false,
      message: 'Email is required'
    });
  }
  
  // Generate 6-digit OTP
  const otp = generateOTP();
  const expiresAt = Date.now() + 5 * 60 * 1000; // OTP expires in 5 minutes
  
  // Store OTP (in production, use Redis with TTL)
  otpStore.set(email, { otp, expiresAt });
  
  // In production, send real email using nodemailer or SendGrid
  // For testing, we'll just return the OTP in console
  console.log(`📧 OTP for ${email}: ${otp} (expires in 5 minutes)`);
  
  res.json({
    success: true,
    message: 'OTP sent to your email',
    // FOR TESTING ONLY - Remove in production
    testOtp: otp
  });
});

// Step 2: Verify OTP and login
app.post('/api/auth/verify-otp', (req, res) => {
  const { email, otp } = req.body;
  
  if (!email || !otp) {
    return res.status(400).json({
      success: false,
      message: 'Email and OTP are required'
    });
  }
  
  // Check if OTP exists
  const storedOtpData = otpStore.get(email);
  
  if (!storedOtpData) {
    return res.status(400).json({
      success: false,
      message: 'OTP not found. Please request a new OTP.'
    });
  }
  
  // Check if OTP expired
  if (Date.now() > storedOtpData.expiresAt) {
    otpStore.delete(email);
    return res.status(400).json({
      success: false,
      message: 'OTP has expired. Please request a new OTP.'
    });
  }
  
  // Verify OTP
  if (storedOtpData.otp !== otp) {
    return res.status(400).json({
      success: false,
      message: 'Invalid OTP. Please try again.'
    });
  }
  
  // OTP is valid - delete it and login user
  otpStore.delete(email);
  
  res.json({
    success: true,
    token: 'mock-jwt-token-' + Date.now(),
    user: {
      _id: 'mock-user-' + Date.now(),
      email: email,
      name: 'Patient User',
      location: null
    },
    message: 'Login successful via OTP'
  });
});

// Mock register endpoint for testing (no MongoDB)
app.post('/api/register', (req, res) => {
  const { email, password, name } = req.body;
  
  if (email && password && name) {
    res.json({
      token: 'mock-jwt-token-' + Date.now(),
      user: {
        _id: 'mock-user-' + Date.now(),
        email: email,
        name: name,
        location: null
      },
      message: 'Registration successful (mock)'
    });
  } else {
    res.status(400).json({
      message: 'All fields required'
    });
  }
});

// Helper function to generate unique Medical ID (mock)
function generateMedicalId() {
  const prefix = 'RN'; // ReliefNet prefix
  const randomNum = Math.floor(100000 + Math.random() * 900000);
  return `${prefix}-${randomNum}`;
}

// Mock doctor registration endpoint
app.post('/api/doctors/register', (req, res) => {
  const { firstName, lastName, email, phone, password, specialization, hospital } = req.body;
  
  if (!firstName || !lastName || !email || !password || !specialization) {
    return res.status(400).json({
      message: 'Missing required fields'
    });
  }
  
  // Generate unique Medical ID
  const generatedMedicalId = generateMedicalId();
  const fullName = `Dr. ${firstName} ${lastName}`;
  
  res.json({
    success: true,
    token: 'mock-jwt-token-' + Date.now(),
    user: {
      _id: 'mock-doctor-' + Date.now(),
      email: email,
      name: fullName,
      firstName: firstName,
      lastName: lastName,
      phone: phone,
      specialization: specialization,
      hospital: hospital,
      medicalId: generatedMedicalId, // Company-generated Medical ID
      role: 'doctor'
    },
    message: `Registration successful! Your Medical ID is: ${generatedMedicalId}`
  });
});

// Mock doctor login endpoint for testing (no MongoDB)
app.post('/api/doctor/login', (req, res) => {
  const { email, password, medicalId } = req.body;
  
  // Mock authentication - accept either email or medicalId
  if ((email || medicalId) && password) {
    res.json({
      token: 'mock-jwt-doctor-token-' + Date.now(),
      user: {
        _id: 'mock-doctor-123',
        email: email || 'doctor@example.com',
        name: 'Dr. Test Professional',
        medicalId: medicalId || 'MD-12345',
        specialty: 'Mental Health',
        specialization: 'Psychologist',
        bio: 'Professional mental health specialist',
        location: 'Test Hospital',
        role: 'doctor'
      },
      message: 'Doctor login successful (mock)'
    });
  } else {
    res.status(400).json({
      message: 'Medical ID/Email and password required'
    });
  }
});

// Mock doctors endpoint
app.get('/api/doctors', (req, res) => {
  res.json({
    success: true,
    data: [
      {
        _id: 'doctor-1',
        name: 'Dr. Sarah Johnson',
        email: 'sarah@example.com',
        specialization: 'Psychologist',
        rating: 4.8,
        reviewCount: 127,
        price: 150,
        priceUnit: '/session',
        availability: 'Available',
        experience: '10 years',
        bio: 'Specializing in anxiety and depression treatment'
      },
      {
        _id: 'doctor-2',
        name: 'Dr. Michael Chen',
        email: 'michael@example.com',
        specialization: 'Therapist',
        rating: 4.9,
        reviewCount: 203,
        price: 175,
        priceUnit: '/session',
        availability: 'Available',
        experience: '15 years',
        bio: 'Expert in cognitive behavioral therapy'
      }
    ]
  });
});

const PORT = 5000;
server.listen(PORT, () => {
  console.log('✅ TEST SERVER running at http://localhost:' + PORT);
  console.log('✅ This is a mock server for testing - no MongoDB required');
  console.log('📍 Test endpoint: http://localhost:' + PORT + '/api/test');
  console.log('📍 Mock login: POST http://localhost:' + PORT + '/api/login');
  console.log('📍 Mock doctors: GET http://localhost:' + PORT + '/api/doctors');
});
