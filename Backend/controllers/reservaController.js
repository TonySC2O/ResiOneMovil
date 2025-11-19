// backend/controllers/reservaController.js
const Reserva = require('../models/Reserva');

// Validaciones auxiliares
function validarNumeroPersonas(num) {
  const n = Number(num);
  return Number.isInteger(n) && n > 0;
}

// Validar conflicto de horario
const existeConflicto = async (zona, fecha, horaInicio, horaFin) => {
  const reservas = await Reserva.find({ zona, fecha });
  return reservas.some(r =>
    (horaInicio < r.horaFin && horaFin > r.horaInicio)
  );
};

// Crear reserva
const crearReserva = async (req, res) => {
  const { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios } = req.body;

  if (!zona || !fecha || !horaInicio || !horaFin || numeroPersonas <= 0) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }
  if (horaFin <= horaInicio) {
    return res.status(400).json({ mensaje: "La hora fin debe ser mayor a la hora inicio" });
  }
  if (await existeConflicto(zona, fecha, horaInicio, horaFin)) {
    return res.status(400).json({ mensaje: "Ya existe una reserva en ese horario" });
  }

  try {
    const nuevaReserva = new Reserva({ zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios: comentarios || "Sin comentarios", estado: "pendiente" });
    await nuevaReserva.save();
    res.status(201).json({ mensaje: "Reserva creada correctamente", reserva: nuevaReserva });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Obtener todas las reservas
const obtenerReservas = async (req, res) => {
  try {
    const reservas = await Reserva.find();
    res.json(reservas);
  } catch (error) {
    console.error('Error al obtener reservas:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Actualizar reserva
const actualizarReserva = async (req, res) => {
  const { id } = req.params;
  const { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios } = req.body;

  try {
    const reservaActualizada = await Reserva.findByIdAndUpdate(
      id,
      { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios },
      { new: true, runValidators: true }
    );

    if (!reservaActualizada) return res.status(404).json({ mensaje: 'Reserva no encontrada' });

    res.json({ mensaje: 'Reserva actualizada correctamente', reserva: reservaActualizada });
  } catch (error) {
    console.error('Error al actualizar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Eliminar reserva
const eliminarReserva = async (req, res) => {
  const { id } = req.params;

  try {
    const reservaEliminada = await Reserva.findByIdAndDelete(id);
    if (!reservaEliminada) return res.status(404).json({ mensaje: 'Reserva no encontrada' });

    res.json({ mensaje: 'Reserva eliminada correctamente' });
  } catch (error) {
    console.error('Error al eliminar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

module.exports = { crearReserva, obtenerReservas, actualizarReserva, eliminarReserva };
