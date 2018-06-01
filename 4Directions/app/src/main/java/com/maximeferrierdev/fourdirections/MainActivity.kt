package com.maximeferrierdev.fourdirections

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.maximeferrierdev.fourdirections.Tensorflow.ReconnaissanceCommandes
import com.maximeferrierdev.fourdirections.Model.ReconnaissanceResultat
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.util.concurrent.locks.ReentrantLock



class MainActivity : AppCompatActivity() {

    private var threadEnregistrement : Thread ?= null
    private var threadReconnaissance : Thread ?= null
    private var poursuivreEnregistrement : Boolean = false
    private var poursuivreReconnaissance : Boolean = true
    private var bufferEnregistrement : ShortArray ?= null
    private var offsetEnregistrement : Int = 0
    private var reentrantLock : ReentrantLock ?= null
    private var labels : ArrayList<String> ?= null
    private var interfaceTensorflow : TensorFlowInferenceInterface ?= null
    private var reconnaissanceCommande : ReconnaissanceCommandes ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bufferEnregistrement = ShortArray(DUREE_ENREGISTREMENT)
        reentrantLock = ReentrantLock()

        checkPermissions()
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Gestion des permissions

    private fun checkPermissions() {
        if (checkToutesPermissionsNecessairesDejaAccordees()) {
            init()
        } else {
            demandePermissions()
        }
    }

    /**
     * Vérifie que l'application a déjà le droit d'enregistrer du son
     * @return {Boolean} : true si les permissions sont accordées, false sinon
     */
    private fun checkToutesPermissionsNecessairesDejaAccordees() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Vérifie que l'application a les permissions nécessaires
     * @param permissions {IntArray} : liste des permissions en cours
     * @return {Boolean} : true si les permissions sont accordées, false sinon
     */
    private fun checkToutesPermissionsAccordees(permissions: IntArray) : Boolean {
        return (permissions.isNotEmpty() && permissions[0] == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Demande la permission d'enregistrer du son
     */
    private fun demandePermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_ENREGISTREMENT_SON)
    }

    /**
     * Catch le retour de demande de permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ENREGISTREMENT_SON && checkToutesPermissionsAccordees(grantResults)) {
            init()
        } else {
            demandePermissions()
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Enregistrement de son
    /**
     * Récupère la liste des labels, crée l'objet ed reconnaissance de la commande vocale,
     * créé l'interface avec Tensorflow et lance l'enregistrement et la reconnaissance du son
     */
    private fun init() {

        labels = labels(assets, PATH_LABELS)
        reconnaissanceCommande = ReconnaissanceCommandes(labels!!, MOYENNE_DUREE, SEUIL_DETETCTION,
                SUPPRESSION_MS, NOMBRE_MINIMUM, TEMPS_MINIMUM_ENTRE_ECHANTILLONS_MS)
        interfaceTensorflow = TensorFlowInferenceInterface(assets, PATH_GRAPHDB)
        enregistrementDuSon()
        debutReconnaissanceSon()
    }

    /**
     * Prépare un nouveau thread pour enregistrer le son et lance l'enregistrement
     */
    @Synchronized
    private fun enregistrementDuSon() {
        if(threadEnregistrement != null) {
            Log.e("4DIRECTIONS", "Le thread d'enregistrement est déjà occupé")
            return
        }
        try {
            poursuivreEnregistrement = true
            threadEnregistrement = Thread(Runnable {
                enregistrement()
            })
            threadEnregistrement!!.start()
        } catch (npe : NullPointerException) {Log.e("4Directions", "enregistrementDuSon " + npe.message)}
    }

    /**
     * Détruit le thread d'enregistrement.
     * Non appelé dans ce programme car écoute en continue (mais c'est pour avoir l'exemple quelque part)
     */
    @Synchronized
    private fun arretEnregistrementSon() {
        if(threadEnregistrement == null) {
            return
        }

        poursuivreEnregistrement = false
        threadEnregistrement = null
    }

    /**
     * Lance l'écoute du son
     */
    private fun enregistrement() {
        var tailleBuffer : Int
        var bufferAudio : ShortArray
        var audioRecord : AudioRecord

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

        tailleBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

        if(tailleBuffer == AudioRecord.ERROR || tailleBuffer == AudioRecord.ERROR_BAD_VALUE) {
            tailleBuffer = SAMPLE_RATE * 2
        }

        bufferAudio = ShortArray(tailleBuffer / 2)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                tailleBuffer)

        if(audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("4Directions", "Echec dans l'initialisation de l'audio record")
            return
        }

