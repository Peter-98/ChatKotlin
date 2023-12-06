package com.pedmar.chatkotlin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.R

class UsersFragment : Fragment() {

    private var userAdapter : UserAdapter?=null
    private var userList : List<User>?=null
    private var rvUsers : RecyclerView?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_fragmento_usuarios, container, false)

        rvUsers = view.findViewById(R.id.RV_users)
        rvUsers!!.setHasFixedSize(true)
        rvUsers!!.layoutManager = LinearLayoutManager(context)

        userList = ArrayList()
        getUsersBd()

        return view
    }

    private fun getUsersBd() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = FirebaseDatabase.getInstance().reference.child("Users").orderByChild("username")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList<User>).clear()

                //Se recorre la base de datos para guardar en la lista
                for (sh in snapshot.children){
                    val user : User?= sh.getValue(User::class.java)

                    //Recuperar todos los usuarios menos el usuario actual
                    if(!user!!.getUid().equals(firebaseUser)){
                        (userList as ArrayList<User>).add(user)
                    }
                }

                //Pasar la lista al adaptador
                userAdapter = UserAdapter(context!!, userList!!)

                //Seteamos el adaptador al recycleView
                rvUsers!!.adapter = userAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

}