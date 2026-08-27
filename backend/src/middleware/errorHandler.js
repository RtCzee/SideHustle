/**
 * Sends JSON errors to clients without leaking stack traces.
 */
function errorHandler(err, req, res, next) {
  if (res.headersSent) {
    return next(err);
  }

  console.error(err);

  const status = err.status || err.statusCode || 500;
  const message =
    status >= 500 ? 'Internal server error' : err.message || 'Request failed';

  res.status(status).json({ error: message });
}

function notFoundHandler(req, res) {
  res.status(404).json({ error: 'Route not found' });
}

module.exports = { errorHandler, notFoundHandler };
