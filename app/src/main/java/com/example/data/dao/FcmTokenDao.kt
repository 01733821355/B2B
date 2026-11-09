package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FcmTokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FcmTokenDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertToken(token: FcmTokenEntity)

  @Query("SELECT * FROM fcm_tokens WHERE rmCode = :rmCode")
  suspend fun getTokensForRm(rmCode: String): List<FcmTokenEntity>

  @Query("SELECT * FROM fcm_tokens WHERE token = :token LIMIT 1")
  suspend fun getToken(token: String): FcmTokenEntity?

  @Query("SELECT * FROM fcm_tokens ORDER BY updatedAt DESC")
  fun getAllTokensFlow(): Flow<List<FcmTokenEntity>>

  @Query("SELECT * FROM fcm_tokens WHERE isSynced = 0")
  suspend fun getUnsyncedTokens(): List<FcmTokenEntity>

  @Query("UPDATE fcm_tokens SET isSynced = 1 WHERE token = :token")
  suspend fun markSynced(token: String)

  @Query("DELETE FROM fcm_tokens WHERE token = :token")
  suspend fun deleteToken(token: String)
}
