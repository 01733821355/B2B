package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.UserEntity
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository(private val database: AppDatabase) {

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  suspend fun login(
    rmCodeInput: String,
    passwordInput: String,
    latitude: Double? = null,
    longitude: Double? = null,
    address: String? = null
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanRmCode = rmCodeInput.trim()
    val cleanPassword = passwordInput.trim()

    val user = database.userDao().getUser(cleanRmCode)
      ?: return@withContext Result.failure(Exception("Invalid username / RM Code or password."))

    if (user.accountStatus.equals("PENDING_APPROVAL", ignoreCase = true)) {
      return@withContext Result.failure(Exception("This RM account is pending Mentor approval. You can log in once your Mentor approves."))
    }
    if (user.accountStatus.equals("INACTIVE", ignoreCase = true)) {
      return@withContext Result.failure(Exception("This account is inactive. Please contact the administrator."))
    }
    if (user.accountStatus.equals("SUSPENDED", ignoreCase = true)) {
      return@withContext Result.failure(Exception("This account has been suspended for security reasons."))
    }

    val isValid = SecurityUtils.verifyPassword(cleanPassword, user.salt, user.passwordHash)
    if (!isValid) {
      return@withContext Result.failure(Exception("Invalid username / RM Code or password."))
    }

    val now = DateUtils.currentDhakaMillis()
    database.userDao().updateLastLogin(user.rmCode, now)

    // If location is provided on login, update coordinates & history for Mentor tracking
    if (latitude != null && longitude != null) {
      database.userDao().updateLocation(
        rmCode = user.rmCode,
        lat = latitude,
        lng = longitude,
        address = address ?: "Detected on Login",
        time = now
      )
      database.userLocationLogDao().insertLocationLog(
        com.example.data.model.UserLocationLogEntity(
          rmCode = user.rmCode,
          userName = user.name,
          latitude = latitude,
          longitude = longitude,
          address = address ?: "Detected on Login",
          sourceAction = "LOGIN_TRACKING",
          timestamp = now
        )
      )
    }
    
    // Log login audit with time and location as specifically requested
    val locDesc = if (!address.isNullOrBlank()) address else if (latitude != null && longitude != null) "Lat: $latitude, Lng: $longitude" else "Location: GPS Auto"
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-AUTH-${SecurityUtils.generateUniqueId().take(8)}",
        userId = user.rmCode,
        role = user.role,
        action = "LOGIN",
        rmCode = if (user.role == "RM") user.rmCode else null,
        timestamp = now,
        details = "${user.name} logged in. Time: ${DateUtils.formatDateTime(now)} | Location: $locDesc"
      )
    )

    val updatedUser = user.copy(
      lastLogin = now,
      lastLatitude = latitude ?: user.lastLatitude,
      lastLongitude = longitude ?: user.lastLongitude,
      lastLocationAddress = address ?: user.lastLocationAddress,
      lastLocationTime = if (latitude != null) now else user.lastLocationTime,
      isOnline = true
    )
    _currentUser.value = updatedUser
    Result.success(updatedUser)
  }

  suspend fun logout() = withContext(Dispatchers.IO) {
    val user = _currentUser.value
    if (user != null) {
      val now = DateUtils.currentDhakaMillis()
      val logoutLoc = if (!user.lastLocationAddress.isNullOrBlank()) {
        user.lastLocationAddress
      } else if (user.lastLatitude != null && user.lastLongitude != null) {
        "Lat: ${user.lastLatitude}, Lng: ${user.lastLongitude}"
      } else {
        "Dhaka Operations"
      }
      database.auditLogDao().insertLog(
        AuditLogEntity(
          logId = "LOG-LOGOUT-${SecurityUtils.generateUniqueId().take(8)}",
          userId = user.rmCode,
          role = user.role,
          action = "LOGOUT",
          rmCode = if (user.role == "RM") user.rmCode else null,
          timestamp = now,
          details = "${user.name} logged out. Time: ${DateUtils.formatDateTime(now)} | Location: $logoutLoc"
        )
      )
    }
    _currentUser.value = null
  }

  suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not authenticated."))

    if (!SecurityUtils.verifyPassword(oldPassword.trim(), user.salt, user.passwordHash)) {
      return@withContext Result.failure(Exception("Current password does not match."))
    }

    val (valid, errorMsg) = SecurityUtils.isPasswordValid(newPassword.trim())
    if (!valid) {
      return@withContext Result.failure(Exception(errorMsg ?: "Invalid new password."))
    }

    val newSalt = SecurityUtils.generateSalt()
    val newHash = SecurityUtils.hashPassword(newPassword.trim(), newSalt)
    database.userDao().updatePassword(user.rmCode, newHash, newSalt, mustChange = false)

    _currentUser.value = user.copy(
      passwordHash = newHash,
      salt = newSalt,
      mustChangePassword = false
    )

    Result.success(Unit)
  }
}
