package com.travis.inshape

data class FoodItem(
    val calories: Int,
    val carbohydrates: Double,
    val fat: Double,
    val fibre: Double,
    val name: String,
    val protein: Double,
    val sugars: Double,
    val vitamins: Vitamins
)