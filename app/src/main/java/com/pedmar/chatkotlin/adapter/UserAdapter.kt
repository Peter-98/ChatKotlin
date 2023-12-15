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
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.chat.MessageActivity

class UserAdapter (context: Context, usersList: List<User>, viewedChat : Boolean) : RecyclerView.Adapter<UserAdapter.ViewHolder?>(){

    private val context : Context
    private val usersList : List<User>
    private val viewedChat : Boolean

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

        //inicializar
        init {
            username = itemView.findViewById(R.id.Item_username)
            //userEmail = itemView.findViewById(R.id.Item_user_email)
            userImage = itemView.findViewById(R.id.Item_image)
            statusOnline = itemView.findViewById(R.id.statusOnline)
            statusOffline = itemView.findViewById(R.id.statusOffline)
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
}