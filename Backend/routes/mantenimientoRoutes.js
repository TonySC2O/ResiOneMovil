const express = require("express");
const router = express.Router();

const {
    crearBitacora,
    obtenerBitacoras
} = require("../controllers/mantenimientoController");

// Registra (solo técnico)
router.post("/", crearBitacora);

// Obtiene historial (admin)
router.get("/", obtenerBitacoras);

module.exports = router;
