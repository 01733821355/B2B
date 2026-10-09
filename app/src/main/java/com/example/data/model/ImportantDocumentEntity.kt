package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "important_documents",
  indices = [
    Index(value = ["category"]),
    Index(value = ["createdAt"]),
    Index(value = ["isDeleted"])
  ]
)
data class ImportantDocumentEntity(
  @PrimaryKey
  val docId: String,
  val title: String, // Custom name given by Admin/Mentor
  val category: String, // "Policies & Circulars", "Forms & Formats", "Product Guidelines", "CPV & Compliance", "Notices & Announcements", "Other"
  val description: String = "",
  val fileName: String,
  val fileType: String, // "application/pdf", "image/jpeg", "image/png", etc.
  val fileSizeBytes: Long = 0L,
  val fileUri: String = "",
  val storagePath: String = "",
  val uploadedBy: String, // RM Code / User ID of uploader
  val uploaderName: String,
  val uploaderRole: String, // "ADMIN" or "MENTOR"
  val createdAt: Long,
  val updatedAt: Long,
  val isDeleted: Boolean = false,
  val isSynced: Boolean = false
)
