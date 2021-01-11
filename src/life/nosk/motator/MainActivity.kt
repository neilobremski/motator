package life.nosk.motator

import android.app.Activity

import android.content.Intent

import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.Toast
import android.widget.LinearLayout
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
import android.view.ViewGroup

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

import kotlin.concurrent.fixedRateTimer

class MainActivity : Activity() {

    private var locationManager : LocationManager? = null
    private var locationText : TextView? = null
    private var statsText : TextView? = null
    private var map : MapView? = null
    private var controller : IMapController? = null
    private val routeLine = Polyline()
    private var lastLocation = 0
    private var startMarker : Marker? = null

    private val timer = fixedRateTimer(period = 1000L) {
        runOnUiThread {
            var appNullable = getApplicationContext() as MotatorApp?
            var sliderNullable = findViewById(R.id.eye_slider) as SeekBar?

            if (appNullable != null && sliderNullable != null) {
                val app = appNullable
                val slider = sliderNullable
                if (app.locations.size > 0 && controller != null) {
                    Log.i("MOTATOR", "Timer center on: ${slider.progress}")
                    val latestLocation = app.locations[app.locations.size-1];
                    locationText?.text = "Location: ${latestLocation}"
                    statsText?.text = "${app.locations.size} GPS points"

                    while (lastLocation < app.locations.size) {
                        Log.i("MOTATOR", "Adding point: ${app.locations[lastLocation]} (${lastLocation})")
                        routeLine.addPoint(GeoPoint(app.locations[lastLocation]))
                        map!!.invalidate()
                        lastLocation += 1
                    }

                    if (startMarker == null) {
                        Log.i("MOTATOR", "Initializing start marker")
                        startMarker = Marker(map)
                        startMarker?.let {
                            it.setPosition(GeoPoint(latestLocation))
                            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            map!!.getOverlays().add(startMarker)
                            if (Build.VERSION.SDK_INT < 21) {
                                @Suppress("DEPRECATION")
                                it.setIcon(getResources().getDrawable(R.drawable.ic_menu_mylocation))
                            } else {
                                it.setIcon(getResources().getDrawable(R.drawable.ic_menu_mylocation, null))
                            }
                            it.title = "Start point"
                        }
                    }

                    controller!!.setCenter(GeoPoint(latestLocation))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = getApplicationContext() as MotatorApp
        Configuration.getInstance().load(app, PreferenceManager.getDefaultSharedPreferences(app))

        locationText = findViewById(R.id.txt_location) as TextView
        statsText = findViewById(R.id.txt_stats) as TextView

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        map = findViewById(R.id.map) as MapView

        Log.i("MOTATOR", "Adding route lines")
        map!!.getOverlayManager().add(routeLine)

        Log.i("MOTATOR", "Setting Map zoom")
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

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        Log.i("MOTATOR", "Configuration changed")
        super.onConfigurationChanged(newConfig)

        val linearLayout = findViewById(R.id.main_layout) as LinearLayout
        var lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 0.5f)

        // Checks the orientation of the screen
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("MOTATOR", "Orientation: Landscape")
            linearLayout.orientation = LinearLayout.HORIZONTAL
            lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT, 0.5f)

        } else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            Log.i("MOTATOR", "Orientation: Portrait")
            linearLayout.orientation = LinearLayout.VERTICAL
        }

        for (i in 0..linearLayout.getChildCount()-1) {
            val child = linearLayout.getChildAt(i)
            child.setLayoutParams(lp)
        }

        linearLayout.invalidate()
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
