require('dotenv').config();
const { initFirebase } = require('./config/firebase');
const { getPool } = require('./db/pool');
const { sendJobReminders } = require('./services/JobReminderSender');

async function run() {
  initFirebase();
  try { console.log(await sendJobReminders()); }
  finally { await getPool().end(); }
}
run().catch(() => { console.error('Reminder run failed. Check database and Firebase configuration.'); process.exitCode = 1; });
