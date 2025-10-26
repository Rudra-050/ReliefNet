const mongoose = require('mongoose');

const timeSlotSchema = new mongoose.Schema({
  startTime: { type: String, required: true }, // "09:00"
  endTime: { type: String, required: true }, // "10:00"
  isBooked: { type: Boolean, default: false },
  bookingId: { type: mongoose.Schema.Types.ObjectId, ref: 'Booking' }
}, { _id: true });

const doctorAvailabilitySchema = new mongoose.Schema({
  doctorId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  doctorName: { type: String, required: true },
  
  // Date for this availability
  date: { type: Date, required: true },
  dayOfWeek: { type: String }, // "Monday", "Tuesday", etc.
  
  // Available time slots for this date
  slots: [timeSlotSchema],
  
  // Overall availability for the day
  isAvailable: { type: Boolean, default: true },
  
  // Slot duration (default 60 minutes)
  slotDuration: { type: Number, default: 60 },
  
  // Break times (optional)
  breakStart: { type: String },
  breakEnd: { type: String },
  
  // Notes
  notes: { type: String },
  
  // Timestamps
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

// Indexes
doctorAvailabilitySchema.index({ doctorId: 1, date: 1 });
doctorAvailabilitySchema.index({ date: 1, isAvailable: 1 });

const DoctorAvailability = mongoose.model('DoctorAvailability', doctorAvailabilitySchema);

module.exports = DoctorAvailability;
