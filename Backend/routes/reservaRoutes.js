const express = require('express');
const router = express.Router();
const { crearReserva, obtenerReservas, actualizarReserva, eliminarReserva } = require('../controllers/reservaController');

router.get('/', obtenerReservas);
router.post('/', crearReserva);
router.put('/:id', actualizarReserva);
router.delete('/:id', eliminarReserva);

module.exports = router;
