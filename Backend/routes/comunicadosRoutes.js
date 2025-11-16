const express = require('express');
const router = express.Router();
const comunicadoController = require('../controllers/comunicadoController');

router.get('/feed', comunicadoController.obtenerComunicados);
router.post('/crear', comunicadoController.crearComunicado);
router.put('/editar/:id', comunicadoController.editarComunicado);
router.delete('/eliminar/:id', comunicadoController.eliminarComunicado);

module.exports = router;
