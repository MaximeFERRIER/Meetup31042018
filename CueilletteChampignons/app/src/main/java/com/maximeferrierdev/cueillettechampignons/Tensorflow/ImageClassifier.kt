package com.maximeferrierdev.cueillettechampignons.Tensorflow

import android.graphics.Bitmap
import com.maximeferrierdev.cueillettechampignons.CANAL_COULEUR
import com.maximeferrierdev.cueillettechampignons.Model.ResultatClassificationImage
import java.util.*
import java.lang.Float
import org.tensorflow.contrib.android.TensorFlowInferenceInterface

/**
    Maxime FERRIER
    Le 29/04/18
*/

/**
 * Appel de fonctions issus de la bibliothèque Tensorflow
 * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/TensorFlowImageClassifier.java
 */
class ImageClassifier (
        private val inputName: String,
        private val outputName: String,
        private val imageSize: Long,
        private val labels: List<String>,
        private val imageBitmapPixels: IntArray,
        private val imageNormalizedPixels: FloatArray,
        private val results: FloatArray,
        private val tensorFlowInference: TensorFlowInferenceInterface) : Classifier {

    override fun reconnaissanceImage(bitmap: Bitmap): ResultatClassificationImage {
        preprocessImageToNormalizedFloats(bitmap)
        classifyImageToOutputs()
        val outputQueue = getResults()
        return outputQueue.poll()
    }

    private fun preprocessImageToNormalizedFloats(bitmap: Bitmap) {
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        val imageMean = 128
        val imageStd = 128.0f
        bitmap.getPixels(imageBitmapPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in imageBitmapPixels.indices) {
            val valPixel = imageBitmapPixels[i]
            imageNormalizedPixels[i * 3] = ((valPixel shr 16 and 0xFF) - imageMean) / imageStd
            imageNormalizedPixels[i * 3 + 1] = ((valPixel shr 8 and 0xFF) - imageMean) / imageStd
            imageNormalizedPixels[i * 3 + 2] = ((valPixel and 0xFF) - imageMean) / imageStd
        }
    }

    private fun classifyImageToOutputs() {
        tensorFlowInference.feed(inputName, imageNormalizedPixels,
                1L, imageSize, imageSize, CANAL_COULEUR.toLong())
        tensorFlowInference.run(arrayOf(outputName), false)
        tensorFlowInference.fetch(outputName, results)
    }

    private fun getResults(): PriorityQueue<ResultatClassificationImage> {
        val outputQueue = createOutputQueue()
        results.indices.mapTo(outputQueue) { ResultatClassificationImage(labels[it], results[it]) }
        return outputQueue
    }

    private fun createOutputQueue(): PriorityQueue<ResultatClassificationImage> {
        return PriorityQueue(
                labels.size,
                Comparator { (_, rConfidence), (_, lConfidence) ->
                    Float.compare(lConfidence, rConfidence) })
    }
}