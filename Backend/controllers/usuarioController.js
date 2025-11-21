const Usuario = require('../models/Usuario');

// Validaciones auxiliares
function validarTelefono(telefono) { return /^\d{8}$/.test(telefono); }
function validarApartamento(apto) { return typeof apto === 'string' && apto.trim().length >= 1; }
function validarHabitantes(hab) { const num = Number(hab); return Number.isInteger(num) && num > 0; }

// Registro
const registrarUsuario = async (req, res) => {
  console.log("Registro recibido", req.body);
  const { nombre, correo, telefono, identificacion, contraseña, apartamento, habitantes, esAdministrador, codigoEmpleado } = req.body;

  if (!nombre || !correo || !telefono || !identificacion || !contraseña || !apartamento || !habitantes || (esAdministrador && !codigoEmpleado)) {
    return res.status(400).json({ mensaje: 'Todos los campos son obligatorios' });
  }

  try {
    const nuevoUsuario = new Usuario({
      nombre, correo, telefono, identificacion, contraseña, apartamento, habitantes,
      esAdministrador: !!esAdministrador,
      codigoEmpleado: esAdministrador ? codigoEmpleado : undefined,
      rol: esAdministrador ? 'ADMIN' : 'RESIDENTE'
    });

    await nuevoUsuario.save();
    console.log(`✓ Usuario registrado: ${correo} - Rol: ${nuevoUsuario.rol}`);
    res.status(201).json({ mensaje: 'Usuario registrado correctamente' });
  } catch (error) {
    console.error('Error en registro:', error);
    if (error.code === 11000) return res.status(409).json({ mensaje: 'Correo o identificación ya registrados' });
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Login
const loginUsuario = async (req, res) => {
  console.log("Login recibido", req.body);
  const { correo, contraseña } = req.body;
  if (!correo || !contraseña) return res.status(400).json({ mensaje: 'Faltan datos' });

  try {
    const usuario = await Usuario.findOne({ correo });
    if (!usuario) return res.status(404).json({ mensaje: 'Usuario no encontrado' });
    if (usuario.contraseña !== contraseña) return res.status(401).json({ mensaje: 'Contraseña incorrecta' });

    console.log(`✓ Login exitoso: ${correo} - Rol: ${usuario.rol} - Es Admin: ${usuario.esAdministrador}`);

    res.json({
      mensaje: 'Inicio de sesión exitoso',
      usuario: {
        nombre: usuario.nombre,
        correo: usuario.correo,
        identificacion: usuario.identificacion,
        telefono: usuario.telefono,
        apartamento: usuario.apartamento,
        habitantes: usuario.habitantes,
        esAdministrador: usuario.esAdministrador,
        codigoEmpleado: usuario.codigoEmpleado,
        rol: usuario.rol
      }
    });
  } catch (error) {
    res.status(500).json({ mensaje: 'Error interno del servidor' });
  }
};

// Editar perfil
const editarUsuario = async (req, res) => {
  const { correo, telefono, apartamento, habitantes } = req.body;

  if (!correo) return res.status(400).json({ mensaje: 'Correo es requerido para identificar al usuario' });
  if (telefono && !validarTelefono(telefono)) return res.status(400).json({ mensaje: 'Teléfono inválido: debe tener 8 dígitos' });
  if (apartamento && !validarApartamento(apartamento)) return res.status(400).json({ mensaje: 'Apartamento inválido' });
  if (habitantes && !validarHabitantes(habitantes)) return res.status(400).json({ mensaje: 'Habitantes inválido: debe ser un número entero mayor que 0' });

  try {
    const datosAActualizar = {};
    if (telefono !== undefined) datosAActualizar.telefono = telefono;
    if (apartamento !== undefined) datosAActualizar.apartamento = apartamento;
    if (habitantes !== undefined) datosAActualizar.habitantes = habitantes;

    const usuarioActualizado = await Usuario.findOneAndUpdate(
      { correo },
      datosAActualizar,
      { new: true, runValidators: true }
    );

    if (!usuarioActualizado) return res.status(404).json({ mensaje: 'Usuario no encontrado' });

    const usuarioPublico = {
      nombre: usuarioActualizado.nombre,
      correo: usuarioActualizado.correo,
      telefono: usuarioActualizado.telefono,
      identificacion: usuarioActualizado.identificacion,
      apartamento: usuarioActualizado.apartamento,
      habitantes: usuarioActualizado.habitantes,
      esAdministrador: usuarioActualizado.esAdministrador,
      codigoEmpleado: usuarioActualizado.codigoEmpleado,
      rol: usuarioActualizado.rol
    };

    return res.json({ mensaje: 'Perfil actualizado correctamente', usuario: usuarioPublico });

  } catch (error) {
    console.error('Error al editar perfil:', error);
    return res.status(500).json({ mensaje: 'Error interno al actualizar el perfil' });
  }
};

module.exports = { registrarUsuario, loginUsuario, editarUsuario };
