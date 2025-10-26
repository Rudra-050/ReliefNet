# ReliefNet Server

Node.js + Express backend for the ReliefNet mental health application.

## Features

- 🔐 JWT Authentication (Password & OTP-based)
- 📧 Email OTP Login via SendGrid
- 🏥 Doctor/Patient Management
- 💬 Real-time Chat (Socket.IO)
- 📅 Booking System
- 💳 Payment Integration (Razorpay)
- 🔔 Push Notifications (Firebase)
- 🗄️ MongoDB Atlas Database

## Quick Start

### Prerequisites
- Node.js 18+ 
- MongoDB Atlas account
- SendGrid account (for OTP emails)
- Firebase project (for push notifications)

### Setup

1. **Install dependencies:**
```bash
cd server
npm install
```

2. **Configure environment variables:**
```bash
cp .env.example .env
# Edit .env with your credentials (see .env.example for details)
```

3. **Add Firebase service account:**
   - Download `serviceAccountKey.json` from Firebase Console
   - Place it in the `server/` directory

4. **Start server:**
```bash
node server.js
```

Server will start at `http://localhost:5000`

## Environment Variables

See `.env.example` for all required environment variables.

**Required:**
- `MONGODB_URI` - MongoDB Atlas connection string
- `SENDGRID_API_KEY` - SendGrid API key for emails
- `SENDGRID_FROM_EMAIL` - Verified sender email

**Optional:**
- `NODE_ENV` - Set to `development` for testing features

## API Endpoints

### Authentication
- `POST /api/patient/signup` - Register patient
- `POST /api/patient/login` - Login with password
- `POST /api/auth/send-otp` - Send OTP to email
- `POST /api/auth/verify-otp` - Verify OTP and login
- `POST /api/doctor/login` - Doctor login with Medical ID
- `POST /api/doctors/register` - Register doctor

### Health Check
- `GET /health` - Server and DB status

### Profiles
- `GET /api/patient/profile` - Get patient profile (auth)
- `PUT /api/patient/profile` - Update patient profile (auth)
- `GET /api/doctor/profile` - Get doctor profile (auth)

### Professionals
- `GET /api/professionals` - List mental health professionals
- `GET /api/professionals/:id` - Get professional details
- `GET /api/professionals/search` - Search professionals

### Bookings, Chats, Sessions
See `server.js` for complete endpoint list.

## Security

- ⚠️ Never commit `.env` or `serviceAccountKey.json` to git
- ⚠️ Both files are protected by `.gitignore`
- ⚠️ Rotate credentials regularly
- ⚠️ Use environment variables in production

## Credentials Management

Your credentials are stored in:
- `.env` - Active credentials (local only, not in git)
- `CREDENTIALS.md` - Backup reference (also in .gitignore)

See `.env.example` for setup instructions.

## Testing

```bash
# Health check
curl http://localhost:5000/health

# Create test patient
curl -X POST http://localhost:5000/api/patient/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","name":"Test User"}'

# Send OTP
curl -X POST http://localhost:5000/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

## Troubleshooting

### MongoDB Connection Failed
- Check if your IP is whitelisted in MongoDB Atlas → Network Access
- Verify credentials in `.env`
- Ensure database user has proper permissions

### SendGrid Email Not Sending
- Verify sender email at https://app.sendgrid.com/settings/sender_auth/senders
- Check API key is active
- Look for errors in server console

### Port 5000 Already in Use
```bash
# Windows
netstat -ano | findstr :5000
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:5000 | xargs kill -9
```

## License

MIT

## Support

For issues, contact the development team or open an issue in the repository.
