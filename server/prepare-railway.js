#!/usr/bin/env node

/**
 * Railway Deployment Preparation Script
 * Generates necessary secrets and validates configuration
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

console.log('🚄 Railway Deployment Preparation\n');
console.log('='.repeat(50));

// Generate JWT Secret
console.log('\n1️⃣  JWT Secret Generation');
console.log('-'.repeat(50));
const jwtSecret = crypto.randomBytes(64).toString('hex');
console.log('Generated JWT Secret:');
console.log(jwtSecret);
console.log('\n✅ Copy this to Railway as: JWT_SECRET');

// Check for required files
console.log('\n2️⃣  File Validation');
console.log('-'.repeat(50));

const requiredFiles = [
  'package.json',
  'server.js',
  'railway.json',
  'Procfile'
];

let allFilesExist = true;
requiredFiles.forEach(file => {
  const exists = fs.existsSync(path.join(__dirname, file));
  console.log(`${exists ? '✅' : '❌'} ${file}`);
  if (!exists) allFilesExist = false;
});

// Check for service account
console.log('\n3️⃣  Firebase Service Account');
console.log('-'.repeat(50));

const serviceAccountPaths = [
  'serviceAccountKey.json',
  'serviceAccountKey.json.json'
];

let serviceAccountPath = null;
for (const path of serviceAccountPaths) {
  if (fs.existsSync(path)) {
    serviceAccountPath = path;
    break;
  }
}

if (serviceAccountPath) {
  console.log(`✅ Found: ${serviceAccountPath}`);
  
  try {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
    
    console.log('\n📋 Firebase Configuration for Railway:');
    console.log('-'.repeat(50));
    console.log('FIREBASE_PROJECT_ID=' + serviceAccount.project_id);
    console.log('FIREBASE_CLIENT_EMAIL=' + serviceAccount.client_email);
    console.log('\nFIREBASE_PRIVATE_KEY=');
    console.log(serviceAccount.private_key);
    
    // Base64 encode
    const base64 = Buffer.from(JSON.stringify(serviceAccount)).toString('base64');
    console.log('\n📦 Alternative - Base64 Encoded (easier to copy):');
    console.log('-'.repeat(50));
    console.log('FIREBASE_SERVICE_ACCOUNT_BASE64=' + base64);
    
  } catch (error) {
    console.log('❌ Error reading service account:', error.message);
  }
} else {
  console.log('❌ Service account file not found');
  console.log('   Looking for: serviceAccountKey.json or serviceAccountKey.json.json');
}

// Check package.json
console.log('\n4️⃣  Package.json Validation');
console.log('-'.repeat(50));

try {
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  
  if (packageJson.scripts && packageJson.scripts.start) {
    console.log('✅ Start script found:', packageJson.scripts.start);
  } else {
    console.log('❌ No start script in package.json');
    console.log('   Add: "start": "node server.js"');
  }
  
  // Check dependencies
  const requiredDeps = [
    'express',
    'mongoose',
    'firebase-admin',
    '@sendgrid/mail',
    'socket.io',
    'dotenv'
  ];
  
  console.log('\n📦 Required Dependencies:');
  requiredDeps.forEach(dep => {
    const exists = packageJson.dependencies && packageJson.dependencies[dep];
    console.log(`${exists ? '✅' : '❌'} ${dep}`);
  });
  
} catch (error) {
  console.log('❌ Error reading package.json:', error.message);
}

// Generate environment variables template
console.log('\n5️⃣  Environment Variables Checklist');
console.log('-'.repeat(50));

const envVars = [
  { name: 'NODE_ENV', required: true, example: 'production' },
  { name: 'PORT', required: false, example: '8080 (Railway sets this)' },
  { name: 'MONGODB_URI', required: true, example: 'mongodb+srv://...' },
  { name: 'SENDGRID_API_KEY', required: true, example: 'SG.xxx' },
  { name: 'SENDGRID_FROM_EMAIL', required: true, example: 'noreply@example.com' },
  { name: 'JWT_SECRET', required: true, example: 'Generated above' },
  { name: 'FIREBASE_PROJECT_ID', required: true, example: 'From service account' },
  { name: 'FIREBASE_PRIVATE_KEY', required: true, example: 'From service account' },
  { name: 'FIREBASE_CLIENT_EMAIL', required: true, example: 'From service account' },
  { name: 'ALLOWED_ORIGINS', required: false, example: 'https://app.com,capacitor://localhost' },
  { name: 'RAZORPAY_KEY_ID', required: false, example: 'For payments' },
  { name: 'RAZORPAY_KEY_SECRET', required: false, example: 'For payments' }
];

envVars.forEach(({ name, required, example }) => {
  const marker = required ? '🔴' : '🟡';
  console.log(`${marker} ${name.padEnd(30)} ${example}`);
});

console.log('\n🔴 = Required | 🟡 = Optional');

// Deployment instructions
console.log('\n6️⃣  Next Steps');
console.log('-'.repeat(50));
console.log('1. Create account at https://railway.app');
console.log('2. Create new project → "Deploy from GitHub repo"');
console.log('3. Select your repository');
console.log('4. Go to Variables tab and add all environment variables');
console.log('5. Railway will automatically deploy!');

console.log('\n7️⃣  Important MongoDB Setup');
console.log('-'.repeat(50));
console.log('⚠️  Add Railway to MongoDB Atlas whitelist:');
console.log('   1. Go to MongoDB Atlas → Network Access');
console.log('   2. Click "Add IP Address"');
console.log('   3. Add: 0.0.0.0/0 (Allow from anywhere)');
console.log('   4. Or use Railway\'s static IPs for better security');

console.log('\n8️⃣  Testing After Deployment');
console.log('-'.repeat(50));
console.log('curl https://your-app.railway.app/health');
console.log('curl https://your-app.railway.app/');

console.log('\n✅ Preparation Complete!');
console.log('='.repeat(50));
console.log('\nSee RAILWAY_SETUP_GUIDE.md for detailed instructions.\n');
