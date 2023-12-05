package com.pedmar.chatkotlin

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pedmar.chatkotlin.R.*

class Inicio : AppCompatActivity() {


    private lateinit var  Btn_ir_sign_in : Button
    private lateinit var  Btn_ir_login : Button


    var firebaseUser : FirebaseUser?= null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_inicio)

        Btn_ir_sign_in = findViewById(id.Btn_ir_sign_in)
        Btn_ir_login = findViewById(id.Btn_ir_login)

        Btn_ir_sign_in.setOnClickListener{
            val intent = Intent(this@Inicio, RegistroActivity::class.java)
            Toast.makeText(applicationContext, "Sign in", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }

        Btn_ir_login.setOnClickListener {
            val intent = Intent(this@Inicio, LoginActivity::class.java)
            Toast.makeText(applicationContext, "Log in", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
    }


    private fun ComprobarSesion(){
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if(firebaseUser!= null){
            val intent = Intent(this@Inicio, MainActivity::class.java)
            Toast.makeText(applicationContext, "The session is active", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish()
        }
    }

    override fun onStart(){
        ComprobarSesion()
        super.onStart()
    }
}