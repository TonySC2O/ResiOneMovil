const express = require("express");
const router = express.Router();
const multer = require("multer");
const path = require("path");
const { crearReporte, obtenerReportes, cambiarEstado } = require("../controllers/reporteController");

// Configuración de multer para subir archivos
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, 'uploads/reportes/');
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'reporte-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ 
  storage: storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // Límite de 10MB por archivo
  fileFilter: function (req, file, cb) {
    // Aceptar solo imágenes y videos
    const allowedTypes = /jpeg|jpg|png|gif|mp4|mov|avi/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Solo se permiten archivos de imagen y video'));
    }
  }
});

router.post("/", upload.array('archivos', 5), crearReporte); // Máximo 5 archivos
router.get("/", obtenerReportes);
router.put("/:id", cambiarEstado);

module.exports = router;
