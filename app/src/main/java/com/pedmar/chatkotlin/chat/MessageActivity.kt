package com.pedmar.chatkotlin.chat

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.pedmar.chatkotlin.MainActivity
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.ChatAdapter
import com.pedmar.chatkotlin.group.SelectDataGroup
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.notifications.*
import com.pedmar.chatkotlin.profile.VisitedProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class MessageActivity : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var ibSend: ImageButton
    private lateinit var ibInclude: ImageButton
    private lateinit var imageProfileChat: ImageView
    private lateinit var usernameProfileChat: TextView
    private var uidUserSelected: String = ""
    private var firebaseUser: FirebaseUser? = null

    lateinit var rvChats: RecyclerView
    var chatAdapter: ChatAdapter? = null
    var chatList: List<Chat>? = null

    var reference: DatabaseReference? = null
    var seenListener: ValueEventListener? = null

    var notify = false
    var apiService: APIService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        initializeVariables()
        getUid()
        getDataUserSelected()

        ibInclude.setOnClickListener {
            notify = true

            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                selectAction()
            } else {
                requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        ibSend.setOnClickListener {
            notify = true
            val message = etMessage.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(applicationContext, "Please enter a message", Toast.LENGTH_SHORT)
                    .show()
            } else {
                sendMessage(firebaseUser!!.uid, uidUserSelected, message)
                etMessage.setText("")
            }
        }

        viewedMessage(uidUserSelected)
    }


    private fun getUid() {
        intent = intent
        uidUserSelected = intent.getStringExtra("userUid").toString()
    }

    private fun sendMessage(uidIssuer: String, uidReceiver: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        val infoMessage = HashMap<String, Any?>()
        infoMessage["keyMessage"] = keyMessage
        infoMessage["issuer"] = uidIssuer
        infoMessage["receiver"] = uidReceiver
        infoMessage["message"] = message.replace("\n", " ").trim()
        infoMessage["url"] = ""
        infoMessage["viewed"] = false
        infoMessage["groupChat"] = false
        reference.child("Chats").child(keyMessage!!).setValue(infoMessage)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val listMessageIssuer =
                        FirebaseDatabase.getInstance().reference.child("MessageList")
                            .child(firebaseUser!!.uid)
                            .child(uidUserSelected)

                    listMessageIssuer.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                listMessageIssuer.child("uid").setValue(uidUserSelected)
                            }
                            val listMessageReceiver =
                                FirebaseDatabase.getInstance().reference.child("MessageList")
                                    .child(uidUserSelected)
                                    .child(firebaseUser!!.uid)
                            listMessageReceiver.child("uid").setValue(firebaseUser!!.uid)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })
                }

            }

        val userReference =
            FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        userReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (notify) {
                    sendNotification(uidReceiver, user!!.getUsername(), message)
                }
                notify = false
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun sendNotification(uidReceiver: String, username: String?, message: String) {

        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = reference.orderByKey().equalTo(uidReceiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val token: Token? = dataSnapshot.getValue(Token::class.java)

                    val data = Data(
                        firebaseUser!!.uid,
                        R.mipmap.ic_chat,
                        "$username: $message",
                        "New message",
                        uidUserSelected
                    )
                    val sender = Sender(data!!, token!!.getToken().toString())

                    apiService!!.sendNotification(sender).enqueue(object : Callback<MyResponse> {
                        override fun onResponse(
                            call: Call<MyResponse>,
                            response: Response<MyResponse>
                        ) {
                            if (response.code() == 200) {
                                if (response.body()!!.success !== 1) {
                                    Log.d("NOTIFICATION", "Error sending notification to user")
                                    //Toast.makeText(applicationContext,"An error has occurred", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getDataUserSelected() {
        val reference =
            FirebaseDatabase.getInstance().reference.child("Users").child(uidUserSelected)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user: User? = snapshot.getValue(User::class.java)
                //Obtener nombre del usuario
                usernameProfileChat.text = user!!.getUsername()

                //Obtener imagen de perfil
                Glide.with(applicationContext).load(user.getImage())
                    .placeholder(R.drawable.ic_item_user).into(imageProfileChat)

                getMessages(firebaseUser!!.uid, uidUserSelected, user.getImage())
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun viewedMessage(userUid: String) {
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat!!.getReceiver().equals(firebaseUser!!.uid) && chat!!.getIssuer()
                            .equals(userUid)
                    ) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["viewed"] = true
                        dataSnapshot.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getMessages(issuerUid: String, receiverUid: String, receiverImage: String?) {
        chatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for (sn in snapshot.children) {
                    val chat = sn.getValue(Chat::class.java)

                    if (chat!!.getReceiver().equals(issuerUid) && chat.getIssuer()
                            .equals(receiverUid)
                        || chat.getReceiver().equals(receiverUid) && chat.getIssuer()
                            .equals(issuerUid)
                    ) {
                        (chatList as ArrayList<Chat>).add(chat)
                    }

                    chatAdapter = ChatAdapter(
                        this@MessageActivity,
                        (chatList as ArrayList<Chat>),
                        receiverImage!!,
                        emptyMap()
                    )
                    rvChats.adapter = chatAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun initializeVariables() {

        val toolbar: Toolbar = findViewById(R.id.toolbarChat)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        apiService =
            Client.Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        etMessage = findViewById(R.id.etMessage)
        ibSend = findViewById(R.id.iBSend)
        imageProfileChat = findViewById(R.id.imageProfileChat)
        usernameProfileChat = findViewById(R.id.usernameProfileChat)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        ibInclude = findViewById(R.id.ibInclude)

        rvChats = findViewById(R.id.rvChats)
        rvChats.setHasFixedSize(true)
        var linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        rvChats.layoutManager = linearLayoutManager
    }

    private fun selectAction() {
        val items = arrayOf("Take Photo", "Choose from Gallery", "Select document")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setItems(items) { dialog, which ->
            when (which) {
                0 -> dispatchTakePictureIntent()
                1 -> dispatchPickImageIntent()
                2 -> dispatchPickDocumentIntent()
            }
        }
        builder.show()
    }

    private fun dispatchPickDocumentIntent() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val pickDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            pickDocumentIntent.type = "*/*"
            startActivityForResult(pickDocumentIntent, SelectDataGroup.REQUEST_DOCUMENT_PICK)
        } else {
            requestDocumentPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.CAMERA
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(
                    takePictureIntent,
                    SelectDataGroup.REQUEST_IMAGE_CAPTURE
                )
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Camera Not Available", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }


    private fun dispatchPickImageIntent() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val pickImageIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageIntent.type = "image/*"
            startActivityForResult(pickImageIntent, SelectDataGroup.REQUEST_IMAGE_PICK)
        } else {
            requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SelectDataGroup.REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    handleImageSelection(imageBitmap, null)
                }
                SelectDataGroup.REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    handleImageSelection(imageBitmap, null)
                }
                SelectDataGroup.REQUEST_DOCUMENT_PICK -> {
                    val selectedDocumentUri = data?.data
                    selectedDocumentUri?.let { uri ->
                        handleImageSelection(null, uri)
                    }
                }
            }
        }
    }

    private fun handleImageSelection(imageBitmap: Bitmap?, documentUri: Uri?) {
        val loadingImage = ProgressDialog(this@MessageActivity)
        loadingImage.setMessage("Loading Image...")
        loadingImage.setCanceledOnTouchOutside(false)
        loadingImage.show()

        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        var uploadTask: StorageTask<*>? = null
        var imageFolder: StorageReference? = null
        var ref: StorageReference? = null

        if (documentUri != null) {
            imageFolder = FirebaseStorage.getInstance().reference.child("Messages documents")
            // Obtener el nombre del archivo original
            ref = imageFolder.child("$keyMessage")
            uploadTask = ref.putFile(documentUri)

        } else {
            imageFolder = FirebaseStorage.getInstance().reference.child("Messages images")
            ref = imageFolder.child("$keyMessage.png")

            // Convierte el Bitmap a ByteArray
            val stream = ByteArrayOutputStream()
            imageBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = stream.toByteArray()

            uploadTask = ref.putBytes(imageData)
        }
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loadingImage.dismiss()
                val downloadUrl = task.result
                val url = downloadUrl.toString()

                val infoMessageImage = HashMap<String, Any?>()
                infoMessageImage["keyMessage"] = keyMessage
                infoMessageImage["issuer"] = firebaseUser!!.uid
                infoMessageImage["receiver"] = uidUserSelected
                infoMessageImage["message"] = "Submitted image"
                infoMessageImage["url"] = url
                infoMessageImage["viewed"] = false
                infoMessageImage["groupChat"] = false

                if (documentUri != null) {
                    infoMessageImage["message"] = "File: ${getFileNameFromUri(documentUri)}"
                } else {
                    infoMessageImage["message"] = "Submitted image"
                }

                reference.child("Chats").child(keyMessage!!).setValue(infoMessageImage)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                            val userReference =
                                FirebaseDatabase.getInstance().reference.child("Users")
                                    .child(firebaseUser!!.uid)
                            userReference.addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue(User::class.java)
                                    if (notify) {
                                        sendNotification(
                                            uidUserSelected,
                                            user!!.getUsername(),
                                            "Submitted image"
                                        )
                                    }
                                    notify = false
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                        }
                    }

                reference.child("Chats").child(keyMessage!!).setValue(infoMessageImage)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val listMessageIssuer =
                                FirebaseDatabase.getInstance().reference.child("MessageList")
                                    .child(firebaseUser!!.uid)
                                    .child(uidUserSelected)

                            listMessageIssuer.addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        listMessageIssuer.child("uid").setValue(uidUserSelected)
                                    }
                                    val listMessageReceiver =
                                        FirebaseDatabase.getInstance().reference.child("MessageList")
                                            .child(uidUserSelected)
                                            .child(firebaseUser!!.uid)
                                    listMessageReceiver.child("uid").setValue(firebaseUser!!.uid)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                        }
                    }
                Toast.makeText(
                    applicationContext,
                    "The image has been sent successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && it.moveToFirst()) {
                fileName = it.getString(nameIndex)
            }
        }
        return fileName
    }

    private val requestDocumentPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission_granted ->
            if (permission_granted) {
                dispatchPickDocumentIntent()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Permission has not been granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val requestGalleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission_granted ->
            if (permission_granted) {
                selectAction()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Permission has not been granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission_granted ->
            if (permission_granted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Permission has not been granted",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_visit_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_visit_profile -> {
                val intent = Intent(applicationContext, VisitedProfileActivity::class.java)
                intent.putExtra("uid", uidUserSelected)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
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
        reference!!.removeEventListener(seenListener!!)
        updateStatus("offline")
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}