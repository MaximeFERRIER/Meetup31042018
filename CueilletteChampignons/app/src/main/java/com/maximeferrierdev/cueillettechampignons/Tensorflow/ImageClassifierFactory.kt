package com.maximeferrierdev.cueillettechampignons.Tensorflow

import android.content.res.AssetManager
import com.maximeferrierdev.cueillettechampignons.CANAL_COULEUR
import com.maximeferrierdev.cueillettechampignons.labels
import org.tensorflow.contrib.android.TensorFlowInferenceInterface

/**
    Maxime FERRIER
    Le 29/04/18
*/

object ImageClassifierFactory {
    fun create(
            assetManager: AssetManager,
            graphFilePath: String,
            labelsFilePath: String,
            imageSize: Int,
            inputName: String,
            outputName: String) : Classifier {

        val labels = labels(assetManager, labelsFilePath)

        return ImageClassifier(
                inputName,
                outputName,
                imageSize.toLong(),
                labels,
                IntArray(imageSize * imageSize),
                FloatArray(imageSize * imageSize * CANAL_COULEUR),
                FloatArray(labels.size),
                TensorFlowInferenceInterface(assetManager, graphFilePath)
        )
    }
}