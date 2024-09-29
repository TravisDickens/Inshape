package com.travis.inshape

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class MainActivity : AppCompatActivity() {

    private val DisplayLength = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Delayed execution to navigate to LoginActivity after 3 seconds
        Handler().postDelayed({
            val loginIntent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }, DisplayLength.toLong())
    }

}