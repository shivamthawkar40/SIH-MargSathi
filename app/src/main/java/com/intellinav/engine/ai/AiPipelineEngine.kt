package com.intellinav.engine.ai

import android.content.Context
import com.intellinav.data.model.RawSensorData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.sqrt

data class AiFeatures(
    val accVariance: Float = 0f,
    val gyroPower: Float = 0f,
    val vibrationEnergy: Float = 0f,
    val estimatedAiSpeedMs: Float = 0f,
    val statusText: String = "Edge TFLite Inference Active (IO-VNBD)"
)

class AiPipelineEngine(private val context: Context? = null) {

    private var tfliteInterpreter: Interpreter? = null
    private val windowSize = 25
    private val rawWindow = mutableListOf<FloatArray>()
    private val accWindow = mutableListOf<Float>()

    init {
        loadModel()
    }

    private fun loadModel() {
        if (context == null) return
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("speed_model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            tfliteInterpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            tfliteInterpreter = null
        }
    }

    fun extractFeaturesAndPredict(raw: RawSensorData): AiFeatures {
        // 1. Sliding window buffer maintain karein
        accWindow.add(raw.accMagnitude)
        rawWindow.add(floatArrayOf(raw.accX, raw.accY, raw.accZ, raw.gyroX, raw.gyroY, raw.gyroZ))

        if (accWindow.size > windowSize) {
            accWindow.removeAt(0)
        }
        if (rawWindow.size > windowSize) {
            rawWindow.removeAt(0)
        }

        // 2. Statistical Signal Features
        val mean = accWindow.average().toFloat()
        var sumSqDev = 0f
        for (v in accWindow) {
            sumSqDev += (v - mean) * (v - mean)
        }
        val variance = if (accWindow.isNotEmpty()) sumSqDev / accWindow.size else 0f
        val gyroPower = raw.gyroMagnitude * raw.gyroMagnitude

        // 3. Zero Velocity Detection (ZUPT condition)
        val isStationary = variance < 0.02f && abs(raw.gyroMagnitude) < 0.05f

        // 4. Edge AI Speed Prediction
        var predictedSpeed = 0f
        var status = "AI Speed Model Active"

        if (isStationary) {
            predictedSpeed = 0f
            status = "Stationary (ZUPT Active)"
        } else if (tfliteInterpreter != null && rawWindow.size == windowSize) {
            try {
                // Input buffer format: (1, 25, 6) Float32
                val inputBuffer = ByteBuffer.allocateDirect(1 * windowSize * 6 * 4)
                inputBuffer.order(ByteOrder.nativeOrder())
                for (frame in rawWindow) {
                    for (value in frame) {
                        inputBuffer.putFloat(value)
                    }
                }
                inputBuffer.rewind()

                val outputBuffer = ByteBuffer.allocateDirect(1 * 4)
                outputBuffer.order(ByteOrder.nativeOrder())

                tfliteInterpreter?.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                predictedSpeed = outputBuffer.float.coerceIn(0f, 40f)
                status = "Edge TFLite Inference (10Hz)"
            } catch (e: Exception) {
                predictedSpeed = fallbackHeuristicSpeed(variance)
                status = "AI Fallback Filter Active"
            }
        } else {
            predictedSpeed = fallbackHeuristicSpeed(variance)
            status = "Statistical Kinematic Filter"
        }

        return AiFeatures(
            accVariance = variance,
            gyroPower = gyroPower,
            vibrationEnergy = variance * 0.75f,
            estimatedAiSpeedMs = predictedSpeed,
            statusText = status
        )
    }

    private fun fallbackHeuristicSpeed(variance: Float): Float {
        return (sqrt(variance.toDouble()) * 3.2).toFloat().coerceIn(0f, 35f)
    }

    fun close() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }
}