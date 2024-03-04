package com.pedmar.chatkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pedmar.chatkotlin.R.*

class Inicio : AppCompatActivity() {

    private lateinit var  btnIrLogin : MaterialButton
    private lateinit var  btnSignUp : MaterialButton

    var firebaseUser : FirebaseUser?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_inicio)

        btnIrLogin = findViewById(id.Btn_ir_login)
        btnSignUp = findViewById(id.Btn_sign_up)

        btnIrLogin.setOnClickListener {
            val intent = Intent(this@Inicio, LoginActivity::class.java)
            Toast.makeText(applicationContext, "Log in", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }

        btnSignUp.setOnClickListener {
            val intent = Intent(this@Inicio, RegisterActivity::class.java)
            startActivity(intent)
        }


    }


    private fun checkSession(){
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if(firebaseUser!= null){
            val intent = Intent(this@Inicio, MainActivity::class.java)
            Toast.makeText(applicationContext, "The session is active", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish()
        }
    }

    override fun onStart(){
        checkSession()
        super.onStart()
    }
}