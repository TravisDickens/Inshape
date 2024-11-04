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
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BurntCaloriesDetailsActivity : Base() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var lineChart: LineChart
    private lateinit var tabLayout: TabLayout
    private lateinit var currentDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_burnt_calories_details)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Get today's date
        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Initialize views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        totalCaloriesTextView = findViewById(R.id.totalCalories)
        lineChart = findViewById(R.id.lineChart)
        tabLayout = findViewById(R.id.tabLayout)

        // Setup toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Fetch calorie data for today
        fetchCalorieData(currentDate)

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedTab = tab?.text
                when (selectedTab) {
                    "Day" -> fetchCalorieData(currentDate)  // Fetch daily data
                    "Week" -> fetchWeeklyCalories()         // Fetch weekly data
                    "Month" -> fetchMonthlyCalories()       // Fetch monthly data
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    //method to fetch calories burnt from firebase
    private fun fetchCalorieData(date: String) {
        val userId = auth.currentUser?.uid.toString()
        database.child("users").child(userId).child("dailyData").child(date)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (snapshot.exists()) {
                            val calories =
                                snapshot.child("calories").getValue(Double::class.java) ?: 0.0

                            // Display the calories in the TextView
                            totalCaloriesTextView.text = "${calories.toInt()} Kcal"

                            // Optionally, update LineChart here
                            updateLineChart(listOf(calories.toInt()))
                        } else {
                            Toast.makeText(
                                this@BurntCaloriesDetailsActivity,
                                "No data available for today",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@BurntCaloriesDetailsActivity,
                            "Error processing data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@BurntCaloriesDetailsActivity,
                        "Failed to fetch data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    //method to fetch calorie data for the last 7 days
    private fun fetchWeeklyCalories() {
        val userId = auth.currentUser?.uid.toString()
        val caloriesPerWeek = mutableListOf<Int>()
        val calendar = Calendar.getInstance()

        // Fetch data for the past 7 days
        for (i in 0..6) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            database.child("users").child(userId).child("dailyData").child(date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val calories =
                                snapshot.child("calories").getValue(Double::class.java) ?: 0.0
                            caloriesPerWeek.add(calories.toInt())

                            if (caloriesPerWeek.size == 7) {
                                // Update LineChart with weekly data
                                updateLineChart(caloriesPerWeek)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@BurntCaloriesDetailsActivity,
                                "Error processing weekly data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@BurntCaloriesDetailsActivity,
                            "Failed to fetch weekly data: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            // Move to the previous day
            calendar.add(Calendar.DATE, -1)
        }
    }

    //method to fetch calories burnt for the last 30 days
    private fun fetchMonthlyCalories() {
        val userId = auth.currentUser?.uid.toString()
        val caloriesPerMonth = mutableListOf<Int>()
        val calendar = Calendar.getInstance()

        // Fetch data for the past 30 days
        for (i in 0..29) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            database.child("users").child(userId).child("dailyData").child(date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val calories =
                                snapshot.child("calories").getValue(Double::class.java) ?: 0.0
                            caloriesPerMonth.add(calories.toInt())

                            if (caloriesPerMonth.size == 30) {
                                // Update LineChart with monthly data
                                updateLineChart(caloriesPerMonth)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@BurntCaloriesDetailsActivity,
                                "Error processing monthly data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@BurntCaloriesDetailsActivity,
                            "Failed to fetch monthly data: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            // Move to the previous day
            calendar.add(Calendar.DATE, -1)
        }
    }

    private fun updateLineChart(caloriesData: List<Int>) {
        // Reverse the data so that the oldest date is on the left, and today is on the right
        val reversedCaloriesData = caloriesData.reversed()

        val entries = ArrayList<Entry>()

        // Iterate through the reversed list to create entries
        for (i in reversedCaloriesData.indices) {
            entries.add(Entry(i.toFloat(), reversedCaloriesData[i].toFloat()))
        }

        // Create the data set with custom styling
        val dataSet = LineDataSet(entries, "Calories Burned").apply {
            // Set line color
            color = ContextCompat.getColor(this@BurntCaloriesDetailsActivity, R.color.my_secondary)
            // Line width
            lineWidth = 2f
            // Circle radius for data points
            circleRadius = 5f
            // Circle color
            setCircleColor(ContextCompat.getColor(this@BurntCaloriesDetailsActivity, R.color.white))
            // Circle hole color
            setCircleColorHole(
                ContextCompat.getColor(
                    this@BurntCaloriesDetailsActivity,
                    R.color.my_primary
                )
            )
            // Use cubic bezier for smooth line
            mode = LineDataSet.Mode.CUBIC_BEZIER
            // Value text color
            valueTextColor =
                ContextCompat.getColor(this@BurntCaloriesDetailsActivity, R.color.black)
            // Value text size
            valueTextSize = 10f

            // Add a gradient fill
            setDrawFilled(true)
            // Custom gradient drawable
            fillDrawable = ContextCompat.getDrawable(
                this@BurntCaloriesDetailsActivity,
                R.drawable.gradient_fill
            )
        }

        val lineData = LineData(dataSet)
        // Use the custom IntValueFormatter for integer values
        lineData.setValueFormatter(IntValueFormatter())

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
                // Disable x axis
                isEnabled = false
            }
            axisLeft.apply {
                // Disable grid lines on left y-axis
                setDrawGridLines(false)
            }
            // Disable right y-axis
            axisRight.isEnabled = false

            // Refresh the chart
            invalidate()

            // Add animation when the data is set
            animateX(1000) // Animation duration in milliseconds
        }
    }

}
