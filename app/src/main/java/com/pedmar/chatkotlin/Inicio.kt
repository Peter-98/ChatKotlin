package com.pedmar.chatkotlin

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.pedmar.chatkotlin.R.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import java.nio.file.Paths
import java.util.*

class Inicio : AppCompatActivity() {

    private lateinit var  Btn_ir_login : MaterialButton
    private lateinit var  Btn_ir_login_google : MaterialButton


    var firebaseUser : FirebaseUser?= null
    private lateinit var auth : FirebaseAuth
    private lateinit var progressDialog : ProgressDialog
    private lateinit var mGoogleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_inicio)

        Btn_ir_login = findViewById(id.Btn_ir_login)
        Btn_ir_login_google = findViewById(id.Btn_ir_login_google)
        auth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        Btn_ir_login.setOnClickListener {
            val intent = Intent(this@Inicio, LoginActivity::class.java)
            Toast.makeText(applicationContext, "Log in", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }

        Btn_ir_login_google.setOnClickListener {
            startLogInGoogle()

        }
    }

    /* Recibe respuesta de google*/
    private fun startLogInGoogle() {
        val googleSignIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSignIntent)
    }



    /* Comprobar si ha sido seleccionado o cancelado la llamada a google*/
    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode == RESULT_OK){
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try{
                val account = task.getResult(ApiException::class.java)
                checkGoogleFirebase(account.idToken)
            }catch(e : Exception){
                Toast.makeText(applicationContext,"An exception has occurred due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(applicationContext,"Cancelled", Toast.LENGTH_SHORT).show()
        }

    }

    private fun checkGoogleFirebase(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult->
                if (authResult.additionalUserInfo!!.isNewUser){
                    /* Si el usuario es nuevo */
                    saveInfoBD()

                }else{

                    /* Si el usuario ya se registro previamente */
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }.addOnFailureListener{e->
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveInfoBD() {
        progressDialog.setMessage("Your information is being saved...")
        progressDialog.show()

        /*Obtener informacion de la cuenta de google*/
        val uidGoogle = auth.uid
        val emailGoogle = auth.currentUser?.email
        val nGoogle = auth.currentUser?.displayName
        val usernameGoogle = nGoogle.toString()

        val hashmap = HashMap<String, Any?>()

        hashmap["uid"] = uidGoogle
        hashmap["username"] = usernameGoogle
        hashmap["email"] = emailGoogle
        hashmap["image"] = ""
        hashmap["search"] = usernameGoogle.lowercase()

        hashmap["name"] = ""
        hashmap["surnames"] = ""
        hashmap["phone"] = ""
        hashmap["age"] = ""
        hashmap["status"] = "offline"
        hashmap["provider"] = "Google"

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(uidGoogle!!)
            .setValue(hashmap).addOnSuccessListener {
            progressDialog.dismiss()
            startActivity(Intent(applicationContext, MainActivity::class.java))
            Toast.makeText(applicationContext ,"Has been successfully registered", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }.addOnFailureListener{e->
            progressDialog.dismiss()
            Toast.makeText(applicationContext ,"${e.message}", Toast.LENGTH_SHORT).show()
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