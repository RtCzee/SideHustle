# SideHustle API

Node.js + Express REST API for the SideHustle Android app. Issue **#7** scaffold: health check, Firebase token auth, JSON errors.

PostgreSQL and business routes come in issues **#8** and later.

## Requirements

- [Node.js](https://nodejs.org/) 18 or newer
- Firebase project **sidehustle-d709c** (same as the Android app)

## Setup

1. Copy the example env file:

   ```bash
   cd backend
   cp .env.example .env
   ```

2. **Firebase service account** (do this in the Firebase Console):
   - Project settings → **Service accounts** → **Generate new private key**
   - Save the JSON file as `backend/Api-Secrets.json` (gitignored)
   - Do **not** commit this file

3. Install dependencies:

   ```bash
   npm install
   ```

4. Start the server:

   ```bash
   npm run dev
   ```

## Test locally

### Health (no auth)

```bash
curl http://localhost:3000/health
```

Expected: `{"status":"ok","service":"sidehustle-api"}`

### Protected route (needs Firebase ID token)

```bash
curl http://localhost:3000/me
```

Expected: `401` with JSON `{"error":"..."}`

With a valid token:

```bash
curl -H "Authorization: Bearer YOUR_ID_TOKEN" http://localhost:3000/me
```

Expected: `200` with `uid` and `email`.

#### Getting an ID token

Sign in with your test user via Firebase Auth REST API (use the Web API key from `app/google-services.json`):

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_API_KEY
Content-Type: application/json

{
  "email": "test@sidehustle.dev",
  "password": "YOUR_PASSWORD",
  "returnSecureToken": true
}
```

Use the `idToken` from the response as `YOUR_ID_TOKEN`.

## Folder layout

```text
backend/
  src/
    config/       Firebase Admin
    middleware/   auth, errors
    routes/       HTTP routes
    db/           PostgreSQL (issue #8)
  secrets/        optional folder for other local secrets (gitignored)
```

## Scripts

| Command       | Description        |
|---------------|--------------------|
| `npm run dev` | Run with auto-reload |
| `npm start`   | Run once           |
