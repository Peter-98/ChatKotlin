package com.pedmar.chatkotlin.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.*
import android.widget.RelativeLayout.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.pedmar.chatkotlin.MainActivity
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.chat.DownloadFilesActivity
import com.pedmar.chatkotlin.model.Chat

class ChatAdapter(
    context: Context,
    chatList: List<Chat>,
    imageUrl: String,
    userColorsMap: Map<String, Long>?
) : RecyclerView.Adapter<ChatAdapter.ViewHolder?>() {

    private var userColorsMap: Map<String, Long>?
    private val context: Context
    private val chatList: List<Chat>
    private val imageUrl: String
    private var firebaseUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    init {
        this.context = context
        this.chatList = chatList
        this.imageUrl = imageUrl
        this.userColorsMap = userColorsMap
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageProfileChat: ImageView? = null
        var seeMessage: TextView? = null
        var sentLeftImage: ImageView? = null
        var sentRightImage: ImageView? = null
        var seenMessage: TextView? = null
        var userName: TextView? = null

        init {
            imageProfileChat = itemView.findViewById(R.id.imageProfileChat)
            seeMessage = itemView.findViewById(R.id.seeMessage)
            sentLeftImage = itemView.findViewById(R.id.sendedLeftImage)
            sentRightImage = itemView.findViewById(R.id.sendedRightImage)
            seenMessage = itemView.findViewById(R.id.seenMessage)
            userName = itemView.findViewById(R.id.userName)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        return if (position == 1) {
            val view: View = LayoutInflater.from(context)
                .inflate(com.pedmar.chatkotlin.R.layout.item_right_message, parent, false)
            ViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context)
                .inflate(com.pedmar.chatkotlin.R.layout.item_left_message, parent, false)
            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat: Chat = chatList[position]
        // Obtener el color del usuario desde el mapa de colores
        val userColor = userColorsMap?.get(chat.getIssuer())

        // Si el mensaje es de un grupo y se encuentra en el mapa de colores, asignar el color al fondo del mensaje
        if (userColor != null && chat.isGroupChat() && !chat.getIssuer().equals(firebaseUser.uid)) {

            Glide.with(context).load(chat.getImage()).placeholder(R.drawable.ic_image_chat)
                .into(holder.imageProfileChat!!)

            val messageLayout = holder.itemView.findViewById<LinearLayout>(R.id.messageLayout)

            val drawable = messageLayout?.background?.mutate()
            drawable?.setColorFilter(userColor.toInt(), PorterDuff.Mode.SRC)
            messageLayout?.setBackground(drawable)

            holder.userName!!.text = chat.getUsernameIssuer()
            holder.userName!!.visibility = View.VISIBLE
        } else {
            holder.userName!!.visibility = View.GONE
            Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_image_chat)
                .into(holder.imageProfileChat!!)
        }

        //Copiar mensaje al mantener pulsado
        holder.seeMessage!!.setOnLongClickListener {
            val clipboardManager =
                it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Text", chat.getMessage())
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(it.context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        /* Si el mensaje contiene image*/
        if (chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")) {

            /* Usuario envia una imagen como mensaje*/
            if (chat.getIssuer().equals(firebaseUser!!.uid)) {
                holder.seeMessage!!.visibility = View.GONE
                holder.sentRightImage!!.visibility = View.VISIBLE
                Glide.with(context).load(chat.getUrl()).placeholder(R.drawable.ic_send_image)
                    .into(holder.sentRightImage!!)

                holder.sentRightImage!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("View image", "Delete image")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                viewImage(chat.getUrl())
                            } else if (i == 1) {
                                deleteMessage(position, holder, 1)
                            }
                        })
                    builder.show()
                }
            }
            /* Usuario nos envia una imagen como mensaje*/
            else if (!chat.getIssuer().equals(firebaseUser!!.uid)) {
                holder.seeMessage!!.visibility = View.GONE
                holder.sentLeftImage!!.visibility = View.VISIBLE
                Glide.with(context).load(chat.getUrl()).placeholder(R.drawable.ic_send_image)
                    .into(holder.sentLeftImage!!)

                holder.sentLeftImage!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("View image")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                viewImage(chat.getUrl())
                            }
                        })
                    builder.show()
                }
            }
        } else if (chat.getMessage()!!.contains("File: ") && !chat.getUrl().equals("")) {

            /* Usuario envia un archivo como mensaje*/
            if (chat.getIssuer().equals(firebaseUser!!.uid)) {
                holder.seeMessage!!.text = chat.getMessage()
                holder.sentRightImage!!.visibility = View.VISIBLE
                Glide.with(context).load(R.drawable.ic_file).into(holder.sentRightImage!!)

                holder.sentRightImage!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("Download file", "Delete file")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                val intent = Intent(context, DownloadFilesActivity::class.java)
                                intent.putExtra("uri", chat.getUrl()!!)
                                intent.putExtra(
                                    "name",
                                    chat.getMessage()!!.substringAfter("File: ")
                                )
                                context.startActivity(intent)
                            } else if (i == 1) {
                                deleteMessage(position, holder, 2)
                            }
                        })
                    builder.show()
                }
                /* Usuario nos envia un arhicvo como mensaje*/
            } else if (!chat.getIssuer().equals(firebaseUser!!.uid)) {
                holder.seeMessage!!.text = chat.getMessage()
                holder.sentLeftImage!!.visibility = View.VISIBLE
                Glide.with(context).load(R.drawable.ic_file).into(holder.sentLeftImage!!)

                holder.sentLeftImage!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("Download file")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                val intent = Intent(context, DownloadFilesActivity::class.java)
                                intent.putExtra("uri", chat.getUrl()!!)
                                intent.putExtra(
                                    "name",
                                    chat.getMessage()!!.substringAfter("File: ")
                                )
                                context.startActivity(intent)
                            }
                        })
                    builder.show()
                }
            }

        } else {
            /* Mensaje contiene texto*/
            holder.seeMessage!!.text = chat.getMessage()
            //Se puede eliminar mensaje
            if (firebaseUser!!.uid == chat.getIssuer()) {
                holder.seeMessage!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("Delete message")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    //builder.setTitle("")
                    builder.setItems(
                        options,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            if (i == 0) {
                                deleteMessage(position, holder, 0)
                            }
                        })
                    builder.show()
                }
            }

            // Verificar si el mensaje contiene un enlace
            if (isHyperlink(chat.getMessage()!!) || isCustomAppLink(chat.getMessage()!!)) {
                holder.seeMessage!!.setOnClickListener {
                    val context = holder.itemView.context
                    val options = arrayOf<CharSequence>("Open link with app")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    builder.setItems(options) { dialogInterface, _ ->
                        openLinkInApp(context, chat.getMessage()!!)
                    }
                    builder.show()
                }
            }
        }

        //Mensaje enviado y visto
        if (position == chatList.size - 1) {
            if (chat.isViewed()) {
                holder.seenMessage!!.text = "Viewed"
                if (chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")) {
                   /* val lp: RelativeLayout.LayoutParams =
                        holder.seenMessage!!.layoutParams as LayoutParams
                    //Establecemos la posicion del mensaje de visto
                    lp!!.setMargins(0, 50, 10, 0) //top: 245
                    holder.seenMessage!!.layoutParams = lp*/
                }
            } else {
                holder.seenMessage!!.text = "Sent"
                if (chat.getMessage().equals("Submitted image") && !chat.getUrl().equals("")) {
                   /* val lp: RelativeLayout.LayoutParams =
                        holder.seenMessage!!.layoutParams as LayoutParams
                    //Establecemos la posicion del mensaje de visto
                    lp!!.setMargins(0, 50, 10, 0) //top: 245
                    holder.seenMessage!!.layoutParams = lp*/
                }
            }
        } else {
            holder.seenMessage!!.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].getIssuer().equals(firebaseUser!!.uid)) {
            1
        } else {
            0
        }
    }

    private fun viewImage(image: String?) {
        val imgView: PhotoView
        val btnCloseV: Button
        val dialog = Dialog(context)

        dialog.setContentView(R.layout.dialog_view_image)

        imgView = dialog.findViewById(R.id.imgView)
        btnCloseV = dialog.findViewById(R.id.Btn_close_w)

        Glide.with(context).load(image).placeholder(R.drawable.ic_send_image).into(imgView)

        btnCloseV.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun deleteMessage(position: Int, holder: ChatAdapter.ViewHolder, type: Int) {
        val keyMessage = chatList.get(position).getKeyMessage()!!
        when (type) {
            1 -> {
                val storageReference = FirebaseStorage.getInstance().getReference()
                    .child("Messages_images/$keyMessage.png")
                storageReference.delete().addOnSuccessListener {
                    println("Imagen $keyMessage eliminado exitosamente del almacenamiento de Firebase.")
                }.addOnFailureListener { exception ->
                    println("Error al intentar eliminar la imagen $keyMessage del almacenamiento de Firebase: $exception")
                }
            }

            2 -> {
                val storageReference = FirebaseStorage.getInstance().getReference()
                    .child("Messages_documents/$keyMessage")
                storageReference.delete().addOnSuccessListener {
                    println("Archivo $keyMessage eliminado exitosamente del almacenamiento de Firebase.")
                }.addOnFailureListener { exception ->
                    println("Error al intentar eliminar el archivo $keyMessage del almacenamiento de Firebase: $exception")
                }
            }
            else -> ""
        }
        FirebaseDatabase.getInstance().reference.child("Chats")
            .child(keyMessage)
            .removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(holder.itemView.context, "Deleted message", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "The message has not been deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun isHyperlink(message: String): Boolean {
        val regex = Regex("(http(s)?://[\\w-]+(\\.[\\w-]+)+(/[\\w- ./?%&=]*)?)")
        return regex.find(message) != null
    }

    private fun isCustomAppLink(message: String): Boolean {
        val customAppLinkRegex = Regex("miapp://[\\w-]+(/[\\w-]+)+")
        return customAppLinkRegex.matches(message)
    }

    private fun openLinkInApp(context: Context, link: String) {

        if (isCustomAppLink(link)) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("linkApp", link)
            context.startActivity(intent)
        } else {
            if (URLUtil.isValidUrl(link)) {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(
                        context,
                        "No application found to handle the URL",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

}