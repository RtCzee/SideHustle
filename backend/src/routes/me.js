const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { findUserById, createUser, updateUser } = require('../db/users');

const router = express.Router();

function normalizeFullName(value) {
  if (typeof value !== 'string') {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function profilePayload(row) {
  return {
    user_id: row.user_id,
    email: row.email,
    full_name: row.full_name,
    phone_number: row.phone_number,
    profile_picture_url: row.profile_picture_url,
    preferred_currency: row.preferred_currency,
    preferred_language: row.preferred_language,
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

/** GET /me — profile for the signed-in Firebase user (issue #11). */
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const profile = await findUserById(req.user.uid);
    if (!profile) {
      return res.status(404).json({ error: 'Profile not found. Create one with POST /me.' });
    }
    res.json(profilePayload(profile));
  } catch (error) {
    next(error);
  }
});

/** POST /me — create profile after register (issue #11). */
router.post('/', requireAuth, async (req, res, next) => {
  try {
    const existing = await findUserById(req.user.uid);
    if (existing) {
      return res.status(409).json({ error: 'Profile already exists. Use PUT /me to update.' });
    }

    const fullName = normalizeFullName(req.body.full_name ?? req.body.fullName);
    if (!fullName) {
      return res.status(400).json({ error: 'full_name is required and cannot be empty.' });
    }

    const email = req.user.email;
    if (!email) {
      return res.status(400).json({ error: 'A verified email is required to create a profile.' });
    }

    const profile = await createUser({
      userId: req.user.uid,
      email,
      fullName,
      phoneNumber: req.body.phone_number ?? req.body.phoneNumber ?? null,
      preferredCurrency: req.body.preferred_currency ?? req.body.preferredCurrency,
      preferredLanguage: req.body.preferred_language ?? req.body.preferredLanguage,
    });

    res.status(201).json(profilePayload(profile));
  } catch (error) {
    next(error);
  }
});

/** PUT /me — update profile for the signed-in user (issue #11). */
router.put('/', requireAuth, async (req, res, next) => {
  try {
    const existing = await findUserById(req.user.uid);
    if (!existing) {
      return res.status(404).json({ error: 'Profile not found. Create one with POST /me.' });
    }

    const hasFullName =
      req.body.full_name !== undefined || req.body.fullName !== undefined;
    if (hasFullName) {
      const fullName = normalizeFullName(req.body.full_name ?? req.body.fullName);
      if (!fullName) {
        return res.status(400).json({ error: 'full_name cannot be empty.' });
      }
    }

    const profile = await updateUser(req.user.uid, {
      fullName: hasFullName
        ? normalizeFullName(req.body.full_name ?? req.body.fullName)
        : undefined,
      phoneNumber: req.body.phone_number ?? req.body.phoneNumber,
      profilePictureUrl: req.body.profile_picture_url ?? req.body.profilePictureUrl,
      preferredCurrency: req.body.preferred_currency ?? req.body.preferredCurrency,
      preferredLanguage: req.body.preferred_language ?? req.body.preferredLanguage,
    });

    res.json(profilePayload(profile));
  } catch (error) {
    next(error);
  }
});

module.exports = router;
