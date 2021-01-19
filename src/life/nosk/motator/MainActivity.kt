package life.nosk.motator

import android.app.Activity

import android.content.Intent

import android.os.Bundle
import android.os.Build
import android.view.View
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
    private var locationMarker : Marker? = null
    private var tracking = false

    private val trackLines = mutableListOf<Polyline>()

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

                    // calculate total distance
                    if (lastLocation < app.locations.size && app.locations.size > 1) {
                        var meters = 0.0
                        for (i in 1..app.locations.size-1) {
                            meters += app.locations[i-1].distanceTo(app.locations[i])
                        }
                        var km = meters / 1000.0
                        var miles = km * 0.621371
                        statsText?.text = "${miles} miles; ${app.locations.size} GPS points"
                    }

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
                            it.setPosition(GeoPoint(GeoPoint(app.locations[0])))
                            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            map!!.getOverlays().add(it)
                            if (Build.VERSION.SDK_INT < 21) {
                                @Suppress("DEPRECATION")
                                it.setIcon(getResources().getDrawable(R.drawable.moreinfo_arrow))
                            } else {
                                it.setIcon(getResources().getDrawable(R.drawable.moreinfo_arrow, null))
                            }
                            it.title = "Start point"
                        }
                    }

                    if (locationMarker == null) {
                        locationMarker = Marker(map)
                        locationMarker?.let {
                            map!!.getOverlays().add(it)
                            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            map!!.getOverlays().add(it)
                            if (Build.VERSION.SDK_INT < 21) {
                                @Suppress("DEPRECATION")
                                it.setIcon(getResources().getDrawable(R.drawable.ic_menu_mylocation))
                            } else {
                                it.setIcon(getResources().getDrawable(R.drawable.ic_menu_mylocation, null))
                            }
                            it.title = "Current Location"
                        }
                    }
                    locationMarker?.let {
                        it.setPosition(GeoPoint(GeoPoint(latestLocation)))
                    }

                    if (tracking == true) {
                        controller!!.setCenter(GeoPoint(latestLocation))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reorient(getResources().getConfiguration().orientation)

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
            app.tracks.clear()

            // remove all overlay lines from map view 
            for (trackLine in trackLines) {
                map!!.getOverlayManager().remove(trackLine)
            }
        }

        val btnPlay = findViewById(R.id.btn_play) as Button
        btnPlay.setOnClickListener {
            Log.i("MOTATOR", "Play Clicked")
            when {
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    startTracking()                        
                } else -> {
                    Log.i("MOTATOR", "Requesting permissions")
                    requestPermissions(app.requiredPermissions, 0)
                }
            }
        }

        val btnPause = findViewById(R.id.btn_pause) as Button
        btnPause.setOnClickListener {
            Log.i("MOTATOR", "Pause Clicked")
            stopTracking()
        }

        val btnShare = findViewById(R.id.btn_share) as Button
        btnShare.setOnClickListener {
            Log.i("MOTATOR", "Share Clicked")
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        Log.i("MOTATOR", "Configuration changed")
        super.onConfigurationChanged(newConfig)
        reorient(newConfig.orientation)
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

    fun reorient(orientation : Int) {
        val linearLayout = findViewById(R.id.main_layout) as LinearLayout
        var lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 0.5f)

        // Checks the orientation of the screen
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("MOTATOR", "Orientation: Landscape")
            linearLayout.orientation = LinearLayout.HORIZONTAL
            lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT, 0.5f)

        } else if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            Log.i("MOTATOR", "Orientation: Portrait")
            linearLayout.orientation = LinearLayout.VERTICAL
        }

        // I wouldn't have to recalculate everything if I was using different layout XML's
        // but refreshing all the UI stuff in a brand new activity XML is a pain too
        for (i in 0..linearLayout.getChildCount()-1) {
            val child = linearLayout.getChildAt(i)
            child.setLayoutParams(lp)
            refreshLayoutRecurse(child)

            // TODO: make this recursive so buttons aren't messed up
        }

        linearLayout.invalidate()
    }

    fun refreshLayoutRecurse(view : View?) {
        view?.let{
            it.invalidate()
            it.requestLayout()

            var viewGroup = it as? ViewGroup?
            if (viewGroup != null) {
                for (i in 0..viewGroup.getChildCount()-1) {
                    refreshLayoutRecurse(viewGroup.getChildAt(i))
                }
            }
        }
    }

    fun startTracking() {
        // startForegroundService() added in API 26 but Debian Android SDK is API 23
        // startForegroundService(this, Intent(this, MotatorService::class.java))
        startService(Intent(this, MotatorService::class.java))
        tracking = true
    }

    fun stopTracking() {
        stopService(Intent(this, MotatorService::class.java))
        tracking = false
    }
}
