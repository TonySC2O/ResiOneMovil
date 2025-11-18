const mongoose = require("mongoose");

const visitaSchema = new mongoose.Schema({
    idVisitante: {
        type: String,
        required: true,
        unique: true,
        minlength: 8,
        maxlength: 8
    },
    nombre: { type: String, required: true },
    tipoVisita: { type: String, required: true },
    residente: { type: String },
    correo: { type: String, required: true },

    fechaIngreso: { type: Date, required: true },
    fechaSalida: { type: Date },

    vehiculo: {
        placa: { type: String },
        modelo: { type: String },
        descripcion: { type: String }
    },

    qrId: { type: String, required: true },   // UUID asociado a la visita
    estado: { type: String, default: "Abierta" }
});

module.exports = mongoose.model("Visita", visitaSchema);
