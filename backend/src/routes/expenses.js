const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { EXPENSE_CATEGORIES, createExpense, listExpenses } = require('../db/expenses');
const { findUserById } = require('../db/users');

const router = express.Router();

function isIsoDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

/** GET /expenses — expense records for the signed-in user. */
router.get('/', requireAuth, async (req, res, next) => {
  try {
    res.json(await listExpenses(req.user.uid));
  } catch (error) {
    next(error);
  }
});

/** POST /expenses — records a business expense for the signed-in user. */
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const { amount, expense_date: expenseDate, category, description } = req.body || {};
    const parsedAmount = typeof amount === 'number' ? amount : Number(amount);

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > 9999999999.99) {
      return res.status(400).json({ error: 'Amount must be greater than 0.' });
    }
    if (!isIsoDate(expenseDate)) {
      return res.status(400).json({ error: 'A valid expense date is required.' });
    }
    if (!EXPENSE_CATEGORIES.includes(category)) {
      return res.status(400).json({ error: 'Select a valid expense category.' });
    }
    if (description != null && (typeof description !== 'string' || description.length > 255)) {
      return res.status(400).json({ error: 'Description must be 255 characters or fewer.' });
    }
    if (!await findUserById(req.user.uid)) {
      return res.status(404).json({ error: 'Profile not found. Please complete registration first.' });
    }

    const expense = await createExpense(req.user.uid, {
      amount: parsedAmount,
      expenseDate,
      category,
      description: description?.trim(),
    });
    res.status(201).json(expense);
  } catch (error) {
    next(error);
  }
});

module.exports = router;
