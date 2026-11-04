//importing xpress to create a router
const express = require('express');
const healthRouter = require('./health');
const meRouter = require('./me');
const dashboardRouter = require('./dashboard');
const clientsRouter = require('./clients');
const jobsRouter = require('./jobs');

const router = express.Router();

router.use('/health', healthRouter);
router.use('/me', meRouter);
router.use('/me/push-device', require('./pushDevices'));
router.use('/dashboard', dashboardRouter);
router.use('/clients', clientsRouter);
router.use('/jobs', jobsRouter);
router.use('/expenses', require('./expenses'));
router.use('/attachments', require('./attachments'));

module.exports = router;
