package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TrackWeightActivity : AppCompatActivity() {

    private lateinit var etCurrentWeight: EditText
    private lateinit var btnSaveWeight: Button
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_weight)
        etCurrentWeight = findViewById(R.id.etCurrentWeight)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)

        btnSaveWeight.setOnClickListener {
            saveWeight()
        }
    }

    private fun saveWeight() {
        val currentWeight = etCurrentWeight.text.toString().trim()

        if (currentWeight.isNotEmpty()) {
            val userId = auth.currentUser?.uid
            val database = FirebaseDatabase.getInstance().getReference("users")

            val weightData = mapOf(
                "weight" to currentWeight,
                "timestamp" to System.currentTimeMillis()
            )

            // Save weight data under the current user's node
            userId?.let {
                database.child(it).child("WeightData").push().setValue(weightData)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Weight saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to save weight. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } else {
            Toast.makeText(this, "Please enter your current weight", Toast.LENGTH_SHORT).show()
        }
    }
}
