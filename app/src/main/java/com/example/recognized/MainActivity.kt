package com.example.recognized

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.recognized.databinding.ActivityMainBinding
import com.example.recognized.ml.Model7068
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMARA = 1001

    private val verduras = arrayOf("ajo", "cebolla", "chicharo", "chicoria", "chile", "col", "coliflor", "espinaca",
        "jengibre", "jitomate", "lechuga", "maiz", "nabo", "papa", "pepino", "pimiento", "zanahoria")

    private lateinit var binding: ActivityMainBinding
    private lateinit var imgFoto: ImageView
    private lateinit var btn_camara: Button
    private lateinit var text_output: TextView

    var foto: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imgFoto = binding.imgFoto
        btn_camara = binding.btnCamara
        text_output = binding.textOutput
        camaraClick()
    }

    private fun camaraClick(){
        btn_camara.setOnClickListener(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    val permisosCamara = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permisosCamara, REQUEST_CAMARA)
                }else
                    takePicturePreview.launch(null)
            }else
                takePicturePreview.launch(null)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CAMARA -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    takePicturePreview.launch(null)
                else
                    Toast.makeText(applicationContext, "No se pudo acceder a la cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap ->
        if (bitmap != null){
            imgFoto.setImageBitmap(bitmap)
            prediccion(bitmap)
        }
    }

    private fun prediccion(bitmap: Bitmap){
        val model = Model7068.newInstance(this)

        // Creates inputs for reference.
        var image = TensorImage.fromBitmap(bitmap)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(100, 100, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()
        image = imageProcessor.process(image)
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 100, 100, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(image.buffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
        val labels = outputFeature0.floatArray
        val max = labels.maxOrNull()
        val predict = labels.indexOfFirst { it == max }
        text_output.text = verduras[predict]
        // Releases model resources if no longer used.
        model.close()
    }
}