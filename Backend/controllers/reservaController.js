// backend/controllers/reservaController.js
const Reserva = require('../models/Reserva');

// Validar conflicto de horario solo con reservas APROBADAS
const existeConflicto = async (zona, fecha, horaInicio, horaFin, reservaIdExcluir = null) => {
  const filtro = { zona, fecha, estado: "aprobada" };
  
  // Si estamos editando, excluir la reserva actual del check
  if (reservaIdExcluir) {
    filtro._id = { $ne: reservaIdExcluir };
  }
  
  const reservasAprobadas = await Reserva.find(filtro);
  
  return reservasAprobadas.some(r =>
    (horaInicio < r.horaFin && horaFin > r.horaInicio)
  );
};

// Crear reserva (solicitud pendiente)
const crearReserva = async (req, res) => {
  const { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios, 
          residenteCorreo, residenteNombre, residenteApartamento, residenteIdentificacion } = req.body;

  // Validaciones
  if (!zona || !fecha || !horaInicio || !horaFin || !numeroPersonas) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }
  
  if (!residenteCorreo || !residenteNombre || !residenteApartamento || !residenteIdentificacion) {
    return res.status(400).json({ mensaje: "Información del residente incompleta" });
  }
  
  if (numeroPersonas <= 0) {
    return res.status(400).json({ mensaje: "El número de personas debe ser mayor a 0" });
  }
  
  if (horaFin <= horaInicio) {
    return res.status(400).json({ mensaje: "La hora fin debe ser mayor a la hora inicio" });
  }

  // Verificar conflictos solo con reservas APROBADAS
  if (await existeConflicto(zona, fecha, horaInicio, horaFin)) {
    return res.status(400).json({ mensaje: "Ya existe una reserva aprobada en ese horario" });
  }

  try {
    const nuevaReserva = new Reserva({ 
      zona, 
      fecha, 
      horaInicio, 
      horaFin, 
      numeroPersonas, 
      comentarios: comentarios || "", 
      estado: "pendiente",
      residenteCorreo,
      residenteNombre,
      residenteApartamento,
      residenteIdentificacion
    });
    
    await nuevaReserva.save();
    
    console.log(`Solicitud de reserva creada: ${zona} - ${fecha} por ${residenteNombre}`);
    
    res.status(201).json({ mensaje: "Solicitud de reserva creada correctamente. Esperando aprobación.", reserva: nuevaReserva });
  } catch (error) {
    console.error('Error al crear reserva:', error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Obtener reservas con filtros opcionales
const obtenerReservas = async (req, res) => {
  try {
    const { estado, residenteCorreo } = req.query;
    
    const filtro = {};
    if (estado) filtro.estado = estado;
    if (residenteCorreo) filtro.residenteCorreo = residenteCorreo;
    
    const reservas = await Reserva.find(filtro).sort({ fecha: 1, horaInicio: 1 });
    
    console.log(`Obteniendo reservas. Filtros: ${JSON.stringify(filtro)}. Total: ${reservas.length}`);
    
    res.json(reservas);
  } catch (error) {
    console.error('Error al obtener reservas:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Aprobar reserva (admin)
const aprobarReserva = async (req, res) => {
  const { id } = req.params;
  const { administradorCorreo } = req.body;

  if (!administradorCorreo) {
    return res.status(400).json({ mensaje: "Se requiere el correo del administrador" });
  }

  try {
    const reserva = await Reserva.findById(id);
    
    if (!reserva) {
      return res.status(404).json({ mensaje: 'Reserva no encontrada' });
    }

    console.log('Reserva obtenida para aprobación:', JSON.stringify(reserva, null, 2));
    
    if (reserva.estado !== "pendiente") {
      return res.status(400).json({ mensaje: 'Solo se pueden aprobar reservas pendientes' });
    }

    // Validar que la reserva tenga información del residente
    if (!reserva.residenteCorreo || !reserva.residenteNombre || 
        !reserva.residenteApartamento || !reserva.residenteIdentificacion) {
      console.error('Reserva sin información completa del residente:', reserva);
      return res.status(400).json({ 
        mensaje: 'La reserva no contiene información completa del residente. Debe recrearse.' 
      });
    }

    // Verificar conflictos antes de aprobar
    if (await existeConflicto(reserva.zona, reserva.fecha, reserva.horaInicio, reserva.horaFin)) {
      return res.status(400).json({ 
        mensaje: "Conflicto detectado: ya existe una reserva aprobada en ese horario" 
      });
    }

    // Aprobar
    reserva.estado = "aprobada";
    reserva.administradorQueResponde = administradorCorreo;
    reserva.fechaRespuesta = new Date();
    
    await reserva.save();
    
    console.log(`✓ Reserva aprobada: ${reserva.zona} - ${reserva.fecha} por admin ${administradorCorreo}`);
    
    res.json({ mensaje: 'Reserva aprobada correctamente', reserva });
  } catch (error) {
    console.error('Error al aprobar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Rechazar reserva (admin) - ELIMINA el documento de la BD
const rechazarReserva = async (req, res) => {
  const { id } = req.params;
  const { razonRechazo, administradorCorreo } = req.body;

  if (!razonRechazo || !administradorCorreo) {
    return res.status(400).json({ mensaje: "Se requiere la razón del rechazo y el correo del administrador" });
  }

  try {
    const reserva = await Reserva.findById(id);
    
    if (!reserva) {
      return res.status(404).json({ mensaje: 'Reserva no encontrada' });
    }
    
    if (reserva.estado !== "pendiente") {
      return res.status(400).json({ mensaje: 'Solo se pueden rechazar reservas pendientes' });
    }

    // Guardar info antes de eliminar (para logging/notificaciones)
    const infoReserva = {
      zona: reserva.zona,
      fecha: reserva.fecha,
      residenteNombre: reserva.residenteNombre,
      residenteCorreo: reserva.residenteCorreo
    };

    // ELIMINAR el documento de la base de datos
    await Reserva.findByIdAndDelete(id);
    
    console.log(`❌ Reserva rechazada y eliminada: ${infoReserva.zona} - ${infoReserva.fecha}`);
    console.log(`   Razón: ${razonRechazo} por admin ${administradorCorreo}`);
    
    // TODO: Aquí enviar notificación por correo al residente con la razón del rechazo
    
    res.json({ 
      mensaje: 'Reserva rechazada y eliminada correctamente', 
      razonRechazo,
      infoReserva 
    });
  } catch (error) {
    console.error('Error al rechazar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Actualizar reserva (edición de reservas aprobadas)
const actualizarReserva = async (req, res) => {
  const { id } = req.params;
  const { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios } = req.body;

  try {
    const reservaActual = await Reserva.findById(id);
    
    if (!reservaActual) {
      return res.status(404).json({ mensaje: 'Reserva no encontrada' });
    }

    // Solo permitir editar reservas aprobadas
    if (reservaActual.estado !== "aprobada") {
      return res.status(400).json({ mensaje: 'Solo se pueden editar reservas aprobadas' });
    }

    // Verificar conflictos (excluyendo la reserva actual)
    if (await existeConflicto(zona, fecha, horaInicio, horaFin, id)) {
      return res.status(400).json({ mensaje: "La modificación produce un conflicto de horario" });
    }

    const reservaActualizada = await Reserva.findByIdAndUpdate(
      id,
      { zona, fecha, horaInicio, horaFin, numeroPersonas, comentarios },
      { new: true, runValidators: true }
    );

    console.log(`✏️ Reserva actualizada: ${reservaActualizada.zona} - ${reservaActualizada.fecha}`);

    res.json({ mensaje: 'Reserva actualizada correctamente', reserva: reservaActualizada });
  } catch (error) {
    console.error('Error al actualizar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Eliminar reserva (cancelación)
const eliminarReserva = async (req, res) => {
  const { id } = req.params;

  try {
    const reservaEliminada = await Reserva.findByIdAndDelete(id);
    
    if (!reservaEliminada) {
      return res.status(404).json({ mensaje: 'Reserva no encontrada' });
    }

    console.log(`🗑️ Reserva eliminada (cancelada): ${reservaEliminada.zona} - ${reservaEliminada.fecha}`);

    res.json({ mensaje: 'Reserva eliminada correctamente' });
  } catch (error) {
    console.error('Error al eliminar reserva:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// ELIMINAR TODAS LAS RESERVAS (solo para limpieza de desarrollo)
const eliminarTodasLasReservas = async (req, res) => {
  try {
    const resultado = await Reserva.deleteMany({});
    console.log(`🗑️ TODAS LAS RESERVAS ELIMINADAS: ${resultado.deletedCount} documentos`);
    res.json({ 
      mensaje: 'Todas las reservas han sido eliminadas', 
      eliminadas: resultado.deletedCount 
    });
  } catch (error) {
    console.error('Error al eliminar todas las reservas:', error);
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

module.exports = { 
  crearReserva, 
  obtenerReservas, 
  aprobarReserva, 
  rechazarReserva, 
  actualizarReserva, 
  eliminarReserva,
  eliminarTodasLasReservas
};
