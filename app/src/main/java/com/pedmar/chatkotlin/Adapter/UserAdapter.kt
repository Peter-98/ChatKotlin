package com.pedmar.chatkotlin.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pedmar.chatkotlin.Model.User
import com.pedmar.chatkotlin.R

class UserAdapter (context: Context, usersList: List<User>) : RecyclerView.Adapter<UserAdapter.ViewHolder?>(){

    private val context : Context
    private val usersList : List<User>

    init{
        this.context = context
        this.usersList = usersList
    }

    class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView){
        var username : TextView
        var userEmail : TextView
        var userImage : ImageView

        //inicializar
        init {
            username = itemView.findViewById(R.id.Item_username)
            userEmail = itemView.findViewById(R.id.Item_user_email)
            userImage = itemView.findViewById(R.id.Item_image)
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
        holder.userEmail.text = user.getEmail()
        //Mientras se carga la imagen se mostrara ic_item_user
        Glide.with(context).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(holder.userImage)
    }
}