        audioRecord.startRecording()
        recuperationSon(audioRecord, bufferAudio)
        audioRecord.stop()
        audioRecord.release()
    }

    /**
     * Enregistre le son capté et le copie dans le buffer. Toutes les données de l'enregistrement
     * sont copiées sur le thread servant à la reconnaissance
     * @param audioRecord { AudioRecord } : Interface avec le microphone pour récupérer le son
     * @param bufferAudio { ShortArray } : Mise en mémoire tampon du son
     */
    private fun recuperationSon(audioRecord : AudioRecord, bufferAudio : ShortArray) {
        while(poursuivreEnregistrement) {
            try {
                var nombreLectures: Int = audioRecord.read(bufferAudio, 0, bufferAudio.size)
                var tailleMax: Int? = bufferEnregistrement?.size
                var nouvelOffsetEnregistrement: Int? = nombreLectures + offsetEnregistrement
                var taille2ndeCopie: Int = Math.max(0, (nouvelOffsetEnregistrement?.minus(tailleMax!!)!!))
                var taille1ereCopie: Int = nombreLectures - taille2ndeCopie
                reentrantLock!!.lock()

                System.arraycopy(bufferAudio, 0, bufferEnregistrement!!, offsetEnregistrement, taille1ereCopie)
                System.arraycopy(bufferAudio, taille1ereCopie, bufferEnregistrement, 0, taille2ndeCopie)
                offsetEnregistrement = nouvelOffsetEnregistrement % tailleMax!!

            } catch (npe: NullPointerException) {
                Log.e("4Directions", npe.message)
            } finally {
                reentrantLock!!.unlock()
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Reconnaissance du son

    /**
     * Crée un nouveau thread pour la reconnaissance du son
     */
    @Synchronized
    fun debutReconnaissanceSon() {
        if(threadReconnaissance != null) {
            Log.e("4DIRECTIONS", "Le thread de reconnaissance est déjà occupé")
            return
        }

        poursuivreReconnaissance = true
        threadReconnaissance = Thread(Runnable {
            reconnaissance()
        })
        threadReconnaissance!!.start()
    }

    /**
     * Lance la reconnaissance du son capté sur le modèle.
     * Les données sont enregistrées dans un buffer pour éviter que l'arrivée des données en continue
     * n'écrase les données en cours d'analyse.
     */
    fun reconnaissance() {
        val labels = labels(assets, PATH_LABELS)
        val bufferEntree = ShortArray(DUREE_ENREGISTREMENT)
        val bufferEntreeFloat = FloatArray(DUREE_ENREGISTREMENT)
        val scores = FloatArray(labels!!.size)
        val scoresIntitules = arrayOf<String>(NOM_SCORE)
        val listSampleRate = intArrayOf(SAMPLE_RATE)
        var tempsActuelMS : Long
        var resultat : ReconnaissanceResultat

        while(poursuivreReconnaissance) {
            reentrantLock!!.lock()
            try {
                var tailleMax : Int = bufferEnregistrement!!.size
                var taille1ereCopie : Int = tailleMax - offsetEnregistrement
                var taille2ndeCopie = offsetEnregistrement
                System.arraycopy(bufferEnregistrement, offsetEnregistrement, bufferEntree, 0, taille1ereCopie)
                System.arraycopy(bufferEnregistrement, 0, bufferEntree, taille1ereCopie, taille2ndeCopie)
            } catch (npe : NullPointerException) {Log.e("4Directions", "reconnaissance NPE " + npe.message)}
            finally {
                reentrantLock!!.unlock()
            }

            for(i in 0 until DUREE_ENREGISTREMENT) {
                bufferEntreeFloat[i] = bufferEntree[i] / ENTREE_16BITS
            }

            //Exécute la reconnaissance contre le modèle
            interfaceTensorflow!!.feed(NOM_ECHANTILLON, listSampleRate)
            interfaceTensorflow!!.feed(NOM_ENTREE_DONNEES, bufferEntreeFloat, DUREE_ENREGISTREMENT.toLong(), 1)
            interfaceTensorflow!!.run(scoresIntitules)
            interfaceTensorflow!!.fetch(NOM_SCORE, scores)

            tempsActuelMS = System.currentTimeMillis()
            resultat = reconnaissanceCommande!!.executionDerniersResultats(scores, tempsActuelMS)

            runOnUiThread(
                    {
                        //Traitement du resultat. Si le résultat est connu et est différent du _silence_
                        if(!resultat.commandeDetectee.startsWith("_") && resultat.nouvelleCommande) {
                            changeBgDirection(resultat.commandeDetectee)
                            tv_indiceConfiance.text = "%.2f".format(resultat.score * 100) + "%"
                        }
                    }
            )
            try {
                //Pause pour ne pas exécuter la reconnaissance en permanance.
                Thread.sleep(TEMPS_MINIMUM_ENTRE_ECHANTILLONS_MS)
            } catch (ie : InterruptedException) { Log.e("4Directions", "reconnaissance IE " + ie.message) }
        }

        Log.e("4Directions", "Fin de la reconnaissance")
    }

    /**
     * Change le background des flèches selon le mot clef reconnu (ou fait apparaitre le pouce baissé
     * si ce n'est pas une direction).
     * @param motClef {String} : Mot clef reconnu
     */
    private fun changeBgDirection(motClef : String) {
        resetAffichage()
        when(motClef) {
            "up" -> {
                img_up.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
            "down" -> {
                img_down.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
            "left" -> {
                img_left.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
            "right" -> {
                img_right.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
            else -> {
                img_nope.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Remet l'affichage dans son état initial
     */
    private fun resetAffichage() {
        img_up.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        img_down.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        img_left.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        img_right.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        img_nope.visibility = View.GONE
    }
}
