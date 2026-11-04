# FCM job reminders

The Android app registers its Firebase Cloud Messaging token with the authenticated
API at sign-in, token refresh, app resume, and after saving Settings. Each installation
has a device ID excluded from Android backup. Token refresh replaces that installation's
old token. Missing Play Services, missing tokens, denied permission, and network errors
leave the rest of the app usable; registration retries on the next sync.

The API checks jobs at startup and every 15 minutes. Jobs qualify when:

- They are Pending or In Progress.
- Their due date falls between today and today plus `user_settings.reminder_days_before`
  (default three days), using `REMINDER_TIME_ZONE` (default Africa/Johannesburg).
- The owner has `notifications_enabled = true` and a registered, enabled device.

Each device receives one reminder for each job/due-date pair. Changing a due date can
produce a new reminder. Completed, cancelled, undated, and already overdue jobs do not
qualify. This feature wires the job-due event; it does not send invoice reminders.

## Deployment

1. In the same Firebase project as `app/google-services.json`, enable the Firebase Cloud
   Messaging API (HTTP v1). The existing backend Firebase Admin service account must
   have permission to send messages for this project. No server key belongs in Android.
2. On an existing database, run `cd backend` then `npm run db:notifications` with
   `DATABASE_URL` configured. It creates `push_devices` and `job_reminder_deliveries`.
   Fresh installs receive these tables via `npm run db:schema`.
3. Deploy the updated API with Firebase credentials. `REMINDERS_ENABLED=true` starts
   the worker with the API. It must remain running for scheduled checks. For a sleeping
   service, set `REMINDERS_ENABLED=false` and schedule `npm run reminders:send` every
   15 minutes in a separate server cron job with the same environment and credentials.
4. Build/install the updated Android app on a device or emulator with Google Play
   Services. Sign in and allow the notification permission on Android 13 or newer.

`PUT /me/push-device` requires Firebase authentication and JSON:

```json
{
  "device_id": "11111111-1111-4111-8111-111111111111",
  "user_id": "signed-in-firebase-uid",
  "token": "fcm-registration-token",
  "enabled": true
}
```

The owner must match the authenticated user. `enabled` records whether Android permits
notifications on that device; the account's separate Settings preference also gates
all sends. Registration and reminder handlers avoid logging FCM tokens. Invalid or expired FCM tokens are removed when
FCM rejects them; transient failures release the delivery claim for the next check.

## Settings and navigation

Turn Notifications off in Settings and tap Save. The API persists the preference;
the device caches it and clears existing reminders after the save succeeds. The app
checks the cached preference and current account before displaying each data message,
including messages delivered while the app is in the background. Switching accounts
or signing out clears displayed reminders; messages for another account are suppressed.

Notification taps open that job's details. A tap while signed out retains the job target
through login, then opens it only for its owner. Missing or malformed remote targets
are ignored. Deleted jobs use the existing not-found details state.

## End-to-end check

1. Sign in on a Play Services device, grant notification permission, and verify its
   `push_devices` row has the correct owner and token, with `enabled=true`.
2. Create a Pending job due tomorrow. Close the app normally or swipe it from recents.
   Run `npm run reminders:send` on the configured backend (this sends real due reminders
   to all eligible devices), or wait for the next scheduled check.
3. Confirm the notification appears and tapping it opens the correct job. The matching
   `job_reminder_deliveries` row should have `sent_at` set.
4. Run the sender again; confirm no duplicate for that job/due-date/device pair.
5. Turn Notifications off and Save, then create another qualifying job. Confirm no
   push is sent. Reopen Settings and verify the switch remains off.
6. Deny Android notification permission; the app must remain usable and reminders must
   not display. Re-enable permission in Android settings and reopen the app to sync.
7. Test a device without an available FCM token or with networking disabled; sign-in and
   navigation must remain usable. A later resume with networking retries registration.

Database claims prevent concurrent workers from sending the same reminder. Claims expire
after 15 minutes following a worker crash. As with most external message delivery, a
crash after FCM accepts a message but before the database acknowledgement can cause a retry;
the app replaces notifications for the same job rather than adding another tray entry.
FCM acceptance is not proof of device delivery. Background delivery depends on Google
Play Services, device connectivity, and Android restrictions; an explicitly force-stopped
app must be opened again.

Automated tests cover the sender, registration validation, token replacement, settings
persistence, and account/target validation. They use mocked FCM and database calls; live
FCM delivery and migration execution require the configured services described above.

References: [Firebase Android setup](https://firebase.google.com/docs/cloud-messaging/android/get-started),
[message handling](https://firebase.google.com/docs/cloud-messaging/android/receive-messages),
and [Android message priority](https://firebase.google.com/docs/cloud-messaging/android-message-priority).
