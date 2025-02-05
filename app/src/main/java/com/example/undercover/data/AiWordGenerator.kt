package com.example.undercover.data

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.FloatBuffer

class AiWordGenerator(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile(context))
            Log.d("AiWordGenerator", "DistilGPT-2 model loaded successfully")
        } catch (e: FileNotFoundException) {
            Log.e("AiWordGenerator", "Model file not found: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e("AiWordGenerator", "Error loading model: ${e.localizedMessage}")
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileName = "distilgpt2_model.tflite"
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun generateWord(prompt: String): String {
        if (interpreter == null) {
            Log.e("AiWordGenerator", "Model is not loaded.")
            return "Error: AI model not found"
        }

        // 🛠️ Verificăm dimensiunile tensorilor de input și output
        val inputShape = interpreter!!.getInputTensor(0).shape()
        val outputShape = interpreter!!.getOutputTensor(0).shape()
        Log.d("AiWordGenerator", "Expected input tensor shape: ${inputShape.toList()}")
        Log.d("AiWordGenerator", "Expected output tensor shape: ${outputShape.toList()}")

        // 🛠️ Creăm input-ul corect pentru model (un token valid)
        val inputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        inputBuffer.putInt(1) // Exemplu de token valid

        // 🛠️ Creăm un buffer suficient de mare pentru output
        val outputBuffer = ByteBuffer.allocateDirect(outputShape.reduce { acc, i -> acc * i } * 4)
            .order(ByteOrder.nativeOrder())

        try {
            interpreter!!.run(inputBuffer, outputBuffer)
            Log.d("AiWordGenerator", "Model inference successful")

            // 🛠️ Convertim buffer-ul într-un array de float-uri
            val outputArray = FloatArray(outputShape.reduce { acc, i -> acc * i })
            outputBuffer.rewind()
            outputBuffer.asFloatBuffer().get(outputArray)

            // 🛠️ Transformăm rezultatul într-un cuvânt (decodificare simplificată)
            return outputArray.joinToString(" ") { it.toString() }
        } catch (e: Exception) {
            Log.e("AiWordGenerator", "Error running model: ${e.localizedMessage}")
            return "Error generating word"
        }
    }
}
