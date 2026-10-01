package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomerFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerFileDao {
  @Query("SELECT * FROM customer_files WHERE fileId = :fileId LIMIT 1")
  suspend fun getFileById(fileId: String): CustomerFileEntity?

  @Query("SELECT * FROM customer_files WHERE fileId = :fileId LIMIT 1")
  fun getFileByIdFlow(fileId: String): Flow<CustomerFileEntity?>

  @Query("SELECT * FROM customer_files WHERE assignedRmCode = :rmCode AND isDeleted = 0 ORDER BY updatedAt DESC")
  fun getFilesForRmFlow(rmCode: String): Flow<List<CustomerFileEntity>>

  @Query("SELECT * FROM customer_files WHERE isDeleted = 0 ORDER BY updatedAt DESC")
  fun getAllActiveFilesFlow(): Flow<List<CustomerFileEntity>>

  @Query("SELECT * FROM customer_files ORDER BY updatedAt DESC")
  fun getAllFilesIncludingDeletedFlow(): Flow<List<CustomerFileEntity>>

  @Query("SELECT * FROM customer_files WHERE isSynced = 0")
  suspend fun getUnsyncedFiles(): List<CustomerFileEntity>

  @Query("SELECT COUNT(*) FROM customer_files WHERE isSynced = 0")
  fun getUnsyncedCountFlow(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFile(file: CustomerFileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFiles(files: List<CustomerFileEntity>)

  @Update
  suspend fun updateFile(file: CustomerFileEntity)

  @Query("UPDATE customer_files SET isDeleted = 1, updatedBy = :updatedBy, updatedAt = :updatedAt, isSynced = 0 WHERE fileId = :fileId")
  suspend fun softDeleteFile(fileId: String, updatedBy: String, updatedAt: Long)

  @Query("UPDATE customer_files SET isDeleted = 0, updatedBy = :updatedBy, updatedAt = :updatedAt, isSynced = 0 WHERE fileId = :fileId")
  suspend fun restoreFile(fileId: String, updatedBy: String, updatedAt: Long)

  @Query("DELETE FROM customer_files WHERE fileId = :fileId")
  suspend fun permanentDeleteFile(fileId: String)

  @Query("UPDATE customer_files SET isSynced = 1 WHERE fileId IN (:fileIds)")
  suspend fun markFilesSynced(fileIds: List<String>)
}
