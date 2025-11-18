const express = require("express");
const router = express.Router();
const {
    registrarEntrada,
    registrarSalida,
    obtenerBitacora
} = require("../controllers/seguridadController");

// Registra entrada
router.post("/entrada", registrarEntrada);

// Registra salida con QR
router.post("/salida", registrarSalida);

// Bitácora digital
router.get("/bitacora", obtenerBitacora);

module.exports = router;
