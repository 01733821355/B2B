package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode)) LIMIT 1")
  suspend fun getUser(rmCode: String): UserEntity?

  @Query("SELECT * FROM users WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode)) LIMIT 1")
  fun getUserFlow(rmCode: String): Flow<UserEntity?>

  @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'RM' ORDER BY createdAt DESC")
  fun getAllRmsFlow(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) IN ('RM', 'MENTOR', 'ADMIN') ORDER BY role ASC, name ASC")
  fun getAllOfficersFlow(): Flow<List<UserEntity>>


  @Query("SELECT * FROM users WHERE UPPER(TRIM(role)) = 'MENTOR' ORDER BY name ASC")
  fun getAllMentorsFlow(): Flow<List<UserEntity>>

  @Query("UPDATE users SET role = :role WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateUserRole(rmCode: String, role: String)

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  suspend fun getAllUsers(): List<UserEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(users: List<UserEntity>)

  @Update
  suspend fun updateUser(user: UserEntity)

  @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt, mustChangePassword = :mustChange WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updatePassword(rmCode: String, passwordHash: String, salt: String, mustChange: Boolean)

  @Query("UPDATE users SET accountStatus = :status WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateStatus(rmCode: String, status: String)

  @Query("UPDATE users SET name = :name, mobile = :mobile, email = :email, officeAddress = :officeAddress WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateUserDetails(rmCode: String, name: String, mobile: String, email: String, officeAddress: String)

  @Query("UPDATE users SET lastLogin = :timestamp, isOnline = 1 WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateLastLogin(rmCode: String, timestamp: Long)

  @Query("UPDATE users SET lastLatitude = :latitude, lastLongitude = :longitude, lastLocationAddress = :address, lastLocationTime = :timestamp WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateUserLocation(rmCode: String, latitude: Double, longitude: Double, address: String, timestamp: Long)

  @Query("UPDATE users SET lastLatitude = :lat, lastLongitude = :lng, lastLocationAddress = :address, lastLocationTime = :time WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateLocation(rmCode: String, lat: Double, lng: Double, address: String, time: Long)

  @Query("UPDATE users SET isOnline = :isOnline WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun updateOnlineStatus(rmCode: String, isOnline: Boolean)

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  fun getAllUsersFlow(): Flow<List<UserEntity>>

  @Query("DELETE FROM users WHERE UPPER(TRIM(rmCode)) = UPPER(TRIM(:rmCode))")
  suspend fun deleteUser(rmCode: String)
}
