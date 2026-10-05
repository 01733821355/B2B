package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
  fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

  @Query("SELECT * FROM audit_logs WHERE rmCode = :rmCode ORDER BY timestamp DESC")
  fun getLogsForRmFlow(rmCode: String): Flow<List<AuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: AuditLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLogs(logs: List<AuditLogEntity>)

  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 500")
  suspend fun getAllLogs(): List<AuditLogEntity>

  @Query("DELETE FROM audit_logs WHERE action NOT IN ('LOGIN', 'LOGOUT')")
  suspend fun purgeNonAuthLogs()
}
