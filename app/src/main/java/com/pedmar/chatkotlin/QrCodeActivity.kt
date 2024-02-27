package com.pedmar.chatkotlin

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pedmar.chatkotlin.adapter.CardAdapter
import com.pedmar.chatkotlin.chat.MessageActivity
import com.pedmar.chatkotlin.model.QrData
import com.pedmar.chatkotlin.model.User
import java.security.MessageDigest
import java.util.*

class QrCodeActivity : AppCompatActivity() {

    private lateinit var generateButton: Button
    private lateinit var scanButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var cardList: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private var scannedCards: MutableList<String> = mutableListOf()

    private val qrCodeWidthPixels = 750

    private lateinit var userData: User
    private var share: Boolean = true
    private lateinit var jsonData: String

    private var reference: DatabaseReference? = null
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        initializeVariables()
        isShare()
        getData()

        Glide.with(applicationContext).load(R.drawable.example_qr_code).into(qrCodeImageView)

        generateButton.setOnClickListener {
            val inputData =
                "John Doe\nCEO\nAcme Corporation\njohndoe@example.com" // Business card data

            val qrCodeBitmap = generateQRCode(userData.getUid()!!)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)

            val uniqueUrl = generateUniqueUrlFromData(jsonData)
            println("URL única generada: $uniqueUrl")

            scannedCards.clear()
            scannedCards.add(uniqueUrl)
            cardAdapter.notifyDataSetChanged()
        }

        scanButton.setOnClickListener {
            startQRCodeScanner()
        }
    }

    private fun isShare() {
        intent = intent
        share = intent.getBooleanExtra("share", true)
    }

    private fun initializeVariables() {
        reference = FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        generateButton = findViewById(R.id.generateButton)
        scanButton = findViewById(R.id.scanButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        cardList = findViewById(R.id.cardList)

        cardAdapter = CardAdapter(scannedCards)
        cardList.layoutManager = LinearLayoutManager(this)
        cardList.adapter = cardAdapter
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

        var qrDataDatabase = parseJsonToQrData(userData.getQrMark()!!)
        if (qrDataDatabase == null) {
            qrDataDatabase = QrData("", "")
        }

        val qrData: QrData = if (share) {
            QrData("${userDataUid}_${System.currentTimeMillis()}", qrDataDatabase!!.getQrLogin())
        } else {
            QrData(
                qrDataDatabase!!.getQrDataShare(),
                "${userDataUid}_${System.currentTimeMillis()}"
            )
        }

        val gson = Gson()
        jsonData = gson.toJson(qrData)


        val bitMatrix: BitMatrix = try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            MultiFormatWriter().encode(
                jsonData,
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

        saveQrMark(jsonData)
        return bitmap
    }

    private fun saveQrMark(data: String) {
        reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null) {
                    val hashMap = HashMap<String, Any>()
                    hashMap["qrMark"] = data
                    dataSnapshot.ref.updateChildren(hashMap)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun saveQrDataUser(data: String){
        val gson = Gson()
        val qrData: QrData = gson.fromJson(data, QrData::class.java)

        reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userPrimary = dataSnapshot.getValue(User::class.java)
                val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(
                    qrData.getQrDataShare()!!.split("_")[0]
                )
                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val userSecondary = userSnapshot.getValue(User::class.java)

                        if (userPrimary != null && userSecondary != null &&
                            !userPrimary.getKnownPrivateUsers().contains(userSecondary.getUid())
                            && qrData.getQrDataShare() == parseJsonToQrData(userSecondary.getQrMark()!!)!!.getQrDataShare()
                        ) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["knownPrivateUsers"] =
                                userPrimary.getKnownPrivateUsers() + qrData.getQrDataShare()!!
                                    .split("_")[0]
                            dataSnapshot.ref.updateChildren(hashMap)

                            Toast.makeText(applicationContext, "User ${userSecondary.getUsername()} added", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@QrCodeActivity, MessageActivity::class.java)
                            intent.putExtra("userUid", qrData.getQrDataShare()!!.split("_")[0])
                            this@QrCodeActivity.startActivity(intent)
                        }else{
                            Toast.makeText(applicationContext, "Previously added user", Toast.LENGTH_SHORT).show()
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

    private fun parseJsonToQrData(jsonData: String): QrData? {
        val gson = Gson()
        return try {
            gson.fromJson(jsonData, QrData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    private fun startQRCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents != null) {
                saveQrDataUser(result.contents)
            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
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


}