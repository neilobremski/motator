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
import android.widget.SeekBar
import android.preference.PreferenceManager
import android.location.Criteria

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint

import kotlin.concurrent.fixedRateTimer

class MainActivity : Activity() {

    private var locationManager : LocationManager? = null
    private var locationText : TextView? = null
    private var map : MapView? = null
    private var controller : IMapController? = null

    private val timer = fixedRateTimer(period = 1000L) {
        val app = getApplicationContext() as MotatorApp
        runOnUiThread {
            val slider = findViewById(R.id.eye_slider) as SeekBar

            if (app.locations.size > 0 && controller != null) {
                Log.i("MOTATOR", "Timer center on: ${slider.progress}")
                val latestLocation = app.locations[app.locations.size-1];
                controller!!.setCenter(GeoPoint(latestLocation))
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = getApplicationContext() as MotatorApp
        Configuration.getInstance().load(app, PreferenceManager.getDefaultSharedPreferences(app))

        locationText = findViewById(R.id.txt_location) as TextView

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        map = findViewById(R.id.map) as MapView
        controller = map?.getController()
        controller!!.zoomTo(map!!.getMaxZoomLevel() * 0.65)
        map?.setTileSource(TileSourceFactory.MAPNIK)

        val btnStop = findViewById(R.id.btn_stop) as Button
        btnStop.setOnClickListener {
            Log.i("MOTATOR", "Stop Clicked")
            stopTracking()
        }

        val btnShare = findViewById(R.id.btn_share) as Button
        btnShare.setOnClickListener {
            Log.i("MOTATOR", "Share Clicked")
        }

        val btnPlay = findViewById(R.id.btn_play) as Button
        btnPlay.setOnClickListener {
            Log.i("MOTATOR", "Play Clicked")
            when {
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MOTATOR", "Permissions granted")
                    startTracking()                        
                } else -> {
                    Log.i("MOTATOR", "Requesting permissions")
                    requestPermissions(app.requiredPermissions, 0)
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

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        }
    }

    fun startTracking() {
        // startForegroundService() added in API 26 but Debian Android SDK is API 23
        // startForegroundService(this, Intent(this, MotatorService::class.java))
        startService(Intent(this, MotatorService::class.java))
    }

    fun stopTracking() {
        stopService(Intent(this, MotatorService::class.java))
    }
}
