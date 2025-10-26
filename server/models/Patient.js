const mongoose = require('mongoose');

const patientSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  name: String,
  password: String, // hashed password
  location: String,
  photoUrl: { type: String }
  // add more patient-specific fields here
});

module.exports = mongoose.model('User', patientSchema); // or 'Patient'
