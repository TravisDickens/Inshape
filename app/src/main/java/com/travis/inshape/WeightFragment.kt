package com.travis.inshape

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.travis.inshape.databinding.FragmentWeightBinding

class WeightFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentWeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var weightViewModel: WeightViewModel
    private lateinit var networkReceiver: NetworkReceiver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set up ViewBinding
        _binding = FragmentWeightBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize ViewModel
        weightViewModel = ViewModelProvider(requireActivity())[WeightViewModel::class.java]

        // Set up network receiver to detect connectivity changes
        networkReceiver = NetworkReceiver { isConnected ->
            if (isConnected) {
                Log.d("WeightFragment", "Network connected: syncing unsynced weights.")
                weightViewModel.syncUnsyncedWeights()
            }
        }
        requireActivity().registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        // Observe changes in weight data
        weightViewModel.allWeights.observe(viewLifecycleOwner) { weights ->
            if (weights.isNotEmpty()) {
                Log.d("WeightFragment", "Weights loaded: ${weights.joinToString(", ")}")
            } else {
                Toast.makeText(requireContext(), "No weights recorded yet", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up save button listener
        binding.btnSaveWeight.setOnClickListener {
            saveWeight()
        }

        return view
    }

    /**
     * Function to save the entered weight. If valid, weight is saved locally or synced
     * with Firebase based on network availability.
     */
    private fun saveWeight() {
        val currentWeight = binding.etCurrentWeight.text.toString().trim().toFloatOrNull()

        if (currentWeight != null) {
            val timestamp = System.currentTimeMillis()

            // Create Weight object with unsynced status
            val weight = Weight(
                weight = currentWeight.toString(),
                timestamp = timestamp,
                isSynced = false
            )

            weightViewModel.getLastWeight { lastWeight ->
                try {
                    if (lastWeight != null && currentWeight < lastWeight) {
                        val weightLoss = lastWeight - currentWeight
                        NotificationUtils.sendNotification(
                            requireContext(),
                            "Congratulations!",
                            "You lost $weightLoss kg!"
                        )
                    }

                    if (NetworkUtils.isNetworkAvailable(requireContext())) {
                        Log.d("WeightFragment", "Network available: syncing weight with Firebase.")
                        weightViewModel.syncWithFirebase(weight)
                    } else {
                        Log.d("WeightFragment", "No network: saving weight locally.")
                        weightViewModel.insert(weight)
                        Toast.makeText(requireContext(), "Weight saved locally", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("WeightFragment", "Error saving weight: ${e.message}", e)
                    Toast.makeText(requireContext(), "An error occurred while saving weight", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w("WeightFragment", "Invalid weight input.")
            Toast.makeText(requireContext(), "Please enter a valid weight", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireActivity().unregisterReceiver(networkReceiver)
            Log.d("WeightFragment", "Network receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.e("WeightFragment", "Receiver not registered or already unregistered: ${e.message}", e)
        }
    }
}
