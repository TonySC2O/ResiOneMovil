const mongoose = require('mongoose');

const reservaSchema = new mongoose.Schema({
  zona: { type: String, required: true },           
  fecha: { type: Date, required: true },
  horaInicio: { type: String, required: true },   
  horaFin: { type: String, required: true },       
  numeroPersonas: { type: Number, required: true, min: 1 },
  comentarios: { type: String, required: true },
  estado: { type: String, enum: ["pendiente", "aprobada", "rechazada"], default: "pendiente" }

}, { timestamps: true });

module.exports = mongoose.model('Reserva', reservaSchema);
