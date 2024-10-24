package com.travis.inshape

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.travis.inshape.databinding.ActivityRegisterBinding

class RegisterActivity : Base() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Set click listeners for registration button and return to login button
        binding.btnReg.setOnClickListener {
            registerUser()
        }

        binding.ReturnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        val email = binding.UserName.editText?.text.toString().trim()
        val password = binding.Password.editText?.text.toString().trim()
        val confirmPassword = binding.ConfirmPassword.editText?.text.toString().trim()

        // Perform input validation
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter a valid password", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(confirmPassword) || password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Attempt user registration with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful, display success message and start LoginActivity
                    Toast.makeText(this, "Registration complete, now login", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    // Finish RegisterActivity to prevent going back to it after logging in
                    finish()
                } else {
                    // Registration failed, display error message
                    val errorMessage = task.exception?.message ?: "Registration failed, try again"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
