package com.pedmar.chatkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.pedmar.chatkotlin.Fragments.ChatsFragment
import com.pedmar.chatkotlin.Fragments.UsersFragment
import com.pedmar.chatkotlin.Model.User

class MainActivity : AppCompatActivity() {

    var reference : DatabaseReference?=null //No es null
    var firebaseUser : FirebaseUser?=null //No es null
    private lateinit var username : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InicializarComponentes()
        ObtenerDato()
    }

    fun InicializarComponentes(){

        var toolbar : Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        firebaseUser = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        username = findViewById(R.id.username)

        val tabLayout : TabLayout = findViewById(R.id.TabLayoutMain)
        val viewPager : ViewPager = findViewById(R.id.ViewPagerMain)

        //Inicializar adaptador
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)

        //Agregar los fragmentos al adaptador
        viewPagerAdapter.addItem(UsersFragment(), "Users")
        viewPagerAdapter.addItem(ChatsFragment(), "Chats")

        //Setear el adaptador al viewPager
        viewPager.adapter = viewPagerAdapter

        //Agregar las stats
        tabLayout.setupWithViewPager(viewPager)
    }

    fun ObtenerDato(){
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                //Si el usuario existe
                if (snapshot.exists()){
                    val user : User? = snapshot.getValue(User::class.java)
                    username.text = user!!.getUsername()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    class ViewPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {

        private val listaFragmentos : MutableList<Fragment> = ArrayList()
        private val listaTitulos : MutableList<String> = ArrayList()
        override fun getCount(): Int {
          return listaFragmentos.size
        }

        override fun getItem(position: Int): Fragment {
           return listaFragmentos[position]
        }

        //Cambiar nombre de la stat
        override fun getPageTitle(position: Int): CharSequence {
            return listaTitulos[position]
        }

        fun addItem(fragment: Fragment, titulo: String){
            listaFragmentos.add(fragment)
            listaTitulos.add(titulo)
        }
    }

    //Ver menu cerrar sesion
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater : MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_principal, menu)
        return true;
    }

    //Seleccionar menu cerrar sesion
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_exit->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this@MainActivity, Inicio::class.java)
                Toast.makeText(applicationContext, "You have logged out", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                return true
            }else-> super.onOptionsItemSelected(item)
        }
    }
}