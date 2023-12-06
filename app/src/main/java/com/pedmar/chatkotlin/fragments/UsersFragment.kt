package com.pedmar.chatkotlin.fragments

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

class UsersFragment : Fragment() {

    private var userAdapter : UserAdapter?=null
    private var userList : List<User>?=null
    private var rvUsers : RecyclerView?=null
    private lateinit var etSearchUser : EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_fragmento_usuarios, container, false)

        rvUsers = view.findViewById(R.id.RV_users)
        rvUsers!!.setHasFixedSize(true)
        rvUsers!!.layoutManager = LinearLayoutManager(context)
        etSearchUser = view.findViewById(R.id.Et_search_user)


        userList = ArrayList()
        getUsersBd()

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

    private fun getUsersBd() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = FirebaseDatabase.getInstance().reference.child("Users").orderByChild("username")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList<User>).clear()

                if(etSearchUser.text.toString().isEmpty()){
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
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    //Se actualiza la busqueda dependiendo del termino de entrada
    private fun searchUser(userToSearch : String){
        val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
        val consult = FirebaseDatabase.getInstance().reference.child("Users").orderByChild("search")
            .startAt(userToSearch).endAt(userToSearch + "\uf8ff")
        consult.addValueEventListener(object : ValueEventListener{
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