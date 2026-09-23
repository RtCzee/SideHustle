const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { getDashboardMetrics } = require('../db/dashboard');

const router = express.Router();

/** GET /dashboard — business metrics for the signed-in user (issue #13). */
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const metrics = await getDashboardMetrics(req.user.uid);
    if (!metrics) {
      return res.status(404).json({ error: 'Profile not found. Create one with POST /me.' });
    }
    res.json(metrics);
  } catch (error) {
    next(error);
  }
});

module.exports = router;
