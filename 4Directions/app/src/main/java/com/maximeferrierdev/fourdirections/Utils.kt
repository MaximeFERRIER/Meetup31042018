package com.maximeferrierdev.fourdirections

import android.content.res.AssetManager
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
    Maxime FERRIER
    Le 03/05/18
*/

/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
//Récupération des labels

fun labels(assetManager: AssetManager, path_fichierLabel: String): ArrayList<String>? {
    return recuperationLabelsFichier(assetManager, recuperationNomFichierLabels(path_fichierLabel))
}

private fun recuperationLabelsFichier(assetManager: AssetManager, nomFichier: String): ArrayList<String> {
    val labels = ArrayList<String>()
    try {
        BufferedReader(InputStreamReader(assetManager.open(nomFichier))).use {
            var ligne: String? = it.readLine()
            while (ligne != null) {
                labels.add(ligne)
                ligne = it.readLine()
            }
            it.close()
        }
    } catch (e : IOException) {Log.e("4Directions", "recuperationLabelsFichier " + e.message)}

    return labels
}

private fun recuperationNomFichierLabels(path_fichierLabels: String): String {
    return path_fichierLabels.split(PATH_ASSET.toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
}

