const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const admin = require('firebase-admin');

// Load Firebase service account from env (for Railway/production) or file (for local dev)
let serviceAccount;
if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
  // Production: use base64-encoded service account from env
  const decoded = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, 'base64').toString('utf-8');
  serviceAccount = JSON.parse(decoded);
} else if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
  // Production: use individual env vars
  serviceAccount = {
    type: 'service_account',
    project_id: process.env.FIREBASE_PROJECT_ID,
    private_key: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
    client_email: process.env.FIREBASE_CLIENT_EMAIL
  };
} else {
  // Local dev: use file
  try {
    serviceAccount = require('./serviceAccountKey.json');
  } catch (err) {
    console.error('⚠️  Firebase service account not found. Set FIREBASE_SERVICE_ACCOUNT_BASE64 or provide serviceAccountKey.json');
    process.exit(1);
  }
}

const mongoose = require('mongoose');
const multer = require('multer');
const path = require('path');
const http = require('http');
const socketio = require('socket.io');
const sgMail = require('@sendgrid/mail');
const crypto = require('crypto');
const axios = require('axios');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
// Load environment variables from .env if present
try { require('dotenv').config(); } catch (_) {}
// Doctor model is defined below in this file
const router = express.Router();
const Session = require('./models/Session');
const LoginSession = require('./models/LoginSession');
const Message = require('./models/Message');
const Conversation = require('./models/Conversation');
const Feedback = require('./models/Feedback');
const DoctorPayout = require('./models/DoctorPayout');
const VideoCall = require('./models/VideoCall');

// Configure SendGrid API Key (prefer env var)
sgMail.setApiKey(process.env.SENDGRID_API_KEY || '');

// In-memory OTP storage (use Redis in production for scalability)
const otpStore = new Map();

// Helper function to generate 6-digit OTP
function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Function to send OTP email using SendGrid
async function sendOTPEmail(email, otp) {
  const msg = {
    to: email,
    from: process.env.SENDGRID_FROM_EMAIL || 'sentrive.ai@gmail.com', // Verified sender from SendGrid
    replyTo: process.env.SENDGRID_REPLY_TO || process.env.SENDGRID_FROM_EMAIL || 'sentrive.ai@gmail.com',
    subject: 'Your ReliefNet Login Code',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
          .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
          .otp-box { background: white; border: 2px dashed #667eea; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }
          .otp-code { font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; }
          .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
          .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>🏥 ReliefNet</h1>
            <p>Your Mental Health Companion</p>
          </div>
          <div class="content">
            <h2>Your Login Code</h2>
            <p>Hello,</p>
            <p>You requested to log in to your ReliefNet account. Use the code below to complete your login:</p>
            
            <div class="otp-box">
              <p style="margin: 0; font-size: 14px; color: #666;">Your OTP Code</p>
              <div class="otp-code">${otp}</div>
            </div>
            
            <div class="warning">
              <strong>⚠️ Important:</strong>
              <ul style="margin: 10px 0;">
                <li>This code expires in <strong>5 minutes</strong></li>
                <li>Do not share this code with anyone</li>
                <li>ReliefNet staff will never ask for your OTP</li>
              </ul>
            </div>
            
            <p>If you didn't request this code, please ignore this email or contact our support team.</p>
            
            <p>Best regards,<br><strong>The ReliefNet Team</strong></p>
          </div>
          <div class="footer">
            <p>This is an automated email. Please do not reply.</p>
            <p>&copy; 2025 ReliefNet. All rights reserved.</p>
          </div>
        </div>
      </body>
      </html>
    `
  };

  try {
    await sgMail.send(msg);
    console.log(`✅ OTP email sent successfully to ${email} via SendGrid`);
    return true;
  } catch (error) {
    console.error(`❌ Failed to send OTP email to ${email}:`, error.message);
    if (error.response) {
      console.error('SendGrid error details:', error.response.body);
    }
    return false;
  }
}

// Function to send Doctor Welcome Email with Medical ID
async function sendDoctorWelcomeEmail(email, name, medicalId) {
  const fromEmail = process.env.SENDGRID_FROM_EMAIL || 'sentrive.ai@gmail.com';
  const supportEmail = process.env.SENDGRID_REPLY_TO || fromEmail;
  const appUrl = process.env.APP_URL || 'https://reliefnet.app';

  const msg = {
    to: email,
    from: fromEmail,
    replyTo: supportEmail,
    subject: 'Welcome to ReliefNet — Your Medical ID',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <style>
          body { font-family: Arial, sans-serif; color:#333; }
          .wrap { max-width: 640px; margin: 0 auto; padding: 24px; }
          .card { background: #ffffff; border-radius: 10px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
          .title { color:#5b67f1; margin: 0 0 8px; }
          .muted { color:#6b7280 }
          .badge { display:inline-block; background:#EEF2FF; color:#4F46E5; border:1px dashed #C7D2FE; padding:12px 16px; border-radius:8px; font-weight:bold; letter-spacing: 1px; }
          .btn { display:inline-block; background:#4F46E5; color:#fff; padding:12px 16px; text-decoration:none; border-radius:8px; }
          .footer { color:#9ca3af; font-size:12px; margin-top:24px; }
        </style>
      </head>
      <body>
        <div class="wrap">
          <div class="card">
            <h2 class="title">Welcome, ${name} 👋</h2>
            <p class="muted">Your ReliefNet professional account has been created successfully.</p>
            <p>Here is your Medical ID:</p>
            <p class="badge">${medicalId}</p>
            <p style="margin-top:16px">Use this Medical ID (or your email) with your password to log in.</p>
            <p>
              <a class="btn" href="${appUrl}" target="_blank">Open ReliefNet</a>
            </p>
            <p class="footer">If you did not create this account, please contact support at ${supportEmail}.</p>
          </div>
        </div>
      </body>
      </html>
    `
  };

  try {
    if (!process.env.SENDGRID_API_KEY) {
      console.warn('SENDGRID_API_KEY not set — skipping doctor welcome email.');
      return;
    }
    await sgMail.send(msg);
    console.log(`📧 Doctor welcome email sent to ${email}`);
  } catch (error) {
    console.error('Failed to send doctor welcome email:', error.message);
    if (error.response?.body) {
      console.error('SendGrid error details:', error.response.body);
    }
  }
}

// Notification model
const notificationSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, required: true, refPath: 'userType' },
  userType: { type: String, enum: ['Doctor', 'User'], required: true },
  message: { type: String, required: true },
  type: { type: String, required: true }, // e.g., 'booking', 'session', 'info'
  data: { type: Object }, // extra data (optional)
  isRead: { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now }
});
const Notification = mongoose.model('Notification', notificationSchema);

// Device Token model (for FCM push notifications)
const deviceTokenSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, required: true, refPath: 'userType' },
  userType: { type: String, enum: ['Doctor', 'User'], required: true },
  token: { type: String, required: true },
  platform: { type: String, default: 'android' },
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});
deviceTokenSchema.index({ userId: 1, userType: 1, token: 1 }, { unique: true });
const DeviceToken = mongoose.model('DeviceToken', deviceTokenSchema);

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

// Connect to MongoDB Atlas (uses env var when available)
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://rudranshbhatt2005:tiger%401358@relief.sb1iuve.mongodb.net/reliefnet?retryWrites=true&w=majority&appName=Relief';
mongoose.connect(MONGODB_URI, { serverSelectionTimeoutMS: 10000 })
  .then(() => {
    console.log('✅ Connected to MongoDB Atlas successfully');
  })
  .catch((err) => {
    console.error('❌ MongoDB connection error:', err.message);
    console.log('⚠️  Server will continue running but database features will be unavailable');
    console.log('ℹ️  Ensure your current IP is whitelisted in Atlas and credentials are correct.');
  });

const db = mongoose.connection;
db.on('error', console.error.bind(console, 'MongoDB connection error:'));
db.on('disconnected', () => {
  console.log('⚠️  MongoDB disconnected. Attempting to reconnect...');
});

// Health endpoint will be registered after Express app is initialized

// Patient (User) Schema and Model
const patientSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  name: String,
  password: String, // hashed password
  location: String,
  phone: String, // patient phone number (optional)
  photoUrl: { type: String }, // <-- Added for Google profile photo
  // add more patient-specific fields here
});
const Patient = mongoose.model('User', patientSchema); // or 'Patient'

// Doctor Schema and Model
const doctorSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  name: String,
  password: String, // hashed password
  medicalId: String, // Medical ID field
  specialization: String, // Specialization field
  specialty: String, // (optional, for compatibility)
  bio: String,
  location: String,
  photoUrl: { type: String },
  
  // Enhanced fields for professional listing
  title: String, // e.g., "Clinical Psychologist", "Elderly Care Specialist"
  category: String, // e.g., "Psychologist", "Therapist", "Nurse", "Caregiver"
  description: String, // detailed description
  experience: { type: String, default: "0 years" }, // years of experience as string
  rating: { type: Number, default: 0, min: 0, max: 5 },
  reviewCount: { type: Number, default: 0 },
  price: { type: Number, default: 0 }, // per session/day
  priceUnit: { type: String, default: "/session" }, // "/session", "/day", "/hour"
  availability: { type: String, default: "Available" }, // "Available", "Busy", "Unavailable"
  statusColor: { type: String, default: "#34d399" },
  
  // Professional details
  certifications: { type: String, default: "" }, // certifications as string
  education: String,
  hospital: String,
  phoneNumber: String,
  
  // Service areas and specializations
  serviceAreas: [String], // e.g., ["Mumbai", "Pune"]
  languages: { type: String, default: "English" }, // languages as string
  specializations: [String], // e.g., ["CBT", "Depression", "Anxiety"]
  
  // Location and distance
  coordinates: {
    latitude: Number,
    longitude: Number
  },
  distance: { type: Number, default: 0 }, // calculated distance from user
  
  // Verification and status
  isVerified: { type: Boolean, default: false },
  isActive: { type: Boolean, default: true },
  
  // Timestamps
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

const Doctor = mongoose.model('Doctor', doctorSchema);

// Track connected users: { userId: socketId }
const connectedUsers = {};

const app = express();
// Trust the first proxy (needed to detect HTTPS when behind a reverse proxy)
app.set('trust proxy', 1);
// Enforce HTTPS in production for GET/HEAD requests (avoid redirecting POST webhooks and health checks)
if (process.env.NODE_ENV === 'production') {
  app.use((req, res, next) => {
    // Skip HTTPS redirect for health endpoint
    if (req.path === '/health') {
      return next();
    }
    if (!req.secure && (req.method === 'GET' || req.method === 'HEAD')) {
      return res.redirect(301, 'https://' + req.headers.host + req.originalUrl);
    }
    next();
  });
}
const server = http.createServer(app);
const io = socketio(server, {
  cors: {
    origin: [
      'http://localhost:3000',
      'http://localhost:3001', 
      'http://localhost:5173',
      'https://emotional-support-bot.netlify.app',
      process.env.FRONTEND_URL
    ].filter(Boolean),
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    credentials: true
  }
});

// Health endpoint (after app initialization)
app.get('/health', (req, res) => {
  const state = ['disconnected','connected','connecting','disconnecting'][mongoose.connection.readyState] || 'unknown';
  res.json({ status: 'ok', db: state, time: new Date().toISOString() });
});

// Get login sessions for a user (authenticated)
app.get('/api/auth/sessions', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const sessions = await LoginSession.find({ email })
      .sort({ loginAt: -1 })
      .limit(20)
      .select('-__v');
    
    res.json({
      success: true,
      sessions,
      count: sessions.length
    });
  } catch (error) {
    console.error('Error fetching sessions:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch sessions'
    });
  }
});

// Get all login sessions (admin endpoint - no auth for testing)
app.get('/api/auth/all-sessions', async (req, res) => {
  try {
    const sessions = await LoginSession.find()
      .sort({ loginAt: -1 })
      .limit(50)
      .select('-__v');
    
    res.json({
      success: true,
      sessions,
      count: sessions.length
    });
  } catch (error) {
    console.error('Error fetching all sessions:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch sessions'
    });
  }
});

// ==================== CHAT API ENDPOINTS ====================

// Get all conversations for a user
app.get('/api/chat/conversations/:userType/:userId', authenticateToken, async (req, res) => {
  try {
    const { userType, userId } = req.params;
    console.log(`[API] Fetching conversations for ${userType}:${userId}`);

    let query = {};
    if (userType === 'patient') {
      query.patientId = userId;
    } else {
      query.doctorId = userId;
    }

    const conversations = await Conversation.find(query)
      .sort({ lastMessageTime: -1 })
      .limit(50);

    res.json({
      success: true,
      conversations,
      count: conversations.length
    });
  } catch (error) {
    console.error('[API] Error fetching conversations:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch conversations'
    });
  }
});

// Get messages for a specific conversation
app.get('/api/chat/messages/:conversationId', authenticateToken, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { limit = 50, before } = req.query;
    
    console.log(`[API] Fetching messages for conversation ${conversationId}`);

    let query = { conversationId };
    if (before) {
      query.createdAt = { $lt: new Date(before) };
    }

    const messages = await Message.find(query)
      .sort({ createdAt: -1 })
      .limit(parseInt(limit));

    // Return in chronological order (oldest first)
    messages.reverse();

    res.json({
      success: true,
      messages,
      count: messages.length
    });
  } catch (error) {
    console.error('[API] Error fetching messages:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch messages'
    });
  }
});

// Send a message via REST API (alternative to Socket.IO)
app.post('/api/chat/send-message', authenticateToken, async (req, res) => {
  try {
    const { conversationId, senderId, senderType, receiverId, receiverType, messageType = 'text', content, voiceUrl, imageUrl } = req.body;

    if (!conversationId || !senderId || !receiverId || !content) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields'
      });
    }

    console.log(`[API] Sending message from ${senderType}:${senderId} to ${receiverType}:${receiverId}`);

    // Save message
    const message = new Message({
      conversationId,
      senderId,
      senderType,
      receiverId,
      receiverType,
      messageType,
      content,
      voiceUrl,
      imageUrl,
      isRead: false
    });
    await message.save();

    // Update conversation
    let conversation = await Conversation.findOne({ conversationId });
    if (!conversation) {
      conversation = new Conversation({
        conversationId,
        patientId: senderType === 'patient' ? senderId : receiverId,
        doctorId: senderType === 'doctor' ? senderId : receiverId,
        lastMessage: content,
        lastMessageTime: new Date(),
        lastMessageSender: senderType
      });
    } else {
      conversation.lastMessage = content;
      conversation.lastMessageTime = new Date();
      conversation.lastMessageSender = senderType;
      
      if (receiverType === 'patient') {
        conversation.unreadCountPatient += 1;
      } else {
        conversation.unreadCountDoctor += 1;
      }
    }
    await conversation.save();

    // Emit to receiver via Socket.IO if online
    const recipientSocketId = connectedUsers[`${receiverType}:${receiverId}`];
    if (recipientSocketId) {
      io.to(recipientSocketId).emit('chat:new-message', {
        _id: message._id,
        conversationId,
        senderId,
        senderType,
        receiverId,
        receiverType,
        messageType,
        content,
        voiceUrl,
        imageUrl,
        isRead: false,
        createdAt: message.createdAt
      });
    }

    res.json({
      success: true,
      message: {
        _id: message._id,
        conversationId,
        senderId,
        senderType,
        receiverId,
        receiverType,
        messageType,
        content,
        voiceUrl,
        imageUrl,
        isRead: false,
        createdAt: message.createdAt
      }
    });
  } catch (error) {
    console.error('[API] Error sending message:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to send message'
    });
  }
});

// Mark messages as read
app.post('/api/chat/mark-read', authenticateToken, async (req, res) => {
  try {
    const { conversationId, userId, userType } = req.body;

    console.log(`[API] Marking messages as read for ${userType}:${userId} in ${conversationId}`);

    await Message.updateMany(
      { conversationId, receiverId: userId, isRead: false },
      { isRead: true, readAt: new Date() }
    );

    const conversation = await Conversation.findOne({ conversationId });
    if (conversation) {
      if (userType === 'patient') {
        conversation.unreadCountPatient = 0;
      } else {
        conversation.unreadCountDoctor = 0;
      }
      await conversation.save();
    }

    res.json({
      success: true,
      message: 'Messages marked as read'
    });
  } catch (error) {
    console.error('[API] Error marking messages as read:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to mark messages as read'
    });
  }
});

