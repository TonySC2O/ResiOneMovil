const mongoose = require('mongoose');

const comunicadoSchema = new mongoose.Schema({
  titulo: { type: String, required: true, trim: true },
  contenido: { type: String, required: true },
  fechaPublicacion: { type: Date, default: Date.now },
  estado: { type: String, enum: ['activo', 'archivado'], default: 'activo' },
  creadoPorAdministrador: { type: Boolean, default: true,required: true },
  ultimaActualizacion: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Comunicado', comunicadoSchema);