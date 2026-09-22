//importing xpress to create a router
const express = require('express');
const healthRouter = require('./health');
const meRouter = require('./me');
const dashboardRouter = require('./dashboard');
const expensesRouter = require('./expenses');
const invoicesRouter = require('./invoices');

const router = express.Router();

router.use('/health', healthRouter);
router.use('/me', meRouter);
router.use('/dashboard', dashboardRouter);
router.use('/expenses', expensesRouter);
router.use('/invoices', invoicesRouter);

module.exports = router;
