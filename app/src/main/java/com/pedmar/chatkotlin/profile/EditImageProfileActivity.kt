package com.pedmar.chatkotlin.profile

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.pedmar.chatkotlin.R
import java.io.ByteArrayOutputStream

class EditImageProfileActivity : AppCompatActivity() {

    private lateinit var btnSelectImage: Button
    private lateinit var updateImageProfile: ImageView

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var progressDialog : ProgressDialog

    var firebaseUser : FirebaseUser ?= null

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image_profile)

        btnSelectImage = findViewById(R.id.BtnSelectImage)
        updateImageProfile = findViewById(R.id.UpdateImageProfile)

        progressDialog = ProgressDialog(this@EditImageProfileActivity)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = FirebaseAuth.getInstance().currentUser

        // Recibe el ByteArray de la actividad anterior
        val byteArray = intent.getByteArrayExtra("imageByteArray")

        // Convierte el ByteArray de nuevo a Bitmap
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            // Muestra el bitmap en la imagen
            updateImageProfile.setImageBitmap(bitmap)
        }

        btnSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }
    }

    private fun showImageSelectionDialog() {
        val items = arrayOf("Take Photo", "Choose from Gallery")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(items) { dialog, which ->
            when (which) {
                0 -> dispatchTakePictureIntent()
                1 -> dispatchPickImageIntent()
            }
        }
        builder.show()
    }

    private fun dispatchTakePictureIntent() {
        if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Camera Not Available", Toast.LENGTH_SHORT)
                    .show()
            }
        }else{
           requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }


    private fun dispatchPickImageIntent() {
        if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageIntent.type = "image/*"
            startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK)
        }else{
            requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    updateImageProfile.setImageBitmap(imageBitmap)
                    updateByteArray(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    updateImageProfile.setImageBitmap(imageBitmap)
                    updateByteArray(imageBitmap)
                }
            }
        }
    }

    private fun updateByteArray(bitmap: Bitmap) {
        // Convierte el Bitmap a ByteArray
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        val pathImage = "Profile/${firebaseAuth.uid}.png"
        val referenceStorage = FirebaseStorage.getInstance().getReference(pathImage)

        // Sube el ByteArray al almacenamiento de Firebase
        val uploadTask = referenceStorage.putBytes(byteArray)

        // Muestra un diálogo de progreso mientras se carga la imagen
        progressDialog.setMessage("Uploading Image...")
        progressDialog.show()

        // Maneja el resultado de la carga
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Cierre el diálogo de progreso
            progressDialog.dismiss()

            // Obtiene la URL de descarga de la imagen cargada
            referenceStorage.downloadUrl.addOnSuccessListener { uri ->
                // Aquí puedes hacer algo con la URL de descarga, como guardarla en la base de datos
                val downloadUrl = uri.toString()
                if(downloadUrl!=null){
                    val hashmap : HashMap<String, Any> = HashMap()
                    hashmap["image"] = downloadUrl
                    val reference = FirebaseDatabase.getInstance().getReference("Users")
                    reference.child(firebaseAuth.uid!!).updateChildren(hashmap).addOnSuccessListener {
                        Toast.makeText(applicationContext, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()

                        // Cierra la actividad actual y vuelve a la actividad anterior
                        finish()
                    }.addOnFailureListener{e->
                        progressDialog.dismiss()
                        // Muestra un mensaje de error al subir a la base de datos
                        Toast.makeText(applicationContext, "Error uploading image to database: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
            // Muestra un mensaje de error al subir a storage
            Toast.makeText(applicationContext, "Error uploading image to storage: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestGalleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchPickImageIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){permission_granted->
            if (permission_granted){
                dispatchTakePictureIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }

        }

    private fun updateStatus(status : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["status"] = status
        reference!!.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        updateStatus("online")
    }
    override fun onPause() {
        super.onPause()
        updateStatus("offline")
    }
}
