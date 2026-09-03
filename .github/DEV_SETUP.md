# SideHustle — Developer setup

Guide for teammates joining the project. Read this before your first issue.

---

## 1. Accounts and access

You should already have invites to:

| Service | Project | Role | Why |
|---------|---------|------|-----|
| **GitHub** | [RtCzee/SideHustle](https://github.com/RtCzee/SideHustle) | Collaborator | Code, PRs, issues |
| **Firebase** | `sidehustle-d709c` | Editor | View Auth users; generate service account key (backend devs only) |
| **Railway** | SideHustle | Member | View deploy logs, Postgres, env vars |

You do **not** need Firebase or Railway access for **Android-only UI work** that uses the hosted API.

---

## 2. Clone and branch workflow

```bash
git clone https://github.com/RtCzee/SideHustle.git
cd SideHustle
git checkout dev
git pull origin dev
git checkout -b issue/14-clients-screen
```

- Branch off **`dev`**, never `master`
- Open PRs into **`dev`**
- Railway deploys from **`dev`** — after merge, wait 1–2 minutes for redeploy

See [CONTRIBUTING.md](CONTRIBUTING.md) for full rules.

---

## 3. Android setup (everyone)

### Requirements

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 11+ (bundled with Android Studio)
- Phone or emulator with **internet**

### Steps

1. Open the project folder in Android Studio
2. **Sync Gradle** when prompted
3. Run the **`app`** configuration

### No extra config needed

- `app/google-services.json` is **already in the repo** — Firebase Auth works after clone
- API URL is set in `app/build.gradle.kts`: `https://sidehustle-production-f596.up.railway.app/`
- Login/register creates a Firebase user and a PostgreSQL profile via the hosted API

### Quick test

1. Register or log in
2. Dashboard should show your **name** and metrics (zeros if you have no finance data yet)
3. Logcat filter: **`SideHustle`** — look for `Dashboard loaded for …`

### Where UI work lives

```text
app/src/main/java/com/example/sidehustle/
  ui/              Fragments and screens
  ui/navigation/   Custom bottom nav
  network/         Retrofit client
  data/model/      JSON models (ApiModels.kt)
  data/repository/ App-facing API methods
  data/remote/     HTTP error handling
```

---

## 4. When your issue needs new API data

Phase 2 features (clients, jobs, income, etc.) need **backend + Android** work. Follow this pattern:

### Backend (Node.js)

1. Add DB helper: `backend/src/db/<resource>.js`
2. Add routes: `backend/src/routes/<resource>.js`
3. Mount in `backend/src/routes/index.js`
4. Test with `curl` and a Firebase ID token
5. PR to `dev` — Railway redeploys automatically

### Android

1. Add models in `data/model/ApiModels.kt`
2. Add endpoints in `network/SideHustleApi.kt`
3. Add methods in `data/remote/RemoteDataSource.kt`
4. Expose via `data/repository/SideHustleRepository.kt`
5. Call from your Fragment: `(requireActivity().application as SideHustleApp).repository`

**Auth is automatic** — `AuthInterceptor` attaches the Firebase token to every request.

### Current live endpoints

| Method | Path | Auth | Used by |
|--------|------|------|---------|
| GET | `/health` | No | Health check |
| GET | `/me` | Yes | Profile read |
| POST | `/me` | Yes | Create profile after register |
| PUT | `/me` | Yes | Update profile (settings, later) |
| GET | `/dashboard` | Yes | Dashboard screen |

More detail: [`backend/README.md`](../backend/README.md)

---

## 5. Backend local setup (API developers only)

Only needed if you run the API on your laptop or apply schema to Postgres.

### Requirements

- Node.js 18+
- PostgreSQL 14+ (local) or Railway Postgres public URL

### Steps

```bash
cd backend
cp .env.example .env
npm install
```

Edit `backend/.env`:

```env
PORT=3000
GOOGLE_APPLICATION_CREDENTIALS=./secrets/firebase-service-account.json
DATABASE_URL=postgresql://postgres:YOUR_PASSWORD@localhost:5432/sidehustle
```

**Firebase service account** (one per dev — do not commit):

1. Firebase Console → `sidehustle-d709c` → Project settings → Service accounts
2. Generate new private key
3. Save as `backend/secrets/firebase-service-account.json`

```bash
npm run db:schema    # create tables (first time)
npm run dev          # API on http://localhost:3000
```

**Point Android at local API** (optional, temporary):

In `app/build.gradle.kts`, change `API_BASE_URL`:

- Emulator: `"http://10.0.2.2:3000/"`
- Physical phone (same Wi‑Fi): `"http://YOUR_PC_IP:3000/"`

Revert before merging — production uses Railway.

---

## 6. Railway (hosted API)

- **Deploy branch:** `dev`
- **API URL:** `https://sidehustle-production-f596.up.railway.app/`
- After your PR merges to `dev`, Railway redeploys — no manual deploy needed
- Verify: `curl https://sidehustle-production-f596.up.railway.app/health`

**View data:** Railway → Postgres service → Query / Connect:

```sql
SELECT email, full_name FROM users;
SELECT * FROM income_records;
```

---

## 7. Secrets — never commit

| File | Gitignored? |
|------|-------------|
| `backend/.env` | Yes |
| `backend/secrets/*.json` | Yes |
| `app/google-services.json` | No (client config — safe in repo) |

---

## 8. Common issues

| Problem | Fix |
|---------|-----|
| Dashboard shows "metrics unavailable" banner | `/dashboard` not on Railway yet — merge backend to `dev`, wait for redeploy |
| "Profile not found" on login | Register again, or check `users` table in Postgres |
| API works on device but not emulator | Use `10.0.2.2` for localhost, not `127.0.0.1` |
| Nothing useful in Logcat | Filter by **`SideHustle`** |
| Dashboard stale after deploy | Switch away from Dashboard tab and back, or restart the app |

---

## 9. Who does what

| Task | Typical owner |
|------|----------------|
| UI layouts, styling, animations | Any teammate — Android only |
| New API endpoints + DB queries | Backend + Android together |
| Firebase auth screens | Done (Phase 1) |
| Clients, jobs, income, expenses | Phase 2 issues — needs API + UI |
| PR review and merge to `master` | @RtCzee |
