package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsNotificationDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSms(sms: SmsNotificationEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(list: List<SmsNotificationEntity>)

  @Query("SELECT * FROM sms_notifications WHERE recipientRmCode = :rmCode ORDER BY sentTimestamp DESC")
  fun getSmsForRmFlow(rmCode: String): Flow<List<SmsNotificationEntity>>

  @Query("SELECT * FROM sms_notifications ORDER BY sentTimestamp DESC")
  fun getAllSmsFlow(): Flow<List<SmsNotificationEntity>>

  @Query("SELECT * FROM sms_notifications ORDER BY sentTimestamp DESC")
  suspend fun getAllSms(): List<SmsNotificationEntity>

  @Query("SELECT COUNT(*) FROM sms_notifications WHERE recipientRmCode = :rmCode AND isRead = 0")
  fun getUnreadSmsCountFlow(rmCode: String): Flow<Int>

  @Query("UPDATE sms_notifications SET isRead = 1 WHERE id = :id")
  suspend fun markAsRead(id: Long)

  @Query("UPDATE sms_notifications SET isRead = 1 WHERE recipientRmCode = :rmCode")
  suspend fun markAllAsReadForRm(rmCode: String)

  @Query("DELETE FROM sms_notifications WHERE id = :id")
  suspend fun deleteSms(id: Long)

  @Query("DELETE FROM sms_notifications WHERE recipientRmCode = :rmCode")
  suspend fun clearSmsForRm(rmCode: String)

  @Query("DELETE FROM sms_notifications")
  suspend fun clearAllSms()
}
