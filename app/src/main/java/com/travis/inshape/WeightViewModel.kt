package com.travis.inshape

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val weightDao: WeightDao = WeightDatabase.getDatabase(application).weightDao()
    private val context: Context = application.applicationContext
    val allWeights: LiveData<List<Weight>> = weightDao.getAllWeights().asLiveData()


     //Inserts a weight entry into the local database.
    fun insert(weight: Weight) {
        viewModelScope.launch {
            try {
                weightDao.insert(weight)
                Log.d("WeightViewModel", "Weight inserted locally: $weight")
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error inserting weight locally: ${e.message}", e)
                Toast.makeText(context, "Failed to save weight locally", Toast.LENGTH_SHORT).show()
            }
        }
    }


     // Synchronizes a given weight with Firebase and updates its sync status in the local database.
    fun syncWithFirebase(weight: Weight) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("WeightViewModel", "User not logged in. Cannot sync weight with Firebase.")
            return
        }

        val weightRef = FirebaseDatabase.getInstance().getReference("users/$userId/WeightData")

        // Save new weight entry in Firebase
        weightRef.child(weight.timestamp.toString()).setValue(weight).addOnSuccessListener {
            viewModelScope.launch {
                try {
                    weight.isSynced = true
                    weight.firebaseId = weight.timestamp.toString()
                    weightDao.update(weight)
                    Toast.makeText(context, "Weight synced with Firebase", Toast.LENGTH_SHORT).show()
                    Log.d("WeightViewModel", "Weight synced successfully with Firebase: $weight")
                } catch (e: Exception) {
                    Log.e("WeightViewModel", "Error updating weight sync status: ${e.message}", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("WeightViewModel", "Failed to sync weight with Firebase: ${e.message}", e)
            Toast.makeText(context, "Failed to sync weight with Firebase", Toast.LENGTH_SHORT).show()
        }
    }


     // Retrieves the last recorded weight from Firebase and returns it via a callback.
    fun getLastWeight(callback: (Float?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("WeightViewModel", "User not logged in. Cannot fetch last weight from Firebase.")
            callback(null)
            return
        }

        val weightRef = FirebaseDatabase.getInstance().getReference("users/$userId/WeightData")

        weightRef.orderByKey().limitToLast(1).get().addOnSuccessListener { snapshot ->
            try {
                if (snapshot.exists()) {
                    val lastWeight = snapshot.children.first().child("weight").getValue(String::class.java)?.toFloat()
                    callback(lastWeight)
                    Log.d("WeightViewModel", "Last weight fetched from Firebase: $lastWeight")
                } else {
                    callback(null)
                    Log.d("WeightViewModel", "No weight records found in Firebase.")
                }
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error fetching last weight from Firebase: ${e.message}", e)
                callback(null)
            }
        }.addOnFailureListener { e ->
            Log.e("WeightViewModel", "Failed to fetch last weight from Firebase: ${e.message}", e)
            callback(null)
        }
    }


     // Synchronizes all unsynced weights with Firebase.
    fun syncUnsyncedWeights() {
        viewModelScope.launch {
            try {
                val unsyncedWeights = weightDao.getUnsyncedWeights()
                if (unsyncedWeights.isNotEmpty()) {
                    Log.d("WeightViewModel", "Syncing ${unsyncedWeights.size} unsynced weights with Firebase.")
                    for (weight in unsyncedWeights) {
                        syncWithFirebase(weight)
                    }
                } else {
                    Log.d("WeightViewModel", "No unsynced weights to sync.")
                }
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error syncing unsynced weights: ${e.message}", e)
            }
        }
    }
}
