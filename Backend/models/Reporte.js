const mongoose = require('mongoose');

const reporteSchema = new mongoose.Schema({
  tipo: { type: String, required: true },
  descripcion: { type: String, required: true },
  nivelPrioridad: { type: String, required: true },
  archivos: [String],
  fecha: { type: String, required: true },
  estado: { type: String, default: "Pendiente" },
  seguimiento: { type: String, required: true },
  comentariosAdmin: { type: String, default: "" }
});

module.exports = mongoose.model("Reporte", reporteSchema);
