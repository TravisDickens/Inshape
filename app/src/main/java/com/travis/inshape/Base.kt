package com.travis.inshape

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class Base : AppCompatActivity(){
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved language from SharedPreferences
        sharedPreferences = getSharedPreferences("InShapePrefs", MODE_PRIVATE)
        val savedLang = sharedPreferences.getString("language", "en") ?: "en"

        // Apply the saved locale
        setLocale(savedLang)
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        // Apply the configuration
        val context = createConfigurationContext(config)
        resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}