package com.travis.inshape

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.travis.inshape.databinding.ActivityWaterIntakeDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class WaterIntakeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterIntakeDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var totalWaterIntake: Int = 0
    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityWaterIntakeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users").child(auth.currentUser?.uid.toString())

        // Initialize chart
        lineChart = binding.lineChart

        // Fetch water intake details and display
        fetchWaterIntakeDetails()
    }

    private fun fetchWaterIntakeDetails() {
        val currentUser = auth.currentUser?.uid

        if (currentUser != null) {
            // Reference to the user's water goal in Firebase
            val userGoalsRef = database.child("goals")

            // Fetch water goal from Firebase
            userGoalsRef.child("waterGoal").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val waterGoal = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 2000 // Default to 2000 if null

                    // Fetch the current day's water intake from Firebase
                    val currentDate = getCurrentDate()
                    fetchWaterIntakeForToday(currentUser, currentDate, waterGoal)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching water goal: ${error.message}")
                }
            })

            // Fetch water intake history for chart plotting
            fetchWaterIntakeHistory(currentUser)
        }
    }

    private fun fetchWaterIntakeForToday(currentUser: String, currentDate: String, waterGoal: Int) {
        database.child("dailywaterconsumption").child(currentUser).child(currentDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val waterIntake = snapshot.child("waterIntake").getValue(Int::class.java) ?: 0
                        totalWaterIntake = waterIntake

                        // Update UI with water intake and water goal
                        binding.waterIntakeText.text = "$totalWaterIntake ml / $waterGoal ml"

                        // Update progress bar
                        binding.waterIntakeProgress.max = waterGoal
                        binding.waterIntakeProgress.progress = totalWaterIntake
                    } else {
                        // Handle case where no water intake data is available for today
                        totalWaterIntake = 0
                        binding.waterIntakeText.text = "$totalWaterIntake ml / $waterGoal ml"
                        binding.waterIntakeProgress.max = waterGoal
                        binding.waterIntakeProgress.progress = totalWaterIntake
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching today's water intake: ${error.message}")
                }
            })
    }

    private fun fetchWaterIntakeHistory(currentUser: String) {
        val waterIntakeHistoryRef = database.child("dailywaterconsumption").child(currentUser)

        waterIntakeHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                var index = 0

                for (dataSnapshot in snapshot.children) {
                    val waterIntake = dataSnapshot.child("waterIntake").getValue(Int::class.java) ?: 0
                    entries.add(Entry(index.toFloat(), waterIntake.toFloat()))
                    index++
                }

                // Plot the chart
                plotChart(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching water intake history: ${error.message}")
            }
        })
    }

    private fun plotChart(entries: ArrayList<Entry>) {
        // Create dataset
        val dataSet = LineDataSet(entries, "Water Intake History").apply {
            color = resources.getColor(R.color.purple_200)
            valueTextColor = resources.getColor(R.color.black)
            lineWidth = 3f // Increase line thickness
            circleRadius = 5f // Circle indicators size
            setCircleColor(resources.getColor(R.color.purple_500)) // Circle color
            setCircleColorHole(resources.getColor(R.color.white))
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth line
            setDrawFilled(true) // Fill below the line
            fillDrawable = ContextCompat.getDrawable(this@WaterIntakeDetailsActivity, R.drawable.gradient_fill) // Custom gradient drawable
            fillAlpha = 80 // Transparency of the fill
        }

        // Set data
        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Customize the chart appearance
        lineChart.apply {
            description.isEnabled = false // Disable description
            legend.isEnabled = true // Show legend
            axisLeft.apply {
                setDrawGridLines(false) // Hide grid lines
                textColor = resources.getColor(R.color.black) // Y-axis text color
                axisMinimum = 0f // Minimum value
            }
            axisRight.isEnabled = false // Disable right axis
            xAxis.apply {
                setDrawGridLines(false) // Hide grid lines
                textColor = resources.getColor(R.color.black) // X-axis text color
                position = XAxis.XAxisPosition.BOTTOM // Position at the bottom
            }
            animateX(1000) // Animate the x-axis on loading
        }

        // Refresh the chart
        lineChart.invalidate()
    }

    // Helper function to get the current date in YYYY-MM-DD format
    private fun getCurrentDate(): String {
        // Format the current date as yyyy-MM-dd
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date()) // Returns current date
    }
}
