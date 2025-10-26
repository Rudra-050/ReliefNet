const mongoose = require('mongoose');

/**
 * DoctorPayout Schema
 * Tracks all payouts made to doctors for completed bookings
 */
const doctorPayoutSchema = new mongoose.Schema({
  doctorId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Doctor', 
    required: true,
    index: true
  },
  
  bookingId: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'Booking',
    required: true
  },
  
  paymentId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Payment',
    required: true
  },
  
  // Payout details
  amount: { 
    type: Number, 
    required: true,
    min: 0
  },
  
  platformFee: {
    type: Number,
    required: true,
    min: 0
  },
  
  totalBookingAmount: {
    type: Number,
    required: true,
    min: 0
  },
  
  // Payout status
  status: {
    type: String,
    enum: ['pending', 'processing', 'completed', 'failed', 'cancelled'],
    default: 'pending'
  },
  
  // Transaction details
  merchantTransactionId: {
    type: String,
    required: true
  },
  
  payoutTransactionId: {
    type: String, // PhonePe payout transaction ID (when processed)
  },
  
  payoutMethod: {
    type: String,
    enum: ['phonepe', 'bank_transfer', 'upi', 'manual'],
    default: 'phonepe'
  },
  
  // Doctor's bank/UPI details (encrypted in production)
  doctorPaymentDetails: {
    upiId: String,
    accountNumber: String,
    ifscCode: String,
    accountHolderName: String
  },
  
  // Timestamps
  scheduledAt: {
    type: Date,
    default: Date.now
  },
  
  processedAt: {
    type: Date
  },
  
  completedAt: {
    type: Date
  },
  
  // Error tracking
  errorMessage: String,
  retryCount: {
    type: Number,
    default: 0
  },
  
  // Metadata
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

// Index for efficient queries
doctorPayoutSchema.index({ doctorId: 1, status: 1 });
doctorPayoutSchema.index({ bookingId: 1 });
doctorPayoutSchema.index({ paymentId: 1 });
doctorPayoutSchema.index({ status: 1, scheduledAt: 1 });

// Update timestamp on save
doctorPayoutSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  next();
});

// Virtual for payout percentage
doctorPayoutSchema.virtual('payoutPercentage').get(function() {
  if (!this.totalBookingAmount) return 0;
  return Math.round((this.amount / this.totalBookingAmount) * 100);
});

// Ensure virtuals are included in JSON
doctorPayoutSchema.set('toJSON', { virtuals: true });
doctorPayoutSchema.set('toObject', { virtuals: true });

module.exports = mongoose.model('DoctorPayout', doctorPayoutSchema);
