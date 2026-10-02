package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rm_targets")
data class RmTargetEntity(
  @PrimaryKey val rmCode: String,
  val creditCardTarget: Int = 20,
  val corporateCardTarget: Int = 10,
  val b2bTarget: Int = 15,
  val updatedAt: Long = System.currentTimeMillis()
)
