package com.pedmar.chatkotlin.chat

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.pedmar.chatkotlin.MainActivity
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.ChatAdapter
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.notifications.*
import com.pedmar.chatkotlin.profile.VisitedProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageGroupActivity : AppCompatActivity() {
    private lateinit var etMessage : EditText
    private lateinit var ibSend : ImageButton
    private lateinit var ibInclude : ImageButton
    private lateinit var imageGroupChat : ImageView
    private lateinit var nameGroupChat : TextView
    private var uidGroup  : String = ""
    private var firebaseUser : FirebaseUser?= null
    private var userList : List<String>?=null

    lateinit var rvChats : RecyclerView
    var chatAdapter : ChatAdapter?= null
    var chatList : List<Chat> ?= null

    var reference : DatabaseReference?= null
    var seenListener : ValueEventListener?= null

    var notify = false
    var apiService : APIService?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_group)
        initializeVariables()
        getUidGroup()
        getDataGroupSelected()

        ibInclude.setOnClickListener {
            notify = true

            if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
                pickImage()
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

    private fun sendMessage(uidIssuer: String, uidGroup: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        val infoMessage = HashMap<String, Any?>()
        infoMessage["keyMessage"] = keyMessage
        infoMessage["issuer"] = uidIssuer
        infoMessage["receiver"] = uidGroup
        infoMessage["message"] = message.replace("\n", " ").trim()
        infoMessage["url"] = ""
        infoMessage["viewed"] = false
        infoMessage["groupChat"] = true
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
                            .child(firebaseUser!!.uid)
                        listMessageReceiver.child("uid").setValue(firebaseUser!!.uid)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
            }

        }

        val groupReference = FirebaseDatabase.getInstance().reference.child("Group").child(uidGroup)
        groupReference.addValueEventListener(object  : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChat = snapshot.getValue(GroupChat::class.java)
                if (groupChat != null) {
                    for(userId in groupChat.getUidUsersList()!!){
                        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(userId)
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (notify && user != null) {
                                    sendNotification(userId, user.getUsername(), message)
                                }
                                notify = false
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                    }
                }
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
                for (dataSnapshot in snapshot.children){
                    val token : Token?= dataSnapshot.getValue(Token::class.java)

                    val data = Data(firebaseUser!!.uid, R.mipmap.ic_chat, "$username: $message","New message", uidReceiver)
                    val sender = Sender(data!!, token!!.getToken().toString())

                    apiService!!.sendNotification(sender).enqueue(object: Callback<MyResponse> {
                        override fun onResponse(
                            call: Call<MyResponse>,
                            response: Response<MyResponse>
                        ) {
                            if(response.code() == 200){
                                if (response.body()!!.success !==1){
                                    Toast.makeText(applicationContext,"An error has occurred", Toast.LENGTH_SHORT).show()
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
                //Obtener nombre del usuario
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
                    if (chat!!.getReceiver().equals(firebaseUser!!.uid) && chat!!.getIssuer().equals(uidGroup)){
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

    private fun pickImage() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageIntent.type = "image/*"

        // Lanzar la actividad para seleccionar una imagen de la galería
        pickImageActivityResult.launch(pickImageIntent)
    }

    private val pickImageActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        } else {
            Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(imageUri: Uri) {
        val loadingImage = ProgressDialog(this@MessageGroupActivity)
        loadingImage.setMessage("Loading Image...")
        loadingImage.setCanceledOnTouchOutside(false)
        loadingImage.show()

        userList = uidGroup.split("-")
        val imageFolder = FirebaseStorage.getInstance().reference.child("Messages images")
        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key
        val imageName = imageFolder.child("$keyMessage.jpg")

        val uploadTask: StorageTask<*>
        uploadTask = imageName.putFile(imageUri)
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation imageName.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loadingImage.dismiss()
                val downloadUrl = task.result
                val url = downloadUrl.toString()

                val infoMessageImage = HashMap<String, Any?>()
                infoMessageImage["keyMessage"] = keyMessage
                infoMessageImage["issuer"] = firebaseUser!!.uid
                infoMessageImage["receiver"] = uidGroup
                infoMessageImage["message"] = "Submitted image"
                infoMessageImage["url"] = url
                infoMessageImage["viewed"] = false
                infoMessageImage["groupChat"] = true

                reference.child("Chats").child(keyMessage!!).setValue(infoMessageImage)
                    .addOnCompleteListener { task->
                        if (task.isSuccessful){
                            for (uidUser in userList!!) {
                                val userReference =
                                    FirebaseDatabase.getInstance().reference.child("Users")
                                        .child(uidUser)
                                userReference.addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val user = snapshot.getValue(User::class.java)
                                        if (notify) {

                                            sendNotification(
                                                uidUser,
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

    private val requestGalleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                pickImage()
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
        inflater.inflate(R.menu.menu_visit_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_visit->{
                val intent = Intent(applicationContext, VisitedProfileActivity::class.java)
                intent.putExtra("uid", uidGroup)
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
    }
}