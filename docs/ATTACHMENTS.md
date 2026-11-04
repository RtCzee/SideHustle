# Job and expense attachments

Saved jobs have an Attachments button. Saving a new job opens its saved details;
select Attachments, then Attach a file or image. Saving an expense opens its details
and attachment dialog. Tap any existing expense row to return to that dialog.
Uploads use the system document picker, Firebase Storage, and then the authenticated
API. Files are limited to 20 MB. Open buttons launch the Firebase download URL in
the device's browser/viewer. Picker cancellation and upload/save failures show messages.
Retry after an API failure reuses the uploaded object and the endpoint is idempotent.

## Deploy

1. Enable Cloud Storage in Firebase project `sidehustle-d709c` and ensure the bucket
   matches `app/google-services.json`. Publish the repository's `storage.rules` in
   Firebase Console → Storage → Rules. These allow the owner to upload/read/delete
   their files and limit uploads to 20 MB.
2. Set API environment variable
   `FIREBASE_STORAGE_BUCKET=sidehustle-d709c.firebasestorage.app`. The existing Firebase
   Admin service account needs permission to read object metadata in that bucket.
3. For an existing database, run `cd backend` then `npm run db:attachments` with
   `DATABASE_URL` configured. This migration is additive and repeatable. New installs
   receive the attachment table from `npm run db:schema`.
4. Deploy the API and build/install the updated Android app.

The API exposes GET/POST `/attachments/{jobs|expenses}/{entityId}`. POST accepts
`name`, `url`, and `storage_path`; it verifies entity ownership, the configured Firebase
bucket, user/entity path, object existence, size, and download token. The database
stores the URL and a foreign key to the job or expense. Download URLs contain bearer
tokens; treat them as private links.

## Verify on a connected device

- Save a job, attach an image or PDF, reopen its details, and open the attachment.
- Save an expense, attach a receipt, then tap its row after refreshing and open it.
- Confirm the object exists under `attachments/{uid}/{type}/{id}/{uuid}` in Firebase
  Storage and its URL and entity ID appear in the database's `attachments` table.
- Cancel the picker; verify the message and that another selection works.
- Disable networking during upload; verify a failure message and select the file again.
- Simulate an API failure after upload; restore it and tap Retry. Verify one database
  record and no second upload. Attempt another account's entity ID and expect 404.

Uploads run while the attachment view is alive. Leaving or recreating that view can
cancel an upload; select the file again. An interrupted upload/save can leave an
unreferenced Storage object, which may be removed administratively. Deleting a parent
record cascades its database metadata but does not automatically remove Storage files.

Implementation follows Firebase's [Android upload guide](https://firebase.google.com/docs/storage/android/upload-files).
