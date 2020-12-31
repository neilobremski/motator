package life.nosk.motator

import android.app.Activity
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
import android.preference.PreferenceManager;

// import androidx.core.content.ContextCompat

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

class MainActivity : Activity() {

    private var locationManager : LocationManager? = null
    private var thetext : TextView? = null
    private var map : MapView? = null

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ctx = getApplicationContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        thetext = findViewById(R.id.the_text) as TextView
        thetext?.text = "Created"

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        map = findViewById(R.id.map) as MapView
        map?.setTileSource(TileSourceFactory.MAPNIK)

        // val context = this as Context
        val myButton = findViewById(R.id.my_button) as Button
        myButton.setOnClickListener {
            when {
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(this@MainActivity, "Location granted", Toast.LENGTH_SHORT).show()
                    locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1L, 0f, locationListener)
                } else -> {
                    Log.i("MOTATOR", "Requesting permissions")
                    requestPermissions(arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
            thetext!!.text = ("" + location.longitude + ":" + location.latitude)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
