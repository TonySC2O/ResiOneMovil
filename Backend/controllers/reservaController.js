// backend/controllers/reservaController.js
const Reserva = require('../models/Reserva');

// Validaciones auxiliares
function validarNumeroPersonas(num) {
  const n = Number(num);
  return Number.isInteger(n) && n > 0;
}

// Validar conflicto de horario (solo con reservas aprobadas)
const existeConflicto = async (zona, fecha, horaInicio, horaFin) => {
  const reservas = await Reserva.find({ zona, fecha, estado: 'aprobada' });
  return reservas.some(r =>
    (horaInicio < r.horaFin && horaFin > r.horaInicio)
  );
};

// Crear reserva (solicitud)
const crearReserva = async (req, res) => {
  const { zona, fecha, horaInicio, horaFin, numeroPersonas, creador, residente, correoResidente } = req.body;

  if (!zona || !fecha || !horaInicio || !horaFin || !numeroPersonas || !creador || !residente || !correoResidente) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }
  if (numeroPersonas <= 0) {
    return res.status(400).json({ mensaje: "El número de personas debe ser mayor a 0" });
  }
  if (horaFin <= horaInicio) {
    return res.status(400).json({ mensaje: "La hora fin debe ser mayor a la hora inicio" });
  }
  if (await existeConflicto(zona, fecha, horaInicio, horaFin)) {
    return res.status(400).json({ mensaje: "Ya existe una reserva aprobada en ese horario" });
  }

  try {
    // Generar comentario automático
    const fechaFormateada = new Date(fecha).toLocaleDateString('es-ES', { 
      day: '2-digit', 
      month: '2-digit', 
      year: 'numeric' 
    });
    const comentarios = `${residente} ha solicitado reservar un espacio en ${zona} el ${fechaFormateada} de ${horaInicio} a ${horaFin}`;
    
    const nuevaReserva = new Reserva({ 
      zona, 
      fecha, 
      horaInicio, 
      horaFin, 
      numeroPersonas, 
      comentarios,
      creador,
      residente,
      correoResidente,
      estado: "pendiente" 
    });
    
    await nuevaReserva.save();
    
    // Simular envío de email al administrador
    console.log(`✉️ Email enviado al administrador: Nueva solicitud de reserva de ${residente} para ${zona}`);
    
    res.status(201).json({ mensaje: "Solicitud de reserva creada correctamente", reserva: nuevaReserva });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Obtener todas las reservas (con filtro opcional por estado)
const obtenerReservas = async (req, res) => {
  try {
    const { estado } = req.query;  // Filtro opcional: ?estado=pendiente
    
    const filtro = estado ? { estado } : {};
    const reservas = await Reserva.find(filtro).sort({ fecha: 1, horaInicio: 1 });
    
    res.json(reservas);
  } catch (error) {
    console.error('Error al obtener reservas:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Actualizar reserva (aprobar/rechazar o editar)
const actualizarReserva = async (req, res) => {
  const { id } = req.params;
  const { estado, razonRechazo, zona, fecha, horaInicio, horaFin, numeroPersonas, ...otrosCampos } = req.body;

  try {
    const reservaActual = await Reserva.findById(id);
    if (!reservaActual) return res.status(404).json({ mensaje: 'Reserva no encontrada' });

    // Preparar datos de actualización
    const datosActualizacion = { ...otrosCampos };
    
    // ===== CASO 1: Edición de datos de la reserva =====
    if (zona || fecha || horaInicio || horaFin || numeroPersonas) {
      // Usar valores nuevos o mantener los actuales
      const nuevaZona = zona || reservaActual.zona;
      const nuevaFecha = fecha || reservaActual.fecha;
      const nuevaHoraInicio = horaInicio || reservaActual.horaInicio;
      const nuevaHoraFin = horaFin || reservaActual.horaFin;
      const nuevoNumeroPersonas = numeroPersonas || reservaActual.numeroPersonas;
      
      // Validaciones
      if (nuevoNumeroPersonas <= 0) {
        return res.status(400).json({ mensaje: "El número de personas debe ser mayor a 0" });
      }
      if (nuevaHoraFin <= nuevaHoraInicio) {
        return res.status(400).json({ mensaje: "La hora fin debe ser mayor a la hora inicio" });
      }
      
      // Validar conflictos EXCLUYENDO la reserva actual
      const reservasConflictivas = await Reserva.find({ 
        zona: nuevaZona, 
        fecha: nuevaFecha, 
        estado: 'aprobada',
        _id: { $ne: id }  // Excluir la reserva actual
      });
      
      const hayConflicto = reservasConflictivas.some(r =>
        (nuevaHoraInicio < r.horaFin && nuevaHoraFin > r.horaInicio)
      );
      
      if (hayConflicto) {
        return res.status(400).json({ mensaje: "Ya existe una reserva aprobada en ese horario" });
      }
      
      // Actualizar campos editados
      if (zona) datosActualizacion.zona = zona;
      if (fecha) datosActualizacion.fecha = fecha;
      if (horaInicio) datosActualizacion.horaInicio = horaInicio;
      if (horaFin) datosActualizacion.horaFin = horaFin;
      if (numeroPersonas) datosActualizacion.numeroPersonas = numeroPersonas;
      
      // Regenerar comentario si cambiaron datos relevantes
      if (zona || fecha || horaInicio || horaFin) {
        const fechaFormateada = new Date(nuevaFecha).toLocaleDateString('es-ES', { 
          day: '2-digit', 
          month: '2-digit', 
          year: 'numeric' 
        });
        datosActualizacion.comentarios = `${reservaActual.residente} ha solicitado reservar un espacio en ${nuevaZona} el ${fechaFormateada} de ${nuevaHoraInicio} a ${nuevaHoraFin}`;
      }
    }
    
    // ===== CASO 2: Cambio de estado (aprobar/rechazar) =====
    if (estado) {
      // Si se rechaza, ELIMINAR la reserva en lugar de actualizarla
      if (estado === 'rechazada') {
        await Reserva.findByIdAndDelete(id);
        console.log(`✉️ Email enviado a ${reservaActual.correoResidente}: Su reserva para ${reservaActual.zona} ha sido RECHAZADA. Razón: ${razonRechazo}`);
        return res.json({ mensaje: 'Reserva rechazada y eliminada correctamente' });
      }
      
      // Si se aprueba, actualizar estado
      if (estado === 'aprobada') {
        datosActualizacion.estado = estado;
        console.log(`✉️ Email enviado a ${reservaActual.correoResidente}: Su reserva para ${reservaActual.zona} ha sido APROBADA`);
      }
    }

    const reservaActualizada = await Reserva.findByIdAndUpdate(
      id,
      datosActualizacion,
      { new: true, runValidators: true }
    );

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
