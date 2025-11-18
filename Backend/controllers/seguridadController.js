const Visita = require("../models/Seguridad");
const { v4: uuidv4 } = require("uuid");
const QRCode = require("qrcode");

// Valida ID visitante (8 dígitos)
function validarIdVisitante(id) {
    return /^[0-9]{8}$/.test(id);
}

// Valida email
function validarEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// Valida placa de Costa Rica
function validarPlaca(placa) {
    const regex = /^(?:[A-Z]{3}\d{3}|\d{6}[A-Z]?|\d{5})$/;
    return regex.test(placa);
}


const registrarEntrada = async (req, res) => {
    const {
        idVisitante,
        nombre,
        tipoVisita,
        residente,
        correo,
        fechaIngreso,
        vehiculo
    } = req.body;

    // Valida campos obligatorios
    if (!idVisitante || !nombre || !correo || !tipoVisita || !fechaIngreso) {
        return res.status(400).json({ mensaje: "Todos los campos obligatorios deben completarse" });
    }

    // Validaciones específicas
    if (!validarIdVisitante(idVisitante)) {
        return res.status(400).json({ mensaje: "El ID del visitante debe tener 8 dígitos numéricos" });
    }

    if (!validarEmail(correo)) {
        return res.status(400).json({ mensaje: "Formato de correo inválido" });
    }

    const fechaIng = new Date(fechaIngreso);
    if (fechaIng < new Date()) {
        return res.status(400).json({ mensaje: "La fecha de ingreso no puede ser anterior a la actual" });
    }

    if (vehiculo && vehiculo.placa && !validarPlaca(vehiculo.placa)) {
        return res.status(400).json({ mensaje: "Formato de placa de Costa Rica inválido" });
    }

    try {
        // Verifica que no exista otra visita abierta con ese ID
        const existente = await Visita.findOne({ idVisitante, estado: "Abierta" });
        if (existente) {
            return res.status(400).json({ mensaje: "Este visitante ya tiene una visita activa" });
        }

        // Crea UUID y QR
        const qrId = uuidv4();
        const qrData = await QRCode.toDataURL(qrId);

        const nuevaVisita = new Visita({
            idVisitante,
            nombre,
            tipoVisita,
            residente: tipoVisita === "Visitar residente" ? residente : "",
            correo,
            fechaIngreso: fechaIng,
            vehiculo: vehiculo || null,
            qrId,
            estado: "Abierta"
        });

        await nuevaVisita.save();

        res.status(201).json({
            mensaje: "Entrada registrada correctamente",
            visita: nuevaVisita,
            qr: qrData
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({ mensaje: "Error interno del servidor" });
    }
};


const registrarSalida = async (req, res) => {
    const { qrId, fechaSalida } = req.body;

    if (!qrId || !fechaSalida) {
        return res.status(400).json({ mensaje: "QR y fecha de salida son obligatorios" });
    }

    const fechaSal = new Date(fechaSalida);
    if (fechaSal < new Date()) {
        return res.status(400).json({ mensaje: "La fecha de salida no puede ser anterior a la actual" });
    }

    try {
        const visita = await Visita.findOne({ qrId, estado: "Abierta" });

        if (!visita) {
            return res.status(404).json({ mensaje: "Visita no encontrada o ya cerrada" });
        }

        if (fechaSal < visita.fechaIngreso) {
            return res.status(400).json({ mensaje: "La salida no puede ser anterior al ingreso" });
        }

        visita.fechaSalida = fechaSal;
        visita.estado = "Cerrada";

        await visita.save();

        res.json({ mensaje: "Salida registrada correctamente", visita });

    } catch (error) {
        console.error(error);
        res.status(500).json({ mensaje: "Error interno del servidor" });
    }
};


const obtenerBitacora = async (req, res) => {
    try {
        const visitas = await Visita.find().sort({ fechaIngreso: -1 });
        res.json(visitas);
    } catch (error) {
        console.error("Error al obtener bitácora:", error);
        res.status(500).json({ mensaje: "Error interno del servidor" });
    }
};


module.exports = { registrarEntrada, registrarSalida, obtenerBitacora };
