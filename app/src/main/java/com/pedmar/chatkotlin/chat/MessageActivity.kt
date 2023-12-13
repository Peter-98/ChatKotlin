package com.pedmar.chatkotlin.chat

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Adapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
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
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.ChatAdapter
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.notifications.*
import com.pedmar.chatkotlin.profile.EditImageProfileActivity
import com.pedmar.chatkotlin.profile.VisitedProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageActivity : AppCompatActivity() {

    private lateinit var etMessage : EditText
    private lateinit var ibSend : ImageButton
    private lateinit var ibInclude : ImageButton
    private lateinit var imageProfileChat : ImageView
    private lateinit var usernameProfileChat : TextView
    var uidUserSelected : String = ""
    var firebaseUser : FirebaseUser ?= null

    lateinit var rvChats : RecyclerView
    var chatAdapter : ChatAdapter ?= null
    var chatList : List<Chat> ?= null

    var reference : DatabaseReference ?= null
    var seenListener : ValueEventListener ?= null

    var notify = false
    var apiService : APIService?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        initializeVariables()
        getUid()
        getDataUserSelected()

        ibInclude.setOnClickListener {
            notify = true
            pickImage()
        }

        ibSend.setOnClickListener{
            notify = true
            val message = etMessage.text.toString()
            if (message.isEmpty()){
                Toast.makeText(applicationContext, "Please enter a message", Toast.LENGTH_SHORT).show()
            }else{
                sendMessage(firebaseUser!!.uid, uidUserSelected, message)
                etMessage.setText("")
            }
        }

        viewedMessage(uidUserSelected)
    }



    private fun getUid(){
        intent = intent
        uidUserSelected = intent.getStringExtra("userUid").toString()
    }

    private fun sendMessage(uidIssuer: String, uidReceiver: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val keyMessage = reference.push().key

        val infoMessage = HashMap<String, Any?>()
        infoMessage["idMessage"] = keyMessage
        infoMessage["issuer"] = uidIssuer
        infoMessage["receiver"] = uidReceiver
        infoMessage["message"] = message
        infoMessage["url"] = ""
        infoMessage["viewed"] = false
        reference.child("Chats").child(keyMessage!!).setValue(infoMessage).addOnCompleteListener{task->
            if (task.isSuccessful){
                val listMessageIssuer = FirebaseDatabase.getInstance().reference.child("MessageList")
                    .child(firebaseUser!!.uid)
                    .child(uidUserSelected)

                listMessageIssuer.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()){
                            listMessageIssuer.child("uid").setValue(uidUserSelected)
                        }
                        val listMessageReceiver = FirebaseDatabase.getInstance().reference.child("MessageList")
                            .child(uidUserSelected)
                            .child(firebaseUser!!.uid)
                        listMessageReceiver.child("uid").setValue(firebaseUser!!.uid)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
            }

        }

        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        userReference.addValueEventListener(object  : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
               val user = snapshot.getValue(User::class.java)
                if (notify){
                    sendNotification(uidReceiver, user!!.getUsername(),message)
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

        query.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
               for (dataSnapshot in snapshot.children){
                   val token : Token?= dataSnapshot.getValue(Token::class.java)

                   val data = Data(firebaseUser!!.uid, R.mipmap.ic_chat, "$username: $message","New message", uidUserSelected)
                   val sender = Sender(data!!, token!!.getToken().toString())

                   apiService!!.sendNotification(sender).enqueue(object: Callback<MyResponse>{
                       override fun onResponse(
                           call: Call<MyResponse>,
                           response: Response<MyResponse>
                       ) {
                           if(response.code() == 200){
                               if (response.body()!!.succes !==1){
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

    private fun getDataUserSelected(){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(uidUserSelected)
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user : User? = snapshot.getValue(User::class.java)
                //Obtener nombre del usuario
                usernameProfileChat.text = user!!.getUsername()

                //Obtener imagen de perfil
                Glide.with(applicationContext).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(imageProfileChat)

                getMessages(firebaseUser!!.uid, uidUserSelected, user.getImage())
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun viewedMessage(userUid : String){
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat!!.getReceiver().equals(firebaseUser!!.uid) && chat!!.getIssuer().equals(userUid)){
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
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for (sn in snapshot.children){
                    val chat = sn.getValue(Chat::class.java)

                    if(chat!!.getReceiver().equals(issuerUid) && chat.getIssuer().equals(receiverUid)
                        || chat.getReceiver().equals(receiverUid) && chat.getIssuer().equals(issuerUid)){
                        (chatList as ArrayList<Chat>).add(chat)
                    }

                    chatAdapter = ChatAdapter(this@MessageActivity, (chatList as ArrayList<Chat>), receiverImage!!)
                    rvChats.adapter = chatAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
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
        val loadingImage = ProgressDialog(this@MessageActivity)
        loadingImage.setMessage("Loading Image...")
        loadingImage.setCanceledOnTouchOutside(false)
        loadingImage.show()

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
                infoMessageImage["idMessage"] = keyMessage
                infoMessageImage["issuer"] = firebaseUser!!.uid
                infoMessageImage["receiver"] = uidUserSelected
                infoMessageImage["message"] = "Submitted image"
                infoMessageImage["url"] = url
                infoMessageImage["viewed"] = false

                reference.child("Chats").child(keyMessage!!).setValue(infoMessageImage)
                    .addOnCompleteListener { task->
                        if (task.isSuccessful){

                            val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
                            userReference.addValueEventListener(object  : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue(User::class.java)
                                    if (notify){
                                        sendNotification(uidUserSelected, user!!.getUsername(),"Submitted image")
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
                Toast.makeText(
                    applicationContext,
                    "The image has been sent successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //Detiene la tarea de actualizar viewed de false a true
    override fun onPause() {
        super.onPause()
        reference!!.removeEventListener(seenListener!!)
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
                intent.putExtra("uid", uidUserSelected)
                startActivity(intent)
                return true
            }else -> super.onOptionsItemSelected(item)
        }
    }
}