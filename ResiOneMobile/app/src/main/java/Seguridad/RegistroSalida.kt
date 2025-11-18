package Seguridad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.resionemobile.R
import com.example.resionemobile.api.GenericResponse
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.api.SalidaRequest
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class RegistroSalida : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_salida)
        barcodeView = findViewById(R.id.zxing_barcode_scanner)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    barcodeView.pause()
                    handleQrResult(it.text)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun handleQrResult(qrIdentifier: String) {
        val fechaHoraSalida = SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val request = SalidaRequest(
            qrIdentificador = qrIdentifier,
            fechaHoraSalida = fechaHoraSalida
        )

        RetrofitClient.api.registrarSalida(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegistroSalida, "Salida registrada exitosamente", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@RegistroSalida, "Error al registrar salida: ${response.message()}", Toast.LENGTH_SHORT).show()
                    barcodeView.resume()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@RegistroSalida, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                barcodeView.resume()
            }
        })
    }
}
