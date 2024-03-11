package com.pedmar.chatkotlin.group

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import java.io.ByteArrayOutputStream

class VisitedGroupActivity : AppCompatActivity() {

    private lateinit var gvGroupName : TextView
    private lateinit var gvGroupImage : ImageView
    private lateinit var backArrow : ImageView
    private var gvUsers : RecyclerView?=null
    private var uidGroup = ""
    private var firebaseUser : FirebaseUser ?= null
    private var userList : List<User>?=null
    private var userAdapter : UserAdapter?=null
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visited_group)
        initializeVariables()
        getUidGroup()

        gvUsers = findViewById(R.id.GV_users)
        gvUsers!!.setHasFixedSize(true)
        gvUsers!!.layoutManager = LinearLayoutManager(this)

        val context = this
        getGroupData(context)


        backArrow.setOnClickListener(){
            finish()
        }


        gvGroupImage.setOnClickListener{
                val options = arrayOf<CharSequence>("View image","Take Photo", "Choose from Gallery")
                val builder : AlertDialog.Builder = AlertDialog.Builder(gvGroupImage.context)
                //builder.setTitle("")
                builder.setItems(options, DialogInterface.OnClickListener {
                        dialogInterface, i ->
                    if (i==0){
                        getImage()
                    }else if (i==1){
                        dispatchTakePictureIntent()
                    }else if(i==2){
                        dispatchPickImageIntent()
                    }
                })
                builder.show()
        }
    }

    private fun getImage() {
        val reference = FirebaseDatabase.getInstance().reference.child("Groups").child(uidGroup)

        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat : GroupChat?= snapshot.getValue(GroupChat::class.java)
                val image = groupChat!!.getImage()
                displayImage(image)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun displayImage(image: String?) {
        val imgView : PhotoView
        val btnCloseV : Button
        val dialog = Dialog(this@VisitedGroupActivity)

        dialog.setContentView(R.layout.dialog_view_image)

        imgView = dialog.findViewById(R.id.imgView)
        btnCloseV = dialog.findViewById(R.id.Btn_close_w)

        Glide.with(applicationContext).load(image).placeholder(R.drawable.ic_send_image).into(imgView)

        btnCloseV.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun getUidGroup() {
        intent = intent
        uidGroup = intent.getStringExtra("uidGroup").toString()
    }


    private fun initializeVariables(){

        backArrow = findViewById(R.id.back_arrow)
        gvGroupName = findViewById(R.id.GV_groupName)
        gvGroupImage = findViewById(R.id.GV_groupImage)
        firebaseUser = FirebaseAuth.getInstance().currentUser

        //Configuramos el progressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun getGroupData(context: Context?){
        userList = ArrayList()
        val groupReference = FirebaseDatabase.getInstance().reference.child("Groups").child(uidGroup)
        groupReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat = snapshot.getValue(GroupChat::class.java)
                if (groupChat != null) {

                    gvGroupName.text = groupChat.getName()

                    Glide.with(applicationContext).load(groupChat.getImage()).placeholder(R.drawable.imagen_usuario_visitado)
                        .into(gvGroupImage)
                    (userList as ArrayList<User>).clear()
                    val userReferences = groupChat.getUidUsersList()?.map {
                        FirebaseDatabase.getInstance().reference.child("Users").child(it)
                    }
                    userReferences?.forEach { userReference ->
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user : User? = userSnapshot.getValue(User::class.java)
                                (userList as ArrayList<User>).add(user!!)

                                // Verificar si todas las consultas de usuario han finalizado
                                if ((userList as ArrayList<User>).size == groupChat.getUidUsersList()?.size) {
                                    //Pasar la lista al adaptador
                                    userAdapter = UserAdapter(
                                        context!!,
                                        userList!!,
                                        false,
                                        false,
                                        true,
                                        null
                                    )
                                    //Seteamos el adaptador al recycleView
                                    gvUsers!!.adapter = userAdapter
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Manejar errores de cancelación de la consulta
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de cancelación de la consulta
            }
        })
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


    private fun dispatchTakePictureIntent() {
        if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent,
                    SelectDataGroup.REQUEST_IMAGE_CAPTURE
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
            startActivityForResult(pickImageIntent, SelectDataGroup.REQUEST_IMAGE_PICK)
        }else{
            requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SelectDataGroup.REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    gvGroupImage.setImageBitmap(imageBitmap)
                    updateByteArray(imageBitmap)
                }
                SelectDataGroup.REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    gvGroupImage.setImageBitmap(imageBitmap)
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

        val pathImage = "Group/${uidGroup}.png"
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
                    val hashmap : HashMap<String, Any> = HashMap()
                    hashmap["image"] = downloadUrl
                    val referenceGroup = FirebaseDatabase.getInstance().getReference("Groups")
                    referenceGroup.child(uidGroup).updateChildren(hashmap).addOnSuccessListener {
                        Toast.makeText(applicationContext, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchTakePictureIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }

        }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}