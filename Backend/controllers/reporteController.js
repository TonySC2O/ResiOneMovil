const Reporte = require('../models/Reporte');
const nodemailer = require('nodemailer');

// Validar fecha no anterior al día actual
const validarFecha = (fecha) => {
  const hoy = new Date().toISOString().split("T")[0];
  return fecha >= hoy;
};

// Crear reporte
const crearReporte = async (req, res) => {
  const { tipo, descripcion, nivelPrioridad, correoResidente } = req.body;
  const fecha = new Date().toISOString().split("T")[0];

  if (!tipo || !descripcion || !nivelPrioridad || !correoResidente) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }

  if (!validarFecha(fecha)) {
    return res.status(400).json({ mensaje: "La fecha no puede ser anterior al día actual" });
  }

  try {
    // Procesar archivos subidos
    let archivosUrls = [];
    if (req.files && req.files.length > 0) {
      archivosUrls = req.files.map(file => `/uploads/reportes/${file.filename}`);
    }

    const nuevoReporte = new Reporte({
      tipo,
      descripcion,
      nivelPrioridad,
      archivos: archivosUrls,
      fecha,
      estado: "Pendiente",
      seguimiento: "R" + Math.floor(Math.random() * 100000),
      comentariosAdmin: ""
    });

    await nuevoReporte.save();

    // Enviar correo (simulado)
    await enviarCorreo(
      correoResidente,
      "Reporte creado",
      `Tu reporte ha sido creado con número de seguimiento: ${nuevoReporte.seguimiento}`
    );

    res.status(201).json({ mensaje: "Reporte creado correctamente", reporte: nuevoReporte });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Obtener todos los reportes
const obtenerReportes = async (req, res) => {
  try {
    const reportes = await Reporte.find().sort({ fecha: -1 });
    res.json(reportes);
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Cambiar estado (administrador con código SMS)
const cambiarEstado = async (req, res) => {
  const { id } = req.params;
  const { nuevoEstado, comentarios, codigo, codigoValido, correoResidente } = req.body;

  if (!codigoValido) return res.status(403).json({ mensaje: "Código de verificación inválido" });

  try {
    const reporte = await Reporte.findByIdAndUpdate(
      id,
      { estado: nuevoEstado, comentariosAdmin: comentarios },
      { new: true }
    );

    if (!reporte) return res.status(404).json({ mensaje: "Reporte no encontrado" });

    // Enviar notificación al residente
    await enviarCorreo(
      correoResidente,
      "Reporte actualizado",
      `Tu reporte (${reporte.seguimiento}) ha sido actualizado a "${nuevoEstado}". Comentarios: ${comentarios || "N/A"}`
    );

    res.json({ mensaje: "Estado actualizado", reporte });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Simulación de envío de correo
const enviarCorreo = async (destino, asunto, texto) => {
  // Configura nodemailer con tu SMTP real si quieres enviar correos
  console.log(`Simulando envío de correo a ${destino}: ${asunto} - ${texto}`);
};

module.exports = { crearReporte, obtenerReportes, cambiarEstado };
