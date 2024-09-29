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
                    "Day" -> fetchStepData(currentDate) // Fetch daily data
                    "Week" -> fetchWeeklyData()         // Fetch weekly data
                    "Month" -> fetchMonthlyData()       // Fetch monthly data

                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchStepData(date: String) {
        val userId = auth.currentUser?.uid.toString()
        database.child("users").child(userId).child("dailyData").child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0

                        // Display the steps in the TextView
                        totalStepsTextView.text = steps.toString()

                        // Optionally, update LineChart here
                        updateLineChart(listOf(steps))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StepDetailsActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
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
                        val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0
                        stepsPerWeek.add(steps)

                        if (stepsPerWeek.size == 7) {
                            // Update LineChart with weekly data
                            updateLineChart(stepsPerWeek)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
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
                        val steps = snapshot.child("steps").getValue(Int::class.java) ?: 0
                        stepsPerMonth.add(steps)

                        if (stepsPerMonth.size == 30) {
                            // Update LineChart with monthly data
                            updateLineChart(stepsPerMonth)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            // Move to the previous day
            calendar.add(Calendar.DATE, -1)
        }
    }




    private fun updateLineChart(stepsData: List<Int>) {
        // Reverse the data so that the oldest date is on the left, and today is on the right
        val reversedStepsData = stepsData.reversed()

        val entries = ArrayList<Entry>()

        // Iterate through the reversed list to create entries
        for (i in reversedStepsData.indices) {
            entries.add(Entry(i.toFloat(), reversedStepsData[i].toFloat()))
        }

        // Create the data set with custom styling
        val dataSet = LineDataSet(entries, "Steps").apply {
            color = ContextCompat.getColor(this@StepDetailsActivity, R.color.my_secondary) // Line color
            lineWidth = 2f // Line width
            circleRadius = 5f // Circle radius for data points
            setCircleColor(ContextCompat.getColor(this@StepDetailsActivity, R.color.white)) // Circle color
            setCircleColorHole(ContextCompat.getColor(this@StepDetailsActivity, R.color.my_primary)) // Circle hole color
            mode = LineDataSet.Mode.CUBIC_BEZIER // Use cubic bezier for smooth line
            valueTextColor = ContextCompat.getColor(this@StepDetailsActivity, R.color.black) // Value text color
            valueTextSize = 10f // Value text size

            // Optional: Add a gradient fill
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@StepDetailsActivity, R.drawable.gradient_fill) // Custom gradient drawable
        }

        val lineData = LineData(dataSet)

        // Customize the chart appearance
        lineChart.apply {
            data = lineData
            description.isEnabled = false // Disable description label
            legend.isEnabled = false // Disable legend
            setDrawGridBackground(false) // Disable grid background
            setDrawBorders(false) // Disable borders
            xAxis.apply {
                setDrawGridLines(false) // Disable grid lines on x-axis
                position = XAxis.XAxisPosition.BOTTOM // Positioning of x-axis labels
                xAxis.isEnabled = false
            }
            axisLeft.apply {
                setDrawGridLines(false) // Disable grid lines on left y-axis
            }
            axisRight.isEnabled = false // Disable right y-axis
            animateX(500) // Animation for line chart
        }

        lineChart.invalidate() // Refresh chart with new data
    }

}