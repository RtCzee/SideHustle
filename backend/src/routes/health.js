const express = require('express');
const { checkDatabaseConnection } = require('../db/pool');

const router = express.Router();

router.get('/', async (req, res, next) => {
  const payload = { status: 'ok', service: 'sidehustle-api' };

  if (process.env.DATABASE_URL) {
    try {
      await checkDatabaseConnection();
      payload.database = 'connected';
    } catch (err) {
      payload.database = 'error';
      payload.databaseError = 'Could not reach PostgreSQL';
    }
  } else {
    payload.database = 'not_configured';
  }

  res.json(payload);
});

module.exports = router;
