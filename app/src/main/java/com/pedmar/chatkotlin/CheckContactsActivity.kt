package com.pedmar.chatkotlin

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.User

class CheckContactsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchContactsIntent()
        finish()
    }

    private fun dispatchContactsIntent() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED ) {
            getContacts(applicationContext)
        } else {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchContactsIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private fun getContacts(context: Context) {
        val contactsList = mutableListOf<Contact>()

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor != null) {

            while (cursor.moveToNext()) {

                // Obtener el índice de la columna DISPLAY_NAME
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                var id = ""
                var name = ""
                if(idIndex >= 0
                    && nameIndex >= 0){
                    id = cursor.getString(idIndex)
                    name = cursor.getString(nameIndex)
                }

                // Obtener el correo electrónico
                var email = ""
                val emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                if (emailCursor != null && emailCursor.moveToFirst()) {
                    val emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                    if(emailIndex > 0){
                        email = emailCursor.getString(emailIndex)
                    }
                    emailCursor.close()
                }

                // Obtener el teléfono
                var phoneNumber = ""
                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    val phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if(phoneIndex > 0){
                        phoneNumber = phoneCursor.getString(phoneIndex)
                    }
                    phoneCursor.close()
                }

                val contact = Contact(name, phoneNumber, email, true)
                contactsList.add(contact)
            }
            cursor.close()
        }
        importUsersForMe(getExistContact(contactsList))
    }

    private fun getExistContact(contactsList : List<Contact>): ArrayList<String> {
        val usersList = ArrayList<String>()
        val usersReference = FirebaseDatabase.getInstance().reference.child("Users")
        usersReference.addValueEventListener(object  : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        for (contact in contactsList) {
                            if((contact.email == user.getEmail()
                                || contact.phoneNumber == user.getPhone() && user.isPrivate()))
                                usersList.add(user.getUid()!!)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        return usersList
    }

    private fun importUsersForMe(usersList : ArrayList<String>){
        val saveUsers = ArrayList<String>()
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = firebaseUser?.let {
            FirebaseDatabase.getInstance().reference.child("Users").child(
                it.uid)
        }
        reference!!.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null && !usersList.isEmpty()){
                    for (usersToCheck in usersList){
                        if(!user.getKnownPrivateUsers().contains(usersToCheck)){
                            saveUsers.add(usersToCheck)
                        }
                    }
                    val hashMap = HashMap<String, Any>()
                    hashMap["knownPrivateUsers"] = user.getKnownPrivateUsers() + saveUsers
                    dataSnapshot.ref.updateChildren(hashMap)
                    if(saveUsers.size >0){
                        Toast.makeText(applicationContext,"${saveUsers.size} contacts have been imported successfully", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(applicationContext,"All contacts are imported", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(applicationContext,"All contacts are imported", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // Clase para representar un contacto
    data class Contact(val name: String, val phoneNumber: String, val email: String, val register: Boolean)

















}