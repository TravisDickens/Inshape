package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.travis.inshape.databinding.ActivityNutritionDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class NutritionDetails : AppCompatActivity() {

    private lateinit var binding: ActivityNutritionDetailsBinding
    private val database = FirebaseDatabase.getInstance().reference.child("users")
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        fetchUserCalorieGoalAndDisplay()

        // Fetch and display nutritional data
        fetchAndDisplayNutritionalData()


    }

    private fun fetchAndDisplayNutritionalData() {
        val currentUser = auth.currentUser?.uid
        val currentDate = getCurrentDate() // Get the current date dynamically

        if (currentUser != null) {
            // Reference to the user's nutritional info in Firebase
            val userNutritionalInfoRef = database.child(currentUser).child("nutritionalInfo").child(currentDate)

            userNutritionalInfoRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalCalories = 0.0
                    var totalCarbohydrates = 0.0
                    var totalFats = 0.0
                    var totalProteins = 0.0
                    var totalFibers = 0.0
                    var totalSugars = 0.0

                    if (snapshot.exists()) {
                        // Loop through each meal type (e.g., Breakfast, Lunch)
                        for (mealTypeSnapshot in snapshot.children) {
                            Log.d("NutritionalInfo", "Meal Type: ${mealTypeSnapshot.key}")

                            // Loop through the individual meals under each meal type
                            for (entrySnapshot in mealTypeSnapshot.children) {
                                totalCalories += entrySnapshot.child("calories").getValue(Double::class.java) ?: 0.0
                                totalCarbohydrates += entrySnapshot.child("carbohydrates").getValue(Double::class.java) ?: 0.0
                                totalFats += entrySnapshot.child("fat").getValue(Double::class.java) ?: 0.0
                                totalProteins += entrySnapshot.child("protein").getValue(Double::class.java) ?: 0.0
                                totalFibers += entrySnapshot.child("fibre").getValue(Double::class.java) ?: 0.0
                                totalSugars += entrySnapshot.child("sugars").getValue(Double::class.java) ?: 0.0

                                Log.d("NutritionalInfo", "Calories for ${entrySnapshot.key}: ${entrySnapshot.child("calories").value}")
                            }
                        }

                        // Update the UI with total values
                        binding.caloriesText.text = "Calories (${totalCalories.toInt()} kcal)"

                        binding.caloriesProgress.progress = totalCalories.toInt()

                        binding.carbohydratesText.text = "Carbohydrates (${totalCarbohydrates.toInt()} g)"
                        binding.carbProgress.max = 300
                        binding.carbProgress.progress = totalCarbohydrates.toInt()

                        binding.fatText.text = "Fat (${totalFats.toInt()} g)"
                        binding.fatProgress.max = 70
                        binding.fatProgress.progress = totalFats.toInt()

                        binding.proteinText.text = "Protein (${totalProteins.toInt()} g)"
                        binding.proteinProgress.max = 50
                        binding.proteinProgress.progress = totalProteins.toInt()

                        binding.fiberText.text = "Fiber (${totalFibers.toInt()} g)"
                        binding.fiberProgress.max = 30
                        binding.fiberProgress.progress = totalFibers.toInt()

                        binding.sugarsText.text = "Sugars (${totalSugars.toInt()} g)"
                        binding.sugarsProgress.max = 50
                        binding.sugarsProgress.progress = totalSugars.toInt()

                    } else {
                        Toast.makeText(this@NutritionDetails, "No data found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NutritionalInfo", "Failed to read data: ${error.message}")
                }
            })
        }
    }

    //helper method to get current date
    private fun getCurrentDate(): String {
        // Format the current date as yyyy-MM-dd
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // Fetch user's calorie goal from Firebase and update the recommendation
    private fun fetchUserCalorieGoalAndDisplay() {
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            val userGoalsRef = database.child(currentUser).child("goals")

            userGoalsRef.child("calorieGoal").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyCalorieGoal = snapshot.getValue(String::class.java)?.toDoubleOrNull() ?: 2500.0
                    // Update the recommended calories TextView with the user's goal
                    binding.caloriesRecommendation.text = "Recommended: ${dailyCalorieGoal.toInt()} kcal"
                    binding.caloriesProgress.max = dailyCalorieGoal.toInt()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserGoals", "Failed to fetch calorie goal: ${error.message}")
                }
            })
        }
    }
}
