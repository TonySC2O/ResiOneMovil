const mongoose = require('mongoose');

const reservaSchema = new mongoose.Schema({
  zona: { type: String, required: true },           
  fecha: { type: Date, required: true },
  horaInicio: { type: String, required: true },   
  horaFin: { type: String, required: true },       
  numeroPersonas: { type: Number, required: true, min: 1 },
  comentarios: { type: String, default: "" },
  estado: { type: String, enum: ["pendiente", "aprobada"], default: "pendiente" },
  
  // Información del residente que solicita
  residenteCorreo: { type: String, required: true },
  residenteNombre: { type: String, required: true },
  residenteApartamento: { type: String, required: true },
  residenteIdentificacion: { type: String, required: true },
  
  // Información del administrador que aprueba (solo si estado = aprobada)
  administradorQueResponde: { type: String, default: null },
  fechaRespuesta: { type: Date, default: null }

}, { timestamps: true });

module.exports = mongoose.model('Reserva', reservaSchema);
