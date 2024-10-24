package com.travis.inshape

import android.app.Application
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

class WeightViewModel(application: Application) : AndroidViewModel(application) {
    private val weightDao: WeightDao = WeightDatabase.getDatabase(application).weightDao()
    val allWeights: LiveData<List<Weight>> = weightDao.getAllWeights().asLiveData()

    fun insert(weight: Weight) {
        viewModelScope.launch {
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

    private fun syncWithFirebase(weight: Weight) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val weightRef = database.child(userId).child("WeightData")

            // Check if weight already exists
            weightRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.hasChild(weight.timestamp.toString())) {
                        val weightData = mapOf(
                            "weight" to weight.weight,
                            "timestamp" to weight.timestamp
                        )

                        weightRef.child(weight.timestamp.toString()).setValue(weightData)
                            .addOnSuccessListener {
                                viewModelScope.launch {
                                    weight.isSynced = true
                                    weight.firebaseId = weight.timestamp.toString()
                                    weightDao.update(weight) // Update weight as synced
                                }
                            }
                            .addOnFailureListener {
                                // Handle sync failure
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                }
            })
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
