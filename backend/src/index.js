// importing the dotenv package to load environment variables like the port number and the firebase API key
require('dotenv').config();

const { createApp } = require('./app');
const { initFirebase } = require('./config/firebase');

// port number for the server 
const port = Number(process.env.PORT) || 3000;

try {
  initFirebase();
} catch (err) {
  console.error('Firebase Admin failed to start:', err.message);
  process.exit(1);
}
 //creating an app listening port for the server to run
const app = createApp();

//logging message to the console, and liostening 
app.listen(port, () => {
  console.log(`SideHustle API listening on http://localhost:${port}`);
  console.log(`Health check: http://localhost:${port}/health`);
});
