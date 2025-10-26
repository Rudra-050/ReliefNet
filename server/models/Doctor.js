const mongoose = require('mongoose');

const doctorSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  name: String,
  password: String, // hashed password
  medicalId: String,
  specialization: String,
  specialty: String,
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

// Update timestamp on save
doctorSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  next();
});

module.exports = mongoose.model('Doctor', doctorSchema);
