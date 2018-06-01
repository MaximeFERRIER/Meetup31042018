package com.maximeferrierdev.cueillettechampignons

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
    Maxime FERRIER
    Le 29/04/18
*/


/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
//Récupération des labels

fun labels(assetManager: AssetManager, path_fichierLabel: String): List<String> {
    return recuperationLabelsFichier(assetManager, recuperationNomFichierLabels(path_fichierLabel))
}

private fun recuperationLabelsFichier(assetManager: AssetManager, nomFichier: String): ArrayList<String> {
    val labels = ArrayList<String>()
    BufferedReader(InputStreamReader(assetManager.open(nomFichier))).use {
        var ligne: String? = it.readLine()
        while (ligne != null) {
            labels.add(ligne)
            ligne = it.readLine()
        }
        it.close()
    }
    return labels
}

private fun recuperationNomFichierLabels(path_fichierLabels: String): String {
    return path_fichierLabels.split(PATH_ASSET.toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
}

/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
//Traitement de l'Uri

/**
 * Conversion d'une photo à partir de son path en Uri
 */
fun extractionUriDepuisPathFichier(context: Context, pathFichier: String): Uri {
    val fichier = File(pathFichier)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".uri",
                fichier)
    } else {
        Uri.fromFile(fichier)
    }
}

/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
//Traitement de l'image

fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
    val croppedBitmap = Bitmap.createBitmap(TAILLE_IMAGE, TAILLE_IMAGE, Bitmap.Config.ARGB_8888)
    val transformationMatrix = getPhotoBitmapTransformationMatrix(bitmap)
    val canvas = Canvas(croppedBitmap)
    canvas.drawBitmap(bitmap, transformationMatrix, null)
    return croppedBitmap
}

private fun getPhotoBitmapTransformationMatrix(bitmap: Bitmap): Matrix {
    val frameToCropTransformationMatrix = getTransformationMatrix(
            bitmap.width, bitmap.height,
            TAILLE_IMAGE, TAILLE_IMAGE,
            0, true)

    val cropToFrameTransformationMatrix = Matrix()
    frameToCropTransformationMatrix.invert(cropToFrameTransformationMatrix)
    return frameToCropTransformationMatrix
}

private fun getTransformationMatrix(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int,
        maintainAspectRatio: Boolean): Matrix {
    val matrix = Matrix()

    // Translate so center of image is at origin.
    matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

    // Rotate around origin.
    matrix.postRotate(applyRotation.toFloat())

    // Account for the already applied rotation, if any, and then determine how
    // much scaling is needed for each axis.
    val transpose = (Math.abs(applyRotation) + 90) % 180 == 0

    val inWidth = if (transpose) srcHeight else srcWidth
    val inHeight = if (transpose) srcWidth else srcHeight

    // Apply scaling if necessary.
    if (inWidth != dstWidth || inHeight != dstHeight) {
        val scaleFactorX = dstWidth / inWidth.toFloat()
        val scaleFactorY = dstHeight / inHeight.toFloat()

        if (maintainAspectRatio) { // Scale by minimum factor so that dst is filled completely while
            // maintaining the aspect ratio. Some image may fall off the edge.
            val scaleFactor = Math.max(scaleFactorX, scaleFactorY)
            matrix.postScale(scaleFactor, scaleFactor)
        } else { // Scale exactly to fill dst from src.
            matrix.postScale(scaleFactorX, scaleFactorY)
        }
    }

    // Translate back from origin centered reference to destination frame.
    matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)

    return matrix
}