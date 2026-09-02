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
