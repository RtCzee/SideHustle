# SideHustle

An Android app for students, freelancers, and small side-hustle owners to manage **clients, jobs, income, expenses, and business performance** in one place.

This repository is the Part 2 prototype (and later the PoE). Source lives on GitHub only no zip submissions.

**Status:** Issue #1 complete app skeleton and navigation run on a physical phone.

---

## Purpose

Side hustles are often run from a mix of chats, notes, and spreadsheets. SideHustle puts the admin in one app so users can:

- keep client details together
- track jobs/projects and their status
- record income and expenses
- see profit, outstanding payments, and a **SideHustle Score** on a dashboard

The prototype must compile and run, support register/login with encrypted passwords, let users change settings, talk to a **hosted REST API + database**, and survive invalid input without crashing.

---

## Design (short)

From our Part 1 design:

| Layer | Choice |
| --- | --- |
| Android UI | Kotlin, Material 3, Navigation Component |
| Auth | Firebase Authentication (email/password; passwords hashed by Firebase) |
| API | Node.js + Express, called with Retrofit |
| Database | PostgreSQL (hosted, e.g. Railway or Render) |
| Files / push | Firebase Storage and FCM (PoE) |
| Offline | Room + sync (PoE) |

**Screens in the skeleton:** Login, Register, Dashboard, Clients, Client details, Projects, Project details, Invoices, Expenses, Settings.

**Deferred to PoE:** offline mode, Google Sign-In, FCM, file attachments, extra languages, final artwork.

**Libraries (why they are here)**

- **Firebase Auth** — assignment SDK; email/password login with hashed passwords
- **Retrofit + Gson** — REST calls to our hosted API (connected in a later issue)
- **Room** — not added yet; reserved for PoE offline mode

---

## How we use GitHub

- Repo: [RtCzee/SideHustle](https://github.com/RtCzee/SideHustle)
- Work is split into **milestones**: Phase 1 (foundation, auth, API), Phase 2 (core features), Phase 3 (tests, polish, demo), PoE
- Each task is a **GitHub issue** with an overview, behaviour, and completion checklist
- Feature work goes on a branch (example: `issue/1-android-app-architecture`), then we commit and push as we go

---

## GitHub Actions

Not set up yet. We will add a workflow based on the module guides:

- [Automated Build Android App with GitHub Action](https://github.com/marketplace/actions/automated-build-android-app-with-github-action)
- [IMAD5112 sample `build.yml`](https://github.com/IMAD5112/Github-actions/blob/main/.github/workflows/build.yml)

Plan: run `./gradlew test` and a debug build on push/PR so the app is checked on GitHub’s machines, not only on one laptop. A `release` branch will produce APK/AAB artifacts for submission.

---

## Running the app

1. Open the project in Android Studio
2. Sync Gradle
3. Run the `app` configuration on a phone or emulator

**Issue #1 check:** Login → Create account / Continue to app → bottom nav (Dashboard, Clients, Projects, Finances, Settings) → details screens.

---

## Demo video

_Link will be added here after the Phase 3 demonstration video (voice-over, phone, hosted auth/API/database)._

---

## AI tools used

This section is the short write-up of how AI was used (under 500 words). It will be updated as we go.

**Tool:** Cursor (model: Cursor Grok 4.6), used inside the IDE as a coding assistant.

The **product idea is ours**. SideHustle, the competitor research, screens, data model, and tech choices come from our Part 1 document (`OPSC6312 Part 1`). AI did not invent the app concept.

**What AI has been used for so far**

1. **Planning** - a GitHub issue list grouped into Phase 1–3 and PoE, each with a feature overview and completion requirements.


2. **GitHub setup** - creating the issue locally on a txt file and then using the GitHub CLI , to proceed and take those issues i wrote and push them all onto github all at once at the same time.


3. **Issue #1 implementation** - Kotlin package layout (`ui`, `data`, `network`, `util`), Material 3, Navigation Component, placeholder screens, and bottom navigation.


4. **Git workflow help** - issue branch, commit message, push, and ignoring local Android Studio `.idea` files so they do not clutter pull requests.


5. **Submission planning** - how our  README will be structurd Git actions, comments, logging, how we plan to do the demo video, and this AI note fit the mark scheme.

6. **Google Sign-In icon** — Cursor’s image generator made the colourful G logo used on the “Continue with Google” button (`app/src/main/res/drawable/ic_google_logo.png`). The button is on the register screen now; Google Sign-In itself is still a later issue.

<small><em>In summary, AI was used for some planning and automating the most repetitive stuff.</em></small>

**What was not done by AI**

- Running and checking the app on a **physical phone** in Android Studio
- Fixing a **Gradle/Kotlin plugin build error** after the skeleton was generated (AGP 9 already includes Kotlin; the extra plugin was removed)
- Creating git branches and confirming navigation actually works on device

**Citation**

Cursor (2026). *Cursor Grok 4.6* coding assistant. Used for assignment planning, GitHub issue setup, the issue #1 app skeleton, the register screen, and this README draft. Human team members reviewed the output, ran the app on a device, and keep ownership of design decisions.

Cursor (2026). *GenerateImage*. Used to create the Google-style G icon for the future Google authentication button on the register screen.

---

## Team

Work is on GitHub under [RtCzee/SideHustle](https://github.com/RtCzee/SideHustle).

| GitHub | Name |
| --- | --- |
| [RtCzee](https://github.com/RtCzee) | Didintle Mokgoro |
| [sibongiseni-ngwamba](https://github.com/sibongiseni-ngwamba) | Sibongiseni Ngwamaba |
| [Globoysosa](https://github.com/Globoysosa) | Thokozani Masondo |
| [OnesimoTuswa](https://github.com/OnesimoTuswa) | Onesimo Tuswa |
