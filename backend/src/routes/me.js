const express = require('express');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

/** Protected test route — proves Firebase token verification works (issue #7). */
router.get('/', requireAuth, (req, res) => {
  res.json({
    uid: req.user.uid,
    email: req.user.email ?? null,
  });
});

module.exports = router;
