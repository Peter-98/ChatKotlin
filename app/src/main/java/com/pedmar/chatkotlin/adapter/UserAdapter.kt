package com.pedmar.chatkotlin.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.SelectDataGroup
import com.pedmar.chatkotlin.chat.MessageActivity
import com.pedmar.chatkotlin.chat.MessageGroupActivity
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.GroupChat
import java.security.acl.Group

class UserAdapter(
    context: Context,
    usersList: List<User>,
    viewedChat: Boolean,
    createGroup: Boolean,
    groupList: List<GroupChat>?
) : RecyclerView.Adapter<UserAdapter.ViewHolder?>(){

    private val context : Context
    private val usersList : List<User>
    private val viewedChat : Boolean
    private val createGroup : Boolean
    private val groupList : List<GroupChat>
    var lastMessage : String = ""

    private val selectedUsers: MutableSet<String> = mutableSetOf()
    private var addGroupButton: FloatingActionButton? = null

    init{
        this.context = context
        this.usersList = usersList
        this.viewedChat = viewedChat
        this.createGroup = createGroup
        this.groupList = (groupList ?: emptyList()) as List<GroupChat>
    }

    class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView){
        var username : TextView
        var userImage : ImageView
        var statusOnline : ImageView
        var statusOffline : ImageView
        var itemLastMessage : TextView

        //inicializar
        init {
            username = itemView.findViewById(R.id.Item_username)
            userImage = itemView.findViewById(R.id.Item_image)
            statusOnline = itemView.findViewById(R.id.statusOnline)
            statusOffline = itemView.findViewById(R.id.statusOffline)
            itemLastMessage = itemView.findViewById(R.id.Item_last_message)
        }
    }

    //Conectar adaptador con el item usuario
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view : View = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usersList.size + groupList.size
    }

    fun setAddGroupButton(button: FloatingActionButton) {
        addGroupButton = button
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < usersList.size) { // Si es un usuario
            val user : User = usersList[position]
            holder.username.text = user.getUsername()

            //Mientras se carga la imagen se mostrara ic_item_user
            Glide.with(context).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(holder.userImage)

            // Cambiar la apariencia según si el usuario está seleccionado o no
            if (selectedUsers.contains(user.getUid())) {
                holder.itemView.setBackgroundResource(R.color.teal_200)
            } else {
                holder.itemView.setBackgroundResource(android.R.color.transparent)
            }

            holder.itemView.setOnClickListener{
                if(createGroup){

                    // Manejar la selección/deselección al hacer clic en el usuario
                    if (selectedUsers.contains(user.getUid())) {
                        selectedUsers.remove(user.getUid())
                    } else {
                        user.getUid()?.let { it1 -> selectedUsers.add(it1) }
                    }

                    // Mostrar el botón de acción flotante si hay dos o más elementos seleccionados y addGroupButton no es nulo
                    if (selectedUsers.size >= 2 && addGroupButton != null) {
                        addGroupButton!!.visibility = View.VISIBLE
                    } else {
                        addGroupButton?.visibility = View.GONE
                    }

                    // Notificar al adaptador que los datos han cambiado
                    notifyDataSetChanged()
                }else{
                    val intent = Intent(context, MessageActivity::class.java)
                    //Se envia el uid del usuario
                    intent.putExtra("userUid", user.getUid())
                    //se llama al chat
                    context.startActivity(intent)
                }
            }

            addGroupButton?.setOnClickListener {
                if(selectedUsers.size >= 2){
                    var firebaseUser : FirebaseUser?= FirebaseAuth.getInstance().currentUser
                    selectedUsers.add(firebaseUser!!.uid)
                    selectedUsers.sorted()
                    val intent = Intent(context, SelectDataGroup::class.java)
                    intent.putExtra("selectedUsers", selectedUsers.joinToString("-"))
                    println("lista de usuarios seleccionados$selectedUsers")
                    context.startActivity(intent)
                }
            }

            if(viewedChat){
                getLastMessage(user.getUid(), holder.itemLastMessage)
            }else{
                holder.itemLastMessage.visibility = View.GONE
            }

            if(viewedChat){
                if(user.getStatus() == "online"){
                    holder.statusOnline.visibility = View.VISIBLE
                    holder.statusOffline.visibility = View.GONE
                }else{
                    holder.statusOnline.visibility = View.GONE
                    holder.statusOffline.visibility = View.VISIBLE
                }
            }else{
                holder.statusOnline.visibility = View.GONE
                holder.statusOffline.visibility = View.GONE
            }

        }else{// Si es un grupo de chat
            val groupChat : GroupChat = groupList[position - usersList.size]
            holder.username.text = groupChat.getName()
            holder.itemView.setOnClickListener{
                val intent = Intent(context, MessageGroupActivity::class.java)
                //Se envia el uid del usuario
                intent.putExtra("uidGroup", groupChat.getUidGroup())
                //se llama al chat
                context.startActivity(intent)
            }
            if(viewedChat){
                getLastMessage(groupChat.getUidGroup(), holder.itemLastMessage)
            }else{
                holder.itemLastMessage.visibility = View.GONE
            }

            //Mientras se carga la imagen se mostrara ic_item_user
            Glide.with(context).load(groupChat.getImage()).placeholder(R.drawable.ic_item_user).into(holder.userImage)
        }

    }

    // Método para obtener los usuarios seleccionados
    fun getSelectedUsers(): Set<String> {
        return selectedUsers
    }

    private fun getLastMessage(userUid: String?, itemLastMessage: TextView) {
        lastMessage = "defaultMessage"
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val chat : Chat?= dataSnapshot.getValue(Chat::class.java)
                    if(firebaseUser!= null && chat!= null){
                        if (chat.getReceiver() == firebaseUser.uid &&
                            chat.getIssuer() == userUid ||
                            chat.getReceiver() == userUid &&
                            chat.getIssuer() == firebaseUser!!.uid){
                            lastMessage = chat.getMessage()!!
                        }
                    }
                }
                when(lastMessage){
                    "defaultMessage" -> itemLastMessage.text = "There is no message"
                    "Image has been sent" -> itemLastMessage.text = "Submitted image"
                    else -> itemLastMessage.text = lastMessage
                }
                lastMessage = "defaultMessage"
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}