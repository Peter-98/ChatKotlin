package com.pedmar.chatkotlin.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pedmar.chatkotlin.adapter.UserAdapter
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.R
import com.pedmar.chatkotlin.model.GroupChat

class UsersFragment : Fragment() {

    private var userAdapter : UserAdapter?=null
    private var userList : List<User>?=null
    private var rvUsers : RecyclerView?=null
    private lateinit var etSearchUser : EditText
    private val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
    private val reference = FirebaseDatabase.getInstance().reference.child("Users").orderByChild("username")
    private var valueEventListener: ValueEventListener? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout del fragmento
        val view : View = inflater.inflate(R.layout.fragment_users, container, false)

        rvUsers = view.findViewById(R.id.RV_users)
        rvUsers!!.setHasFixedSize(true)
        rvUsers!!.layoutManager = LinearLayoutManager(context)
        etSearchUser = view.findViewById(R.id.Et_search_user)


        userList = ArrayList()
        val context = requireContext()
        getUsersBd(context)

        etSearchUser.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(userToSearch: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchUser(userToSearch.toString().lowercase())
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        return view
    }

    private fun getUsersBd(context: Context?) {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList<User>).clear()

                if (etSearchUser.text.toString().isEmpty()) {

                    val myUserReference = FirebaseDatabase.getInstance().reference
                        .child("Users")
                        .child(firebaseUser ?: "")

                    myUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(myUserSnapshot: DataSnapshot) {
                            val myUser = myUserSnapshot.getValue(User::class.java)

                            for (sh in snapshot.children) {
                                val user: User? = sh.getValue(User::class.java)

                                // Recuperar todos los usuarios excepto el usuario actual y los usuarios privados que no conocemos
                                if (user != null && myUser != null &&
                                    !user.getUid().equals(firebaseUser) &&
                                    (!user.isPrivate() || myUser.getKnownPrivateUsers().contains(user.getUid()))) {

                                    (userList as ArrayList<User>).add(user)
                                }
                            }

                            // Pasar la lista al adaptador
                            userAdapter = UserAdapter(
                                context!!,
                                userList!!,
                                false,
                                false,
                                false,
                                null
                            )

                            // Setear el adaptador al RecyclerView
                            rvUsers!!.adapter = userAdapter
                        }

                        override fun onCancelled(myUserError: DatabaseError) {
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference?.addValueEventListener(valueEventListener as ValueEventListener)
    }

    private fun searchUser(userToSearch: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val currentUserUid = firebaseUser?.uid

        val myUserReference = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(currentUserUid ?: "")

        myUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(myUserSnapshot: DataSnapshot) {
                val myUser = myUserSnapshot.getValue(User::class.java)

                val consult = FirebaseDatabase.getInstance().reference
                    .child("Users")
                    .orderByChild("search")

                consult.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        (userList as ArrayList<User>).clear()

                        for (sh in snapshot.children) {
                            val user: User? = sh.getValue(User::class.java)

                            if (user != null && myUser != null &&
                                !user.getUid().equals(currentUserUid) &&
                                (!user.isPrivate() || myUser.getKnownPrivateUsers().contains(user.getUid())) &&
                                (user.getName()!!.contains(userToSearch, ignoreCase = true) ||
                                        user.getSurnames()!!.contains(userToSearch, ignoreCase = true) ||
                                        user.getLocation().contains(userToSearch, ignoreCase = true) ||
                                        user.getSearch()!!.contains(userToSearch, ignoreCase = true))
                            ) {
                                (userList as ArrayList<User>).add(user)
                            }
                        }

                        // Pasar la lista al adaptador
                        userAdapter = UserAdapter(
                            context!!,
                            userList!!,
                            false,
                            false,
                            false,
                            null
                        )

                        // Setear el adaptador al RecyclerView
                        rvUsers!!.adapter = userAdapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Manejar errores de cancelación
                    }
                })
            }

            override fun onCancelled(myUserError: DatabaseError) {
                // Manejar errores al obtener el usuario actual
            }
        })
    }




    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { reference?.removeEventListener(it) }
    }

}