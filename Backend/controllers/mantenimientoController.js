const Bitacora = require("../models/Mantenimiento");

// Tipos válidos
const TIPOS_VALIDOS = ["Preventivo", "Reparación", "Correctivo"];

// Validar fecha futura o actual
function fechaValida(fecha) {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const f = new Date(fecha);
    return f >= hoy;
}

// Crea bitácora (acción exclusiva del técnico)
const crearBitacora = async (req, res) => {
    const {
        incidenciaAsociada,
        tipoMantenimiento,
        descripcion,
        responsable,
        fechaEjecucion,
        fotosAntes,
        fotosDespues,
        observaciones
    } = req.body;

    // Validación de campos obligatorios
    if (!incidenciaAsociada || !tipoMantenimiento || !descripcion || !responsable || !fechaEjecucion) {
        return res.status(400).json({ mensaje: "Todos los campos obligatorios deben ser completados" });
    }

    // Validación de tipo
    if (!TIPOS_VALIDOS.includes(tipoMantenimiento)) {
        return res.status(400).json({ mensaje: "Tipo de mantenimiento inválido" });
    }

    // Validación de fecha
    if (!fechaValida(fechaEjecucion)) {
        return res.status(400).json({ mensaje: "La fecha de ejecución no puede ser anterior a hoy" });
    }

    try {
        const nuevaBitacora = new Bitacora({
            incidenciaAsociada,
            tipoMantenimiento,
            descripcion,
            responsable,
            fechaEjecucion,
            fotosAntes: fotosAntes || [],
            fotosDespues: fotosDespues || [],
            observaciones: observaciones || ""
        });

        await nuevaBitacora.save();

        res.status(201).json({ 
            mensaje: "Bitácora registrada correctamente",
            bitacora: nuevaBitacora 
        });

    } catch (error) {
        console.error("Error al crear bitácora:", error);
        res.status(500).json({ mensaje: "Error interno del servidor" });
    }
};


// Obtiene historial de mantenientos (solo admins)
const obtenerBitacoras = async (req, res) => {

    // TODO (Opcional): Validar rol con middleware
    // if (req.user.rol !== "admin") return res.status(403).json({ mensaje: "Acceso denegado" });

    try {
        const bitacoras = await Bitacora.find().sort({ fechaEjecucion: -1 });
        res.json(bitacoras);
    } catch (error) {
        console.error("Error al obtener bitácoras:", error);
        res.status(500).json({ mensaje: "Error interno del servidor" });
    }
};

module.exports = { crearBitacora, obtenerBitacoras };
