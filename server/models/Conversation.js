const mongoose = require('mongoose');

const conversationSchema = new mongoose.Schema({
  conversationId: { 
    type: String, 
    required: true, 
    unique: true
  }, // Format: "smaller_id:larger_id" for consistency
  patientId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Patient',
    required: true 
  },
  doctorId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Doctor',
    required: true 
  },
  patientName: { 
    type: String 
  },
  doctorName: { 
    type: String 
  },
  lastMessage: { 
    type: String 
  },
  lastMessageTime: { 
    type: Date 
  },
  lastMessageSender: { 
    type: String, 
    enum: ['patient', 'doctor'] 
  },
  unreadCountPatient: { 
    type: Number, 
    default: 0 
  }, // Unread messages for patient
  unreadCountDoctor: { 
    type: Number, 
    default: 0 
  }, // Unread messages for doctor
  isActive: { 
    type: Boolean, 
    default: true 
  },
  createdAt: { 
    type: Date, 
    default: Date.now 
  },
  updatedAt: { 
    type: Date, 
    default: Date.now 
  }
}, {
  timestamps: true
});

// Indexes for efficient queries
conversationSchema.index({ patientId: 1, doctorId: 1 });
conversationSchema.index({ lastMessageTime: -1 });

const Conversation = mongoose.model('Conversation', conversationSchema);

module.exports = Conversation;
