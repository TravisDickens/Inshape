package com.travis.inshape

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.travis.inshape.databinding.ActivityHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : Base(), SensorEventListener {

    private var totalWaterIntake = 0
    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var stepsTaken = 0
    private var isFirstSensorEvent = true
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 1
    private  val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private var caloriesBurned = 0.0
    private var lastUpdatedDate: String? = null
    private lateinit var binding: ActivityHomeBinding
    // Firebase references
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var weightViewModel: WeightViewModel
    private val REQUEST_CODE_SCHEDULE_EXACT_ALARM = 1001
    private var currentLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        weightViewModel = ViewModelProvider(this).get(WeightViewModel::class.java)

        // Request permissions sequentially
        requestActivityRecognitionPermission()
        checkExactAlarmPermission()

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

        // Load the current language from SharedPreferences
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentLang = sharedPref.getString("language", Locale.getDefault().language)

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

        //load weight graph
        loadWeightData()
        if (checkExactAlarmPermission()) {
        if (!isAlarmSet(this, 0)) {
            scheduleHydrationReminder()
            checkCalorieGoal()
            scheduleDailyMotivationalQuote()
            scheduleMealReminders()
        }
        } else {
            // Optionally, you can show a message to the user explaining why the permission is needed
            Toast.makeText(this, "Permission needed to schedule reminders", Toast.LENGTH_SHORT).show()
        }

    }


    private fun setupBottomNavigation() {
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    // Already on HomeActivity, no action needed
                    true
                }
                R.id.videos -> {
                    val intent = Intent(this, VideosActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.ChatBot -> {
                    val intent = Intent(this, ChatBotActivity::class.java)
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
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheetlayout, null)

        bottomSheetDialog.setContentView(view)

        // Handle the click on the weight layout
        val weightLayout =  view.findViewById<TextView>(R.id.weightlayout)
        val inputCalories = view.findViewById<TextView>(R.id.NutritionLayout)

        weightLayout.setOnClickListener {
            showTaskFragment()  // Call the function to show the TaskFragment
            bottomSheetDialog.dismiss() // Optionally dismiss the dialog
        }

        inputCalories.setOnClickListener {
            val intent = Intent(this, NutritionActivity::class.java)
            startActivity(intent)
        }



        // Set transparent background if needed
        bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        bottomSheetDialog.show()
    }
    private fun showTaskFragment() {
        val taskFragment = WeightFragment()
        taskFragment.show(supportFragmentManager, "newTaskTag")
    }




    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun loadStepsFromFirebase() {
        val currentDate = getCurrentDate() // Get the current date in the required format
        val dailyDataRef = database.child("dailyData").child(currentDate) // Reference to the user's daily data in Firebase

        // Fetch steps and calories from Firebase
        dailyDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        // Load steps from Firebase, default to 0 if the value is null
                        stepsTaken = snapshot.child("steps").getValue(Int::class.java) ?: 0
                        // Update the last updated date with today's date
                        lastUpdatedDate = currentDate
                        Log.d("Firebase", "Loaded steps: $stepsTaken")
                        // Update the steps text in the UI
                        binding.stepsTaken.text = "$stepsTaken"

                        // Load calories from Firebase, default to 0.0 if the value is null
                        val storedCalories = snapshot.child("calories").getValue(Double::class.java) ?: 0.0
                        Log.d("Firebase", "Loaded calories: $storedCalories")
                        // Format and display calories in the UI
                        binding.caloriesTextView.text = String.format("%.2f Kcal", storedCalories)
                    }

                    // Register the sensor listener after loading steps to track real-time steps
                    registerSensorListener()

                } catch (e: Exception) {
                    // Handle any exceptions during data parsing or UI updates
                    Log.e("Firebase", "Error parsing data from Firebase", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error when Firebase operation is cancelled or fails
                Log.e("Firebase", "Error loading dailyData", error.toException())

                // Still register the sensor listener even if loading from Firebase fails
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
        lastUpdatedDate = currentDate
    }




    private fun registerSensorListener() {
        // Set the flag to indicate the sensor listener is running
        running = true

        // Try to get the step counter sensor from the device's sensor manager
        val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Check if the device has a step counter sensor
        if (stepSensor == null) {
            // Show a message to the user if no step counter sensor is found
            Toast.makeText(this, "No step counter sensor detected on device", Toast.LENGTH_SHORT).show()
            Log.e("Sensor", "Step Counter sensor not available")
        } else {
            try {
                // Register the sensor listener to receive updates from the step counter sensor
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("Sensor", "Step Counter sensor listener registered")
            } catch (e: Exception) {
                // Handle any potential exception during the registration process
                Log.e("Sensor", "Error registering step counter sensor listener", e)
                Toast.makeText(this, "Error registering sensor listener", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Show the dialog to request exact alarm permissions
    private fun showExactAlarmPermissionDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_request, null)

        // Create and configure the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set click listener for the "No" button
        dialogView.findViewById<Button>(R.id.button_no).setOnClickListener {
            dialog.dismiss()  // Close the dialog without doing anything
        }

        // Set click listener for the "Yes" button
        dialogView.findViewById<Button>(R.id.button_yes).setOnClickListener {
            dialog.dismiss()  // Close the dialog
            requestExactAlarmPermission()  // Navigate to settings to request permission
        }

        dialog.show()  // Display the dialog
    }

    // Request exact alarm permission
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM)
        }
    }



    // Handle the result after the user navigates back from settings
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                // Schedule alarms if permission was granted
                scheduleHydrationReminder()
                checkCalorieGoal()
                scheduleDailyMotivationalQuote()
                scheduleMealReminders()
            } else {
                Toast.makeText(this, "Exact Alarm Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestAlarmPermission() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionDialog()
        } else {
            // Schedule the reminders directly if permission is already granted
            scheduleMealReminders()
            scheduleHydrationReminder()
            scheduleDailyMotivationalQuote()
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            }else {
                // Permission is already granted, proceed to the next permission
                requestNotificationPermission()
            }
        } else {
            // For API levels lower than Q, directly request notification permission
            requestNotificationPermission()
        }
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {

            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, now request notification permission
                    requestNotificationPermission()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Activity Recognition Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Notification permission granted
                    Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Notification permission denied
                    Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    override fun onResume() {
        super.onResume()

        // Check if the sensor is not running and register the sensor listener
        if (!running) {
            registerSensorListener()
        }

        // Sync unsynced weights with Firestore
        weightViewModel.syncUnsyncedWeights()

        // Check if the language has changed
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val newLang = sharedPref.getString("language", Locale.getDefault().language)

        if (newLang != currentLang) {
            currentLang = newLang
            recreate() // Only recreate if the language has changed
        }
    }



    override fun onSensorChanged(event: SensorEvent?) {
        // Ensure the sensor event is not null and the listener is running
        if (running && event != null) {
            totalSteps = event.values[0]
            Log.d("Sensor", "Total Steps from sensor: $totalSteps")

            // Check if this is the first sensor event after the app started
            if (isFirstSensorEvent) {
                // Initialize previousTotalSteps to adjust for already counted steps (stepsTaken)
                previousTotalSteps = totalSteps - stepsTaken
                // Mark as false after initializing
                isFirstSensorEvent = false
                Log.d("Sensor", "Set previousTotalSteps to $previousTotalSteps")
            }

            // Calculate the number of new steps by subtracting previousTotalSteps
            val newSteps = (totalSteps - previousTotalSteps).toInt()

            // Ensure that the new steps value is not negative
            if (newSteps < 0) {
                Log.w("StepCounter", "New steps are negative: $newSteps")
                return // Exit the function to avoid updating with negative steps
            }

            if (newSteps > 0) {
                // Add the new steps to the total stepsTaken for the session
                stepsTaken += newSteps
                Log.d("StepCounter", "Added $newSteps steps. Total steps: $stepsTaken") // Log step addition

                // Update the UI with the new step count and calories burned
                binding.stepsTaken.text = "$stepsTaken"
                // Calculate calories burned
                caloriesBurned = stepsTaken * 0.04
                // Update UI with calories
                binding.caloriesTextView.text = String.format("%.2f Kcal", caloriesBurned)
                // Animate progress bar
                binding.circularProgressBar.setProgressWithAnimation(stepsTaken.toFloat())

                // Check if the current day has changed (a new day), and reset steps if needed
                if (getCurrentDate() != lastUpdatedDate) {
                    // Reset steps for the new day
                    resetStepsForNewDay(getCurrentDate())
                }

                // Save the updated step count and calories burned to Firebase
                try {
                    // Save data to Firebase
                    saveStepData(stepsTaken, caloriesBurned)
                } catch (e: Exception) {
                    Log.e("Firebase", "Error saving step data to Firebase", e)
                }

                // Update previousTotalSteps to the current totalSteps after processing
                previousTotalSteps = totalSteps
            }
        } else {
            Log.e("Sensor", "Sensor event is null or listener is not running")
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    private fun saveStepData(currentSteps: Int, caloriesBurned: Double) {
        val currentDate = getCurrentDate() // Get the current date to store data for the specific day
        val dailyDataRef = database.child("dailyData").child(currentDate) // Reference to the daily data node in Firebase

        // Update steps in Firebase under the current date node
        dailyDataRef.child("steps").setValue(currentSteps)
            .addOnSuccessListener {
                
                Log.d("Firebase", "Steps updated successfully: $currentSteps")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@HomeActivity, "Error updating steps", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error updating steps", e)
               
            }

        // Update calories burned in Firebase under the current date node
        dailyDataRef.child("calories").setValue(caloriesBurned)
            .addOnSuccessListener {
               
                Log.d("Firebase", "Calories updated successfully: $caloriesBurned")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@HomeActivity, "Error updating calories", Toast.LENGTH_SHORT).show()
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
        // Reset flag
        isFirstSensorEvent = false 
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
                Toast.makeText(this@HomeActivity, "Failed to reset steps", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error resetting steps", e)
            }

        // Reset calories
        dailyDataRef.child("calories").setValue(0.0)
            .addOnSuccessListener {
                Log.d("Firebase", "Calories reset to 0.0")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@HomeActivity, "Failed to reset calories", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error resetting calories", e)
            }
    }


    private fun initWaterIntake() {
        // Get the current authenticated user
        val currentUser = auth.currentUser?.uid

        // Ensure the user is authenticated before proceeding
        if (currentUser != null) {
            // Reference to the user's water goal node in Firebase
            val userGoalsRef = database.child("goals")

            // Fetch the user's water goal from Firebase
            userGoalsRef.child("waterGoal").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Parse the water goal from Firebase or default to 0 ml if not available
                    val waterGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0

                    // Fetch the current day's water intake from Firebase for the current user
                    database.child("dailywaterconsumption")
                        .child(currentUser)
                        .child(getCurrentDate())
                        .child("waterIntake")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Get the total water intake from Firebase or default to 0 if not found
                                totalWaterIntake = snapshot.getValue(Int::class.java) ?: 0

                                // Update the UI with the current water intake and water goal
                                binding.waterIntakeTextView.text = "$totalWaterIntake / $waterGoal ml"
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@HomeActivity, "Error fetching water intake", Toast.LENGTH_SHORT).show()
                                Log.e("Firebase", "Error fetching water intake: ${error.message}")

                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error fetching water goal", Toast.LENGTH_SHORT).show()
                    Log.e("Firebase", "Error fetching water goal: ${error.message}")

                }
            })
        } else {
            Toast.makeText(this@HomeActivity, "Please login again", Toast.LENGTH_SHORT).show()
            Log.w("Firebase", "No authenticated user found")

        }
    }



    private fun updateWaterIntake(amount: Int) {
        try {
            // Increase water intake by the specified amount
            totalWaterIntake += amount

            // Get the current authenticated user
            val currentUser = auth.currentUser?.uid

            // Ensure user is authenticated before proceeding
            if (currentUser != null) {
                val userGoalsRef = database.child("goals")

                // Fetch the user's water goal from Firebase
                userGoalsRef.child("waterGoal").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Get water goal from Firebase or default to 0 ml if not found
                        val waterGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0

                        // Update the UI with the new total water intake and goal
                        binding.waterIntakeTextView.text = "$totalWaterIntake / $waterGoal ml"

                        // Get the current date in YYYY-MM-DD format
                        val currentDate = getCurrentDate()

                        // Save the updated water intake in Firebase under the current user's node for today's date
                        database.child("dailywaterconsumption")
                            .child(currentUser)
                            .child(currentDate)
                            .child("waterIntake")
                            .setValue(totalWaterIntake)
                            .addOnSuccessListener {

                                Log.d("Firebase", "Water intake updated successfully.")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@HomeActivity, "Error updating water intake", Toast.LENGTH_SHORT).show()
                                Log.e("Firebase", "Error updating water intake", e)

                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@HomeActivity, "Error fetching water goal", Toast.LENGTH_SHORT).show()
                        Log.e("Firebase", "Error fetching water goal: ${error.message}")

                    }
                })
            } else {
                Toast.makeText(this@HomeActivity, "No authenticated user found, Cannot update eater intake", Toast.LENGTH_SHORT).show()
                Log.w("Firebase", "No authenticated user found. Cannot update water intake.")

            }
        } catch (e: Exception) {
            Toast.makeText(this@HomeActivity, "Unexpected error occurred while updating water intake", Toast.LENGTH_SHORT).show()
            Log.e("UpdateWaterIntake", "Unexpected error occurred while updating water intake", e)

        }
    }



    private fun fetchAndDisplayCalories() {
        try {
            // Get the current authenticated user
            val currentUser = auth.currentUser?.uid
            // Get the current date in the correct format
            val currentDate = getCurrentDate()

            // Ensure the user is authenticated before proceeding
            if (currentUser != null) {
                // Reference to the user's goals and nutritional info in Firebase
                val userGoalsRef = database.child("goals")
                val userNutritionalInfoRef = database.child("nutritionalInfo").child(currentDate)

                // Fetch the calorie goal from Firebase
                userGoalsRef.child("calorieGoal").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Get the calorie goal, or use a default value of 0 Kcal if not found
                        val dailyCalorieGoal = snapshot.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

                        // Fetch the calories consumed from Firebase
                        userNutritionalInfoRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var totalCalories = 0.0

                                if (snapshot.exists()) {
                                    // Loop through each meal type
                                    for (mealTypeSnapshot in snapshot.children) {
                                        Log.d("NutritionalInfo", "Meal Type: ${mealTypeSnapshot.key}")

                                        // Loop through individual meals under each meal type and sum the calories
                                        for (entrySnapshot in mealTypeSnapshot.children) {
                                            val calories = entrySnapshot.child("calories").getValue(Double::class.java) ?: 0.0
                                            totalCalories += calories
                                            Log.d("NutritionalInfo", "Calories for ${entrySnapshot.key}: $calories")
                                        }
                                    }

                                    // Update the UI with the total calories consumed and the calorie goal
                                    binding.CaloriesConsumed.text = "${totalCalories.toInt()} / ${dailyCalorieGoal.toInt()} Kcal"
                                } else {
                                    // If no data exists for the current date, set calories to 0
                                    Log.d("NutritionalInfo", "No nutritional data found for the current date.")
                                    binding.CaloriesConsumed.text = "0 / ${dailyCalorieGoal.toInt()} Kcal"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@HomeActivity, "Failed to read nutritional data", Toast.LENGTH_SHORT).show()
                                Log.e("NutritionalInfo", "Failed to read nutritional data: ${error.message}")

                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@HomeActivity, "Failed to fetch goals", Toast.LENGTH_SHORT).show()
                        Log.e("UserGoals", "Failed to read user goals: ${error.message}")

                    }
                })
            } else {
                Toast.makeText(this@HomeActivity, "No authenticated user found. Cannot fetch calorie data.", Toast.LENGTH_SHORT).show()
                Log.w("FirebaseAuth", "No authenticated user found. Cannot fetch calorie data.")

            }
        } catch (e: Exception) {
            Toast.makeText(this@HomeActivity, "An unexpected error occurred while fetching calorie data", Toast.LENGTH_SHORT).show()
            Log.e("FetchCalories", "An unexpected error occurred while fetching calorie data", e)

        }
    }



    private fun fetchStepGoal() {
        try {
            // Get the current authenticated user's ID
            val currentUser = auth.currentUser?.uid

            // Ensure the user is authenticated before fetching data
            if (currentUser != null) {
                // Reference to the user's goals in Firebase
                val userGoalsRef = database.child("goals")

                // Fetch the step goal from Firebase
                userGoalsRef.child("stepGoal").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Get the step goal from Firebase, or use a default value of 0 if not found
                        val stepGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0

                        // Set the max value of the progress bar to the fetched step goal
                        binding.circularProgressBar.progressMax = stepGoal.toFloat()

                        // Set the progress of the progress bar to the current number of steps taken
                        binding.circularProgressBar.setProgressWithAnimation(stepsTaken.toFloat())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@HomeActivity, "Error fetching step goal:", Toast.LENGTH_SHORT).show()
                        Log.e("Firebase", "Error fetching step goal: ${error.message}")

                    }
                })
            } else {
               showToast("No authenticated user found. Cannot fetch step goal.")
                Log.w("FirebaseAuth", "No authenticated user found. Cannot fetch step goal.")

            }
        } catch (e: Exception) {
           showToast("An unexpected error occurred while fetching the step goal")
            Log.e("FetchStepGoal", "An unexpected error occurred while fetching the step goal", e)

        }
    }

    //go up 3 hours
    //method to schedule hydration reminders
    private fun scheduleHydrationReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        // Set a 2-hour interval for hydration reminders
        val triggerTime = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR * 2
        Log.d("Hydration Reminder", "Scheduling hydration reminder at ${Date(triggerTime)}")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
    // method to compare and see if user exceeded 80% of calorie goal intake
    private fun checkCalorieGoal() {
        val currentDate = getCurrentDate()

        // Reference to user's goals and nutritional info in Firebase
        val calorieGoalRef = database.child("goals").child("calorieGoal")
        val nutritionalInfoRef = database.child("nutritionalInfo").child(currentDate)

        // Fetch calorie goal
        calorieGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get calorie goal as Double, handling potential format inconsistencies
                val calorieGoal = try {
                    snapshot.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    snapshot.getValue(Double::class.java) ?: 0.0
                }

                // Fetch calories consumed for the current date
                nutritionalInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalCalories = 0.0

                        if (snapshot.exists()) {
                            // Loop through each meal and sum the calories
                            for (mealSnapshot in snapshot.children) {
                                for (entrySnapshot in mealSnapshot.children) {
                                    val calories = try {
                                        entrySnapshot.child("calories").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                                    } catch (e: Exception) {
                                        entrySnapshot.child("calories").getValue(Double::class.java) ?: 0.0
                                    }
                                    totalCalories += calories
                                }
                            }

                            // Debug logging for verification
                            Log.d("CalorieCheck", "Total Calories: $totalCalories, Calorie Goal: $calorieGoal, 80% Goal: ${0.8 * calorieGoal}")

                            // Send notification if 80% of the goal is reached
                            if (totalCalories >= 0.8 * calorieGoal && calorieGoal > 0) {
                                NotificationUtils.sendNotification(
                                    this@HomeActivity,
                                    "Calorie Alert",
                                    "You've reached 80% of your daily calorie goal!"
                                )
                            }
                        } else {
                            Log.d("CalorieCheck", "No nutritional data found for the current date.")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CalorieCheck", "Failed to read nutritional data: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CalorieGoal", "Failed to read calorie goal: ${error.message}")
            }
        })
    }

    //go up 4 hours
    //method to schedule daily motivational quotes
    private fun scheduleDailyMotivationalQuote() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MotivationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)

        // Set a 3-hour interval for motivational quote reminders
        val triggerTime = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR * 3
        Log.d("Motivation Reminder", "Scheduling motivational reminder at ${Date(triggerTime)}")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    //works
    //method to schedule meal reminders
    private fun scheduleMealReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request exact alarm permission if needed
                requestExactAlarmPermission()
                return
            }
        }

        val intent = Intent(this, MealReminderReceiver::class.java)
        // Meal times: 8 AM, 12 PM, and 6 PM
        val mealTimes = listOf(8, 12, 18)

        for (hour in mealTimes) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time has already passed, schedule for the next day
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(this, hour, intent, PendingIntent.FLAG_IMMUTABLE)

            // Schedule the exact alarm to work even while idle
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Log.d("MealReminder", "Scheduled meal reminder for ${calendar.time}")
        }
    }

    // Check if alarms are set and request permissions if not allowed
    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Show dialog if permission is not granted
                showExactAlarmPermissionDialog()
                // Return false if permission is not granted
                return false
            } else {
                // Return true if permission is granted
                return true
            }
        }
        // Permission is not needed for older versions
        return true
    }


    private fun loadWeightData() {
        // Get the ID of the currently authenticated user
        val userId = auth.currentUser?.uid
        // Check if user ID is valid (user is logged in)
        if (userId != null) {
            // Access the Firebase database for weight data
            database.child("WeightData")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // List to store weight entries for chart
                        val weightEntries = ArrayList<Entry>()
                        // Index counter for each weight entry
                        var index = 0f

                        try {
                            // Check if any weight data exists in the snapshot
                        if (snapshot.exists()) {
                            for (weightSnapshot in snapshot.children) {
                                // Retrieve weight value and convert to Float
                                val weight = weightSnapshot.child("weight").getValue(String::class.java)
                                    ?.toFloatOrNull()
                                // If weight is valid, add to weight entries
                                if (weight != null) {
                                    weightEntries.add(Entry(index++, weight))
                                }
                            }

                            // If weight entries are available, configure the chart
                            if (weightEntries.isNotEmpty()) {
                                // Configure LineDataSet for weight progress chart
                                val lineDataSet = LineDataSet(weightEntries, "Weight Progress").apply {
                                    // Custom line color
                                    color = ContextCompat.getColor(this@HomeActivity, R.color.my_primary)
                                    // Circle color for points
                                    circleColors = listOf(ContextCompat.getColor(this@HomeActivity, R.color.purple_500))
                                    // Line thickness
                                    lineWidth = 3f
                                    // Radius of circle indicators
                                    circleRadius = 6f
                                    // Value text color
                                    valueTextColor = ContextCompat.getColor(this@HomeActivity, R.color.black)
                                    // Size of value text
                                    valueTextSize = 12f
                                    // Smooth line
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    // Fill under the line
                                    setDrawFilled(true)
                                    // Fill color
                                    fillColor = ContextCompat.getColor(this@HomeActivity, R.color.purple_200)
                                }
                                // Create LineData from the LineDataSet and set it to the chart
                                val lineData = LineData(lineDataSet)
                                binding.lineChart.data = lineData

                                // Customize chart appearance
                                with(binding.lineChart) {
                                    // Disable default description
                                    description.isEnabled = false
                                    // Chart background color
                                    setBackgroundColor(Color.WHITE)
                                    // Disable grid background
                                    setDrawGridBackground(false)
                                    axisLeft.apply {
                                        // Left axis text color
                                        textColor = ContextCompat.getColor(this@HomeActivity, R.color.black)
                                        // Minimum value on Y-axis
                                        axisMinimum = 0f
                                        // Granularity for Y-axis labels
                                        granularity = 1f

                                    }
                                    // Disable right axis
                                    axisRight.isEnabled = false
                                    xAxis.apply {
                                        // X-axis text color
                                        textColor = ContextCompat.getColor(this@HomeActivity, R.color.black)
                                        // Position of the X-axis
                                        position = XAxis.XAxisPosition.BOTTOM
                                        // Disable grid lines
                                        setDrawGridLines(false)
                                        // Set X-axis granularity
                                        granularity = 1f
                                        xAxis.isEnabled = false
                                    }
                                    // Animation for chart data loading
                                    animateXY(1000, 1000)
                                    // Refresh the chart
                                    invalidate()
                                }
                            } else {
                                binding.lineChart.clear()
                                Toast.makeText(this@HomeActivity, "No weight data available", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            binding.lineChart.clear()
                            Toast.makeText(this@HomeActivity, "No data found", Toast.LENGTH_SHORT).show()
                        }
                        } catch (e: Exception) {
                            // Catch any unexpected errors during data processing
                            Toast.makeText(this@HomeActivity, "Error processing data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                        Toast.makeText(this@HomeActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    //method to check if alarms had being set
    private fun isAlarmSet(context: Context, requestCode: Int): Boolean {
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        return pendingIntent != null
    }

    private fun getCurrentDate(): String {
        // Format the current date as yyyy-MM-dd
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // Returns current date
        return dateFormat.format(Date())
    }



}
