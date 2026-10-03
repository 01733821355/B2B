package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppSettingEntity
import com.example.data.model.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
  @Query("SELECT * FROM app_settings")
  fun getAllSettingsFlow(): Flow<List<AppSettingEntity>>

  @Query("SELECT * FROM app_settings")
  suspend fun getAllSettings(): List<AppSettingEntity>

  @Query("SELECT * FROM app_settings WHERE settingKey = :key LIMIT 1")
  suspend fun getSetting(key: String): AppSettingEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateSetting(setting: AppSettingEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateSettings(settings: List<AppSettingEntity>)

  @Query("SELECT * FROM sync_status WHERE id = 1 LIMIT 1")
  fun getSyncStatusFlow(): Flow<SyncStatusEntity?>

  @Query("SELECT * FROM sync_status WHERE id = 1 LIMIT 1")
  suspend fun getSyncStatus(): SyncStatusEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateSyncStatus(syncStatus: SyncStatusEntity)
}
