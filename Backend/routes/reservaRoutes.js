const express = require('express');
const router = express.Router();
const { 
  crearReserva, 
  obtenerReservas, 
  aprobarReserva, 
  rechazarReserva, 
  actualizarReserva, 
  eliminarReserva,
  eliminarTodasLasReservas
} = require('../controllers/reservaController');

// Obtener reservas (con filtros opcionales: ?estado=pendiente o ?residenteCorreo=email)
router.get('/', obtenerReservas);

// Crear nueva solicitud de reserva (estado: pendiente por defecto)
router.post('/', crearReserva);

// Aprobar reserva (admin) - cambiar estado a aprobada
router.put('/:id/aprobar', aprobarReserva);

// Rechazar reserva (admin) - ELIMINAR documento de BD
router.delete('/:id/rechazar', rechazarReserva);

// Actualizar reserva aprobada (edición)
router.put('/:id', actualizarReserva);

// Eliminar reserva (cancelación)
router.delete('/:id', eliminarReserva);

// ELIMINAR TODAS LAS RESERVAS (solo para desarrollo/limpieza)
router.delete('/admin/eliminar-todas', eliminarTodasLasReservas);

module.exports = router;
