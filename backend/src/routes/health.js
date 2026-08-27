//const express creating a router
const express = require('express');

const router = express.Router();

//getting the request and sending the response to the client
router.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'sidehustle-api' });
});

module.exports = router;
