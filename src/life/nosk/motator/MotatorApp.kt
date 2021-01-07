package life.nosk.motator

import android.app.Application
import android.util.Log

class MotatorApp : Application() {

    final public val name = "Motator"

    override fun onCreate() {
        Log.i("MOTATOR", "Application::onCreate() (API level ${android.os.Build.VERSION.SDK_INT})")
    }

}
