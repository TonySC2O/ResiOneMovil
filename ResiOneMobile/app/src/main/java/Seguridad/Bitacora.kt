package Seguridad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import com.example.resionemobile.api.BitacoraItem
import com.example.resionemobile.api.BitacoraResponse
import com.example.resionemobile.api.RetrofitClient
import jxl.Workbook
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Bitacora : AppCompatActivity() {

    private lateinit var rvBitacora: RecyclerView
    private lateinit var bitacoraAdapter: BitacoraAdapter
    private lateinit var btnExportarExcel: Button
    private var registros: List<BitacoraItem> = emptyList()
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitacora)

        btnExportarExcel = findViewById(R.id.btnExportarExcel)
        setupRecyclerView()
        obtenerDatosBitacora()

        btnExportarExcel.setOnClickListener {
            checkStoragePermissionAndExport()
        }
    }

    private fun setupRecyclerView() {
        rvBitacora = findViewById(R.id.rvBitacora)
        rvBitacora.layoutManager = LinearLayoutManager(this)
        bitacoraAdapter = BitacoraAdapter(emptyList())
        rvBitacora.adapter = bitacoraAdapter
    }

    private fun obtenerDatosBitacora() {
        RetrofitClient.api.obtenerBitacora().enqueue(object : Callback<BitacoraResponse> {
            override fun onResponse(call: Call<BitacoraResponse>, response: Response<BitacoraResponse>) {
                if (response.isSuccessful) {
                    response.body()?.registros?.let {
                        registros = it
                        bitacoraAdapter.updateData(registros)
                    }
                } else {
                    Toast.makeText(this@Bitacora, "Error al obtener la bitácora", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BitacoraResponse>, t: Throwable) {
                Toast.makeText(this@Bitacora, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkStoragePermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        } else {
            exportToExcel()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToExcel()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportToExcel() {
        if (registros.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Bitacora_ResiOne_$timeStamp.xls"

            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(directory, fileName)

            val workbook = Workbook.createWorkbook(file)
            val sheet = workbook.createSheet("Bitácora", 0)

            // Encabezados
            val headers = listOf(
                "ID Registro",
                "ID Visitante",
                "Nombre",
                "Tipo Visita",
                "Fecha/Hora Ingreso",
                "Fecha/Hora Salida",
                "Placa Vehículo",
                "Residente Relacionado"
            )

            headers.forEachIndexed { index, title ->
                sheet.addCell(jxl.write.Label(index, 0, title))
            }

            // Datos
            registros.forEachIndexed { index, registro ->
                val row = index + 1

                sheet.addCell(jxl.write.Label(0, row, registro.id))
                sheet.addCell(jxl.write.Label(1, row, registro.visitanteId))
                sheet.addCell(jxl.write.Label(2, row, registro.nombre))
                sheet.addCell(jxl.write.Label(3, row, registro.tipoVisita))
                sheet.addCell(jxl.write.Label(4, row, registro.fechaHoraIngreso))
                sheet.addCell(jxl.write.Label(5, row, registro.fechaHoraSalida ?: "N/A"))
                sheet.addCell(jxl.write.Label(6, row, registro.placa ?: "N/A"))
                sheet.addCell(jxl.write.Label(7, row, registro.residenteRelacionado ?: "N/A"))
            }

            workbook.write()
            workbook.close()

            Toast.makeText(this, "Exportado a Excel en Descargas: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
