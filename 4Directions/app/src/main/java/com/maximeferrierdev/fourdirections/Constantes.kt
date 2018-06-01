package com.maximeferrierdev.fourdirections

/**
    Maxime FERRIER
    Le 02/05/18
*/

const val PERMISSION_ENREGISTREMENT_SON = 13
const val PATH_GRAPHDB = "file:///android_asset/conv_actions_frozen.pb"
const val PATH_LABELS = "file:///android_asset/conv_actions_labels.txt"
const val PATH_ASSET = "file:///android_asset/"

const val NOM_ENTREE_DONNEES = "decoded_sample_data:0"
const val NOM_ECHANTILLON = "decoded_sample_data:1"
const val NOM_SCORE = "labels_softmax"

const val SAMPLE_RATE = 16000 //Par défaut, lorsque l'on entraine un modèle aec TF
const val DUREE_ECHANTILLON_MS = 1000
const val DUREE_ENREGISTREMENT = SAMPLE_RATE * DUREE_ECHANTILLON_MS / 1000
const val MOYENNE_DUREE: Long = 500
const val SEUIL_DETETCTION = 0.70f
const val SUPPRESSION_MS = 1500
const val NOMBRE_MINIMUM = 3
const val TEMPS_MINIMUM_ENTRE_ECHANTILLONS_MS: Long = 30

const val VALUE_SILENCE = "_silence_"
const val TEMPS_FRACTION_MINIMUM = 4
const val ENTREE_16BITS : Float = 32767.0f