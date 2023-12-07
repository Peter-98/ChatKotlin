package com.pedmar.chatkotlin.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.User

class MessageActivity : AppCompatActivity() {

    private lateinit var etMessage : EditText
    private lateinit var ibSend : ImageButton
    private lateinit var imageProfileChat : ImageView
    private lateinit var usernameProfileChat : TextView
    var uidUserSelected : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        initializeVariables()
        getUid()
        getDataUserSelected()

        ibSend.setOnClickListener{
            val message = etMessage.text.toString()
            if (message.isEmpty()){
                Toast.makeText(applicationContext, "Please enter a message", Toast.LENGTH_SHORT).show()
            }else{
                sendMessage()
            }
        }
    }

    private fun getUid(){
        intent = intent
        uidUserSelected = intent.getStringExtra("user.uid").toString()
    }

    private fun sendMessage() {

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
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun initializeVariables(){
        etMessage = findViewById(R.id.etMessage)
        ibSend = findViewById(R.id.iBSend)
        imageProfileChat = findViewById(R.id.imageProfileChat)
        usernameProfileChat = findViewById(R.id.usernameProfileChat)
    }
}