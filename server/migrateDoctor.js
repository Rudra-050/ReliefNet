const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

// Connect to MongoDB
mongoose.connect('mongodb+srv://rudranshbhatt2005:tiger%401358@relief.sb1iuve.mongodb.net/reliefnet?retryWrites=true&w=majority&appName=Relief', {
  useNewUrlParser: true,
  useUnifiedTopology: true,
});

// Define the Doctor schema (same as in server.js)
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

const Doctor = mongoose.model('Doctor', doctorSchema);

const sampleProfessionals = [
  {
    name: "Dr. Priya Sharma",
    email: "priya.sharma@reliefnet.com",
    password: "password123",
    medicalId: "MED10001",
    title: "Clinical Psychologist",
    specialization: "Clinical Psychologist",
    category: "Psychologist",
    description: "Experienced clinical psychologist specializing in anxiety, depression, and trauma therapy. Committed to providing compassionate care and evidence-based treatments.",
    experience: "8+ years",
    education: "Ph.D. in Clinical Psychology, Delhi University",
    certifications: "Licensed Clinical Psychologist, CBT Certified",
    languages: "English, Hindi, Punjabi",
    location: "Mumbai, Maharashtra",
    price: 1200,
    priceUnit: "per session",
    rating: 4.8,
    reviewCount: 127,
    availability: "Available",
    specializations: ["Anxiety Disorders", "Depression", "Trauma Therapy", "CBT"],
    photoUrl: "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Rahul Verma",
    email: "rahul.verma@reliefnet.com",
    password: "password123",
    medicalId: "MED10002",
    title: "Counseling Psychologist",
    specialization: "Counseling Psychologist",
    category: "Psychologist",
    description: "Dedicated counseling psychologist with expertise in relationship counseling, stress management, and personal development. Creating a safe space for healing and growth.",
    experience: "5+ years",
    education: "M.A. in Counseling Psychology, Mumbai University",
    certifications: "Licensed Professional Counselor, REBT Certified",
    languages: "English, Hindi, Marathi",
    location: "Pune, Maharashtra",
    price: 900,
    priceUnit: "per session",
    rating: 4.6,
    reviewCount: 89,
    availability: "Available",
    specializations: ["Relationship Counseling", "Stress Management", "Personal Development", "REBT"],
    photoUrl: "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Anjali Patel",
    email: "anjali.patel@reliefnet.com",
    password: "password123",
    medicalId: "MED10003",
    title: "Psychiatrist",
    specialization: "Psychiatrist",
    category: "Psychiatrist",
    description: "Board-certified psychiatrist specializing in medication management and treatment of complex mental health conditions. Combining medical expertise with therapeutic approaches.",
    experience: "12+ years",
    education: "MBBS, MD Psychiatry, AIIMS Delhi",
    certifications: "Board Certified Psychiatrist, Member of Indian Psychiatric Society",
    languages: "English, Hindi, Gujarati",
    location: "Delhi, NCR",
    price: 2000,
    priceUnit: "per session",
    rating: 4.9,
    reviewCount: 203,
    availability: "Available",
    specializations: ["Mood Disorders", "Anxiety Disorders", "Psychotic Disorders", "Medication Management"],
    photoUrl: "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Sneha Reddy",
    email: "sneha.reddy@reliefnet.com",
    password: "password123",
    medicalId: "MED10004",
    title: "Mental Health Therapist",
    specialization: "Mental Health Therapist",
    category: "Therapist",
    description: "Compassionate mental health therapist with extensive experience in treating anxiety, depression, and trauma. Providing personalized therapy in a supportive environment.",
    experience: "6+ years",
    education: "M.A. in Clinical Psychology, Bangalore University",
    certifications: "Licensed Mental Health Therapist, Trauma-Informed Care Certified",
    languages: "English, Hindi, Telugu, Kannada",
    location: "Bangalore, Karnataka",
    price: 1000,
    priceUnit: "per session",
    rating: 4.7,
    reviewCount: 156,
    availability: "Available",
    specializations: ["Anxiety Disorders", "Depression", "Trauma Therapy", "Mindfulness"],
    photoUrl: "https://images.unsplash.com/photo-1576091160399-112ba8d25d1f?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Meera Singh",
    email: "meera.singh@reliefnet.com",
    password: "password123",
    medicalId: "MED10005",
    title: "Child Psychologist",
    specialization: "Child Psychologist",
    category: "Psychologist",
    description: "Specialized child psychologist helping children and adolescents with behavioral issues, learning difficulties, and emotional challenges. Creating a nurturing environment for young minds.",
    experience: "4+ years",
    education: "Ph.D. in Child Psychology, Chandigarh University",
    certifications: "Licensed Child Psychologist, Play Therapy Certified",
    languages: "English, Hindi, Punjabi",
    location: "Chandigarh, Punjab",
    price: 1100,
    priceUnit: "per session",
    rating: 4.8,
    reviewCount: 94,
    availability: "Available",
    specializations: ["Child Psychology", "Behavioral Therapy", "Learning Disabilities", "Play Therapy"],
    photoUrl: "https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Amit Kumar",
    email: "amit.kumar@reliefnet.com",
    password: "password123",
    medicalId: "MED10006",
    title: "Family Therapist",
    specialization: "Family Therapist",
    category: "Therapist",
    description: "Experienced family therapist specializing in relationship dynamics, communication issues, and family conflict resolution. Helping families build stronger, healthier relationships.",
    experience: "7+ years",
    education: "M.A. in Family Therapy, Delhi University",
    certifications: "Licensed Family Therapist, Systemic Therapy Certified",
    languages: "English, Hindi, Bengali",
    location: "Kolkata, West Bengal",
    price: 1300,
    priceUnit: "per session",
    rating: 4.7,
    reviewCount: 112,
    availability: "Available",
    specializations: ["Family Therapy", "Couples Counseling", "Communication Skills", "Conflict Resolution"],
    photoUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Kavita Desai",
    email: "kavita.desai@reliefnet.com",
    password: "password123",
    medicalId: "MED10007",
    title: "Addiction Counselor",
    specialization: "Addiction Counselor",
    category: "Counselor",
    description: "Specialized addiction counselor with expertise in substance abuse treatment, behavioral addictions, and recovery support. Providing compassionate care for individuals on their recovery journey.",
    experience: "9+ years",
    education: "M.A. in Addiction Counseling, Mumbai University",
    certifications: "Licensed Addiction Counselor, Recovery Coach Certified",
    languages: "English, Hindi, Marathi, Gujarati",
    location: "Ahmedabad, Gujarat",
    price: 950,
    priceUnit: "per session",
    rating: 4.6,
    reviewCount: 78,
    availability: "Available",
    specializations: ["Substance Abuse", "Behavioral Addictions", "Recovery Support", "Relapse Prevention"],
    photoUrl: "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Rajesh Malhotra",
    email: "rajesh.malhotra@reliefnet.com",
    password: "password123",
    medicalId: "MED10008",
    title: "Trauma Therapist",
    specialization: "Trauma Therapist",
    category: "Therapist",
    description: "Specialized trauma therapist helping individuals heal from PTSD, childhood trauma, and complex trauma. Using evidence-based approaches for trauma recovery.",
    experience: "10+ years",
    education: "Ph.D. in Trauma Psychology, Delhi University",
    certifications: "Licensed Trauma Therapist, EMDR Certified",
    languages: "English, Hindi, Punjabi",
    location: "Jaipur, Rajasthan",
    price: 1400,
    priceUnit: "per session",
    rating: 4.9,
    reviewCount: 145,
    availability: "Available",
    specializations: ["PTSD", "Childhood Trauma", "Complex Trauma", "EMDR Therapy"],
    photoUrl: "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Sunita Iyer",
    email: "sunita.iyer@reliefnet.com",
    password: "password123",
    medicalId: "MED10009",
    title: "Anxiety & Depression Specialist",
    specialization: "Anxiety & Depression Specialist",
    category: "Specialist",
    description: "Dedicated specialist focusing on anxiety and depression treatment using cognitive behavioral therapy and mindfulness approaches. Helping clients overcome mental health challenges.",
    experience: "6+ years",
    education: "M.A. in Clinical Psychology, Chennai University",
    certifications: "Licensed Mental Health Specialist, CBT Certified",
    languages: "English, Hindi, Tamil, Malayalam",
    location: "Chennai, Tamil Nadu",
    price: 1150,
    priceUnit: "per session",
    rating: 4.8,
    reviewCount: 167,
    availability: "Available",
    specializations: ["Anxiety Disorders", "Depression", "CBT", "Mindfulness"],
    photoUrl: "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=150&h=150&fit=crop&crop=face"
  },
  {
    name: "Dr. Vikram Singh",
    email: "vikram.singh@reliefnet.com",
    password: "password123",
    medicalId: "MED10010",
    title: "Cognitive Behavioral Therapist",
    specialization: "Cognitive Behavioral Therapist",
    category: "Therapist",
    description: "Expert CBT therapist helping clients identify and change negative thought patterns and behaviors. Specializing in evidence-based cognitive behavioral therapy.",
    experience: "8+ years",
    education: "Ph.D. in Psychology, Bangalore University",
    certifications: "Licensed CBT Therapist, Beck Institute Certified",
    languages: "English, Hindi, Kannada",
    location: "Hyderabad, Telangana",
    price: 1250,
    priceUnit: "per session",
    rating: 4.7,
    reviewCount: 134,
    availability: "Available",
    specializations: ["CBT", "Thought Patterns", "Behavioral Change", "Anxiety Treatment"],
    photoUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face"
  }
];

async function migrateProfessionals() {
  try {
    console.log('Starting mental health professionals data migration...');
    
    // Clear existing data
    await Doctor.deleteMany({});
    console.log('Cleared existing professional data');
    
    // Hash passwords
    for (let prof of sampleProfessionals) {
      prof.password = await bcrypt.hash(prof.password, 10);
    }
    
    // Insert sample data
    const insertedProfessionals = await Doctor.insertMany(sampleProfessionals);
    console.log(`Successfully inserted ${insertedProfessionals.length} mental health professionals`);
    
    // Log the specializations for verification
    const specializations = [...new Set(sampleProfessionals.map(p => p.specialization))];
    console.log('Mental health specializations added:', specializations);
    
    console.log('Mental health professionals migration completed successfully!');
    process.exit(0);
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  }
}

migrateProfessionals();
