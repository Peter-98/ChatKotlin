package com.pedmar.chatkotlin.notifications

import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseInstanceIdService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task->
                if (task.isSuccessful){
                    if (task.result != null && !TextUtils.isEmpty(task.result)){
                        val myToken : String = task.result!!
                        if (firebaseUser != null){
                            updateToken(myToken)
                        }
                    }
                }
            }
    }

    private fun updateToken(myToken: String?) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().getReference().child("Tokens")
        val token = Token(myToken!!)
        reference.child(firebaseUser!!.uid).setValue(token)

    }

}