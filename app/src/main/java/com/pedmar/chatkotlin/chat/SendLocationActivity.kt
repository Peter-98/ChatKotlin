package com.pedmar.chatkotlin.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class SendLocationActivity  : AppCompatActivity(){

    private var firebaseUser : FirebaseUser? = FirebaseAuth.getInstance().currentUser

    // Declara una variable para el proveedor de ubicación fusionada
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dispatchPickLocationIntent()
    }

    private fun dispatchPickLocationIntent() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationClient.requestLocationUpdates(
                LocationRequest.create(),
                locationCallback,
                Looper.getMainLooper())
        }else{
            requestAccessFineLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    }


    private fun getLocationMap(latitude: Double?, longitude: Double?) {

        val zoom = 15 // Nivel de zoom deseado para el mapa estático

        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap?center=$latitude,$longitude&zoom=$zoom&size=600x300&markers=color:red%7C$latitude,$longitude"

        val intent = Intent()
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        intent.putExtra("url", staticMapUrl)

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    // Define un objeto de devolución de llamada para recibir actualizaciones de ubicación
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            // Aquí puedes obtener la ubicación actual
            val location = locationResult.lastLocation
            var latitude = location?.latitude
            var longitude = location?.longitude
            getLocationMap(latitude, longitude)
        }
    }


    private val requestAccessFineLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchPickLocationIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private val requestAccessCoarseLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission_granted->
            if (permission_granted){
                dispatchPickLocationIntent()
            }else{
                Toast.makeText(applicationContext,"Permission has not been granted", Toast.LENGTH_SHORT).show()
                finish()
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