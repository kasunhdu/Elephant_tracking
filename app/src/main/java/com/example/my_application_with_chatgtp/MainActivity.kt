package com.example.my_application_with_chatgtp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var btnReportElephant: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)

        btnReportElephant = findViewById(R.id.btnReportElephant)
        btnReportElephant.setOnClickListener {
            sendLocationToFirestore()
        }


        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnZoomIn.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomIn()) // Zoom in
        }

        btnZoomOut.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomOut()) // Zoom out
        }

        // Initialize Location Provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }




    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        fetchLocationsFromFirestore()
        mMap.uiSettings.isZoomControlsEnabled = false // Default zoom controls


        // Enable My Location Layer (if permissions are granted)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun fetchLocationsFromFirestore() {
        db.collection("elephants-yala").document("Biso-Menike")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    document.data?.forEach { (key, value) ->
                        if (value is List<*> && value.size == 2) {
                            val latitude = value[0] as? Double
                            val longitude = value[1] as? Double
                            if (latitude != null && longitude != null) {
                                val location = LatLng(latitude, longitude)
                                mMap.addMarker(MarkerOptions().position(location).title(key))
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))

                            }
                        }
                    }
                }
                val db = FirebaseFirestore.getInstance()

                db.collection("your_collection_name")
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            Log.d("FirebaseData", "Document: ${document.id} => ${document.data}")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseError", "Error fetching data", exception)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendLocationToFirestore() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLocation = hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("reported-elephants") // Change collection name as needed
                    .add(userLocation)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Elephant location reported!", Toast.LENGTH_SHORT).show()
                        Log.d("Firestore", "Location saved: ${location.latitude}, ${location.longitude}")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send location!", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore", "Error adding document", e)
                    }
            } else {
                Toast.makeText(this, "Could not get location!", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                mMap.addMarker(MarkerOptions().position(userLocation).title("You are here!"))



            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
