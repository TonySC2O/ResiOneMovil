const express = require("express");
const router = express.Router();
const multer = require("multer");
const upload = multer({ dest: "uploads/" });

const { 
    crearCuota, obtenerCuotas,
    registrarPago, historialPagos,
    emitirFactura
} = require("../controllers/finanzaController");

// Cuotas
router.post("/cuotas", crearCuota);
router.get("/cuotas", obtenerCuotas);

// Pagos
router.post("/pagos", upload.single("comprobantePDF"), registrarPago);
router.get("/pagos/historial", historialPagos);

// Facturas
router.post("/facturas", emitirFactura);

module.exports = router;
