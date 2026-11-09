package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fcm_tokens")
data class FcmTokenEntity(
  @PrimaryKey
  val token: String,
  val rmCode: String,
  val userName: String,
  val role: String,
  val deviceModel: String,
  val androidVersion: String,
  val updatedAt: Long,
  val isSynced: Boolean = false
)
