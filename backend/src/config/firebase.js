const admin = require('firebase-admin');
const path = require('path');

let initialized = false;

/**
 * Starts Firebase Admin once so middleware can verify ID tokens from the Android app.
 */
function initFirebase() {
  if (initialized) {
    return admin;
  }

  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentialsPath) {
    throw new Error(
      'GOOGLE_APPLICATION_CREDENTIALS is not set. Copy backend/.env.example to backend/.env'
    );
  }

  const resolved = path.isAbsolute(credentialsPath)
    ? credentialsPath
    : path.resolve(process.cwd(), credentialsPath);

  admin.initializeApp({
    credential: admin.credential.cert(require(resolved)),
  });

  initialized = true;
  return admin;
}

module.exports = { initFirebase, admin };
