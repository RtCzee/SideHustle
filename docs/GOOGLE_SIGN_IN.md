# Google Sign-In for PoE

Login and Register both offer Continue with Google. The buttons use Android Credential
Manager's explicit Google button flow, which supports existing accounts and new users.
The Google ID token is exchanged using Firebase Auth's `GoogleAuthProvider` and
`signInWithCredential`. API requests then use the same Firebase ID token interceptor
as email/password sessions.

After authentication, the app fetches `GET /me`. An existing API profile is reused.
Only a 404 triggers `POST /me`, with the Google display name and the usual default
currency/language. Missing display names fall back to the email's local part. A 409
creation race triggers another GET instead of failing or overwriting an existing
profile. The dashboard opens after profile resolution succeeds. API failures show
a retry action. Email login uses the same profile loader.

## Firebase setup required

The currently checked-in `app/google-services.json` has an empty `oauth_client` list.
The Google flow therefore reports that configuration is missing; email/password
continues to work. To enable live Google login:

1. In Firebase project `sidehustle-d709c`, open Authentication → Sign-in method and
   enable Google. Select the project support email and save.
2. Under Project settings → Your apps, select Android app `com.example.sidehustle`.
   Add the SHA-1 signing fingerprint for the build you will install.
3. Download the updated `google-services.json` and replace the file in `app/`.
   The build reads its Web OAuth client (`client_type: 3`) for Google ID tokens.
   Use the Web client ID, rather than the Android client ID.
4. Rebuild and install on a device/emulator with current Google Play services.

The local debug build's fingerprint, verified with `:app:signingReport`, is:

```text
SHA-1: CB:A9:09:76:C9:AA:36:98:D2:D9:0A:AE:9A:FA:8A:20:7D:97:1E:2D
SHA-256: E0:6A:48:3E:4D:5C:09:AA:80:C6:DB:AE:FC:AF:2D:DD:95:02:A0:8E:96:1E:4F:C5:18:7D:04:59:1F:55:89:AA
```

Other machines and release/Play signing keys can have different fingerprints. Obtain
their values with `./gradlew :app:signingReport` or the Play Console as appropriate.
No release signing key is configured in this repository.

For CI or local overrides, supply Gradle property `GOOGLE_WEB_CLIENT_ID` or environment
variable of the same name. Gradle property takes precedence, followed by the environment,
then the matching Android client's Web OAuth ID in `google-services.json`. This does
not replace enabling the provider and registering the correct package/signing key.
OAuth client IDs are public configuration; client secrets are not needed in the app.

## Session and error behavior

Firebase Auth persists the Google user just as it persists email/password users. The
existing navigation/session guards keep signed-in users out of the login/register
screens, and sign-out returns them to Login. Sign-out also clears Credential Manager's
credential state, allowing a different Google account to be selected on the next login.

Closing the Google sheet restores the form controls and shows a cancellation message.
It does not create an API profile or navigate. Missing accounts/providers, invalid
credentials, disabled accounts, network failures, and missing configuration show safe
messages. Coroutine cancellation when the auth view is destroyed is propagated without
updating the destroyed view. Tokens and provider exception details are not logged by
the Google flow. If Firebase reports an account collision, the user is asked to use
the account's original sign-in method; this feature does not manually merge accounts.

## PoE verification

1. With Firebase configured, select Continue with Google from Login. Complete consent;
   verify the dashboard opens and the user has the Google provider in Firebase
   Authentication → Users. Verify `users.user_id` matches that Firebase UID.
2. Repeat from Register with a new Google account. No email/password form fields
   should be required; verify one API profile is created.
3. Sign out and sign in again with the same Google account. Verify existing jobs,
   expenses, settings, and the same API profile are retained.
4. Close/reopen the app while signed in; verify it resumes to the authenticated app.
   Sign out and reopen; verify Login remains accessible.
5. Open the Google sheet and cancel. Confirm no dashboard navigation, no crash, and
   that both email login and another Google attempt remain available.
6. Test a network/API failure and retry. Verify errors remain on the auth screen and
   a retry loads/creates the profile without duplicating it.

Automated tests cover profile creation/reuse, failures and conflicts, sheet cancellation,
view cancellation, and safe error messages. Real account consent, the Firebase Google
provider entry, and device session persistence require the configured live test above.

References: [Firebase Google authentication](https://firebase.google.com/docs/auth/android/google-signin)
and [Credential Manager Google button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).
