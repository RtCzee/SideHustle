//importing xpress to create a router
const express = require('express');
const healthRouter = require('./health');
const meRouter = require('./me');

const router = express.Router();

router.use('/health', healthRouter);
router.use('/me', meRouter);

module.exports = router;
