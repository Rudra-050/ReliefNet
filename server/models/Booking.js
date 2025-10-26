const mongoose = require('mongoose');

const bookingSchema = new mongoose.Schema({
  patientId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  patientName: { type: String, required: true },
  patientEmail: { type: String, required: true },
  patientPhone: { type: String },
  
  doctorId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  doctorName: { type: String, required: true },
  doctorEmail: { type: String, required: true },
  
  sessionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Session' },
  availabilitySlotId: { type: mongoose.Schema.Types.ObjectId, ref: 'DoctorAvailability' },
  
  // Appointment details
  appointmentDate: { type: Date, required: true },
  appointmentTime: { type: String, required: true }, // "14:00"
  duration: { type: Number, default: 60 }, // minutes
  sessionType: { type: String, default: 'consultation' }, // consultation, therapy, follow-up
  
  // Booking status
  status: { 
    type: String, 
    enum: ['pending', 'confirmed', 'cancelled', 'completed', 'no-show'],
    default: 'pending' 
  },
  
  // Payment details
  paymentId: { type: String },
  paymentStatus: { 
    type: String, 
    enum: ['pending', 'paid', 'refunded', 'failed'],
    default: 'pending'
  },
  amount: { type: Number, required: true },
  currency: { type: String, default: 'INR' },
  
  // Notes and reason
  reasonForVisit: { type: String },
  symptoms: { type: String },
  notes: { type: String },
  doctorNotes: { type: String },
  
  // Reminders
  reminderSent24h: { type: Boolean, default: false },
  reminderSent1h: { type: Boolean, default: false },
  
  // Cancellation
  cancelledBy: { type: String, enum: ['patient', 'doctor', 'system'] },
  cancellationReason: { type: String },
  cancelledAt: { type: Date },
  
  // Confirmation
  confirmedAt: { type: Date },
  confirmedBy: { type: String },
  
  // Timestamps
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

// Indexes for faster queries
bookingSchema.index({ patientId: 1, appointmentDate: -1 });
bookingSchema.index({ doctorId: 1, appointmentDate: -1 });
bookingSchema.index({ status: 1, appointmentDate: 1 });
bookingSchema.index({ appointmentDate: 1, status: 1 }); // For reminders

const Booking = mongoose.model('Booking', bookingSchema);

module.exports = Booking;
