package com.pedmar.chatkotlin

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.pedmar.chatkotlin.adapter.CardAdapter
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.QrData
import com.pedmar.chatkotlin.model.User
import java.util.*

class QrCodeActivity : AppCompatActivity() {

    private lateinit var generateButton: Button
    private lateinit var scanButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var cardList: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private var scannedCards: MutableList<String> = mutableListOf()

    private val qrCodeWidthPixels = 500

    private lateinit var userData : User
    private lateinit var imageBitmap : Bitmap
    private var share  : Boolean = true

    private var reference : DatabaseReference?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        initializeVariables()
        isShare()
        getData()

        generateButton.setOnClickListener {
            val inputData = "John Doe\nCEO\nAcme Corporation\njohndoe@example.com" // Business card data

            val qrCodeBitmap = generateQRCode(userData.getUid()!!)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            scannedCards.add(inputData)
            cardAdapter.notifyDataSetChanged()
        }

        scanButton.setOnClickListener {
            startQRCodeScanner()
        }
    }

    private fun isShare(){
        intent = intent
        share = intent.getBooleanExtra("share", true)
    }

    private fun initializeVariables() {
        reference = FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().currentUser!!.uid)
        generateButton = findViewById(R.id.generateButton)
        scanButton = findViewById(R.id.scanButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        cardList = findViewById(R.id.cardList)

        cardAdapter = CardAdapter(scannedCards)
        cardList.layoutManager = LinearLayoutManager(this)
        cardList.adapter = cardAdapter
    }

    private fun getData(){
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val user : User?= snapshot.getValue(User::class.java)
                    userData = user!!

                    Glide.with(applicationContext /* Context */)
                        .asBitmap()
                        .load(user.getImage())
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                imageBitmap = resource
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        })
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun generateQRCode(userDataUid: String): Bitmap? {

        val qrDataDatabase =  parseJsonToQrData(userData.getQrMark()!!)

        var qrData : QrData = if(share){
            QrData("${userDataUid}_${System.currentTimeMillis()}",qrDataDatabase!!.getQrLogin())
        }else{
            QrData(qrDataDatabase!!.getQrDataShare(),"${userDataUid}_${System.currentTimeMillis()}")
        }

        val gson = Gson()
        val jsonData = gson.toJson(qrData)


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

        val qrCodeWidth = bitMatrix.width
        val qrCodeHeight = bitMatrix.height
        val pixels = IntArray(qrCodeWidth * qrCodeHeight)

        for (y in 0 until qrCodeHeight) {
            val offset = y * qrCodeWidth
            for (x in 0 until qrCodeWidth) {
                pixels[offset + x] = if (bitMatrix[x, y]) {
                    resources.getColor(R.color.white, theme) // QR code color (black)
                } else {
                    resources.getColor(R.color.black, theme) // Background color (white)
                }
            }
        }

        val bitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.RGB_565)
        bitmap.setPixels(pixels, 0, qrCodeWidth, 0, 0, qrCodeWidth, qrCodeHeight)

        var logoBitmap = imageBitmap

        val scaledLogoBitmap =
            Bitmap.createScaledBitmap(logoBitmap, qrCodeWidth / 4, qrCodeHeight / 4, false)

        saveQrMark(jsonData)
        return combineBitmaps(bitmap, scaledLogoBitmap)
    }

    private fun combineBitmaps(backgroundBitmap: Bitmap, overlayBitmap: Bitmap): Bitmap {
        val combinedBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)
        val canvas = Canvas(combinedBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        val left = (backgroundBitmap.width - overlayBitmap.width) / 2
        val top = (backgroundBitmap.height - overlayBitmap.height) / 2
        canvas.drawBitmap(overlayBitmap, left.toFloat(), top.toFloat(), null)
        return combinedBitmap
    }

    private fun saveQrMark(data: String) {
        reference!!.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null){
                    val hashMap = HashMap<String, Any>()
                    hashMap["qrMark"] = data
                    dataSnapshot.ref.updateChildren(hashMap)
                }
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

    private fun startQRCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents != null) {
                Toast.makeText(this, result.contents, Toast.LENGTH_SHORT).show()
                scannedCards.add(result.contents)
                cardAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}