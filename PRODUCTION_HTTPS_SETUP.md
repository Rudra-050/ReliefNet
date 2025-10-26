# Production setup with your own domain + HTTPS

This guide gets your ReliefNet backend live on a public HTTPS domain like `https://api.yourdomain.com`, and wires the Android release build to it.

## What you’ll get
- Public HTTPS API endpoint on your own domain (TLS cert auto-managed)
- Safe reverse-proxy settings (trust proxy + HTTPS redirect + HSTS)
- Clean environment variable setup for MongoDB, JWT, SendGrid, PhonePe
- Android release BASE_URL pointed to your domain

---

## 1) Pick a hosting option (recommended: Render)

Render makes HTTPS + custom domains easy, with zero-ops TLS certificates.

- Create a free account at https://render.com
- Push this repo to GitHub (or a private mirror that Render can read)
- New → Web Service
  - Repository: select this repo
  - Root directory: `server`
  - Runtime: Node
  - Build command: `npm install`
  - Start command: `npm start`
  - Instance type:
    - **Free** (testing only; spins down after 15min idle → 30-60s cold starts)
    - **Starter ($7/mo)** — recommended for production (always-on, no cold starts)
    - **Professional ($25/mo)** — for scaling (2GB RAM, horizontal scaling, zero-downtime deploys)

When it finishes, you’ll get a temp URL, e.g. `https://reliefnet.onrender.com`.

Verify health:
- Open `https://<render-url>/health` → expect `{ status: "ok", db: "connected", ... }`

---

## 2) Add your custom domain

In the Render service:
- Settings → Custom Domains → Add `api.yourdomain.com`
- Render shows a DNS record to add (usually a CNAME to your render URL)
- Go to your domain DNS provider (Cloudflare/Namecheap/GoDaddy):
  - Add the CNAME record exactly as shown
  - Wait for DNS to propagate (5–30 minutes)
- Render will auto-issue an HTTPS certificate when DNS is correct

Test: `https://api.yourdomain.com/health` should return the JSON health payload.

---

## 3) Environment variables (production)

In Render → Environment → add the following:

Required
- NODE_ENV = `production`
- PORT = `10000` (Render sets this automatically; you can omit)
- APP_BASE_URL = `https://api.yourdomain.com`
- JWT_SECRET = a long, random string
- MONGODB_URI = your Atlas connection string

Email (if using OTP/doctor emails)
- SENDGRID_API_KEY = from SendGrid
- SENDGRID_FROM_EMAIL = verified sender (e.g. `no-reply@yourdomain.com`)
- SENDGRID_REPLY_TO = support email

PhonePe (if payments live)
- PHONEPE_ENV = `production` (or `sandbox` for testing)
- PHONEPE_MERCHANT_ID
- PHONEPE_SALT_KEY
- PHONEPE_SALT_INDEX = `1`

Optional (for any web client CORS)
- FRONTEND_URL = `https://app.yourdomain.com`

No server restart? Click “Deploy latest commit” after adding env.

---

## 4) Server is HTTPS-aware (already wired)

In `server/server.js` we added:
- `app.set('trust proxy', 1)` to trust the reverse proxy
- HTTPS redirect for GET/HEAD in production (prevents loops and preserves POST webhooks)
- HSTS in production (forces browsers to use HTTPS on your domain)

Nothing you need to do here—just deploy with `NODE_ENV=production`.

---

## 5) Update Android release BASE_URL

Open `Reliefnet-android/app/build.gradle.kts` and set release BASE_URL:

- release: `BASE_URL = "https://api.yourdomain.com/"`

Rebuild release:
- Windows PowerShell (from `Reliefnet-android`):

```powershell
./gradlew :app:assembleRelease
```

The APK/AAB will be in `Reliefnet-android/app/build/outputs/`.

Note
- Debug already uses `http://10.0.2.2:5000/` (emulator → local dev)
- Release must point to your HTTPS domain

---

## 6) PhonePe callbacks

We already use `APP_BASE_URL` for PhonePe URLs:
- redirectUrl: `${APP_BASE_URL}/api/payments/callback`
- callbackUrl: `${APP_BASE_URL}/api/payments/webhook`

When you set `APP_BASE_URL` to `https://api.yourdomain.com`, PhonePe will call back to your HTTPS endpoints.

In PhonePe dashboard, whitelist these URLs if required.

---

## 7) Quick production checklist

- [ ] `/health` returns ok on your custom domain
- [ ] MongoDB Atlas IP access allows your hosting region
- [ ] JWT_SECRET set and long/random
- [ ] SendGrid sender verified and keys set (if you use email)
- [ ] PhonePe creds set and ENV=`production` (if live payments)
- [ ] Android release BASE_URL updated and app installs/tests OK on device
- [ ] Optional: set `APP_URL` for links in emails (e.g., `https://reliefnet.app`)

---

## 8) Alternative: VPS + Nginx + Let’s Encrypt (DIY)

If you prefer your own VM (Ubuntu):
- Point `api.yourdomain.com` A record to your server IP
- Install Node LTS and pm2 (or Docker Compose)
- Reverse proxy with Nginx to `http://localhost:<PORT>`
- Use `certbot --nginx` to issue/renew TLS automatically

Example Nginx server block (TLS terminates at Nginx):

```
server {
  listen 80;
  server_name api.yourdomain.com;
  return 301 https://$host$request_uri;
}

server {
  listen 443 ssl http2;
  server_name api.yourdomain.com;

  ssl_certificate     /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;

  location / {
    proxy_pass http://127.0.0.1:5000;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

With the `trust proxy` we added, Express will detect HTTPS correctly.

---

If you share your domain name, I can update the Android release BASE_URL now and produce a signed release build checklist for you.