// Create or get conversation between patient and doctor
app.post('/api/chat/create-conversation', authenticateToken, async (req, res) => {
  try {
    const { patientId, doctorId, patientName, doctorName } = req.body;

    console.log('[API] Create conversation request:', { patientId, doctorId, patientName, doctorName });

    if (!patientId || !doctorId) {
      return res.status(400).json({
        success: false,
        message: 'Patient ID and Doctor ID are required'
      });
    }

    // Validate ObjectId format
    if (!mongoose.Types.ObjectId.isValid(patientId) || !mongoose.Types.ObjectId.isValid(doctorId)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid patient or doctor ID format'
      });
    }

    // Create consistent conversation ID (smaller ID first)
    const ids = [patientId, doctorId].sort();
    const conversationId = `${ids[0]}:${ids[1]}`;

    console.log(`[API] Creating/Getting conversation ${conversationId}`);

    let conversation = await Conversation.findOne({ conversationId });
    
    if (!conversation) {
      console.log('[API] Creating new conversation...');
      conversation = new Conversation({
        conversationId,
        patientId,
        doctorId,
        patientName,
        doctorName,
        lastMessage: null,
        lastMessageTime: new Date(),
        unreadCountPatient: 0,
        unreadCountDoctor: 0
      });
      await conversation.save();
      console.log('[API] Conversation created successfully');
    } else {
      console.log('[API] Conversation already exists');
    }

    res.json({
      success: true,
      conversation
    });
  } catch (error) {
    console.error('[API] Error creating conversation:', error);
    console.error('[API] Error stack:', error.stack);
    res.status(500).json({
      success: false,
      message: 'Failed to create conversation',
      error: error.message
    });
  }
});

// ==================== END CHAT API ENDPOINTS ====================



io.on('connection', (socket) => {
  console.log(`Socket connected: ${socket.id}`);
  
  socket.on('register', ({ userId, userType }) => {
    if (userId && userType) {
      const key = `${userType}:${userId}`;
      connectedUsers[key] = socket.id;
      console.log(`User registered: ${key} -> ${socket.id}`);
      console.log('Current connected users:', Object.keys(connectedUsers));
    } else {
      console.log('Invalid registration data:', { userId, userType });
    }
  });
  
  socket.on('disconnect', () => {
    console.log(`Socket disconnected: ${socket.id}`);
    for (const key in connectedUsers) {
      if (connectedUsers[key] === socket.id) {
        console.log(`User unregistered: ${key}`);
        delete connectedUsers[key];
        break;
      }
    }
    console.log('Remaining connected users:', Object.keys(connectedUsers));
  });

  // Initiate call (alert recipient)
  socket.on('call:initiate', ({ toUserId, toUserType, fromUserId, fromUserType, callType }) => {
    const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
    if (recipientSocketId) {
      io.to(recipientSocketId).emit('call:incoming', {
        fromUserId,
        fromUserType,
        callType, // 'video' or 'audio'
      });
    } else {
      // Recipient offline, send push notification
      sendNotification({
        userId: toUserId,
        userType: toUserType === 'patient' ? 'User' : 'Doctor',
        message: `Incoming ${callType} call`,
        type: 'call',
        data: { fromUserId, fromUserType, callType }
      }).catch(e => console.error('Failed to notify offline recipient about call:', e.message));
    }
  });

  // ============================================
  // WebRTC Video Call Signaling
  // ============================================
  
  // Initiate call (caller sends offer)
  socket.on('call:offer', async ({ toUserId, toUserType, offer, fromUserId, fromUserType, bookingId }) => {
    try {
      console.log(`[WebRTC] Call offer from ${fromUserType}:${fromUserId} to ${toUserType}:${toUserId}`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      
      if (recipientSocketId) {
        // Recipient is online, send offer directly
        io.to(recipientSocketId).emit('call:offer', { 
          offer, 
          fromUserId, 
          fromUserType,
          bookingId 
        });
        console.log(`[WebRTC] Offer sent to online user ${toUserType}:${toUserId}`);
      } else {
        // Recipient offline, send push notification
        console.log(`[WebRTC] Recipient offline, sending push notification`);
        
        const callerName = fromUserType === 'Doctor' 
          ? (await Doctor.findById(fromUserId))?.name 
          : (await Patient.findById(fromUserId))?.name;
        
        await sendPushNotification({
          userId: toUserId,
          userType: toUserType,
          title: 'Incoming Video Call',
          body: `${callerName || 'Someone'} is calling you`,
          data: {
            type: 'video_call',
            callerId: String(fromUserId),
            callerType: fromUserType,
            bookingId: bookingId ? String(bookingId) : ''
          }
        });
        
        // Notify caller that recipient is offline
        socket.emit('call:recipient-offline', { toUserId, toUserType });
      }
    } catch (error) {
      console.error('[WebRTC] Error handling call offer:', error);
      socket.emit('call:error', { message: 'Failed to send call offer' });
    }
  });

  // Answer call (callee sends answer)
  socket.on('call:answer', ({ toUserId, toUserType, answer, fromUserId, fromUserType }) => {
    try {
      console.log(`[WebRTC] Call answer from ${fromUserType}:${fromUserId} to ${toUserType}:${toUserId}`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:answer', { 
          answer, 
          fromUserId, 
          fromUserType 
        });
        console.log(`[WebRTC] Answer sent to ${toUserType}:${toUserId}`);
      } else {
        console.log(`[WebRTC] Cannot send answer - recipient offline`);
        socket.emit('call:error', { message: 'Recipient is offline' });
      }
    } catch (error) {
      console.error('[WebRTC] Error handling call answer:', error);
    }
  });

  // Exchange ICE candidates
  socket.on('call:ice-candidate', ({ toUserId, toUserType, candidate, fromUserId, fromUserType }) => {
    try {
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:ice-candidate', { 
          candidate, 
          fromUserId, 
          fromUserType 
        });
        console.log(`[WebRTC] ICE candidate sent from ${fromUserType}:${fromUserId} to ${toUserType}:${toUserId}`);
      }
    } catch (error) {
      console.error('[WebRTC] Error handling ICE candidate:', error);
    }
  });

  // Call rejected by callee
  socket.on('call:reject', ({ toUserId, toUserType, fromUserId, fromUserType, reason }) => {
    try {
      console.log(`[WebRTC] Call rejected by ${fromUserType}:${fromUserId}. Reason: ${reason || 'none'}`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:rejected', { 
          fromUserId, 
          fromUserType,
          reason: reason || 'Call declined'
        });
      }
    } catch (error) {
      console.error('[WebRTC] Error handling call reject:', error);
    }
  });

  // End/hang up call
  socket.on('call:end', ({ toUserId, toUserType, fromUserId, fromUserType, duration }) => {
    try {
      console.log(`[WebRTC] Call ended by ${fromUserType}:${fromUserId}. Duration: ${duration || 0}s`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:end', { 
          fromUserId, 
          fromUserType,
          duration 
        });
      }
      
      // Acknowledge to caller
      socket.emit('call:ended', { success: true });
    } catch (error) {
      console.error('[WebRTC] Error handling call end:', error);
    }
  });

  // Call busy (callee is in another call)
  socket.on('call:busy', ({ toUserId, toUserType, fromUserId, fromUserType }) => {
    try {
      console.log(`[WebRTC] User busy: ${fromUserType}:${fromUserId}`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:busy', { 
          fromUserId, 
          fromUserType 
        });
      }
    } catch (error) {
      console.error('[WebRTC] Error handling call busy:', error);
    }
  });

  // Renegotiation (for adding/removing tracks)
  socket.on('call:renegotiate', ({ toUserId, toUserType, offer, fromUserId, fromUserType }) => {
    try {
      console.log(`[WebRTC] Renegotiation from ${fromUserType}:${fromUserId}`);
      
      const recipientSocketId = connectedUsers[`${toUserType}:${toUserId}`];
      if (recipientSocketId) {
        io.to(recipientSocketId).emit('call:renegotiate', { 
          offer, 
          fromUserId, 
          fromUserType 
        });
      }
    } catch (error) {
      console.error('[WebRTC] Error handling renegotiation:', error);
    }
  });

  // Chat: Send message
  socket.on('chat:send-message', async ({ conversationId, senderId, senderType, receiverId, receiverType, messageType, content, voiceUrl, imageUrl }) => {
    try {
      console.log(`[Chat] Message from ${senderType}:${senderId} to ${receiverType}:${receiverId}`);
      
      // Save message to database
      const message = new Message({
        conversationId,
        senderId,
        senderType,
        receiverId,
        receiverType,
        messageType,
        content,
        voiceUrl,
        imageUrl,
        isRead: false
      });
      await message.save();

      // Update or create conversation
      let conversation = await Conversation.findOne({ conversationId });
      if (!conversation) {
        conversation = new Conversation({
          conversationId,
          patientId: senderType === 'patient' ? senderId : receiverId,
          doctorId: senderType === 'doctor' ? senderId : receiverId,
          lastMessage: content,
          lastMessageTime: new Date(),
          lastMessageSender: senderType
        });
      } else {
        conversation.lastMessage = content;
        conversation.lastMessageTime = new Date();
        conversation.lastMessageSender = senderType;
        
        // Increment unread count for receiver
        if (receiverType === 'patient') {
          conversation.unreadCountPatient += 1;
        } else {
          conversation.unreadCountDoctor += 1;
        }
      }
      await conversation.save();

      // Emit to sender (confirmation)
      socket.emit('chat:message-sent', {
        _id: message._id,
        conversationId,
        senderId,
        senderType,
        receiverId,
        receiverType,
        messageType,
        content,
        voiceUrl,
        imageUrl,
        isRead: false,
        createdAt: message.createdAt
      });

      // Emit to receiver if online
      const recipientSocketId = connectedUsers[`${receiverType}:${receiverId}`];
      if (recipientSocketId) {
        console.log(`[Chat] Delivering message to ${receiverType}:${receiverId} via socket ${recipientSocketId}`);
        io.to(recipientSocketId).emit('chat:new-message', {
          _id: message._id,
          conversationId,
          senderId,
          senderType,
          receiverId,
          receiverType,
          messageType,
          content,
          voiceUrl,
          imageUrl,
          isRead: false,
          createdAt: message.createdAt
        });
      } else {
        console.log(`[Chat] Receiver ${receiverType}:${receiverId} is offline, message saved to DB`);
        try {
          await sendNotification({
            userId: receiverId,
            userType: receiverType === 'patient' ? 'User' : 'Doctor',
            message: content?.slice(0, 80) || (imageUrl ? 'Image' : voiceUrl ? 'Voice message' : 'New message'),
            type: 'chat',
            data: { conversationId, senderId, senderType, messageType, imageUrl: imageUrl || '', voiceUrl: voiceUrl || '' }
          });
        } catch (e) {
          console.error('[Chat] Failed to send offline notification:', e.message);
        }
      }
    } catch (error) {
      console.error('[Chat] Error sending message:', error);
      socket.emit('chat:error', { message: 'Failed to send message' });
    }
  });

  // Chat: Mark messages as read
  socket.on('chat:mark-read', async ({ conversationId, userId, userType }) => {
    try {
      console.log(`[Chat] Marking messages as read for ${userType}:${userId} in conversation ${conversationId}`);
      
      // Update messages to read
      await Message.updateMany(
        { conversationId, receiverId: userId, isRead: false },
        { isRead: true, readAt: new Date() }
      );

      // Reset unread count in conversation
      const conversation = await Conversation.findOne({ conversationId });
      if (conversation) {
        if (userType === 'patient') {
          conversation.unreadCountPatient = 0;
        } else {
          conversation.unreadCountDoctor = 0;
        }
        await conversation.save();
      }

      socket.emit('chat:marked-read', { conversationId });
    } catch (error) {
      console.error('[Chat] Error marking messages as read:', error);
    }
  });

  // Chat: Typing indicator
  socket.on('chat:typing', ({ conversationId, userId, userType, isTyping }) => {
    // Determine receiver type
    const receiverType = userType === 'patient' ? 'doctor' : 'patient';
    
    // Find receiver in conversation and emit typing status
    Conversation.findOne({ conversationId }).then(conversation => {
      if (conversation) {
        const receiverId = userType === 'patient' ? conversation.doctorId : conversation.patientId;
        const recipientSocketId = connectedUsers[`${receiverType}:${receiverId}`];
        
        if (recipientSocketId) {
          io.to(recipientSocketId).emit('chat:user-typing', {
            conversationId,
            userId,
            userType,
            isTyping
          });
        }
      }
    }).catch(err => {
      console.error('[Chat] Error handling typing indicator:', err);
    });
  });
});

// Helper to send notification
async function sendNotification({ userId, userType, message, type, data }) {
  try {
    console.log(`Sending notification to ${userType}:${userId}`, { message, type, data });
    
    // Save to DB
    const notification = new Notification({ userId, userType, message, type, data });
    await notification.save();
    console.log(`Notification saved to DB with ID: ${notification._id}`);
    
  // Emit via socket if user is online
    const socketId = connectedUsers[`${userType}:${userId}`];
    if (socketId) {
      console.log(`User ${userType}:${userId} is online, emitting via socket: ${socketId}`);
      io.to(socketId).emit('notification', {
        _id: notification._id,
        message,
        type,
        data,
        createdAt: notification.createdAt,
        isRead: false
      });
    } else {
      console.log(`User ${userType}:${userId} is offline, notification saved to DB only`);
    }

    // Also send push notification via FCM (best-effort)
    try {
      const title = type === 'chat' ? 'New message' : type === 'booking' ? 'Booking update' : 'Notification';
      await sendPushNotification({
        userId,
        userType,
        title,
        body: message,
        data: { type, ...(data || {}), notificationId: String(notification._id) }
      });
    } catch (e) {
      console.error('sendPushNotification error (non-blocking):', e.message);
    }

    return notification;
  } catch (error) {
    console.error('Error sending notification:', error);
    throw error;
  }
}

// Enable CORS for all routes
app.use((req, res, next) => {
  const allowedOrigins = [
    'http://localhost:3000',
    'http://localhost:3001',
    'http://localhost:5173',
    'https://emotional-support-bot.netlify.app',
    process.env.FRONTEND_URL
  ].filter(Boolean);
  
  const origin = req.headers.origin;
  if (allowedOrigins.includes(origin)) {
    res.header('Access-Control-Allow-Origin', origin);
  }
  
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Credentials', true);
  
  if (req.method === 'OPTIONS') {
    res.sendStatus(200);
  } else {
    next();
  }
});

// ============================================
// Security Middleware
// ============================================

// Helmet - Secure HTTP headers
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      scriptSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'", "https:"],
      fontSrc: ["'self'", "data:"],
      objectSrc: ["'none'"],
      mediaSrc: ["'self'"],
      frameSrc: ["'none'"],
    },
  },
  crossOriginEmbedderPolicy: false, // Allow CORS
  crossOriginResourcePolicy: { policy: "cross-origin" } // Allow cross-origin resources
}));

// Enable HSTS in production to force HTTPS for future requests
if (process.env.NODE_ENV === 'production') {
  app.use(helmet.hsts({
    maxAge: 60 * 60 * 24 * 180, // 180 days
    includeSubDomains: true,
    preload: false
  }));
}

// Rate Limiting Configuration
// General API rate limit: 100 requests per 15 minutes per IP
const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.',
  standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
  legacyHeaders: false, // Disable the `X-RateLimit-*` headers
  handler: (req, res) => {
    console.log(`[RATE-LIMIT] IP ${req.ip} exceeded general rate limit`);
    res.status(429).json({
      message: 'Too many requests. Please try again later.',
      retryAfter: Math.ceil(15 * 60) // seconds
    });
  }
});

// Strict rate limit for authentication endpoints: 5 requests per 15 minutes
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // Limit each IP to 5 login/OTP requests per windowMs
  message: 'Too many authentication attempts, please try again later.',
  skipSuccessfulRequests: false,
  handler: (req, res) => {
    console.log(`[RATE-LIMIT] IP ${req.ip} exceeded auth rate limit on ${req.path}`);
    res.status(429).json({
      message: 'Too many authentication attempts. Please try again after 15 minutes.',
      retryAfter: Math.ceil(15 * 60) // seconds
    });
  }
});

// Payment endpoints rate limit: 10 requests per hour
const paymentLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 10,
  message: 'Too many payment requests, please try again later.',
  handler: (req, res) => {
    console.log(`[RATE-LIMIT] IP ${req.ip} exceeded payment rate limit`);
    res.status(429).json({
      message: 'Too many payment requests. Please try again later.',
      retryAfter: Math.ceil(60 * 60) // seconds
    });
  }
});

// OTP rate limit: 3 requests per 10 minutes per email
const otpLimiter = rateLimit({
  windowMs: 10 * 60 * 1000, // 10 minutes
  max: 3,
  message: 'Too many OTP requests, please try again later.',
  handler: (req, res) => {
    console.log(`[RATE-LIMIT] IP ${req.ip} exceeded OTP rate limit`);
    res.status(429).json({
      message: 'Too many OTP requests. Please try again after 10 minutes.',
      retryAfter: Math.ceil(10 * 60) // seconds
    });
  }
});

// Apply general rate limiter to all routes
app.use(generalLimiter);

console.log('✓ Security middleware enabled (Helmet + Rate Limiting)');

app.use(express.json());

const JWT_SECRET = 'your_jwt_secret_key'; // Change this in production!

// Middleware to authenticate JWT and attach user info to req.user
function authenticateToken(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth) return res.status(401).json({ message: 'No token' });
  const token = auth.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ message: 'Invalid token' });
  }
}

