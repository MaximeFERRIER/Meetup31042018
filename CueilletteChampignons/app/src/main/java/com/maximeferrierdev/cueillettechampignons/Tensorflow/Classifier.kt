package com.maximeferrierdev.cueillettechampignons.Tensorflow

/**
    Maxime FERRIER
    Le 29/04/18
*/

import android.graphics.Bitmap
import com.maximeferrierdev.cueillettechampignons.Model.ResultatClassificationImage

/**
 * Interface avec Tensorflow et notre implémentation de classifier
 */
interface Classifier {
    /**
     * Classification de l'image prise en fonction du modèle entrainé
     * @param bitmap {Bitmap} : image d'entrée
     * @return {ResultatClassificationImage} : Objet contenant le nom de la catégorie
     * à laquelle l'image appartient et son indice de confiance
     */
    fun reconnaissanceImage(bitmap: Bitmap): ResultatClassificationImage
}