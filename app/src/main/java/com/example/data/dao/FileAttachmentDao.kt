package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FileAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileAttachmentDao {
  @Query("SELECT * FROM file_attachments WHERE fileId = :fileId ORDER BY uploadedAt DESC")
  fun getAttachmentsForFileFlow(fileId: String): Flow<List<FileAttachmentEntity>>

  @Query("SELECT * FROM file_attachments WHERE fileId = :fileId ORDER BY uploadedAt DESC")
  suspend fun getAttachmentsForFile(fileId: String): List<FileAttachmentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachment(attachment: FileAttachmentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachments(attachments: List<FileAttachmentEntity>)

  @Query("DELETE FROM file_attachments WHERE attachmentId = :attachmentId")
  suspend fun deleteAttachment(attachmentId: String)

  @Query("SELECT * FROM file_attachments ORDER BY uploadedAt DESC")
  suspend fun getAllAttachments(): List<FileAttachmentEntity>
}
