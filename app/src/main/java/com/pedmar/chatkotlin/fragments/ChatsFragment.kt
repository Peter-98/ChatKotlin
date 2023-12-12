package com.pedmar.chatkotlin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.model.ChatsList
import com.pedmar.chatkotlin.model.User

class ChatsFragment : Fragment() {

    private var userAdapter : UserAdapter?= null
    private var userList : List<User>?=null
    private var userListChats : List<ChatsList>?=null

    lateinit var rvChatsList : RecyclerView
    private var firebaseUser : FirebaseUser?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_chats, container, false)

        rvChatsList = view.findViewById(R.id.RV_chatsList)
        rvChatsList.setHasFixedSize(true)
        rvChatsList.layoutManager = LinearLayoutManager(context)

        firebaseUser = FirebaseAuth.getInstance().currentUser
        userListChats = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("MessageList").child(firebaseUser!!.uid)
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (userListChats as ArrayList).clear()
                for (dataSnapshot in snapshot.children){
                    val chatList = dataSnapshot.getValue(ChatsList::class.java)
                    (userListChats as ArrayList).add(chatList!!)
                }
                getChatList()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        return view
    }

    private fun getChatList(){
        userList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("Users")
        reference!!.addValueEventListener(object : ValueEventListener{
            //Leemos en tiempo real
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList).clear()
                for (dataSnapshot in snapshot.children){
                    val user = dataSnapshot.getValue(User::class.java)
                    for(chat in userListChats!!){
                        //Comprobamos si el usuario actual es igual que la lista de chat del otro usuario
                        if(user!!.getUid().equals(chat.getUid())){
                            (userList as ArrayList).add(user!!)
                        }
                    }
                    userAdapter = UserAdapter(context!!, (userList as ArrayList<User>), true)
                    rvChatsList.adapter = userAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

}