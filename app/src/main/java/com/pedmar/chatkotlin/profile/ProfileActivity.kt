package com.pedmar.chatkotlin.profile

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.hbb20.CountryCodePicker
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
    private lateinit var phone : TextView
    private lateinit var btnSave : Button
    private lateinit var provider : TextView
    private lateinit var editPhone : ImageView
    private lateinit var btnVerify : MaterialButton
    private lateinit var checkBoxPrivate: CheckBox

    private var user : FirebaseUser?= null
    private var reference : DatabaseReference?=null

    private var phoneCode = ""
    private var phoneNumber = ""
    private var phoneCodeNumber = ""

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initializeVariables()
        getData()
        statusVerifyAccount()

        btnSave.setOnClickListener{
            updateData()
        }

        image.setOnClickListener{
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

        editPhone.setOnClickListener{
            setNumberPhone()
        }

        btnVerify.setOnClickListener{
            if(user!!.isEmailVerified){
                //Usuario verificado
                //Toast.makeText(applicationContext, "User verified", Toast.LENGTH_SHORT).show()
                verifyAccount()
            }else{
                //Usuario no verificado
                confirmVerify()
            }
        }
    }

    private fun confirmVerify() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify account")
            .setMessage("Send verification instructions to ${user!!.email} ?")
            .setPositiveButton("Send"){d,e->
                sendConfirmationEmail()
            }.setNegativeButton("Cancel"){d,e->
                d.dismiss()
            }.show()
    }

    private fun sendConfirmationEmail() {
        progressDialog.setMessage("Sending verification instructions to email  ${user!!.email}...")
        progressDialog.show()

        user!!.sendEmailVerification().addOnSuccessListener {
            //Envio existoso
            progressDialog.dismiss()
            Toast.makeText(applicationContext,"Instructions sent. Check the email ${user!!.email}",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {e->
            //Envio no exitoso
            progressDialog.dismiss()
            Toast.makeText(applicationContext,"The operation failed due to ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun statusVerifyAccount(){
        if(user!!.isEmailVerified){
            btnVerify.text = "Verified"
        }else{
            btnVerify.text = "Not verified"
        }

    }

    private fun setNumberPhone() {

        /*Declarar vistas del cuadro de dialogo*/
        val setPhoneNumber : EditText
        val setPhoneCode : CountryCodePicker
        val btnAccept : MaterialButton

        val dialog = Dialog(this@ProfileActivity)

        /*Realizar conexion con el diseño*/
        dialog.setContentView(R.layout.dialog_set_phone)

        /*Inicializar las vistas*/
        setPhoneNumber = dialog.findViewById(R.id.setPhone)
        setPhoneCode = dialog.findViewById(R.id.codeSelect)
        btnAccept = dialog.findViewById(R.id.Btn_accept_phone)

        /*Asignar el evento al boton*/
        btnAccept.setOnClickListener{
            phoneCode = setPhoneCode.selectedCountryCodeWithPlus
            phoneNumber = setPhoneNumber.text.toString().trim()
            phoneCodeNumber = phoneCode + phoneNumber
            if(phoneNumber.isEmpty()){
                Toast.makeText(applicationContext, "Enter a phone number",Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }else{
                phone.text =  phoneCodeNumber
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
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
        provider = findViewById(R.id.P_provider)
        editPhone = findViewById(R.id.edit_phone)
        btnVerify = findViewById(R.id.Btn_verify)
        checkBoxPrivate = findViewById(R.id.P_private)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

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
                    provider.text = user!!.getProvider()
                    checkBoxPrivate.isChecked = user!!.isPrivate()
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
        hashmap["private"] = checkBoxPrivate.isChecked

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

    private fun verifyAccount(){
        val btnVerifiedAccount : MaterialButton
        val dialog = Dialog(this@ProfileActivity)

        dialog.setContentView(R.layout.dialog_verify_account)
        btnVerifiedAccount = dialog.findViewById(R.id.Btn_verified_account)
        btnVerifiedAccount.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun updateStatus(status : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(user!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["status"] = status
        reference!!.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        updateStatus("online")
    }
    override fun onPause() {
        super.onPause()
        updateStatus("offline")
    }
}