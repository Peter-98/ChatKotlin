package com.pedmar.chatkotlin

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RegisterActivity : AppCompatActivity() {

    private lateinit var R_Et_username: EditText
    private lateinit var R_Et_email: EditText
    private lateinit var R_Et_password: EditText
    private lateinit var R_Et_r_password: EditText
    private lateinit var Btn_sign_in: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        //supportActionBar!!.title = "Sign In"
        initializeVariables()

        Btn_sign_in.setOnClickListener {
            checkData()
        }
    }

    private fun initializeVariables() {
        R_Et_username = findViewById(R.id.R_Et_username)
        R_Et_email = findViewById(R.id.R_Et_email)
        R_Et_password = findViewById(R.id.R_Et_password)
        R_Et_r_password = findViewById(R.id.R_Et_r_password)
        Btn_sign_in = findViewById(R.id.Btn_sign_in)
        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Registering information")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun checkData() {
        val username: String = R_Et_username.text.toString()
        val email: String = R_Et_email.text.toString()
        val password: String = R_Et_password.text.toString()
        val rPassword: String = R_Et_r_password.text.toString()

        if (username.isEmpty()) {
            Toast.makeText(applicationContext, "Username is empty", Toast.LENGTH_SHORT).show()
        } else if (username.length < 5) {
            Toast.makeText(
                applicationContext,
                "Username must be at least 5 characters",
                Toast.LENGTH_SHORT
            ).show()
        } else if (email.isEmpty()) {
            Toast.makeText(applicationContext, "Email is empty", Toast.LENGTH_SHORT).show()
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(applicationContext, "Invalid email address", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            Toast.makeText(applicationContext, "Password is empty", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            Toast.makeText(
                applicationContext,
                "Password must be at least 6 characters",
                Toast.LENGTH_SHORT
            ).show()
        } else if (rPassword.isEmpty()) {
            Toast.makeText(applicationContext, "Repeat the password is empty", Toast.LENGTH_SHORT)
                .show()
        } else if (!password.equals(rPassword)) {
            Toast.makeText(applicationContext, "Passwords do not match", Toast.LENGTH_SHORT).show()
        } else {
            saveUser(email, password)
        }
    }


    @SuppressLint("HardwareIds")
    private fun saveUser(email: String, password: String) {
        progressDialog.setMessage("Please wait")
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    var uid: String = ""
                    uid = auth.currentUser!!.uid
                    reference = FirebaseDatabase.getInstance().reference.child("Users").child(uid)

                    //Ordenar los datos en un hashmap para guardarlos en firebase
                    val hashmap = HashMap<String, Any>()
                    val hUsername: String = R_Et_username.text.toString()
                    val hEmail: String = R_Et_email.text.toString()

                    var customToken = postCustomToken(uid)
                    if(customToken.isEmpty()){
                        customToken = "Error generating custom token"
                    }


                    hashmap["uid"] = uid
                    hashmap["username"] = hUsername
                    hashmap["email"] = hEmail
                    hashmap["image"] = ""
                    hashmap["search"] = hUsername.lowercase()

                    hashmap["name"] = ""
                    hashmap["surnames"] = ""
                    hashmap["phone"] = ""
                    hashmap["age"] = ""
                    hashmap["status"] = "offline"
                    hashmap["provider"] = "Email"
                    hashmap["private"] = true
                    hashmap["customToken"] = customToken
                    reference.updateChildren(hashmap)
                        .addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {

                                val deviceId: String =
                                    Settings.Secure.getString(
                                        applicationContext.contentResolver,
                                        Settings.Secure.ANDROID_ID
                                    )
                                val reference =
                                    FirebaseDatabase.getInstance().reference.child(
                                        "UsersDevice"
                                    ).child(deviceId)

                                val hashMap = java.util.HashMap<String, Any>()
                                hashMap["idDevice"] = deviceId
                                hashMap["userIdToken"] = customToken
                                hashMap["uid"] = uid
                                hashMap["enable"] = false
                                reference!!.updateChildren(hashMap)

                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                Toast.makeText(
                                    applicationContext,
                                    "Has been successfully registered",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(intent)
                                finish()
                            }

                        }.addOnFailureListener { e ->
                            Toast.makeText(applicationContext, "{$e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }

                } else {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "An error has occurred", Toast.LENGTH_SHORT)
                        .show()
                }

            }.addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "{$e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun postCustomToken(uid: String): String{
        val customClaims = CustomClaims("admin", "premium")

        val jsonBody = Json.encodeToString(customClaims)
        val jsonString = "{\"uid\":\"$uid\",\"customClaims\":$jsonBody}"

        val url = "http://localhost:3000/generate-token"
        var customToken = ""

        url.httpPost()
            .header("Content-Type" to "application/json")
            .body(jsonString)
            .response { _, _, result ->
                when (result) {
                    is Result.Success -> {
                        println("Token generado: ${result.value}")
                        customToken = result.value.toString()
                    }
                    is Result.Failure -> {
                        println("Error al generar el token: ${result.error}")
                    }
                }
            }
        return customToken
    }
    @Serializable
    data class CustomClaims(
        val role: String,
        val subscription: String
    )
}