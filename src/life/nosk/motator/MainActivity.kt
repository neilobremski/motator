package life.nosk.motator

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.util.Log
// import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // val context = this as Context
        val myButton = findViewById(R.id.my_button) as Button
        myButton.setOnClickListener {
            Log.i("MOTATOR", "Button clicked")
            when {
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(this@MainActivity, "Location granted", Toast.LENGTH_SHORT).show()
                } else -> {
                    Log.i("MOTATOR", "Requesting permissions")
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
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
}
