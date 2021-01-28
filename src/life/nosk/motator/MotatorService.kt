package life.nosk.motator

import android.app.Notification  // Notification.Builder only in API 11+
import android.app.PendingIntent
import android.app.Service
import android.content.Intent

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process

import android.util.Log

import android.widget.TextView
import android.widget.Toast
import android.widget.RemoteViews

import java.lang.Thread
import java.util.Calendar

class MotatorService : Service() {

    private val track = mutableListOf<Pair<Calendar, Location>>()

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.i("MOTATOR", "Location #${track.size}: ${location}")
            track.add(Pair<Calendar, Location>(Calendar.getInstance(), location))
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("MOTATOR", "Starting Service")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = Notification()
        notification.contentIntent = pendingIntent
        notification.contentView = RemoteViews(getPackageName(), R.layout.notification)
        notification.icon = R.drawable.running_shoe  // MUST be set or API 28+ won't display
        startForeground(1, notification)

        val app = getApplicationContext() as MotatorApp
        Log.i("MOTATOR", "Add Location Track #${app.tracks.size}")
        app.tracks.add(track)
        app.tracking = true

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        for (provider in locationManager.getProviders(true)) {
            Log.i("MOTATOR", "Enabled Location Provider: ${provider}")
        }

        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                val locationCriteria = android.location.Criteria()
                locationCriteria.accuracy = android.location.Criteria.ACCURACY_FINE
                locationManager.getBestProvider(locationCriteria, true)
            }

        Log.i("MOTATOR", "Request Location Updates (${provider})")
        locationManager.requestLocationUpdates(provider, 1L, 0f, locationListener)

        Log.i("MOTATOR", "Service Started")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Log.i("MOTATOR", "Destroying Service")
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)

        val app = getApplicationContext() as MotatorApp
        app.tracking = false

        super.onDestroy()
        Log.i("MOTATOR", "Service Destroyed")
    }
}
