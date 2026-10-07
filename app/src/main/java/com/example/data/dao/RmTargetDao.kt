package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RmTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RmTargetDao {
  @Query("SELECT * FROM rm_targets WHERE rmCode = :rmCode LIMIT 1")
  fun getTargetForRmFlow(rmCode: String): Flow<RmTargetEntity?>

  @Query("SELECT * FROM rm_targets WHERE rmCode = :rmCode LIMIT 1")
  suspend fun getTargetForRm(rmCode: String): RmTargetEntity?

  @Query("SELECT * FROM rm_targets")
  fun getAllTargetsFlow(): Flow<List<RmTargetEntity>>

  @Query("SELECT * FROM rm_targets")
  suspend fun getAllTargets(): List<RmTargetEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateTarget(target: RmTargetEntity)

  @Query("DELETE FROM rm_targets WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun deleteTargetForRm(rmCode: String)
}
