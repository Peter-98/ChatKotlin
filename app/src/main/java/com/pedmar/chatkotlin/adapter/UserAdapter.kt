package com.pedmar.chatkotlin.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.chat.MessageActivity
import com.pedmar.chatkotlin.model.Chat

class UserAdapter (context: Context, usersList: List<User>, viewedChat : Boolean) : RecyclerView.Adapter<UserAdapter.ViewHolder?>(){

    private val context : Context
    private val usersList : List<User>
    private val viewedChat : Boolean
    var lastMessage : String = ""

    init{
        this.context = context
        this.usersList = usersList
        this.viewedChat = viewedChat
    }

    class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView){
        var username : TextView
        //var userEmail : TextView
        var userImage : ImageView
        var statusOnline : ImageView
        var statusOffline : ImageView
        var itemLastMessage : TextView

        //inicializar
        init {
            username = itemView.findViewById(R.id.Item_username)
            //userEmail = itemView.findViewById(R.id.Item_user_email)
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
        return usersList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       val user : User = usersList[position]
        holder.username.text = user.getUsername()
        //holder.userEmail.text = user.getEmail()

        //Mientras se carga la imagen se mostrara ic_item_user
        Glide.with(context).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(holder.userImage)

        holder.itemView.setOnClickListener{
            val intent = Intent(context, MessageActivity::class.java)

            //Se envia el uid del usuario
            intent.putExtra("userUid", user.getUid())
            Toast.makeText(context, "The selected user is: "+user.getUsername(), Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
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