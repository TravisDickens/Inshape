package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.travis.inshape.databinding.ActivityNutritionalInfoBinding
import java.text.SimpleDateFormat
import java.util.*

class NutritionalInfoActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityNutritionalInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityNutritionalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data from Intent
        val calories = intent.getDoubleExtra("calories", 0.0)
        val carbohydrates = intent.getDoubleExtra("carbohydrates", 0.0)
        val fat = intent.getDoubleExtra("fat", 0.0)
        val protein = intent.getDoubleExtra("protein", 0.0)
        val fibre = intent.getDoubleExtra("fibre", 0.0)
        val sugars = intent.getDoubleExtra("sugars", 0.0)
        val foodName = intent.getStringExtra("food_name")
        val mealType = intent.getStringExtra("meal_type") ?: "Unknown"
        val mealTime = intent.getStringExtra("meal_time")
        val grams = intent.getIntExtra("grams", 0)

        // Display the retrieved data using ViewBinding
        displayNutritionalData(calories, carbohydrates, protein, fat, fibre, sugars, foodName, mealType, mealTime, grams)

        // Save data to Firebase when save button is clicked
        binding.saveButton.setOnClickListener {
            saveNutritionalDataToFirebase(calories, carbohydrates, fat, protein, fibre, sugars, foodName, mealType, mealTime, grams)
        }
    }

    private fun displayNutritionalData(
        calories: Double,
        carbohydrates: Double,
        protein: Double,
        fat: Double,
        fibre: Double,
        sugars: Double,
        foodName: String?,
        mealType: String,
        mealTime: String?,
        grams: Int
    ) {
        binding.caloriesText.text = "$calories"
        binding.carbohydratesText.text = "${carbohydrates}g"
        binding.proteinText.text = " ${protein}g"
        binding.fatText.text = " ${fat}g"
        binding.fibreText.text = " ${fibre}g"
        binding.sugarsText.text = " ${sugars}g"
        binding.foodNameText.text = " $foodName"
        binding.mealTypeText.text = "$mealType"
        binding.mealTimeText.text = "$mealTime"
        binding.gramsText.text = "$grams"
    }

    private fun saveNutritionalDataToFirebase(
        calories: Double,
        carbohydrates: Double,
        fat: Double,
        protein: Double,
        fibre: Double,
        sugars: Double,
        foodName: String?,
        mealType: String,
        mealTime: String?,
        grams: Int
    ) {
        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Get current date in "yyyy-MM-dd" format
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Firebase database reference
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("nutritionalInfo").child(currentDate)

        // Nutritional data to be stored
        val nutritionalData = mapOf(
            "calories" to calories,
            "carbohydrates" to carbohydrates,
            "fat" to fat,
            "protein" to protein,
            "fibre" to fibre,
            "sugars" to sugars,
            "foodName" to foodName,
            "mealTime" to mealTime,
            "grams" to grams
        )

        // Save the data under the current date node, within the meal type
        userRef.child(mealType).push().setValue(nutritionalData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
