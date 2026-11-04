# Contributing

## Unit tests

Run `./gradlew test` (`gradlew.bat test` on Windows) from the repository root. This runs
the app's configured JVM suites and `node --test` in `backend/`. Use JDK 25 and Node.js
18+ on PATH; `-PnodeExecutable=/absolute/path/to/node` selects a different Node executable.
The backend suites use Node's built-in test runner and require no database, Firebase account,
device, API server, or npm dependency installation. A failing backend test fails the Gradle task.

Auth tests call the same validators as the forms, including email syntax, six-character password
boundaries, exact confirmation, optional contact fields, and finite non-negative job amounts.
Backend tests call the production rules used by dashboard profit, job status validation, invoice
creation, and the documented SideHustle Score. Invoices currently bill one job's agreed amount,
without invented line items or tax. Profit tests include losses and decimal cents; score tests
isolate the 50/30/20 weights, caps, rounding, zero income, and invalid data.

Run just the API rules with `cd backend && node --test`, or just Android JVM tests with
`./gradlew :app:test`. Use fixed expected results and boundary cases rather than reproducing a
formula inside assertions. Add coverage in the language that owns the production calculation.

## Error states

New API screens should use `ApiResult.Error.toUiState()` in `ui/common/error`:

| Failure | Message policy | Action |
| --- | --- | --- |
| Network / timeout | Localized connection or timeout message, using `ErrorKind` | Retry |
| 401 | Session expired | Log in (signs out Firebase and clears the navigation stack) |
| 404 | Item not found | Back; falls back to Dashboard or Login when there is no back stack |
| 400 / 409 / 422 | Short readable server validation message, otherwise a friendly fallback | Dismiss and fix input |
| 5xx | Safe server-error message | Retry |
| Unknown | Safe generic message; never raw exception text | Retry |

Place an `ErrorStateView` in the content container and call `showFullPageError(view, error, retry)`
when the initial request fails. Hide content and loading indicators before displaying it.
On nonfatal refresh or save failures, call `showInlineError(error, retry)` to retain rows and form
input. The Snackbar has a recovery action, stays above bottom navigation, and is dismissed with
the Fragment view. Call `dismissInlineError()` at the start of a new request.

Retry must invoke the failed operation directly. Use `viewLifecycleOwner.lifecycleScope`, cancel
superseded requests, and let `CancellationException` propagate. A cancelled request must not hide
the loading indicator owned by its replacement. Do not replace a 401 with a profile fallback.

Use `navigateSafely` for navigation. Unknown destinations and stale actions go to the shared 404
page. Project details require a UUID `jobId`; creation must explicitly pass `isCreating = true`
with no ID. Missing/blank/malformed IDs show 404 before any request; a well-formed absent ID uses
the API's 404 response. `sidehustle://projects/{jobId}` links open details;
`sidehustle://projects` (missing ID) and unknown SideHustle links show 404.

## Demo checks

1. Disable connectivity, open Dashboard or Jobs, and verify a connection error and Retry.
   Restore connectivity and tap Retry; content should load without restarting.
2. Return a delayed response or HTTP 500 from `/dashboard`, `/me`, or `/jobs` and verify the
   loading indicator resolves to a themed error; Retry makes another request.
3. Load Jobs successfully, then fail a pull-to-refresh. Existing rows remain visible with an
   actionable Snackbar. A successful retry replaces them with fresh data.
4. Return 401 and tap Log in. Firebase signs out, Login opens, and Back cannot reopen the old page.
5. Open `sidehustle://projects`, `sidehustle://projects/invalid`, or an unknown URI. Verify 404
   and Back. For a valid UUID absent from the database, verify the API's 404 also shows Back.
6. Return HTTP 400 on Save. Verify the validation message and Dismiss, with typed input retained.
7. Navigate away while a request is pending. Its cancellation must not update a destroyed view.

Run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` for local verification.
The instrumented `expiredSessionActionClearsProtectedNavigation` test signs out Firebase;
run it on an emulator or with a test account. Other error-state device tests preserve the session.

## Light and dark themes

The app follows the device theme through `Theme.Material3.DayNight`. Keep shared theme roles in
`values/themes.xml`; provide semantic color overrides in `values-night/colors.xml` instead of
hardcoding white backgrounds or dark text. All screens, cards, form controls, dialogs, loading
states, errors, and navigation should use these roles. System bar icon brightness also follows
night resources. Branded Google and launcher artwork keeps its original colors.

Use `background` for pages, `surface` for controls and dialogs, `card_surface` for cards,
`on_background` for primary text, and `on_surface_variant` for secondary text. Filled action
buttons retain lime with `on_primary` text; `primary` is the contrast-safe accent for text actions,
focused fields, and selections. Status badges pair their status color with `on_status`.

## Prototype navigation and list states

After login, the visible bottom bar opens Dashboard, Clients, Projects, Finances, and Settings.
Finances has labeled Invoices / Expenses / Income tabs. Each opens its list immediately;
Create invoice / Add expense / Record income opens the corresponding form. Empty states offer
the same action. Client creation is available from Clients; project creation is in Projects.
Invoices guide a new account to create a client and a project first. Refresh and Retry are visible.

Login/registration clear the auth stack. Logout in Settings and expired-session recovery clear
both the active stack and saved bottom-tab stacks. Auth destinations redirect an existing session
back to Dashboard; protected destinations require login. Missing detail IDs remain 404 states.

`ListStateHandler` in `ui/common/list` is the shared first-load / empty / error / refresh pattern
used by Clients and all three finance lists. Projects retains its existing equivalent pattern.
Only a successful zero-item response shows Empty. Initial failures replace the list with an
actionable error; refresh failures keep the last result and show an inline recovery action.
Use the Fragment view lifecycle and cancel superseded loads. Never use demo rows to hide failure.

MainActivity owns system-bar, keyboard, and bottom-navigation clearance. Do not add fixed 110dp
navigation padding to individual screens. Scrollable forms and navigation labels can grow with
phone font settings; the bottom bar hides while the keyboard is open. Device checks in
`PrototypeNavigationTest` never sign out the actual Firebase session. Stack-reset checks use an
isolated graph; the rapid-tab regression check uses the existing session for read-only requests
and skips when signed out.
