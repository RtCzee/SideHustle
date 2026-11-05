const { getPool } = require('./pool');

async function listClients(userId) {
  const { rows } = await getPool().query(
    'SELECT * FROM clients WHERE user_id = $1 ORDER BY name, client_id', [userId]
  );
  return rows;
}

async function findClientById(userId, id) {
  const { rows } = await getPool().query(
    'SELECT * FROM clients WHERE user_id = $1 AND client_id = $2', [userId, id]
  );
  return rows[0] ?? null;
}

async function createClient(userId, fields, recordId) {
  const { rows } = await getPool().query(
    `INSERT INTO clients (client_id, user_id, name, email, phone_number, address, notes)
     VALUES (COALESCE($1::uuid, gen_random_uuid()), $2, $3, $4, $5, $6, $7) RETURNING *`,
    [recordId, userId, fields.name, fields.email, fields.phoneNumber, fields.address, fields.notes]
  );
  return rows[0];
}

async function updateClient(userId, id, fields) {
  const columns = { name: fields.name, email: fields.email, phone_number: fields.phoneNumber,
    address: fields.address, notes: fields.notes };
  const values = [userId, id];
  const updates = Object.entries(columns).filter(([, value]) => value !== undefined)
    .map(([column, value]) => column + ' = $' + values.push(value));
  if (!updates.length) return findClientById(userId, id);
  const { rows } = await getPool().query(
    'UPDATE clients SET ' + updates.join(', ') + ', updated_at = NOW() WHERE user_id = $1 AND client_id = $2 RETURNING *',
    values
  );
  return rows[0] ?? null;
}

async function deleteClient(userId, id) {
  const { rowCount } = await getPool().query(
    'DELETE FROM clients WHERE user_id = $1 AND client_id = $2', [userId, id]
  );
  return rowCount > 0;
}

module.exports = { listClients, findClientById, createClient, updateClient, deleteClient };
