package com.maximeferrierdev.fourdirections.Model

/**
    Maxime FERRIER
    Le 04/05/18
*/

//Objet stockant les informations des sons reconnus.
data class ReconnaissanceResultat(val commandeDetectee: String, val score: Float, val nouvelleCommande: Boolean)