package com.pedmar.chatkotlin.adapter

import android.app.AlertDialog
import android.content.*
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
import com.google.firebase.storage.FirebaseStorage
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.chat.MessageActivity
import com.pedmar.chatkotlin.chat.MessageGroupActivity
import com.pedmar.chatkotlin.group.SelectDataGroup
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.ChatsList
import com.pedmar.chatkotlin.model.GroupChat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.profile.VisitedProfileActivity
import java.util.logging.Logger

class UserAdapter(
    context: Context,
    usersList: List<User>,
    viewedChat: Boolean,
    createGroup: Boolean,
    groupMembers: Boolean,
    groupList: List<GroupChat>?
) : RecyclerView.Adapter<UserAdapter.ViewHolder?>() {

    private val context: Context
    private val usersList: List<User>
    private val viewedChat: Boolean
    private val createGroup: Boolean
    private val groupMembers: Boolean
    private val groupList: List<GroupChat>
    private var lastMessage: String = ""
    private var countUnreadMessage: Int = 0
    private var countUnreadMessageGroup: Int = 0
    private val logger = Logger.getLogger("MyLogger")

    private val selectedUsers: MutableSet<String> = mutableSetOf()
    private var addGroupButton: FloatingActionButton? = null

    init {
        this.context = context
        this.usersList = usersList
        this.viewedChat = viewedChat
        this.createGroup = createGroup
        this.groupMembers = groupMembers
        this.groupList = (groupList ?: emptyList()) as List<GroupChat>
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var username: TextView
        var userImage: ImageView
        var statusOnline: ImageView
        var statusOffline: ImageView
        var itemLastMessage: TextView
        var statusNotSeenMessages: ImageView
        var numbersNotSeenMessages: TextView

        //inicializar
        init {
            username = itemView.findViewById(R.id.Item_username)
            userImage = itemView.findViewById(R.id.Item_image)
            statusOnline = itemView.findViewById(R.id.statusOnline)
            statusOffline = itemView.findViewById(R.id.statusOffline)
            itemLastMessage = itemView.findViewById(R.id.Item_last_message)
            statusNotSeenMessages = itemView.findViewById(R.id.P_not_seen)
            numbersNotSeenMessages = itemView.findViewById(R.id.P_number_not_seen)
        }
    }

    //Conectar adaptador con el item usuario
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view: View = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
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
            val user: User = usersList[position]
            holder.username.text = user.getUsername()

            //Mientras se carga la imagen se mostrara ic_item_user
            Glide.with(context).load(user.getImage())
                .placeholder(R.drawable.imagen_usuario_visitado)
                .into(holder.userImage)

            // Cambiar la apariencia según si el usuario está seleccionado o no
            if (selectedUsers.contains(user.getUid())) {
                holder.itemView.setBackgroundResource(R.color.teal_200)
            } else {
                holder.itemView.setBackgroundResource(android.R.color.transparent)
            }

            //Elimina el chat
            holder.itemView!!.setOnLongClickListener {
                val options = arrayOf<CharSequence>("Delete chat")
                val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                //builder.setTitle("")
                builder.setItems(
                    options,
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        if (i == 0) {
                            deleteChat(user, null)
                        }
                    })
                builder.show()
                true
            }



            holder.itemView.setOnClickListener {
                if (createGroup) {

                    // Manejar la selección/deselección al hacer clic en el usuario
                    if (selectedUsers.contains(user.getUid())) {
                        selectedUsers.remove(user.getUid())
                    } else {
                        user.getUid()?.let { it1 -> selectedUsers.add(it1) }
                    }

                    // Mostrar el botón  si hay dos o más elementos
                    if (selectedUsers.size >= 2 && addGroupButton != null) {
                        addGroupButton!!.visibility = View.VISIBLE
                    } else {
                        addGroupButton?.visibility = View.GONE
                    }

                    // Notificar al adaptador que los datos han cambiado
                    notifyDataSetChanged()

                } else if (groupMembers) {
                    val options = arrayOf<CharSequence>("Send a message", "View Profile")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                val intent = Intent(context, MessageActivity::class.java)
                                intent.putExtra("userUid", user.getUid())
                                context.startActivity(intent)
                            } else if (i == 1) {
                                val intent = Intent(context, VisitedProfileActivity::class.java)
                                intent.putExtra("uid", user.getUid())
                                context.startActivity(intent)
                            }
                        })
                    builder.show()

                } else {
                    val intent = Intent(context, MessageActivity::class.java)
                    //Se envia el uid del usuario
                    intent.putExtra("userUid", user.getUid())
                    //se llama al chat
                    context.startActivity(intent)
                }
            }

            addGroupButton?.setOnClickListener {
                if (selectedUsers.size >= 2) {
                    var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
                    selectedUsers.add(firebaseUser!!.uid)
                    selectedUsers.sorted()
                    val intent = Intent(context, SelectDataGroup::class.java)
                    intent.putExtra("selectedUsers", selectedUsers.joinToString("-"))
                    context.startActivity(intent)
                }
            }

            if (viewedChat) {
                getLastMessage(user.getUid(), holder.itemLastMessage)
                getNotSeenMessage(holder.statusNotSeenMessages, holder.numbersNotSeenMessages)
            } else {
                holder.itemLastMessage.visibility = View.GONE
            }

            if (viewedChat) {
                if (user.getStatus() == "online") {
                    holder.statusOnline.visibility = View.VISIBLE
                    holder.statusOffline.visibility = View.GONE
                } else {
                    holder.statusOnline.visibility = View.GONE
                    holder.statusOffline.visibility = View.VISIBLE
                }
            } else {
                holder.statusOnline.visibility = View.GONE
                holder.statusOffline.visibility = View.GONE
            }

        } else {// Si es un grupo de chat
            val groupChat: GroupChat = groupList[position - usersList.size]
            holder.username.text = groupChat.getName()
            holder.itemView.setOnClickListener {
                val intent = Intent(context, MessageGroupActivity::class.java)
                //Se envia el uid del usuario
                intent.putExtra("uidGroup", groupChat.getUidGroup())
                //se llama al chat
                context.startActivity(intent)
            }

            //Elimina el chat
            holder.itemView!!.setOnLongClickListener {
                val options = arrayOf<CharSequence>("Delete chat")
                val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                //builder.setTitle("")
                builder.setItems(
                    options,
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        if (i == 0) {
                            deleteChat(null, groupChat)
                        }
                    })
                builder.show()
                true
            }

            if (viewedChat) {
                getLastMessage(
                    groupChat.getUidGroup(), holder.itemLastMessage
                )
                getNotSeenMessageGroup(
                    groupChat,
                    holder.statusNotSeenMessages,
                    holder.numbersNotSeenMessages
                )
                countUnreadMessageGroup = 0
            } else {
                holder.itemLastMessage.visibility = View.GONE
            }

            //Mientras se carga la imagen se mostrara ic_item_user
            Glide.with(context).load(groupChat.getImage())
                .placeholder(R.drawable.imagen_usuario_visitado)
                .into(holder.userImage)
        }

    }

    private fun getLastMessage(userUid: String?, itemLastMessage: TextView) {
        lastMessage = "defaultMessage"
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                    if (firebaseUser != null && chat != null) {
                        if ((chat.getReceiver() == firebaseUser.uid &&
                                    chat.getIssuer() == userUid ||
                                    chat.getReceiver() == userUid &&
                                    chat.getIssuer() == firebaseUser!!.uid)

                            || (chat.isGroupChat() && chat.getReceiver() == userUid)
                        ) {
                            lastMessage = chat.getMessage() ?: ""
                            val maxLength = 30
                            if (lastMessage.length > maxLength) {
                                lastMessage = lastMessage.substring(0, maxLength) + "..."
                            }
                        }
                    }
                }

                when (lastMessage) {
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

    private fun getNotSeenMessage(
        statusNotSeenMessages: ImageView, numbersNotSeenMessages: TextView
    ) {
        countUnreadMessage = 0
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                    if (firebaseUser != null && chat != null) {
                        if (chat!!.getReceiver()
                                .equals(firebaseUser!!.uid) && !chat.isViewed()
                        ) {
                            countUnreadMessage += 1
                        }
                    }
                }
                if (countUnreadMessage != 0) {
                    statusNotSeenMessages.visibility = View.VISIBLE
                    numbersNotSeenMessages.visibility = View.VISIBLE
                    numbersNotSeenMessages.text = countUnreadMessage.toString()
                }
                countUnreadMessage = 0
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getNotSeenMessageGroup(
        groupChat: GroupChat?,
        statusNotSeenMessages: ImageView, numbersNotSeenMessages: TextView
    ) {
        countUnreadMessageGroup = 0
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                    if (firebaseUser != null && chat != null) {
                        val indexUser = groupChat!!.getUidUsersList()!!.indexOf(firebaseUser!!.uid)
                        val groupId = groupChat.getUidGroup()
                        if (chat.getReceiver().equals(groupId) &&
                            chat != null &&
                            indexUser != -1 &&
                            chat!!.getAllViewed()!!.isNotEmpty() &&
                            !chat!!.getAllViewed()?.get(indexUser)!! &&
                            !chat.isViewed()
                        ) {
                            countUnreadMessageGroup += 1
                        }
                    }
                }
                if (countUnreadMessageGroup != 0) {
                    statusNotSeenMessages.visibility = View.VISIBLE
                    numbersNotSeenMessages.visibility = View.VISIBLE
                    numbersNotSeenMessages.text = countUnreadMessageGroup.toString()
                }
                countUnreadMessageGroup = 0
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun deleteChat(user: User?, groupChat: GroupChat?) {

        var uidReference = user?.getUid()?: groupChat!!.getUidGroup()
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        //Elimina el uid (receptor) de tu uid
        FirebaseDatabase.getInstance().reference.child("MessageList").child(firebaseUser!!.uid)
            .child(uidReference!!).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logger.info("MessageList: Usuario $uidReference eliminado de currentUser.")
                }
            }.addOnFailureListener { exception ->
                logger.info("MessageList:: Error al intentar eliminar el usuario $uidReference de la base de datos:  $exception")
            }

        if (user!= null) {

            //Elimina los mensajes de chats individuales
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (dataSnapshot in snapshot.children) {
                        val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                        if (firebaseUser != null && chat != null) {
                            if (chat!!.getReceiver()
                                    .equals(uidReference)
                            ) {
                                deleteMessage(chat)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }else{
            var emptyGroup = false

            //Elimina tu uid del grupo
            FirebaseDatabase.getInstance().reference.child("MessageList").child(uidReference!!)
                .child(firebaseUser.uid).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logger.info("Usuario $uidReference eliminado exitosamente.")
                    }
                }.addOnFailureListener { exception ->
                    logger.info("Error al intentar eliminar el usuario $uidReference de la base de datos:  $exception")
                }

            //Elimina tu participacion del grupo
            val referenceGroup = FirebaseDatabase.getInstance().reference.child("Groups").child(uidReference!!)
            referenceGroup.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val groupChat: GroupChat? = snapshot.getValue(GroupChat::class.java)
                    if (firebaseUser != null && groupChat != null &&
                        groupChat!!.getUidUsersList()!!.contains(firebaseUser.uid) &&
                        groupChat!!.getColorUsersList()!!.contains(firebaseUser.uid)) {

                        val uidUsersMutable = groupChat!!.getUidUsersList()!!.toMutableList()
                        val uidColorsUsersMutable = groupChat!!.getColorUsersList()!!.toMutableMap()
                        uidUsersMutable.remove(firebaseUser.uid)
                        uidColorsUsersMutable.remove(firebaseUser.uid)

                        // Actualizar la lista en Firebase
                        referenceGroup.child("uidUsersList").setValue(uidUsersMutable)
                        referenceGroup.child("colorUsersList").setValue(uidColorsUsersMutable)

                        if(uidUsersMutable.isEmpty()){
                            emptyGroup= true
                        }

                    }

                }
                override fun onCancelled(error: DatabaseError) {
                }
            })

            //Elimina el grupo si esta vacio
            if(emptyGroup){

                //Elimina los mensajes de chats del grupo
                reference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (dataSnapshot in snapshot.children) {
                            val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                            if (firebaseUser != null && chat != null) {
                                if (chat!!.getReceiver()
                                        .equals(uidReference)
                                ) {
                                    deleteMessage(chat)
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })

                //Elimina el grupo
                FirebaseDatabase.getInstance().reference.child("Groups")
                    .child(uidReference!!).removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            emptyGroup = false
                            logger.info("Grupo $uidReference eliminado exitosamente.")
                        }
                    }.addOnFailureListener { exception ->
                        logger.info("Error al intentar eliminar el grupo $uidReference de la base de datos:  $exception")
                    }
            }
        }
    }

    private fun deleteMessage(chat: Chat) {
        val keyMessage = chat.getKeyMessage()!!
        var type = 0
        if (chat.getMessage().equals("Submitted image") && !chat.getUrl()!!.isEmpty()) {
            type = 1
        } else if (chat.getMessage()!!.contains("File: ") && !chat.getUrl()!!.isEmpty()) {
            type = 2
        }
        when (type) {
            1 -> {
                val storageReference = FirebaseStorage.getInstance().getReference()
                    .child("Messages_images/$keyMessage.png")
                storageReference.delete().addOnSuccessListener {
                    logger.info("Imagen $keyMessage eliminado exitosamente del almacenamiento de Firebase.")
                }.addOnFailureListener { exception ->
                    logger.info("Error al intentar eliminar la imagen $keyMessage del almacenamiento de Firebase: $exception")
                }
            }

            2 -> {
                val storageReference = FirebaseStorage.getInstance().getReference()
                    .child("Messages_documents/$keyMessage")
                storageReference.delete().addOnSuccessListener {
                    logger.info("Archivo $keyMessage eliminado exitosamente del almacenamiento de Firebase.")
                }.addOnFailureListener { exception ->
                    logger.info("Error al intentar eliminar el archivo $keyMessage del almacenamiento de Firebase: $exception")
                }
            }
            else -> ""
        }
        FirebaseDatabase.getInstance().reference.child("Chats")
            .child(keyMessage)
            .removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logger.info("Chats: Mensaje $keyMessage eliminado exitosamente.")
                }
            }.addOnFailureListener { exception ->
                logger.info("Chats: Error al intentar eliminar el mensaje $keyMessage de la base de datos:  $exception")
            }
    }
}