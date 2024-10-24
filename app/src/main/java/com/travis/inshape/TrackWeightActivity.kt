package com.travis.inshape

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class TrackWeightActivity : AppCompatActivity() {

    private lateinit var etCurrentWeight: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var weightViewModel: WeightViewModel
    private lateinit var networkReceiver: NetworkReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_weight)

        etCurrentWeight = findViewById(R.id.etCurrentWeight)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)



        // Initialize the ViewModel using ViewModelProvider
        weightViewModel = ViewModelProvider(this).get(WeightViewModel::class.java)

        networkReceiver = NetworkReceiver { isConnected ->
            if (isConnected) {
                weightViewModel.syncUnsyncedWeights()
            }
        }

        // Register the network receiver
        this.registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        // Observe allWeights LiveData from ViewModel
        weightViewModel.allWeights.observe(this, Observer { weights ->
            // Here you can update the UI or perform actions with the weights
            if (weights.isNotEmpty()) {
                // Do something with the list of weights, e.g., log or display in UI
                Toast.makeText(this, "Weights loaded: ${weights.joinToString(", ")}", Toast.LENGTH_SHORT).show()
                Log.d("Test","Weights loaded: ${weights.joinToString(", ")}")
            } else {
                Toast.makeText(this, "No weights recorded yet", Toast.LENGTH_SHORT).show()
            }
        })

        btnSaveWeight.setOnClickListener {
            saveWeight()
        }
    }

    private fun saveWeight() {
        val currentWeight = etCurrentWeight.text.toString().trim()

        if (currentWeight.isNotEmpty()) {
            val timestamp = System.currentTimeMillis()

            val weight = Weight(
                weight = currentWeight,
                timestamp = timestamp,
                isSynced = false
            )

            // Insert into Room, will sync when online
            weightViewModel.insert(weight)
            Toast.makeText(this, "Weight saved locally", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter your current weight", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the network receiver
        this.unregisterReceiver(networkReceiver)
    }

}
