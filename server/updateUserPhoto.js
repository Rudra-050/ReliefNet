/**
 * Update Existing User Profile with Photo URL
 * 
 * This script updates users who logged in before photo implementation
 * Run this to add photo URLs to existing accounts
 */

const BASE_URL = 'http://localhost:5000';

console.log('\n╔════════════════════════════════════════════════════════╗');
console.log('║     UPDATE EXISTING USER WITH PHOTO                    ║');
console.log('╚════════════════════════════════════════════════════════╝\n');

async function updateUserWithPhoto() {
  console.log('🔍 Instructions to add your photo:\n');
  
  console.log('OPTION 1: Re-login with Google (Easiest)\n');
  console.log('   1. In the app, find and tap "Logout" or "Sign Out"');
  console.log('   2. Tap "Continue with Google" again');
  console.log('   3. Select your Google account');
  console.log('   4. Your photo will now be fetched and stored\n');
  
  console.log('OPTION 2: Manual Profile Update via API\n');
  console.log('   If you want to manually update your photo URL:');
  console.log('   1. Get your current JWT token from the app');
  console.log('   2. Run this command:\n');
  
  const exampleCommand = `
   curl -X PUT http://localhost:5000/api/patient/profile \\
     -H "Authorization: Bearer YOUR_TOKEN" \\
     -H "Content-Type: application/json" \\
     -d "{\\"photoUrl\\":\\"YOUR_GOOGLE_PHOTO_URL\\"}"
  `;
  
  console.log(exampleCommand);
  
  console.log('\n📝 How to get your Google photo URL:');
  console.log('   1. Go to https://myaccount.google.com');
  console.log('   2. Click on your profile picture');
  console.log('   3. Right-click the large photo → Copy image address');
  console.log('   4. Use that URL in the API call above\n');
  
  console.log('OPTION 3: Database Direct Update (Advanced)\n');
  console.log('   If you have MongoDB access, update directly:');
  console.log('   1. Connect to MongoDB');
  console.log('   2. Find your user document');
  console.log('   3. Add/update the photoUrl field\n');
  
  console.log('═'.repeat(60) + '\n');
  console.log('🎯 RECOMMENDED: Just logout and login again!\n');
  console.log('   This is the easiest way to get your photo synced.\n');
  console.log('═'.repeat(60) + '\n');
}

updateUserWithPhoto();
