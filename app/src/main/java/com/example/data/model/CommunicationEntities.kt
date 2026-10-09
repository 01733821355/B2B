package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "chat_messages",
  indices = [
    Index(value = ["timestamp"]),
    Index(value = ["recipientRmCode"])
  ]
)
data class ChatMessageEntity(
  @PrimaryKey
  val id: String,
  val senderRmCode: String,
  val senderName: String,
  val senderRole: String, // "ADMIN", "MENTOR", "RM"
  val recipientRmCode: String? = null, // null for General Team Hub broadcast, or specific RM code
  val messageText: String,
  val timestamp: Long,
  val messageType: String = "TEXT", // "TEXT", "EVENT", "CALL_LOG", "ANNOUNCEMENT"
  val eventId: String? = null
)

@Entity(
  tableName = "team_events",
  indices = [
    Index(value = ["createdAt"]),
    Index(value = ["status"])
  ]
)
data class TeamEventEntity(
  @PrimaryKey
  val eventId: String,
  val title: String,
  val description: String,
  val creatorRmCode: String,
  val creatorName: String,
  val targetDate: String, // e.g. "12/10/2026"
  val createdAt: Long,
  val status: String = "ACTIVE" // "ACTIVE", "COMPLETED"
)

@Entity(
  tableName = "event_responses",
  indices = [
    Index(value = ["eventId"]),
    Index(value = ["rmCode"])
  ]
)
data class EventResponseEntity(
  @PrimaryKey
  val responseId: String,
  val eventId: String,
  val rmCode: String,
  val rmName: String,
  val filesCount: Int,
  val requestedDate: String,
  val location: String,
  val remarks: String = "",
  val submittedAt: Long
)
