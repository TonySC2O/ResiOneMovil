const mongoose = require("mongoose");

const cuotaSchema = new mongoose.Schema({
  monto: { type: Number, required: true },
  fechaVencimiento: { type: String, required: true },
  unidadHabitacional: { type: String, required: true },
  residente: { type: String, required: true },
  estado: { 
    type: String, 
    enum: ["Pendiente", "Cancelado", "Atrasado"], 
    default: "Pendiente" 
  },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Cuota", cuotaSchema);


const pagoSchema = new mongoose.Schema({
  cuotaId: { type: mongoose.Schema.Types.ObjectId, ref: "Cuota", required: true },
  
  residenteId: { type: String, required: true },
  nombreResidente: { type: String, required: true },
  unidadHabitacional: { type: String, required: true },

  fechaPago: { type: String, required: true },

  metodoPago: { 
    type: String, 
    enum: ["Efectivo", "Transferencia bancaria"], 
    required: true 
  },

  comprobantePDF: { type: String },  // URL o path al archivo
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Pago", pagoSchema);


const facturaSchema = new mongoose.Schema({
  pagoId: { type: mongoose.Schema.Types.ObjectId, ref: "Pago", required: true },
  cuotaId: { type: mongoose.Schema.Types.ObjectId, ref: "Cuota", required: true },
  numeroFactura: { type: String, required: true, unique: true },

  detalle: { type: String, required: true },
  nombreResidente: { type: String, required: true },
  metodoPago: { type: String, required: true },

  pdfPath: { type: String },  // archivo generado

  fechaEmision: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Factura", facturaSchema);
