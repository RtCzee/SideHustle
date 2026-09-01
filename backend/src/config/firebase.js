const admin = require('firebase-admin');
const path = require('path');

let initialized = false;

function loadCredential() {
  const json = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (json) {
    const serviceAccount = JSON.parse(json);
    if (typeof serviceAccount.private_key === 'string') {
      serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, '\n');
    }
    return admin.credential.cert(serviceAccount);
  }

  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentialsPath) {
    throw new Error(
      'Firebase credentials missing. Set FIREBASE_SERVICE_ACCOUNT_JSON (Railway) or GOOGLE_APPLICATION_CREDENTIALS (local) — see backend/.env.example'
    );
  }

  const resolved = path.isAbsolute(credentialsPath)
    ? credentialsPath
    : path.resolve(process.cwd(), credentialsPath);

  return admin.credential.cert(require(resolved));
}

/**
 * Starts Firebase Admin once so middleware can verify ID tokens from the Android app.
 */
function initFirebase() {
  if (initialized) {
    return admin;
  }

  admin.initializeApp({
    credential: loadCredential(),
  });

  initialized = true;
  return admin;
}

module.exports = { initFirebase, admin };