// Register device token for push notifications
app.post('/api/notifications/register-token', authenticateToken, async (req, res) => {
  try {
    const { token, platform = 'android' } = req.body;
    if (!token) return res.status(400).json({ success: false, message: 'Token is required' });
    // Determine user from JWT
    const role = req.user.role === 'doctor' ? 'Doctor' : 'User';
    const user = role === 'Doctor' ? await Doctor.findOne({ email: req.user.email }) : await Patient.findOne({ email: req.user.email });
    if (!user) return res.status(404).json({ success: false, message: 'User not found' });
    await DeviceToken.findOneAndUpdate(
      { userId: user._id, userType: role, token },
      { token, platform, updatedAt: new Date() },
      { upsert: true }
    );
    res.json({ success: true, message: 'Token registered' });
  } catch (error) {
    console.error('Error registering device token:', error);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// Helper to send FCM push notifications (multicast)
async function sendPushNotification({ userId, userType, title, body, data }) {
  try {
    const tokens = await DeviceToken.find({ userId, userType }).select('token');
    if (!tokens.length) return;
    const multicast = {
      notification: { title: title || 'ReliefNet', body: body || '' },
      data: Object.fromEntries(Object.entries(data || {}).map(([k, v]) => [k, String(v)])),
      tokens: tokens.map(t => t.token)
    };
    const response = await admin.messaging().sendMulticast(multicast);
    console.log(`📲 Push sent to ${tokens.length} devices (${response.successCount} success, ${response.failureCount} failed)`);
  } catch (err) {
    console.error('Error sending push notification:', err.message);
  }
}

// Helper: Find patient by email
async function findPatientByEmail(email) {
  return Patient.findOne({ email });
}
// Helper: Find doctor by email
async function findDoctorByEmail(email) {
  return Doctor.findOne({ email });
}

// Patient Login (with rate limiting)
app.post('/api/patient/login', authLimiter, async (req, res) => {
  const { email, password } = req.body;
  const patient = await Patient.findOne({ email });
  if (!patient) return res.status(401).json({ message: 'Invalid credentials' });
  const valid = await bcrypt.compare(password, patient.password);
  if (!valid) return res.status(401).json({ message: 'Invalid credentials' });
  const token = jwt.sign({ email, name: patient.name, role: 'patient' }, JWT_SECRET, { expiresIn: '1h' });
  
  // Save login session to MongoDB
  try {
    await LoginSession.create({
      userId: patient._id,
      userType: 'patient',
      email: patient.email,
      name: patient.name,
      loginMethod: 'password',
      ipAddress: req.ip || req.connection.remoteAddress,
      userAgent: req.headers['user-agent'],
      expiresAt: new Date(Date.now() + 60 * 60 * 1000) // 1 hour from now
    });
    console.log(`✅ Login session saved for ${patient.email} (password)`);
  } catch (sessionError) {
    console.error('Failed to save login session:', sessionError.message);
  }
  
  res.json({ token, user: { email, name: patient.name, role: 'patient' } });
});

// OTP Login System for Patients

// Step 1: Send OTP to email (with rate limiting)
app.post('/api/auth/send-otp', otpLimiter, async (req, res) => {
  const { email } = req.body;
  
  if (!email) {
    return res.status(400).json({
      success: false,
      message: 'Email is required'
    });
  }
  
  // Check if patient exists (optional - you can allow OTP for new users too)
  const patient = await Patient.findOne({ email });
  if (!patient) {
    return res.status(404).json({
      success: false,
      message: 'No account found with this email. Please register first.'
    });
  }
  
  // Generate 6-digit OTP
  const otp = generateOTP();
  const expiresAt = Date.now() + 5 * 60 * 1000; // OTP expires in 5 minutes
  
  // Store OTP (in production, use Redis with TTL)
  otpStore.set(email, { otp, expiresAt });
  
  // Send OTP via email
  const emailSent = await sendOTPEmail(email, otp);
  
  if (!emailSent) {
    // Email sending failed, but still log to console for testing
    console.log(`⚠️ Email failed, but OTP for ${email}: ${otp} (expires in 5 minutes)`);
  }
  
  res.json({
    success: true,
    message: emailSent 
      ? 'OTP sent to your email' 
      : 'OTP generated (email sending failed, check console)',
    // FOR TESTING ONLY - Remove in production
    testOtp: process.env.NODE_ENV === 'development' ? otp : undefined
  });
});

// Step 2: Verify OTP and login (with rate limiting)
app.post('/api/auth/verify-otp', authLimiter, async (req, res) => {
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
  
  // Get patient data
  const patient = await Patient.findOne({ email });
  if (!patient) {
    return res.status(404).json({
      success: false,
      message: 'Patient not found'
    });
  }
  
  const token = jwt.sign({ email, name: patient.name, role: 'patient' }, JWT_SECRET, { expiresIn: '1h' });
  
  // Save login session to MongoDB
  try {
    await LoginSession.create({
      userId: patient._id,
      userType: 'patient',
      email: patient.email,
      name: patient.name,
      loginMethod: 'otp',
      ipAddress: req.ip || req.connection.remoteAddress,
      userAgent: req.headers['user-agent'],
      expiresAt: new Date(Date.now() + 60 * 60 * 1000) // 1 hour from now (matches JWT expiry)
    });
    console.log(`✅ Login session saved for ${patient.email} (OTP)`);
  } catch (sessionError) {
    console.error('Failed to save login session:', sessionError.message);
    // Don't fail the login if session save fails
  }
  
  res.json({
    success: true,
    token,
    user: {
      _id: patient._id,
      email: patient.email,
      name: patient.name,
      location: patient.location,
      role: 'patient'
    },
    message: 'Login successful via OTP'
  });
});

// Patient Signup
app.post('/api/patient/signup', async (req, res) => {
  const { email, password, name, location } = req.body;
  if (!email || !password || !name) return res.status(400).json({ message: 'Missing fields' });
  if (await findPatientByEmail(email)) return res.status(409).json({ message: 'Patient already exists' });
  const hashedPassword = await bcrypt.hash(password, 10);
  const patient = new Patient({ email, password: hashedPassword, name, location });
  await patient.save();
  const token = jwt.sign({ email, name, role: 'patient' }, JWT_SECRET, { expiresIn: '1h' });
  res.json({ token, user: { email, name, role: 'patient' } });
});

// GET /api/patient/profile - fetch current patient's profile
app.get('/api/patient/profile', authenticateToken, async (req, res) => {
  const { email } = req.user;
  const patient = await findPatientByEmail(email);
  if (!patient) return res.status(404).json({ message: 'Patient not found' });
  console.log('[PatientProfile][GET] photoUrl for', email, ':', patient.photoUrl);
  res.json({ 
    id: patient._id,
    name: patient.name, 
    email: patient.email, 
    location: patient.location,
    photoUrl: patient.photoUrl || null
  });
});

// PUT /api/patient/profile - update current patient's profile
app.put('/api/patient/profile', authenticateToken, async (req, res) => {
  const { email } = req.user;
  const { name, email: newEmail, location, photoUrl, phone } = req.body;
  const patient = await findPatientByEmail(email);
  if (!patient) return res.status(404).json({ message: 'Patient not found' });
  if (name) patient.name = name;
  if (location !== undefined) patient.location = location;
  if (photoUrl !== undefined) patient.photoUrl = photoUrl;
  if (phone !== undefined) patient.phone = phone;
  if (newEmail && newEmail !== patient.email) patient.email = newEmail;
  await patient.save();
  console.log('[PatientProfile][PUT] Updated photoUrl for', email, ':', patient.photoUrl);
  res.json({ 
    id: patient._id,
    name: patient.name, 
    email: patient.email, 
    location: patient.location,
    phone: patient.phone || null,
    photoUrl: patient.photoUrl || null
  });
});

// Helper function to generate unique Medical ID
async function generateMedicalId() {
  const prefix = 'RN'; // ReliefNet prefix
  let medicalId;
  let exists = true;
  
  // Keep generating until we find a unique one
  while (exists) {
    const randomNum = Math.floor(100000 + Math.random() * 900000); // 6-digit random number
    medicalId = `${prefix}-${randomNum}`;
    exists = await Doctor.findOne({ medicalId });
  }
  
  return medicalId;
}

// Doctor Signup (old endpoint for backward compatibility)
app.post('/api/doctor/signup', async (req, res) => {
  const { email, password, name, specialty, bio, location, medicalId, specialization } = req.body;
  if (!email || !password || !name) return res.status(400).json({ message: 'Missing fields' });
  if (await findDoctorByEmail(email)) return res.status(409).json({ message: 'Doctor already exists' });
  
  // Generate Medical ID if not provided
  const finalMedicalId = medicalId || await generateMedicalId();
  
  const hashedPassword = await bcrypt.hash(password, 10);
  const doctor = new Doctor({ email, password: hashedPassword, name, specialty, bio, location, medicalId: finalMedicalId, specialization });
  await doctor.save();
  const token = jwt.sign({ email, name, role: 'doctor' }, JWT_SECRET, { expiresIn: '1h' });
  res.json({ token, user: { email, name, specialty, bio, location, medicalId: finalMedicalId, specialization, role: 'doctor' } });
});

// Doctor Registration (new endpoint matching Android app) - with rate limiting
app.post('/api/doctors/register', authLimiter, async (req, res) => {
  const { firstName, lastName, email, phone, password, specialization, hospital, medicalId } = req.body;
  
  // Validate required fields
  if (!firstName || !lastName || !email || !password || !specialization) {
    return res.status(400).json({ message: 'Missing required fields' });
  }
  
  // ReliefNet is for MENTAL HEALTH ONLY - validate specialization
  const allowedSpecializations = [
    'Psychologist',
    'Therapist',
    'Psychiatrist',
    'Counselor',
    'Clinical Psychologist',
    'Mental Health Specialist',
    'Mental Health Counselor',
    'Licensed Therapist',
    'Licensed Clinical Social Worker',
    'Marriage and Family Therapist'
  ];
  
  if (!allowedSpecializations.includes(specialization)) {
    return res.status(400).json({ 
      message: 'ReliefNet is a mental health platform. Only mental health professionals can register.',
      allowedSpecializations: allowedSpecializations
    });
  }
  
  // Check if doctor already exists
  if (await findDoctorByEmail(email)) {
    return res.status(409).json({ message: 'Doctor with this email already exists' });
  }
  
  // Generate unique Medical ID (company provides this)
  const generatedMedicalId = await generateMedicalId();
  
  const hashedPassword = await bcrypt.hash(password, 10);
  const fullName = `${firstName} ${lastName}`;
  
  const doctor = new Doctor({ 
    email, 
    password: hashedPassword, 
    name: fullName,
    firstName,
    lastName,
    phone,
    specialty: specialization, // Map specialization to specialty
    specialization,
    location: hospital,
    hospital,
    medicalId: generatedMedicalId // Use generated ID
  });
  
  await doctor.save();
  
  const token = jwt.sign({ email, name: fullName, role: 'doctor' }, JWT_SECRET, { expiresIn: '1h' });

  // Fire-and-forget welcome email (do not block response)
  sendDoctorWelcomeEmail(email, fullName, generatedMedicalId).catch((e) => {
    console.error('Welcome email error (non-blocking):', e.message);
  });
  
  res.json({ 
    success: true,
    token, 
    doctor: { 
      id: doctor._id,
      email, 
      name: fullName,
      firstName,
      lastName,
      phone,
      specialty: specialization,
      specialization,
      location: hospital,
      hospital,
      medicalId: generatedMedicalId, // Return the generated Medical ID
      role: 'doctor' 
    },
    message: `Registration successful! Your Medical ID is: ${generatedMedicalId}`
  });
});

// Doctor Login (with rate limiting)
app.post('/api/doctor/login', authLimiter, async (req, res) => {
  const { email, password, medicalId } = req.body;
  
  // Allow login with either email or medicalId
  const doctor = medicalId 
    ? await Doctor.findOne({ medicalId })
    : await Doctor.findOne({ email });
    
  if (!doctor) return res.status(401).json({ message: 'Invalid credentials' });
  const valid = await bcrypt.compare(password, doctor.password);
  if (!valid) return res.status(401).json({ message: 'Invalid credentials' });
  const token = jwt.sign({ email: doctor.email, name: doctor.name, role: 'doctor', userId: doctor._id, userType: 'Doctor' }, JWT_SECRET, { expiresIn: '1h' });
  
  // Save login session to MongoDB
  try {
    await LoginSession.create({
      userId: doctor._id,
      userType: 'doctor',
      email: doctor.email,
      name: doctor.name,
      loginMethod: medicalId ? 'medical-id' : 'password',
      ipAddress: req.ip || req.connection.remoteAddress,
      userAgent: req.headers['user-agent'],
      expiresAt: new Date(Date.now() + 60 * 60 * 1000) // 1 hour from now
    });
    console.log(`✅ Login session saved for ${doctor.email} (${medicalId ? 'medical-id' : 'password'})`);
  } catch (sessionError) {
    console.error('Failed to save login session:', sessionError.message);
  }
  
  res.json({ token, user: { id: doctor._id, email: doctor.email, name: doctor.name, specialty: doctor.specialty, bio: doctor.bio, location: doctor.location, medicalId: doctor.medicalId, specialization: doctor.specialization, role: 'doctor', userType: 'Doctor' } });
});

// GET /api/doctor/profile - fetch current doctor's profile
app.get('/api/doctor/profile', authenticateToken, async (req, res) => {
  const { email } = req.user;
  const doctor = await findDoctorByEmail(email);
  if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
  res.json({
    id: doctor._id,
    name: doctor.name,
    email: doctor.email,
    specialty: doctor.specialty,
    bio: doctor.bio,
    location: doctor.location,
    medicalId: doctor.medicalId,
    specialization: doctor.specialization,
    userType: 'Doctor'
  });
});

// PUT /api/doctor/profile - update current doctor's profile
app.put('/api/doctor/profile', authenticateToken, async (req, res) => {
  const { email } = req.user;
  const { name, email: newEmail, specialty, bio, location, medicalId, specialization } = req.body;
  const doctor = await findDoctorByEmail(email);
  if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
  if (name) doctor.name = name;
  if (specialty !== undefined) doctor.specialty = specialty;
  if (bio !== undefined) doctor.bio = bio;
  if (location !== undefined) doctor.location = location;
  if (medicalId !== undefined) doctor.medicalId = medicalId;
  if (specialization !== undefined) doctor.specialization = specialization;
  if (newEmail && newEmail !== doctor.email) doctor.email = newEmail;
  await doctor.save();
  res.json({
    id: doctor._id,
    name: doctor.name,
    email: doctor.email,
    specialty: doctor.specialty,
    bio: doctor.bio,
    location: doctor.location,
    medicalId: doctor.medicalId,
    specialization: doctor.specialization,
    userType: 'Doctor'
  });
});

// Doctor utilities and endpoints for app pages
// GET /api/doctor/chats - list of patient conversations for the logged in doctor
app.get('/api/doctor/chats', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const conversations = await Conversation.find({ doctorId: doctor._id })
      .sort({ lastMessageTime: -1 })
      .select('patientId patientName lastMessage lastMessageTime unreadCountDoctor conversationId');
    res.json({ success: true, conversations });
  } catch (err) {
    console.error('Error fetching doctor chats:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// GET /api/doctor/feedback - list of feedback items for logged in doctor
app.get('/api/doctor/feedback', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const items = await Feedback.find({ doctorId: doctor._id }).sort({ createdAt: -1 }).limit(100);
    const avg = items.length ? (items.reduce((a, b) => a + (b.rating || 0), 0) / items.length) : 0;
    res.json({ success: true, averageRating: Math.round(avg * 10) / 10, count: items.length, feedback: items });
  } catch (err) {
    console.error('Error fetching feedback:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// GET /api/doctor/payments - summarized list of payments to this doctor
app.get('/api/doctor/payments', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const payments = await Payment.find({ professionalId: doctor._id })
      .sort({ createdAt: -1 })
      .limit(200);
    const totalPaid = payments.filter(p => p.status === 'succeeded').reduce((s, p) => s + (p.amount || 0), 0);
    const totalPending = payments.filter(p => p.status !== 'succeeded').reduce((s, p) => s + (p.amount || 0), 0);
    res.json({ success: true, totalPaid, totalPending, payments });
  } catch (err) {
    console.error('Error fetching doctor payments:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// Google Auth Endpoint
app.post('/api/auth/google', async (req, res) => {
  const { idToken, photoUrl } = req.body;
  console.log('[GoogleAuth] Received photoUrl from client:', photoUrl);
  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const { email, name, picture } = decodedToken;
    
    // Use photo from token or request body
    const userPhotoUrl = photoUrl || picture || null;
    console.log('[GoogleAuth] Using userPhotoUrl:', userPhotoUrl);
    
    // Find or create user in MongoDB
    let user = await findPatientByEmail(email);
    if (!user) {
      user = new Patient({ 
        email, 
        name,
        photoUrl: userPhotoUrl
      });
      await user.save();
    } else {
      // Update existing user with latest info
      if (name && user.name !== name) user.name = name;
      if (userPhotoUrl) {
        console.log('[GoogleAuth] Updating photoUrl from', user.photoUrl, 'to', userPhotoUrl);
        user.photoUrl = userPhotoUrl;
      }
      await user.save();
      console.log('[GoogleAuth] User saved with photoUrl:', user.photoUrl);
    }
    
    // Issue your own JWT with user type
    const token = jwt.sign({ 
      email, 
      name: user.name,
      userType: 'User',
      userId: user._id 
    }, JWT_SECRET, { expiresIn: '7d' });
    
    res.json({ 
      token, 
      user: { 
        _id: user._id,
        email: user.email, 
        name: user.name,
        photoUrl: user.photoUrl,
        location: user.location,
        userType: 'User'
      } 
    });
  } catch (err) {
    console.error('Google auth error:', err);
    res.status(401).json({ message: 'Invalid Google token' });
  }
});

// Example protected route
app.get('/api/protected', (req, res) => {
  const auth = req.headers.authorization;
  if (!auth) return res.status(401).json({ message: 'No token' });
  const token = auth.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    res.json({ message: 'Protected data', user: decoded });
  } catch (err) {
    res.status(401).json({ message: 'Invalid token' });
  }
});

// Professional Listing APIs

// GET /api/professionals - Get mental health professionals with filtering
app.get('/api/professionals', async (req, res) => {
  try {
    const { 
      category, 
      specialization, 
      location, 
      minRating, 
      maxPrice, 
      availability,
      limit = 20,
      page = 1
    } = req.query;

    // Define mental health specializations
    const mentalHealthSpecializations = [
      'Clinical Psychologist',
      'Counseling Psychologist', 
      'Psychiatrist',
      'Mental Health Therapist',
      'Child Psychologist',
      'Family Therapist',
      'Addiction Counselor',
      'Trauma Therapist',
      'Anxiety & Depression Specialist',
      'Cognitive Behavioral Therapist',
      'Art Therapist',
      'Music Therapist',
      'Mindfulness Coach',
      'Life Coach',
      'Crisis Counselor',
      'psychiatry', // Include lowercase variations
      'psychologist',
      'therapist',
      'counselor',
      'mental health'
    ];

    let query = { 
      isActive: true,
      $or: [
        { specialization: { $in: mentalHealthSpecializations } },
        { specializations: { $in: mentalHealthSpecializations } },
        { category: { $in: mentalHealthSpecializations } },
        { specialization: { $regex: /psych|therap|counsel|mental/i } }, // Regex for partial matches
        { specializations: { $regex: /psych|therap|counsel|mental/i } }
      ]
    };

    // Filter by category/specialization
    if (category) {
      query.specialization = { $regex: category, $options: 'i' };
    }

    // Filter by specific specialization
    if (specialization) {
      query.specializations = { $in: [new RegExp(specialization, 'i')] };
    }

    // Filter by location
    if (location) {
      query.location = { $regex: location, $options: 'i' };
    }

    // Filter by minimum rating
    if (minRating) {
      query.rating = { $gte: parseFloat(minRating) };
    }

    // Filter by maximum price
    if (maxPrice) {
      query.price = { $lte: parseFloat(maxPrice) };
    }

    // Filter by availability
    if (availability) {
      query.availability = availability;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const professionals = await Doctor.find(query)
      .select('-password -__v')
      .sort({ rating: -1, reviewCount: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    const total = await Doctor.countDocuments(query);

    res.json({
      professionals,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + professionals.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching professionals:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/categories - Get mental health categories
app.get('/api/professionals/categories', async (req, res) => {
  try {
    // Focus on mental health categories only
    const mentalHealthCategories = [
      'Clinical Psychologist',
      'Counseling Psychologist', 
      'Psychiatrist',
      'Mental Health Therapist',
      'Child Psychologist',
      'Family Therapist',
      'Addiction Counselor',
      'Trauma Therapist',
      'Anxiety & Depression Specialist',
      'Cognitive Behavioral Therapist',
      'Art Therapist',
      'Music Therapist',
      'Mindfulness Coach',
      'Life Coach',
      'Crisis Counselor'
    ];
    res.json({ categories: mentalHealthCategories });
  } catch (error) {
    console.error('Error fetching categories:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/search - Search mental health professionals
app.get('/api/professionals/search', async (req, res) => {
  try {
    const { q, limit = 10 } = req.query;
    
    if (!q) {
      return res.status(400).json({ message: 'Search query required' });
    }

    // Define mental health specializations
    const mentalHealthSpecializations = [
      'Clinical Psychologist',
      'Counseling Psychologist', 
      'Psychiatrist',
      'Mental Health Therapist',
      'Child Psychologist',
      'Family Therapist',
      'Addiction Counselor',
      'Trauma Therapist',
      'Anxiety & Depression Specialist',
      'Cognitive Behavioral Therapist',
      'Art Therapist',
      'Music Therapist',
      'Mindfulness Coach',
      'Life Coach',
      'Crisis Counselor'
    ];

          const searchQuery = {
        isActive: true,
        $or: [
          { specialization: { $in: mentalHealthSpecializations } },
          { specializations: { $in: mentalHealthSpecializations } },
          { category: { $in: mentalHealthSpecializations } },
          { specialization: { $regex: /psych|therap|counsel|mental/i } },
          { specializations: { $regex: /psych|therap|counsel|mental/i } }
        ],
        $and: [
          {
            $or: [
              { name: { $regex: q, $options: 'i' } },
              { specialization: { $regex: q, $options: 'i' } },
              { title: { $regex: q, $options: 'i' } },
              { specializations: { $in: [new RegExp(q, 'i')] } },
              { location: { $regex: q, $options: 'i' } }
            ]
          }
        ]
      };

    const professionals = await Doctor.find(searchQuery)
      .select('-password -__v')
      .sort({ rating: -1 })
      .limit(parseInt(limit));

    res.json(professionals);
  } catch (error) {
    console.error('Error searching professionals:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/:id - Get specific professional details
app.get('/api/professionals/:id', async (req, res) => {
  try {
    const professional = await Doctor.findById(req.params.id)
      .select('-password -__v');
    
    if (!professional) {
      return res.status(404).json({ message: 'Professional not found' });
    }

    res.json(professional);
  } catch (error) {
    console.error('Error fetching professional:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/nearby - Get mental health professionals near user location
app.get('/api/professionals/nearby', async (req, res) => {
  try {
    const { lat, lng, radius = 10, limit = 20 } = req.query;
    
    if (!lat || !lng) {
      return res.status(400).json({ message: 'Latitude and longitude required' });
    }

    // Define mental health specializations
    const mentalHealthSpecializations = [
      'Clinical Psychologist',
      'Counseling Psychologist', 
      'Psychiatrist',
      'Mental Health Therapist',
      'Child Psychologist',
      'Family Therapist',
      'Addiction Counselor',
      'Trauma Therapist',
      'Anxiety & Depression Specialist',
      'Cognitive Behavioral Therapist',
      'Art Therapist',
      'Music Therapist',
      'Mindfulness Coach',
      'Life Coach',
      'Crisis Counselor'
    ];

    // Simple distance calculation (for production, use proper geospatial queries)
    const professionals = await Doctor.find({ 
      isActive: true,
      $or: [
        { specialization: { $in: mentalHealthSpecializations } },
        { specializations: { $in: mentalHealthSpecializations } },
        { category: { $in: mentalHealthSpecializations } }
      ]
    })
      .select('-password -__v')
      .limit(parseInt(limit));

    // Calculate distances and filter by radius
    const nearbyProfessionals = professionals
      .map(prof => {
        if (prof.coordinates && prof.coordinates.latitude && prof.coordinates.longitude) {
          const distance = calculateDistance(
            parseFloat(lat), 
            parseFloat(lng), 
            prof.coordinates.latitude, 
            prof.coordinates.longitude
          );
          return { ...prof.toObject(), distance };
        }
        return prof;
      })
      .filter(prof => prof.distance <= parseFloat(radius))
      .sort((a, b) => a.distance - b.distance);

    res.json(nearbyProfessionals);
  } catch (error) {
    console.error('Error fetching nearby professionals:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Helper function to calculate distance between two points
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Radius of the Earth in kilometers
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

// Configure multer for local storage (for demo; use S3/Cloudinary for production)
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, 'uploads/'),
  filename: (req, file, cb) => cb(null, Date.now() + path.extname(file.originalname))
});
const upload = multer({ storage });

// Serve static files from the 'uploads' directory
app.use('/uploads', express.static('uploads'));

// Upload API - upload single file and return public URL
// Field name: 'file'
app.post('/api/upload', authenticateToken, upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: 'No file uploaded' });
    }
    const baseUrl = `${req.protocol}://${req.get('host')}`;
    const url = `${baseUrl}/uploads/${req.file.filename}`;
    return res.json({ url, filename: req.file.filename });
  } catch (error) {
    console.error('Error uploading file:', error);
    return res.status(500).json({ message: 'Failed to upload file' });
  }
});

// Booking System APIs

// Booking Schema
const bookingSchema = new mongoose.Schema({
  patientId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  professionalId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  date: { type: Date, required: true },
  time: { type: String, required: true },
  duration: { type: Number, default: 60 }, // in minutes
  type: { type: String, default: 'consultation' }, // consultation, therapy, etc.
  status: { 
    type: String, 
    enum: ['pending', 'confirmed', 'completed', 'cancelled', 'no-show'],
    default: 'pending'
  },
  price: { type: Number, required: true },
  paymentStatus: { 
    type: String, 
    enum: ['pending', 'paid', 'refunded'],
    default: 'pending'
  },
  notes: String,
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

const Booking = mongoose.model('Booking', bookingSchema);

// POST /api/bookings - Create a new booking
app.post('/api/bookings', authenticateToken, async (req, res) => {
  try {
    const { professionalId, date, time, duration, type, notes, sessionId } = req.body;
    const { email } = req.user;

    // Validate required fields
    if (!professionalId || !date || !time) {
      return res.status(400).json({ message: 'Missing required fields' });
    }

    // Get patient details
    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    // Get professional details
    const professional = await Doctor.findById(professionalId);
    if (!professional) {
      return res.status(404).json({ message: 'Professional not found' });
    }

    // Check if professional is available
    if (professional.availability === 'Unavailable') {
      return res.status(400).json({ message: 'Professional is currently unavailable' });
    }

    // Check for booking conflicts
    const bookingDate = new Date(date);
    const existingBooking = await Booking.findOne({
      professionalId,
      date: {
        $gte: new Date(bookingDate.getTime() - duration * 60000),
        $lte: new Date(bookingDate.getTime() + duration * 60000)
      },
      status: { $in: ['pending', 'confirmed'] }
    });

    if (existingBooking) {
      return res.status(400).json({ message: 'Time slot is already booked' });
    }

    // If sessionId is provided, mark the session as booked
    if (sessionId) {
      const session = await Session.findById(sessionId);
      if (!session) {
        return res.status(404).json({ message: 'Session not found' });
      }
      if (session.status !== 'available') {
        return res.status(400).json({ message: 'Session is not available' });
      }
      session.status = 'booked';
      session.updatedAt = new Date();
      await session.save();
    }

    // Create booking
    const booking = new Booking({
      patientId: patient._id,
      professionalId,
      date: bookingDate,
      time,
      duration: duration || 60,
      type: type || 'consultation',
      price: professional.price,
      notes
    });

    await booking.save();

    // Populate professional details for response
    await booking.populate('professionalId', 'name title specialization');

    // Notify doctor
    await sendNotification({
      userId: professional._id,
      userType: 'Doctor',
      message: `New booking from ${patient.name} for ${date} at ${time}`,
      type: 'booking',
      data: { bookingId: booking._id }
    });

    res.status(201).json({
      message: 'Booking created successfully',
      booking
    });
  } catch (error) {
    console.error('Error creating booking:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/bookings - Get user's bookings
app.get('/api/bookings', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { status, page = 1, limit = 10 } = req.query;

    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    let query = { patientId: patient._id };
    if (status) {
      query.status = status;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const bookings = await Booking.find(query)
      .populate('professionalId', 'name title specialization photoUrl')
      .sort({ date: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    const total = await Booking.countDocuments(query);

    res.json({
      bookings,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + bookings.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching bookings:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Alias: GET /api/patient/bookings - Get current patient's bookings (same as /api/bookings)
app.get('/api/patient/bookings', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { status, page = 1, limit = 10 } = req.query;

    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    let query = { patientId: patient._id };
    if (status) {
      query.status = status;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    const bookings = await Booking.find(query)
      .populate('professionalId', 'name title specialization photoUrl')
      .sort({ date: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    const total = await Booking.countDocuments(query);

    res.json({
      bookings,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + bookings.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching patient bookings:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/bookings/:id - Get specific booking
app.get('/api/bookings/:id', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    
    const booking = await Booking.findOne({
      _id: req.params.id,
      patientId: patient._id
    }).populate('professionalId', 'name title specialization photoUrl email phoneNumber');

    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }

    res.json(booking);
  } catch (error) {
    console.error('Error fetching booking:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/bookings/:id - Update booking status
app.put('/api/bookings/:id', authenticateToken, async (req, res) => {
  try {
    const { status, notes } = req.body;
    const { email } = req.user;
    
    const patient = await Patient.findOne({ email });
    const booking = await Booking.findOne({
      _id: req.params.id,
      patientId: patient._id
    });

    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }

    if (status) booking.status = status;
    if (notes !== undefined) booking.notes = notes;
    booking.updatedAt = Date.now();

    await booking.save();
    await booking.populate('professionalId', 'name title specialization');

    // Notify doctor and patient
    await sendNotification({
      userId: booking.professionalId._id,
      userType: 'Doctor',
      message: `Booking updated by patient: status is now ${booking.status}`,
      type: 'booking',
      data: { bookingId: booking._id, status: booking.status }
    });
    await sendNotification({
      userId: booking.patientId,
      userType: 'User',
      message: `Your booking status updated to ${booking.status}`,
      type: 'booking',
      data: { bookingId: booking._id, status: booking.status }
    });

    res.json({
      message: 'Booking updated successfully',
      booking
    });
  } catch (error) {
    console.error('Error updating booking:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// DELETE /api/bookings/:id - Cancel booking
app.delete('/api/bookings/:id', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    
    const booking = await Booking.findOne({
      _id: req.params.id,
      patientId: patient._id
    });

    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }

    if (booking.status === 'completed') {
      return res.status(400).json({ message: 'Cannot cancel completed booking' });
    }

    booking.status = 'cancelled';
    booking.updatedAt = Date.now();
    await booking.save();

    // Notify doctor and patient
    await sendNotification({
      userId: booking.professionalId,
      userType: 'Doctor',
      message: `Booking cancelled by patient`,
      type: 'booking',
      data: { bookingId: booking._id }
    });
    await sendNotification({
      userId: booking.patientId,
      userType: 'User',
      message: `Your booking was cancelled`,
      type: 'booking',
      data: { bookingId: booking._id }
    });

    res.json({ message: 'Booking cancelled successfully' });
  } catch (error) {
    console.error('Error cancelling booking:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/:id/bookings - Get professional's bookings (for doctors)
app.get('/api/professionals/:id/bookings', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { status, page = 1, limit = 10 } = req.query;

    // Check if user is the professional
    const professional = await Doctor.findOne({ email });
    if (!professional || professional._id.toString() !== req.params.id) {
      return res.status(403).json({ message: 'Unauthorized' });
    }

    let query = { professionalId: req.params.id };
    if (status) {
      query.status = status;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const bookings = await Booking.find(query)
      .populate('patientId', 'name email')
      .sort({ date: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    const total = await Booking.countDocuments(query);

    res.json({
      bookings,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + bookings.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching professional bookings:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Chat System APIs
// Note: Chat models (Message, Conversation) are now in separate files in models/ directory

// Payment Schema
const paymentSchema = new mongoose.Schema({
  bookingId: { type: mongoose.Schema.Types.ObjectId, ref: 'Booking', required: true },
  patientId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  professionalId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  amount: { type: Number, required: true },
  currency: { type: String, default: 'INR' },
  phonepeTransactionId: { type: String, required: true, unique: true }, // PhonePe Transaction ID
  phonepeMerchantTransactionId: { type: String, required: true }, // Our generated transaction ID
  status: { 
    type: String, 
    enum: ['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'],
    default: 'PENDING'
  },
  paymentMethod: { type: String, default: 'phonepe' },
  paymentInstrument: String, // UPI, CARD, NETBANKING, etc.
  doctorAmount: Number, // Amount to be transferred to doctor
  platformFee: Number, // Platform commission
  receiptUrl: String,
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

const Payment = mongoose.model('Payment', paymentSchema);

// POST /api/chats - Create a new chat session
app.post('/api/chats', authenticateToken, async (req, res) => {
  try {
    const { professionalId } = req.body;
    const { email } = req.user;

    if (!professionalId) {
      return res.status(400).json({ message: 'Professional ID is required' });
    }

    // Get patient details
    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    // Get professional details
    const professional = await Doctor.findById(professionalId);
    if (!professional) {
      return res.status(404).json({ message: 'Professional not found' });
    }

    // Check if chat session already exists
    let chatSession = await ChatSession.findOne({
      patientId: patient._id,
      professionalId,
      status: 'active'
    });

    if (chatSession) {
      return res.json({
        message: 'Chat session already exists',
        chatSession: {
          ...chatSession.toObject(),
          professional: {
            name: professional.name,
            title: professional.title,
            photoUrl: professional.photoUrl
          }
        }
      });
    }

    // Create new chat session
    chatSession = new ChatSession({
      patientId: patient._id,
      professionalId
    });

    await chatSession.save();

    // Populate professional details
    await chatSession.populate('professionalId', 'name title specialization photoUrl');

    res.status(201).json({
      message: 'Chat session created successfully',
      chatSession
    });
  } catch (error) {
    console.error('Error creating chat session:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/chats - Get user's chat sessions
app.get('/api/chats', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { status, page = 1, limit = 10 } = req.query;

    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    let query = { patientId: patient._id };
    if (status) {
      query.status = status;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const chatSessions = await ChatSession.find(query)
      .populate('professionalId', 'name title specialization photoUrl')
      .sort({ lastMessageAt: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    // Get last message for each chat session
    const chatSessionsWithLastMessage = await Promise.all(
      chatSessions.map(async (session) => {
        const lastMessage = await Message.findOne({ chatSessionId: session._id })
          .sort({ createdAt: -1 })
          .limit(1);
        
        return {
          ...session.toObject(),
          lastMessage: lastMessage ? {
            content: lastMessage.content,
            createdAt: lastMessage.createdAt,
            senderType: lastMessage.senderType
          } : null
        };
      })
    );

    const total = await ChatSession.countDocuments(query);

    res.json({
      chatSessions: chatSessionsWithLastMessage,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + chatSessions.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching chat sessions:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/chats/:id - Get specific chat session with messages
app.get('/api/chats/:id', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { page = 1, limit = 50 } = req.query;

    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    const chatSession = await ChatSession.findOne({
      _id: req.params.id,
      patientId: patient._id
    }).populate('professionalId', 'name title specialization photoUrl');

    if (!chatSession) {
      return res.status(404).json({ message: 'Chat session not found' });
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const messages = await Message.find({ chatSessionId: req.params.id })
      .sort({ createdAt: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    const total = await Message.countDocuments({ chatSessionId: req.params.id });

    res.json({
      chatSession,
      messages: messages.reverse(), // Show oldest first
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + messages.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching chat session:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// POST /api/chats/:id/messages - Send a message
app.post('/api/chats/:id/messages', authenticateToken, async (req, res) => {
  try {
    const { content, messageType = 'text', fileUrl } = req.body;
    const { email } = req.user;

    if (!content) {
      return res.status(400).json({ message: 'Message content is required' });
    }

    const patient = await Patient.findOne({ email });
    const doctor = await Doctor.findOne({ email });
    let sender, senderType, recipientId, recipientType, recipientName;
    if (patient) {
      sender = patient;
      senderType = 'patient';
      // Find the chat session to get the professionalId
      const chatSession = await ChatSession.findOne({ _id: req.params.id, patientId: patient._id, status: 'active' });
      if (!chatSession) {
        return res.status(404).json({ message: 'Chat session not found or inactive' });
      }
      recipientId = chatSession.professionalId;
      recipientType = 'Doctor';
      const doctorObj = await Doctor.findById(recipientId);
      recipientName = doctorObj ? doctorObj.name : '';
    } else if (doctor) {
      sender = doctor;
      senderType = 'professional';
      // Find the chat session to get the patientId
      const chatSession = await ChatSession.findOne({ _id: req.params.id, professionalId: doctor._id, status: 'active' });
      if (!chatSession) {
        return res.status(404).json({ message: 'Chat session not found or inactive' });
      }
      recipientId = chatSession.patientId;
      recipientType = 'User';
      const patientObj = await Patient.findById(recipientId);
      recipientName = patientObj ? patientObj.name : '';
    } else {
      return res.status(404).json({ message: 'Sender not found' });
    }

    // Create message
    const message = new Message({
      chatSessionId: req.params.id,
      senderId: sender._id,
      senderType,
      content,
      messageType,
      fileUrl
    });

    await message.save();

    // Update chat session last message time
    await ChatSession.findByIdAndUpdate(req.params.id, { lastMessageAt: Date.now() });

    // Send notification to recipient
    await sendNotification({
      userId: recipientId,
      userType: recipientType,
      message: `New chat message from ${sender.name}: ${content.slice(0, 50)}`,
      type: 'chat',
      data: { chatSessionId: req.params.id, senderName: sender.name }
    });

    res.status(201).json({
      message: 'Message sent successfully',
      message: {
        ...message.toObject(),
        sender: {
          name: sender.name,
          type: senderType
        }
      }
    });
  } catch (error) {
    console.error('Error sending message:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/chats/:id/messages/:messageId/read - Mark message as read
app.put('/api/chats/:id/messages/:messageId/read', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    
    const message = await Message.findOne({
      _id: req.params.messageId,
      chatSessionId: req.params.id,
      senderType: 'professional' // Only mark professional messages as read
    });

    if (!message) {
      return res.status(404).json({ message: 'Message not found' });
    }

    message.isRead = true;
    await message.save();

    res.json({ message: 'Message marked as read' });
  } catch (error) {
    console.error('Error marking message as read:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/chats/:id/end - End chat session
app.put('/api/chats/:id/end', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    
    const chatSession = await ChatSession.findOne({
      _id: req.params.id,
      patientId: patient._id
    });

    if (!chatSession) {
      return res.status(404).json({ message: 'Chat session not found' });
    }

    chatSession.status = 'ended';
    chatSession.updatedAt = Date.now();
    await chatSession.save();

    res.json({ message: 'Chat session ended successfully' });
  } catch (error) {
    console.error('Error ending chat session:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/professionals/:id/chats - Get professional's chat sessions
app.get('/api/professionals/:id/chats', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const { status, page = 1, limit = 10 } = req.query;

    // Check if user is the professional
    const professional = await Doctor.findOne({ email });
    if (!professional || professional._id.toString() !== req.params.id) {
      return res.status(403).json({ message: 'Unauthorized' });
    }

    let query = { professionalId: req.params.id };
    if (status) {
      query.status = status;
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const chatSessions = await ChatSession.find(query)
      .populate('patientId', 'name email')
      .sort({ lastMessageAt: -1 })
      .limit(parseInt(limit))
      .skip(skip);

    // Get last message for each chat session
    const chatSessionsWithLastMessage = await Promise.all(
      chatSessions.map(async (session) => {
        const lastMessage = await Message.findOne({ chatSessionId: session._id })
          .sort({ createdAt: -1 })
          .limit(1);
        
        return {
          ...session.toObject(),
          lastMessage: lastMessage ? {
            content: lastMessage.content,
            createdAt: lastMessage.createdAt,
            senderType: lastMessage.senderType
          } : null
        };
      })
    );

    const total = await ChatSession.countDocuments(query);

    res.json({
      chatSessions: chatSessionsWithLastMessage,
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / parseInt(limit)),
        hasMore: skip + chatSessions.length < total
      }
    });
  } catch (error) {
    console.error('Error fetching professional chat sessions:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Doctor Session Management Endpoints

// POST /api/doctor/sessions - Doctor creates a session
app.post('/api/doctor/sessions', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const { date, time, duration, type } = req.body;
    if (!date || !time || !duration || !type) return res.status(400).json({ message: 'Missing fields' });
    const session = new Session({ doctor: doctor._id, date, time, duration, type });
    await session.save();
    res.json({ message: 'Session created', session });
  } catch (err) {
    console.error('Error creating session:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/doctor/sessions - Doctor fetches their sessions, or patient fetches sessions for a doctor
app.get('/api/doctor/sessions', async (req, res) => {
  try {
    const { doctorId } = req.query;
    let query = {};
    if (doctorId) {
      // Patient fetching sessions for a doctor
      query.doctor = doctorId;
      query.status = 'available';
    } else {
      // Doctor fetching their own sessions (auth required)
      const auth = req.headers.authorization;
      if (!auth) return res.status(401).json({ message: 'No token' });
      const token = auth.split(' ')[1];
      let decoded;
      try {
        decoded = jwt.verify(token, JWT_SECRET);
      } catch (err) {
        return res.status(401).json({ message: 'Invalid token' });
      }
      if (decoded.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
      const doctor = await Doctor.findOne({ email: decoded.email });
      if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
      query.doctor = doctor._id;
    }
    const sessions = await Session.find(query).sort({ date: 1, time: 1 });
    res.json({ sessions });
  } catch (err) {
    console.error('Error fetching sessions:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/doctor/sessions/:id - Doctor updates a session
app.put('/api/doctor/sessions/:id', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const session = await Session.findById(req.params.id);
    if (!session) return res.status(404).json({ message: 'Session not found' });
    if (String(session.doctor) !== String(doctor._id)) return res.status(403).json({ message: 'Forbidden' });
    const { date, time, duration, type, status } = req.body;
    if (date !== undefined) session.date = date;
    if (time !== undefined) session.time = time;
    if (duration !== undefined) session.duration = duration;
    if (type !== undefined) session.type = type;
    if (status !== undefined) session.status = status;
    session.updatedAt = new Date();
    await session.save();

    // Notify all patients with bookings for this session
    const bookings = await Booking.find({ sessionId: session._id, status: { $in: ['pending', 'confirmed'] } });
    for (const booking of bookings) {
      await sendNotification({
        userId: booking.patientId,
        userType: 'User',
        message: `A session you booked has been updated by the doctor.`,
        type: 'session',
        data: { sessionId: session._id }
      });
    }

    res.json({ message: 'Session updated', session });
  } catch (err) {
    console.error('Error updating session:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// DELETE /api/doctor/sessions/:id - Doctor deletes a session
app.delete('/api/doctor/sessions/:id', authenticateToken, async (req, res) => {
  try {
    if (req.user.role !== 'doctor') return res.status(403).json({ message: 'Forbidden' });
    const doctor = await Doctor.findOne({ email: req.user.email });
    if (!doctor) return res.status(404).json({ message: 'Doctor not found' });
    const session = await Session.findById(req.params.id);
    if (!session) return res.status(404).json({ message: 'Session not found' });
    if (String(session.doctor) !== String(doctor._id)) return res.status(403).json({ message: 'Forbidden' });

    // Notify all patients with bookings for this session
    const bookings = await Booking.find({ sessionId: session._id, status: { $in: ['pending', 'confirmed'] } });
    for (const booking of bookings) {
      await sendNotification({
        userId: booking.patientId,
        userType: 'User',
        message: `A session you booked has been cancelled by the doctor.`,
        type: 'session',
        data: { sessionId: session._id }
      });
    }

    await session.deleteOne();

    res.json({ message: 'Session deleted' });
  } catch (err) {
    console.error('Error deleting session:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/notifications - Get all notifications for logged-in user
app.get('/api/notifications', authenticateToken, async (req, res) => {
  try {
    let userType = req.user.role === 'doctor' ? 'Doctor' : 'User';
    let user = null;
    if (userType === 'Doctor') {
      user = await Doctor.findOne({ email: req.user.email });
    } else {
      user = await Patient.findOne({ email: req.user.email });
    }
    if (!user) return res.status(404).json({ message: 'User not found' });
    const notifications = await Notification.find({ userId: user._id, userType }).sort({ createdAt: -1 });
    res.json({ notifications });
  } catch (err) {
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/notifications/:id/read - Mark a notification as read
app.put('/api/notifications/:id/read', authenticateToken, async (req, res) => {
  try {
    let userType = req.user.role === 'doctor' ? 'Doctor' : 'User';
    let user = null;
    if (userType === 'Doctor') {
      user = await Doctor.findOne({ email: req.user.email });
    } else {
      user = await Patient.findOne({ email: req.user.email });
    }
    if (!user) return res.status(404).json({ message: 'User not found' });
    const notification = await Notification.findOne({ _id: req.params.id, userId: user._id, userType });
    if (!notification) return res.status(404).json({ message: 'Notification not found' });
    notification.isRead = true;
    await notification.save();
    res.json({ message: 'Notification marked as read' });
  } catch (err) {
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Payment System APIs - PhonePe Integration

// PhonePe Configuration
const PHONEPE_MERCHANT_ID = process.env.PHONEPE_MERCHANT_ID || 'YOUR_MERCHANT_ID';
const PHONEPE_SALT_KEY = process.env.PHONEPE_SALT_KEY || 'YOUR_SALT_KEY';
const PHONEPE_SALT_INDEX = process.env.PHONEPE_SALT_INDEX || '1';
const PHONEPE_HOST_URL = process.env.PHONEPE_ENV === 'production' 
  ? 'https://api.phonepe.com/apis/hermes' 
  : 'https://api-preprod.phonepe.com/apis/pg-sandbox';

// Helper: Generate PhonePe checksum
function generatePhonePeChecksum(payload, endpoint) {
  const string = payload + endpoint + PHONEPE_SALT_KEY;
  const sha256 = crypto.createHash('sha256').update(string).digest('hex');
  return sha256 + '###' + PHONEPE_SALT_INDEX;
}

// Helper: Verify PhonePe callback checksum
function verifyPhonePeChecksum(checksum, payload) {
  const [receivedChecksum, saltIndex] = checksum.split('###');
  const string = payload + PHONEPE_SALT_KEY;
  const calculatedChecksum = crypto.createHash('sha256').update(string).digest('hex');
  return receivedChecksum === calculatedChecksum;
}

/**
 * Process Doctor Payout - 90% to doctor, 10% platform fee
 * Creates a payout record and processes payment split
 * 
 * @param {String} doctorId - Doctor's MongoDB ID
 * @param {Number} totalAmount - Total booking amount
 * @param {String} bookingId - Booking ID
 * @param {String} paymentId - Payment ID
 * @param {String} merchantTransactionId - PhonePe merchant transaction ID
 * @returns {Promise<Object>} Payout record
 */
async function processDoctorPayout(doctorId, totalAmount, bookingId, paymentId, merchantTransactionId) {
  try {
    console.log(`[PAYOUT] Processing payout for doctor ${doctorId}, amount: ${totalAmount}`);
    
    // Calculate split (90% doctor, 10% platform)
    const platformFeePercent = 0.10;
    const platformFee = Math.round(totalAmount * platformFeePercent * 100) / 100;
    const doctorAmount = Math.round((totalAmount - platformFee) * 100) / 100;
    
    console.log(`[PAYOUT] Split - Doctor: ₹${doctorAmount} (90%), Platform Fee: ₹${platformFee} (10%)`);
    
    // Get doctor details
    const doctor = await Doctor.findById(doctorId);
    if (!doctor) {
      throw new Error('Doctor not found');
    }
    
    // Create payout record
    const payout = new DoctorPayout({
      doctorId,
      bookingId,
      paymentId,
      amount: doctorAmount,
      platformFee,
      totalBookingAmount: totalAmount,
      merchantTransactionId,
      status: 'pending',
      payoutMethod: 'phonepe',
      notes: `Payout for booking ${bookingId} - 90% doctor share`
    });
    
    await payout.save();
    console.log(`[PAYOUT] Payout record created: ${payout._id}`);
    
    // In production, you would integrate with PhonePe Payout API here
    // For now, we mark it as processing and log the details
    payout.status = 'processing';
    payout.processedAt = new Date();
    await payout.save();
    
    console.log(`[PAYOUT] ✓ Payout queued successfully for doctor ${doctor.name}`);
    console.log(`[PAYOUT] Doctor will receive: ₹${doctorAmount}`);
    console.log(`[PAYOUT] Platform fee retained: ₹${platformFee}`);
    
    // TODO: In production, integrate with PhonePe Payout API
    // Example:
    // const payoutResponse = await initiatePhonePePayout(doctor, doctorAmount, payout._id);
    // if (payoutResponse.success) {
    //   payout.status = 'completed';
    //   payout.payoutTransactionId = payoutResponse.transactionId;
    //   payout.completedAt = new Date();
    // } else {
    //   payout.status = 'failed';
    //   payout.errorMessage = payoutResponse.error;
    // }
    // await payout.save();
    
    return payout;
    
  } catch (error) {
    console.error('[PAYOUT] Error processing doctor payout:', error);
    
    // Create failed payout record for tracking
    try {
      const failedPayout = new DoctorPayout({
        doctorId,
        bookingId,
        paymentId,
        amount: totalAmount * 0.9,
        platformFee: totalAmount * 0.1,
        totalBookingAmount: totalAmount,
        merchantTransactionId,
        status: 'failed',
        errorMessage: error.message,
        notes: `Failed payout attempt for booking ${bookingId}`
      });
      await failedPayout.save();
    } catch (saveError) {
      console.error('[PAYOUT] Failed to save error record:', saveError);
    }
    
    throw error;
  }
}

/**
 * Initiate PhonePe Payout (Placeholder for production integration)
 * In production, this would call PhonePe's Payout API to transfer funds
 * 
 * @param {Object} doctor - Doctor object with payment details
 * @param {Number} amount - Amount to transfer
 * @param {String} payoutId - Payout record ID
 * @returns {Promise<Object>} Payout response
 */
async function initiatePhonePePayout(doctor, amount, payoutId) {
  // Production implementation would use PhonePe Payout API
  // Documentation: https://developer.phonepe.com/v1/docs/payout-apis
  
  console.log('[PAYOUT-API] This would call PhonePe Payout API in production');
  console.log(`[PAYOUT-API] Transfer ₹${amount} to doctor: ${doctor.name}`);
  console.log(`[PAYOUT-API] Payout ID: ${payoutId}`);
  
  // Example payload structure:
  const payoutPayload = {
    merchantId: PHONEPE_MERCHANT_ID,
    merchantTransactionId: `PAYOUT_${Date.now()}_${payoutId}`,
    merchantUserId: doctor._id.toString(),
    amount: Math.round(amount * 100), // Convert to paise
    // Add beneficiary details (UPI, bank account, etc.)
    // beneficiaryVpa: doctor.upiId,
    // beneficiaryAccountNumber: doctor.accountNumber,
    // beneficiaryIfsc: doctor.ifscCode
  };
  
  // In production, make actual API call:
  // const response = await axios.post(`${PHONEPE_HOST_URL}/v1/payout`, payoutPayload);
  
  return {
    success: true,
    transactionId: `PAYOUT_${Date.now()}`,
    message: 'Payout initiated successfully (simulation mode)'
  };
}

// POST /api/payments/create-order - Create PhonePe payment order (with rate limiting)
app.post('/api/payments/create-order', paymentLimiter, authenticateToken, async (req, res) => {
  try {
    const { amount, doctorId, appointmentDate, appointmentTime } = req.body;
    const { email } = req.user;

    if (!amount || amount <= 0) {
      return res.status(400).json({ message: 'Invalid amount' });
    }

    // Get patient details
    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    // Get doctor details
    const doctor = await Doctor.findById(doctorId);
    if (!doctor) {
      return res.status(404).json({ message: 'Doctor not found' });
    }

    // Generate unique merchant transaction ID
    const merchantTransactionId = `TXN_${Date.now()}_${patient._id.toString().substring(0, 8)}`;
    
    // Calculate splits
    const platformFeePercent = 0.10; // 10% platform fee
    const platformFee = Math.round(amount * platformFeePercent * 100) / 100;
    const doctorAmount = amount - platformFee;

    // PhonePe payment payload
    const paymentPayload = {
      merchantId: PHONEPE_MERCHANT_ID,
      merchantTransactionId: merchantTransactionId,
      merchantUserId: patient._id.toString(),
      amount: Math.round(amount * 100), // Convert to paise
      redirectUrl: `${process.env.APP_BASE_URL || 'http://localhost:5000'}/api/payments/callback`,
      redirectMode: 'POST',
      callbackUrl: `${process.env.APP_BASE_URL || 'http://localhost:5000'}/api/payments/webhook`,
      mobileNumber: patient.phone || '9999999999',
      paymentInstrument: {
        type: 'PAY_PAGE' // Let user choose payment method
      }
    };

    // Encode payload to base64
    const payloadBase64 = Buffer.from(JSON.stringify(paymentPayload)).toString('base64');
    
    // Generate checksum
    const checksum = generatePhonePeChecksum(payloadBase64, '/pg/v1/pay');

    // Call PhonePe API
    const phonePeResponse = await axios.post(
      `${PHONEPE_HOST_URL}/pg/v1/pay`,
      {
        request: payloadBase64
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'X-VERIFY': checksum
        }
      }
    );

    if (phonePeResponse.data.success) {
      res.json({
        success: true,
        merchantTransactionId: merchantTransactionId,
        amount: amount,
        doctorAmount: doctorAmount,
        platformFee: platformFee,
        paymentUrl: phonePeResponse.data.data.instrumentResponse.redirectInfo.url,
        phonepeData: phonePeResponse.data.data
      });
    } else {
      res.status(400).json({ 
        success: false,
        message: 'Failed to initiate payment',
        error: phonePeResponse.data.message 
      });
    }
  } catch (error) {
    console.error('Error creating PhonePe order:', error.response?.data || error.message);
    res.status(500).json({ 
      success: false,
      message: 'Failed to create payment order',
      error: error.message 
    });
  }
});

// POST /api/payments/check-status - Check PhonePe payment status
app.post('/api/payments/check-status', authenticateToken, async (req, res) => {
  try {
    const { merchantTransactionId } = req.body;

    if (!merchantTransactionId) {
      return res.status(400).json({ message: 'Merchant transaction ID is required' });
    }

    // Create checksum for status check
    const endpoint = `/pg/v1/status/${PHONEPE_MERCHANT_ID}/${merchantTransactionId}`;
    const checksum = generatePhonePeChecksum('', endpoint);

    // Call PhonePe status API
    const statusResponse = await axios.get(
      `${PHONEPE_HOST_URL}${endpoint}`,
      {
        headers: {
          'Content-Type': 'application/json',
          'X-VERIFY': checksum,
          'X-MERCHANT-ID': PHONEPE_MERCHANT_ID
        }
      }
    );

    if (statusResponse.data.success) {
      const paymentData = statusResponse.data.data;
      
      res.json({
        success: true,
        status: paymentData.state, // COMPLETED, FAILED, PENDING
        transactionId: paymentData.transactionId,
        amount: paymentData.amount / 100, // Convert from paise to rupees
        paymentInstrument: paymentData.paymentInstrument?.type,
        responseCode: paymentData.responseCode
      });
    } else {
      res.status(400).json({
        success: false,
        message: 'Payment status check failed',
        code: statusResponse.data.code
      });
    }
  } catch (error) {
    console.error('Error checking payment status:', error.response?.data || error.message);
    res.status(500).json({ 
      success: false,
      message: 'Failed to check payment status',
      error: error.message 
    });
  }
});

// POST /api/payments/webhook - PhonePe webhook for payment callbacks
app.post('/api/payments/webhook', async (req, res) => {
  try {
    const { response } = req.body;
    const xVerify = req.headers['x-verify'];

    if (!response || !xVerify) {
      return res.status(400).json({ message: 'Invalid webhook payload' });
    }

    // Verify checksum
    if (!verifyPhonePeChecksum(xVerify, response)) {
      console.error('PhonePe webhook checksum verification failed');
      return res.status(401).json({ message: 'Invalid checksum' });
    }

    // Decode response
    const decodedResponse = JSON.parse(Buffer.from(response, 'base64').toString());
    
    const { 
      merchantTransactionId, 
      transactionId, 
      amount, 
      state, 
      responseCode,
      paymentInstrument 
    } = decodedResponse.data;

    console.log('PhonePe Webhook received:', {
      merchantTransactionId,
      transactionId,
      state,
      responseCode
    });

    // Find existing payment record or create new one
    let payment = await Payment.findOne({ phonepeMerchantTransactionId: merchantTransactionId });
    
    if (!payment) {
      // Create new payment record from webhook
      payment = new Payment({
        phonepeMerchantTransactionId: merchantTransactionId,
        phonepeTransactionId: transactionId,
        amount: amount / 100, // Convert from paise
        status: state === 'COMPLETED' ? 'SUCCESS' : state === 'FAILED' ? 'FAILED' : 'PENDING',
        paymentMethod: 'phonepe',
        paymentInstrument: paymentInstrument?.type,
        responseCode: responseCode
      });
    } else {
      // Update existing payment
      payment.phonepeTransactionId = transactionId;
      payment.status = state === 'COMPLETED' ? 'SUCCESS' : state === 'FAILED' ? 'FAILED' : 'PENDING';
      payment.paymentInstrument = paymentInstrument?.type;
      payment.responseCode = responseCode;
    }

    await payment.save();

    // If payment successful, process booking creation
    if (state === 'COMPLETED' && payment.bookingId) {
      try {
        const booking = await Booking.findById(payment.bookingId);
        if (booking && booking.status === 'pending') {
          booking.status = 'confirmed';
          booking.paymentStatus = 'paid';
          await booking.save();

          // Trigger doctor payout (split payment - 90% doctor, 10% platform)
          try {
            await processDoctorPayout(
              payment.professionalId || payment.doctorId,
              payment.amount,
              booking._id,
              payment._id,
              payment.phonepeMerchantTransactionId
            );
            console.log(`[WEBHOOK] ✓ Doctor payout processed for booking ${booking._id}`);
          } catch (payoutError) {
            console.error('[WEBHOOK] Doctor payout failed:', payoutError.message);
            // Continue execution - booking is still confirmed, payout can be retried
          }
          
          // Send notifications
          // await sendBookingConfirmationNotifications(booking);
        }
      } catch (bookingError) {
        console.error('Error updating booking:', bookingError);
      }
    }

    res.status(200).json({ message: 'Webhook processed successfully' });
  } catch (error) {
    console.error('Error processing PhonePe webhook:', error);
    res.status(500).json({ message: 'Webhook processing failed' });
  }
});

// POST /api/payments/callback - PhonePe redirect callback (browser redirect)
app.post('/api/payments/callback', async (req, res) => {
  try {
    const { merchantTransactionId, code } = req.body;
    
    // Redirect to app with status
    const deepLink = code === 'PAYMENT_SUCCESS'
      ? `reliefnet://payment/success?txnId=${merchantTransactionId}`
      : `reliefnet://payment/failed?txnId=${merchantTransactionId}`;
    
    res.redirect(deepLink);
  } catch (error) {
    console.error('Error in payment callback:', error);
    res.redirect('reliefnet://payment/error');
  }
});

// POST /api/payments/confirm - Confirm PhonePe payment and create booking (with rate limiting)
app.post('/api/payments/confirm', paymentLimiter, authenticateToken, async (req, res) => {
  try {
    const { 
      merchantTransactionId, // PhonePe merchant transaction ID
      professionalId, 
      date, 
      time, 
      duration, 
      type, 
      notes,
      sessionId,
      amount
    } = req.body;
    const { email } = req.user;

    if (!merchantTransactionId || !professionalId || !date || !time) {
      return res.status(400).json({ message: 'Missing required fields' });
    }

    // Get patient details
    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    // Get professional details
    const professional = await Doctor.findById(professionalId);
    if (!professional) {
      return res.status(404).json({ message: 'Professional not found' });
    }

    // Check payment status with PhonePe
    const endpoint = `/pg/v1/status/${PHONEPE_MERCHANT_ID}/${merchantTransactionId}`;
    const checksum = generatePhonePeChecksum('', endpoint);
    
    const statusResponse = await axios.get(
      `${PHONEPE_HOST_URL}${endpoint}`,
      {
        headers: {
          'Content-Type': 'application/json',
          'X-VERIFY': checksum,
          'X-MERCHANT-ID': PHONEPE_MERCHANT_ID
        }
      }
    );

    if (!statusResponse.data.success || statusResponse.data.data.state !== 'COMPLETED') {
      return res.status(400).json({ 
        message: 'Payment not completed',
        status: statusResponse.data.data.state 
      });
    }

    const phonepeData = statusResponse.data.data;
    
    // Check for duplicate transaction
    const existingPayment = await Payment.findOne({ 
      phonepeMerchantTransactionId: merchantTransactionId 
    });
    if (existingPayment && existingPayment.bookingId) {
      return res.status(400).json({ message: 'Payment already processed' });
    }

    // Check for booking conflicts
    const bookingDate = new Date(date);
    const existingBooking = await Booking.findOne({
      professionalId,
      date: {
        $gte: new Date(bookingDate.getTime() - (duration || 60) * 60000),
        $lte: new Date(bookingDate.getTime() + (duration || 60) * 60000)
      },
      status: { $in: ['pending', 'confirmed'] }
    });

    if (existingBooking) {
      return res.status(400).json({ message: 'Time slot is already booked' });
    }

    // If sessionId is provided, mark the session as booked
    if (sessionId) {
      const session = await Session.findById(sessionId);
      if (!session) {
        return res.status(404).json({ message: 'Session not found' });
      }
      if (session.status !== 'available') {
        return res.status(400).json({ message: 'Session is not available' });
      }
      session.status = 'booked';
      session.updatedAt = new Date();
      await session.save();
    }

    // Calculate payment split
    const totalAmount = phonepeData.amount / 100; // Convert from paise
    const platformFeePercent = 0.10; // 10% platform fee
    const platformFee = Math.round(totalAmount * platformFeePercent * 100) / 100;
    const doctorAmount = totalAmount - platformFee;

    // Create booking
    const booking = new Booking({
      patientId: patient._id,
      professionalId,
      date: bookingDate,
      time,
      duration: duration || 60,
      type: type || 'consultation',
      price: totalAmount,
      notes,
      paymentStatus: 'paid',
      status: 'confirmed'
    });

    await booking.save();

    // Create/Update payment record
    let payment = existingPayment;
    if (!payment) {
      payment = new Payment({
        phonepeTransactionId: phonepeData.transactionId,
        phonepeMerchantTransactionId: merchantTransactionId,
        amount: totalAmount,
        status: 'SUCCESS',
        paymentMethod: 'phonepe'
      });
    }
    
    payment.bookingId = booking._id;
    payment.patientId = patient._id;
    payment.professionalId = professionalId;
    payment.doctorAmount = doctorAmount;
    payment.platformFee = platformFee;
    payment.paymentInstrument = phonepeData.paymentInstrument?.type;
    payment.responseCode = phonepeData.responseCode;

    await payment.save();

    // Update booking with payment reference
    booking.paymentId = payment._id;
    await booking.save();

    // Populate professional details for response
    await booking.populate('professionalId', 'name title specialization');

    // Trigger doctor payout (split payment - 90% doctor, 10% platform)
    try {
      await processDoctorPayout(
        professionalId,
        totalAmount,
        booking._id,
        payment._id,
        merchantTransactionId
      );
      console.log(`[PAYMENT] ✓ Doctor payout processed for booking ${booking._id}`);
      console.log(`[PAYMENT] Doctor receives: ₹${doctorAmount} (90%), Platform fee: ₹${platformFee} (10%)`);
    } catch (payoutError) {
      console.error('[PAYMENT] Doctor payout failed:', payoutError.message);
      // Continue execution - booking is still confirmed, payout can be retried later
    }

    // Notify doctor
    await sendNotification({
      userId: professional._id,
      userType: 'Doctor',
      message: `New paid booking from ${patient.name} for ${date} at ${time}`,
      type: 'booking',
      data: { bookingId: booking._id, paymentId: payment._id }
    });

    // Notify patient
    await sendNotification({
      userId: patient._id,
      userType: 'User',
      message: `Your payment and booking for ${professional.name} has been confirmed`,
      type: 'payment',
      data: { bookingId: booking._id, paymentId: payment._id }
    });

    res.status(201).json({
      message: 'Payment and booking successful',
      booking,
      payment: {
        _id: payment._id,
        amount: payment.amount,
        doctorAmount: payment.doctorAmount,
        platformFee: payment.platformFee,
        status: payment.status,
        transactionId: payment.phonepeTransactionId
      }
    });
  } catch (error) {
    console.error('Error confirming payment:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/payments/:bookingId - Get payment details for a booking
app.get('/api/payments/:bookingId', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    
    const payment = await Payment.findOne({
      bookingId: req.params.bookingId,
      patientId: patient._id
    }).populate('bookingId professionalId');

    if (!payment) {
      return res.status(404).json({ message: 'Payment not found' });
    }

    res.json(payment);
  } catch (error) {
    console.error('Error fetching payment:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/patient/payments - List payments for current patient
app.get('/api/patient/payments', authenticateToken, async (req, res) => {
  try {
    const { email } = req.user;
    const patient = await Patient.findOne({ email });
    if (!patient) {
      return res.status(404).json({ message: 'Patient not found' });
    }

    const payments = await Payment.find({ patientId: patient._id })
      .sort({ createdAt: -1 })
      .select('-__v');

    res.json({
      success: true,
      payments: payments.map(p => ({
        _id: p._id,
        amount: p.amount,
        currency: (p.currency || 'inr').toUpperCase(),
        status: p.status,
        createdAt: p.createdAt,
        receiptUrl: p.receiptUrl || null
      }))
    });
  } catch (error) {
    console.error('Error fetching patient payments:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// POST /api/payments/webhook - Razorpay webhook for payment updates
app.post('/api/payments/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
  try {
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET;
    
    // Verify webhook signature
    const crypto = require('crypto');
    const signature = crypto
      .createHmac('sha256', webhookSecret)
      .update(JSON.stringify(req.body))
      .digest('hex');

    if (signature !== req.headers['x-razorpay-signature']) {
      console.error('Webhook signature verification failed');
      return res.status(400).send('Webhook signature verification failed');
    }

    const event = req.body;

    try {
      switch (event.event) {
        case 'payment.captured':
          const payment = event.payload.payment.entity;
          console.log('Payment captured:', payment.id);
          
          // Update payment status in database
          await Payment.findOneAndUpdate(
            { razorpayPaymentId: payment.id },
            { 
              status: 'succeeded',
              updatedAt: new Date()
            }
          );
          break;

        case 'payment.failed':
          const failedPayment = event.payload.payment.entity;
          console.log('Payment failed:', failedPayment.id);
          
          // Update payment status in database
          await Payment.findOneAndUpdate(
            { razorpayPaymentId: failedPayment.id },
            { 
              status: 'failed',
              updatedAt: new Date()
            }
          );
          break;

        default:
          console.log(`Unhandled event type: ${event.event}`);
      }

      res.json({ received: true });
    } catch (error) {
      console.error('Error processing webhook:', error);
      res.status(500).json({ message: 'Webhook processing failed' });
    }
  } catch (error) {
    console.error('Webhook error:', error);
    res.status(500).json({ message: 'Webhook processing failed' });
  }
});

// ============================================
// Video Call Management APIs
// ============================================

// POST /api/video-call/initiate - Start a new video call
app.post('/api/video-call/initiate', authenticateToken, async (req, res) => {
  try {
    const { receiverId, receiverType, bookingId } = req.body;
    const { email, role } = req.user;
    
    if (!receiverId || !receiverType) {
      return res.status(400).json({ message: 'receiverId and receiverType are required' });
    }
    
    // Get caller info
    const callerType = role === 'doctor' ? 'Doctor' : 'Patient';
    const caller = callerType === 'Doctor' 
      ? await Doctor.findOne({ email })
      : await Patient.findOne({ email });
    
    if (!caller) {
      return res.status(404).json({ message: 'Caller not found' });
    }
    
    // Verify receiver exists
    const receiver = receiverType === 'Doctor'
      ? await Doctor.findById(receiverId)
      : await Patient.findById(receiverId);
    
    if (!receiver) {
      return res.status(404).json({ message: 'Receiver not found' });
    }
    
    // Create video call record
    const videoCall = new VideoCall({
      callerId: caller._id,
      callerType,
      receiverId,
      receiverType,
      bookingId: bookingId || null,
      status: 'initiated'
    });
    
    await videoCall.save();
    
    console.log(`[VideoCall] Call initiated: ${videoCall._id} from ${callerType}:${caller._id} to ${receiverType}:${receiverId}`);
    
    res.status(201).json({
      success: true,
      callId: videoCall._id,
      message: 'Video call initiated',
      call: videoCall
    });
  } catch (error) {
    console.error('Error initiating video call:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/video-call/:callId/answer - Mark call as answered
app.put('/api/video-call/:callId/answer', authenticateToken, async (req, res) => {
  try {
    const { callId } = req.params;
    
    const videoCall = await VideoCall.findById(callId);
    if (!videoCall) {
      return res.status(404).json({ message: 'Call not found' });
    }
    
    videoCall.status = 'ongoing';
    videoCall.answeredAt = new Date();
    await videoCall.save();
    
    console.log(`[VideoCall] Call answered: ${callId}`);
    
    res.json({
      success: true,
      message: 'Call answered',
      call: videoCall
    });
  } catch (error) {
    console.error('Error answering video call:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/video-call/:callId/end - End video call
app.put('/api/video-call/:callId/end', authenticateToken, async (req, res) => {
  try {
    const { callId } = req.params;
    const { endReason, quality } = req.body;
    
    const videoCall = await VideoCall.findById(callId);
    if (!videoCall) {
      return res.status(404).json({ message: 'Call not found' });
    }
    
    videoCall.status = 'completed';
    videoCall.endedAt = new Date();
    videoCall.endReason = endReason || 'completed';
    
    if (quality) {
      videoCall.quality = quality;
    }
    
    await videoCall.save();
    
    console.log(`[VideoCall] Call ended: ${callId}, Duration: ${videoCall.duration}s`);
    
    res.json({
      success: true,
      message: 'Call ended',
      call: videoCall,
      duration: videoCall.duration
    });
  } catch (error) {
    console.error('Error ending video call:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// PUT /api/video-call/:callId/reject - Reject incoming call
app.put('/api/video-call/:callId/reject', authenticateToken, async (req, res) => {
  try {
    const { callId } = req.params;
    const { reason } = req.body;
    
    const videoCall = await VideoCall.findById(callId);
    if (!videoCall) {
      return res.status(404).json({ message: 'Call not found' });
    }
    
    videoCall.status = 'rejected';
    videoCall.endedAt = new Date();
    videoCall.endReason = 'rejected';
    videoCall.notes = reason || 'Call declined';
    await videoCall.save();
    
    console.log(`[VideoCall] Call rejected: ${callId}`);
    
    res.json({
      success: true,
      message: 'Call rejected',
      call: videoCall
    });
  } catch (error) {
    console.error('Error rejecting video call:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/video-call/history - Get call history for current user
app.get('/api/video-call/history', authenticateToken, async (req, res) => {
  try {
    const { email, role } = req.user;
    const { page = 1, limit = 20, status } = req.query;
    
    // Get user info
    const userType = role === 'doctor' ? 'Doctor' : 'Patient';
    const user = userType === 'Doctor'
      ? await Doctor.findOne({ email })
      : await Patient.findOne({ email });
    
    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }
    
    // Build query - calls where user is either caller or receiver
    const query = {
      $or: [
        { callerId: user._id, callerType: userType },
        { receiverId: user._id, receiverType: userType }
      ]
    };
    
    if (status) {
      query.status = status;
    }
    
    const calls = await VideoCall.find(query)
      .populate('callerId', 'name email photoUrl')
      .populate('receiverId', 'name email photoUrl')
      .populate('bookingId', 'date time type')
      .sort({ startedAt: -1 })
      .limit(parseInt(limit))
      .skip((parseInt(page) - 1) * parseInt(limit));
    
    const totalCalls = await VideoCall.countDocuments(query);
    
    // Calculate statistics
    const stats = await VideoCall.aggregate([
      { $match: query },
      {
        $group: {
          _id: '$status',
          count: { $sum: 1 },
          totalDuration: { $sum: '$duration' }
        }
      }
    ]);
    
    res.json({
      success: true,
      calls,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        totalPages: Math.ceil(totalCalls / parseInt(limit)),
        totalCalls
      },
      stats
    });
  } catch (error) {
    console.error('Error fetching call history:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/video-call/:callId - Get specific call details
app.get('/api/video-call/:callId', authenticateToken, async (req, res) => {
  try {
    const { callId } = req.params;
    
    const videoCall = await VideoCall.findById(callId)
      .populate('callerId', 'name email photoUrl specialization')
      .populate('receiverId', 'name email photoUrl specialization')
      .populate('bookingId', 'date time type status notes');
    
    if (!videoCall) {
      return res.status(404).json({ message: 'Call not found' });
    }
    
    res.json({
      success: true,
      call: videoCall
    });
  } catch (error) {
    console.error('Error fetching call details:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/video-call/active-calls - Check for active calls for current user
app.get('/api/video-call/active-calls', authenticateToken, async (req, res) => {
  try {
    const { email, role } = req.user;
    
    const userType = role === 'doctor' ? 'Doctor' : 'Patient';
    const user = userType === 'Doctor'
      ? await Doctor.findOne({ email })
      : await Patient.findOne({ email });
    
    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }
    
    // Find ongoing or ringing calls
    const activeCalls = await VideoCall.find({
      $or: [
        { callerId: user._id, callerType: userType },
        { receiverId: user._id, receiverType: userType }
      ],
      status: { $in: ['initiated', 'ringing', 'ongoing'] }
    })
      .populate('callerId', 'name photoUrl')
      .populate('receiverId', 'name photoUrl')
      .sort({ startedAt: -1 });
    
    res.json({
      success: true,
      activeCalls,
      hasActiveCall: activeCalls.length > 0
    });
  } catch (error) {
    console.error('Error checking active calls:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// ============================================
// Doctor Payout Management APIs
// ============================================

// GET /api/doctor/payouts - Get all payouts for logged-in doctor
app.get('/api/doctor/payouts', authenticateToken, async (req, res) => {
  try {
    const { email, userType } = req.user;
    
    // Verify user is a doctor
    if (userType !== 'Doctor') {
      return res.status(403).json({ message: 'Access denied. Doctors only.' });
    }
    
    const doctor = await Doctor.findOne({ email });
    if (!doctor) {
      return res.status(404).json({ message: 'Doctor not found' });
    }
    
    const { status, page = 1, limit = 20 } = req.query;
    const query = { doctorId: doctor._id };
    
    if (status) {
      query.status = status;
    }
    
    const payouts = await DoctorPayout.find(query)
      .populate('bookingId', 'date time type status')
      .populate('paymentId', 'amount status phonepeMerchantTransactionId')
      .sort({ createdAt: -1 })
      .limit(parseInt(limit))
      .skip((parseInt(page) - 1) * parseInt(limit));
    
    const totalPayouts = await DoctorPayout.countDocuments(query);
    const totalEarnings = await DoctorPayout.aggregate([
      { $match: { doctorId: doctor._id, status: 'completed' } },
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    
    const pendingEarnings = await DoctorPayout.aggregate([
      { $match: { doctorId: doctor._id, status: { $in: ['pending', 'processing'] } } },
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    
    res.json({
      success: true,
      payouts,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        totalPages: Math.ceil(totalPayouts / parseInt(limit)),
        totalPayouts
      },
      summary: {
        totalEarnings: totalEarnings[0]?.total || 0,
        pendingEarnings: pendingEarnings[0]?.total || 0,
        payoutPercentage: 90, // 90% to doctor
        platformFeePercentage: 10 // 10% platform fee
      }
    });
  } catch (error) {
    console.error('Error fetching doctor payouts:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/doctor/payouts/:payoutId - Get specific payout details
app.get('/api/doctor/payouts/:payoutId', authenticateToken, async (req, res) => {
  try {
    const { email, userType } = req.user;
    
    if (userType !== 'Doctor') {
      return res.status(403).json({ message: 'Access denied. Doctors only.' });
    }
    
    const doctor = await Doctor.findOne({ email });
    if (!doctor) {
      return res.status(404).json({ message: 'Doctor not found' });
    }
    
    const payout = await DoctorPayout.findOne({
      _id: req.params.payoutId,
      doctorId: doctor._id
    })
      .populate('bookingId', 'date time type status notes duration')
      .populate('paymentId', 'amount status phonepeTransactionId phonepeMerchantTransactionId createdAt');
    
    if (!payout) {
      return res.status(404).json({ message: 'Payout not found' });
    }
    
    res.json({
      success: true,
      payout
    });
  } catch (error) {
    console.error('Error fetching payout details:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// GET /api/doctor/earnings-summary - Get earnings summary for doctor
app.get('/api/doctor/earnings-summary', authenticateToken, async (req, res) => {
  try {
    const { email, userType } = req.user;
    
    if (userType !== 'Doctor') {
      return res.status(403).json({ message: 'Access denied. Doctors only.' });
    }
    
    const doctor = await Doctor.findOne({ email });
    if (!doctor) {
      return res.status(404).json({ message: 'Doctor not found' });
    }
    
    const { startDate, endDate } = req.query;
    const dateFilter = {};
    
    if (startDate) {
      dateFilter.createdAt = { $gte: new Date(startDate) };
    }
    if (endDate) {
      dateFilter.createdAt = { ...dateFilter.createdAt, $lte: new Date(endDate) };
    }
    
    const earnings = await DoctorPayout.aggregate([
      { $match: { doctorId: doctor._id, ...dateFilter } },
      {
        $group: {
          _id: '$status',
          count: { $sum: 1 },
          totalAmount: { $sum: '$amount' },
          totalPlatformFee: { $sum: '$platformFee' },
          totalBookingAmount: { $sum: '$totalBookingAmount' }
        }
      }
    ]);
    
    const summary = {
      completed: earnings.find(e => e._id === 'completed') || { count: 0, totalAmount: 0 },
      pending: earnings.find(e => e._id === 'pending') || { count: 0, totalAmount: 0 },
      processing: earnings.find(e => e._id === 'processing') || { count: 0, totalAmount: 0 },
      failed: earnings.find(e => e._id === 'failed') || { count: 0, totalAmount: 0 }
    };
    
    res.json({
      success: true,
      summary,
      doctor: {
        name: doctor.name,
        email: doctor.email,
        specialization: doctor.specialization
      }
    });
  } catch (error) {
    console.error('Error fetching earnings summary:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// POST /api/admin/payouts/retry/:payoutId - Retry failed payout (Admin only)
app.post('/api/admin/payouts/retry/:payoutId', authenticateToken, async (req, res) => {
  try {
    // TODO: Add admin authentication check
    // For now, allowing authenticated users
    
    const payout = await DoctorPayout.findById(req.params.payoutId);
    if (!payout) {
      return res.status(404).json({ message: 'Payout not found' });
    }
    
    if (payout.status !== 'failed') {
      return res.status(400).json({ message: 'Only failed payouts can be retried' });
    }
    
    // Retry payout
    payout.status = 'pending';
    payout.retryCount += 1;
    payout.errorMessage = '';
    await payout.save();
    
    // Process payout
    try {
      const updatedPayout = await processDoctorPayout(
        payout.doctorId,
        payout.totalBookingAmount,
        payout.bookingId,
        payout.paymentId,
        payout.merchantTransactionId
      );
      
      res.json({
        success: true,
        message: 'Payout retry initiated',
        payout: updatedPayout
      });
    } catch (payoutError) {
      res.status(500).json({
        success: false,
        message: 'Payout retry failed',
        error: payoutError.message
      });
    }
  } catch (error) {
    console.error('Error retrying payout:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// ============================================
// Booking & Availability System
// ============================================
const DoctorAvailability = require('./models/DoctorAvailability');

// Helper function to send booking confirmation email
async function sendBookingConfirmationEmail(booking) {
  try {
    const msg = {
      to: booking.patientEmail,
      from: SENDGRID_FROM_EMAIL,
      subject: 'Appointment Confirmed - ReliefNet',
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
            .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
            .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
            .details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
            .detail-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }
            .label { font-weight: bold; color: #667eea; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>🎉 Appointment Confirmed!</h1>
              <p>Your mental health appointment has been booked</p>
            </div>
            <div class="content">
              <p>Dear ${booking.patientName},</p>
              <p>Your appointment with <strong>${booking.doctorName}</strong> has been confirmed.</p>
              
              <div class="details">
                <div class="detail-row">
                  <span class="label">📅 Date:</span>
                  <span>${new Date(booking.appointmentDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
                </div>
                <div class="detail-row">
                  <span class="label">🕐 Time:</span>
                  <span>${booking.appointmentTime}</span>
                </div>
                <div class="detail-row">
                  <span class="label">⏱️ Duration:</span>
                  <span>${booking.duration} minutes</span>
                </div>
                <div class="detail-row">
                  <span class="label">💼 Session Type:</span>
                  <span>${booking.sessionType}</span>
                </div>
                <div class="detail-row">
                  <span class="label">💰 Amount:</span>
                  <span>₹${booking.amount}</span>
                </div>
                <div class="detail-row">
                  <span class="label">📋 Status:</span>
                  <span>${booking.status}</span>
                </div>
              </div>
              
              ${booking.reasonForVisit ? `<p><strong>Reason for Visit:</strong> ${booking.reasonForVisit}</p>` : ''}
              
              <p><strong>Important Reminders:</strong></p>
              <ul>
                <li>You will receive a reminder 24 hours before your appointment</li>
                <li>You will receive another reminder 1 hour before your appointment</li>
                <li>Please be available 5 minutes before the scheduled time</li>
                <li>If you need to cancel or reschedule, please do so at least 24 hours in advance</li>
              </ul>
              
              <p>We look forward to seeing you!</p>
              
              <p>Best regards,<br><strong>ReliefNet Team</strong></p>
              <p style="color: #888; font-size: 12px;">Your Mental Health Companion</p>
            </div>
          </div>
        </body>
        </html>
      `
    };
    
    await sgMail.send(msg);
    console.log(`✅ Booking confirmation email sent to ${booking.patientEmail}`);
  } catch (error) {
    console.error('❌ Error sending booking confirmation email:', error);
  }
}

// POST /api/doctor/availability - Doctor sets availability
app.post('/api/doctor/availability', authenticateToken, async (req, res) => {
  try {
    const { doctorId, date, slots, slotDuration, notes } = req.body;
    
    if (!doctorId || !date || !slots || !Array.isArray(slots)) {
      return res.status(400).json({ message: 'Missing required fields' });
    }
    
    // Get doctor info
    const doctor = await Doctor.findById(doctorId);
    if (!doctor) {
      return res.status(404).json({ message: 'Doctor not found' });
    }
    
    // Check if availability already exists for this date
    let availability = await DoctorAvailability.findOne({ doctorId, date: new Date(date) });
    
    if (availability) {
      // Update existing availability
      availability.slots = slots.map(slot => ({
        startTime: slot.startTime,
        endTime: slot.endTime,
        isBooked: slot.isBooked || false,
        bookingId: slot.bookingId || null
      }));
      availability.slotDuration = slotDuration || 60;
      availability.notes = notes || '';
      availability.updatedAt = new Date();
    } else {
      // Create new availability
      availability = new DoctorAvailability({
        doctorId,
        doctorName: doctor.name,
        date: new Date(date),
        dayOfWeek: new Date(date).toLocaleDateString('en-US', { weekday: 'long' }),
        slots: slots.map(slot => ({
          startTime: slot.startTime,
          endTime: slot.endTime,
          isBooked: false
        })),
        slotDuration: slotDuration || 60,
        notes: notes || ''
      });
    }
    
    await availability.save();
    
    res.json({ 
      success: true, 
      message: 'Availability set successfully',
      availability 
    });
  } catch (error) {
    console.error('Error setting availability:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/doctor/:doctorId/availability - Get doctor's availability for a date range
app.get('/api/doctor/:doctorId/availability', async (req, res) => {
  try {
    const { doctorId } = req.params;
    const { startDate, endDate } = req.query;
    
    if (!startDate || !endDate) {
      return res.status(400).json({ message: 'Start date and end date required' });
    }
    
    const availability = await DoctorAvailability.find({
      doctorId,
      date: {
        $gte: new Date(startDate),
        $lte: new Date(endDate)
      },
      isAvailable: true
    }).sort({ date: 1 });
    
    res.json({ success: true, availability });
  } catch (error) {
    console.error('Error fetching availability:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/doctor/:doctorId/available-slots - Get only available (not booked) slots
app.get('/api/doctor/:doctorId/available-slots', async (req, res) => {
  try {
    const { doctorId } = req.params;
    const { date } = req.query;
    
    if (!date) {
      return res.status(400).json({ message: 'Date required' });
    }
    
    const availability = await DoctorAvailability.findOne({
      doctorId,
      date: new Date(date),
      isAvailable: true
    });
    
    if (!availability) {
      return res.json({ success: true, availableSlots: [] });
    }
    
    // Filter out booked slots
    const availableSlots = availability.slots.filter(slot => !slot.isBooked);
    
    res.json({ 
      success: true, 
      date: availability.date,
      availableSlots,
      slotDuration: availability.slotDuration
    });
  } catch (error) {
    console.error('Error fetching available slots:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// POST /api/bookings - Create a new booking
app.post('/api/bookings', authenticateToken, async (req, res) => {
  try {
    const {
      patientId, patientName, patientEmail, patientPhone,
      doctorId, doctorName, doctorEmail,
      appointmentDate, appointmentTime, duration,
      sessionType, amount, reasonForVisit, symptoms
    } = req.body;
    
    if (!patientId || !doctorId || !appointmentDate || !appointmentTime) {
      return res.status(400).json({ message: 'Missing required booking information' });
    }
    
    // Check if slot is still available
    const availability = await DoctorAvailability.findOne({
      doctorId,
      date: new Date(appointmentDate),
      isAvailable: true
    });
    
    if (!availability) {
      return res.status(400).json({ message: 'No availability found for this date' });
    }
    
    // Find the specific slot
    const slot = availability.slots.find(s => s.startTime === appointmentTime && !s.isBooked);
    if (!slot) {
      return res.status(400).json({ message: 'This time slot is no longer available' });
    }
    
    // Create booking
    const booking = new Booking({
      patientId, patientName, patientEmail, patientPhone,
      doctorId, doctorName, doctorEmail,
      appointmentDate: new Date(appointmentDate),
      appointmentTime,
      duration: duration || 60,
      sessionType: sessionType || 'consultation',
      amount,
      reasonForVisit,
      symptoms,
      status: 'pending',
      paymentStatus: 'pending'
    });
    
    await booking.save();
    
    // Mark slot as booked
    slot.isBooked = true;
    slot.bookingId = booking._id;
    await availability.save();
    
    res.json({ 
      success: true, 
      message: 'Booking created successfully',
      booking 
    });
  } catch (error) {
    console.error('Error creating booking:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// POST /api/bookings/:id/confirm - Confirm a booking
app.post('/api/bookings/:id/confirm', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const { paymentId, confirmedBy } = req.body;
    
    const booking = await Booking.findById(id);
    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }
    
    booking.status = 'confirmed';
    booking.confirmedAt = new Date();
    booking.confirmedBy = confirmedBy || 'system';
    
    if (paymentId) {
      booking.paymentId = paymentId;
      booking.paymentStatus = 'paid';
    }
    
    await booking.save();
    
    // Send confirmation email
    await sendBookingConfirmationEmail(booking);
    
    // Send notification
    await sendNotification({
      userId: booking.patientId,
      userType: 'patient',
      type: 'booking',
      title: 'Appointment Confirmed',
      message: `Your appointment with ${booking.doctorName} on ${new Date(booking.appointmentDate).toLocaleDateString()} at ${booking.appointmentTime} has been confirmed.`,
      data: { bookingId: booking._id.toString() }
    });
    
    res.json({ 
      success: true, 
      message: 'Booking confirmed successfully',
      booking 
    });
  } catch (error) {
    console.error('Error confirming booking:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// POST /api/bookings/:id/cancel - Cancel a booking
app.post('/api/bookings/:id/cancel', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const { cancelledBy, cancellationReason } = req.body;
    
    const booking = await Booking.findById(id);
    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }
    
    booking.status = 'cancelled';
    booking.cancelledBy = cancelledBy || 'patient';
    booking.cancellationReason = cancellationReason;
    booking.cancelledAt = new Date();
    
    await booking.save();
    
    // Free up the slot
    const availability = await DoctorAvailability.findOne({
      doctorId: booking.doctorId,
      date: booking.appointmentDate
    });
    
    if (availability) {
      const slot = availability.slots.find(s => s.bookingId && s.bookingId.toString() === id);
      if (slot) {
        slot.isBooked = false;
        slot.bookingId = null;
        await availability.save();
      }
    }
    
    // Send notification
    await sendNotification({
      userId: booking.doctorId,
      userType: 'doctor',
      type: 'booking',
      title: 'Appointment Cancelled',
      message: `${booking.patientName}'s appointment on ${new Date(booking.appointmentDate).toLocaleDateString()} at ${booking.appointmentTime} has been cancelled.`,
      data: { bookingId: booking._id.toString() }
    });
    
    res.json({ 
      success: true, 
      message: 'Booking cancelled successfully',
      booking 
    });
  } catch (error) {
    console.error('Error cancelling booking:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/bookings/upcoming - Get upcoming appointments (for reminders)
app.get('/api/bookings/upcoming', async (req, res) => {
  try {
    const now = new Date();
    const next24Hours = new Date(now.getTime() + (24 * 60 * 60 * 1000));
    
    const upcomingBookings = await Booking.find({
      appointmentDate: {
        $gte: now,
        $lte: next24Hours
      },
      status: { $in: ['pending', 'confirmed'] }
    });
    
    res.json({ success: true, bookings: upcomingBookings });
  } catch (error) {
    console.error('Error fetching upcoming bookings:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/bookings/:id/status - Get booking status
app.get('/api/bookings/:id/status', async (req, res) => {
  try {
    const { id } = req.params;
    
    const booking = await Booking.findById(id);
    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }
    
    res.json({ 
      success: true, 
      status: booking.status,
      paymentStatus: booking.paymentStatus,
      booking 
    });
  } catch (error) {
    console.error('Error fetching booking status:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/patient/:patientId/bookings - Get patient's bookings
app.get('/api/patient/:patientId/bookings', authenticateToken, async (req, res) => {
  try {
    const { patientId } = req.params;
    const { status } = req.query;
    
    let query = { patientId };
    if (status) {
      query.status = status;
    }
    
    const bookings = await Booking.find(query)
      .sort({ appointmentDate: -1 })
      .limit(50);
    
    res.json({ success: true, bookings });
  } catch (error) {
    console.error('Error fetching patient bookings:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// GET /api/doctor/:doctorId/bookings - Get doctor's bookings
app.get('/api/doctor/:doctorId/bookings', authenticateToken, async (req, res) => {
  try {
    const { doctorId } = req.params;
    const { status, date } = req.query;
    
    let query = { doctorId };
    if (status) {
      query.status = status;
    }
    if (date) {
      const startOfDay = new Date(date);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(date);
      endOfDay.setHours(23, 59, 59, 999);
      query.appointmentDate = { $gte: startOfDay, $lte: endOfDay };
    }
    
    const bookings = await Booking.find(query)
      .sort({ appointmentDate: 1 })
      .limit(100);
    
    res.json({ success: true, bookings });
  } catch (error) {
    console.error('Error fetching doctor bookings:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// ============================================
// Reminder System (Cron Job Simulation)
// ============================================
async function sendAppointmentReminders() {
  try {
    const now = new Date();
    const in24Hours = new Date(now.getTime() + (24 * 60 * 60 * 1000));
    const in1Hour = new Date(now.getTime() + (60 * 60 * 1000));
    
    // Find bookings that need 24-hour reminder
    const bookings24h = await Booking.find({
      appointmentDate: {
        $gte: now,
        $lte: in24Hours
      },
      status: { $in: ['pending', 'confirmed'] },
      reminderSent24h: false
    });
    
    for (const booking of bookings24h) {
      const hoursUntil = Math.round((new Date(booking.appointmentDate) - now) / (1000 * 60 * 60));
      if (hoursUntil >= 23 && hoursUntil <= 25) { // Within 24-hour window
        // Send email reminder
        const msg = {
          to: booking.patientEmail,
          from: SENDGRID_FROM_EMAIL,
          subject: 'Reminder: Appointment Tomorrow - ReliefNet',
          html: `
            <h2>🔔 Appointment Reminder</h2>
            <p>Dear ${booking.patientName},</p>
            <p>This is a friendly reminder about your appointment tomorrow:</p>
            <ul>
              <li><strong>Doctor:</strong> ${booking.doctorName}</li>
              <li><strong>Date:</strong> ${new Date(booking.appointmentDate).toLocaleDateString()}</li>
              <li><strong>Time:</strong> ${booking.appointmentTime}</li>
              <li><strong>Duration:</strong> ${booking.duration} minutes</li>
            </ul>
            <p>Please be ready 5 minutes before your scheduled time.</p>
            <p>Best regards,<br>ReliefNet Team</p>
          `
        };
        
        try {
          await sgMail.send(msg);
          
          // Send push notification
          await sendNotification({
            userId: booking.patientId,
            userType: 'patient',
            type: 'reminder',
            title: 'Appointment Tomorrow',
            message: `Reminder: Your appointment with ${booking.doctorName} is tomorrow at ${booking.appointmentTime}`,
            data: { bookingId: booking._id.toString() }
          });
          
          booking.reminderSent24h = true;
          await booking.save();
          console.log(`✅ 24h reminder sent for booking ${booking._id}`);
        } catch (error) {
          console.error(`❌ Error sending 24h reminder for booking ${booking._id}:`, error);
        }
      }
    }
    
    // Find bookings that need 1-hour reminder
    const bookings1h = await Booking.find({
      appointmentDate: {
        $gte: now,
        $lte: in1Hour
      },
      status: { $in: ['pending', 'confirmed'] },
      reminderSent1h: false
    });
    
    for (const booking of bookings1h) {
      const minutesUntil = Math.round((new Date(booking.appointmentDate) - now) / (1000 * 60));
      if (minutesUntil >= 50 && minutesUntil <= 70) { // Within 1-hour window
        // Send push notification only (shorter notice)
        await sendNotification({
          userId: booking.patientId,
          userType: 'patient',
          type: 'reminder',
          title: 'Appointment in 1 Hour',
          message: `Your appointment with ${booking.doctorName} starts in 1 hour at ${booking.appointmentTime}. Please be ready!`,
          data: { bookingId: booking._id.toString() }
        });
        
        booking.reminderSent1h = true;
        await booking.save();
        console.log(`✅ 1h reminder sent for booking ${booking._id}`);
      }
    }
  } catch (error) {
    console.error('❌ Error in reminder system:', error);
  }
}

// Run reminder check every 15 minutes
setInterval(sendAppointmentReminders, 15 * 60 * 1000);

// Also run on startup
setTimeout(sendAppointmentReminders, 5000);

// ============================================
// Chatbot Endpoint
// ============================================

// POST /chat - Chatbot endpoint for emotional support
app.post('/chat', async (req, res) => {
  try {
    const { user_id, message } = req.body;
    
    // Validate input
    if (!user_id || !message) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        reply: 'I need both a user ID and a message to respond.' 
      });
    }

    console.log(`💬 Chatbot request from user ${user_id}: "${message}"`);

    // Simple emotional support chatbot responses
    const responses = {
      // Greetings
      greetings: [
        "Hello! 👋 I'm here to support you. How are you feeling today?",
        "Hi there! 💙 I'm glad you're here. What's on your mind?",
        "Welcome! I'm here to listen. How can I help you today?"
      ],
      
      // Anxiety/Stress
      anxiety: [
        "I hear that you're feeling anxious. Remember, it's okay to feel this way. Try taking three deep breaths - breathe in for 4 counts, hold for 4, and exhale for 4. 🌸",
        "Anxiety can be overwhelming. You're not alone in this. Would you like to try a grounding technique? Name 5 things you can see, 4 you can touch, 3 you can hear, 2 you can smell, and 1 you can taste. 🧘",
        "It's brave of you to share that you're anxious. Remember: this feeling is temporary. You've gotten through difficult moments before, and you can get through this one too. 💪"
      ],
      
      // Depression/Sadness
      sadness: [
        "I'm sorry you're feeling this way. Your feelings are valid, and it's okay to not be okay sometimes. Would you like to talk about what's making you feel sad? 💙",
        "Feeling down can be really hard. Remember to be gentle with yourself. Small steps count - even getting out of bed or having a glass of water is an accomplishment. 🌱",
        "Thank you for sharing how you're feeling. Depression is tough, but you're stronger than you know. Have you talked to someone you trust about this? Professional support can really help. 🤗"
      ],
      
      // Loneliness
      lonely: [
        "Feeling lonely is difficult, but please know that you're not alone in feeling this way. I'm here with you right now. 💙",
        "Loneliness can feel overwhelming. Would it help to reach out to someone - even a simple text to a friend or family member? Connection can make a big difference. 🌟",
        "I'm here for you. Sometimes loneliness comes even when we're surrounded by people. Your feelings are valid. Consider joining a support group or online community where you can connect with others. 💬"
      ],
      
      // Stress
      stress: [
        "Stress can feel like too much to handle. Let's break it down - what's the most pressing thing on your mind right now? Sometimes tackling one thing at a time helps. 📝",
        "I can hear that you're stressed. Remember: you don't have to do everything at once. Try the 5-5-5 rule: breathe in for 5 seconds, hold for 5, breathe out for 5. This helps activate your calm response. 🌊",
        "Feeling stressed is your body's way of saying you need care. What's one small thing you could do right now to take care of yourself? Even 5 minutes of rest counts. 🛀"
      ],
      
      // Panic/Crisis
      crisis: [
        "I'm concerned about what you're sharing. If you're in crisis or having thoughts of self-harm, please reach out to a crisis helpline immediately: National Suicide Prevention Lifeline: 988 or 1-800-273-8255. They're available 24/7. 🆘",
        "Your safety is the top priority. Please call emergency services (911) or go to your nearest emergency room if you're in immediate danger. You deserve help and support. 💙",
        "This sounds really serious. Please don't face this alone. Reach out to a mental health professional or crisis helpline right now. You matter, and help is available. 🚨"
      ],
      
      // Encouragement
      encouragement: [
        "You're doing better than you think. Every day you keep going is a victory. I'm proud of you! 🌟",
        "Remember: progress isn't always linear. Be patient with yourself. You're on a journey, and that's okay. 🌈",
        "You've survived 100% of your worst days so far. That's an amazing track record. Keep going! 💪"
      ],
      
      // General support
      general: [
        "I'm here to listen. Tell me more about what's going on. 💙",
        "That sounds challenging. How are you coping with that? 🤗",
        "Thank you for sharing. Your feelings are important. What would help you feel better right now? 🌸",
        "I hear you. It's okay to feel what you're feeling. What kind of support would be most helpful for you? 💬"
      ]
    };

    // Simple keyword-based response logic
    const lowerMessage = message.toLowerCase();
    let reply;

    // Crisis detection (highest priority)
    if (lowerMessage.match(/suicide|kill myself|end it all|want to die|self.harm|hurt myself/i)) {
      reply = responses.crisis[Math.floor(Math.random() * responses.crisis.length)];
    }
    // Greetings
    else if (lowerMessage.match(/^(hi|hello|hey|good morning|good afternoon|good evening)/i)) {
      reply = responses.greetings[Math.floor(Math.random() * responses.greetings.length)];
    }
    // Anxiety/Panic
    else if (lowerMessage.match(/anxious|anxiety|panic|worried|nervous|scared|fear/i)) {
      reply = responses.anxiety[Math.floor(Math.random() * responses.anxiety.length)];
    }
    // Depression/Sadness
    else if (lowerMessage.match(/depressed|depression|sad|down|hopeless|empty|numb/i)) {
      reply = responses.sadness[Math.floor(Math.random() * responses.sadness.length)];
    }
    // Loneliness
    else if (lowerMessage.match(/lonely|alone|isolated|no one cares|nobody/i)) {
      reply = responses.lonely[Math.floor(Math.random() * responses.lonely.length)];
    }
    // Stress
    else if (lowerMessage.match(/stress|stressed|overwhelm|too much|can't cope/i)) {
      reply = responses.stress[Math.floor(Math.random() * responses.stress.length)];
    }
    // Encouragement seeking
    else if (lowerMessage.match(/need help|can you help|support|encourage/i)) {
      reply = responses.encouragement[Math.floor(Math.random() * responses.encouragement.length)];
    }
    // Default response
    else {
      reply = responses.general[Math.floor(Math.random() * responses.general.length)];
    }

    console.log(`✅ Chatbot replying: "${reply}"`);
    
    res.json({ reply });
    
  } catch (error) {
    console.error('❌ Chatbot error:', error);
    res.status(500).json({ 
      reply: "I'm having trouble right now, but I'm here for you. Please try again in a moment. If you're in crisis, please call 988 or visit your nearest emergency room. 💙"
    });
  }
});

const PORT = process.env.PORT || 5000;
const HOST = process.env.NODE_ENV === 'production' ? '0.0.0.0' : 'localhost';
server.listen(PORT, HOST, () => {
  console.log(`Server + Socket.IO started on port ${PORT} (http://${HOST}:${PORT})`);
});


