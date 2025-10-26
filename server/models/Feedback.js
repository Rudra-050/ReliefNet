const mongoose = require('mongoose');

const feedbackSchema = new mongoose.Schema({
  doctorId: { type: mongoose.Schema.Types.ObjectId, ref: 'Doctor', required: true },
  patientId: { type: mongoose.Schema.Types.ObjectId, ref: 'Patient', required: true },
  patientName: { type: String },
  rating: { type: Number, min: 1, max: 5, required: true },
  comment: { type: String },
  createdAt: { type: Date, default: Date.now }
});

feedbackSchema.index({ doctorId: 1, createdAt: -1 });

module.exports = mongoose.model('Feedback', feedbackSchema);
