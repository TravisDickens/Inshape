package com.travis.inshape

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class MainActivity : AppCompatActivity() {

    private val DisplayLength = 3000 // 3 seconds
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("InShapePrefs", MODE_PRIVATE)

        // Delayed execution to navigate based on switch state
        Handler().postDelayed({
            val staySignedIn = sharedPreferences.getBoolean("stay_signed_in", false)
            val nextActivity = if (staySignedIn) {
                Intent(this@MainActivity, HomeActivity::class.java)
            } else {
                Intent(this@MainActivity, LoginActivity::class.java)
            }
            startActivity(nextActivity)
            finish()
        }, DisplayLength.toLong())
    }
}