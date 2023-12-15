package com.pedmar.chatkotlin

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail : EditText
    private lateinit var btnSendEmail : MaterialButton
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog : ProgressDialog

    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)
        initializeViews()

        btnSendEmail.setOnClickListener {
            checkInformacion()
        }
    }


    private fun initializeViews(){
        etEmail = findViewById(R.id.L_Et_email)
        btnSendEmail = findViewById(R.id.Btn_send_email)
        firebaseAuth = FirebaseAuth.getInstance()

        //Configuramos el progressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun checkInformacion() {
        //Obtener el email
        email = etEmail.text.toString().trim()
        if (email.isEmpty()){
            Toast.makeText(applicationContext, "Enter your email", Toast.LENGTH_SHORT).show()
        }else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(applicationContext, "Invalid email", Toast.LENGTH_SHORT).show()
        }else{
            recoverPassword()
        }

    }

    private fun recoverPassword() {
        progressDialog.setMessage("Sending password reset instructions to email $email")
        progressDialog.show()

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                progressDialog.dismiss()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
            }
    }
















}