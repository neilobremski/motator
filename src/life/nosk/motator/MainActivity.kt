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

import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Calendar
import kotlin.concurrent.fixedRateTimer

class MainActivity : Activity() {

    // UI convenience references
    private var btnStop : Button? = null
    private var btnPlay : Button? = null
    private var btnPause : Button? = null
    private var btnShare : Button? = null
    private var slider : SeekBar? = null

    // stats UI references and formatters
    private var txtMiles : TextView? = null
    private var txtKilometers : TextView? = null
    private var txtPaceMiles : TextView? = null
    private var txtPaceKilometers : TextView? = null
    private var txtStarted : TextView? = null
    private var txtElapsed : TextView? = null
    private val distanceFormatter = DecimalFormat("000.00")
    private val timeFormatter = DecimalFormat("#00")
    private val dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)

    private var locationManager : LocationManager? = null
    private var map : MapView? = null
    private var controller : IMapController? = null
    private var lastLocation = 0
    private var startMarker : Marker? = null
    private var locationMarker : Marker? = null
    private var mileMarkers = mutableListOf<Marker>()
    private var tracking = false

    private val trackLines = mutableListOf<Polyline>()

    private val timer = fixedRateTimer(period = 1000L) {
        runOnUiThread {
            var appNullable = getApplicationContext() as MotatorApp?

            if (appNullable != null && map != null) {
                val app = appNullable
                var moving = false
                var latestLocation : Location? = null
                var startLocation : Location? = null
                var latestCalendar : Calendar? = null
                var startCalendar : Calendar? = null
                var meters = 0.0  // total meters (distance)
                var points = 0  // total GPS points
                var millis = 0L  // total milliseconds
                var milepost = 0.0  // incrementer for marking miles
                var mileIndex = 0  // current mile index (for markers)

                // update map lines to match recorded tracks
                app.tracks.forEachIndexed { iTrack, track ->
                    if (iTrack >= trackLines.size) {
                        var newLine = Polyline()
                        trackLines.add(newLine)
                        map!!.getOverlayManager().add(newLine)
                    }
                    val trackLine = trackLines[iTrack]
                    latestLocation = null
                    track.forEachIndexed { iPoint, calLocPair ->
                        val (cal, loc) = calLocPair
                        points += 1

                        if (iPoint >= trackLine.getActualPoints().size) {
                            trackLine.addPoint(GeoPoint(loc))
                            moving = true
                        }
                        if (latestLocation != null) {
                            val meterIncrement = latestLocation!!.distanceTo(loc)
                            meters += meterIncrement
                            milepost += (meterIncrement * 0.000621371)
                            if (milepost >= 1.0) {
                                if (mileIndex >= mileMarkers.size) {
                                    mileMarkers.add(addMarker(mileIcon(mileIndex), getString(R.string.marker_title_mile)))
                                }
                                milepost -= 1.0
                                mileIndex += 1
                            }
                        }
                        if (latestCalendar != null) {
                            millis += (cal.getTimeInMillis() - latestCalendar!!.getTimeInMillis())
                        }
                        latestLocation = loc
                        latestCalendar = cal
                        if (startLocation == null) {
                            startLocation = loc
                        }
                        if (startCalendar == null) {
                            startCalendar = cal
                        }
                    }
                }

                // update stats
                var km = meters / 1000.0
                var miles = meters * 0.000621371  // km * 0.621371
                txtMiles?.text = distanceFormatter.format(miles)
                txtKilometers?.text = distanceFormatter.format(km)
                if (startCalendar != null) {
                    txtStarted?.text = dateFormatter.format(startCalendar!!.getTime())
                }
                var seconds = (millis / 1000).toInt()
                var minutes = seconds / 60
                seconds -= (minutes * 60)
                txtElapsed?.text = "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
                seconds = (millis / miles).toInt() / 1000
                minutes = seconds / 60
                seconds -= (minutes * 60)
                txtPaceMiles?.text = "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
                seconds = (millis / km).toInt() / 1000
                minutes = seconds / 60
                seconds -= (minutes * 60)
                txtPaceKilometers?.text = "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"

                // enable/disable buttons based on whether or not moving is true
                btnPlay!!.isEnabled = !app.tracking
                btnPause!!.isEnabled = app.tracking

                // initialize starting position marker
                if (moving && startMarker == null && startLocation != null) {
                    startMarker = addMarker(R.drawable.moreinfo_arrow, getString(R.string.marker_title_start))
                }

                // update position marker
                if (moving && latestLocation != null) {
                    if (locationMarker == null) {
                        locationMarker = addMarker(R.drawable.ic_menu_mylocation, getString(R.string.marker_title_location))
                    }
                    locationMarker?.let {
                        it.setPosition(GeoPoint(GeoPoint(latestLocation)))
                    }
                }

                // center map view on current location
                if (moving && latestLocation != null) {
                    controller!!.setCenter(GeoPoint(latestLocation))
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

        // wire up stats UI components to properties
        txtMiles = findViewById(R.id.txt_miles) as TextView
        txtKilometers = findViewById(R.id.txt_kilometers) as TextView
        txtPaceMiles = findViewById(R.id.txt_pace_miles) as TextView
        txtPaceKilometers = findViewById(R.id.txt_pace_kilometers) as TextView
        txtStarted = findViewById(R.id.txt_started) as TextView
        txtElapsed = findViewById(R.id.txt_elapsed) as TextView

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        map = findViewById(R.id.map) as MapView

        Log.i("MOTATOR", "Setting Map zoom")
        controller = map?.getController()
        controller!!.zoomTo(map!!.getMaxZoomLevel() * 0.65)
        map?.setTileSource(TileSourceFactory.MAPNIK)

        btnStop = findViewById(R.id.btn_stop) as Button?
        btnStop!!.setOnClickListener {
            Log.i("MOTATOR", "Stop Clicked")
            stopTracking()
            app.tracks.clear()

            // remove all overlay lines from map view
            val overlays = map!!.getOverlayManager()
            if (locationMarker != null) {
                overlays.remove(locationMarker)
                locationMarker = null
            }
            if (startMarker != null) {
                overlays.remove(startMarker)
                startMarker = null
            }
            for (trackLine in trackLines) {
                overlays.remove(trackLine)
            }
            trackLines.clear()
            for (mileMarker in mileMarkers) {
                overlays.remove(mileMarker)
            }
            mileMarkers.clear()
            map!!.invalidate()
        }

        btnPlay = findViewById(R.id.btn_play) as Button
        btnPlay!!.setOnClickListener {
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

        btnPause = findViewById(R.id.btn_pause) as Button
        btnPause!!.setOnClickListener {
            Log.i("MOTATOR", "Pause Clicked")
            stopTracking()
        }

        btnShare = findViewById(R.id.btn_share) as Button
        btnShare!!.setOnClickListener {
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
            startTracking()
        }
    }

    fun mileIcon(mile : Int) : Int {
        when (mile) {
            0 -> return R.drawable.mile_1
            1 -> return R.drawable.mile_2
            2 -> return R.drawable.mile_3
            3 -> return R.drawable.mile_4
            4 -> return R.drawable.mile_5
            5 -> return R.drawable.mile_6
            6 -> return R.drawable.mile_7
            7 -> return R.drawable.mile_8
            8 -> return R.drawable.mile_9
            9 -> return R.drawable.mile_10
            10-> return R.drawable.mile_11
            11-> return R.drawable.mile_12
            12-> return R.drawable.mile_13
        }
        return R.drawable.ic_menu_mylocation
    }

    fun addMarker(icon : Int, title : String) : Marker {
        Log.i("MOTATOR", "Marker: ${title} (${icon})")
        val marker = Marker(map)
        map!!.getOverlays().add(marker)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        if (Build.VERSION.SDK_INT < 21) {
            @Suppress("DEPRECATION")
            marker.setIcon(getResources().getDrawable(icon))
        } else {
            marker.setIcon(getResources().getDrawable(icon, null))
        }
        marker.title = title
        return marker
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
