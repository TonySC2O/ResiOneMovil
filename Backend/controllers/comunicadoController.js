// comunicadoController.js
const Comunicado = require('../models/Comunicado');

exports.obtenerComunicados = async (req, res) => {
  try {
    const comunicados = await Comunicado.find();
    res.json({ comunicados });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: 'Error al obtener comunicados' });
  }
};

exports.crearComunicado = async (req, res) => {
  try {
    const { titulo, contenido, autorId, creadoPorAdministrador } = req.body;
    const nuevo = new Comunicado({ titulo, contenido, autorId, creadoPorAdministrador });
    await nuevo.save();
    res.status(201).json(nuevo);
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: 'Error al crear comunicado' });
  }
};

exports.editarComunicado = async (req, res) => {
  try {
    const { id } = req.params;
    const { titulo, contenido } = req.body;
    const actualizado = await Comunicado.findByIdAndUpdate(id, { titulo, contenido, ultimaActualizacion: Date.now() }, { new: true });
    res.json(actualizado);
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: 'Error al editar comunicado' });
  }
};

exports.eliminarComunicado = async (req, res) => {
  try {
    const { id } = req.params;
    await Comunicado.findByIdAndDelete(id);
    res.json({ mensaje: 'Comunicado eliminado' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ mensaje: 'Error al eliminar comunicado' });
  }
};
