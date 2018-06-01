package com.maximeferrierdev.fourdirections.Tensorflow

/**
    Maxime FERRIER
    Le 04/05/18
*/

class TriScore(val score: Float, val index: Int) : Comparable<TriScore> {

    /**
     * Compare le score actuel avec celui passé en paramètres
     * @param triScore {TriScore} : score à comparer avec le score actuel
     * @return { Int } : -1 si le score actuel est supérieur au score passé en paramètres, 1 dans le
     * cas contraire, 0 en cas d'égalité
     */
    override fun compareTo(triScore: TriScore): Int {
        return when {
            this.score > triScore.score -> -1
            this.score < triScore.score -> 1
            else -> 0
        }
    }
}