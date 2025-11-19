const mongoose = require('mongoose');

const reservaSchema = new mongoose.Schema({
  zona: { type: String, required: true },           // Nombre del espacio (ej: "Sala de juntas")
  fecha: { type: Date, required: true },            // Fecha de la reserva
  horaInicio: { type: String, required: true },     // Hora de inicio (formato "HH:mm")
  horaFin: { type: String, required: true },        // Hora de fin (formato "HH:mm")
  numeroPersonas: { type: Number, required: true, min: 1 },  // Cantidad de personas
  comentarios: { type: String, required: true },    // Comentario generado automáticamente
  estado: { type: String, enum: ["pendiente", "aprobada", "rechazada"], default: "pendiente" },  // Estado de la solicitud
  creador: { type: String, required: true },        // ID del usuario que crea la reserva
  residente: { type: String, required: true },      // Nombre del residente
  correoResidente: { type: String, required: true }, // Email del residente para notificaciones
  razonRechazo: { type: String, default: "" }       // Razón del rechazo (solo si estado es rechazada)

}, { timestamps: true });

module.exports = mongoose.model('Reserva', reservaSchema);
