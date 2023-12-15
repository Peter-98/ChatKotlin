package com.pedmar.chatkotlin

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var L_Et_email : EditText
    private lateinit var L_Et_password : EditText
    private lateinit var Btn_login : Button
    private lateinit var auth : FirebaseAuth
    private lateinit var TXT_ir_sign_up : TextView
    private lateinit var progressDialog : ProgressDialog
    private lateinit var forgetPassword : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //supportActionBar!!.title = "Login"
        InicializarVariables()

        forgetPassword.setOnClickListener{
            startActivity(Intent(this@LoginActivity, ForgetPasswordActivity::class.java))
        }

        Btn_login.setOnClickListener{
            ValidarDatos()
        }

        TXT_ir_sign_up.setOnClickListener{
            val intent = Intent(this@LoginActivity, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun InicializarVariables(){
        L_Et_email = findViewById(R.id.L_Et_email)
        L_Et_password = findViewById(R.id.L_Et_password)
        Btn_login = findViewById(R.id.Btn_login)
        auth = FirebaseAuth.getInstance()
        TXT_ir_sign_up = findViewById(R.id.TXT_ir_sign_up)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Logging in...")
        progressDialog.setCanceledOnTouchOutside(false)
        forgetPassword = findViewById(R.id.L_forget_password)
    }

    private fun ValidarDatos() {
        val email: String = L_Et_email.text.toString()
        val password: String = L_Et_password.text.toString()

        if (email.isEmpty()) {
            Toast.makeText(applicationContext, "Email is empty", Toast.LENGTH_SHORT).show()
        }else if (password.isEmpty()) {
            Toast.makeText(applicationContext, "Password is empty", Toast.LENGTH_SHORT).show()
        }else{
            LoginUsuario(email, password)
        }

    }

    private fun LoginUsuario(email: String, password: String) {
        progressDialog.setMessage("Please wait")
        progressDialog.show()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{task ->
                if(task.isSuccessful){
                    progressDialog.dismiss()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    Toast.makeText(applicationContext, "You have logged in successfully", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                    finish()
                }else {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "An error has occurred", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {e ->
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "{$e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}