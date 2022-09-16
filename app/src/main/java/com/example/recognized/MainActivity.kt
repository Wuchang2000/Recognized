package com.example.recognized

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recognized.databinding.ActivityMainBinding
import com.example.recognized.ml.Model7169
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

    private lateinit var camera: Camera
    private lateinit var showCamera: showCamera
    private lateinit var binding: ActivityMainBinding
    private lateinit var imgFoto: FrameLayout
    private lateinit var text_output1: TextView
    private lateinit var text_output2: TextView
    private lateinit var text_output3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imgFoto = binding.imgFoto
        text_output1 = binding.textOutput1
        text_output2 = binding.textOutput2
        text_output3 = binding.textOutput3
        permisos()
        val hilo = Thread(Runnable {
            while (true){
                if (showCamera.foto != null){
                    prediccion(showCamera.foto)
                }
                else
                    Thread.sleep(5000)
            }
        })
        hilo.start()
    }

    private fun permisos(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                val permisosCamara = arrayOf(Manifest.permission.CAMERA)
                requestPermissions(permisosCamara, REQUEST_CAMARA)
            }else
                camara()
        }else
            camara()
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
                    camara()
                else
                    Toast.makeText(applicationContext, "No se pudo acceder a la cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun camara(){
        camera = Camera.open()
        showCamera = showCamera(this, camera)
        imgFoto.addView(showCamera)
    }

    private fun prediccion(bitmap: Bitmap){
        val model = Model7169.newInstance(this)

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
        val sorted = outputFeature0.floatArray
        sorted.sortDescending()
        val indices : MutableList<Int> = mutableListOf()
        for (i in 0..2){
            indices.add(labels.indexOfFirst { it == sorted[i] })
        }
        text_output1.text = verduras[indices[0]]+" "+(labels[indices[0]]*100).toInt()+"%"
        text_output2.text = verduras[indices[1]]+" "+(labels[indices[1]]*100).toInt()+"%"
        text_output3.text = verduras[indices[2]]+" "+(labels[indices[2]]*100).toInt()+"%"
        // Releases model resources if no longer used.
        model.close()
    }
}