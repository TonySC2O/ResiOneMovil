const express = require("express");
const router = express.Router();
const { crearReporte, obtenerReportes, cambiarEstado } = require("../controllers/reporteController");

router.post("/", crearReporte);
router.get("/", obtenerReportes);
router.put("/:id", cambiarEstado);

module.exports = router;
