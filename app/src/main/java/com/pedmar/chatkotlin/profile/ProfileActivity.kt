package com.pedmar.chatkotlin.profile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.hbb20.CountryCodePicker
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.User
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var image : ImageView
    private lateinit var username : TextView
    private lateinit var email : TextView
    private lateinit var name : EditText
    private lateinit var surnames : EditText
    private lateinit var age : EditText
    private lateinit var location : EditText
    private lateinit var phone : TextView
    private lateinit var btnSave : Button
    private lateinit var provider : TextView
    private lateinit var editPhone : ImageView
    private lateinit var btnVerify : MaterialButton
    private lateinit var checkBoxPrivate: CheckBox
    private lateinit var backArrow : ImageView

    private var user : FirebaseUser?= null
    private var reference : DatabaseReference?=null
    private var userData : User?= null

    private var phoneCode = ""
    private var phoneNumber = ""
    private var phoneCodeNumber = ""

    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth : FirebaseAuth

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initializeVariables()
        getData()
        statusVerifyAccount()

        btnSave.setOnClickListener{
            if(checkData()){
                updateData()
            }else{
                Toast.makeText(applicationContext,"No data has changed",Toast.LENGTH_SHORT).show()
            }
        }

        backArrow.setOnClickListener(){
            finish()
        }

        image.setOnClickListener{
            showImageSelectionDialog()
        }

        editPhone.setOnClickListener{
            setNumberPhone()
        }

        btnVerify.setOnClickListener{
            if(user!!.isEmailVerified){
                //Usuario verificado
                //Toast.makeText(applicationContext, "User verified", Toast.LENGTH_SHORT).show()
                verifyAccount()
            }else{
                //Usuario no verificado
                confirmVerify()
            }
        }
    }

    private fun checkData(): Boolean {
        return !(userData!!.getName().equals(name.text.toString()) &&
            userData!!.getSurnames().equals(surnames.text.toString()) &&
            userData!!.getAge().equals(age.text.toString()) &&
            userData!!.getPhone().equals(phone.text.toString()) &&
            userData!!.getLocation().equals(location.text.toString()) &&
            userData!!.isPrivate().equals(checkBoxPrivate.isChecked))
    }

    private fun confirmVerify() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify account")
            .setMessage("Send verification instructions to ${user!!.email} ?")
            .setPositiveButton("Send"){d,e->
                sendConfirmationEmail()
            }.setNegativeButton("Cancel"){d,e->
                d.dismiss()
            }.show()
    }

    private fun sendConfirmationEmail() {
        progressDialog.setMessage("Sending verification instructions to email  ${user!!.email}...")
        progressDialog.show()

        user!!.sendEmailVerification().addOnSuccessListener {
            //Envio existoso
            progressDialog.dismiss()
            Toast.makeText(applicationContext,"Instructions sent. Check the email ${user!!.email}",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {e->
            //Envio no exitoso
            progressDialog.dismiss()
            Toast.makeText(applicationContext,"The operation failed due to ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun statusVerifyAccount(){
        if(user!!.isEmailVerified){
            btnVerify.text = "Verified"
        }else{
            btnVerify.text = "Not verified"
        }

    }

    private fun setNumberPhone() {

        /*Declarar vistas del cuadro de dialogo*/
        val setPhoneNumber : EditText
        val setPhoneCode : CountryCodePicker
        val btnAccept : MaterialButton

        val dialog = Dialog(this@ProfileActivity)

        /*Realizar conexion con el diseño*/
        dialog.setContentView(R.layout.dialog_set_phone)

        /*Inicializar las vistas*/
        setPhoneNumber = dialog.findViewById(R.id.setPhone)
        setPhoneCode = dialog.findViewById(R.id.codeSelect)
        btnAccept = dialog.findViewById(R.id.Btn_accept_phone)

        /*Asignar el evento al boton*/
        btnAccept.setOnClickListener{
            phoneCode = setPhoneCode.selectedCountryCodeWithPlus
            phoneNumber = setPhoneNumber.text.toString().trim()
            phoneCodeNumber = phoneCode + phoneNumber
            if(phoneNumber.isEmpty()){
                Toast.makeText(applicationContext, "Enter a phone number",Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }else{
                phone.text =  phoneCodeNumber
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun initializeVariables(){
        backArrow = findViewById(R.id.back_arrow)
        image = findViewById(R.id.P_image)
        username = findViewById(R.id.P_username)
        email = findViewById(R.id.P_email)
        name = findViewById(R.id.P_name)
        surnames = findViewById(R.id.P_surnames)
        location = findViewById(R.id.P_location)
        age = findViewById(R.id.P_age)
        phone = findViewById(R.id.P_phone)
        btnSave = findViewById(R.id.Btn_save)
        provider = findViewById(R.id.P_provider)
        editPhone = findViewById(R.id.edit_phone)
        btnVerify = findViewById(R.id.Btn_verify)
        checkBoxPrivate = findViewById(R.id.P_private)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        user = FirebaseAuth.getInstance().currentUser
        firebaseAuth = FirebaseAuth.getInstance()
        reference = FirebaseDatabase.getInstance().reference.child("Users").child(user!!.uid)
    }

    private fun getData(){
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){

                    //Obtener datos de firebase y seteamos
                    val user : User?= snapshot.getValue(User::class.java)

                    userData = user
                    location.setText(user!!.getLocation())
                    username.text = user!!.getUsername()
                    email.text = user!!.getEmail()
                    name.setText(user!!.getName())
                    surnames.setText(user!!.getSurnames())
                    age.setText(user!!.getAge())
                    phone.setText(user!!.getPhone())
                    provider.text = user!!.getProvider()
                    checkBoxPrivate.isChecked = user!!.isPrivate()
                    Glide.with(applicationContext).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(image)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun updateData(){

        val hashmap = HashMap<String, Any>()
        hashmap["location"] = location.text.toString()
        hashmap["name"] = name.text.toString()
        hashmap["surnames"] = surnames.text.toString()
        hashmap["age"] = age.text.toString()
        hashmap["phone"] = phone.text.toString()
        hashmap["private"] = checkBoxPrivate.isChecked

        reference!!.updateChildren(hashmap).addOnCompleteListener{task->
            if (task.isSuccessful){
                Toast.makeText(applicationContext, "Data updated successfully", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(applicationContext, "Error updating data", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{e->
            Toast.makeText(applicationContext, "An error has occurred ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyAccount(){
        val btnVerifiedAccount : MaterialButton
        val dialog = Dialog(this@ProfileActivity)

        dialog.setContentView(R.layout.dialog_verify_account)
        btnVerifiedAccount = dialog.findViewById(R.id.Btn_verified_account)
        btnVerifiedAccount.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun updateStatus(status : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(user!!.uid)
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
                    //updateImageProfile.setImageBitmap(imageBitmap)
                    updateByteArray(scaleBitmapToFit(imageBitmap, 300, 300))
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    //updateImageProfile.setImageBitmap(imageBitmap)
                    updateByteArray(scaleBitmapToFit(imageBitmap, 300, 300))
                }
            }
        }
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        return if (scaleFactor < 1) {
            val scaledWidth = (width * scaleFactor).toInt()
            val scaledHeight = (height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
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
}