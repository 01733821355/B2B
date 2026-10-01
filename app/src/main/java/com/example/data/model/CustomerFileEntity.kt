package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "customer_files",
  indices = [
    Index(value = ["assignedRmCode"]),
    Index(value = ["applicationStatus"]),
    Index(value = ["productType"]),
    Index(value = ["isDeleted"])
  ]
)
data class CustomerFileEntity(
  @PrimaryKey
  val fileId: String,
  val customerName: String,
  val companyName: String,
  val officeAddress: String,
  val mobile: String,
  val altMobile: String = "",
  val email: String = "",
  val productType: String,
  val applicationStatus: String,
  val activeStatus: String = "N", // Y, N, C
  val assignedRmCode: String,
  val ccNumber: String = "", // Credit Card / Reference Number
  val pendingDocuments: String = "", // Comma-separated list of pending documents
  val remarks: String = "",
  
  // CPV Information
  val cpvStatus: String = "Pending", // Pending, Completed, Failed, Not Required
  val cpvDate: String = "",
  val cpvAddress: String = "",
  val cpvRemarks: String = "",
  val cpvPhotoUri: String = "",
  val cpvSupportingDocUri: String = "",
  val cpvLastUpdatedBy: String = "",

  // RM Submission GPS Location (Auto-captured upon file entry)
  val submissionLatitude: Double? = null,
  val submissionLongitude: Double? = null,
  val submissionAddress: String? = null,

  // Timestamps and Tracking
  val createdAt: Long,
  val updatedAt: Long,
  val submittedAt: Long? = null,
  val approvedAt: Long? = null,
  val createdBy: String,
  val updatedBy: String,
  val isDeleted: Boolean = false,
  val isSynced: Boolean = false
)
