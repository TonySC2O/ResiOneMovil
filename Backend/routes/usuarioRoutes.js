const express = require('express');
const router = express.Router();
const { registrarUsuario, loginUsuario, editarUsuario } = require('../controllers/usuarioController');

router.post('/registro', registrarUsuario);
router.post('/login', loginUsuario);
router.put('/editar', editarUsuario);

module.exports = router;
