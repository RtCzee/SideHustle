// so what this does is basically just to check the database connection and return the status of the database. connectiion 
const express = require('express');
const { checkDatabaseConnection } = require('../db/pool');

const router = express.Router();

//the route used to check the connection 
router.get('/', async (req, res, next) => {
  const payload = { status: 'ok', service: 'sidehustle-api' };
// check if the database isn connected and set the status to connected if it is connected 
  if (process.env.DATABASE_URL) {
    try {
      await checkDatabaseConnection();
      payload.database = 'connected';
    } catch (err) {
      payload.database = 'error';
      payload.databaseError = 'Could not reach PostgreSQL';
    } // or else if not we just give  a status that the databasase isnt configured
  } else {
    payload.database = 'not_configured';
  }

  res.json(payload); // return the payload to the client
});

module.exports = router;
