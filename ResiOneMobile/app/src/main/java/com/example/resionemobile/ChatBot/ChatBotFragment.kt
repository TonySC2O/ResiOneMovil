package com.example.resionemobile.chatbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resionemobile.databinding.FragmentChatbotBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatAdapter

    // MAPA DE RESPUESTAS (aquí pones todas las palabras clave que quieras)
    private val respuestas = mapOf(
        "hola" to "¡Hola! ¿En qué te puedo ayudar hoy?",
        "cuota" to "La cuota de mantenimiento se paga mensualmente. Puedes verla en el módulo de Pagos.",
        "pagar" to "Puedes pagar por transferencia o efectivo. Sube el comprobante en la sección Pagos.",
        "visita" to "Para autorizar una visita, ve a Seguridad → Registrar visitante. Se genera un QR automáticamente.",
        "qr" to "El QR se envía al correo del visitante. También puedes generarlo desde la app.",
        "mantenimiento" to "Reporta daños en el módulo de Incidencias. Un técnico será asignado pronto.",
        "salón" to "Reserva el salón de eventos en la sección Espacios Comunes.",
        "cancelar" to "Puedes cancelar reservas con al menos 24 horas de anticipación sin penalización.",
        "horario" to "La administración atiende de lunes a viernes de 8:00 a.m. a 5:00 p.m.",
        "ayuda" to "Puedo ayudarte con: cuota, pagar, visita, QR, mantenimiento, salón, cancelar, horario"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatAdapter()
        binding.rvChat.adapter = adapter
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())

        // Mensaje de bienvenida
        adapter.addMessage(ChatMessage("¡Hola! Soy ResiBot, tu asistente del residencial. Escribe 'ayuda' para ver comandos.", false))

        binding.btnSend.setOnClickListener {
            val texto = binding.etMessage.text.toString().trim().lowercase()
            if (texto.isNotEmpty()) {
                // Mensaje del usuario
                adapter.addMessage(ChatMessage(texto.replaceFirstChar { it.uppercase() }, true))
                binding.etMessage.text?.clear()

                // Respuesta del bot
                val respuesta = respuestas.entries.find { texto.contains(it.key) }?.value
                    ?: "Lo siento, no entendí. Escribe 'ayuda' para ver lo que puedo hacer."

                // Simular delay como si estuviera "pensando"
                view.postDelayed({
                    adapter.addMessage(ChatMessage(respuesta, false))
                    binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                }, 600)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}