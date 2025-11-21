const mongoose = require('mongoose');

const usuarioSchema = new mongoose.Schema({
  nombre: { type: String, required: true, trim: true },
  correo: { type: String, required: true, unique: true, match: /^[^\s@]+@[^\s@]+\.[^\s@]+$/ },
  telefono: { type: String, required: true, match: /^\d{8}$/ },
  identificacion: { type: String, required: true, match: /^\d{9}$/, unique: true },
  contraseña: { type: String, required: true, match: /^[A-Za-z]{6}\d{4}\.$/ },
  apartamento: { type: String, required: true },
  habitantes: { type: Number, required: true, min: 1 },
  esAdministrador: { type: Boolean, default: false },
  codigoEmpleado: { type: String, required: function () { return this.esAdministrador; }, match: /^[A-Za-z]{4}\d{2}$/ },
  rol: { type: String, required: true, enum: ['RESIDENTE', 'ADMIN', 'TECNICO_MANTENIMIENTO', 'AUXILIAR_SEGURIDAD'], default: 'RESIDENTE' }
});

module.exports = mongoose.model('Usuario', usuarioSchema);
