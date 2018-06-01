package com.maximeferrierdev.fourdirections.Tensorflow

import android.util.Log
import com.maximeferrierdev.fourdirections.Model.ReconnaissanceResultat
import com.maximeferrierdev.fourdirections.TEMPS_FRACTION_MINIMUM
import com.maximeferrierdev.fourdirections.VALUE_SILENCE
import java.util.*

/**
    Maxime FERRIER
    Le 04/05/18
 */

/**
 * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/RecognizeCommands.java
 */

class ReconnaissanceCommandes {
    private var listLabels : List<String> ?= null
    private var tempsMoyenDetection : Long ?= null
    private var seuilDetection : Float ?= null
    private var suppressionMS : Int ?= null
    private var nbMinimum : Int ?= null
    private var tempsMinimumEntreEchantillonsMS : Long ?= null
    private var resultatsPrecedents : Deque<Pair<Long, FloatArray>> = ArrayDeque<Pair<Long, FloatArray>>()
    private var precedentLabel : String ?= null
    private var nombreLabels : Int ?= null
    private var tempsLabelPrecedent : Long ?= null
    private var scoreLabelPrecedent : Float ?= null

    constructor(listLabels : List<String>, tempsMoyenDetection : Long, seuilDetection : Float,
                suppressionMS : Int, nbMinimum : Int, tempsMinimumEntreEchantillonsMS : Long) {
        this.listLabels = listLabels
        this.tempsMoyenDetection = tempsMoyenDetection
        this.seuilDetection = seuilDetection
        this.suppressionMS = suppressionMS
        this.nbMinimum = nbMinimum
        this.tempsMinimumEntreEchantillonsMS = tempsMinimumEntreEchantillonsMS

        this.nombreLabels = listLabels.size
        this.precedentLabel = VALUE_SILENCE
        this.tempsLabelPrecedent = Long.MIN_VALUE
        this.scoreLabelPrecedent = 0.0f

    }

    fun executionDerniersResultats(resultatsActuels : FloatArray, tempsActuelMS : Long) : ReconnaissanceResultat {
        var nombreResultats : Int
        var limiteTemps : Long = tempsActuelMS - tempsMoyenDetection!!
        var scoresMoyens = FloatArray(nombreLabels!!)
        var indexActuel : Int
        var labelActuel : String
        var scoreActuel : Float
        var tempsDepuisPrecedentMS : Long
        var nouvelleCommande : Boolean
        var tempsPrecedent : Long
        var dureeEchantillon : Long


        //Vérifie la cohérence des données reçues. Nombre de résultats, durées d'enregistrements etc.
        if(resultatsActuels.size != nombreLabels) {
            throw RuntimeException("Incohérnce dans le nombre de résultats. " + resultatsActuels.size + " au lieu de " + nombreLabels)
        }

        if(!resultatsPrecedents.isEmpty() && (tempsActuelMS < resultatsPrecedents.first.first)) {
            throw RuntimeException("Incohérence dans le temps. Timestamp actuel : " + tempsActuelMS + " temps reçu : " + resultatsPrecedents.first.first)
        }

        nombreResultats = resultatsPrecedents.size

        if(nombreResultats > 1) {
            if((tempsActuelMS - resultatsPrecedents.first!!.first) < tempsMinimumEntreEchantillonsMS!!) {
                return ReconnaissanceResultat(precedentLabel!!, scoreLabelPrecedent!!, false)
            }
        }

        //Ajoute le dernier résultat en tête de queue
        resultatsPrecedents.addLast(Pair<Long, FloatArray>(tempsActuelMS, resultatsActuels))

        //Supprime les données trop anciennes
        while (resultatsPrecedents.first!!.first < limiteTemps) {
            resultatsPrecedents.removeFirst()
        }

        tempsPrecedent = resultatsPrecedents.first.first
        dureeEchantillon = tempsActuelMS - tempsPrecedent

        if((nombreResultats < nbMinimum!!) || (dureeEchantillon < (tempsMoyenDetection!!.div(TEMPS_FRACTION_MINIMUM)))) {
            Log.d("4Directions", "Trop peu de résultats")
            return ReconnaissanceResultat(precedentLabel!!, 0.0f, false)
        }

        //Calcul le score moyen de tous les résultats
        for (resultatPrecedent in resultatsPrecedents) {
            val score : FloatArray = resultatPrecedent.second

            for(i in 0 until score.size) {
                scoresMoyens[i] += score[i] / nombreResultats
            }
        }

        var scoresMoyensTries = arrayOfNulls<TriScore>(nombreLabels!!)

        //Tri les scores dans l'ordre décroissant
        for (i in 0 until nombreLabels!!) {
            scoresMoyensTries[i] = TriScore(scoresMoyens[i], i)
        }

        Arrays.sort(scoresMoyensTries)

        indexActuel = scoresMoyensTries[0]!!.index
        labelActuel = listLabels!![indexActuel]
        scoreActuel = scoresMoyensTries[0]!!.score

        tempsDepuisPrecedentMS = if(precedentLabel.equals(VALUE_SILENCE) || tempsLabelPrecedent == Long.MIN_VALUE) {
            Long.MAX_VALUE
        } else {
            tempsActuelMS - tempsLabelPrecedent!!
        }

        //On regarde si le meilleur score précédent est suffisant pour considérer que l'on a
        //prononcé un nouveau mot clef
        if((scoreActuel > seuilDetection!!) && tempsDepuisPrecedentMS > suppressionMS!!) {
            precedentLabel = labelActuel
            tempsLabelPrecedent = tempsActuelMS
            scoreLabelPrecedent = scoreActuel
            nouvelleCommande = true
        } else {
            nouvelleCommande = false
        }

        return ReconnaissanceResultat(labelActuel, scoreActuel, nouvelleCommande)
    }

}