package com.oralscan.app.ml.face

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Runs the Siamese model converted from `siamesemodelv4.h5` (see tools/convert_face_model.py).
 * Expects two inputs of shape [1, H, W, 3] and one output of shape [1, 1].
 * Input order doesn't matter: the L1 distance layer is symmetric.
 */
class TfliteFaceVerifier(context: Context, assetPath: String) : FaceVerifier {

    override val name: String = "Siamese face model (${assetPath.substringAfterLast('/')})"
    override val isPlaceholder: Boolean = false

    private val interpreter = Interpreter(loadModel(context, assetPath), Interpreter.Options().setNumThreads(4))
    private val inputSize: Int
    private val lock = Mutex() // Interpreter is not thread-safe

    init {
        check(interpreter.inputTensorCount == 2) {
            "Face model must have 2 inputs (face, reference), found ${interpreter.inputTensorCount}"
        }
        val shape = interpreter.getInputTensor(0).shape()
        check(shape.size == 4 && shape[3] == 3) { "Unexpected face model input shape ${shape.contentToString()}" }
        inputSize = shape[1]
    }

    override suspend fun similarity(face: Bitmap, reference: Bitmap): Float = withContext(Dispatchers.Default) {
        val inputs = arrayOf<Any>(
            FacePreprocessor.toNhwcBuffer(face, inputSize),
            FacePreprocessor.toNhwcBuffer(reference, inputSize),
        )
        val output = Array(1) { FloatArray(1) }
        lock.withLock {
            interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))
        }
        output[0][0]
    }

    private companion object {
        fun loadModel(context: Context, assetPath: String): MappedByteBuffer =
            context.assets.openFd(assetPath).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}
