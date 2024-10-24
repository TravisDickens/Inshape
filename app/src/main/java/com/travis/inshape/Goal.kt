package com.travis.inshape

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = "user_goals",
    val stepGoal: String,
    val waterGoal: String,
    val calorieGoal: String
)

