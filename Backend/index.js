const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db');
const usuarioRoutes = require('./routes/usuarioRoutes');
const reservaRoutes = require('./routes/reservaRoutes');
const reporteRoutes = require('./routes/reporteRoutes');
const comunicadosRoutes = require('./routes/comunicadosRoutes');


const app = express();
const PORT = 5050;

// Conectar a MongoDB
connectDB();

// Middleware
app.use(cors({
  origin: ['http://localhost:5173', 'https://43f74ba2dfa8.ngrok-free.app'],
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rutas
app.use('/api', usuarioRoutes);
app.use('/api/comunicados', comunicadosRoutes);
app.use('/api/reservas', reservaRoutes);
app.use('/api/reportes', reporteRoutes);

// Servidor
app.listen(PORT, () => console.log(`Servidor corriendo en puerto ${PORT}`));
