package com.travis.inshape

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.travis.inshape.databinding.ActivityHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), SensorEventListener {

    private var totalWaterIntake = 0
    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var stepsTaken = 0
    private var isFirstSensorEvent = true
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 1
    private var caloriesBurned = 0.0

    private var lastUpdatedDate: String? = null



    private lateinit var binding: ActivityHomeBinding

    // Firebase references
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestActivityRecognitionPermission()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        database = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Load current step count from Firebase
        loadStepsFromFirebase()


        // Initialize water intake when the app starts
        initWaterIntake()

        // Add 20ml when button is clicked
        binding.addWaterButton.setOnClickListener {
            updateWaterIntake(200)
        }

        binding.cardCalorieIntake.setOnClickListener {
            val intent = Intent(this, NutritionDetails::class.java)
            startActivity(intent)
        }

        binding.cardWaterDetails.setOnClickListener {
            val intent = Intent(this, WaterIntakeDetailsActivity::class.java)
            startActivity(intent)
        }

        binding.CardSteps.setOnClickListener {
            val intent = Intent(this, StepDetailsActivity::class.java)
            startActivity(intent)
        }

        binding.CardBurnedCalories.setOnClickListener {
            val intent = Intent(this, BurntCaloriesDetailsActivity::class.java)
            startActivity(intent)
        }

        setupBottomNavigation()
        setupFabClickListener()

        // Fetch and display calories
        fetchAndDisplayCalories()

        // Fetch and set the step goal
        fetchStepGoal()
    }





    private fun setupBottomNavigation() {
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    // Already on HomeActivity, no action needed
                    true
                }
                R.id.Food -> {
                    val intent = Intent(this, NutritionActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.videos -> {
                    val intent = Intent(this, VideosActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }


    private fun setupFabClickListener() {
        binding.fab.setOnClickListener {
            showBottomDialog()
        }
    }


    private fun showBottomDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheetlayout)
        val weightLayout = dialog.findViewById<LinearLayout>(R.id.weightlayout)

        weightLayout.setOnClickListener {
            showToast("Coming soon")
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = com.google.android.material.R.style.MaterialAlertDialog_Material3_Animation
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun loadStepsFromFirebase() {
        val currentDate = getCurrentDate()
        val dailyDataRef = database.child("dailyData").child(currentDate)

        // Fetch steps and calories
        dailyDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Load steps
                    stepsTaken = snapshot.child("steps").getValue(Int::class.java) ?: 0
                    lastUpdatedDate = currentDate // Update last updated date
                    Log.d("Firebase", "Loaded steps: $stepsTaken")
                    binding.stepsTaken.text = "$stepsTaken"

                    // Load calories
                    val storedCalories = snapshot.child("calories").getValue(Double::class.java) ?: 0.0
                    Log.d("Firebase", "Loaded calories: $storedCalories")
                    binding.caloriesTextView.text = String.format("%.2f Kcal", storedCalories)
                }

                // Register the sensor listener after loading steps
                registerSensorListener()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading dailyData", error.toException())
                // Still register the sensor listener even if Firebase fails
                registerSensorListener()
            }
        })
    }


    private fun resetStepsForNewDay(currentDate: String) {
        // Check if it's a new day
        if (lastUpdatedDate != null && lastUpdatedDate != currentDate) {
            stepsTaken = 0 // Reset steps taken for the new day
            Log.d("StepsReset", "New day detected. Resetting steps to 0.")

            // Update Firebase with reset values
            saveStepData(stepsTaken, 0.0)

            // Update UI to reflect reset values
            binding.stepsTaken.text = "0"
            binding.caloriesTextView.text = String.format("%.2f Kcal", 0.0)
        } else {

            stepsTaken = 0 // Default value if no steps (could be updated elsewhere)
            binding.stepsTaken.text = "0"
            binding.caloriesTextView.text = String.format("%.2f Kcal", 0.0)
        }

        // Update last updated date regardless of whether it's a new day
        lastUpdatedDate = currentDate // Update last updated date
    }




    private fun registerSensorListener() {
        running = true
        val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No step counter sensor detected on device", Toast.LENGTH_SHORT).show()
            Log.e("Sensor", "Step Counter sensor not available")
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            Log.d("Sensor", "Step Counter sensor listener registered")
        }
    }


    private fun requestActivityRecognitionPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start tracking steps
                Toast.makeText(this, "Activity Recognition Permission Granted", Toast.LENGTH_SHORT).show()
                registerSensorListener()
            } else {
                Toast.makeText(this, "Activity Recognition Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (!running) {
            registerSensorListener()
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (running && event != null) {
            totalSteps = event.values[0]
            Log.d("Sensor", "Total Steps from sensor: $totalSteps")

            if (isFirstSensorEvent) {
                // Initialize previousTotalSteps based on stepsTaken
                previousTotalSteps = totalSteps - stepsTaken
                isFirstSensorEvent = false
                Log.d("Sensor", "Set previousTotalSteps to $previousTotalSteps")
            }

            // Calculate new steps
            val newSteps = (totalSteps - previousTotalSteps).toInt()

            // Ensure steps are not negative
            if (newSteps < 0) {
                Log.w("StepCounter", "New steps negative: $newSteps")
                return
            }

            if (newSteps > 0) {
                // Update stepsTaken
                stepsTaken += newSteps
                Log.d("StepCounter", "Added $newSteps steps. Total steps: $stepsTaken")

                // Update UI
                binding.stepsTaken.text = "$stepsTaken"
                caloriesBurned = stepsTaken * 0.04
                binding.caloriesTextView.text = String.format("%.2f Kcal", caloriesBurned)
                binding.circularProgressBar.setProgressWithAnimation(stepsTaken.toFloat())

                // Check if it's a new day before saving
                if (getCurrentDate() != lastUpdatedDate) {
                    resetStepsForNewDay(getCurrentDate())
                }

                // Update Firebase with the new steps after resetting for a new day
                saveStepData(stepsTaken, caloriesBurned)

                // Update previousTotalSteps
                previousTotalSteps = totalSteps
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    private fun saveStepData(currentSteps: Int, caloriesBurned: Double) {
        val currentDate = getCurrentDate()
        val dailyDataRef = database.child("dailyData").child(currentDate)

        // Update steps
        dailyDataRef.child("steps").setValue(currentSteps)
            .addOnSuccessListener {
                Log.d("Firebase", "Steps updated successfully: $currentSteps")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error updating steps", e)
            }

        // Update calories
        dailyDataRef.child("calories").setValue(caloriesBurned)
            .addOnSuccessListener {
                Log.d("Firebase", "Calories updated successfully: $caloriesBurned")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error updating calories", e)
            }
    }


    fun resetSteps() {
        binding.stepsTaken.text = "0"
        caloriesBurned = 0.0
        binding.caloriesTextView.text = String.format("%.2f Kcal", caloriesBurned)

        // Store the current sensor steps as the new previousTotalSteps
        val currentTotalSteps = totalSteps
        previousTotalSteps = currentTotalSteps
        isFirstSensorEvent = false // Reset flag
        stepsTaken = 0
        Log.d("ResetSteps", "Steps reset. previousTotalSteps set to $previousTotalSteps")

        // Reset Firebase data
        resetFirebaseData()
    }


    private fun resetFirebaseData() {
        val currentDate = getCurrentDate()
        val dailyDataRef = database.child("dailyData").child(currentDate)

        // Reset steps
        dailyDataRef.child("steps").setValue(0)
            .addOnSuccessListener {
                Log.d("Firebase", "Steps reset to 0")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error resetting steps", e)
            }

        // Reset calories
        dailyDataRef.child("calories").setValue(0.0)
            .addOnSuccessListener {
                Log.d("Firebase", "Calories reset to 0.0")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error resetting calories", e)
            }
    }


    private fun initWaterIntake() {
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            // Reference to the user's water goal in Firebase
            val userGoalsRef = database.child("goals")

            // Fetch water goal from Firebase
            userGoalsRef.child("waterGoal").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val waterGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 2000

                    // Load initial water intake from Firebase
                    database.child("dailywaterconsumption").child(currentUser).child(getCurrentDate()).child("waterIntake")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                totalWaterIntake = snapshot.getValue(Int::class.java) ?: 0

                                // Update UI with water intake and water goal
                                binding.waterIntakeTextView.text = "$totalWaterIntake ml / $waterGoal ml"
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Firebase", "Error fetching water intake: ${error.message}")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching water goal: ${error.message}")
                }
            })
        }
    }


    private fun updateWaterIntake(amount: Int) {
        // Increase water intake
        totalWaterIntake += amount

        // Fetch current user and water goal to update UI properly
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            val userGoalsRef = database.child("goals")

            // Fetch water goal again (if needed)
            userGoalsRef.child("waterGoal").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val waterGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 2000

                    // Update the UI with new water intake and goal
                    binding.waterIntakeTextView.text = "$totalWaterIntake ml / $waterGoal ml"

                    // Get the current date in YYYY-MM-DD format
                    val currentDate = getCurrentDate()

                    // Save water intake under the current date in Firebase
                    database.child("dailywaterconsumption").child(currentUser).child(currentDate).child("waterIntake").setValue(totalWaterIntake)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Water intake updated successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error updating water intake", e)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching water goal: ${error.message}")
                }
            })
        }
    }


    private fun fetchAndDisplayCalories() {
        val currentUser = auth.currentUser?.uid
        val currentDate = getCurrentDate() // Get the current date dynamically

        if (currentUser != null) {
            // Reference to the user's goals in Firebase
            val userGoalsRef = database.child("goals")
            val userNutritionalInfoRef = database.child("nutritionalInfo").child(currentDate)

            // Fetch calorie goal first
            userGoalsRef.child("calorieGoal").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyCalorieGoal = snapshot.getValue(String::class.java)?.toDoubleOrNull() ?: 2500.0

                    // Now fetch the calories consumed
                    userNutritionalInfoRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var totalCalories = 0.0

                            if (snapshot.exists()) {
                                // Loop through each meal type (e.g., Breakfast, Lunch)
                                for (mealTypeSnapshot in snapshot.children) {
                                    Log.d("NutritionalInfo", "Meal Type: ${mealTypeSnapshot.key}")

                                    // Loop through the individual meals under each meal type
                                    for (entrySnapshot in mealTypeSnapshot.children) {
                                        val calories = entrySnapshot.child("calories").getValue(Double::class.java) ?: 0.0
                                        totalCalories += calories
                                        Log.d("NutritionalInfo", "Calories for ${entrySnapshot.key}: $calories")
                                    }
                                }

                                // Update the UI with total calories consumed and calorie goal
                                binding.CaloriesConsumed.text = "${totalCalories.toInt()} Kcal / ${dailyCalorieGoal.toInt()} Kcal"
                            } else {
                                Log.d("NutritionalInfo", "No data found for the current date.")
                                binding.CaloriesConsumed.text = "0 Kcal / ${dailyCalorieGoal.toInt()} Kcal"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("NutritionalInfo", "Failed to read data: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserGoals", "Failed to read user goals: ${error.message}")
                }
            })
        }
    }


    private fun fetchStepGoal() {
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            val userGoalsRef = database.child("goals")
            userGoalsRef.child("stepGoal").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stepGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0
                    binding.circularProgressBar.progressMax = stepGoal.toFloat() // Set the max value of the progress bar

                    // Since stepsTaken is loaded from Firebase, set the progress accordingly
                    binding.circularProgressBar.setProgressWithAnimation(stepsTaken.toFloat())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching step goal: ${error.message}")
                }
            })
        }
    }


    private fun getCurrentDate(): String {
        // Format the current date as yyyy-MM-dd
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date()) // Returns current date
    }
}
