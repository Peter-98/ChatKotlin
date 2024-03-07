package com.pedmar.chatkotlin.chat

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
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
import com.pedmar.chatkotlin.group.VisitedGroupActivity
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.notifications.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MessageGroupActivity : AppCompatActivity(){
    private lateinit var etMessage : EditText
    private lateinit var ibSend : ImageButton
    private lateinit var ibInclude : ImageButton
    private lateinit var imageGroupChat : ImageView
    private lateinit var nameGroupChat : TextView
    private var uidGroup  : String = ""
    private var firebaseUser : FirebaseUser?= null
    private var userList : List<String>?=null
    private var currentUser: User? = null

    lateinit var rvChats : RecyclerView
    var chatAdapter : ChatAdapter?= null
    var chatList : List<Chat> ?= null

    var reference : DatabaseReference?= null
    var seenListener : ValueEventListener?= null

    var notify = false
    var apiService : APIService?=null

    var uri : String = ""
    var name : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_group)
        initializeVariables()
        getCurrentUserFromDatabase()
        getUidGroup()
        getDataGroupSelected()

        ibInclude.setOnClickListener {
            notify = true

            if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
                selectAction()
            }else{
                requestGalleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        ibSend.setOnClickListener{
            notify = true
            val message = etMessage.text.toString()
            if (message.isEmpty()){
                Toast.makeText(applicationContext, "Please enter a message", Toast.LENGTH_SHORT).show()
            }else{
                sendMessage(firebaseUser!!.uid, uidGroup, message)
                etMessage.setText("")
            }
        }

        viewedMessage(uidGroup)
    }

    private fun getUidGroup(){
        intent = intent
        uidGroup = intent.getStringExtra("uidGroup").toString()
    }

    private fun getCurrentUserFromDatabase() {
        firebaseUser?.uid?.let { userId ->
            val usersReference =
                FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        currentUser = dataSnapshot.getValue(User::class.java)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Manejar el error si la operación es cancelada
                }
            })
        }
    }

    private fun sendMessage(uidIssuer: String, uidGroup: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        val infoMessage = HashMap<String, Any?>()
        infoMessage["keyMessage"] = keyMessage
        infoMessage["issuer"] = uidIssuer
        infoMessage["usernameIssuer"] = currentUser!!.getUsername()
        infoMessage["receiver"] = uidGroup
        infoMessage["message"] = message.replace("\n", " ").trim()
        infoMessage["url"] = ""
        infoMessage["viewed"] = false
        infoMessage["groupChat"] = true
        infoMessage["image"] = currentUser!!.getImage()

        val isViewedList = MutableList(userList!!.size) { false }
        isViewedList[userList!!.indexOf(currentUser!!.getUid())] = true
        infoMessage["allViewed"] = isViewedList

        reference.child("Chats").child(keyMessage!!).setValue(infoMessage).addOnCompleteListener{task->
            if (task.isSuccessful){
                val listMessageIssuer = FirebaseDatabase.getInstance().reference.child("MessageList")
                    .child(firebaseUser!!.uid)
                    .child(uidGroup)

                listMessageIssuer.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()){
                            listMessageIssuer.child("uid").setValue(uidGroup)
                        }
                        val listMessageReceiver = FirebaseDatabase.getInstance().reference.child("MessageList")
                            .child(uidGroup)
                        for (user in userList!!){
                            listMessageReceiver.child(user).child("uid").setValue(user)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
            }

        }

        val groupReference = FirebaseDatabase.getInstance().reference.child("Groups").child(uidGroup)
        groupReference.addValueEventListener(object  : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat = snapshot.getValue(GroupChat::class.java)
                if (groupChat != null) {
                    val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
                    userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val user = userSnapshot.getValue(User::class.java)
                            for (userId in groupChat.getUidUsersList()!!) {

                                if (notify && user != null && !user.getUid().equals(userId)) {
                                    sendNotification(userId, user.getUsername(), message, groupChat.getName()!!)
                                }
                                if (userId == groupChat.getUidUsersList()!!.last()) {
                                    notify = false
                                }
                            }
                        }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })

                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun sendNotification(uidReceiver: String, username: String?, message: String, groupName : String) {

        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = reference.orderByKey().equalTo(uidReceiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val token : Token?= dataSnapshot.getValue(Token::class.java)

                    val data = Data(firebaseUser!!.uid, R.mipmap.ic_chat, "$username: $message","$groupName: New message", uidReceiver)
                    val sender = Sender(data!!, token!!.getToken().toString())

                    apiService!!.sendNotification(sender).enqueue(object: Callback<MyResponse> {
                        override fun onResponse(
                            call: Call<MyResponse>,
                            response: Response<MyResponse>
                        ) {
                            if(response.code() == 200){
                                if (response.body()!!.success !==1){
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

    private fun getDataGroupSelected(){
        val reference = FirebaseDatabase.getInstance().reference.child("Groups").child(uidGroup.toString())
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat : GroupChat? = snapshot.getValue(GroupChat::class.java)
                userList = groupChat!!.getUidUsersList()

                nameGroupChat.text = groupChat!!.getName()
                //Obtener imagen de perfil
                if(groupChat.getImage().toString().equals("No image")){
                    Glide.with(applicationContext).load(R.drawable.ic_item_user).placeholder(R.drawable.ic_item_user).into(imageGroupChat)
                }else{
                    Glide.with(applicationContext).load(groupChat.getImage()).placeholder(R.drawable.ic_item_user).into(imageGroupChat)
                }


                getMessages(firebaseUser!!.uid, uidGroup.toString(), groupChat.getImage())
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun viewedMessage(uidGroup : String){
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    val indexUser = userList!!.indexOf(firebaseUser!!.uid)

                    if (userList!!.contains(firebaseUser!!.uid) &&
                        chat!!.getReceiver().equals(uidGroup) &&
                        indexUser != -1 &&
                        chat!!.getAllViewed()?.isNotEmpty() == true &&
                        !chat!!.getAllViewed()?.get(indexUser)!!){

                        // Obtener la lista de booleanos isAllViewed del chat
                        val isAllViewed = chat!!.getAllViewed() ?: emptyList<Boolean>()
                        // Crear una copia mutable de la lista
                        val viewedList = isAllViewed.toMutableList()
                        viewedList[indexUser] = true

                        val hashMap = HashMap<String, Any>()
                        hashMap["allViewed"] = viewedList

                        if(viewedList.all { it }){
                            hashMap["viewed"] = true
                        }
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
        var userColorsMap: Map<String, Long>? = null // Mapa de colores de usuarios

        // Obtener la referencia a la base de datos de Groups
        val groupsReference = FirebaseDatabase.getInstance().reference.child("Groups")

        // Obtener la lista de colores de usuarios del grupo
        groupsReference.child(receiverUid).child("colorUsersList")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(colorSnapshot: DataSnapshot) {
                    userColorsMap = colorSnapshot.value as Map<String, Long>?

                    // Obtener los mensajes del chat
                    val chatsReference = FirebaseDatabase.getInstance().reference.child("Chats")
                    chatsReference.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            (chatList as ArrayList<Chat>).clear()
                            for (sn in snapshot.children){
                                val chat = sn.getValue(Chat::class.java)

                                if(chat!!.getReceiver().equals(receiverUid)
                                    || chat.getReceiver().equals(receiverUid) && chat.getIssuer().equals(issuerUid)){
                                    (chatList as ArrayList<Chat>).add(chat)
                                }
                            }

                            // Crear y configurar el adaptador con la lista de mensajes y el mapa de colores de usuarios
                            chatAdapter = ChatAdapter(this@MessageGroupActivity, chatList as ArrayList<Chat>, receiverImage!!, userColorsMap)
                            rvChats.adapter = chatAdapter
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Manejar la cancelación de la lectura de datos
                        }
                    })
                }

                override fun onCancelled(colorError: DatabaseError) {
                    // Manejar la cancelación de la lectura de datos
                }
            })
    }


    private fun initializeVariables(){

        val toolbar : Toolbar = findViewById(R.id.toolbarChat)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        apiService = Client.Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        etMessage = findViewById(R.id.etMessage)
        ibSend = findViewById(R.id.iBSend)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        ibInclude = findViewById(R.id.ibInclude)
        imageGroupChat = findViewById(R.id.imageGroupChat)
        nameGroupChat = findViewById(R.id.nameGroupChat)

        rvChats = findViewById(R.id.rvChats)
        rvChats.setHasFixedSize(true)
        var linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        rvChats.layoutManager = linearLayoutManager
    }
    private fun selectAction() {
        val items = arrayOf("Take Photo", "Choose from Gallery", "Select document", "Send location")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        //builder.setTitle("Select Image")
        builder.setItems(items) { dialog, which ->
            when (which) {
                0 -> dispatchTakePictureIntent()
                1 -> dispatchPickImageIntent()
                2 -> dispatchPickDocumentIntent()
                3 -> dispatchPickLocationIntent()
            }
        }
        builder.show()
    }

    private fun dispatchPickLocationIntent() {
        val intent = Intent(this, SendLocationActivity::class.java)
        startActivityForResult(intent, SelectDataGroup.REQUEST_CODE_PICK_LOCATION)
    }

    private fun dispatchPickDocumentIntent() {
        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val pickDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            pickDocumentIntent.type = "*/*"
            startActivityForResult(pickDocumentIntent, SelectDataGroup.REQUEST_DOCUMENT_PICK)
        } else {
            requestDocumentPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun dispatchTakePictureIntent() {
        if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent,
                    SelectDataGroup.REQUEST_IMAGE_CAPTURE
                )
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Camera Not Available", Toast.LENGTH_SHORT)
                    .show()
            }
        }else{
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun dispatchPickImageIntent() {
        if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED){
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageIntent.type = "image/*"
            startActivityForResult(pickImageIntent, SelectDataGroup.REQUEST_IMAGE_PICK)
        }else{
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
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    handleImageSelection(imageBitmap, null)
                }
                SelectDataGroup.REQUEST_DOCUMENT_PICK -> {
                    val selectedDocumentUri = data?.data
                    selectedDocumentUri?.let { uri ->
                        handleImageSelection(null, uri)
                    }
                }
                SelectDataGroup.REQUEST_CODE_PICK_LOCATION  -> {

                    val latitude = data?.getDoubleExtra("latitude", 0.0)
                    val longitude = data?.getDoubleExtra("longitude", 0.0)
                    val url = data?.getStringExtra("url")

                }
            }
        }
    }

    private fun handleImageSelection(imageBitmap: Bitmap?, documentUri: Uri?) {
        val loadingImage = ProgressDialog(this@MessageGroupActivity)
        loadingImage.setMessage("Loading Image...")
        loadingImage.setCanceledOnTouchOutside(false)
        loadingImage.show()

        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        var uploadTask: StorageTask<*>? = null
        var imageFolder: StorageReference? = null
        var ref : StorageReference? = null

        if(documentUri != null){
            imageFolder = FirebaseStorage.getInstance().reference.child("Messages_documents")
            // Obtener el nombre del archivo original
            ref = imageFolder.child("$keyMessage")
            uploadTask = ref.putFile(documentUri)

        }else{
            imageFolder = FirebaseStorage.getInstance().reference.child("Messages_images")
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
                infoMessageImage["usernameIssuer"] = currentUser!!.getUsername()
                infoMessageImage["receiver"] = uidGroup
                infoMessageImage["url"] = url
                infoMessageImage["viewed"] = false
                infoMessageImage["groupChat"] = true
                infoMessageImage["image"] = currentUser!!.getImage()

                if(documentUri != null){
                    infoMessageImage["message"] = "File: ${getFileNameFromUri(documentUri)}"
                }else{
                    infoMessageImage["message"] = "Submitted image"
                }

                val isViewedList = MutableList(userList!!.size) { false }
                isViewedList[userList!!.indexOf(currentUser!!.getUid())] = true
                infoMessageImage["allViewed"] = isViewedList

                reference.child("Chats").child(keyMessage!!).setValue(infoMessageImage)
                    .addOnCompleteListener { task->
                        if (task.isSuccessful){
                            val groupReference = FirebaseDatabase.getInstance().reference.child("Groups").child(uidGroup)
                            groupReference.addValueEventListener(object  : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val groupChat = snapshot.getValue(GroupChat::class.java)
                                    if (groupChat != null) {
                                        val userReference =
                                            FirebaseDatabase.getInstance().reference.child("Users")
                                                .child(firebaseUser!!.uid)
                                        userReference.addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val user = snapshot.getValue(User::class.java)
                                                for (userId in groupChat.getUidUsersList()!!) {

                                                    if (notify && user != null && !user.getUid().equals(userId)) {
                                                        sendNotification(
                                                            userId,
                                                            user!!.getUsername(),
                                                            "Submitted image",
                                                            groupChat.getName()!!
                                                        )}
                                                    if (userId == groupChat.getUidUsersList()!!.last()) {
                                                        notify = false
                                                    }
                                                }
                                            }
                                            override fun onCancelled(error: DatabaseError) {
                                            }
                                        })
                                    }
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
                                    .child(uidGroup)

                            listMessageIssuer.addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        listMessageIssuer.child("uid").setValue(uidGroup)
                                    }
                                    val listMessageReceiver =
                                        FirebaseDatabase.getInstance().reference.child("MessageList")
                                            .child(uidGroup)
                                    for (user in userList!!){
                                        listMessageReceiver.child(user).child("uid").setValue(user)
                                    }
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchPickDocumentIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestGalleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                selectAction()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchTakePictureIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
            }
        }

    //Detiene la tarea de actualizar viewed de false a true
    override fun onPause() {
        super.onPause()
        reference!!.removeEventListener(seenListener!!)
        updateStatus("offline")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater : MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_visit_group, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_visit_group->{
                val intent = Intent(applicationContext, VisitedGroupActivity::class.java)
                intent.putExtra("uidGroup", uidGroup)
                startActivity(intent)
                return true
            }else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateStatus(status : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["status"] = status
        reference!!.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        updateStatus("online")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}