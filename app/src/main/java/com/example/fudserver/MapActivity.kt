package com.example.fudserver

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fudserver.databinding.ActivityMapBinding
import com.example.fudserver.model.Order
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

private const val REQUEST_CODE=1094
class MapActivity : AppCompatActivity(),LocationListener {
    private lateinit var binding:ActivityMapBinding
    private val permissions= arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION,
        ACCESS_WIFI_STATE,
        WRITE_EXTERNAL_STORAGE,
        ACCESS_NETWORK_STATE
    )
    private val fine= ACCESS_FINE_LOCATION
    private val coure= ACCESS_COARSE_LOCATION
    private val wifi= ACCESS_WIFI_STATE
    private val storage= WRITE_EXTERNAL_STORAGE
    private val network= ACCESS_NETWORK_STATE
    private lateinit var locationManager: LocationManager
    private var location:Location?=null
    private lateinit var currentMarker:OverlayItem
    private lateinit var nextMarker:OverlayItem
    private var lat=0.0
    private var lon=0.0
    private var currentLocation:GeoPoint?=null
    private lateinit var nextLocation:GeoPoint
    private lateinit var items:ArrayList<OverlayItem>
    private lateinit var mapController: MapController
    private lateinit var markerOverlay: ItemizedIconOverlay<OverlayItem>
    private var distance=""
    private var id=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMapBinding.inflate(layoutInflater)
        Configuration.getInstance().load(this,PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)
        supportActionBar?.hide()
        items= arrayListOf()
        binding.mapview.visibility=View.GONE
        id=intent.getStringExtra("id")!!
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            requestPermissions()
        }else{
            Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_SHORT).show()
        }

    }
    private fun requestPermissions(){
        if (ContextCompat.checkSelfPermission(this,fine) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,coure) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,storage) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,wifi) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,network) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
            location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            getCustomerLocation()
        }
    }

    private fun getCustomerLocation() {
        binding.progress.visibility= View.VISIBLE
        Firebase.database.getReference("Orders").child(id).addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val item=snapshot.getValue(Order::class.java)!!
                lat=item.address?.lat!!.toDouble()
                lon=item.address.lon!!.toDouble()
                nextLocation=GeoPoint(lat,lon)
                binding.progress.visibility=View.GONE
                binding.mapview.visibility=View.VISIBLE
                initMapView()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun initMapView() {
        if (location != null){
            currentLocation= GeoPoint(location?.latitude!!,location?.longitude!!)
        }
        binding.mapview.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapview.setBuiltInZoomControls(true)
        binding.mapview.setMultiTouchControls(true)
        mapController=binding.mapview.controller as MapController
        mapController.setZoom(15.0)
        mapController.setCenter(currentLocation)
        currentMarker= OverlayItem("You","My Location",currentLocation)
        currentMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
        nextMarker= OverlayItem("Delivery Address","Customer Location",nextLocation)
        nextMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
        items.add(currentMarker)
        items.add(nextMarker)
        markerOverlay=ItemizedIconOverlay(items,iconClick,applicationContext)
        binding.mapview.overlays.clear()
        binding.mapview.overlays.add(markerOverlay)
        try{
            drawRoad(currentLocation!!,nextLocation)
            val dist=(currentLocation!!.distanceToAsDouble(nextLocation))/1000
            distance=String.format("%.2f",dist)
            binding.distance.text="Distance:${distance}Km"
        }catch (_:NullPointerException){}
    }

    private fun drawRoad(start:GeoPoint,end:GeoPoint){
        CoroutineScope(Dispatchers.IO).launch {
            val roadManager= OSRMRoadManager(this@MapActivity,BuildConfig.APPLICATION_ID)
            val waypoints= arrayListOf<GeoPoint>()
            waypoints.add(start)
            waypoints.add(end)
            val road=roadManager.getRoad(waypoints)
            if (road.mStatus == Road.STATUS_OK) {
                val roadOverlay = RoadManager.buildRoadOverlay(road)
                binding.mapview.overlays.add(roadOverlay)
                withContext(Dispatchers.Main){
                    binding.mapview.invalidate()
                }
            } else {
                Toast.makeText(this@MapActivity, "Error when loading the road - status=${road.mStatus}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val iconClick=object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
        override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
            AlertDialog.Builder(this@MapActivity)
                .setTitle(item?.title)
                .setIcon(R.drawable.baseline_location_on_24)
                .setMessage(item?.snippet)
                .setPositiveButton("Ok"){dialog,_->
                    dialog.dismiss()
                }
                .show()
            return true
        }

        override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
            return false
        }
    }

    override fun onLocationChanged(location: Location) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            markerOverlay.removeItem(currentMarker)
            currentLocation= GeoPoint(location.latitude,location.longitude)
            mapController.animateTo(currentLocation)
            currentMarker= OverlayItem("You","My Location",currentLocation)
            currentMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
            markerOverlay.addItem(currentMarker)
            binding.mapview.overlays.clear()
            binding.mapview.overlays.add(markerOverlay)
            drawRoad(currentLocation!!,nextLocation)
            try {
                val dist=(currentLocation!!.distanceToAsDouble(nextLocation))/1000
                distance= String.format("%.2f",dist)
                binding.distance.text="Distance: ${distance}Km"
                if (dist<0.2){
                    locationManager.removeUpdates(this)
                }
            }catch (_:NullPointerException){}
        }else{
            Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
    }

    override fun onResume() {
        super.onResume()
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
//            location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//            getCustomerLocation()
//        }else{
//            Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_SHORT).show()
//        }
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
        binding.mapview.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE->{
                if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]
                ==PackageManager.PERMISSION_GRANTED && grantResults[2]==PackageManager.PERMISSION_GRANTED &&
                        grantResults[3]==PackageManager.PERMISSION_GRANTED && grantResults[4]==PackageManager.PERMISSION_GRANTED){
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
                        location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        getCustomerLocation()
                    }else{
                        Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        requestPermissions()
                    }else{
                        Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}