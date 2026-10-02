package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.AppSettingEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CustomerFileEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.RmTargetEntity
import com.example.data.model.SmsNotificationEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.SecurityUtils
import com.example.util.SmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EblRepository(
  private val database: AppDatabase,
  private val authRepository: AuthRepository,
  private val context: Context? = null
) {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  // 1. Customer Files Access Control
  @OptIn(ExperimentalCoroutinesApi::class)
  fun getAuthorizedFilesFlow(): Flow<List<CustomerFileEntity>> {
    return authRepository.currentUser.flatMapLatest { currentUser ->
      if (currentUser == null) {
        database.customerFileDao().getAllActiveFilesFlow()
      } else if (currentUser.role == "RM") {
        database.customerFileDao().getFilesForRmFlow(currentUser.rmCode)
      } else {
        database.customerFileDao().getAllActiveFilesFlow()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getAllFilesIncludingDeletedFlow(): Flow<List<CustomerFileEntity>> {
    return authRepository.currentUser.flatMapLatest { currentUser ->
      if (currentUser == null) {
        database.customerFileDao().getAllFilesIncludingDeletedFlow()
      } else if (currentUser.role == "MENTOR") {
        database.customerFileDao().getAllFilesIncludingDeletedFlow()
      } else if (currentUser.role == "RM") {
        database.customerFileDao().getFilesForRmFlow(currentUser.rmCode)
      } else {
        database.customerFileDao().getAllActiveFilesFlow()
      }
    }
  }

  // RM Target vs Achievement
  fun getTargetForRmFlow(rmCode: String): Flow<RmTargetEntity?> {
    return database.rmTargetDao().getTargetForRmFlow(rmCode)
  }

  suspend fun getTargetForRm(rmCode: String): RmTargetEntity = withContext(Dispatchers.IO) {
    database.rmTargetDao().getTargetForRm(rmCode) ?: RmTargetEntity(rmCode = rmCode)
  }

  suspend fun setRmTargets(
    rmCode: String,
    creditCardTarget: Int,
    corporateCardTarget: Int,
    b2bTarget: Int
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val now = DateUtils.currentDhakaMillis()
    val target = RmTargetEntity(
      rmCode = rmCode,
      creditCardTarget = creditCardTarget,
      corporateCardTarget = corporateCardTarget,
      b2bTarget = b2bTarget,
      updatedAt = now
    )
    database.rmTargetDao().insertOrUpdateTarget(target)

    val currentUser = authRepository.currentUser.value
    if (currentUser != null && (currentUser.role == "ADMIN" || currentUser.role == "MENTOR")) {
      val targetRmUser = database.userDao().getUser(rmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: rmCode
      val formattedTime = DateUtils.formatDateTime(now)
      val smsMessage = "[EBL Alert] Dear $rmName ($rmCode), your sales target was updated by ${currentUser.role} (${currentUser.rmCode}). Targets -> CC: $creditCardTarget, Corp: $corporateCardTarget, B2B: $b2bTarget. Timestamp: $formattedTime. EBL Sales Suite."

      var smsStatus = "DELIVERED"
      if (context != null && rmMobile.isNotBlank()) {
        val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
        smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
      }

      database.smsNotificationDao().insertSms(
        SmsNotificationEntity(
          recipientRmCode = rmCode,
          recipientMobile = rmMobile,
          recipientName = rmName,
          triggeredByRole = currentUser.role,
          triggeredByCode = currentUser.rmCode,
          actionType = "TARGET",
          targetType = "RM_TARGET",
          fileId = null,
          customerName = null,
          messageText = smsMessage,
          sentTimestamp = now,
          status = smsStatus,
          isRead = false
        )
      )
    }

    Result.success(Unit)
  }

  // SMS Notifications
  fun getSmsForRmFlow(rmCode: String): Flow<List<SmsNotificationEntity>> {
    return database.smsNotificationDao().getSmsForRmFlow(rmCode)
  }

  fun getAllSmsFlow(): Flow<List<SmsNotificationEntity>> {
    return database.smsNotificationDao().getAllSmsFlow()
  }

  fun getUnreadSmsCountFlow(rmCode: String): Flow<Int> {
    return database.smsNotificationDao().getUnreadSmsCountFlow(rmCode)
  }

  suspend fun markSmsAsRead(id: Long) = withContext(Dispatchers.IO) {
    database.smsNotificationDao().markAsRead(id)
  }

  suspend fun markAllSmsAsReadForRm(rmCode: String) = withContext(Dispatchers.IO) {
    database.smsNotificationDao().markAllAsReadForRm(rmCode)
  }

  fun getFileByIdFlow(fileId: String): Flow<CustomerFileEntity?> {
    return database.customerFileDao().getFileByIdFlow(fileId)
  }

  suspend fun getFileById(fileId: String): CustomerFileEntity? = withContext(Dispatchers.IO) {
    database.customerFileDao().getFileById(fileId)
  }

  suspend fun saveCustomerFile(
    fileId: String?,
    customerName: String,
    companyName: String,
    officeAddress: String,
    mobile: String,
    altMobile: String,
    email: String,
    productType: String,
    applicationStatus: String,
    activeStatus: String,
    assignedRmCode: String,
    ccNumber: String = "",
    pendingDocuments: List<String>,
    remarks: String,
    cpvStatus: String,
    cpvDate: String,
    cpvAddress: String,
    cpvRemarks: String,
    cpvPhotoUri: String = "",
    cpvSupportingDocUri: String = "",
    submissionLatitude: Double? = null,
    submissionLongitude: Double? = null,
    submissionAddress: String? = null
  ): Result<CustomerFileEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized operation."))

    // RM can only save their own RM code
    val resolvedRmCode = (if (currentUser.role == "RM") {
      currentUser.rmCode
    } else {
      assignedRmCode.ifBlank { currentUser.rmCode }
    }).trim().uppercase()

    val now = DateUtils.currentDhakaMillis()
    val targetFileId = if (fileId.isNullOrBlank()) {
      SecurityUtils.generateFileId(resolvedRmCode)
    } else {
      fileId.trim()
    }

    val existing = database.customerFileDao().getFileById(targetFileId)
    val isNew = existing == null

    // Check authorization for edit
    if (existing != null && currentUser.role == "RM" && !existing.assignedRmCode.trim().equals(currentUser.rmCode.trim(), ignoreCase = true)) {
      return@withContext Result.failure(Exception("You do not have permission to modify this record."))
    }

    val createdTimestamp = existing?.createdAt ?: now
    val createdBy = existing?.createdBy ?: currentUser.rmCode
    val submittedAt = if (applicationStatus.equals("Submitted", ignoreCase = true)) {
      existing?.submittedAt ?: now
    } else existing?.submittedAt

    val approvedAt = if (applicationStatus.equals("Approved", ignoreCase = true)) {
      existing?.approvedAt ?: now
    } else existing?.approvedAt

    val pendingDocsJoined = pendingDocuments.joinToString(",")

    val resolvedSubmissionLat = submissionLatitude ?: existing?.submissionLatitude
    val resolvedSubmissionLng = submissionLongitude ?: existing?.submissionLongitude
    val resolvedSubmissionAddr = submissionAddress ?: existing?.submissionAddress

    val entity = CustomerFileEntity(
      fileId = targetFileId,
      customerName = customerName.trim(),
      companyName = companyName.trim(),
      officeAddress = officeAddress.trim(),
      mobile = mobile.trim(),
      altMobile = altMobile.trim(),
      email = email.trim(),
      productType = productType,
      applicationStatus = applicationStatus,
      activeStatus = activeStatus,
      assignedRmCode = resolvedRmCode,
      ccNumber = ccNumber.trim(),
      pendingDocuments = pendingDocsJoined,
      remarks = remarks.trim(),
      cpvStatus = cpvStatus,
      cpvDate = cpvDate,
      cpvAddress = cpvAddress.trim(),
      cpvRemarks = cpvRemarks.trim(),
      cpvPhotoUri = cpvPhotoUri.ifBlank { existing?.cpvPhotoUri ?: "" },
      cpvSupportingDocUri = cpvSupportingDocUri.ifBlank { existing?.cpvSupportingDocUri ?: "" },
      cpvLastUpdatedBy = currentUser.rmCode,
      submissionLatitude = resolvedSubmissionLat,
      submissionLongitude = resolvedSubmissionLng,
      submissionAddress = resolvedSubmissionAddr,
      createdAt = createdTimestamp,
      updatedAt = now,
      submittedAt = submittedAt,
      approvedAt = approvedAt,
      createdBy = createdBy,
      updatedBy = currentUser.rmCode,
      isDeleted = false,
      isSynced = false
    )

    if (isNew || existing == null) {
      database.customerFileDao().insertFile(entity)
      database.auditLogDao().insertLog(
        AuditLogEntity(
          logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
          userId = currentUser.rmCode,
          role = currentUser.role,
          action = "FILE_CREATE",
          fileId = targetFileId,
          rmCode = resolvedRmCode,
          timestamp = now,
          details = "Created customer file for '$customerName' ($productType, CC: ${ccNumber.ifBlank { "N/A" }})."
        )
      )
    } else {
      database.customerFileDao().updateFile(entity)
      database.auditLogDao().insertLog(
        AuditLogEntity(
          logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
          userId = currentUser.rmCode,
          role = currentUser.role,
          action = "FILE_UPDATE",
          fileId = targetFileId,
          rmCode = resolvedRmCode,
          timestamp = now,
          details = "Updated customer file ($applicationStatus, Active: $activeStatus, CC: ${ccNumber.ifBlank { "N/A" }})."
        )
      )

      // Notify RM via SMS & App Alert if updated by Admin or Mentor
      if ((currentUser.role == "MENTOR" || currentUser.role == "ADMIN") && existing.assignedRmCode != currentUser.rmCode) {
        val changedItems = mutableListOf<String>()
        if (existing.applicationStatus != applicationStatus) changedItems.add("Status -> $applicationStatus")
        if (existing.activeStatus != activeStatus) changedItems.add("Active -> $activeStatus")
        if (existing.remarks != remarks.trim()) changedItems.add("Remarks updated")
        if (existing.cpvStatus != cpvStatus) changedItems.add("CPV -> $cpvStatus")
        if (existing.ccNumber != ccNumber.trim()) changedItems.add("CC Number -> $ccNumber")
        if (changedItems.isEmpty()) changedItems.add("File details modified by ${currentUser.role}")

        val changeDetailsStr = changedItems.joinToString(", ")
        val targetRmUser = database.userDao().getUser(resolvedRmCode)
        val rmMobile = targetRmUser?.mobile ?: ""
        val rmName = targetRmUser?.name ?: resolvedRmCode
        val formattedTime = DateUtils.formatDateTime(now)

        val smsMessage = "[EBL Alert] Dear $rmName ($resolvedRmCode), customer file $targetFileId for '$customerName' was UPDATED by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Changes: $changeDetailsStr. Timestamp: $formattedTime. EBL Sales Suite."

        var smsStatus = "DELIVERED"
        if (context != null && rmMobile.isNotBlank()) {
          val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
          smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
        }

        database.smsNotificationDao().insertSms(
          SmsNotificationEntity(
            recipientRmCode = resolvedRmCode,
            recipientMobile = rmMobile,
            recipientName = rmName,
            triggeredByRole = currentUser.role,
            triggeredByCode = currentUser.rmCode,
            actionType = "UPDATE",
            targetType = "CUSTOMER_FILE",
            fileId = targetFileId,
            customerName = customerName,
            messageText = smsMessage,
            sentTimestamp = now,
            status = smsStatus,
            isRead = false
          )
        )

        context?.let { ctx ->
          NotificationHelper.sendRmFileUpdateNotification(
            context = ctx,
            targetRmCode = resolvedRmCode,
            ccNumber = ccNumber.ifBlank { existing.ccNumber.ifBlank { targetFileId } },
            customerName = customerName,
            changeDetails = changeDetailsStr,
            updatedByRole = currentUser.role
          )
        }
      }
    }

    // Auto-record location log if GPS coordinates are captured
    if (resolvedSubmissionLat != null && resolvedSubmissionLng != null) {
      database.userLocationLogDao().insertLocationLog(
        com.example.data.model.UserLocationLogEntity(
          rmCode = resolvedRmCode,
          userName = currentUser.name,
          latitude = resolvedSubmissionLat,
          longitude = resolvedSubmissionLng,
          address = resolvedSubmissionAddr ?: "Auto-Captured via File Entry",
          sourceAction = "CUSTOMER_FILE_ENTRY",
          timestamp = now
        )
      )
      database.userDao().updateLocation(
        rmCode = resolvedRmCode,
        lat = resolvedSubmissionLat,
        lng = resolvedSubmissionLng,
        address = resolvedSubmissionAddr ?: "Auto-Captured via File Entry",
        time = now
      )
    }

    updatePendingSyncCount()

    // Automatically trigger real-time Google Sheets sync in background
    applicationScope.launch {
      triggerGoogleSheetsSync()
    }

    Result.success(entity)
  }

  suspend fun softDeleteCustomerFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    val file = database.customerFileDao().getFileById(fileId)
      ?: return@withContext Result.failure(Exception("File not found."))

    if (currentUser.role == "RM" && file.assignedRmCode != currentUser.rmCode) {
      return@withContext Result.failure(Exception("Unauthorized to delete this record."))
    }

    val now = DateUtils.currentDhakaMillis()
    database.customerFileDao().softDeleteFile(fileId, currentUser.rmCode, now)

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "FILE_DELETE",
        fileId = fileId,
        rmCode = file.assignedRmCode,
        timestamp = now,
        details = "Soft-deleted customer file '${file.customerName}' ($fileId)."
      )
    )

    // Notify RM via SMS if deleted by Admin or Mentor!
    if ((currentUser.role == "MENTOR" || currentUser.role == "ADMIN") && file.assignedRmCode != currentUser.rmCode) {
      val targetRmUser = database.userDao().getUser(file.assignedRmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: file.assignedRmCode
      val formattedTime = DateUtils.formatDateTime(now)

      val smsMessage = "[EBL Alert] ATTENTION: Dear $rmName (${file.assignedRmCode}), your customer file $fileId ('${file.customerName}') was DELETED by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Timestamp: $formattedTime. EBL Sales Suite."

      var smsStatus = "DELIVERED"
      if (context != null && rmMobile.isNotBlank()) {
        val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
        smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
      }

      database.smsNotificationDao().insertSms(
        SmsNotificationEntity(
          recipientRmCode = file.assignedRmCode,
          recipientMobile = rmMobile,
          recipientName = rmName,
          triggeredByRole = currentUser.role,
          triggeredByCode = currentUser.rmCode,
          actionType = "DELETE",
          targetType = "CUSTOMER_FILE",
          fileId = fileId,
          customerName = file.customerName,
          messageText = smsMessage,
          sentTimestamp = now,
          status = smsStatus,
          isRead = false
        )
      )

      context?.let { ctx ->
        NotificationHelper.sendRmFileUpdateNotification(
          context = ctx,
          targetRmCode = file.assignedRmCode,
          ccNumber = file.ccNumber.ifBlank { fileId },
          customerName = file.customerName,
          changeDetails = "File DELETED by ${currentUser.role}",
          updatedByRole = currentUser.role
        )
      }
    }

    updatePendingSyncCount()
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun restoreCustomerFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Admin or Mentor can restore deleted files."))
    }

    val file = database.customerFileDao().getFileById(fileId)
    val now = DateUtils.currentDhakaMillis()
    database.customerFileDao().restoreFile(fileId, currentUser.rmCode, now)

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "FILE_RESTORE",
        fileId = fileId,
        rmCode = file?.assignedRmCode,
        timestamp = now,
        details = "Restored previously soft-deleted customer file $fileId."
      )
    )

    if (file != null && (currentUser.role == "MENTOR" || currentUser.role == "ADMIN") && file.assignedRmCode != currentUser.rmCode) {
      val targetRmUser = database.userDao().getUser(file.assignedRmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: file.assignedRmCode
      val formattedTime = DateUtils.formatDateTime(now)

      val smsMessage = "[EBL Alert] Dear $rmName (${file.assignedRmCode}), your customer file $fileId ('${file.customerName}') was RESTORED by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Timestamp: $formattedTime. EBL Sales Suite."

      var smsStatus = "DELIVERED"
      if (context != null && rmMobile.isNotBlank()) {
        val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
        smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
      }

      database.smsNotificationDao().insertSms(
        SmsNotificationEntity(
          recipientRmCode = file.assignedRmCode,
          recipientMobile = rmMobile,
          recipientName = rmName,
          triggeredByRole = currentUser.role,
          triggeredByCode = currentUser.rmCode,
          actionType = "RESTORE",
          targetType = "CUSTOMER_FILE",
          fileId = fileId,
          customerName = file.customerName,
          messageText = smsMessage,
          sentTimestamp = now,
          status = smsStatus,
          isRead = false
        )
      )
    }

    updatePendingSyncCount()
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun permanentDeleteCustomerFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Permanent deletion restricted strictly to Mentor role."))
    }

    val file = database.customerFileDao().getFileById(fileId)
    val now = DateUtils.currentDhakaMillis()
    database.customerFileDao().permanentDeleteFile(fileId)

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "FILE_PERMANENT_DELETE",
        fileId = fileId,
        rmCode = file?.assignedRmCode,
        timestamp = now,
        details = "Permanently expunged record $fileId by Mentor."
      )
    )

    if (file != null && file.assignedRmCode != currentUser.rmCode) {
      val targetRmUser = database.userDao().getUser(file.assignedRmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: file.assignedRmCode
      val formattedTime = DateUtils.formatDateTime(now)

      val smsMessage = "[EBL Alert] ATTENTION: Dear $rmName (${file.assignedRmCode}), customer file $fileId ('${file.customerName}') was PERMANENTLY REMOVED by Mentor (${currentUser.name.ifBlank { currentUser.rmCode }}). Timestamp: $formattedTime. EBL Sales Suite."

      var smsStatus = "DELIVERED"
      if (context != null && rmMobile.isNotBlank()) {
        val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
        smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
      }

      database.smsNotificationDao().insertSms(
        SmsNotificationEntity(
          recipientRmCode = file.assignedRmCode,
          recipientMobile = rmMobile,
          recipientName = rmName,
          triggeredByRole = currentUser.role,
          triggeredByCode = currentUser.rmCode,
          actionType = "PERMANENT_DELETE",
          targetType = "CUSTOMER_FILE",
          fileId = fileId,
          customerName = file.customerName,
          messageText = smsMessage,
          sentTimestamp = now,
          status = smsStatus,
          isRead = false
        )
      )
    }

    updatePendingSyncCount()
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  // 2. Attachments
  fun getAttachmentsForFileFlow(fileId: String): Flow<List<FileAttachmentEntity>> {
    return database.fileAttachmentDao().getAttachmentsForFileFlow(fileId)
  }

  suspend fun addAttachment(
    fileId: String,
    category: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String
  ): Result<FileAttachmentEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    val attachmentId = "ATT-${SecurityUtils.generateUniqueId().take(8)}"
    val now = DateUtils.currentDhakaMillis()

    val attachment = FileAttachmentEntity(
      attachmentId = attachmentId,
      fileId = fileId,
      category = category,
      fileName = fileName,
      fileType = fileType,
      storagePath = "files/${currentUser.rmCode}/$fileId/$fileName",
      fileUri = fileUri,
      fileSizeBytes = fileSizeBytes,
      uploadedBy = currentUser.rmCode,
      uploadedAt = now
    )

    database.fileAttachmentDao().insertAttachment(attachment)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "FILE_ATTACHMENT_UPLOAD",
        fileId = fileId,
        rmCode = if (currentUser.role == "RM") currentUser.rmCode else null,
        timestamp = now,
        details = "Uploaded attachment '$fileName' ($category) for $fileId."
      )
    )

    Result.success(attachment)
  }

  suspend fun deleteAttachment(attachmentId: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    database.fileAttachmentDao().deleteAttachment(attachmentId)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "FILE_ATTACHMENT_DELETE",
        fileId = fileId,
        rmCode = if (currentUser.role == "RM") currentUser.rmCode else null,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Removed attachment $attachmentId from $fileId."
      )
    )
    Result.success(Unit)
  }

  // 3. RM Management (Admin / Mentor only)
  fun getAllRmsFlow(): Flow<List<UserEntity>> {
    return database.userDao().getAllRmsFlow()
  }

  suspend fun createRm(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    initialPassword: String
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN") {
      return@withContext Result.failure(Exception("Only Admin can assign new RM accounts. Mentor approves or edits."))
    }

    val cleanRmCode = rmCode.trim()
    if (database.userDao().getUser(cleanRmCode) != null) {
      return@withContext Result.failure(Exception("RM Code '$cleanRmCode' is already assigned to an existing account."))
    }

    val now = DateUtils.currentDhakaMillis()
    val salt = SecurityUtils.generateSalt()
    val hash = SecurityUtils.hashPassword(initialPassword.trim(), salt)

    val newUser = UserEntity(
      rmCode = cleanRmCode,
      name = name.trim(),
      role = "RM",
      passwordHash = hash,
      salt = salt,
      mobile = mobile.trim(),
      email = email.trim(),
      officeAddress = officeAddress.trim(),
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = now,
      authUid = "AUTH_RM_$cleanRmCode"
    )

    database.userDao().insertUser(newUser)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_ASSIGN_ACTIVE",
        rmCode = cleanRmCode,
        timestamp = now,
        details = "Admin created active RM account for ${name.trim()} (Code: $cleanRmCode)."
      )
    )

    Result.success(newUser)
  }

  suspend fun updateRm(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    newPassword: String? = null
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }

    val existing = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("RM user not found."))

    val targetStatus = existing.accountStatus

    val updated = if (!newPassword.isNullOrBlank()) {
      val salt = SecurityUtils.generateSalt()
      val hash = SecurityUtils.hashPassword(newPassword.trim(), salt)
      existing.copy(
        name = name.trim(),
        mobile = mobile.trim(),
        email = email.trim(),
        officeAddress = officeAddress.trim(),
        passwordHash = hash,
        salt = salt,
        mustChangePassword = false
      )
    } else {
      existing.copy(
        name = name.trim(),
        mobile = mobile.trim(),
        email = email.trim(),
        officeAddress = officeAddress.trim(),
        accountStatus = targetStatus
      )
    }

    database.userDao().updateUser(updated)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_UPDATE",
        rmCode = rmCode,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Updated profile for RM $rmCode" + if (!newPassword.isNullOrBlank()) " (password changed)" else ""
      )
    )

    if (currentUser.role == "ADMIN") {
      context?.let { ctx ->
        NotificationHelper.sendNewRmApprovalNotification(ctx, name.trim(), rmCode)
      }
    }

    // Only the RM whose data was updated receives the notification with unique sound
    context?.let { ctx ->
      NotificationHelper.sendRmProfileUpdateNotification(
        context = ctx,
        targetRmCode = rmCode,
        rmName = name.trim(),
        updatedByRole = currentUser.role,
        details = "Profile details updated by ${currentUser.role}"
      )
    }

    // Send SMS to the RM whose data was updated
    val rmUpdateMobile = mobile.trim().ifBlank { existing.mobile }
    val now = DateUtils.currentDhakaMillis()
    val formattedTime = DateUtils.formatDateTime(now)
    val passNote = if (!newPassword.isNullOrBlank()) " Password was updated." else ""
    val smsMessage = "[EBL Alert] Dear ${name.trim()} ($rmCode), your RM profile details were updated by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}).$passNote Timestamp: $formattedTime. EBL Sales Suite."

    var smsStatus = "DELIVERED"
    if (context != null && rmUpdateMobile.isNotBlank()) {
      val sentHardware = SmsService.sendSms(context, rmUpdateMobile, smsMessage)
      smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
    }

    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = rmCode,
        recipientMobile = rmUpdateMobile,
        recipientName = name.trim(),
        triggeredByRole = currentUser.role,
        triggeredByCode = currentUser.rmCode,
        actionType = "PROFILE",
        targetType = "RM_PROFILE",
        fileId = null,
        customerName = null,
        messageText = smsMessage,
        sentTimestamp = now,
        status = smsStatus,
        isRead = false
      )
    )

    Result.success(Unit)
  }

  suspend fun approveRm(rmCode: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Mentor can approve RM accounts."))
    }
    val existing = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("RM not found."))
    database.userDao().updateStatus(rmCode, "ACTIVE")
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_APPROVE",
        rmCode = rmCode,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Mentor approved RM account $rmCode (${existing.name})."
      )
    )

    // Notify approved RM that they can now log in
    context?.let { ctx ->
      NotificationHelper.sendRmProfileUpdateNotification(
        context = ctx,
        targetRmCode = rmCode,
        rmName = existing.name,
        updatedByRole = "Mentor",
        details = "Your RM account has been approved! You can now log in."
      )
    }

    Result.success(Unit)
  }

  suspend fun rejectRm(rmCode: String, reason: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Mentor can reject RM accounts."))
    }
    database.userDao().updateStatus(rmCode, "INACTIVE")
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_REJECT",
        rmCode = rmCode,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Mentor rejected RM account $rmCode. Reason: ${reason.ifBlank { "Not specified" }}."
      )
    )
    Result.success(Unit)
  }

  suspend fun resetRmPassword(rmCode: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }
    val existing = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("RM not found."))
    val salt = SecurityUtils.generateSalt()
    val hash = SecurityUtils.hashPassword(newPassword, salt)
    database.userDao().updatePassword(rmCode, hash, salt, mustChange = false)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_PASSWORD_RESET",
        rmCode = rmCode,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "${currentUser.role} reset password for RM $rmCode (${existing.name})."
      )
    )
    Result.success(Unit)
  }

  fun getAppCustomNameFlow(): Flow<String> {
    return database.appSettingDao().getAllSettingsFlow().map { settings ->
      settings.find { it.settingKey == "app_custom_name" }?.settingValue ?: "RM File Management Suite"
    }
  }

  suspend fun setAppCustomName(name: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Mentor can configure the universal app name."))
    }
    val cleanName = name.trim().ifBlank { "RM File Management Suite" }
    val setting = AppSettingEntity(
      settingKey = "app_custom_name",
      settingValue = cleanName,
      updatedBy = currentUser.rmCode,
      updatedAt = DateUtils.currentDhakaMillis()
    )
    database.appSettingDao().insertOrUpdateSetting(setting)
    Result.success(Unit)
  }

  suspend fun setRmStatus(rmCode: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }

    database.userDao().updateStatus(rmCode, newStatus)
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "RM_STATUS_CHANGE",
        rmCode = rmCode,
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Changed RM $rmCode status to $newStatus."
      )
    )

    Result.success(Unit)
  }

  // 4. Audit Logs
  fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> {
    val currentUser = authRepository.currentUser.value ?: return emptyFlow()
    return if (currentUser.role == "RM") {
      database.auditLogDao().getLogsForRmFlow(currentUser.rmCode)
    } else {
      database.auditLogDao().getAllLogsFlow()
    }
  }

  // 5. Settings
  fun getAppSettingsFlow(): Flow<List<AppSettingEntity>> {
    return database.appSettingDao().getAllSettingsFlow()
  }

  suspend fun updateAppSetting(key: String, value: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied: settings can only be altered by Admin or Mentor."))
    }

    val setting = AppSettingEntity(
      settingKey = key,
      settingValue = value,
      updatedBy = currentUser.rmCode,
      updatedAt = DateUtils.currentDhakaMillis()
    )
    database.appSettingDao().insertOrUpdateSetting(setting)

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "APP_SETTING_CHANGE",
        timestamp = DateUtils.currentDhakaMillis(),
        details = "Updated setting '$key'."
      )
    )
    Result.success(Unit)
  }

  // 6. Google Sheets Synchronization
  fun getSyncStatusFlow(): Flow<SyncStatusEntity?> {
    return database.appSettingDao().getSyncStatusFlow()
  }

  private suspend fun updatePendingSyncCount() {
    val unsynced = database.customerFileDao().getUnsyncedFiles().size
    val current = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    database.appSettingDao().insertOrUpdateSyncStatus(current.copy(pendingRecordsCount = unsynced))
  }

  suspend fun setGoogleSheetUrl(urlOrId: String): Result<String> = withContext(Dispatchers.IO) {
    val clean = urlOrId.trim()
    val match = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)").find(clean)
    val extractedId = match?.groupValues?.get(1) ?: clean

    val current = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    val updated = current.copy(
      spreadsheetId = extractedId,
      lastSyncMessage = "Linked to Google Sheet ($extractedId). Real-time auto-sync active."
    )
    database.appSettingDao().insertOrUpdateSyncStatus(updated)
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(extractedId)
  }

  suspend fun updateAppsScriptConfig(url: String, secretKey: String): Result<Unit> = withContext(Dispatchers.IO) {
    val current = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    val updated = current.copy(
      appsScriptUrl = url.trim(),
      syncSecretKey = secretKey.trim()
    )
    database.appSettingDao().insertOrUpdateSyncStatus(updated)
    Result.success(Unit)
  }

  suspend fun triggerGoogleSheetsSync(): Result<String> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    val currentStatus = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    database.appSettingDao().insertOrUpdateSyncStatus(
      currentStatus.copy(
        lastSyncStatus = "IN_PROGRESS",
        lastSyncMessage = "Preparing payload and connecting to Google Sheets..."
      )
    )

    try {
      val allFiles = database.customerFileDao().getAllActiveFiles()
      val unsyncedFiles = database.customerFileDao().getUnsyncedFiles()
      val recentLogs = database.auditLogDao().getAllLogs()
      val recentLocations = database.userLocationLogDao().getRecentLocationLogs(50)

      // If an Apps Script Web App URL is provided, send real HTTP request
      if (currentStatus.appsScriptUrl.isNotBlank() && currentStatus.appsScriptUrl.startsWith("http")) {
        val filesArray = org.json.JSONArray()
        for (f in allFiles) {
          val fObj = JSONObject().apply {
            put("ccNumber", f.ccNumber.ifBlank { f.fileId })
            put("fileId", f.fileId)
            put("customerName", f.customerName)
            put("companyName", f.companyName)
            put("officeAddress", f.officeAddress)
            put("mobile", f.mobile)
            put("email", f.email)
            put("productType", f.productType)
            put("applicationStatus", f.applicationStatus)
            put("activeStatus", f.activeStatus)
            put("assignedRmCode", f.assignedRmCode)
            put("pendingDocuments", f.pendingDocuments)
            put("cpvRemarks", f.cpvRemarks)
            put("submissionAddress", f.submissionAddress ?: "")
            put("submissionLat", f.submissionLatitude ?: 0.0)
            put("submissionLng", f.submissionLongitude ?: 0.0)
            put("updatedAt", DateUtils.formatDateTime(f.updatedAt))
          }
          filesArray.put(fObj)
        }

        val locationsArray = org.json.JSONArray()
        for (loc in recentLocations) {
          locationsArray.put(JSONObject().apply {
            put("rmCode", loc.rmCode)
            put("userName", loc.userName)
            put("latitude", loc.latitude)
            put("longitude", loc.longitude)
            put("address", loc.address)
            put("timestamp", DateUtils.formatDateTime(loc.timestamp))
            put("sourceAction", loc.sourceAction)
          })
        }

        val payload = JSONObject().apply {
          put("action", "SYNC_ALL_DATA")
          put("spreadsheetId", currentStatus.spreadsheetId)
          put("secretKey", currentStatus.syncSecretKey)
          put("timestamp", DateUtils.currentDhakaMillis())
          put("syncedBy", currentUser.rmCode)
          put("filesCount", filesArray.length())
          put("files", filesArray)
          put("locations", locationsArray)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(currentStatus.appsScriptUrl)
          .post(requestBody)
          .build()

        try {
          httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val respBodyStr = response.body?.string() ?: ""
            if (respBodyStr.isNotBlank()) {
              try {
                val respJson = JSONObject(respBodyStr)
                val sheetFiles = respJson.optJSONArray("latestFiles") ?: respJson.optJSONArray("files")
                if (sheetFiles != null && sheetFiles.length() > 0) {
                  processSheetFiles(sheetFiles)
                }
              } catch (_: Exception) {}
            }
          }
        } catch (e: Exception) {
          // If remote webhook call fails, report the clear error to user and record it
          val errorMsg = "Remote sync error: ${e.message ?: "Failed to reach Apps Script endpoint"}"
          database.appSettingDao().insertOrUpdateSyncStatus(
            currentStatus.copy(
              lastSyncTimestamp = DateUtils.currentDhakaMillis(),
              lastSyncStatus = "FAILED",
              lastSyncMessage = errorMsg,
              pendingRecordsCount = unsyncedFiles.size
            )
          )
          return@withContext Result.failure(Exception(errorMsg))
        }
      }

      // Mark unsynced files as synced locally
      val unsyncedIds = unsyncedFiles.map { it.fileId }
      if (unsyncedIds.isNotEmpty()) {
        database.customerFileDao().markFilesSynced(unsyncedIds)
      }

      val now = DateUtils.currentDhakaMillis()
      val successMsg = "Successfully synchronized ${unsyncedIds.size} records to Spreadsheet (${currentStatus.spreadsheetId})."

      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          lastSyncTimestamp = now,
          lastSyncStatus = "SUCCESS",
          lastSyncMessage = successMsg,
          pendingRecordsCount = 0
        )
      )

      database.auditLogDao().insertLog(
        AuditLogEntity(
          logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
          userId = currentUser.rmCode,
          role = currentUser.role,
          action = "SYNC_SHEETS",
          timestamp = now,
          details = "Google Sheets sync completed for ${unsyncedIds.size} records."
        )
      )

      Result.success(successMsg)
    } catch (e: Exception) {
      val failureMsg = "Synchronization failed: ${e.message ?: "Unknown error"}"
      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          lastSyncTimestamp = DateUtils.currentDhakaMillis(),
          lastSyncStatus = "FAILED",
          lastSyncMessage = failureMsg
        )
      )
      Result.failure(e)
    }
  }

  suspend fun pullDataFromGoogleSheets(): Result<String> = withContext(Dispatchers.IO) {
    val currentStatus = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    if (currentStatus.appsScriptUrl.isBlank() || !currentStatus.appsScriptUrl.startsWith("http")) {
      return@withContext Result.failure(Exception("Apps Script Web App URL is not configured. Please paste URL and save connector."))
    }

    try {
      val payload = JSONObject().apply {
        put("action", "FETCH_SHEET_DATA")
        put("spreadsheetId", currentStatus.spreadsheetId)
        put("secretKey", currentStatus.syncSecretKey)
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(currentStatus.appsScriptUrl)
        .post(requestBody)
        .build()

      val respStr = httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw Exception("HTTP ${response.code}: ${response.message}")
        }
        response.body?.string() ?: ""
      }

      val json = JSONObject(respStr)
      val filesArray = json.optJSONArray("files") ?: json.optJSONArray("latestFiles") ?: JSONArray()
      val updatedCount = processSheetFiles(filesArray)

      val now = DateUtils.currentDhakaMillis()
      val msg = "Pulled ${filesArray.length()} rows from Google Sheets. $updatedCount record(s) updated locally."
      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          lastSyncTimestamp = now,
          lastSyncStatus = "SUCCESS",
          lastSyncMessage = msg
        )
      )

      database.auditLogDao().insertLog(
        AuditLogEntity(
          logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
          userId = authRepository.currentUser.value?.rmCode ?: "SYSTEM",
          role = authRepository.currentUser.value?.role ?: "MENTOR",
          action = "PULL_SHEETS",
          timestamp = now,
          details = msg
        )
      )

      Result.success(msg)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun processSheetFiles(sheetFilesJson: JSONArray): Int {
    var updatedCount = 0
    val now = DateUtils.currentDhakaMillis()
    for (i in 0 until sheetFilesJson.length()) {
      val obj = sheetFilesJson.optJSONObject(i) ?: continue
      val fileId = obj.optString("fileId").trim()
      val ccNumber = obj.optString("ccNumber").trim()
      val targetId = if (fileId.isNotBlank()) fileId else ccNumber
      if (targetId.isBlank()) continue

      val existing = database.customerFileDao().getFileById(targetId)
      val appStatus = obj.optString("applicationStatus", existing?.applicationStatus ?: "Submitted")
      val activeStatus = obj.optString("activeStatus", existing?.activeStatus ?: "Y")
      val remarks = obj.optString("remarks", existing?.remarks ?: "")
      val pendingDocs = obj.optString("pendingDocuments", existing?.pendingDocuments ?: "")
      val rmCode = obj.optString("assignedRmCode", existing?.assignedRmCode ?: "104393").trim().uppercase()
      val custName = obj.optString("customerName", existing?.customerName ?: "Customer")

      if (existing != null) {
        val hasStatusChanged = existing.applicationStatus != appStatus
        val hasActiveChanged = existing.activeStatus != activeStatus
        val hasRemarksChanged = existing.remarks != remarks
        val hasDocsChanged = existing.pendingDocuments != pendingDocs
        val hasCcChanged = ccNumber.isNotBlank() && existing.ccNumber != ccNumber

        if (hasStatusChanged || hasActiveChanged || hasRemarksChanged || hasDocsChanged || hasCcChanged) {
          val updated = existing.copy(
            applicationStatus = appStatus,
            activeStatus = activeStatus,
            remarks = remarks,
            pendingDocuments = pendingDocs,
            ccNumber = if (ccNumber.isNotBlank()) ccNumber else existing.ccNumber,
            updatedAt = now,
            updatedBy = "GoogleSheets_Sync"
          )
          database.customerFileDao().updateFile(updated)
          updatedCount++

          // Send SMS to RM if status or active was changed from Google Sheets!
          if (hasStatusChanged || hasActiveChanged) {
            val targetRmUser = database.userDao().getUser(existing.assignedRmCode)
            val rmMobile = targetRmUser?.mobile ?: ""
            val rmName = targetRmUser?.name ?: existing.assignedRmCode
            val formattedTime = DateUtils.formatDateTime(now)
            val changeNote = if (hasStatusChanged) "Status -> $appStatus" else "Active -> $activeStatus"
            val smsMessage = "[EBL Alert] Dear $rmName (${existing.assignedRmCode}), your customer file ${existing.fileId} ('${existing.customerName}') was updated in Google Sheets ($changeNote). Timestamp: $formattedTime. EBL Sales Suite."

            var smsStatus = "DELIVERED"
            if (context != null && rmMobile.isNotBlank()) {
              val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
              smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
            }

            database.smsNotificationDao().insertSms(
              SmsNotificationEntity(
                recipientRmCode = existing.assignedRmCode,
                recipientMobile = rmMobile,
                recipientName = rmName,
                triggeredByRole = "SHEETS_SYNC",
                triggeredByCode = "Admin_Sheets",
                actionType = "UPDATE",
                targetType = "CUSTOMER_FILE",
                fileId = existing.fileId,
                customerName = existing.customerName,
                messageText = smsMessage,
                sentTimestamp = now,
                status = smsStatus,
                isRead = false
              )
            )

            context?.let { ctx ->
              NotificationHelper.sendRmFileUpdateNotification(
                context = ctx,
                targetRmCode = existing.assignedRmCode,
                ccNumber = existing.ccNumber.ifBlank { existing.fileId },
                customerName = existing.customerName,
                changeDetails = "Google Sheet Update: $changeNote",
                updatedByRole = "ADMIN"
              )
            }
          }
        }
      } else {
        // New file row added in Google Sheets
        val newFile = CustomerFileEntity(
          fileId = targetId,
          customerName = custName,
          companyName = obj.optString("companyName", "N/A"),
          officeAddress = obj.optString("officeAddress", ""),
          mobile = obj.optString("mobile", ""),
          altMobile = "",
          email = obj.optString("email", ""),
          productType = obj.optString("productType", "Credit Card"),
          applicationStatus = appStatus,
          activeStatus = activeStatus,
          assignedRmCode = rmCode,
          ccNumber = ccNumber,
          pendingDocuments = pendingDocs,
          remarks = remarks,
          cpvStatus = obj.optString("cpvRemarks", "Pending"),
          createdAt = now,
          updatedAt = now,
          createdBy = "GoogleSheets",
          updatedBy = "GoogleSheets_Sync",
          isSynced = true
        )
        database.customerFileDao().insertFile(newFile)
        updatedCount++
      }
    }
    return updatedCount
  }

  // 7. CSV Export Utility
  fun generateCustomerFilesCsv(files: List<CustomerFileEntity>, isRmUser: Boolean): String {
    val sb = StringBuilder()
    val headers = if (isRmUser) {
      listOf(
        "CC-Number", "File ID", "Customer Name", "Company", "Mobile", "Email",
        "Product Type", "Status", "Active", "Pending Docs", "Remarks",
        "CPV Status", "Created Date", "Last Updated"
      )
    } else {
      listOf(
        "CC-Number", "File ID", "Customer Name", "Company", "Mobile", "Email",
        "Product Type", "Status", "Active", "RM Code", "Pending Docs", "Remarks",
        "CPV Status", "Created Date", "Last Updated"
      )
    }
    sb.append(headers.joinToString(",")).append("\n")

    for (f in files) {
      val ccDisplay = if (f.ccNumber.isNotBlank()) f.ccNumber else f.fileId
      val row = if (isRmUser) {
        listOf(
          escapeCsv(ccDisplay),
          escapeCsv(f.fileId),
          escapeCsv(f.customerName),
          escapeCsv(f.companyName),
          escapeCsv(f.mobile),
          escapeCsv(f.email),
          escapeCsv(f.productType),
          escapeCsv(f.applicationStatus),
          escapeCsv(f.activeStatus),
          escapeCsv(f.pendingDocuments),
          escapeCsv(f.remarks),
          escapeCsv(f.cpvStatus),
          escapeCsv(DateUtils.formatDateTime(f.createdAt)),
          escapeCsv(DateUtils.formatDateTime(f.updatedAt))
        )
      } else {
        listOf(
          escapeCsv(ccDisplay),
          escapeCsv(f.fileId),
          escapeCsv(f.customerName),
          escapeCsv(f.companyName),
          escapeCsv(f.mobile),
          escapeCsv(f.email),
          escapeCsv(f.productType),
          escapeCsv(f.applicationStatus),
          escapeCsv(f.activeStatus),
          escapeCsv(f.assignedRmCode),
          escapeCsv(f.pendingDocuments),
          escapeCsv(f.remarks),
          escapeCsv(f.cpvStatus),
          escapeCsv(DateUtils.formatDateTime(f.createdAt)),
          escapeCsv(DateUtils.formatDateTime(f.updatedAt))
        )
      }
      sb.append(row.joinToString(",")).append("\n")
    }
    return sb.toString()
  }

  fun generateRmMappingsCsv(rms: List<UserEntity>): String {
    val sb = StringBuilder()
    val headers = listOf("RM Code", "RM Name", "Mobile", "Email", "Office Branch", "Account Status", "Created Date", "Last Login")
    sb.append(headers.joinToString(",")).append("\n")
    for (r in rms) {
      val row = listOf(
        escapeCsv(r.rmCode),
        escapeCsv(r.name),
        escapeCsv(r.mobile),
        escapeCsv(r.email),
        escapeCsv(r.officeAddress),
        escapeCsv(r.accountStatus),
        escapeCsv(DateUtils.formatDateTime(r.createdAt)),
        escapeCsv(DateUtils.formatDateTime(r.lastLogin))
      )
      sb.append(row.joinToString(",")).append("\n")
    }
    return sb.toString()
  }

  private fun escapeCsv(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
      "\"$escaped\""
    } else {
      escaped
    }
  }

  // 7. Location Tracking & Monitoring (Mentor Access)
  suspend fun updateUserLocation(
    rmCode: String,
    latitude: Double,
    longitude: Double,
    address: String,
    sourceAction: String,
    fileId: String? = null
  ) = withContext(Dispatchers.IO) {
    val now = DateUtils.currentDhakaMillis()
    database.userDao().updateUserLocation(rmCode, latitude, longitude, address, now)

    val user = database.userDao().getUser(rmCode)
    val userName = user?.name ?: rmCode
    database.userLocationLogDao().insertLocationLog(
      com.example.data.model.UserLocationLogEntity(
        rmCode = rmCode,
        userName = userName,
        latitude = latitude,
        longitude = longitude,
        address = address,
        sourceAction = sourceAction,
        relatedFileId = fileId,
        timestamp = now
      )
    )
  }

  fun getAllUsersFlow(): Flow<List<UserEntity>> {
    return database.userDao().getAllUsersFlow()
  }

  fun getRecentLocationLogsFlow(limit: Int = 100): Flow<List<com.example.data.model.UserLocationLogEntity>> {
    return database.userLocationLogDao().getRecentLocationLogsFlow(limit)
  }

  fun getLocationLogsForRmFlow(rmCode: String, limit: Int = 50): Flow<List<com.example.data.model.UserLocationLogEntity>> {
    return database.userLocationLogDao().getLocationLogsForRmFlow(rmCode, limit)
  }
}
