package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserLocationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLocationLogDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLocationLog(log: UserLocationLogEntity): Long

  @Query("SELECT * FROM user_location_logs ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentLocationLogsFlow(limit: Int = 100): Flow<List<UserLocationLogEntity>>

  @Query("SELECT * FROM user_location_logs WHERE rmCode = :rmCode ORDER BY timestamp DESC LIMIT :limit")
  fun getLocationLogsForRmFlow(rmCode: String, limit: Int = 50): Flow<List<UserLocationLogEntity>>

  @Query("SELECT * FROM user_location_logs ORDER BY timestamp DESC LIMIT :limit")
  suspend fun getRecentLocationLogs(limit: Int = 100): List<UserLocationLogEntity>

  @Query("DELETE FROM user_location_logs WHERE timestamp < :beforeTimestamp")
  suspend fun deleteOldLogs(beforeTimestamp: Long): Int
}
