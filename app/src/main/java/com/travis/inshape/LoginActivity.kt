package com.travis.inshape

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.travis.inshape.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var oneTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        sharedPreferences = getSharedPreferences("InShapePrefs", MODE_PRIVATE)

        // Initialize biometric authentication
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Handle error
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // After successful biometric authentication, check if user is logged in
                val user = auth.currentUser
                if (user != null) {
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()  // Close the current activity
                } else {
                    Toast.makeText(applicationContext, "Please log in first", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Handle failed authentication attempt
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        // Set click listeners for buttons using ViewBinding
        binding.register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.login.setOnClickListener {
            val email = binding.UserName.editText?.text.toString().trim()
            val password = binding.Password.editText?.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        binding.googleSignInButton.setOnClickListener {
            signinGoogle()
        }

        binding.loginBiometrics.setOnClickListener {
            if (sharedPreferences.getBoolean("biometric_enabled", false)) {
                authenticateBiometrics()
            } else {
                Toast.makeText(this, "Biometric authentication is disabled", Toast.LENGTH_SHORT).show()
            }
        }

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()
    }

    private fun signinGoogle() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = oneTapClient?.beginSignIn(signInRequest)?.await()
                val intentSenderRequest = IntentSenderRequest.Builder(result!!.pendingIntent).build()
                activityResultLauncher.launch(intentSenderRequest)
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient!!.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: ApiException){
                    e.printStackTrace()
                }
            }
        }

    // Function to authenticate user with Firebase
    private fun loginUser(email: String, password: String) {
        // Authenticate user with email and password
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // If login is successful, display a success message and start HomeActivity
                Toast.makeText(this, "You are now logged in", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()  // Close the current activity
            } else {
                // If login fails, display an error message
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to handle biometric authentication
    private fun authenticateBiometrics() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Biometric hardware not available", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric hardware currently unavailable", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
