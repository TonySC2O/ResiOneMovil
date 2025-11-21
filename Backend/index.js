const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db');
const usuarioRoutes = require('./routes/usuarioRoutes');
const reservaRoutes = require('./routes/reservaRoutes');
const reporteRoutes = require('./routes/reporteRoutes');
const comunicadosRoutes = require('./routes/comunicadosRoutes');
const finanzaRoutes = require("./routes/finanzaRoutes");
const seguridadRoutes = require("./routes/seguridadRoutes");
const bitacoraRoutes = require("./routes/mantenimientoRoutes");


const app = express();
const PORT = 5050;

// Conecta a MongoDB
connectDB();

// Middleware
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Servir archivos estáticos (imágenes, videos, documentos)
app.use('/uploads', express.static('uploads'));

// Rutas
app.use('/api', usuarioRoutes);
app.use('/api/comunicados', comunicadosRoutes);
app.use('/api/reservas', reservaRoutes);
app.use('/api/reportes', reporteRoutes);
app.use("/api/finanzas", finanzaRoutes);
app.use("/api/visitas", seguridadRoutes);
app.use("/api/bitacoras", bitacoraRoutes);

// Servidor
app.listen(PORT, "0.0.0.0", () => console.log(`Servidor corriendo en puerto ${PORT}`));
