package com.travis.inshape

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WeightViewModel(application: Application) : AndroidViewModel(application) {
    private val weightDao: WeightDao = WeightDatabase.getDatabase(application).weightDao()
    private val context: Context = application.applicationContext

    val allWeights: LiveData<List<Weight>> = weightDao.getAllWeights().asLiveData()

    fun insert(weight: Weight) {
        viewModelScope.launch {
            // Fetch the previous weight before inserting the new one
            val previousWeight = getPreviousWeight()

            // Convert weight.weight to Double for comparison
            val weightDifference = previousWeight?.let { it - weight.weight.toDouble() }

            // Send notification if the weight has decreased
            if (weightDifference != null && weightDifference > 0) {
                NotificationUtils.sendNotification(
                    context,
                    "Congratulations!",
                    "You lost $weightDifference kg!"
                )
            }

            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                // Sync with Firebase directly
                syncWithFirebase(weight)
            } else {
                // Save locally, mark as unsynced
                weight.isSynced = false
                weightDao.insert(weight)
            }
        }
    }


    // Helper function to get the most recent weight from Firebase
    private suspend fun getPreviousWeight(): Double? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val weightRef = FirebaseDatabase.getInstance().getReference("users/$userId/WeightData")

        return suspendCoroutine { continuation ->
            weightRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val lastWeightString = dataSnapshot.children.lastOrNull()?.child("weight")?.getValue(String::class.java)
                    val lastWeight = lastWeightString?.toDoubleOrNull() // Convert to Double if possible
                    continuation.resume(lastWeight)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("WeightViewModel", "Error fetching previous weight: ${databaseError.message}")
                    continuation.resume(null)
                }
            })
        }
    }


    // Sync the weight with Firebase
    private fun syncWithFirebase(weight: Weight) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val weightRef = FirebaseDatabase.getInstance().getReference("users/$userId/WeightData")

        // Save new weight entry in Firebase
        weightRef.child(weight.timestamp.toString()).setValue(weight).addOnSuccessListener {
            viewModelScope.launch {
                weight.isSynced = true
                weight.firebaseId = weight.timestamp.toString()
                weightDao.update(weight)
            }
        }.addOnFailureListener {
            Log.e("WeightViewModel", "Failed to sync weight with Firebase")
        }
    }
    fun syncUnsyncedWeights() {
        viewModelScope.launch {
            val unsyncedWeights = weightDao.getUnsyncedWeights()
            for (weight in unsyncedWeights) {
                syncWithFirebase(weight)
            }
        }
    }

}