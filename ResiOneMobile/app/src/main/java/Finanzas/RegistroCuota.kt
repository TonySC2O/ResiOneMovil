package Finanzas

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.api.CrearCuotaRequest
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.databinding.ActivityRegistroCuotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegistroCuota : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroCuotaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroCuotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupDatePicker()

        binding.btnGuardarCuota.setOnClickListener {
            registrarCuota()
        }
    }

    private fun setupSpinner() {
        val estados = arrayOf("Pendiente", "Cancelado", "Atrasado")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.etFechaVencimiento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, {
                _, selectedYear, selectedMonth, selectedDay ->
                val fechaSeleccionada = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                binding.etFechaVencimiento.setText(fechaSeleccionada)
            }, year, month, day)
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }
    }

    private fun registrarCuota() {
        val monto = binding.etMonto.text.toString().toDoubleOrNull()
        val fechaVencimiento = binding.etFechaVencimiento.text.toString()
        val unidadHabitacional = binding.etUnidadHabitacional.text.toString()
        val residente = binding.etResidente.text.toString()
        val estado = binding.spinnerEstado.selectedItem.toString()

        if (monto == null || monto <= 0) {
            Toast.makeText(this, "El monto debe ser mayor a cero", Toast.LENGTH_SHORT).show()
            return
        }
        if (fechaVencimiento.isEmpty()) {
            Toast.makeText(this, "Seleccione una fecha de vencimiento", Toast.LENGTH_SHORT).show()
            return
        }
        if (unidadHabitacional.isEmpty() || residente.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CrearCuotaRequest(
            monto = monto,
            fechaVencimiento = fechaVencimiento,
            unidadHabitacional = unidadHabitacional,
            residente = residente,
            estado = estado
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.crearCuota(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistroCuota, "Cuota creada exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegistroCuota, "Error al crear la cuota", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistroCuota, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
