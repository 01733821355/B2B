package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChatMessageEntity
import com.example.data.model.EventResponseEntity
import com.example.data.model.TeamEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunicationDao {

  // Messages
  @Query("SELECT * FROM chat_messages WHERE recipientRmCode IS NULL OR recipientRmCode = :myRmCode OR senderRmCode = :myRmCode ORDER BY timestamp ASC")
  fun getVisibleMessagesFlow(myRmCode: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE recipientRmCode IS NULL ORDER BY timestamp ASC")
  fun getTeamHubMessagesFlow(): Flow<List<ChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity)

  @Query("DELETE FROM chat_messages WHERE id = :id")
  suspend fun deleteMessage(id: String)

  @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 200")
  suspend fun getAllMessages(): List<ChatMessageEntity>

  // Events
  @Query("SELECT * FROM team_events ORDER BY createdAt DESC")
  fun getAllEventsFlow(): Flow<List<TeamEventEntity>>

  @Query("SELECT * FROM team_events ORDER BY createdAt DESC")
  suspend fun getAllEventsList(): List<TeamEventEntity>


  @Query("SELECT * FROM team_events WHERE eventId = :eventId LIMIT 1")
  suspend fun getEventById(eventId: String): TeamEventEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: TeamEventEntity)

  @Query("UPDATE team_events SET status = :status WHERE eventId = :eventId")
  suspend fun updateEventStatus(eventId: String, status: String)

  // Event Responses (e.g. Hand delivery dates, files count)
  @Query("SELECT * FROM event_responses WHERE eventId = :eventId ORDER BY submittedAt DESC")
  fun getResponsesForEventFlow(eventId: String): Flow<List<EventResponseEntity>>

  @Query("SELECT * FROM event_responses WHERE eventId = :eventId ORDER BY submittedAt DESC")
  suspend fun getResponsesForEvent(eventId: String): List<EventResponseEntity>

  @Query("SELECT * FROM event_responses WHERE eventId = :eventId AND UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode)) LIMIT 1")
  suspend fun getUserResponseForEvent(eventId: String, rmCode: String): EventResponseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateResponse(response: EventResponseEntity)

  @Query("SELECT COUNT(*) FROM event_responses WHERE eventId = :eventId")
  suspend fun getResponseCountForEvent(eventId: String): Int

  @Query("SELECT * FROM event_responses ORDER BY submittedAt DESC")
  suspend fun getAllResponsesList(): List<EventResponseEntity>

  @Query("DELETE FROM team_events WHERE eventId = :eventId")
  suspend fun deleteEvent(eventId: String)

  @Query("DELETE FROM event_responses WHERE responseId = :responseId")
  suspend fun deleteResponse(responseId: String)
}
