package com.pedmar.chatkotlin.group

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import java.io.ByteArrayOutputStream

class SelectDataGroup  : AppCompatActivity(){

    private lateinit var etName : EditText
    private lateinit var ivImage : ImageView
    private lateinit var btnSendEmail : MaterialButton
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog : ProgressDialog
    private lateinit var groupChat : GroupChat

    private var selectUsers : String? = null
    private var referenceStorage : StorageReference?=null
    private var firebaseUser : FirebaseUser?= null

    private val reference = FirebaseDatabase.getInstance().reference
    private val keyGroup = reference.push().key

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val REQUEST_DOCUMENT_PICK = 3
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseUser = FirebaseAuth.getInstance().currentUser

        setContentView(R.layout.activity_select_data_group)
        initializeViews()
        getSelectedUsers()

        btnSendEmail.setOnClickListener {
            checkInformation()
        }

        ivImage.setOnClickListener{
            showImageSelectionDialog()
        }
    }

    private fun initializeViews(){
        etName = findViewById(R.id.Et_name)
        btnSendEmail = findViewById(R.id.Btn_send_email)
        firebaseAuth = FirebaseAuth.getInstance()
        ivImage = findViewById(R.id.Iv_image)
        groupChat = GroupChat()

        //Configuramos el progressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        ivImage.setImageResource(R.drawable.imagen_usuario_visitado)
        selectUsers = intent.getStringExtra("selectedUsers")
        referenceStorage = FirebaseStorage.getInstance().getReference("Group/${keyGroup}.png")
    }


    private fun checkInformation() {
        //Obtener el email
        var name = etName.text.toString().trim()
        if (name.isEmpty()){
            Toast.makeText(applicationContext, "Enter a group name", Toast.LENGTH_SHORT).show()
        }else{
            // Llama al método específico de CreateGroupActivity
            groupChat.setUidGroup(keyGroup!!)
            groupChat.setName(name)
            ActivityManager.callCreateGroup(groupChat)
        }

    }
    private fun getSelectedUsers() {
        intent = intent

        if (null != selectUsers) {
            val userIds = selectUsers!!.split("-") // Separar los identificadores de usuario

            val reference = FirebaseDatabase.getInstance().reference.child("Users")
            val usersList: MutableList<String> = mutableListOf()

            for (userId in userIds) {
                reference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            usersList.add(it.getUid()!!)
                        }

                        // Verificar si se han recuperado todos los usuarios
                        if (usersList.size == userIds.size) {

                            // Todos los usuarios han sido recuperados
                            groupChat.setUidGroup(selectUsers!!)
                            groupChat.setUidUsersList(usersList)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
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
                startActivityForResult(takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
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
                    ivImage.setImageBitmap(imageBitmap)
                    updateByteArray(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    ivImage.setImageBitmap(imageBitmap)
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

        val pathImage = "Group/${keyGroup}.png"
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
                val downloadUrl = uri.toString()
                if (downloadUrl != null) {
                    groupChat.setImage(downloadUrl)
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
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