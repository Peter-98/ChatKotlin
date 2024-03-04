package com.pedmar.chatkotlin

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pedmar.chatkotlin.model.UsersDevice
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var eTPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnLoginGoogle: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var forgetPassword: TextView
    private lateinit var secretKey: String
    private lateinit var qrCodeImageLogin: ImageView

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val qrCodeWidthPixels = 750

    @SuppressLint("HardwareIds")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initializeVariables()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)


        val deviceId: String = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        qrCodeImageLogin.setImageBitmap(generateQRCode(deviceId))
        forgetPassword.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgetPasswordActivity::class.java))
        }

        btnLogin.setOnClickListener {
            checkData()
        }

        btnLoginGoogle.setOnClickListener {
            startLogInGoogle()
        }


        // Establece un temporizador para verificar la base de datos cada x segundos
        val handler = Handler(Looper.getMainLooper())
        val delay = 5000L // Verificar cada 5 segundos (5000 milisegundos)
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkDatabase(deviceId)
                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    private fun initializeVariables() {
        etEmail = findViewById(R.id.L_Et_email)
        eTPassword = findViewById(R.id.L_Et_password)
        btnLogin = findViewById(R.id.Btn_login)
        btnLoginGoogle = findViewById(R.id.Btn_login_google)
        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Logging in...")
        progressDialog.setCanceledOnTouchOutside(false)
        forgetPassword = findViewById(R.id.L_forget_password)
        secretKey = loadSecretKey()
        qrCodeImageLogin = findViewById(R.id.qrCodeImageLogin)
    }

    private fun checkData() {
        val email: String = etEmail.text.toString()
        val password: String = eTPassword.text.toString()

        if (email.isEmpty()) {
            Toast.makeText(applicationContext, "Email is empty", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            Toast.makeText(applicationContext, "Password is empty", Toast.LENGTH_SHORT).show()
        } else {
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        progressDialog.setMessage("Please wait")
        progressDialog.show()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    Toast.makeText(
                        applicationContext,
                        "You have logged in successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(intent)
                    finish()
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


    /* Recibe respuesta de google*/
    private fun startLogInGoogle() {
        val googleSignIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSignIntent)
    }



    /* Comprobar si ha sido seleccionada o cancelada la llamada a google*/
    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result->
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
                // Finalizar la actividad actual para que no aparezca en la pila de actividades
                finish()
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

    fun checkDatabase(idDevice: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("UsersDevice")
            .child(idDevice)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usersDevice: UsersDevice? = dataSnapshot.getValue(UsersDevice::class.java)
                if (usersDevice != null && usersDevice.isEnable()) {
                    loginWithToken(
                        usersDevice.getUserIdToken(),
                        usersDevice.getUid(),
                        usersDevice.getIdDevice()
                    )
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        reference.addListenerForSingleValueEvent(valueEventListener)
    }

    private fun loginWithToken(token: String, uid: String, deviceId: String) {
        progressDialog.setMessage("Please wait")
        progressDialog.show()
        FirebaseAuth.getInstance().signInWithCustomToken(token)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // El usuario ha iniciado sesión con éxito
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        progressDialog.dismiss()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        Toast.makeText(
                            applicationContext,
                            "You have logged in successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(intent)
                        finish()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            applicationContext,
                            "Could not get current user after login",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    progressDialog.dismiss()
                    val exception = task.exception
                    Toast.makeText(
                        applicationContext,
                        "Failed to login with token: $exception",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val reference =
            FirebaseDatabase.getInstance().reference.child("UsersDevice").child(deviceId)

        val hashMap = HashMap<String, Any>()
        hashMap["idDevice"] = deviceId
        hashMap["uid"] = uid
        hashMap["enable"] = false
        reference!!.updateChildren(hashMap)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateQRCode(deviceId: String): Bitmap? {

        val encodedDeviceId = encrypt(
            "AES/CBC/PKCS5Padding",
            deviceId,
            SecretKeySpec(secretKey.toByteArray(), "AES"),
            IvParameterSpec(ByteArray(16))
        )

        val bitMatrix: BitMatrix = try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            MultiFormatWriter().encode(
                "login: $encodedDeviceId",
                BarcodeFormat.QR_CODE,
                qrCodeWidthPixels,
                qrCodeWidthPixels,
                hints
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.createBitmap(bitMatrix)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun encrypt(
        algorithm: String,
        inputText: String,
        key: SecretKeySpec,
        iv: IvParameterSpec
    ): String {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText = cipher.doFinal(inputText.toByteArray())
        return Base64.getEncoder().encodeToString(cipherText)
    }

    private fun loadSecretKey(): String {
        val properties = Properties()
        val assetManager = applicationContext.assets
        val inputStream = assetManager.open("configs/config.properties")
        properties.load(inputStream)
        return properties.getProperty("secretKey")
    }
}