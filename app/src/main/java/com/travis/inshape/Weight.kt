package com.travis.inshape

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weights")
data class Weight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weight: String,
    val timestamp: Long,
    var isSynced: Boolean = false,
    var firebaseId: String? = null
)
