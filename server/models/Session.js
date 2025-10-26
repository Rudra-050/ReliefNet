const mongoose = require('mongoose');

const sessionSchema = new mongoose.Schema({
  doctor: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  date: { type: String, required: true }, // ISO date string
  time: { type: String, required: true }, // e.g., '14:00'
  duration: { type: Number, required: true }, // in minutes
  type: { type: String, required: true }, // e.g., 'consultation'
  status: { type: String, default: 'available' }, // 'available', 'booked', 'cancelled'
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

const Session = mongoose.model('Session', sessionSchema);

module.exports = Session; 