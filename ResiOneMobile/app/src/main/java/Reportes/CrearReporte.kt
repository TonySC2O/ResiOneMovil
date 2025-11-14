package Reportes

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R

class CrearReporte : BaseActivity() {

    private val attachedUris = mutableListOf<Uri>()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            attachedUris.clear()
            attachedUris.addAll(uris)
            Toast.makeText(this, "Archivos adjuntados: ${attachedUris.size}", Toast.LENGTH_SHORT).show()
            showThumbnails()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_reporte)

        // Configurar MaterialToolbar como ActionBar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val spinnerTipo = findViewById<Spinner>(R.id.spinner_tipo)
        val etDescripcion = findViewById<EditText>(R.id.et_descripcion)
        val btnAdjuntar = findViewById<ImageButton>(R.id.btn_adjuntar)
        val btnRealizar = findViewById<Button>(R.id.btn_realizar)

        btnAdjuntar.setOnClickListener {
            // Allow images and videos: use wildcard and filter by mime type later
            pickMedia.launch("*/*")
        }

        btnRealizar.setOnClickListener {
            val tipo = spinnerTipo.selectedItem?.toString() ?: ""
            val descripcion = etDescripcion.text.toString().trim()
            if (tipo.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "Por favor completa el tipo y la descripción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Implement real submission (network or DB). For now, show a confirmation.
            Toast.makeText(this, "Reporte creado (simulado)", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("tipo", tipo)
                putExtra("descripcion", descripcion)
            })
            finish()
        }
    }

    private fun showThumbnails() {
        val container = findViewById<LinearLayout>(R.id.attachments_container)
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val sizePx = (120 * density).toInt()
        val margin = (8 * density).toInt()

        for (uri in attachedUris) {
            try {
                val mime = contentResolver.getType(uri) ?: ""
                val imageView = ImageView(this)
                val lp = LinearLayout.LayoutParams(sizePx, sizePx)
                lp.setMargins(margin, margin, margin, margin)
                imageView.layoutParams = lp
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                if (mime.startsWith("image")) {
                    imageView.setImageURI(uri)
                } else if (mime.startsWith("video")) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(this, uri)
                    val bitmap: Bitmap? = mmr.frameAtTime
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        // fallback placeholder
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                    mmr.release()
                } else {
                    // unknown type -> show generic icon
                    imageView.setImageResource(android.R.drawable.ic_menu_help)
                }

                // Add to container
                container.addView(imageView)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
