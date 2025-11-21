const Reporte = require('../models/Reporte');
const Usuario = require('../models/Usuario');
const nodemailer = require('nodemailer');

// Crear reporte
const crearReporte = async (req, res) => {
  console.log("=== Crear Reporte ===");
  console.log("Body recibido:", req.body);
  console.log("Archivos recibidos:", req.files ? req.files.length : 0);
  
  const { tipo, descripcion, nivelPrioridad, fecha, residenteCorreo, residenteNombre, residenteApartamento, residenteIdentificacion } = req.body;
  
  if (!tipo || !descripcion || !nivelPrioridad || !residenteCorreo || !residenteNombre) {
    console.log("Error: Faltan campos obligatorios");
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios (tipo, descripcion, nivelPrioridad, residenteCorreo, residenteNombre)" });
  }

  try {
    // Obtener rutas de archivos subidos
    const archivosRutas = req.files ? req.files.map(file => file.path) : [];
    
    // Usar la fecha proporcionada o la fecha actual
    const fechaReporte = fecha || new Date().toISOString().split("T")[0];
    
    const nuevoReporte = new Reporte({
      tipo,
      descripcion,
      nivelPrioridad,
      archivos: archivosRutas,
      fecha: fechaReporte,
      estado: "Pendiente",
      seguimiento: "R" + Math.floor(Math.random() * 100000),
      comentariosAdmin: "",
      residenteCorreo,
      residenteNombre,
      residenteApartamento: residenteApartamento || "",
      residenteIdentificacion: residenteIdentificacion || ""
    });

    await nuevoReporte.save();

    // Enviar correo (simulado)
    await enviarCorreo(
      residenteCorreo,
      "Reporte creado",
      `Hola ${residenteNombre}, tu reporte ha sido creado con número de seguimiento: ${nuevoReporte.seguimiento}`
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
    res.json({ reportes: reportes });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor", reportes: [] });
  }
};

// Cambiar estado del reporte
const cambiarEstado = async (req, res) => {
  const { id } = req.params;
  const { estado, comentariosAdmin, identificacionTecnico } = req.body;

  console.log("=== Cambiar Estado de Reporte ===");
  console.log("Estado recibido:", JSON.stringify(estado));
  console.log("Identificación técnico:", identificacionTecnico);
  console.log("Tipo de estado:", typeof estado);
  console.log("Longitud del estado:", estado ? estado.length : 0);

  try {
    // Si el estado es "En análisis", validar técnico
    const estadoNormalizado = estado ? estado.trim() : "";
    console.log("Estado normalizado:", JSON.stringify(estadoNormalizado));
    
    if (estadoNormalizado === "En análisis") {
      console.log("✓ Estado coincide con 'En análisis', validando técnico...");
      if (!identificacionTecnico || identificacionTecnico.trim() === "") {
        console.log("ERROR: No se proporcionó identificación del técnico");
        return res.status(400).json({ mensaje: "Debe proporcionar la identificación del técnico" });
      }

      // Buscar técnico por identificación y validar rol
      const identificacionBuscar = identificacionTecnico.trim();
      console.log("Buscando técnico con identificación:", JSON.stringify(identificacionBuscar));
      
      const tecnico = await Usuario.findOne({ identificacion: identificacionBuscar });
      
      console.log("Técnico encontrado:", tecnico ? tecnico.nombre : "NO ENCONTRADO");
      console.log("Total usuarios en BD:", await Usuario.countDocuments());
      
      if (!tecnico) {
        console.log("ERROR: No se encontró usuario con identificación:", identificacionBuscar);
        return res.status(404).json({ mensaje: `No se encontró un usuario con la identificación: ${identificacionBuscar}` });
      }

      console.log("Rol del técnico encontrado:", JSON.stringify(tecnico.rol));
      console.log("¿Es TÉCNICO DE MANTENIMIENTO?:", tecnico.rol === "TÉCNICO DE MANTENIMIENTO");
      
      if (tecnico.rol !== "TECNICO_MANTENIMIENTO") {
        console.log("ERROR: El usuario tiene rol", tecnico.rol, "pero se requiere TECNICO_MANTENIMIENTO");
        return res.status(403).json({ mensaje: `El usuario ${tecnico.nombre} tiene el rol "${tecnico.rol}". Se requiere el rol "TECNICO_MANTENIMIENTO"` });
      }
      
      console.log("✓ Validación exitosa, asignando técnico al reporte...");

      // Actualizar reporte con técnico asignado
      const reporte = await Reporte.findByIdAndUpdate(
        id,
        { 
          estado: estado, 
          comentariosAdmin: comentariosAdmin || "",
          tecnicoAsignado: tecnico.nombre
        },
        { new: true }
      );

      if (!reporte) return res.status(404).json({ mensaje: "Reporte no encontrado" });

      // Enviar notificación al residente (simulado)
      await enviarCorreo(
        reporte.residenteCorreo,
        "Reporte actualizado",
        `Tu reporte (${reporte.seguimiento}) ha sido asignado al técnico ${tecnico.nombre} y está en análisis.`
      );

      return res.json({ mensaje: "Estado actualizado y técnico asignado", reporte });
    }

    // Para otros estados, actualizar normalmente sin técnico
    console.log("Estado NO es 'En análisis', actualizando normalmente...");
    const reporte = await Reporte.findByIdAndUpdate(
      id,
      { estado: estadoNormalizado, comentariosAdmin: comentariosAdmin || "" },
      { new: true }
    );

    if (!reporte) return res.status(404).json({ mensaje: "Reporte no encontrado" });

    // Enviar notificación al residente (simulado)
    await enviarCorreo(
      reporte.residenteCorreo,
      "Reporte actualizado",
      `Tu reporte (${reporte.seguimiento}) ha sido actualizado a "${estado}". Comentarios: ${comentariosAdmin || "N/A"}`
    );

    res.json({ mensaje: "Estado actualizado", reporte });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Eliminar todos los reportes (solo para desarrollo/testing)
const eliminarTodosLosReportes = async (req, res) => {
  try {
    const resultado = await Reporte.deleteMany({});
    console.log(`Se eliminaron ${resultado.deletedCount} reportes de la base de datos`);
    res.json({ mensaje: `Se eliminaron ${resultado.deletedCount} reportes exitosamente`, cantidad: resultado.deletedCount });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error al eliminar reportes" });
  }
};

// Simulación de envío de correo
const enviarCorreo = async (destino, asunto, texto) => {
  // Configura nodemailer con tu SMTP real si quieres enviar correos
  console.log(`Simulando envío de correo a ${destino}: ${asunto} - ${texto}`);
};

module.exports = { crearReporte, obtenerReportes, cambiarEstado, eliminarTodosLosReportes };
