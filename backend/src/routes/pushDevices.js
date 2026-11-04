const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { registerDevice } = require('../db/pushDevices');
const { findUserById } = require('../db/users');

function createPushDevicesRouter({ auth = requireAuth, save = registerDevice, findUser = findUserById } = {}) {
  const router = express.Router();
  router.put('/', auth, async (req, res, next) => {
    const { device_id: deviceId, token, enabled } = req.body || {};
    if (typeof deviceId !== 'string' || !/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(deviceId) ||
        typeof token !== 'string' || !token.trim() || token.length > 4096 || typeof enabled !== 'boolean') {
      return res.status(400).json({ error: 'A valid device_id, FCM token and enabled flag are required.' });
    }
    if (req.body.user_id !== req.user.uid) return res.status(403).json({ error: 'Device owner does not match the session.' });
    try {
      if (!await findUser(req.user.uid)) return res.status(404).json({ error: 'Create your profile first.' });
      await save(req.user.uid, { deviceId, token, enabled });
      res.json({ registered: true });
    } catch (error) {
      // Database errors can include the token in their detail; do not forward that to the shared logger.
      console.error('Push device registration failed', error.code || 'unknown');
      next(new Error('Push device registration failed.'));
    }
  });
  return router;
}
module.exports = createPushDevicesRouter();
module.exports.createPushDevicesRouter = createPushDevicesRouter;
