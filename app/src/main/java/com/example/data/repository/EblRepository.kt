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

    applicationScope.launch { triggerGoogleSheetsSync() }
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

  suspend fun deleteSms(id: Long) = withContext(Dispatchers.IO) {
    database.smsNotificationDao().deleteSms(id)
  }

  suspend fun clearSmsForRm(rmCode: String) = withContext(Dispatchers.IO) {
    database.smsNotificationDao().clearSmsForRm(rmCode)
  }

  suspend fun clearAllSms() = withContext(Dispatchers.IO) {
    database.smsNotificationDao().clearAllSms()
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
    } else {
      database.customerFileDao().updateFile(entity)

      // Notify RM via SMS & App Alert if updated or reassigned by Admin or Mentor
      if ((currentUser.role == "MENTOR" || currentUser.role == "ADMIN") && (existing.assignedRmCode != currentUser.rmCode || existing.assignedRmCode != resolvedRmCode)) {
        val hasRmChanged = !existing.assignedRmCode.equals(resolvedRmCode, ignoreCase = true)
        val changedItems = mutableListOf<String>()
        if (hasRmChanged) changedItems.add("Assigned RM Reassigned -> $resolvedRmCode")
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

        val smsMessage = if (hasRmChanged) {
          "[EBL Alert] Dear $rmName ($resolvedRmCode), customer file $targetFileId for '$customerName' has been REASSIGNED to you by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Timestamp: $formattedTime. EBL Sales Suite."
        } else {
          "[EBL Alert] Dear $rmName ($resolvedRmCode), customer file $targetFileId for '$customerName' was UPDATED by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Changes: $changeDetailsStr. Timestamp: $formattedTime. EBL Sales Suite."
        }

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
            actionType = if (hasRmChanged) "TRANSFER" else "UPDATE",
            targetType = "CUSTOMER_FILE",
            fileId = targetFileId,
            customerName = customerName,
            messageText = smsMessage,
            sentTimestamp = now,
            status = smsStatus,
            isRead = false
          )
        )

        // If RM code changed, notify old RM that the file has moved to new RM
        if (hasRmChanged) {
          val oldRmUser = database.userDao().getUser(existing.assignedRmCode)
          val oldRmMobile = oldRmUser?.mobile ?: ""
          val oldRmName = oldRmUser?.name ?: existing.assignedRmCode
          val oldMsg = "[EBL Alert] Notice: Dear $oldRmName (${existing.assignedRmCode}), customer file $targetFileId ('$customerName') previously under your code has been transferred to RM $resolvedRmCode by ${currentUser.role}. Timestamp: $formattedTime."

          var oldSmsStatus = "DELIVERED"
          if (context != null && oldRmMobile.isNotBlank()) {
            val sent = SmsService.sendSms(context, oldRmMobile, oldMsg)
            oldSmsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
          }

          database.smsNotificationDao().insertSms(
            SmsNotificationEntity(
              recipientRmCode = existing.assignedRmCode,
              recipientMobile = oldRmMobile,
              recipientName = oldRmName,
              triggeredByRole = currentUser.role,
              triggeredByCode = currentUser.rmCode,
              actionType = "TRANSFER",
              targetType = "CUSTOMER_FILE",
              fileId = targetFileId,
              customerName = customerName,
              messageText = oldMsg,
              sentTimestamp = now,
              status = oldSmsStatus,
              isRead = false
            )
          )
        }

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

  suspend fun syncFileDeletionToGoogleSheets(fileId: String, ccNumber: String?): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val delArray = org.json.JSONArray().apply {
        put(fileId)
        if (!ccNumber.isNullOrBlank()) put(ccNumber)
      }

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_FILE")
        put("fileId", fileId)
        put("ccNumber", ccNumber ?: "")
        put("deletedFileIds", delArray)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp ->
        resp.body?.string()
      }
    } catch (_: Exception) {}
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

    // Store in persistent deleted_file_ids setting
    val currentDel = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
    val toAdd = listOfNotNull(file.fileId.ifBlank { null }, file.ccNumber.ifBlank { null }).joinToString(",")
    val updatedDel = if (currentDel.isBlank()) toAdd else "$currentDel,$toAdd"
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_file_ids", updatedDel, currentUser.rmCode, now)
    )

    // Immediately synchronize deletion with Google Sheets
    applicationScope.launch {
      syncFileDeletionToGoogleSheets(fileId, file.ccNumber)
      triggerGoogleSheetsSync()
    }

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

    // Immediately synchronize permanent deletion with Google Sheets
    applicationScope.launch {
      syncFileDeletionToGoogleSheets(fileId, file?.ccNumber)
    }

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
    Result.success(attachment)
  }

  suspend fun deleteAttachment(attachmentId: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    database.fileAttachmentDao().deleteAttachment(attachmentId)
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
    database.rmTargetDao().insertOrUpdateTarget(
      RmTargetEntity(
        rmCode = cleanRmCode,
        creditCardTarget = 15,
        corporateCardTarget = 5,
        b2bTarget = 2,
        updatedAt = now
      )
    )

    applicationScope.launch {
      syncRmPasswordToGoogleSheets(cleanRmCode, hash, salt)
      triggerGoogleSheetsSync()
    }

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

    if (!newPassword.isNullOrBlank()) {
      val uSalt = updated.salt
      val uHash = updated.passwordHash
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("RM_PASS_UPDATED_AT_${rmCode.trim().uppercase()}", now.toString(), currentUser.rmCode, now)
      )
      applicationScope.launch {
        syncRmPasswordToGoogleSheets(rmCode, uHash, uSalt)
      }
    }

    applicationScope.launch { triggerGoogleSheetsSync() }
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

    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun rejectRm(rmCode: String, reason: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Mentor can reject RM accounts."))
    }
    database.userDao().updateStatus(rmCode, "INACTIVE")
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun syncRmPasswordToGoogleSheets(rmCode: String, passwordHash: String, salt: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "UPDATE_RM_PASSWORD")
        put("rmCode", rmCode.trim().uppercase())
        put("passwordHash", passwordHash)
        put("salt", salt)
        put("updatedAt", DateUtils.formatDateTime(DateUtils.currentDhakaMillis()))
        put("spreadsheetId", currentStatus.spreadsheetId)
        put("secretKey", currentStatus.syncSecretKey)
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp ->
        resp.body?.string()
      }
    } catch (_: Exception) {}
  }

  suspend fun resetRmPassword(rmCode: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }
    val existing = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("RM not found."))
    val now = DateUtils.currentDhakaMillis()
    val salt = SecurityUtils.generateSalt()
    val hash = SecurityUtils.hashPassword(newPassword, salt)
    database.userDao().updatePassword(rmCode, hash, salt, mustChange = false)
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("RM_PASS_UPDATED_AT_${rmCode.trim().uppercase()}", now.toString(), currentUser.rmCode, now)
    )
    applicationScope.launch {
      syncRmPasswordToGoogleSheets(rmCode, hash, salt)
      triggerGoogleSheetsSync()
    }
    Result.success(Unit)
  }

  suspend fun saveUniversalChecklistSettings(
    headerTemplate: String,
    regardsTemplate: String,
    corporateDocsJson: String = "",
    enhancementDocsJson: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Admin / Mentor can configure universal checklist templates."))
    }
    val now = DateUtils.currentDhakaMillis()
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("CHECKLIST_HEADER_TEMPLATE", headerTemplate, currentUser.rmCode, now)
    )
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("CHECKLIST_REGARDS_TEMPLATE", regardsTemplate, currentUser.rmCode, now)
    )
    if (corporateDocsJson.isNotBlank()) {
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("CHECKLIST_CORP_DOCS", corporateDocsJson, currentUser.rmCode, now)
      )
    }
    if (enhancementDocsJson.isNotBlank()) {
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("CHECKLIST_ENHANCE_DOCS", enhancementDocsJson, currentUser.rmCode, now)
      )
    }
    applicationScope.launch { triggerGoogleSheetsSync() }
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
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun setRmStatus(rmCode: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }

    database.userDao().updateStatus(rmCode, newStatus)
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun deleteRmProfile(rmCode: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Admin or Mentor can delete RM profiles."))
    }

    val user = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("RM $rmCode not found."))

    val now = DateUtils.currentDhakaMillis()
    // Remove user and target records from local database
    database.userDao().deleteUser(rmCode)
    database.rmTargetDao().deleteTargetForRm(rmCode)

    // Store in persistent deleted_rm_codes setting
    val currentDel = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
    val updatedDel = if (currentDel.isBlank()) rmCode else "$currentDel,$rmCode"
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_rm_codes", updatedDel, currentUser.rmCode, now)
    )

    // Security Audit Log
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "DELETE_RM_PROFILE",
        details = "Deleted RM Profile ${user.name} ($rmCode) by ${currentUser.role} (${currentUser.rmCode})",
        timestamp = now,
        rmCode = rmCode
      )
    )

    // Synchronize RM deletion with Google Sheets
    applicationScope.launch {
      syncRmDeletionToGoogleSheets(rmCode)
    }

    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun syncRmDeletionToGoogleSheets(rmCode: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = JSONObject().apply {
        put("action", "DELETE_RM")
        put("rmCode", rmCode)
        put("deletedRmCodes", org.json.JSONArray().apply { put(rmCode) })
        put("spreadsheetId", currentStatus.spreadsheetId)
        put("secretKey", currentStatus.syncSecretKey)
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().close()
    } catch (_: Exception) {}
  }

  suspend fun reassignCustomerFileRm(fileId: String, newRmCode: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Admin or Mentor can change RM assignment."))
    }

    val file = database.customerFileDao().getFileByAnyId(fileId)
      ?: return@withContext Result.failure(Exception("Customer file not found."))

    val oldRmCode = file.assignedRmCode
    if (oldRmCode.equals(newRmCode, ignoreCase = true)) {
      return@withContext Result.success(Unit)
    }

    val targetRmUser = database.userDao().getUser(newRmCode)
    val oldRmUser = database.userDao().getUser(oldRmCode)

    val now = DateUtils.currentDhakaMillis()
    val updated = file.copy(
      assignedRmCode = newRmCode.trim().uppercase(),
      updatedAt = now,
      updatedBy = "${currentUser.role}_${currentUser.rmCode}",
      isSynced = false
    )
    database.customerFileDao().updateFile(updated)

    // Security Audit Log
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "REASSIGN_RM",
        fileId = file.fileId,
        details = "Reassigned file ${file.fileId} ('${file.customerName}') from RM $oldRmCode to RM $newRmCode by ${currentUser.role}",
        timestamp = now,
        rmCode = newRmCode
      )
    )

    // Send SMS alerts to BOTH new RM and old RM
    val formattedTime = DateUtils.formatDateTime(now)

    // 1. Alert New RM
    val newRmName = targetRmUser?.name ?: newRmCode
    val newRmMobile = targetRmUser?.mobile ?: ""
    val msgForNewRm = "[EBL Alert] Dear $newRmName ($newRmCode), customer file ${file.fileId} ('${file.customerName}') has been REASSIGNED to you by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }}). Please follow up. Timestamp: $formattedTime."

    var newSmsStatus = "DELIVERED"
    if (context != null && newRmMobile.isNotBlank()) {
      val sent = SmsService.sendSms(context, newRmMobile, msgForNewRm)
      newSmsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
    }

    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = newRmCode,
        recipientMobile = newRmMobile,
        recipientName = newRmName,
        triggeredByRole = currentUser.role,
        triggeredByCode = currentUser.rmCode,
        actionType = "TRANSFER",
        targetType = "CUSTOMER_FILE",
        fileId = file.fileId,
        customerName = file.customerName,
        messageText = msgForNewRm,
        sentTimestamp = now,
        status = newSmsStatus,
        isRead = false
      )
    )

    // 2. Alert Old RM
    val oldRmName = oldRmUser?.name ?: oldRmCode
    val oldRmMobile = oldRmUser?.mobile ?: ""
    val msgForOldRm = "[EBL Alert] Notice: Dear $oldRmName ($oldRmCode), customer file ${file.fileId} ('${file.customerName}') previously assigned to you was reassigned to RM $newRmCode by ${currentUser.role}. Timestamp: $formattedTime."

    var oldSmsStatus = "DELIVERED"
    if (context != null && oldRmMobile.isNotBlank()) {
      val sent = SmsService.sendSms(context, oldRmMobile, msgForOldRm)
      oldSmsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
    }

    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = oldRmCode,
        recipientMobile = oldRmMobile,
        recipientName = oldRmName,
        triggeredByRole = currentUser.role,
        triggeredByCode = currentUser.rmCode,
        actionType = "TRANSFER",
        targetType = "CUSTOMER_FILE",
        fileId = file.fileId,
        customerName = file.customerName,
        messageText = msgForOldRm,
        sentTimestamp = now,
        status = oldSmsStatus,
        isRead = false
      )
    )

    updatePendingSyncCount()
    applicationScope.launch { triggerGoogleSheetsSync() }

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
    applicationScope.launch { triggerGoogleSheetsSync() }
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
    val syncedBy = currentUser?.rmCode ?: "APP_BACKGROUND_SYNC"

    val currentStatus = database.appSettingDao().getSyncStatus() ?: SyncStatusEntity()
    val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
      "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
    }

    database.appSettingDao().insertOrUpdateSyncStatus(
      currentStatus.copy(
        appsScriptUrl = activeWebAppUrl,
        lastSyncStatus = "IN_PROGRESS",
        lastSyncMessage = "Connecting 7-tab payload with Google Sheets..."
      )
    )

    try {
      val allFiles = database.customerFileDao().getAllActiveFiles()
      val filesToPush = allFiles.filter { !it.isDeleted }

      val allUsers = database.userDao().getAllUsers()
      val allRms = allUsers.filter { it.role == "RM" }
      val allTargets = database.rmTargetDao().getAllTargets()
      val allSettings = database.appSettingDao().getAllSettings()
      val recentLogs = database.auditLogDao().getAllLogs().take(50)
      val allAttachments = database.fileAttachmentDao().getAllAttachments()
      val recentLocations = database.userLocationLogDao().getRecentLocationLogs(50)

      // If an Apps Script Web App URL is provided, send real HTTP request
      if (activeWebAppUrl.isNotBlank() && activeWebAppUrl.startsWith("http")) {
        val filesArray = org.json.JSONArray()
        for (f in filesToPush) {
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
            put("cpvStatus", f.cpvStatus)
            put("submissionAddress", f.submissionAddress ?: "")
            put("submissionLat", f.submissionLatitude ?: 0.0)
            put("submissionLng", f.submissionLongitude ?: 0.0)
            put("updatedAt", DateUtils.formatDateTime(f.updatedAt))
            put("updatedBy", f.updatedBy)
          }
          filesArray.put(fObj)
        }

        val rmsArray = org.json.JSONArray()
        for (rm in allRms) {
          val target = allTargets.find { it.rmCode == rm.rmCode }
          rmsArray.put(JSONObject().apply {
            put("rmCode", rm.rmCode)
            put("name", rm.name)
            put("mobile", rm.mobile)
            put("email", rm.email)
            put("officeAddress", rm.officeAddress)
            put("role", rm.role)
            put("accountStatus", rm.accountStatus)
            put("creditCardTarget", target?.creditCardTarget ?: 15)
            put("corporateCardTarget", target?.corporateCardTarget ?: 5)
            put("b2bTarget", target?.b2bTarget ?: 2)
            put("passwordHash", rm.passwordHash)
            put("salt", rm.salt)
            put("createdAt", DateUtils.formatDateTime(rm.createdAt))
            put("updatedAt", DateUtils.formatDateTime(target?.updatedAt ?: rm.createdAt))
          })
        }

        val settingsArray = org.json.JSONArray()
        for (s in allSettings) {
          settingsArray.put(JSONObject().apply {
            put("settingKey", s.settingKey)
            put("settingValue", s.settingValue)
            put("description", "Universal Setting - Auto-synced across all mobile apps")
            put("updatedBy", s.updatedBy)
            put("updatedAt", DateUtils.formatDateTime(s.updatedAt))
          })
        }

        val auditLogsArray = org.json.JSONArray()
        for (l in recentLogs) {
          auditLogsArray.put(JSONObject().apply {
            put("logId", l.logId)
            put("userId", l.userId)
            put("role", l.role)
            put("action", l.action)
            put("targetId", l.fileId ?: l.rmCode ?: "")
            put("details", l.details)
            put("timestamp", DateUtils.formatDateTime(l.timestamp))
          })
        }

        val attachmentsArray = org.json.JSONArray()
        for (att in allAttachments) {
          attachmentsArray.put(JSONObject().apply {
            put("attachmentId", att.attachmentId)
            put("fileId", att.fileId)
            put("fileName", att.fileName)
            put("category", att.category)
            put("fileSize", att.fileSizeBytes)
            put("uploadedBy", att.uploadedBy)
            put("uploadedAt", DateUtils.formatDateTime(att.uploadedAt))
          })
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

        val recentSms = database.smsNotificationDao().getAllSms().take(50)
        val smsArray = org.json.JSONArray()
        for (s in recentSms) {
          smsArray.put(JSONObject().apply {
            put("id", s.id)
            put("recipientRmCode", s.recipientRmCode)
            put("recipientName", s.recipientName)
            put("recipientMobile", s.recipientMobile)
            put("triggeredByRole", s.triggeredByRole)
            put("triggeredByCode", s.triggeredByCode)
            put("actionType", s.actionType)
            put("targetType", s.targetType)
            put("fileId", s.fileId ?: "")
            put("customerName", s.customerName ?: "")
            put("messageText", s.messageText)
            put("sentTimestamp", DateUtils.formatDateTime(s.sentTimestamp))
            put("status", s.status)
          })
        }

        // Gather all deleted file IDs so Google Sheets deletes them from spreadsheet
        val allDeletedFiles = database.customerFileDao().getAllDeletedFiles()
        val deletedFileIds = org.json.JSONArray()
        for (df in allDeletedFiles) {
          deletedFileIds.put(df.fileId)
          if (df.ccNumber.isNotBlank()) deletedFileIds.put(df.ccNumber)
        }
        val delFilesSetting = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
        delFilesSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedFileIds.put(it)
        }

        // Gather all deleted RM codes so Google Sheets deletes them from RM sheet
        val delRmsSetting = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
        val deletedRmCodes = org.json.JSONArray()
        delRmsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.forEach {
          deletedRmCodes.put(it)
        }

        val payload = JSONObject().apply {
          put("action", "SYNC_ALL_DATA")
          put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
          put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
          put("timestamp", DateUtils.currentDhakaMillis())
          put("syncedBy", syncedBy)
          put("filesCount", filesArray.length())
          put("files", filesArray)
          put("deletedFileIds", deletedFileIds)
          put("deletedRmCodes", deletedRmCodes)
          put("rms", rmsArray)
          put("settings", settingsArray)
          put("auditLogs", auditLogsArray)
          put("attachments", attachmentsArray)
          put("sms", smsArray)
          put("locations", locationsArray)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(activeWebAppUrl)
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
                val sheetRms = respJson.optJSONArray("rms")
                if (sheetRms != null && sheetRms.length() > 0) {
                  processSheetRms(sheetRms)
                }
                val sheetSettings = respJson.optJSONArray("settings")
                if (sheetSettings != null && sheetSettings.length() > 0) {
                  processSheetSettings(sheetSettings)
                }
              } catch (_: Exception) {}
            }
          }
        } catch (e: Exception) {
          val errorMsg = "Remote sync error: ${e.message ?: "Failed to reach Apps Script endpoint"}"
          database.appSettingDao().insertOrUpdateSyncStatus(
            currentStatus.copy(
              lastSyncTimestamp = DateUtils.currentDhakaMillis(),
              lastSyncStatus = "FAILED",
              lastSyncMessage = errorMsg,
              pendingRecordsCount = filesToPush.size
            )
          )
          return@withContext Result.failure(Exception(errorMsg))
        }
      }

      // Mark pushed files as synced locally
      val pushedIds = filesToPush.map { it.fileId }
      if (pushedIds.isNotEmpty()) {
        database.customerFileDao().markFilesSynced(pushedIds)
      }

      val now = DateUtils.currentDhakaMillis()
      val successMsg = "Synchronized ${pushedIds.size} file records, ${allRms.size} RMs & Universal Settings to Spreadsheet."

      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          appsScriptUrl = activeWebAppUrl,
          lastSyncTimestamp = now,
          lastSyncStatus = "SUCCESS",
          lastSyncMessage = successMsg,
          pendingRecordsCount = 0
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
    val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
      "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
    }

    try {
      val payload = JSONObject().apply {
        put("action", "FETCH_SHEET_DATA")
        put("spreadsheetId", currentStatus.spreadsheetId)
        put("secretKey", currentStatus.syncSecretKey)
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(activeWebAppUrl)
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
      val updatedFilesCount = processSheetFiles(filesArray)

      val rmsArray = json.optJSONArray("rms") ?: JSONArray()
      val updatedRmsCount = processSheetRms(rmsArray)

      val settingsArray = json.optJSONArray("settings") ?: JSONArray()
      val updatedSettingsCount = processSheetSettings(settingsArray)

      val now = DateUtils.currentDhakaMillis()
      val msg = "Pulled from Google Sheets: $updatedFilesCount file(s), $updatedRmsCount RM(s), $updatedSettingsCount setting(s) updated."
      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          appsScriptUrl = activeWebAppUrl,
          lastSyncTimestamp = now,
          lastSyncStatus = "SUCCESS",
          lastSyncMessage = msg
        )
      )

      Result.success(msg)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun processSheetRms(sheetRmsJson: JSONArray): Int {
    var updatedCount = 0
    val now = DateUtils.currentDhakaMillis()

    // 1. Gather all RM codes from sheet
    val sheetRmCodes = HashSet<String>()
    for (idx in 0 until sheetRmsJson.length()) {
      val o = sheetRmsJson.optJSONObject(idx) ?: continue
      val c = o.optString("rmCode").trim().uppercase()
      if (c.isNotBlank()) sheetRmCodes.add(c)
    }

    // 2. If sheet returned RM rows, remove any local custom RM that was deleted in Google Sheet
    if (sheetRmsJson.length() > 0) {
      val allLocalUsers = database.userDao().getAllUsers()
      val protectedCodes = setOf("104393", "ADMIN", "MENTOR")
      for (u in allLocalUsers) {
        val uCode = u.rmCode.trim().uppercase()
        if (u.role == "RM" && uCode !in protectedCodes && uCode !in sheetRmCodes) {
          database.userDao().deleteUser(u.rmCode)
          database.rmTargetDao().deleteTargetForRm(u.rmCode)
          updatedCount++
        }
      }
    }

    // 3. Check persistent deleted RM codes so app never resurrects deleted RMs
    val delRmsSetting = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
    val deletedCodes = delRmsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()

    for (i in 0 until sheetRmsJson.length()) {
      val obj = sheetRmsJson.optJSONObject(i) ?: continue
      val code = obj.optString("rmCode").trim().uppercase()
      if (code.isBlank() || code == "ADMIN0" || code == "MENTOR0" || code in deletedCodes) continue

      val name = obj.optString("name", "Relationship Manager").trim()
      val mobile = obj.optString("mobile", "").trim()
      val email = obj.optString("email", "").trim()
      val office = obj.optString("officeAddress", "Dhaka Branch").trim()
      val role = obj.optString("role", "RM").trim()
      val status = obj.optString("accountStatus", "ACTIVE").trim()
      val ccTarget = obj.optInt("creditCardTarget", 15)
      val corpTarget = obj.optInt("corporateCardTarget", 5)
      val b2bTarget = obj.optInt("b2bTarget", 2)

      val sheetHash = obj.optString("passwordHash", "").trim()
      val sheetSalt = obj.optString("salt", "").trim()

      val existing = database.userDao().getUser(code)
      if (existing != null) {
        val lastLocalPassChangeTime = database.appSettingDao().getSetting("RM_PASS_UPDATED_AT_$code")?.updatedAt ?: 0L
        val isRecentLocalPassChange = (now - lastLocalPassChangeTime) < 180_000L // 3 minutes immunity window

        val hashToUse = if (isRecentLocalPassChange) existing.passwordHash else if (sheetHash.isNotBlank()) sheetHash else existing.passwordHash
        val saltToUse = if (isRecentLocalPassChange) existing.salt else if (sheetSalt.isNotBlank()) sheetSalt else existing.salt
        val hasChanges = existing.name != name || existing.mobile != mobile ||
                         existing.email != email || existing.officeAddress != office ||
                         existing.accountStatus != status ||
                         (!isRecentLocalPassChange && sheetHash.isNotBlank() && existing.passwordHash != sheetHash)
        if (hasChanges) {
          database.userDao().updateUser(
            existing.copy(
              name = name,
              mobile = mobile,
              email = email,
              officeAddress = office,
              accountStatus = status,
              passwordHash = hashToUse,
              salt = saltToUse
            )
          )
          updatedCount++
        }
      } else {
        val (finalHash, finalSalt) = if (sheetHash.isNotBlank() && sheetSalt.isNotBlank()) {
          Pair(sheetHash, sheetSalt)
        } else {
          val salt = SecurityUtils.generateSalt()
          Pair(SecurityUtils.hashPassword("#123456A", salt), salt)
        }
        val newUser = UserEntity(
          rmCode = code,
          name = name,
          role = role,
          passwordHash = finalHash,
          salt = finalSalt,
          mobile = mobile,
          email = email,
          officeAddress = office,
          accountStatus = status,
          mustChangePassword = false,
          createdAt = now,
          authUid = "AUTH_RM_$code"
        )
        database.userDao().insertUser(newUser)
        updatedCount++
      }

      val existingTarget = database.rmTargetDao().getTargetForRm(code)
      if (existingTarget == null) {
        database.rmTargetDao().insertOrUpdateTarget(
          RmTargetEntity(code, ccTarget, corpTarget, b2bTarget, now)
        )
      } else if (existingTarget.creditCardTarget != ccTarget ||
                 existingTarget.corporateCardTarget != corpTarget ||
                 existingTarget.b2bTarget != b2bTarget) {
        database.rmTargetDao().insertOrUpdateTarget(
          existingTarget.copy(
            creditCardTarget = ccTarget,
            corporateCardTarget = corpTarget,
            b2bTarget = b2bTarget,
            updatedAt = now
          )
        )
        updatedCount++
      }
    }
    return updatedCount
  }

  private suspend fun processSheetSettings(sheetSettingsJson: JSONArray): Int {
    var updatedCount = 0
    val now = DateUtils.currentDhakaMillis()
    for (i in 0 until sheetSettingsJson.length()) {
      val obj = sheetSettingsJson.optJSONObject(i) ?: continue
      val key = obj.optString("settingKey").trim()
      val value = obj.optString("settingValue").trim()
      if (key.isBlank()) continue

      val existing = database.appSettingDao().getSetting(key)
      if (existing == null || existing.settingValue != value) {
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity(
            settingKey = key,
            settingValue = value,
            updatedBy = "GoogleSheets_Universal",
            updatedAt = now
          )
        )
        updatedCount++
      }
    }
    return updatedCount
  }

  private suspend fun processSheetFiles(sheetFilesJson: JSONArray): Int {
    var updatedCount = 0
    val now = DateUtils.currentDhakaMillis()

    // 1. Gather all file identifiers present in the Google Sheet
    val sheetIds = HashSet<String>()
    for (idx in 0 until sheetFilesJson.length()) {
      val o = sheetFilesJson.optJSONObject(idx) ?: continue
      val fId = o.optString("fileId").trim().lowercase()
      val cc = o.optString("ccNumber").trim().lowercase()
      if (fId.isNotBlank()) sheetIds.add(fId)
      if (cc.isNotBlank()) sheetIds.add(cc)
    }

    // 2. Detect records DELETED in Google Sheets:
    // If the sheet returned data, any local active file that was previously synced (or created > 20s ago)
    // and is completely missing from the sheet has been deleted by an administrator or mentor in Google Sheets!
    if (sheetFilesJson.length() > 0) {
      val allLocalActive = database.customerFileDao().getAllActiveFiles()
      for (localFile in allLocalActive) {
        val fMatch = localFile.fileId.trim().lowercase() in sheetIds
        val ccMatch = localFile.ccNumber.isNotBlank() && localFile.ccNumber.trim().lowercase() in sheetIds
        if (!fMatch && !ccMatch && (localFile.isSynced || now - localFile.createdAt > 10000)) {
          database.customerFileDao().softDeleteFile(localFile.fileId, "GoogleSheets_Delete", now)
          database.customerFileDao().markFileSynced(localFile.fileId)
          updatedCount++
        }
      }
    }

    // 3. Upsert / update files present in Google Sheets
    for (i in 0 until sheetFilesJson.length()) {
      val obj = sheetFilesJson.optJSONObject(i) ?: continue
      val fileId = obj.optString("fileId").trim()
      val ccNumber = obj.optString("ccNumber").trim()
      val targetId = if (fileId.isNotBlank()) fileId else ccNumber
      if (targetId.isBlank()) continue

      val existing = database.customerFileDao().getFileByAnyId(targetId)
        ?: (if (ccNumber.isNotBlank()) database.customerFileDao().getFileByAnyId(ccNumber) else null)

      if (existing != null && existing.isDeleted) {
        // Record was deleted in app or sheet; do NOT resurrect or re-enable it!
        continue
      }
      val appStatus = obj.optString("applicationStatus", existing?.applicationStatus ?: "Submitted")
      val activeStatus = obj.optString("activeStatus", existing?.activeStatus ?: "Y")
      val remarks = obj.optString("remarks", existing?.remarks ?: "")
      val pendingDocs = obj.optString("pendingDocuments", existing?.pendingDocuments ?: "")
      val rmCode = obj.optString("assignedRmCode", existing?.assignedRmCode ?: "104393").trim().uppercase()
      val custName = obj.optString("customerName", existing?.customerName ?: "Customer")

      if (existing != null) {
        // CRITICAL FIX: If local file has pending un-synced edits, NEVER overwrite with sheet data!
        // This ensures editing in app never gets reverted back to old values!
        if (!existing.isSynced) {
          continue
        }

        val custName = obj.optString("customerName", existing.customerName)
        val compName = obj.optString("companyName", existing.companyName)
        val offAddr = obj.optString("officeAddress", existing.officeAddress)
        val mob = obj.optString("mobile", existing.mobile)
        val em = obj.optString("email", existing.email)
        val prodType = obj.optString("productType", existing.productType)
        val cpvStatus = obj.optString("cpvStatus", existing.cpvStatus)
        val cpvRemarks = obj.optString("cpvRemarks", obj.optString("remarks", existing.cpvRemarks))
        val remarksVal = obj.optString("remarks", obj.optString("cpvRemarks", existing.remarks))

        val hasStatusChanged = existing.applicationStatus != appStatus
        val hasActiveChanged = existing.activeStatus != activeStatus
        val hasRemarksChanged = existing.remarks != remarksVal || existing.cpvRemarks != cpvRemarks
        val hasDocsChanged = existing.pendingDocuments != pendingDocs
        val hasCcChanged = ccNumber.isNotBlank() && existing.ccNumber != ccNumber
        val hasRmChanged = rmCode.isNotBlank() && !existing.assignedRmCode.equals(rmCode, ignoreCase = true)
        val hasDetailsChanged = existing.customerName != custName || existing.companyName != compName ||
                                existing.mobile != mob || existing.email != em || existing.officeAddress != offAddr ||
                                existing.productType != prodType || existing.cpvStatus != cpvStatus

        if (hasStatusChanged || hasActiveChanged || hasRemarksChanged || hasDocsChanged || hasCcChanged || hasRmChanged || hasDetailsChanged) {
          val updated = existing.copy(
            customerName = custName,
            companyName = compName,
            officeAddress = offAddr,
            mobile = mob,
            email = em,
            productType = prodType,
            applicationStatus = appStatus,
            activeStatus = activeStatus,
            remarks = remarksVal,
            cpvRemarks = cpvRemarks,
            cpvStatus = cpvStatus,
            pendingDocuments = pendingDocs,
            ccNumber = if (ccNumber.isNotBlank()) ccNumber else existing.ccNumber,
            assignedRmCode = if (rmCode.isNotBlank()) rmCode else existing.assignedRmCode,
            updatedAt = now,
            updatedBy = "GoogleSheets_Sync",
            isSynced = true
          )
          database.customerFileDao().updateFile(updated)
          updatedCount++

          // Send SMS to RM if status, active, remarks, documents, or RM assignment were changed from Google Sheets!
          if (hasStatusChanged || hasActiveChanged || hasRemarksChanged || hasDocsChanged || hasRmChanged) {
            val targetRmUser = database.userDao().getUser(updated.assignedRmCode)
            val rmMobile = targetRmUser?.mobile ?: ""
            val rmName = targetRmUser?.name ?: updated.assignedRmCode
            val formattedTime = DateUtils.formatDateTime(now)
            val changeNote = when {
              hasRmChanged -> "Assigned RM: $rmCode"
              hasStatusChanged -> "Status: $appStatus"
              hasActiveChanged -> "Active Status: $activeStatus"
              hasRemarksChanged -> "CPV Remarks Updated"
              else -> "Pending Docs Updated"
            }
            val smsMessage = "[EBL Alert] Dear $rmName (${updated.assignedRmCode}), customer file ${existing.fileId} ('${existing.customerName}') was updated in Google Sheets ($changeNote). Timestamp: $formattedTime. EBL Sales Suite."

            var smsStatus = "DELIVERED"
            if (context != null && rmMobile.isNotBlank()) {
              val sentHardware = SmsService.sendSms(context, rmMobile, smsMessage)
              smsStatus = if (sentHardware) "DELIVERED" else "SENT_IN_APP"
            }

            database.smsNotificationDao().insertSms(
              SmsNotificationEntity(
                recipientRmCode = updated.assignedRmCode,
                recipientMobile = rmMobile,
                recipientName = rmName,
                triggeredByRole = "SHEETS_SYNC",
                triggeredByCode = "Admin_Sheets",
                actionType = if (hasRmChanged) "TRANSFER" else "UPDATE",
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
                targetRmCode = updated.assignedRmCode,
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
