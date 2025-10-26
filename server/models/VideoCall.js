const mongoose = require('mongoose');

/**
 * VideoCall Schema
 * Tracks video call sessions between patients and doctors
 */
const videoCallSchema = new mongoose.Schema({
  bookingId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Booking'
  },
  
  callerId: {
    type: mongoose.Schema.Types.ObjectId,
    required: true,
    refPath: 'callerType'
  },
  
  callerType: {
    type: String,
    required: true,
    enum: ['Doctor', 'Patient']
  },
  
  receiverId: {
    type: mongoose.Schema.Types.ObjectId,
    required: true,
    refPath: 'receiverType'
  },
  
  receiverType: {
    type: String,
    required: true,
    enum: ['Doctor', 'Patient']
  },
  
  status: {
    type: String,
    enum: ['initiated', 'ringing', 'ongoing', 'completed', 'rejected', 'missed', 'failed'],
    default: 'initiated',
    index: true
  },
  
  // Call timing
  startedAt: {
    type: Date,
    default: Date.now
  },
  
  answeredAt: {
    type: Date
  },
  
  endedAt: {
    type: Date
  },
  
  duration: {
    type: Number, // Duration in seconds
    default: 0
  },
  
  // Connection quality metrics
  quality: {
    callerNetwork: String, // 'excellent', 'good', 'fair', 'poor'
    receiverNetwork: String,
    avgBitrate: Number,
    packetLoss: Number
  },
  
  // Call metadata
  endReason: {
    type: String,
    enum: ['completed', 'caller_ended', 'receiver_ended', 'network_issue', 'timeout', 'rejected', 'no_answer']
  },
  
  recordingUrl: String, // If call was recorded
  
  notes: String,
  
  createdAt: {
    type: Date,
    default: Date.now
  },
  
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// Indexes for efficient queries
videoCallSchema.index({ callerId: 1, status: 1 });
videoCallSchema.index({ receiverId: 1, status: 1 });
videoCallSchema.index({ bookingId: 1 });
videoCallSchema.index({ startedAt: -1 });

// Update timestamp on save
videoCallSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  
  // Calculate duration if call ended
  if (this.endedAt && this.answeredAt) {
    this.duration = Math.floor((this.endedAt - this.answeredAt) / 1000);
  }
  
  next();
});

// Virtual for formatted duration
videoCallSchema.virtual('formattedDuration').get(function() {
  if (!this.duration) return '0:00';
  const minutes = Math.floor(this.duration / 60);
  const seconds = this.duration % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
});

// Ensure virtuals are included in JSON
videoCallSchema.set('toJSON', { virtuals: true });
videoCallSchema.set('toObject', { virtuals: true });

module.exports = mongoose.model('VideoCall', videoCallSchema);
