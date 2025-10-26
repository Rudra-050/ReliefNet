const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
  conversationId: { 
    type: String, 
    required: true,
    index: true 
  }, // Format: "patient_id:doctor_id" or "doctor_id:patient_id"
  senderId: { 
    type: mongoose.Schema.Types.ObjectId, 
    required: true 
  },
  senderType: { 
    type: String, 
    enum: ['patient', 'doctor'], 
    required: true 
  },
  receiverId: { 
    type: mongoose.Schema.Types.ObjectId, 
    required: true 
  },
  receiverType: { 
    type: String, 
    enum: ['patient', 'doctor'], 
    required: true 
  },
  messageType: { 
    type: String, 
    enum: ['text', 'voice', 'image'], 
    default: 'text' 
  },
  content: { 
    type: String, 
    required: true 
  },
  voiceUrl: { 
    type: String 
  }, // For voice messages
  imageUrl: { 
    type: String 
  }, // For image messages
  isRead: { 
    type: Boolean, 
    default: false 
  },
  readAt: { 
    type: Date 
  },
  createdAt: { 
    type: Date, 
    default: Date.now,
    index: true 
  }
}, {
  timestamps: true
});

// Compound index for efficient conversation queries
messageSchema.index({ conversationId: 1, createdAt: -1 });
messageSchema.index({ senderId: 1, receiverId: 1 });

const Message = mongoose.model('Message', messageSchema);

module.exports = Message;
