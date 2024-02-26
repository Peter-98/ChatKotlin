package com.pedmar.chatkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ImageView
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
import com.pedmar.chatkotlin.fragments.ChatsFragment
import com.pedmar.chatkotlin.fragments.UsersFragment
import com.pedmar.chatkotlin.model.Chat
import com.pedmar.chatkotlin.model.User
import com.pedmar.chatkotlin.profile.EditImageProfileActivity
import com.pedmar.chatkotlin.profile.ProfileActivity

class MainActivity : AppCompatActivity() {

    var reference : DatabaseReference?=null //No es null
    var firebaseUser : FirebaseUser?=null //No es null
    private lateinit var username : TextView
    private lateinit var qrCode : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeComponents()
        getData()
    }

    private fun initializeComponents(){

        var toolbar : Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        firebaseUser = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        username = findViewById(R.id.username)

        val tabLayout : TabLayout = findViewById(R.id.TabLayoutMain)
        val viewPager : ViewPager = findViewById(R.id.ViewPagerMain)
        qrCode = findViewById(R.id.qrCode)

        val ref = FirebaseDatabase.getInstance().reference.child("Chats")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                //Inicializar adaptador
                val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
                var countUnreadMessage = 0
                for (dataSnapshot in snapshot.children){
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat!!.getReceiver().equals(firebaseUser!!.uid) && !chat.isViewed()){
                        countUnreadMessage+=1
                    }
                }

                //Agregar los fragmentos al adaptador
                if (countUnreadMessage == 0){
                    viewPagerAdapter.addItem(ChatsFragment(), "Chats")
                }else{
                    viewPagerAdapter.addItem(ChatsFragment(), "[$countUnreadMessage] Chats")
                }
                viewPagerAdapter.addItem(UsersFragment(), "Users")

                //Setear el adaptador al viewPager
                viewPager.adapter = viewPagerAdapter
                //Agregar las stats
                tabLayout.setupWithViewPager(viewPager)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        qrCode.setOnClickListener {
            val intent = Intent(applicationContext, QrCodeActivity::class.java)
            intent.putExtra("share", true)
            startActivity(intent)
        }
    }

    private fun getData(){
        reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                //Si el usuario existe
                if (snapshot.exists()){
                    val user : User? = snapshot.getValue(User::class.java)
                    username.text = user!!.getUsername()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    class ViewPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {

        private val fragmentList : MutableList<Fragment> = ArrayList()
        private val titlesList : MutableList<String> = ArrayList()
        override fun getCount(): Int {
          return fragmentList.size
        }

        override fun getItem(position: Int): Fragment {
           return fragmentList[position]
        }

        //Cambiar nombre de la stat
        override fun getPageTitle(position: Int): CharSequence {
            return titlesList[position]
        }

        fun addItem(fragment: Fragment, titulo: String){
            fragmentList.add(fragment)
            titlesList.add(titulo)
        }
    }

    //Ver menu cerrar sesion
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater : MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_principal, menu)
        return true;
    }

    //Seleccionar menu lateral
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_profile->{
                val intent = Intent(applicationContext, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.menu_checkContacts->{
                val intent = Intent(applicationContext, CheckContactsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.menu_about->{
                Toast.makeText(applicationContext, "About...", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_exit->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this@MainActivity, Inicio::class.java)
                Toast.makeText(applicationContext, "You have logged out", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                // Finalizar la actividad actual para que no aparezca en la pila de actividades
                finish()
                return true

            }
            else-> super.onOptionsItemSelected(item)
        }
    }

    private fun updateStatus(status : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
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