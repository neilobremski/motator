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

import android.widget.Toast

import java.lang.Thread

class MotatorService : Service() {

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.i("MOTATOR", "Location: ${location.longitude}, ${location.latitude}")
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

        val notification = Notification.Builder(this)
            .setContentTitle("Fuzzy Wuzzy")
            .setContentText("Was a bear")
            // .setSmallIcon(R.drawable.ic_android_24dp)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        locationManager.requestLocationUpdates(provider, 1L, 0f, locationListener)

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
        super.onDestroy()
    }
}
