package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StepDetailsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var totalStepsTextView: TextView
    private lateinit var lineChart: LineChart
    private lateinit var tabLayout: TabLayout
    private lateinit var currentDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_details)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Get today's date
        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Initialize views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        totalStepsTextView = findViewById(R.id.totalSteps)
        lineChart = findViewById(R.id.lineChart)
        tabLayout = findViewById(R.id.tabLayout)

        // Setup toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Fetch step data for today
        fetchStepData(currentDate)

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedTab = tab?.text
                when (selectedTab) {
                    // Fetch daily data
                    "Day" -> fetchStepData(currentDate)
                    // Fetch weekly data
                    "Week" -> fetchWeeklyData()
                    // Fetch monthly data
                    "Month" -> fetchMonthlyData()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchStepData(date: String) {
        val userId = auth.currentUser?.uid.toString()
        database.child("users").child(userId).child("dailyData").child(date)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (snapshot.exists()) {
                            val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0

                            // Display the steps in the TextView
                            totalStepsTextView.text = steps.toString()

                            //update LineChart here
                            updateLineChart(listOf(steps))
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@StepDetailsActivity, "Error processing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StepDetailsActivity, "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchWeeklyData() {
        val userId = auth.currentUser?.uid.toString()
        val stepsPerWeek = mutableListOf<Int>()
        val calendar = Calendar.getInstance()

        // Fetch data for the past 7 days
        for (i in 0..6) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            database.child("users").child(userId).child("dailyData").child(date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0
                            stepsPerWeek.add(steps)

                            if (stepsPerWeek.size == 7) {
                                // Update LineChart with weekly data
                                updateLineChart(stepsPerWeek)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@StepDetailsActivity, "Error processing weekly data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@StepDetailsActivity, "Failed to fetch weekly data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            // Move to the previous day
            calendar.add(Calendar.DATE, -1)
        }
    }

    private fun fetchMonthlyData() {
        val userId = auth.currentUser?.uid.toString()
        val stepsPerMonth = mutableListOf<Int>()
        val calendar = Calendar.getInstance()

        // Fetch data for the past 30 days
        for (i in 0..29) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            database.child("users").child(userId).child("dailyData").child(date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0
                            stepsPerMonth.add(steps)

                            if (stepsPerMonth.size == 30) {
                                // Update LineChart with monthly data
                                updateLineChart(stepsPerMonth)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@StepDetailsActivity, "Error processing monthly data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@StepDetailsActivity, "Failed to fetch monthly data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            // Move to the previous day
            calendar.add(Calendar.DATE, -1)
        }
    }

    private fun updateLineChart(stepsData: List<Int>) {
        try {
            // Reverse the data so that the oldest date is on the left, and today is on the right
            val reversedStepsData = stepsData.reversed()

            val entries = ArrayList<Entry>()

            // Iterate through the reversed list to create entries
            for (i in reversedStepsData.indices) {
                entries.add(Entry(i.toFloat(), reversedStepsData[i].toFloat()))
            }

            // Create the data set with custom styling
            val dataSet = LineDataSet(entries, "Steps").apply {
                // Line color
                color = ContextCompat.getColor(this@StepDetailsActivity, R.color.my_secondary)
                // Line width
                lineWidth = 2f
                // Circle radius for data points
                circleRadius = 5f
                // Circle color
                setCircleColor(ContextCompat.getColor(this@StepDetailsActivity, R.color.white))
                // Circle hole color
                setCircleColorHole(ContextCompat.getColor(this@StepDetailsActivity, R.color.my_primary))
                // Use cubic bezier for smooth line
                mode = LineDataSet.Mode.CUBIC_BEZIER
                // Value text color
                valueTextColor = ContextCompat.getColor(this@StepDetailsActivity, R.color.black)
                valueTextSize = 10f // Value text size

                //Add a gradient fill
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(this@StepDetailsActivity, R.drawable.gradient_fill)
            }

            val lineData = LineData(dataSet)

            // Customize the chart appearance
            lineChart.apply {
                data = lineData
                // Disable description label
                description.isEnabled = false
                // Disable legend
                legend.isEnabled = false
                // Disable grid background
                setDrawGridBackground(false)
                // Disable borders
                setDrawBorders(false)
                xAxis.apply {
                    // Disable grid lines on x-axis
                    setDrawGridLines(false)
                    // Positioning of x-axis labels
                    position = XAxis.XAxisPosition.BOTTOM
                    xAxis.isEnabled = false
                }
                axisLeft.apply {
                    // Disable grid lines on left y-axis
                    setDrawGridLines(false)
                }
                // Disable right y-axis
                axisRight.isEnabled = false
                // Animation for line chart
                animateX(500)
            }
             // Refresh chart with new data
            lineChart.invalidate()
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating line chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
