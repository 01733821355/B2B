package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "file_attachments",
  indices = [Index(value = ["fileId"])]
)
data class FileAttachmentEntity(
  @PrimaryKey
  val attachmentId: String,
  val fileId: String,
  val category: String, // "NID", "Bank Statement", "Office ID", "Salary Certificate", "CPV", "General"
  val fileName: String,
  val fileType: String, // "image/jpeg", "image/png", "application/pdf"
  val storagePath: String = "",
  val fileUri: String = "",
  val fileSizeBytes: Long = 0L,
  val uploadedBy: String,
  val uploadedAt: Long
)
