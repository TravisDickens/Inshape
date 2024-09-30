package com.travis.inshape

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    private val PICK_IMAGE = 100
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("InShapePrefs", MODE_PRIVATE)

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
            openImagePicker() // Open the image picker dialog
        }

        // Load profile image
        loadProfileImage()
    }

    private fun getLoggedInUserEmail() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email
            binding.userText.text = userEmail ?: "No Email"
        } else {
            binding.userText.text = "No User Logged In"
        }
    }

    private fun saveUserGoals() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Get values from the TextInputEditTexts
            val stepGoal = binding.stepsGoalInput.text.toString().trim()
            val waterGoal = binding.waterGoalInput.text.toString().trim()
            val calorieGoal = binding.calorieGoalInput.text.toString().trim()

            // Input validation
            if (stepGoal.isEmpty() || waterGoal.isEmpty() || calorieGoal.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return
            }

            // Create a map to store the goals
            val userGoals = mapOf(
                "stepGoal" to stepGoal,
                "waterGoal" to waterGoal,
                "calorieGoal" to calorieGoal
            )

            // Save the goals under the current user's node in Firebase
            database.child("users").child(currentUser.uid).child("goals")
                .setValue(userGoals)
                .addOnSuccessListener {
                    Toast.makeText(this, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save goals", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserGoals() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userGoalsRef = database.child("users").child(currentUser.uid).child("goals")

            userGoalsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val stepGoal = snapshot.child("stepGoal").getValue(String::class.java)
                        val waterGoal = snapshot.child("waterGoal").getValue(String::class.java)
                        val calorieGoal = snapshot.child("calorieGoal").getValue(String::class.java)

                        binding.stepsGoalInput.setText(stepGoal ?: "")
                        binding.waterGoalInput.setText(waterGoal ?: "")
                        binding.calorieGoalInput.setText(calorieGoal ?: "")
                    } else {
                        Toast.makeText(this@SettingsActivity, "No goals found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity, "Failed to load goals", Toast.LENGTH_SHORT).show()
                }
            })
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
            imageUri?.let { uploadImageToFirebase(it) }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val fileRef = storageReference.child("profile_images/${currentUser.uid}.jpg")
            fileRef.putFile(imageUri).addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToDatabase(uri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
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
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.child("users").child(currentUser.uid).child("profileImage")

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.getValue(String::class.java)
                    imageUrl?.let {
                        Glide.with(this@SettingsActivity).load(it).into(binding.profileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
