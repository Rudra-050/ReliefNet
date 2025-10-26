# Security Policy

Thank you for helping keep ReliefNet secure. This policy explains how we handle secrets, how to report vulnerabilities, and safe practices for contributors.

## Supported Versions

We maintain the main branch actively. If you discover a security issue, please report it privately regardless of version.

## Reporting a Vulnerability

- Please do not open a public issue for suspected security problems.
- Email: rudransh.bhatt120960@marwadiuniversity.ac.in (or repository owner via GitHub profile) with:
  - Description, reproduction steps, impact
  - A minimal PoC if possible
- We aim to acknowledge within 48 hours and remediate promptly.

## Secret Management

Never commit real secrets to the repository. Use environment variables for all credentials.

- Server
  - Secrets are stored in environment variables (.env) which are gitignored.
  - Do not commit `.env` or any service account JSONs.
  - Templates: `server/.env.example`, `server/.env.production.example`.
- Android
  - `google-services.json` is committed by design, but restrict API keys in Google Cloud Console to intended Android package/SHA-1 and APIs only.

### Approved secret locations

- Local/dev: `server/.env` (ignored by git)
- Railway/Prod: Project → Variables (Railway dashboard)

### Rotation checklist (if exposure suspected)

1. SendGrid: Create a new API key, update `SENDGRID_API_KEY`, delete the old key.
2. MongoDB: Change the database user password, update `MONGODB_URI` (URL-encode special chars), restart services.
3. Firebase Admin: Generate a new service account key, update the environment (or `FIREBASE_SERVICE_ACCOUNT_BASE64`), delete old key.
4. JWT Secret: Generate a new strong secret and redeploy.

## Development Hygiene

- Do not paste terminal-generated secrets into docs. Use placeholders instead.
- Validate with secret scanning locally before pushing when possible.
- Use least-privilege API keys (scope SendGrid keys appropriately).

## Contact

For urgent matters, reach out to the repository owner on GitHub.
