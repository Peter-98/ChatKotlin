package com.pedmar.chatkotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.Chat

class ChatAdapter (context : Context, chatList : List<Chat>, imageUrl : String)
    : RecyclerView.Adapter<ChatAdapter.ViewHolder?>(){

    private val context : Context
    private val chatList : List<Chat>
    private val imageUrl : String
    var firebaseUser : FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    init {
        this.context = context
        this.chatList = chatList
        this.imageUrl = imageUrl
    }


    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        var imageProfileChat : ImageView?= null
        var seeMessage : TextView?= null
        var sendedLeftImage : ImageView?= null
        var sendedRightImage : ImageView?= null
        var seenMessage : TextView?= null

        init{
            imageProfileChat = itemView.findViewById(R.id.imageProfileChat)
            seeMessage = itemView.findViewById(R.id.seeMessage)
            sendedLeftImage = itemView.findViewById(R.id.sendedLeftImage)
            sendedRightImage = itemView.findViewById(R.id.sendedRightImage)
            seenMessage = itemView.findViewById(R.id.seenMessage)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        return if(position == 1){
            val view : View = LayoutInflater.from(context).inflate(com.pedmar.chatkotlin.R.layout.item_right_message, parent, false)
            ViewHolder(view)
        }else{
            val view : View = LayoutInflater.from(context).inflate(com.pedmar.chatkotlin.R.layout.item_left_message, parent, false)
            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       val chat : Chat = chatList[position]
        Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_image_chat).into(holder.imageProfileChat!!)

        /* Si el mensaje contiene image*/
        if(chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")){

            /* Usuario envia una imagen como mensaje*/
            if(chat.getIssuer().equals(firebaseUser!!.uid)){
                holder.seeMessage!!.visibility = View.GONE
                holder.sendedRightImage!!.visibility = View.VISIBLE
                Glide.with(context).load(chat.getUrl()).placeholder(R.drawable.ic_send_image).into(holder.sendedRightImage!!)
            }
            /* Usuario nos envia una imagen como mensaje*/
            else if(!chat.getIssuer().equals(firebaseUser!!.uid)){
                holder.seeMessage!!.visibility = View.GONE
                holder.sendedLeftImage!!.visibility = View.VISIBLE
                Glide.with(context).load(chat.getUrl()).placeholder(R.drawable.ic_send_image).into(holder.sendedLeftImage!!)
            }
        }else{
            /* Mensaje contiene texto*/
            holder.seeMessage!!.text = chat.getMessage()
        }

        //Mensaje enviado y visto
        if(position == chatList.size-1){
           if(chat.isViewed()){
               holder.seenMessage!!.text = "Viewed"
               if(chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")){
                   val lp : RelativeLayout.LayoutParams = holder.seenMessage!!.layoutParams as LayoutParams
                   //Establecemos la posicion del mensaje de visto
                   lp!!.setMargins(0,245,10,0)
                   holder.seenMessage!!.layoutParams = lp
               }
           }else{
               holder.seenMessage!!.text = "Sent"
               if(chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")){
                   val lp : RelativeLayout.LayoutParams = holder.seenMessage!!.layoutParams as LayoutParams
                   //Establecemos la posicion del mensaje de visto
                   lp!!.setMargins(0,245,10,0)
                   holder.seenMessage!!.layoutParams = lp
               }
           }
        }else{
            holder.seenMessage!!.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(chatList[position].getIssuer().equals(firebaseUser!!.uid)){
            1
        }else{
            0
        }
    }
}