package com.maximeferrierdev.cueillettechampignons

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import com.maximeferrierdev.cueillettechampignons.Tensorflow.Classifier
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import com.maximeferrierdev.cueillettechampignons.Model.ResultatClassificationImage
import com.maximeferrierdev.cueillettechampignons.Tensorflow.ImageClassifierFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val handler = Handler()
    private lateinit var classifier: Classifier
    private var photoPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Gestion des permissions

    /**
     * Lance l'appareil photo si l'application a le droit d'accéder au stockage. S'il n'en a pas les droits, les demande
     */
    private fun checkPermissions() {
        if (checkToutesPermissionsNecessairesDejaAccordees()) {
            init()
        } else {
            demandePermissions()
        }
    }

    /**
     * Vérifie que l'application a déjà le droit d'accéder au stockage
     * @return {Boolean} : true si les permissions sont accordées, false sinon
     */
    private fun checkToutesPermissionsNecessairesDejaAccordees() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
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
     * Demande la permission d'accéder au stockage
     */
    private fun demandePermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                DEMANDE_PERMISSIONS)
    }

    /**
     * Catch le retour de demande de permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == DEMANDE_PERMISSIONS && checkToutesPermissionsAccordees(grantResults)) {
            init()
        } else {
            demandePermissions()
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Prise de photo

    private fun init() {
        creationClassifier()
        prisePhoto()
    }

    /**
     * Prend une photo au format jpg
     */
    private fun prisePhoto() {
        photoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/${System.currentTimeMillis()}.jpg"
        val photoUri = extractionUriDepuisPathFichier(this, photoPath)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, DEMANDE_PRISE_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val fichier = File(photoPath)
        if (requestCode == DEMANDE_PRISE_PHOTO && fichier.exists()) {
            classifierImage(fichier)
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Classification

    private fun creationClassifier() {
        classifier = ImageClassifierFactory.create(
                assets,
                PATH_GRAPHDB,
                PATH_LABELS,
                TAILLE_IMAGE,
                NOM_ENTREE,
                NOM_SORTIE
        )
    }

    /**
     * Classifie la photo grâce au modèle pré-entraîné et affiche le résultat
     * @param fichier {File} : photo
     */
    private fun classifierImage(fichier: File) {
        val photoBitmap = BitmapFactory.decodeFile(fichier.absolutePath)
        val croppedBitmap = getCroppedBitmap(photoBitmap)
        classifierEtAfficherResultat(croppedBitmap)
        img_photo.setImageBitmap(photoBitmap)
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Afficher les résultats

    private fun classifierEtAfficherResultat(imageCoupee: Bitmap) {
        runInBackground(
                Runnable {
                    afficheResultats(classifier.reconnaissanceImage(imageCoupee))
                })
    }

    /**
     * Affiche le type de champignon, son nom et le % de certitude à l'écran. Change la couleur de fond
     * en fonction du résultat (Commestible ou non)
     * @param resultatClassificationImage {ResultatClassificationImage} : Le résultat de la classification
     */
    private fun afficheResultats(resultatClassificationImage: ResultatClassificationImage) {
        var nomChampignon : String
        var indiceConfiance : Double = resultatClassificationImage.indiceConfiance * 100.0
        if (resultatClassificationImage.categorie.contains("commestible", true)) {
            txt_resultat.text = getString(R.string.commestible)
            layoutContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.commestible))
            nomChampignon = resultatClassificationImage.categorie.substring(TAILLE_MOT_COMMESTIBLE)
        } else {
            txt_resultat.text = getString(R.string.veneneu)
            layoutContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.veneneu))
            nomChampignon = resultatClassificationImage.categorie.substring(TAILLE_MOT_VENENEU)
        }

        tv_nomChamignon.text = getString(R.string.champignon) + " : " + nomChampignon
        tv_indiceConfiance.text = getString(R.string.indiceConfiance) + " : %.2f".format(indiceConfiance) + "%"
    }

    @Synchronized
    private fun runInBackground(runnable: Runnable) {
        handler.post(runnable)
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    //Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.take_photo) {
            prisePhoto()
            true
        } else {
            super.onOptionsItemSelected(menuItem)
        }
    }

}
