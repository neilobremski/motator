package life.nosk.motator

import android.app.Application
import android.util.Log

import android.location.Location

import android.Manifest

import java.util.Calendar

import kotlin.collections.List

class MotatorApp : Application() {

    final public val name = "Motator"

    final public val locations =  mutableListOf<Location>()

    final public val tracks = mutableListOf<List<Pair<Calendar,Location>>>()

    final public val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        // Manifest.permission.WRITE_EXTERNAL_STORAGE,  // save map tiles to internal data/cache
    )

    override fun onCreate() {
        Log.i("MOTATOR", "Application::onCreate() (API level ${android.os.Build.VERSION.SDK_INT})")
    }
}
