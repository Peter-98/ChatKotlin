package com.pedmar.chatkotlin.group

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.chat.MessageGroupActivity
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CreateGroupActivity : AppCompatActivity() {

    private var userAdapter : UserAdapter?=null
    private var userList : List<User>?=null
    private var rvUsers : RecyclerView?=null
    private val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
    private val reference = FirebaseDatabase.getInstance().reference.child("Users").orderByChild("username")
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        var toolbar: Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        // Habilitar la flecha de retroceso
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        }

        rvUsers = findViewById(R.id.RV_users)
        rvUsers!!.setHasFixedSize(true)
        rvUsers!!.layoutManager = LinearLayoutManager(this)

        userList = ArrayList()
        val context = this
        getUsersBd(context)

        // Registrar esta actividad con el Singleton
        ActivityManager.setCreateGroupActivity(this)
    }

    private fun getUsersBd(context: Context?) {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList<User>).clear()
                val myUserReference = FirebaseDatabase.getInstance().reference
                    .child("Users")
                    .child(firebaseUser)

                myUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(myUserSnapshot: DataSnapshot) {
                        val myUser = myUserSnapshot.getValue(User::class.java)

                        for (sh in snapshot.children) {
                            val user: User? = sh.getValue(User::class.java)

                            // Recuperar todos los usuarios excepto el usuario actual y los usuarios privados
                            if (user != null && myUser != null &&
                                !user.getUid().equals(firebaseUser) &&
                                (!user.isPrivate() || myUser.getKnownPrivateUsers().contains(user.getUid()))
                            ) {
                                (userList as ArrayList<User>).add(user)
                            }
                        }

                        //Pasar la lista al adaptador
                        userAdapter = UserAdapter(
                            context!!,
                            userList!!,
                            false,
                            true,
                            false,
                            null
                        )
                        userAdapter!!.setAddGroupButton(findViewById(R.id.addGroup))
                                    // Setear el adaptador al RecyclerView
                                    rvUsers!!.adapter = userAdapter
                    }

                    override fun onCancelled(myUserError: DatabaseError) {
                    }
                })

            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference.addValueEventListener(valueEventListener as ValueEventListener)
    }



    fun createGroup(groupChat: GroupChat) {
        val reference = FirebaseDatabase.getInstance().reference
        val groupInfo = HashMap<String, Any?>()

        groupInfo["uidGroup"] = groupChat.getUidGroup()
        groupInfo["image"] = groupChat.getImage()
        groupInfo["name"] = groupChat.getName()
        groupInfo["uidUsersList"] = groupChat.getUidUsersList()
        groupInfo["colorUsersList"] =  assignRandomColorsToUsers(groupChat.getUidUsersList()!!)

        reference.child("Groups").child(groupChat.getUidGroup()!!).setValue(groupInfo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Una vez creado el grupo, agregar cada usuario del grupo a la lista de mensajes de cada usuario
                    val usersList = groupChat.getUidUsersList()
                    for (userId in usersList!!) {
                        val userMessageListRef = reference.child("MessageList").child(userId).child(groupChat.getUidGroup()!!)
                        userMessageListRef.child("uid").setValue(groupChat.getUidGroup()!!)
                            .addOnCompleteListener { userTask ->
                                if (userTask.isSuccessful) {
                                    // Verificar si se completaron las operaciones para todos los usuarios antes de iniciar la actividad
                                    if (userId == usersList.last()) {
                                        val intent = Intent(this@CreateGroupActivity, MessageGroupActivity::class.java)
                                        intent.putExtra("uidGroup", groupChat.getUidGroup()!!)
                                        startActivity(intent)
                                    }
                                }
                            }
                    }
                }
            }
    }

    private fun assignRandomColorsToUsers(userIds: List<String>): Map<String, Long> {
        val userColorsMap = mutableMapOf<String, Long>()
        val usedColors = mutableSetOf<Long>()

        for (userId in userIds) {
            var randomColor = generateRandomColor().toLong()

            // Verificar si el color generado ya está en uso
            while (usedColors.contains(randomColor)) {
                randomColor = generateRandomColor().toLong()
            }

            // Asignar el color al usuario y agregarlo al conjunto de colores usados
            userColorsMap[userId] = randomColor
            usedColors.add(randomColor)
        }

        return userColorsMap
    }

    private fun generateRandomColor(): Int {
        val random = Random()
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

}