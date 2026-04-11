package com.phishblock.models

import android.content.Context
import android.content.res.AssetFileDescriptor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Result of the phishing prediction.
 */
data class PredictionResult(
    val score: Float,
    val classification: String
)

/**
 * Helper class to load and run the TFLite phishing detection model.
 */
class ModelHelper(context: Context) : AutoCloseable {

    private var interpreter: Interpreter? = null
    private val featureExtractor = FeatureExtractor()

    init {
        try {
            val modelBuffer = loadModelFile(context, "phishing_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the TFLite model from the assets folder using a MappedByteBuffer for efficiency.
     */
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Runs inference on the provided URL.
     * @param url The URL to analyze.
     * @return A PredictionResult containing the score and classification.
     */
    fun predict(url: String): PredictionResult {
        val tfliteInterpreter = interpreter ?: return PredictionResult(0f, "unknown")

        return try {
            // 1. Generate FloatArray of size 62
            val features = featureExtractor.extractFeatures(url)

            // 2. Prepare input [1, 62] and output [1, 1] buffers
            val input = Array(1) { features }
            val output = Array(1) { FloatArray(1) }

            // 3. Run inference
            tfliteInterpreter.run(input, output)

            // 4. Determine result
            val score = output[0][0]
            val classification = if (score > 0.1f) "phishing" else "safe"
            android.util.Log.d("ModelHelper", "Score: $score, Result: $classification")

            PredictionResult(score, classification)
        } catch (e: Exception) {
            e.printStackTrace()
            PredictionResult(0f, "error")
        }
    }

    /**
     * Releases the interpreter resources.
     */
    override fun close() {
        interpreter?.close()
        interpreter = null
    }
}
