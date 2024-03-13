package com.pedmar.chatkotlin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pedmar.chatkotlin.chat.MessageActivity
import com.pedmar.chatkotlin.group.ActivityManager
import com.pedmar.chatkotlin.model.User
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class QrCodeActivity : AppCompatActivity() {

    private lateinit var generateButton: Button
    private lateinit var scanButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var urlQr: TextView

    private val qrCodeWidthPixels = 750

    private lateinit var userData: User

    private var reference: DatabaseReference? = null
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        initializeVariables()
        getData()

        // Registrar esta actividad con el Singleton
        ActivityManager.setQrCodeActivity(this)

        Glide.with(applicationContext).load(R.drawable.example_qr_code).into(qrCodeImageView)

        generateButton.setOnClickListener {
            val inputData =
                "John Doe\nCEO\nAcme Corporation\njohndoe@example.com" // Business card data

            val qrCodeBitmap = generateQRCode(userData.getUid()!!)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)

            //val uniqueUrl = generateUniqueUrlFromData(jsonData)
            //println("URL única generada: $uniqueUrl")
            //urlQr.text = uniqueUrl
        }

        scanButton.setOnClickListener {
            startQRCodeScanner()
        }
    }

    fun copyTextToClipboard(view: View) {
        val textView = view as TextView
        val text = textView.text.toString()

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Text", text)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun initializeVariables() {

        var toolbar: Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        // Habilitar la flecha de retroceso
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        }

        reference = FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        generateButton = findViewById(R.id.generateButton)
        scanButton = findViewById(R.id.scanButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        urlQr = findViewById(R.id.urlQr)
    }

    private fun getData() {
        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user: User? = snapshot.getValue(User::class.java)
                    userData = user!!
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun generateQRCode(userDataUid: String): Bitmap? {

        var qrDataShare = "${userDataUid}_${System.currentTimeMillis()}"

        val bitMatrix: BitMatrix = try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            MultiFormatWriter().encode(
                qrDataShare,
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
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)

        saveQrMark(qrDataShare)
        return bitmap
    }

    private fun saveQrMark(data: String) {
        reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null) {
                    val hashMap = HashMap<String, Any>()
                    hashMap["qrDataShare"] = data
                    dataSnapshot.ref.updateChildren(hashMap)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun saveQrDataUser(qrDataShare: String) {

        reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userPrimary = dataSnapshot.getValue(User::class.java)
                val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(
                    qrDataShare.split("_")[0]
                )
                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val userSecondary = userSnapshot.getValue(User::class.java)

                        if (userPrimary != null && userSecondary != null &&
                            !userPrimary.getKnownPrivateUsers().contains(userSecondary.getUid())
                            && qrDataShare == userSecondary.getQrDataShare()!!
                        ) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["knownPrivateUsers"] =
                                userPrimary.getKnownPrivateUsers() + qrDataShare
                                    .split("_")[0]
                            dataSnapshot.ref.updateChildren(hashMap)

                            Toast.makeText(
                                applicationContext,
                                "User ${userSecondary.getUsername()} added",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@QrCodeActivity, MessageActivity::class.java)
                            intent.putExtra("userUid", qrDataShare!!.split("_")[0])
                            this@QrCodeActivity.startActivity(intent)
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Previously added user",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun generateUniqueUrlFromData(qrData: String): String {
        // Obtener el objeto MessageDigest para SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        // Calcular el hash de los datos del QR
        val hashBytes = digest.digest(qrData.toByteArray())
        // Convertir el hash a una cadena hexadecimal
        val hexString = StringBuilder()
        for (byte in hashBytes) {
            val hex = Integer.toHexString(0xff and byte.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        // Devolver la URL única basada en el hash
        return "miapp://${userData.getUsername()}/${hexString.toString()}"
    }

    fun getQrDataFromUniqueUrl(url: String) {

        if (!url.startsWith("miapp://")) {
            val dataString = url.substring("miapp://".length)
            val parts = dataString.split("/")

            if (parts.size >= 2) {
                val hexHash = parts.last()
                val bytes = ByteArray(hexHash.length / 2)
                for (i in 0 until hexHash.length step 2) {
                    bytes[i / 2] = hexHash.substring(i, i + 2).toInt(16).toByte()
                }
                saveQrDataUser(String(bytes))
            }
        }
    }


    private fun startQRCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                {}
                if (result.contents.contains("login: ")) {
                    loginWithQr(result.contents.split("login: ")[1])
                } else {
                    saveQrDataUser(result.contents)
                }

            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loginWithQr(deviceIdEncrypted: String) {

        val decodeDeviceId = decrypt(
            "AES/CBC/PKCS5Padding",
            deviceIdEncrypted,
            SecretKeySpec(loadSecretKey().toByteArray(), "AES"),
            IvParameterSpec(ByteArray(16))
        )


        FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user: User? = snapshot.getValue(User::class.java)
                        if(user != null){
                            val reference =
                                FirebaseDatabase.getInstance().reference.child("UsersDevice").child(decodeDeviceId)

                            val hashMap = HashMap<String, Any>()
                            hashMap["idDevice"] = decodeDeviceId
                            hashMap["uid"] = firebaseUser!!.uid
                            hashMap["enable"] = true
                            hashMap["userIdToken"] = user.getUserIdToken()
                            reference!!.updateChildren(hashMap)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })



    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun decrypt(
        algorithm: String,
        cipherText: String,
        key: SecretKeySpec,
        iv: IvParameterSpec
    ): String {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText))
        return String(plainText)
    }

    private fun updateStatus(status: String) {
        val reference =
            FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
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

    private fun loadSecretKey(): String {
        val properties = Properties()
        val assetManager = applicationContext.assets
        val inputStream = assetManager.open("configs/config.properties")
        properties.load(inputStream)
        return properties.getProperty("secretKey")
    }


}