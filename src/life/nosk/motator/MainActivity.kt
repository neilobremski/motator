package life.nosk.motator

import android.app.Activity

import android.content.Intent

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.util.Log
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.TextView
import android.preference.PreferenceManager
import android.location.Criteria

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint

class MainActivity : Activity() {

    private var locationManager : LocationManager? = null
    private var locationText : TextView? = null
    private var map : MapView? = null
    private var controller : IMapController? = null

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ctx = getApplicationContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        locationText = findViewById(R.id.txt_location) as TextView

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        map = findViewById(R.id.map) as MapView
        controller = map?.getController()
        controller!!.zoomTo(map!!.getMaxZoomLevel() * 0.65)
        map?.setTileSource(TileSourceFactory.MAPNIK)

        val btnStop = findViewById(R.id.btn_stop) as Button
        btnStop.setOnClickListener {
            Log.i("MOTATOR", "Stop Clicked")
            stopService(Intent(this, MotatorService::class.java))
        }

        val btnShare = findViewById(R.id.btn_share) as Button
        btnShare.setOnClickListener {
            Log.i("MOTATOR", "Share Clicked")
            // startForegroundService() added in API 26 but Debian Android SDK is API 23
            // startForegroundService(this, Intent(this, MotatorService::class.java))
            startService(Intent(this, MotatorService::class.java))
        }

        // val context = this as Context
        val myButton = findViewById(R.id.btn_play) as Button
        myButton.setOnClickListener {
            when {
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(this@MainActivity, "Location granted", Toast.LENGTH_SHORT).show()
                    // use GPS only if enabled otherwise default to "NETWORK"
                    val provider = if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                    locationManager?.requestLocationUpdates(provider, 1L, 0f, locationListener)
                } else -> {
                    Log.i("MOTATOR", "Requesting permissions")
                    requestPermissions(arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        //Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ), 0)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
              permissions: Array<String>, grantResults: IntArray) {
        Log.i("MOTATOR", "Permission results")
        for (i in 0..permissions.size-1) {
            Log.i("MOTATOR", "Permission results [$i]")
            Log.i("MOTATOR", "${permissions[i]} = ${grantResults[i]}")
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationText!!.text = "Location: ${location.longitude}, ${location.latitude}"
            controller!!.setCenter(GeoPoint(location))
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
