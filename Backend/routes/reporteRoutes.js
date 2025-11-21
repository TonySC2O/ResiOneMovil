const express = require("express");
const router = express.Router();
const multer = require("multer");
const path = require("path");
const { crearReporte, obtenerReportes, cambiarEstado, eliminarTodosLosReportes } = require("../controllers/reporteController");

// Configurar multer para almacenar archivos de reportes
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, "uploads/reportes/");
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, "reporte-" + uniqueSuffix + path.extname(file.originalname));
  },
});

const upload = multer({ 
  storage: storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB límite por archivo
  fileFilter: function (req, file, cb) {
    // Aceptar solo imágenes y videos
    if (file.mimetype.startsWith("image/") || file.mimetype.startsWith("video/")) {
      cb(null, true);
    } else {
      cb(new Error("Solo se permiten archivos de imagen o video"), false);
    }
  },
});

router.post("/", upload.array("archivos", 5), crearReporte); // Máximo 5 archivos
router.get("/", obtenerReportes);
router.put("/:id", cambiarEstado);
router.delete("/eliminar-todos", eliminarTodosLosReportes); // Endpoint para eliminar todos los reportes

module.exports = router;
