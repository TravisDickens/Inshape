package com.travis.inshape
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert
    suspend fun insert(weight: Weight)

    @Update
    suspend fun update(weight: Weight)

    @Query("SELECT * FROM weights WHERE isSynced = 0")
    suspend fun getUnsyncedWeights(): List<Weight>

    @Query("SELECT * FROM weights")
    fun getAllWeights(): Flow<List<Weight>>
}