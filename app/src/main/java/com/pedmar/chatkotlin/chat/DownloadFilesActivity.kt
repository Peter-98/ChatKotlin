package com.pedmar.chatkotlin.chat

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.group.SelectDataGroup
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL

class DownloadFilesActivity  : AppCompatActivity(){

    private lateinit var uri : String
    private lateinit var name : String
    private var firebaseUser : FirebaseUser? = FirebaseAuth.getInstance().currentUser


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getFileData()
        dispatchExternalStorageIntent()
        finish()
    }

    private fun getFileData(){
        intent = intent
        uri = intent.getStringExtra("uri").toString()
        name = intent.getStringExtra("name").toString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun dispatchExternalStorageIntent() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED ) {
            //requestExternalStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            downloadFile(applicationContext, uri!!, name!!)
        } else {
            this.uri = uri
            this.name = name
            requestExternalStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadFile(context: Context, uri: String, name: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl(uri)

        // Define el contenido de los valores para insertar en MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream") // Cambia esto según el tipo MIME del archivo
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // Obtiene el resolver de contenido del contexto
        val resolver = context.contentResolver

        // Inserta el archivo en la carpeta de descargas utilizando MediaStore
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                storageRef.getFile(File.createTempFile(name, null))
                    .addOnSuccessListener {
                        Toast.makeText(context, "File successfully save in the downloads folder", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        // Se produjo un error al descargar el archivo
                        Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            // No se pudo obtener una URI válida, muestra un mensaje de error
            Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show()
        }
    }




    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchExternalStorageIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
                finish()
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