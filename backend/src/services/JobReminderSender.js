const { admin } = require('../config/firebase');
const { createReminderStore } = require('../db/jobReminders');

function reminderMessage(reminder) {
  return {
    token: reminder.fcm_token,
    // Data messages let the app check its local opt-out before displaying, including in background.
    data: {
      type: 'job_due', user_id: reminder.user_id, job_id: reminder.job_id,
      title: 'Job due soon', body: `${reminder.title} is due on ${reminder.due_date}.`,
    },
    android: { priority: 'high', ttl: 60 * 60 * 1000 },
  };
}

async function sendJobReminders({ store = createReminderStore(), send = message => admin.messaging().send(message),
  log = console } = {}) {
  const summary = { sent: 0, failed: 0, skipped: 0 };
  for (const candidate of await store.candidates()) {
    if (typeof candidate.fcm_token !== 'string' || !candidate.fcm_token.trim()) { summary.skipped++; continue; }
    const reminder = await store.claim(candidate);
    if (!reminder) { summary.skipped++; continue; }
    try {
      await send(reminderMessage(reminder));
      await store.sent(reminder);
      summary.sent++;
    } catch (error) {
      summary.failed++;
      if (['messaging/registration-token-not-registered', 'messaging/invalid-registration-token'].includes(error.code)) {
        await store.removeInvalidDevice(reminder);
      } else {
        await store.release(reminder);
      }
      log.warn('Job reminder failed', error.code || 'unknown');
    }
  }
  return summary;
}

function startReminderWorker({ run = sendJobReminders, log = console, intervalMs = 15 * 60 * 1000 } = {}) {
  let running = false;
  const tick = async () => {
    if (running) return;
    running = true;
    try { await run(); }
    catch (_) { log.error('Job reminder check failed. Check database migration and Firebase configuration.'); }
    finally { running = false; }
  };
  void tick();
  const timer = setInterval(tick, intervalMs);
  timer.unref();
  return () => clearInterval(timer);
}
module.exports = { reminderMessage, sendJobReminders, startReminderWorker };
