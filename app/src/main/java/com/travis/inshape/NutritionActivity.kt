package com.travis.inshape

import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.travis.inshape.databinding.ActivityNutritionBinding
import kotlinx.coroutines.launch

class NutritionActivity : Base() {

    lateinit var binding: ActivityNutritionBinding
    private val mealTypes = listOf("Breakfast", "Lunch", "Supper")
    // List to hold food items
    var foodItems: List<FoodItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize NumberPicker for grams selection
        binding.gramsNumberPicker.minValue = 0
        binding.gramsNumberPicker.maxValue = 500

        // Meal Type Dropdown Adapter
        val mealTypesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mealTypes)
        binding.mealTypeInput.setAdapter(mealTypesAdapter)

        // Show dropdown when meal type input is clicked
        binding.mealTypeInput.setOnClickListener {
            binding.mealTypeInput.showDropDown()
        }

        // Handle meal type selection
        binding.mealTypeInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedMealType = parent.getItemAtPosition(position).toString()
            Toast.makeText(this, "Selected meal type: $selectedMealType", Toast.LENGTH_SHORT).show()
        }

        // Handle meal time selection with a TimePickerDialog
        binding.mealTimeInput.setOnClickListener {
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                binding.mealTimeInput.setText(selectedTime)
            }, 12, 0, true) // Default to 12:00 PM
            timePickerDialog.show()
        }

        // Listen for text changes to filter food items
        binding.searchFoodInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFoodItems(s.toString())
            }
        })

        // Fetch nutrition data from API when the activity is created
        fetchNutritionData()

        // Handle selection of food item from the dropdown
        binding.searchFoodInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedFoodName = parent.getItemAtPosition(position).toString()
            val selectedFood = foodItems.find { it.name == selectedFoodName }
            selectedFood?.let {
                Toast.makeText(this, "Selected: ${it.name}", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Food item not found!", Toast.LENGTH_SHORT).show()
        }

        // Save nutritional info on button click
        binding.saveCaloriesButton.setOnClickListener {
            saveNutritionalInfo()
        }
    }

    // Function to fetch nutrition data from API
    private fun fetchNutritionData() {
        val request = ServiceBuilder.buildService(NutritionApi::class.java)
        lifecycleScope.launch {
            try {
                val response = request.getNutritionData()
                if (response.isSuccessful) {
                    foodItems = response.body()?.record?.food_items ?: emptyList()
                    val foodNames = foodItems.map { it.name }
                    val adapter = ArrayAdapter(this@NutritionActivity, android.R.layout.simple_dropdown_item_1line, foodNames)
                    binding.searchFoodInput.setAdapter(adapter)
                } else {
                    Toast.makeText(this@NutritionActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Log the error and inform the user
                e.printStackTrace()
                Toast.makeText(this@NutritionActivity, "Error fetching nutrition data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to filter food items based on user input
    fun filterFoodItems(query: String) {
        val filtered = foodItems.filter { it.name.contains(query, ignoreCase = true) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filtered.map { it.name })
        binding.searchFoodInput.setAdapter(adapter)
        binding.searchFoodInput.showDropDown()
    }

    // Function to save nutritional info
    private fun saveNutritionalInfo() {
        val selectedFood = binding.searchFoodInput.text.toString()
        val selectedMealType = binding.mealTypeInput.text.toString()
        val selectedMealTime = binding.mealTimeInput.text.toString()
        val grams = binding.gramsNumberPicker.value

        val foodItem = foodItems.find { it.name == selectedFood }
        foodItem?.let {
            // Calculate nutrients based on grams
            val calories = it.calories * grams / 100.0
            val carbohydrates = it.carbohydrates * grams / 100.0
            val fat = it.fat * grams / 100.0
            val protein = it.protein * grams / 100.0
            val fibre = it.fibre * grams / 100.0
            val sugars = it.sugars * grams / 100.0

            // Pass all the required data to NutritionalInfoActivity
            val intent = Intent(this, NutritionalInfoActivity::class.java).apply {
                putExtra("calories", calories)
                putExtra("carbohydrates", carbohydrates)
                putExtra("fat", fat)
                putExtra("protein", protein)
                putExtra("fibre", fibre)
                putExtra("sugars", sugars)
                putExtra("food_name", selectedFood)
                putExtra("meal_type", selectedMealType)
                putExtra("meal_time", selectedMealTime)
                putExtra("grams", grams)
            }
            startActivity(intent)
        } ?: Toast.makeText(this, "Please select a valid food item.", Toast.LENGTH_SHORT).show()
    }
}
