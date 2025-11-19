const mongoose = require("mongoose");

const bitacoraSchema = new mongoose.Schema({
    incidenciaAsociada: { type: String, required: true },
    tipoMantenimiento: {
        type: String,
        enum: ["Preventivo", "Reparación", "Correctivo"],
        required: true
    },
    descripcion: { type: String, required: true },
    responsable: { type: String, required: true },
    fechaEjecucion: { type: Date, required: true },
    fotosAntes: [String],    // URLs de imágenes
    fotosDespues: [String],  // URLs de imágenes
    observaciones: { type: String, default: "" },
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Bitacora", bitacoraSchema);
