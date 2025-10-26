const mongoose = require('mongoose');

const loginSessionSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, required: true },
  userType: { type: String, enum: ['patient', 'doctor'], required: true },
  email: { type: String, required: true },
  name: { type: String, required: true },
  loginMethod: { type: String, enum: ['password', 'otp', 'medical-id'], required: true },
  ipAddress: { type: String },
  userAgent: { type: String },
  loginAt: { type: Date, default: Date.now },
  expiresAt: { type: Date }, // When JWT expires
  isActive: { type: Boolean, default: true }
}, {
  timestamps: true
});

// Index for faster queries
loginSessionSchema.index({ email: 1, loginAt: -1 });
loginSessionSchema.index({ userId: 1, isActive: 1 });

const LoginSession = mongoose.model('LoginSession', loginSessionSchema);

module.exports = LoginSession;
