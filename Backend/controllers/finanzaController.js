const Finanza = require("../models/Finanza");
const { v4: uuidv4 } = require("uuid");

// Validación fecha futura
const fechaEsValida = (fecha) => {
  const hoy = new Date().setHours(0,0,0,0);
  const f = new Date(fecha).setHours(0,0,0,0);
  return f >= hoy;
};


// Crea cuota
const crearCuota = async (req, res) => {
  const { monto, fechaVencimiento, unidadHabitacional, residente, estado } = req.body;

  if (!monto || !fechaVencimiento || !unidadHabitacional || !residente) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }

  if (monto <= 0) {
    return res.status(400).json({ mensaje: "El monto debe ser mayor a 0" });
  }

  if (!fechaEsValida(fechaVencimiento)) {
    return res.status(400).json({ mensaje: "La fecha de vencimiento debe ser hoy o posterior" });
  }

  if (estado && !["Pendiente", "Cancelado", "Atrasado"].includes(estado)) {
    return res.status(400).json({ mensaje: "Estado inválido" });
  }

  try {
    const nuevaCuota = new Finanza({ monto, fechaVencimiento, unidadHabitacional, residente, estado });
    await nuevaCuota.save();

    res.status(201).json({ mensaje: "Cuota registrada correctamente", cuota: nuevaCuota });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Obtiene todas las cuotas
const obtenerCuotas = async (req, res) => {
  try {
    const cuotas = await Finanza.find();
    res.json(cuotas);
  } catch (error) {
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};


const registrarPago = async (req, res) => {
  const { cuotaId, residenteId, nombreResidente, unidadHabitacional, fechaPago, metodoPago } = req.body;
  const comprobantePDF = req.file ? req.file.path : null;

  if (!cuotaId || !residenteId || !nombreResidente || !unidadHabitacional || !fechaPago || !metodoPago) {
    return res.status(400).json({ mensaje: "Todos los campos son obligatorios" });
  }

  if (!["Efectivo", "Transferencia bancaria"].includes(metodoPago)) {
    return res.status(400).json({ mensaje: "Método de pago inválido" });
  }

  if (metodoPago === "Transferencia bancaria" && !comprobantePDF) {
    return res.status(400).json({ mensaje: "Debe adjuntar comprobante PDF" });
  }

  try {
    const cuota = await Finanza.findById(cuotaId);
    if (!cuota) return res.status(404).json({ mensaje: "Cuota no encontrada" });

    // Crea pago
    const nuevoPago = new Finanza({
      cuotaId, residenteId, nombreResidente, unidadHabitacional,
      fechaPago, metodoPago, comprobantePDF
    });
    await nuevoPago.save();

    // Actualiza estado de cuota
    cuota.estado = "Cancelado";
    await cuota.save();

    res.status(201).json({ mensaje: "Pago registrado exitosamente", pago: nuevoPago });

  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};

// Historial de pagos (admin ve todos, residente ve los suyos)
const historialPagos = async (req, res) => {
  const { residenteId } = req.query;

  try {
    const filtro = residenteId ? { residenteId } : {};
    const pagos = await Finanza.find(filtro).sort({ createdAt: -1 });

    res.json(pagos);
  } catch (error) {
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};


const emitirFactura = async (req, res) => {
  const { pagoId } = req.body;

  try {
    const pago = await Finanza.findById(pagoId);
    if (!pago) return res.status(404).json({ mensaje: "Pago no encontrado" });

    const cuota = await Finanza.findById(pago.cuotaId);

    // Número único
    const numeroFactura = "FAC-" + uuidv4();

    // Crea factura
    const factura = new Finanza({
      pagoId,
      cuotaId: cuota._id,
      numeroFactura,
      detalle: `Pago de cuota de mantenimiento: ${cuota.monto}`,
      nombreResidente: pago.nombreResidente,
      metodoPago: pago.metodoPago,
      pdfPath: ""  // TODO: Agregar generación de PDF
    });

    await factura.save();

    res.status(201).json({ mensaje: "Factura emitida", factura });

  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: "Error interno del servidor" });
  }
};


module.exports = {
  crearCuota, obtenerCuotas,
  registrarPago, historialPagos,
  emitirFactura
};
