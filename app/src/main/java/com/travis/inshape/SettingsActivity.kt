package com.travis.inshape

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.travis.inshape.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    private val PICK_IMAGE = 100
    private lateinit var storageReference: StorageReference

    private var currantLang: String = "en"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default language
        currantLang = Locale.getDefault().language

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("InShapePrefs", MODE_PRIVATE)

        // Load the saved language from SharedPreferences
        val savedLang = sharedPreferences.getString("language", "en") // Default is English
        setLocale(savedLang ?: "en") // Ensure the locale is set

        val spinner: Spinner = findViewById(R.id.spinner)
        val langs = arrayOf("English", "Afrikaans", "Zulu")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, langs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set the spinner position based on the saved language
        val selectedPosition = when (savedLang) {
            "af" -> 1
            "zu" -> 2
            else -> 0
        }
        spinner.setSelection(selectedPosition)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedLang = when (position) {
                    0 -> "en"
                    1 -> "af"
                    2 -> "zu"
                    else -> "en"
                }
                // Save the selected language to SharedPreferences
                sharedPreferences.edit().putString("language", selectedLang).apply()
                setLocale(selectedLang)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }

        }
            // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference


        // Access the Switch using the ID
        val biometricSwitch = binding.biometricAuthenticationSwitch // Ensure the ID matches the XML

        // Set the initial state of the Switch based on shared preferences
        biometricSwitch.isChecked = sharedPreferences.getBoolean("biometric_enabled", false)

        // Set up the listener for the Switch
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the new state in shared preferences
            with(sharedPreferences.edit()) {
                putBoolean("biometric_enabled", isChecked)
                apply()
            }

            // Optionally, show a toast message indicating the current state
            val message = if (isChecked) {
                "Biometric authentication enabled"
            } else {
                "Biometric authentication disabled"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Access the "Stay Signed In" Switch using the ID
        val staySignedInSwitch = binding.staySignedInSwitch

        // Set the initial state of the Switch based on shared preferences
        staySignedInSwitch.isChecked = sharedPreferences.getBoolean("stay_signed_in", false)

        // Set up the listener for the Switch
        staySignedInSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the new state in shared preferences
            with(sharedPreferences.edit()) {
                putBoolean("stay_signed_in", isChecked)
                apply()
            }

            // Optionally, show a toast message indicating the current state
            val message = if (isChecked) {
                "You will stay signed in"
            } else {
                "You will need to log in each time"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Display the logged-in user's email
        getLoggedInUserEmail()

        // Retrieve and display user goals in the EditTexts
        loadUserGoals()

        // Set onClickListener for Save button
        binding.saveButton.setOnClickListener {
            saveUserGoals()
        }

        // Set onClickListener for profile image to change picture
        binding.profileImage.setOnClickListener {
            // Open the image picker dialog
            openImagePicker()
        }

        // Load profile image
        loadProfileImage()
    }

    private fun setLocale(lang: String) {
        if (lang != currantLang) {
            currantLang = lang

            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)

            // Apply the new configuration
            val context = createConfigurationContext(config)
            resources.updateConfiguration(config, context.resources.displayMetrics)

            // Recreate the activity to apply the new language
            recreate()
        }
    }


    private fun getLoggedInUserEmail() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email
            binding.userText.text = userEmail ?: "No Email Availible"
        } else {
            binding.userText.text = "No User Logged In"
        }
    }

    private fun saveUserGoals() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val stepGoal = binding.stepsGoalInput.text.toString().trim()
            val waterGoal = binding.waterGoalInput.text.toString().trim()
            val calorieGoal = binding.calorieGoalInput.text.toString().trim()

            if (stepGoal.isEmpty() || waterGoal.isEmpty() || calorieGoal.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return
            }

            val userGoals = mapOf(
                "stepGoal" to stepGoal,
                "waterGoal" to waterGoal,
                "calorieGoal" to calorieGoal
            )

            // Save to Firebase
            database.child("users").child(currentUser.uid).child("goals")
                .setValue(userGoals)
                .addOnSuccessListener {
                    Toast.makeText(this, "Goals saved to Firebase", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to save goals: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            // Save locally to Room database
            val goal = Goal(stepGoal = stepGoal, waterGoal = waterGoal, calorieGoal = calorieGoal)
            lifecycleScope.launch {
                GoalDatabase.getDatabase(this@SettingsActivity).goalDao().insertGoal(goal)
                Toast.makeText(this@SettingsActivity, "Goals saved locally", Toast.LENGTH_SHORT).show() // Local storage toast
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }



    private fun loadUserGoals() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Check if the app is online
            if (isOnline()) {
                // Fetch from Firebase
                val userGoalsRef = database.child("users").child(currentUser.uid).child("goals")

                userGoalsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val stepGoal = snapshot.child("stepGoal").getValue(String::class.java)
                            val waterGoal = snapshot.child("waterGoal").getValue(String::class.java)
                            val calorieGoal = snapshot.child("calorieGoal").getValue(String::class.java)

                            binding.stepsGoalInput.setText(stepGoal ?: "")
                            binding.waterGoalInput.setText(waterGoal ?: "")
                            binding.calorieGoalInput.setText(calorieGoal ?: "")

                            // Save to Room DB
                            val goal = Goal(stepGoal = stepGoal ?: "", waterGoal = waterGoal ?: "", calorieGoal = calorieGoal ?: "")
                            lifecycleScope.launch {
                                GoalDatabase.getDatabase(this@SettingsActivity).goalDao().insertGoal(goal)
                            }
                        } else {
                            Toast.makeText(this@SettingsActivity, "No goals found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SettingsActivity, "Failed to load goals: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                // Fetch from Room DB
                lifecycleScope.launch {
                    val goal = GoalDatabase.getDatabase(this@SettingsActivity).goalDao().getGoals()
                        .collect { localGoal ->
                            localGoal?.let {
                                binding.stepsGoalInput.setText(it.stepGoal)
                                binding.waterGoalInput.setText(it.waterGoal)
                                binding.calorieGoalInput.setText(it.calorieGoal)
                            } ?: run {
                                Toast.makeText(this@SettingsActivity, "No offline data found", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }



    private fun openImagePicker() {
        // Directly open the gallery to choose an image
        openGallery()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE) {
            val imageUri: Uri? = data?.data
            imageUri?.let { uploadImageToFirebase(it) } ?: run {
                Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val fileRef = storageReference.child("profile_images/${currentUser.uid}.jpg")
            fileRef.putFile(imageUri).addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToDatabase(uri.toString())
                }.addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to get download URL: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Failed to upload image: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageUrlToDatabase(imageUrl: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.child("users").child(currentUser.uid).child("profileImage")

            databaseRef.setValue(imageUrl).addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                // Use Glide to update the image
                Glide.with(this).load(imageUrl).into(binding.profileImage)
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Failed to update profile picture: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.child("users").child(currentUser.uid).child("profileImage")

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.getValue(String::class.java)
                    if (imageUrl != null && !isDestroyed && !isFinishing) {
                        Glide.with(this@SettingsActivity).load(imageUrl).into(binding.profileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity, "Failed to load profile image: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
    override fun onBackPressed() {
        // Call the super method to ensure proper back button behavior
        super.onBackPressed()

        // You can add additional logic here if needed, like saving other preferences
        finish() // Finish the activity when back is pressed
    }
}
