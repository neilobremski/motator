package life.nosk.motator

/*
fun main() {
    println("Hello world! 0x" + R.layout.activity_main.toString(16))
}
*/

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
