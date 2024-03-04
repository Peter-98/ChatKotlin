package com.pedmar.chatkotlin.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.pedmar.chatkotlin.group.CreateGroupActivity
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.model.ChatsList
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.notifications.Token

class ChatsFragment : Fragment() {

    private var userAdapter : UserAdapter?= null
    private var userList : List<User>?=null
    private var usersGroupList : List<GroupChat>?=null
    private var userListChats : List<ChatsList>?=null
    private lateinit var addGroup : ImageButton

    lateinit var rvChatsList : RecyclerView
    private var firebaseUser : FirebaseUser?=null
    private var valueEventListener: ValueEventListener? = null
    private var reference : DatabaseReference?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_chats, container, false)

        rvChatsList = view.findViewById(R.id.RV_chatsList)
        rvChatsList.setHasFixedSize(true)
        rvChatsList.layoutManager = LinearLayoutManager(context)
        addGroup = view.findViewById(R.id.addGroup)

        firebaseUser = FirebaseAuth.getInstance().currentUser
        userListChats = ArrayList()
        reference = FirebaseDatabase.getInstance().reference.child("MessageList").child(firebaseUser!!.uid)
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (userListChats as ArrayList).clear()
                for (dataSnapshot in snapshot.children){

                    val uidData = dataSnapshot.key
                    if (uidData != null) {
                            val chatList = ChatsList(uidData)
                            (userListChats as ArrayList).add(chatList)
                    }
                }
                getChatList()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        addGroup.setOnClickListener {
            val intent = Intent(requireActivity(), CreateGroupActivity::class.java)
            startActivity(intent)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task->
            if (task.isSuccessful){
                if (task.result != null && !TextUtils.isEmpty(task.result)){
                    val token : String = task.result!!
                    updateToken(token)
                }
            }
        }

        return view
    }

    private fun updateToken(token: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token1 = Token(token!!)
        reference.child(firebaseUser!!.uid).setValue(token1)
    }

    private fun getChatList() {
        userList = ArrayList()
        usersGroupList = ArrayList()

        val referenceUsers = FirebaseDatabase.getInstance().reference.child("Users")
        val referenceGroups = FirebaseDatabase.getInstance().reference.child("Groups")

        // ValueEventListener para obtener usuarios en tiempo real
        val valueEventListenerUsers = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList).clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)

                    for(chat in userListChats!!){
                        //Comprobamos si el usuario actual es igual que la lista de chat del otro usuario
                        if(user!!.getUid().equals(chat.getUid())){
                            (userList as ArrayList).add(user!!)
                        }
                    }
                }
                updateAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de cancelación
            }
        }

        // ValueEventListener para obtener grupos en tiempo real
        val valueEventListenerGroups = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {

                    val group = dataSnapshot.getValue(GroupChat::class.java)
                    for(chat in userListChats!!){

                        //Comprobamos si el usuario actual es igual que la lista de chat del otro usuario
                        if(group!!.getUidUsersList()!!.contains(firebaseUser!!.uid)
                            && !group!!.getUidUsersList()!!.contains(chat.getUid())
                            && !((usersGroupList as ArrayList).contains(group!!))){
                            (usersGroupList as ArrayList).add(group!!)
                        }
                    }
                }
                updateAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de cancelación
            }
        }

        // Agregar los ValueEventListener a las referencias correspondientes
        referenceUsers.addValueEventListener(valueEventListenerUsers)
        referenceGroups.addValueEventListener(valueEventListenerGroups)
    }
    private fun updateAdapter() {
        // Verificar si el fragmento está adjunto a una actividad y tiene un contexto válido
        if (isAdded) {
            val context = requireContext() // Obtener el contexto del fragmento
            // Verificar que al menos una de las dos listas no sea nula y no esté vacía
            if ((userList != null && userList!!.isNotEmpty()) || (usersGroupList != null && usersGroupList!!.isNotEmpty())) {
                // Crear el adaptador utilizando el contexto del fragmento
                userAdapter = UserAdapter(context, userList ?: emptyList(), true, false, false,usersGroupList ?: emptyList())
                rvChatsList.adapter = userAdapter
            }
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { reference?.removeEventListener(it) }
    }

}