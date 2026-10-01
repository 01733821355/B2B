package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
  ADMIN,
  MENTOR,
  RM
}

enum class AccountStatus {
  ACTIVE,
  INACTIVE,
  SUSPENDED
}

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey
  val rmCode: String, // e.g. "Admin0", "12345", "104393"
  val name: String,
  val role: String, // "ADMIN", "MENTOR", "RM"
  val passwordHash: String,
  val salt: String,
  val mobile: String,
  val email: String,
  val officeAddress: String,
  val accountStatus: String = "ACTIVE", // ACTIVE, INACTIVE, SUSPENDED
  val mustChangePassword: Boolean = false,
  val createdAt: Long,
  val lastLogin: Long? = null,
  val authUid: String = "",
  val lastLatitude: Double? = null,
  val lastLongitude: Double? = null,
  val lastLocationAddress: String? = null,
  val lastLocationTime: Long? = null,
  val isOnline: Boolean = true
)
