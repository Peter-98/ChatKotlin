package com.pedmar.chatkotlin.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.User
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var image : ImageView
    private lateinit var username : TextView
    private lateinit var email : TextView
    private lateinit var name : EditText
    private lateinit var surnames : EditText
    private lateinit var age : EditText
    private lateinit var phone : EditText
    private lateinit var btnSave : Button
    private lateinit var editImage : ImageView


    var user : FirebaseUser?= null
    var reference : DatabaseReference?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initializeVariables()
        getData()
        btnSave.setOnClickListener{
            updateData()
        }

        editImage.setOnClickListener{
            val intent = Intent(applicationContext, EditImageProfileActivity::class.java)

            // Obtén el Drawable de la ImageView
            val drawable = image.drawable

            // Convierte el Drawable a un Bitmap si es posible
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap

                // Convierte el Bitmap a ByteArray
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()

                // Pasa el ByteArray a la actividad
                intent.putExtra("imageByteArray", byteArray)
            }
            startActivity(intent)
        }
    }

    private fun initializeVariables(){
        image = findViewById(R.id.P_image)
        username = findViewById(R.id.P_username)
        email = findViewById(R.id.P_email)
        name = findViewById(R.id.P_name)
        surnames = findViewById(R.id.P_surnames)
        age = findViewById(R.id.P_age)
        phone = findViewById(R.id.P_phone)
        btnSave = findViewById(R.id.Btn_save)
        editImage = findViewById(R.id.P_Edit_image)

        user = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().reference.child("Users").child(user!!.uid)
    }

    private fun getData(){
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){

                    //Obtener datos de firebase y seteamos
                    val user : User?= snapshot.getValue(User::class.java)

                    username.text = user!!.getUsername()
                    email.text = user!!.getEmail()
                    name.setText(user!!.getName())
                    surnames.setText(user!!.getSurnames())
                    age.setText(user!!.getAge())
                    phone.setText(user!!.getPhone())
                    Glide.with(applicationContext).load(user.getImage()).placeholder(R.drawable.ic_item_user).into(image)

                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun updateData(){

        val hashmap = HashMap<String, Any>()
        hashmap["name"] = name.text.toString()
        hashmap["surnames"] = surnames.text.toString()
        hashmap["age"] = age.text.toString()
        hashmap["phone"] = phone.text.toString()

        reference!!.updateChildren(hashmap).addOnCompleteListener{task->
            if (task.isSuccessful){
                Toast.makeText(applicationContext, "Data updated successfully", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(applicationContext, "Error updating data", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{e->
            Toast.makeText(applicationContext, "An error has occurred ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}