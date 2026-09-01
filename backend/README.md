# SideHustle API

Node.js + Express REST API for the SideHustle Android app.

- Issue **#7**: health check, Firebase token auth, JSON errors
- Issue **#8**: PostgreSQL schema + local database setup

## Requirements

- [Node.js](https://nodejs.org/) 18 or newer
- [PostgreSQL](https://www.postgresql.org/) 14+ (local install, or hosted in issue #9)
- Firebase project **sidehustle-d709c** (same as the Android app)

## Setup

1. Copy the example env file:

   ```bash
   cd backend
   cp .env.example .env
   ```

2. **Firebase service account** (Firebase Console):
   - Project settings → **Service accounts** → **Generate new private key**
   - **Local:** save as `backend/secrets/firebase-service-account.json` (gitignored)
   - **Railway (issue #9):** add variable `FIREBASE_SERVICE_ACCOUNT_JSON` with the full JSON pasted in (no file upload needed)

3. **PostgreSQL** (issue #8):
   - Create a database named `sidehustle` (or update `DATABASE_URL` in `.env`)
   - Example (psql or pgAdmin): `CREATE DATABASE sidehustle;`
   - Set `DATABASE_URL` in `.env`, e.g.:
     `postgresql://postgres:YOUR_PASSWORD@localhost:5432/sidehustle`

4. Install dependencies and apply schema:

   ```bash
   npm install
   npm run db:schema
   npm run db:seed
   ```

5. Start the server:

   ```bash
   npm run dev
   ```

## Test locally

### Health (includes database status when configured)

```bash
curl http://localhost:3000/health
```

Expected (with DB): `{"status":"ok","service":"sidehustle-api","database":"connected"}`

### Verify sample rows (psql or pgAdmin)

```sql
SELECT * FROM users;
SELECT * FROM clients;
SELECT * FROM jobs;
```

Or:

```bash
psql $DATABASE_URL -c "SELECT email, full_name FROM users;"
```

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
    db/           schema.sql, seed.sql, pool.js
  secrets/        Firebase service account (gitignored)
```

## Scripts

| Command            | Description                    |
|--------------------|--------------------------------|
| `npm run dev`      | Run API with auto-reload       |
| `npm start`        | Run API once                   |
| `npm run db:schema`| Create tables (schema.sql)     |
| `npm run db:seed`  | Insert demo rows (seed.sql)    |
| `npm run db:verify`| Print row counts (check seed)  |

## Deploy to Railway (issue #9)

1. **PostgreSQL** service — Railway provides `DATABASE_URL` automatically.
2. **API** service from GitHub repo:
   - **Root Directory:** `backend`
   - **Variables:**
     - `DATABASE_URL` → reference from Postgres service
     - `FIREBASE_SERVICE_ACCOUNT_JSON` → paste full service account JSON (from Firebase Console)
   - Do **not** set `PORT` manually; do **not** use `localhost` for `DATABASE_URL`.
3. Apply schema/seed to hosted Postgres from your laptop (use Postgres service **public** URL in `DATABASE_URL` temporarily):
   `npm run db:schema && npm run db:seed`
4. **Settings → Networking → Generate Domain**, then test `GET /health`.
