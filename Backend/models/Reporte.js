const mongoose = require('mongoose');

const reporteSchema = new mongoose.Schema({
  tipo: { type: String, required: true },
  descripcion: { type: String, required: true },
  nivelPrioridad: { type: String, required: true },
  archivos: [String],
  fecha: { type: String, required: true },
  estado: { type: String, default: "Pendiente" },
  seguimiento: { type: String, required: true },
  comentariosAdmin: { type: String, default: "" },
  tecnicoAsignado: { type: String, default: "Sin asignar" },
  // Información del residente que creó el reporte
  residenteCorreo: { type: String, required: true },
  residenteNombre: { type: String, required: true },
  residenteApartamento: { type: String },
  residenteIdentificacion: { type: String }
});

module.exports = mongoose.model("Reporte", reporteSchema);
