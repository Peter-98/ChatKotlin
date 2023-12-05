package com.pedmar.chatkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegistroActivity : AppCompatActivity() {

    private lateinit var R_Et_username : EditText
    private lateinit var R_Et_email : EditText
    private lateinit var R_Et_password : EditText
    private lateinit var R_Et_r_password : EditText
    private lateinit var Btn_sign_in : Button

    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        supportActionBar!!.title = "Sign In"
        InicializarVariables()

        Btn_sign_in.setOnClickListener{
            ValidarDatos()
        }
    }

    private fun InicializarVariables(){
        R_Et_username = findViewById(R.id.R_Et_username)
        R_Et_email = findViewById(R.id.R_Et_email)
        R_Et_password = findViewById(R.id.R_Et_password)
        R_Et_r_password = findViewById(R.id.R_Et_r_password)
        Btn_sign_in = findViewById(R.id.Btn_sign_in)
        auth = FirebaseAuth.getInstance()
    }

    private fun ValidarDatos() {
        val username: String = R_Et_username.text.toString()
        val email: String = R_Et_email.text.toString()
        val password: String = R_Et_password.text.toString()
        val rPassword: String = R_Et_r_password.text.toString()

        if (username.isEmpty()) {
            Toast.makeText(applicationContext, "Username is empty", Toast.LENGTH_SHORT).show()
        } else if (username.length < 5) {
            Toast.makeText(applicationContext, "Username must be at least 5 characters", Toast.LENGTH_SHORT).show()
        } else if (email.isEmpty()) {
            Toast.makeText(applicationContext, "Email is empty", Toast.LENGTH_SHORT).show()
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(applicationContext, "Invalid email address", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            Toast.makeText(applicationContext, "Password is empty", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            Toast.makeText(applicationContext, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
        } else if (rPassword.isEmpty()) {
            Toast.makeText(applicationContext, "Repeat the password is empty", Toast.LENGTH_SHORT).show()
        } else if (!password.equals(rPassword)) {
            Toast.makeText(applicationContext, "Passwords do not match", Toast.LENGTH_SHORT).show()
        } else {
            RegistrarUsuario(email, password)
        }
    }


    private fun RegistrarUsuario(email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{task ->
                if(task.isSuccessful){
                    var uid : String = ""
                    uid = auth.currentUser!!.uid
                    reference = FirebaseDatabase.getInstance().reference.child("Users").child(uid)

                    //Ordenar los datos en un hashmap para guardarlos en firebase
                    val hashmap = HashMap<String, Any>()
                    val hUsername : String = R_Et_username.text.toString()
                    val hEmail : String = R_Et_email.text.toString()

                    hashmap["uid"] = uid
                    hashmap["username"] = hUsername
                    hashmap["email"] = hEmail
                    hashmap["image"] = ""
                    hashmap["search"] = hUsername.lowercase()

                    reference.updateChildren(hashmap)
                        .addOnCompleteListener{task2 ->
                            if(task2.isSuccessful){
                                val intent = Intent(this@RegistroActivity, MainActivity::class.java)
                                Toast.makeText(applicationContext, "Has been successfully registered", Toast.LENGTH_SHORT).show()
                                startActivity(intent)
                            }

                    }.addOnFailureListener{e->
                            Toast.makeText(applicationContext, "{$e.message}", Toast.LENGTH_SHORT).show()
                        }

                }else{
                    Toast.makeText(applicationContext, "An error has occurred", Toast.LENGTH_SHORT).show()
                }

        }.addOnFailureListener{e->
                Toast.makeText(applicationContext, "{$e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}