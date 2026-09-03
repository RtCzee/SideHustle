# Contributing

## Branch workflow

1. Branch off **`dev`** (not `master`):
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b issue/10-your-feature
   ```
2. Commit and push your branch.
3. Open a pull request into **`dev`**.
4. Wait for **@RtCzee** to review and merge.
5. **`master`** is updated by the lead after review — do not open PRs to `master` unless asked.

## Rules

- Do not push directly to `dev` or `master`.
- Do not commit secrets (`.env`, Firebase service account JSON, API keys).
- Keep PRs focused on one issue when possible.

---

## Development environment

**Full setup guide:** [DEV_SETUP.md](DEV_SETUP.md)

### Android-only work (UI, polish, existing screens)

- Clone repo → open in Android Studio → run `app`
- No Firebase or Railway setup required on your machine
- The app uses the hosted API on Railway automatically

### Work that needs new API endpoints

1. Add backend route and DB logic under `backend/src/`
2. Add Android model, Retrofit method, and repository method
3. Call `SideHustleApp.repository` from your Fragment
4. Open a PR into **`dev`** — Railway redeploys from `dev`

### Access you should have

| Service | Purpose |
|---------|---------|
| **GitHub** | Clone, branch, PR to `dev` |
| **Firebase** (`sidehustle-d709c`) | Editor — backend devs generate a service account key |
| **Railway** | Member — view logs and Postgres; never paste secrets into GitHub |

### After merging to `dev`

- Wait for Railway to redeploy (about 1–2 minutes)
- On device: leave the Dashboard tab and return, or restart the app, to refresh API data
- No Android rebuild needed for backend-only changes

### Never commit

- `backend/.env`
- `backend/secrets/` or any `*firebase-adminsdk*.json`
