package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "user_location_logs",
  indices = [Index(value = ["rmCode"]), Index(value = ["timestamp"])]
)
data class UserLocationLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val rmCode: String,
  val userName: String,
  val latitude: Double,
  val longitude: Double,
  val address: String,
  val sourceAction: String, // "GPS_AUTO_DETECT", "LOGIN", "CHECK_IN", "CUSTOMER_FILE_ENTRY"
  val relatedFileId: String? = null,
  val timestamp: Long
)
