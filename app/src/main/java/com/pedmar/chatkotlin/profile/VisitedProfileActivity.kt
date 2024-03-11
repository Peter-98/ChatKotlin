package com.pedmar.chatkotlin.profile

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.pedmar.chatkotlin.MainActivity
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.User

class VisitedProfileActivity : AppCompatActivity() {

    private lateinit var pvUsername : TextView
    private lateinit var pvEmail :TextView
    private lateinit var pvNames  :TextView
    private lateinit var pvSurnames : TextView
    private lateinit var pvPhone : TextView
    private lateinit var pvAge : TextView
    private lateinit var pvProvider : TextView
    private lateinit var pvUserImage : ImageView
    private lateinit var backArrow : ImageView
    private lateinit var btnCall : Button
    var visitedUidUser = ""
    var firebaseUser : FirebaseUser ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visited_profile)
        initializeVariables()
        getUid()
        getUserData()

        btnCall.setOnClickListener{
            if(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                makeCall()
            }else{
                requestCallPhonePermission.launch(Manifest.permission.CALL_PHONE)
            }
        }

        pvUserImage.setOnClickListener{
            getImage()
        }

        backArrow.setOnClickListener(){
            finish()
        }
    }

    private fun getImage() {
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(visitedUidUser)

        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user : User?= snapshot.getValue(User::class.java)
                val image = user!!.getImage()
                displayImage(image)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun displayImage(image: String?) {
        if (!image.isNullOrEmpty()) { // Verificar si la URL de la imagen no es nula ni vacía
            val imgView: PhotoView
            val btnCloseV: Button
            val dialog = Dialog(this@VisitedProfileActivity)

            dialog.setContentView(R.layout.dialog_view_image)

            imgView = dialog.findViewById(R.id.imgView)
            btnCloseV = dialog.findViewById(R.id.Btn_close_w)

            Glide.with(applicationContext).load(image).placeholder(R.drawable.ic_send_image).into(imgView)

            btnCloseV.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dialog.setCanceledOnTouchOutside(false)
        } else {
            Toast.makeText(applicationContext, "The profile does not have any image",Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall() {
        val userPhoneNumber = pvPhone.text.toString()
        if (userPhoneNumber.isEmpty()){
            Toast.makeText(applicationContext, "The user does not have a phone number", Toast.LENGTH_SHORT).show()
        }else{
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$userPhoneNumber")
            startActivity(intent)
        }
    }

    private val requestCallPhonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                makeCall()
            }else{
                Toast.makeText(applicationContext, "Permission to make phone calls has not been granted",Toast.LENGTH_SHORT).show()
            }
        }

    private fun getUid() {
        intent = intent
        visitedUidUser = intent.getStringExtra("uid").toString()
    }


    private fun initializeVariables(){

        backArrow = findViewById(R.id.back_arrow)
        pvUsername = findViewById(R.id.PV_username)
        pvEmail = findViewById(R.id.PV_email)
        pvNames = findViewById(R.id.PV_name)
        pvSurnames = findViewById(R.id.PV_surnames)
        pvPhone = findViewById(R.id.PV_phone)
        pvAge = findViewById(R.id.PV_age)
        pvProvider = findViewById(R.id.PV_provider)
        pvUserImage = findViewById(R.id.PV_userImage)
        btnCall = findViewById(R.id.Btn_call)
        firebaseUser = FirebaseAuth.getInstance().currentUser
    }

    private fun getUserData(){
        val reference = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(visitedUidUser)

        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user : User? = snapshot.getValue(User::class.java)

                pvUsername.text = user!!.getUsername()
                pvEmail.text = user.getEmail()
                pvNames.text = user.getName()
                pvSurnames.text = user.getSurnames()
                pvAge.text = user.getAge()
                pvPhone.text = user.getPhone()
                pvProvider.text = user.getProvider()

                Glide.with(applicationContext).load(user.getImage()).placeholder(R.drawable.imagen_usuario_visitado)
                    .into(pvUserImage)
            }

            override fun onCancelled(error: DatabaseError) {
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}