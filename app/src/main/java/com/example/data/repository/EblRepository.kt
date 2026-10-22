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
    .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
    .retryOnConnectionFailure(true)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  // In-memory persistent tombstone caches to guarantee instant deletion without resurrection
  val deletedDocIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedEventIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedFileIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedSmsIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedChatIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedRmCodesCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedPresetKeysCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  val deletedAttachmentIdsCache: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

  init {
    applicationScope.launch {
      try {
        val delDocs = database.appSettingDao().getSetting("deleted_doc_ids")?.settingValue ?: ""
        delDocs.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedDocIdsCache.add(it)
          deletedDocIdsCache.add(it.uppercase())
          deletedDocIdsCache.add(it.lowercase())
        }
        val delEvents = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
        delEvents.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedEventIdsCache.add(it)
          deletedEventIdsCache.add(it.uppercase())
          deletedEventIdsCache.add(it.lowercase())
        }
        val delFiles = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
        delFiles.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.forEach {
          deletedFileIdsCache.add(it)
        }
        val delSms = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
        delSms.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedSmsIdsCache.add(it)
        }
        val delChats = database.appSettingDao().getSetting("deleted_chat_ids")?.settingValue ?: ""
        delChats.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedChatIdsCache.add(it)
          deletedChatIdsCache.add(it.uppercase())
        }
        val delRms = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
        delRms.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.forEach {
          deletedRmCodesCache.add(it)
        }
        val delPresets = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
        delPresets.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedPresetKeysCache.add(it)
          deletedPresetKeysCache.add(it.uppercase())
        }
        val delAtts = database.appSettingDao().getSetting("deleted_attachment_ids")?.settingValue ?: ""
        delAtts.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedAttachmentIdsCache.add(it)
          deletedAttachmentIdsCache.add(it.uppercase())
        }

        // Auto-restore customer files that were mistakenly soft-deleted by previous GoogleSheets_Delete bug
        val allDeleted = database.customerFileDao().getAllDeletedFiles()
        val explicitDelFiles = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue.orEmpty()
        val explicitDelSet = (explicitDelFiles.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() } + deletedFileIdsCache.map { it.lowercase() }).toSet()
        for (delFile in allDeleted) {
          if (delFile.updatedBy == "GoogleSheets_Delete") {
            val fIdLower = delFile.fileId.trim().lowercase()
            val ccLower = delFile.ccNumber.trim().lowercase()
            if (fIdLower !in explicitDelSet && (ccLower.isBlank() || ccLower !in explicitDelSet)) {
              database.customerFileDao().restoreFile(delFile.fileId, "Recovery_AutoRestore", DateUtils.currentDhakaMillis())
            }
          }
        }

        // Clean up any bogus CHECKLIST_PRESET_ entities created from general universal settings
        val knownNonPresetKeywords = listOf(
          "OFFICE", "LAT", "LNG", "RADIUS", "ADDRESS", "BANK", "APP_NAME", "APPLICATION_NAME",
          "COMPANY", "SUPPORT", "PHONE", "EMAIL", "URL", "TERMS", "POLICY", "PASSWORD", "SECRET",
          "TOMBSTONE", "DELETED", "SYNC", "MAINTENANCE", "VERSION", "PORT", "IP", "HOURS", "STATUS"
        )
        val customPresetSetting = database.appSettingDao().getSetting("CHECKLIST_CUSTOM_PRESETS_LIST")
        if (customPresetSetting != null && customPresetSetting.settingValue.isNotBlank()) {
          try {
            val arr = org.json.JSONArray(customPresetSetting.settingValue)
            val cleanedList = mutableListOf<String>()
            for (i in 0 until arr.length()) {
              val item = arr.getString(i).trim()
              val itemUpper = item.uppercase()
              val isBogus = knownNonPresetKeywords.any { kw -> itemUpper.contains(kw) }
              if (!isBogus && item.isNotBlank()) {
                cleanedList.add(item)
              } else if (isBogus) {
                val cleanKey = "CHECKLIST_CUSTOM_" + item.replace("\\s+".toRegex(), "_").uppercase()
                val legacyKey = "CHECKLIST_PRESET_" + item.replace("\\s+".toRegex(), "_").uppercase()
                database.appSettingDao().deleteSetting(cleanKey)
                database.appSettingDao().deleteSetting(legacyKey)
                database.appSettingDao().deleteSetting(item)
              }
            }
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(cleanedList.distinct()).toString(), "Sanitization", DateUtils.currentDhakaMillis())
            )
          } catch (_: Exception) {}
        }
      } catch (_: Exception) {}
    }
  }

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
  ): Result<Unit> {
    val q = DateUtils.getCurrentQuarter()
    return setRmQuarterTargets(rmCode, q, 2026, creditCardTarget, corporateCardTarget, b2bTarget)
  }

  suspend fun setRmQuarterTargets(
    rmCode: String,
    quarter: String,
    year: Int = 2026,
    creditCardTarget: Int,
    corporateCardTarget: Int,
    b2bTarget: Int
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val now = DateUtils.currentDhakaMillis()
    val existing = database.rmTargetDao().getTargetForRm(rmCode) ?: RmTargetEntity(rmCode = rmCode)
    val cleanQ = quarter.trim().uppercase()
    val updated = when (cleanQ) {
      "Q1" -> existing.copy(
        targetYear = year,
        activeQuarter = "Q1",
        creditCardTarget = creditCardTarget,
        corporateCardTarget = corporateCardTarget,
        b2bTarget = b2bTarget,
        q1CreditCardTarget = creditCardTarget,
        q1CorporateCardTarget = corporateCardTarget,
        q1B2bTarget = b2bTarget,
        updatedAt = now
      )
      "Q2" -> existing.copy(
        targetYear = year,
        activeQuarter = "Q2",
        creditCardTarget = creditCardTarget,
        corporateCardTarget = corporateCardTarget,
        b2bTarget = b2bTarget,
        q2CreditCardTarget = creditCardTarget,
        q2CorporateCardTarget = corporateCardTarget,
        q2B2bTarget = b2bTarget,
        updatedAt = now
      )
      "Q3" -> existing.copy(
        targetYear = year,
        activeQuarter = "Q3",
        creditCardTarget = creditCardTarget,
        corporateCardTarget = corporateCardTarget,
        b2bTarget = b2bTarget,
        q3CreditCardTarget = creditCardTarget,
        q3CorporateCardTarget = corporateCardTarget,
        q3B2bTarget = b2bTarget,
        updatedAt = now
      )
      "Q4" -> existing.copy(
        targetYear = year,
        activeQuarter = "Q4",
        creditCardTarget = creditCardTarget,
        corporateCardTarget = corporateCardTarget,
        b2bTarget = b2bTarget,
        q4CreditCardTarget = creditCardTarget,
        q4CorporateCardTarget = corporateCardTarget,
        q4B2bTarget = b2bTarget,
        updatedAt = now
      )
      else -> existing.copy(
        targetYear = year,
        activeQuarter = cleanQ.ifBlank { "Q4" },
        creditCardTarget = creditCardTarget,
        corporateCardTarget = corporateCardTarget,
        b2bTarget = b2bTarget,
        q1CreditCardTarget = creditCardTarget,
        q1CorporateCardTarget = corporateCardTarget,
        q1B2bTarget = b2bTarget,
        q2CreditCardTarget = creditCardTarget,
        q2CorporateCardTarget = corporateCardTarget,
        q2B2bTarget = b2bTarget,
        q3CreditCardTarget = creditCardTarget,
        q3CorporateCardTarget = corporateCardTarget,
        q3B2bTarget = b2bTarget,
        q4CreditCardTarget = creditCardTarget,
        q4CorporateCardTarget = corporateCardTarget,
        q4B2bTarget = b2bTarget,
        updatedAt = now
      )
    }
    database.rmTargetDao().insertOrUpdateTarget(updated)

    val currentUser = authRepository.currentUser.value
    if (currentUser != null && (currentUser.role == "ADMIN" || currentUser.role == "MENTOR")) {
      val targetRmUser = database.userDao().getUser(rmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: rmCode
      val formattedTime = DateUtils.formatDateTime(now)
      val qLabel = if (cleanQ in listOf("Q1", "Q2", "Q3", "Q4")) "$cleanQ $year" else "All Quarters $year"
      val smsMessage = "[EBL Alert] Dear $rmName ($rmCode), your $qLabel sales target was set by ${currentUser.role} (${currentUser.rmCode}). Targets -> CC: $creditCardTarget, Corp: $corporateCardTarget, B2B: $b2bTarget. Timestamp: $formattedTime. EBL Sales Suite."

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
    val currentUser = authRepository.currentUser.value
    val now = DateUtils.currentDhakaMillis()
    val allSms = database.smsNotificationDao().getAllSms()
    val targetSms = allSms.find { it.id == id }
    database.smsNotificationDao().deleteSms(id)

    // Store in persistent deleted_sms_ids setting
    val currentDel = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
    val toAdd = mutableListOf<String>()
    toAdd.add(id.toString())
    if (targetSms != null && targetSms.sentTimestamp > 0L) {
      toAdd.add(targetSms.sentTimestamp.toString())
    }
    val updatedDel = (currentDel.split(",").filter { it.isNotBlank() } + toAdd).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_sms_ids", updatedDel, currentUser?.rmCode ?: "SYSTEM", now)
    )
    deletedSmsIdsCache.addAll(toAdd)

    // Track signature so it never regenerates on Google Sheets sync
    if (targetSms != null && !targetSms.fileId.isNullOrBlank()) {
      val currentSigs = database.appSettingDao().getSetting("deleted_sms_signatures")?.settingValue ?: ""
      val sig = "${targetSms.fileId}_${targetSms.recipientRmCode}_${targetSms.actionType}"
      val updatedSigs = (currentSigs.split(",").filter { it.isNotBlank() } + sig).distinct().joinToString(",")
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("deleted_sms_signatures", updatedSigs, currentUser?.rmCode ?: "SYSTEM", now)
      )
    }

    try {
      val prefs = context?.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
      val deletedSet = (prefs?.getStringSet("permanently_deleted_sms_keys", emptySet()) ?: emptySet()).toMutableSet()
      deletedSet.add(id.toString())
      targetSms?.let {
        deletedSet.add("${it.recipientRmCode}_${it.fileId ?: ""}_${it.messageText.take(20).hashCode()}")
        if (it.sentTimestamp > 0L) deletedSet.add(it.sentTimestamp.toString())
      }
      prefs?.edit()?.putStringSet("permanently_deleted_sms_keys", deletedSet)?.apply()
    } catch (_: Exception) {}

    // Synchronize SMS deletion directly to Google Sheets permanently
    targetSms?.let {
      syncSmsDeletionToGoogleSheets(it.id.toString(), it.sentTimestamp.toString(), it.messageText)
    } ?: syncSmsDeletionToGoogleSheets(id.toString())

    database.smsNotificationDao().pruneOldSms()
  }

  suspend fun notifySmsDispatchedToAll(
    customerName: String,
    customerMobile: String,
    messageText: String
  ) = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
    val now = DateUtils.currentDhakaMillis()
    val allUsers = database.userDao().getAllUsers()
    val alertMsg = "[EBL SMS Notice] Checklist SMS dispatched to '$customerName' ($customerMobile) by ${currentUser?.name ?: "User"} (${currentUser?.role ?: "RM"}). Message: ${messageText.take(80)}..."

    for (u in allUsers) {
      database.smsNotificationDao().insertSms(
        SmsNotificationEntity(
          recipientRmCode = u.rmCode,
          recipientMobile = u.mobile,
          recipientName = u.name,
          triggeredByRole = currentUser?.role ?: "SYSTEM",
          triggeredByCode = currentUser?.rmCode ?: "SYSTEM",
          actionType = "CHECKLIST_SMS",
          targetType = "CUSTOMER_FILE",
          fileId = null,
          customerName = customerName,
          messageText = alertMsg,
          sentTimestamp = now,
          status = "SENT_IN_APP",
          isRead = false
        )
      )
    }
  }

  suspend fun clearSmsForRm(rmCode: String) = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
    val now = DateUtils.currentDhakaMillis()
    val cleanRm = rmCode.trim().uppercase()
    val allSms = database.smsNotificationDao().getAllSms().filter { it.recipientRmCode.equals(cleanRm, ignoreCase = true) }
    val currentDel = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
    val newDelIds = allSms.map { it.id.toString() }
    newDelIds.forEach { deletedSmsIdsCache.add(it) }
    val updatedDel = (currentDel.split(",").filter { it.isNotBlank() } + newDelIds).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_sms_ids", updatedDel, currentUser?.rmCode ?: "SYSTEM", now)
    )

    // Track last cleared timestamp so historical rows in Google Sheet never re-appear
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("last_cleared_sms_timestamp_$cleanRm", now.toString(), currentUser?.rmCode ?: "SYSTEM", now)
    )

    val currentSigs = database.appSettingDao().getSetting("deleted_sms_signatures")?.settingValue ?: ""
    val newSigs = allSms.filter { !it.fileId.isNullOrBlank() }.map { "${it.fileId}_${it.recipientRmCode}_${it.actionType}" }
    val updatedSigs = (currentSigs.split(",").filter { it.isNotBlank() } + newSigs).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_sms_signatures", updatedSigs, currentUser?.rmCode ?: "SYSTEM", now)
    )

    database.smsNotificationDao().clearSmsForRm(cleanRm)
    database.smsNotificationDao().pruneOldSms()
    syncClearSmsToGoogleSheets(cleanRm)
  }

  suspend fun clearAllSms() = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
    val now = DateUtils.currentDhakaMillis()
    val allSms = database.smsNotificationDao().getAllSms()
    val currentDel = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
    val newDelIds = allSms.flatMap { listOf(it.id.toString(), it.sentTimestamp.toString()) }
    newDelIds.forEach { deletedSmsIdsCache.add(it) }
    val updatedDel = (currentDel.split(",").filter { it.isNotBlank() } + newDelIds).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_sms_ids", updatedDel, currentUser?.rmCode ?: "SYSTEM", now)
    )

    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("last_cleared_sms_timestamp", now.toString(), currentUser?.rmCode ?: "SYSTEM", now)
    )

    val currentSigs = database.appSettingDao().getSetting("deleted_sms_signatures")?.settingValue ?: ""
    val newSigs = allSms.filter { !it.fileId.isNullOrBlank() }.map { "${it.fileId}_${it.recipientRmCode}_${it.actionType}" }
    val updatedSigs = (currentSigs.split(",").filter { it.isNotBlank() } + newSigs).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_sms_signatures", updatedSigs, currentUser?.rmCode ?: "SYSTEM", now)
    )

    database.smsNotificationDao().clearAllSms()
    database.smsNotificationDao().pruneOldSms()
    syncClearSmsToGoogleSheets("ALL")
  }

  fun getFileByIdFlow(fileId: String): Flow<CustomerFileEntity?> {
    return database.customerFileDao().getFileByIdFlow(fileId)
  }

  suspend fun getFileById(fileId: String): CustomerFileEntity? = withContext(Dispatchers.IO) {
    database.customerFileDao().getFileById(fileId) ?: database.customerFileDao().getFileByAnyId(fileId)
  }

  suspend fun getFileByAnyId(id: String): CustomerFileEntity? = withContext(Dispatchers.IO) {
    database.customerFileDao().getFileByAnyId(id) ?: database.customerFileDao().getFileById(id)
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
    submissionAddress: String? = null,
    serialNumber: String? = null
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
    val cleanId = fileId?.trim().orEmpty()
    val cleanCc = ccNumber.trim()

    // Robust resolution: find existing file by fileId, secondary lookup by ccNumber, or any alternate ID
    val existing = if (cleanId.isNotBlank()) {
      database.customerFileDao().getFileById(cleanId)
        ?: database.customerFileDao().getFileByAnyId(cleanId)
        ?: (if (cleanCc.isNotBlank()) database.customerFileDao().getFileByAnyId(cleanCc) else null)
    } else if (cleanCc.isNotBlank()) {
      database.customerFileDao().getFileByAnyId(cleanCc)
    } else null

    // Always keep consistent primary key fileId so Room's @Update updates the exact database row!
    val targetFileId = existing?.fileId ?: (if (cleanId.isNotBlank()) cleanId else SecurityUtils.generateFileId(resolvedRmCode))
    val isNew = existing == null

    // CRITICAL REQUIREMENT: Mobile number MUST be strictly unique across all active files
    val cleanMobile = mobile.trim()
    val normalizedMobile = cleanMobile.replace(Regex("[^0-9]"), "").removePrefix("88").trimStart('0')
    if (normalizedMobile.length >= 6) {
      val allActive = database.customerFileDao().getAllActiveFiles()
      val duplicate = allActive.firstOrNull { f ->
        !f.isDeleted && f.fileId != targetFileId && (
          f.mobile.trim().equals(cleanMobile, ignoreCase = true) ||
          f.mobile.replace(Regex("[^0-9]"), "").removePrefix("88").trimStart('0') == normalizedMobile ||
          (f.altMobile.isNotBlank() && f.altMobile.replace(Regex("[^0-9]"), "").removePrefix("88").trimStart('0') == normalizedMobile)
        )
      }
      if (duplicate != null) {
        val dupIdentifier = duplicate.ccNumber.ifBlank { duplicate.fileId }
        return@withContext Result.failure(
          Exception("This number already exists! Mobile '$cleanMobile' is already registered with Customer '${duplicate.customerName}' ($dupIdentifier). Duplicate mobile numbers are strictly prohibited.")
        )
      }
    }

    // Resolve Serial Number (SL / SL Wise): sequential by default, customizable later
    val allActiveFiles = database.customerFileDao().getAllActiveFiles()
    val resolvedSl = if (!serialNumber.isNullOrBlank()) {
      serialNumber.trim()
    } else if (existing != null && existing.serialNumber.isNotBlank()) {
      existing.serialNumber
    } else {
      val maxSl = allActiveFiles.mapNotNull { it.serialNumber.toIntOrNull() }.maxOrNull() ?: allActiveFiles.size
      (maxSl + 1).toString()
    }

    // Check authorization for edit
    if (existing != null && currentUser.role == "RM" && !existing.assignedRmCode.trim().equals(currentUser.rmCode.trim(), ignoreCase = true)) {
      return@withContext Result.failure(Exception("You do not have permission to modify this record."))
    }

    val createdTimestamp = existing?.createdAt ?: now
    val createdBy = if (existing == null || existing.createdBy == "SYSTEM") currentUser.rmCode else existing.createdBy
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
      serialNumber = resolvedSl,
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

      // Requirement 5: Data add korle admin abong mentor notifiaction pabe.
      val adminAndMentorUsers = database.userDao().getAllUsers().filter { it.role == "ADMIN" || it.role == "MENTOR" }
      val formattedTime = DateUtils.formatDateTime(now)
      for (mgr in adminAndMentorUsers) {
        val alertMsg = "[EBL Alert] 📢 New Customer Data Added: '${entity.customerName}' (${entity.productType}) entered by ${currentUser.name} (${currentUser.role}, RM: ${currentUser.rmCode}). Assigned RM: ${entity.assignedRmCode}. Time: $formattedTime."
        var mgrSmsStatus = "DELIVERED"
        if (context != null && mgr.mobile.isNotBlank()) {
          val sent = SmsService.sendSms(context, mgr.mobile, alertMsg)
          mgrSmsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
        }
        database.smsNotificationDao().insertSms(
          SmsNotificationEntity(
            recipientRmCode = mgr.rmCode,
            recipientMobile = mgr.mobile,
            recipientName = mgr.name,
            triggeredByRole = currentUser.role,
            triggeredByCode = currentUser.rmCode,
            actionType = "NEW_FILE",
            targetType = "CUSTOMER_FILE",
            fileId = targetFileId,
            customerName = customerName,
            messageText = alertMsg,
            sentTimestamp = now,
            status = mgrSmsStatus,
            isRead = false
          )
        )
      }
      context?.let { ctx ->
        for (mgr in adminAndMentorUsers) {
          NotificationHelper.sendRmFileUpdateNotification(
            context = ctx,
            targetRmCode = mgr.rmCode,
            ccNumber = ccNumber.ifBlank { targetFileId },
            customerName = customerName,
            changeDetails = "New customer file created by ${currentUser.name} (${currentUser.role})",
            updatedByRole = currentUser.role
          )
        }
      }
    } else {
      database.customerFileDao().updateFile(entity)

      // Requirement 6: Kono rm er data update korle sei rm notifiaction pabe
      val targetRmUser = database.userDao().getUser(resolvedRmCode)
      val rmMobile = targetRmUser?.mobile ?: ""
      val rmName = targetRmUser?.name ?: resolvedRmCode
      val formattedTime = DateUtils.formatDateTime(now)

      val hasRmChanged = !existing.assignedRmCode.equals(resolvedRmCode, ignoreCase = true)
      val prevStatus = when {
        existing.applicationStatus != applicationStatus && existing.cpvStatus != cpvStatus ->
          "${existing.applicationStatus} (CPV: ${existing.cpvStatus})"
        existing.cpvStatus != cpvStatus -> existing.cpvStatus
        else -> existing.applicationStatus
      }
      val presStatus = when {
        existing.applicationStatus != applicationStatus && existing.cpvStatus != cpvStatus ->
          "$applicationStatus (CPV: $cpvStatus)"
        existing.cpvStatus != cpvStatus -> cpvStatus
        else -> applicationStatus
      }
      val changePerson = "${currentUser.name.ifBlank { currentUser.rmCode }} (${currentUser.role})"
      val fileType = entity.productType.ifBlank { "Credit Card" }

      val smsMessage = if (hasRmChanged) {
        "Dear $rmName your $fileType file customer name '$customerName' is change by $changePerson from 'Assigned to ${existing.assignedRmCode}' to 'Reassigned to $resolvedRmCode' time is $formattedTime"
      } else {
        "Dear $rmName your $fileType file customer name '$customerName' is change by $changePerson from '$prevStatus' to '$presStatus' time is $formattedTime"
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
        val oldMsg = "Dear $oldRmName your $fileType file customer name '$customerName' is change by $changePerson from 'Under your code' to 'Transferred to $resolvedRmCode' time is $formattedTime"

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
          changeDetails = "Status: $presStatus",
          updatedByRole = currentUser.role,
          customMessage = smsMessage,
          dedupeKey = "rm_update_${targetFileId}_${presStatus}_${now / 15000}"
        )
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

    // Automatically trigger immediate customer file sync and full background sync
    applicationScope.launch {
      try {
        syncCustomerFileUpsertToGoogleSheets(entity)
      } catch (_: Exception) {}
      triggerGoogleSheetsSync()
    }

    Result.success(entity)
  }

  suspend fun syncCustomerFileUpsertToGoogleSheets(file: CustomerFileEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: com.example.data.model.SyncStatusEntity()
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val fObj = org.json.JSONObject().apply {
        put("serialNumber", file.serialNumber.ifBlank { "1" })
        put("sl", file.serialNumber.ifBlank { "1" })
        put("ccNumber", file.ccNumber.ifBlank { file.fileId })
        put("fileId", file.fileId)
        put("customerName", file.customerName)
        put("companyName", file.companyName)
        put("officeAddress", file.officeAddress)
        put("mobile", file.mobile)
        put("email", file.email)
        put("productType", file.productType)
        put("applicationStatus", file.applicationStatus)
        put("activeStatus", file.activeStatus)
        put("assignedRmCode", file.assignedRmCode)
        put("pendingDocuments", file.pendingDocuments)
        put("remarks", file.remarks)
        put("cpvRemarks", file.cpvRemarks)
        put("cpvStatus", file.cpvStatus)
        put("submissionAddress", file.submissionAddress ?: "")
        put("submissionLat", file.submissionLatitude ?: 0.0)
        put("submissionLng", file.submissionLongitude ?: 0.0)
        put("updatedAt", DateUtils.formatDateTime(file.updatedAt))
        put("updatedBy", file.updatedBy)
      }

      val filesArr = org.json.JSONArray().apply { put(fObj) }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_CUSTOMER_FILE")
        put("file", fObj)
        put("files", filesArr)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }

      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp ->
        if (resp.isSuccessful) {
          database.customerFileDao().markFileSynced(file.fileId)
          updatePendingSyncCount()
          database.appSettingDao().insertOrUpdateSyncStatus(
            currentStatus.copy(
              appsScriptUrl = activeWebAppUrl,
              lastSyncTimestamp = DateUtils.currentDhakaMillis(),
              lastSyncStatus = "SUCCESS",
              lastSyncMessage = "Customer file '${file.customerName}' synchronized with Google Sheets."
            )
          )
        }
      }
    } catch (_: Exception) {}
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

  suspend fun syncEventDeletionToGoogleSheets(eventId: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_EVENT")
        put("eventId", eventId)
        put("deletedEventIds", org.json.JSONArray().apply { put(eventId) })
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncSettingDeletionToGoogleSheets(key: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_SETTING")
        put("settingKey", key)
        put("presetName", key)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncSmsDeletionToGoogleSheets(smsId: String, timestamp: String = "", messageText: String = ""): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val delArray = org.json.JSONArray().apply {
        put(smsId)
        if (timestamp.isNotBlank() && timestamp != smsId) put(timestamp)
      }
      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_SMS")
        put("smsId", smsId)
        put("timestamp", timestamp)
        put("messageText", messageText)
        put("deletedSmsIds", delArray)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncClearSmsToGoogleSheets(rmCode: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "CLEAR_SMS_FOR_RM")
        put("rmCode", rmCode)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncDocumentDeletionToGoogleSheets(docId: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_DOC")
        put("docId", docId)
        put("deletedDocIds", org.json.JSONArray().apply { put(docId) })
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncAttachmentDeletionToGoogleSheets(attachmentId: String, fileId: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_ATTACHMENT")
        put("attachmentId", attachmentId)
        put("fileId", fileId)
        put("deletedAttachmentIds", org.json.JSONArray().apply { put(attachmentId) })
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncPresetDeletionToGoogleSheets(presetName: String, presetKey: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_SETTING")
        put("settingKey", presetKey)
        put("presetName", presetName)
        put("deletedPresetKeys", org.json.JSONArray().apply {
          put(presetKey)
          put(presetName)
        })
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun uploadFileToGoogleDrive(
    file: java.io.File,
    fileName: String,
    fileType: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val base64 = com.example.util.AttachmentHelper.fileToBase64(file)
        ?: return@withContext Result.failure(Exception("Failed to encode file for upload."))

      val currentStatus = database.appSettingDao().getSyncStatus() ?: com.example.data.model.SyncStatusEntity()
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) {
        return@withContext Result.failure(Exception("Google Apps Script URL is invalid."))
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPLOAD_FILE_TO_DRIVE")
        put("fileBase64", base64)
        put("fileName", fileName)
        put("fileType", fileType)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }

      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      val respStr = httpClient.newCall(request).execute().use { resp ->
        resp.body?.string().orEmpty()
      }

      val respJson = org.json.JSONObject(respStr)
      if (respJson.optString("status").equals("SUCCESS", ignoreCase = true)) {
        val fileUrl = respJson.optString("fileUrl").ifBlank { respJson.optString("driveUrl") }
        if (fileUrl.isNotBlank()) {
          return@withContext Result.success(fileUrl)
        }
      }
      var err = respJson.optString("message", "Unknown error uploading to Drive")
      if (err.contains("permission to call DriveApp", ignoreCase = true) || err.contains("DriveApp", ignoreCase = true)) {
        err = "Google Apps Script-এ DriveApp পারমিশন Allow করা নেই। Apps Script Editor-এ 'authorizeDrivePermissions' রান করে পারমিশন দিন।"
      }
      Result.failure(Exception(err))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncDocumentUpsertToGoogleSheets(doc: com.example.data.model.ImportantDocumentEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val docJson = org.json.JSONObject().apply {
        put("docId", doc.docId)
        put("title", doc.title)
        put("category", doc.category)
        put("description", doc.description)
        put("fileName", doc.fileName)
        put("fileType", doc.fileType)
        put("fileSizeBytes", doc.fileSizeBytes)
        put("fileUri", doc.fileUri)
        put("storagePath", doc.storagePath)
        put("uploadedBy", doc.uploadedBy)
        put("uploaderName", doc.uploaderName)
        put("uploaderRole", doc.uploaderRole)
        put("createdAt", DateUtils.formatDateTime(doc.createdAt))
        put("updatedAt", DateUtils.formatDateTime(doc.updatedAt))

        if (doc.storagePath.isNotBlank()) {
          val localF = java.io.File(doc.storagePath)
          if (localF.exists() && localF.length() > 0) {
            val b64 = com.example.util.AttachmentHelper.fileToBase64(localF)
            if (b64 != null) {
              put("fileBase64", b64)
            }
          }
        }
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_DOC")
        put("document", docJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      val respStr = httpClient.newCall(request).execute().use { resp -> resp.body?.string().orEmpty() }
      if (respStr.isNotBlank()) {
        try {
          val rj = org.json.JSONObject(respStr)
          val returnedUri = rj.optString("fileUri")
          val returnedStorage = rj.optString("storagePath")
          if (returnedUri.startsWith("http")) {
            database.importantDocumentDao().updateDocument(
              doc.copy(fileUri = returnedUri, storagePath = returnedStorage.ifBlank { returnedUri })
            )
          }
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }

  suspend fun syncAttachmentUpsertToGoogleSheets(
    att: com.example.data.model.FileAttachmentEntity,
    fileBase64: String? = null
  ): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val attJson = org.json.JSONObject().apply {
        put("attachmentId", att.attachmentId)
        put("fileId", att.fileId)
        put("category", att.category)
        put("fileName", att.fileName)
        put("fileType", att.fileType)
        put("fileSize", att.fileSizeBytes)
        put("fileUri", att.fileUri)
        put("storagePath", att.storagePath)
        put("uploadedBy", att.uploadedBy)
        put("uploadedAt", DateUtils.formatDateTime(att.uploadedAt))
        if (!fileBase64.isNullOrBlank()) {
          put("fileBase64", fileBase64)
        }
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_ATTACHMENT")
        put("attachment", attJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      val respStr = httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
      if (!respStr.isNullOrBlank()) {
        try {
          val resJson = org.json.JSONObject(respStr)
          val driveUrl = resJson.optString("fileUrl", resJson.optString("driveUrl", ""))
          if (driveUrl.isNotBlank() && driveUrl.startsWith("http")) {
            val updatedAtt = att.copy(fileUri = driveUrl)
            database.fileAttachmentDao().insertAttachment(updatedAtt)
          }
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }

  suspend fun syncEventUpsertToGoogleSheets(event: com.example.data.model.TeamEventEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val evJson = org.json.JSONObject().apply {
        put("eventId", event.eventId)
        put("title", event.title)
        put("description", event.description)
        put("creatorRmCode", event.creatorRmCode)
        put("creatorName", event.creatorName)
        put("targetDate", event.targetDate)
        put("allowedFields", event.allowedFields)
        put("status", event.status)
        put("createdAt", DateUtils.formatDateTime(event.createdAt))
        put("closeTime", event.closeTime)
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_EVENT")
        put("event", evJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncEventResponseUpsertToGoogleSheets(respEntity: com.example.data.model.EventResponseEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val rJson = org.json.JSONObject().apply {
        put("responseId", respEntity.responseId)
        put("eventId", respEntity.eventId)
        put("rmCode", respEntity.rmCode)
        put("rmName", respEntity.rmName)
        put("filesCount", respEntity.filesCount)
        put("requestedDate", respEntity.requestedDate)
        put("location", respEntity.location)
        put("remarks", respEntity.remarks)
        put("customerEntriesJson", respEntity.customerEntriesJson)
        put("submittedAt", DateUtils.formatDateTime(respEntity.submittedAt))
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_EVENT_RESPONSE")
        put("eventResponse", rJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncChatUpsertToGoogleSheets(msg: com.example.data.model.ChatMessageEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val mJson = org.json.JSONObject().apply {
        put("id", msg.id)
        put("senderRmCode", msg.senderRmCode)
        put("senderName", msg.senderName)
        put("senderRole", msg.senderRole)
        put("recipientRmCode", msg.recipientRmCode ?: "")
        put("messageText", msg.messageText)
        put("timestamp", DateUtils.formatDateTime(msg.timestamp))
        put("messageType", msg.messageType)
        put("eventId", msg.eventId ?: "")
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_CHAT")
        put("chatMessage", mJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncSmsUpsertToGoogleSheets(sms: com.example.data.model.SmsNotificationEntity): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val sJson = org.json.JSONObject().apply {
        put("id", sms.id)
        put("recipientRmCode", sms.recipientRmCode)
        put("recipientName", sms.recipientName)
        put("recipientMobile", sms.recipientMobile)
        put("triggeredByRole", sms.triggeredByRole)
        put("triggeredByCode", sms.triggeredByCode)
        put("actionType", sms.actionType)
        put("targetType", sms.targetType)
        put("fileId", sms.fileId ?: "")
        put("customerName", sms.customerName ?: "")
        put("messageText", sms.messageText)
        put("sentTimestamp", DateUtils.formatDateTime(sms.sentTimestamp))
        put("status", sms.status)
      }

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_SMS")
        put("sms", sJson)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncSettingUpsertToGoogleSheets(key: String, value: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "UPSERT_SETTING")
        put("settingKey", key)
        put("settingValue", value)
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
    } catch (_: Exception) {}
  }

  suspend fun syncChatDeletionToGoogleSheets(chatId: String): Unit = withContext(Dispatchers.IO) {
    try {
      val currentStatus = database.appSettingDao().getSyncStatus() ?: return@withContext
      val activeWebAppUrl = currentStatus.appsScriptUrl.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      }
      if (activeWebAppUrl.isBlank() || !activeWebAppUrl.startsWith("http")) return@withContext

      val payload = org.json.JSONObject().apply {
        put("action", "DELETE_CHAT")
        put("chatId", chatId)
        put("deletedChatIds", org.json.JSONArray().apply { put(chatId) })
        put("spreadsheetId", currentStatus.spreadsheetId.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" })
        put("secretKey", currentStatus.syncSecretKey.ifBlank { "ebl_secure_sync_token_2026" })
      }
      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(activeWebAppUrl).post(requestBody).build()
      httpClient.newCall(request).execute().use { resp -> resp.body?.string() }
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
    deletedFileIdsCache.add(fileId.lowercase())
    if (file.ccNumber.isNotBlank()) deletedFileIdsCache.add(file.ccNumber.lowercase())

    // Store in persistent deleted_file_ids setting
    val currentDel = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
    val toAdd = listOfNotNull(file.fileId.ifBlank { null }, file.ccNumber.ifBlank { null }).joinToString(",")
    val updatedDel = if (currentDel.isBlank()) toAdd else "$currentDel,$toAdd"
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_file_ids", updatedDel, currentUser.rmCode, now)
    )

    // Immediately synchronize deletion with Google Sheets
    syncFileDeletionToGoogleSheets(fileId, file.ccNumber)

    // Requirement 6: Kono rm er data update korle ba delet korle sei rm notifiaction pabe!
    val targetRmUser = database.userDao().getUser(file.assignedRmCode)
    val rmMobile = targetRmUser?.mobile ?: ""
    val rmName = targetRmUser?.name ?: file.assignedRmCode
    val formattedTime = DateUtils.formatDateTime(now)

    val smsMessage = "[EBL Alert] ATTENTION: Dear $rmName (${file.assignedRmCode}), your customer file $fileId ('${file.customerName}') was DELETED by ${currentUser.name} (${currentUser.role}). Timestamp: $formattedTime. EBL Sales Suite."

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
        changeDetails = "File DELETED by ${currentUser.name} (${currentUser.role})",
        updatedByRole = currentUser.role
      )
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

    // Remove from persistent deleted_file_ids setting so it pushes back to active sheet!
    val currentDel = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
    val toRemove = setOf(fileId, file?.ccNumber ?: "").filter { it.isNotBlank() }
    val updatedDel = currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() && it !in toRemove }.joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_file_ids", updatedDel, currentUser.rmCode, now)
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
    deletedFileIdsCache.add(fileId.lowercase())
    if (!file?.ccNumber.isNullOrBlank()) deletedFileIdsCache.add(file.ccNumber.lowercase())

    val currentDel = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
    val toAdd = listOfNotNull(fileId, file?.ccNumber?.ifBlank { null }).joinToString(",")
    val updatedDel = if (currentDel.isBlank()) toAdd else "$currentDel,$toAdd"
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_file_ids", updatedDel, currentUser.rmCode, now)
    )

    // Immediately synchronize permanent deletion with Google Sheets
    syncFileDeletionToGoogleSheets(fileId, file?.ccNumber)

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
    fileUri: String,
    localFile: java.io.File? = null
  ): Result<FileAttachmentEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    val attachmentId = "ATT-${SecurityUtils.generateUniqueId().take(8)}"
    val now = DateUtils.currentDhakaMillis()

    var finalFileUri = fileUri
    var finalStoragePath = if (localFile != null && localFile.exists()) localFile.absolutePath else "files/${currentUser.rmCode}/$fileId/$fileName"

    // If a physical file is provided, upload directly to Google Drive so ALL users on other devices can see it!
    val targetFile = localFile
      ?: (if (fileUri.isNotBlank() && !fileUri.startsWith("http")) java.io.File(fileUri) else null)
      ?: (if (finalStoragePath.isNotBlank() && !finalStoragePath.startsWith("http")) java.io.File(finalStoragePath) else null)
    if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
      try {
        val driveRes = uploadFileToGoogleDrive(targetFile, fileName, fileType)
        if (driveRes.isSuccess) {
          val driveUrl = driveRes.getOrNull()
          if (!driveUrl.isNullOrBlank()) {
            finalFileUri = driveUrl
            finalStoragePath = driveUrl
          }
        }
      } catch (_: Exception) {}
    }

    val attachment = FileAttachmentEntity(
      attachmentId = attachmentId,
      fileId = fileId,
      category = category,
      fileName = fileName,
      fileType = fileType,
      storagePath = finalStoragePath,
      fileUri = finalFileUri,
      fileSizeBytes = fileSizeBytes,
      uploadedBy = currentUser.rmCode,
      uploadedAt = now
    )

    database.fileAttachmentDao().insertAttachment(attachment)
    applicationScope.launch {
      val b64 = if (targetFile != null && targetFile.exists() && !finalFileUri.startsWith("http")) {
        com.example.util.AttachmentHelper.fileToBase64(targetFile)
      } else null
      syncAttachmentUpsertToGoogleSheets(attachment, b64)
      triggerGoogleSheetsSync()
    }
    Result.success(attachment)
  }

  suspend fun deleteAttachment(attachmentId: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    database.fileAttachmentDao().deleteAttachment(attachmentId)

    val now = DateUtils.currentDhakaMillis()
    deletedAttachmentIdsCache.add(attachmentId)
    deletedAttachmentIdsCache.add(attachmentId.uppercase())
    val currentDel = database.appSettingDao().getSetting("deleted_attachment_ids")?.settingValue ?: ""
    val updatedDel = (currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() } + attachmentId).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_attachment_ids", updatedDel, currentUser.rmCode, now)
    )

    applicationScope.launch {
      syncAttachmentDeletionToGoogleSheets(attachmentId, fileId)
      triggerGoogleSheetsSync()
    }
    Result.success(Unit)
  }

  // 3. RM & Mentor Management (Admin / Mentor)
  fun getAllRmsFlow(): Flow<List<UserEntity>> {
    return database.userDao().getAllRmsFlow()
  }

  fun getAllOfficersFlow(): Flow<List<UserEntity>> {
    return database.userDao().getAllOfficersFlow()
  }

  fun getAllMentorsFlow(): Flow<List<UserEntity>> {
    return database.userDao().getAllMentorsFlow()
  }

  suspend fun createRm(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    initialPassword: String,
    role: String = "RM"
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Only Admin or Mentor can assign new RM or Mentor accounts."))
    }

    val cleanRmCode = rmCode.trim().uppercase()
    val cleanRole = if (role.trim().equals("MENTOR", ignoreCase = true)) "MENTOR" else "RM"
    if (cleanRmCode.isBlank() || name.trim().isBlank()) {
      return@withContext Result.failure(Exception("RM Code and Name are mandatory."))
    }

    if (database.userDao().getUser(cleanRmCode) != null) {
      return@withContext Result.failure(Exception("Code '$cleanRmCode' is already assigned to an existing account."))
    }

    val effectivePassword = if (initialPassword.trim().isBlank()) SecurityUtils.generateTemporaryPassword() else initialPassword.trim()
    val now = DateUtils.currentDhakaMillis()
    val salt = SecurityUtils.generateSalt()
    val hash = SecurityUtils.hashPassword(effectivePassword, salt)

    val newUser = UserEntity(
      rmCode = cleanRmCode,
      name = name.trim(),
      role = cleanRole,
      passwordHash = hash,
      salt = salt,
      mobile = mobile.trim(),
      email = email.trim(),
      officeAddress = officeAddress.trim(),
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = now,
      authUid = if (cleanRole == "MENTOR") "AUTH_MENTOR_$cleanRmCode" else "AUTH_RM_$cleanRmCode"
    )

    database.userDao().insertUser(newUser)
    if (cleanRole == "RM") {
      database.rmTargetDao().insertOrUpdateTarget(
        RmTargetEntity(
          rmCode = cleanRmCode,
          creditCardTarget = 15,
          corporateCardTarget = 5,
          b2bTarget = 2,
          updatedAt = now
        )
      )
    }

    // Remove from deleted_rm_codes setting and in-memory cache in case previously deleted
    try {
      val currentDel = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
      if (currentDel.isNotBlank()) {
        val updatedDel = currentDel.split(",")
          .map { it.trim().uppercase() }
          .filter { it.isNotBlank() && it != cleanRmCode }
          .joinToString(",")
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_rm_codes", updatedDel, currentUser.rmCode, now)
        )
      }
      deletedRmCodesCache.remove(cleanRmCode)

      // Set password immunity window so background sync does not overwrite newly set password
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("RM_PASS_UPDATED_AT_$cleanRmCode", now.toString(), currentUser.rmCode, now)
      )
    } catch (_: Exception) {}

    // Security Audit Log
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = if (cleanRole == "MENTOR") "MENTOR_CREATE" else "RM_CREATE",
        details = "Assigned & created new $cleanRole account ${newUser.name} ($cleanRmCode) by ${currentUser.role} (${currentUser.name.ifBlank { currentUser.rmCode }})",
        timestamp = now,
        rmCode = cleanRmCode
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
    newPassword: String? = null,
    role: String? = null
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }

    val existing = database.userDao().getUser(rmCode)
      ?: return@withContext Result.failure(Exception("User not found."))

    val targetStatus = existing.accountStatus
    val targetRole = if (role != null && (role.equals("MENTOR", true) || role.equals("RM", true))) role.uppercase() else existing.role

    val updated = if (!newPassword.isNullOrBlank()) {
      val salt = SecurityUtils.generateSalt()
      val hash = SecurityUtils.hashPassword(newPassword.trim(), salt)
      existing.copy(
        name = name.trim(),
        mobile = mobile.trim(),
        email = email.trim(),
        officeAddress = officeAddress.trim(),
        role = targetRole,
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
        role = targetRole,
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

      val targetUser = database.userDao().getUser(rmCode.trim())
      val payload = org.json.JSONObject().apply {
        put("action", "UPDATE_RM_PASSWORD")
        put("rmCode", rmCode.trim().uppercase())
        put("passwordHash", passwordHash)
        put("salt", salt)
        put("name", targetUser?.name ?: "")
        put("mobile", targetUser?.mobile ?: "")
        put("email", targetUser?.email ?: "")
        put("officeAddress", targetUser?.officeAddress ?: "")
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

    val formattedTime = DateUtils.formatDateTime(now)
    val editorName = currentUser.name.ifBlank { currentUser.rmCode }
    val smsMessage = "[EBL Alert] Dear ${existing.name} ($rmCode), your RM password was reset by ${currentUser.role} ($editorName). New Temporary Password: $newPassword. Timestamp: $formattedTime."
    var smsStatus = "DELIVERED"
    if (context != null && existing.mobile.isNotBlank()) {
      val sent = SmsService.sendSms(context, existing.mobile, smsMessage)
      smsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
    }
    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = rmCode,
        recipientMobile = existing.mobile,
        recipientName = existing.name,
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
      ?: return@withContext Result.failure(Exception("Unauthorized. Please log in first."))
    val now = DateUtils.currentDhakaMillis()
    // 1st column: "Checklist Header", 2nd column: header text
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("Checklist Header", headerTemplate, currentUser.rmCode, now)
    )
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("CHECKLIST_HEADER_TEMPLATE", headerTemplate, currentUser.rmCode, now)
    )
    // 1st column: "Checklist Footer", 2nd column: footer text
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("Checklist Footer", regardsTemplate, currentUser.rmCode, now)
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
    applicationScope.launch {
      syncSettingUpsertToGoogleSheets("Checklist Header", headerTemplate)
      syncSettingUpsertToGoogleSheets("CHECKLIST_HEADER_TEMPLATE", headerTemplate)
      syncSettingUpsertToGoogleSheets("Checklist Footer", regardsTemplate)
      syncSettingUpsertToGoogleSheets("CHECKLIST_REGARDS_TEMPLATE", regardsTemplate)
      if (corporateDocsJson.isNotBlank()) syncSettingUpsertToGoogleSheets("CHECKLIST_CORP_DOCS", corporateDocsJson)
      if (enhancementDocsJson.isNotBlank()) syncSettingUpsertToGoogleSheets("CHECKLIST_ENHANCE_DOCS", enhancementDocsJson)
      triggerGoogleSheetsSync()
    }
    Result.success(Unit)
  }

  suspend fun savePresetRow(presetName: String, itemsText: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized. Please log in first."))
    val cleanName = presetName.trim()
    if (cleanName.isBlank()) return@withContext Result.failure(Exception("Preset name cannot be blank."))
    val now = DateUtils.currentDhakaMillis()

    // 1st column: cleanName (e.g. Preset Name), 2nd column: itemsText
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity(cleanName, itemsText.trim(), currentUser.rmCode, now)
    )
    val keySuffix = cleanName.replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_").uppercase()
    val customKey = "CHECKLIST_CUSTOM_" + keySuffix
    val legacyKey = "CHECKLIST_PRESET_" + keySuffix
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity(customKey, itemsText.trim(), currentUser.rmCode, now)
    )
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity(legacyKey, itemsText.trim(), currentUser.rmCode, now)
    )

    // Un-tombstone if was previously deleted
    val currentDel = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
    if (currentDel.isNotBlank()) {
      val updatedDel = currentDel.split(",").map { it.trim() }.filter {
        it.isNotBlank() && !it.equals(cleanName, true) && !it.equals(customKey, true) && !it.equals(legacyKey, true)
      }.joinToString(",")
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("deleted_preset_keys", updatedDel, currentUser.rmCode, now)
      )
    }
    deletedPresetKeysCache.remove(cleanName.uppercase())
    deletedPresetKeysCache.remove(customKey.uppercase())
    deletedPresetKeysCache.remove(legacyKey.uppercase())

    val existingCustom = database.appSettingDao().getSetting("CHECKLIST_CUSTOM_PRESETS_LIST")?.settingValue ?: ""
    val customList = try {
      val arr = org.json.JSONArray(existingCustom)
      (0 until arr.length()).map { arr.getString(it).trim() }
    } catch (_: Exception) {
      existingCustom.split(",").map { it.trim() }
    }.filter { it.isNotBlank() }
    val updatedList = (customList + cleanName).distinct()
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(updatedList).toString(), currentUser.rmCode, now)
    )
    applicationScope.launch {
      syncSettingUpsertToGoogleSheets(cleanName, itemsText.trim())
      syncSettingUpsertToGoogleSheets(legacyKey, itemsText.trim())
      syncSettingUpsertToGoogleSheets("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(updatedList).toString())
      triggerGoogleSheetsSync()
    }
    Result.success(Unit)
  }

  suspend fun deletePresetRow(presetName: String, presetKey: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized. Please log in first."))
    val cleanName = presetName.trim()
    val cleanKey = presetKey.trim().uppercase()
    val legacyKey = "CHECKLIST_PRESET_" + cleanName.replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_").uppercase()

    database.appSettingDao().deleteSetting(cleanName)
    database.appSettingDao().deleteSetting(cleanKey)
    database.appSettingDao().deleteSetting(legacyKey)

    val currentCustom = database.appSettingDao().getSetting("CHECKLIST_CUSTOM_PRESETS_LIST")?.settingValue ?: ""
    val updatedCustom = currentCustom.split(",").map { it.trim() }.filter {
      it.isNotBlank() && !it.equals(cleanName, true) && !it.equals(cleanKey, true)
    }.joinToString(",")
    val now = DateUtils.currentDhakaMillis()
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("CHECKLIST_CUSTOM_PRESETS_LIST", updatedCustom, currentUser.rmCode, now)
    )

    val currentDel = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
    val toAdd = listOf(cleanName, cleanKey, legacyKey).filter { it.isNotBlank() }
    val updatedDel = (currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() } + toAdd).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_preset_keys", updatedDel, currentUser.rmCode, now)
    )
    toAdd.forEach {
      deletedPresetKeysCache.add(it)
      deletedPresetKeysCache.add(it.uppercase())
    }

    syncPresetDeletionToGoogleSheets(cleanName, cleanKey)
    Result.success(Unit)
  }

  fun getAppCustomNameFlow(): Flow<String> {
    return database.appSettingDao().getAllSettingsFlow().map { settings ->
      settings.find {
        it.settingKey == "app_custom_name" || it.settingKey.equals("App Name", ignoreCase = true) || it.settingKey.equals("app_name", ignoreCase = true)
      }?.settingValue?.ifBlank { null } ?: "RM File Management Suite"
    }
  }

  suspend fun setAppCustomName(name: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    if (currentUser.role != "MENTOR" && currentUser.role != "ADMIN") {
      return@withContext Result.failure(Exception("Only Admin or Mentor can configure the universal app name."))
    }
    val cleanName = name.trim().ifBlank { "RM File Management Suite" }
    val now = DateUtils.currentDhakaMillis()
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("app_custom_name", cleanName, currentUser.rmCode, now)
    )
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("App Name", cleanName, currentUser.rmCode, now)
    )
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun setRmStatus(rmCode: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied."))
    }

    val user = database.userDao().getUser(rmCode)
    val now = DateUtils.currentDhakaMillis()
    val formattedTime = DateUtils.formatDateTime(now)
    val editorName = currentUser.name.ifBlank { currentUser.rmCode }
    val smsMessage = "[EBL Alert] Dear ${user?.name ?: rmCode} ($rmCode), your RM account status was changed to '$newStatus' by ${currentUser.role} ($editorName). Timestamp: $formattedTime."
    var smsStatus = "DELIVERED"
    if (context != null && user != null && user.mobile.isNotBlank()) {
      val sent = SmsService.sendSms(context, user.mobile, smsMessage)
      smsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
    }
    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = rmCode,
        recipientMobile = user?.mobile ?: "",
        recipientName = user?.name ?: rmCode,
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

    deletedRmCodesCache.add(rmCode.uppercase())

    // Synchronize RM deletion with Google Sheets
    applicationScope.launch {
      syncRmDeletionToGoogleSheets(rmCode)
    }

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
    if (key.equals("APP_ACTIVE_THEME", true) ||
        key.equals("UI_FONT_STYLE", true) ||
        key.equals("UI_FONT_COLOR", true) ||
        key.equals("UI_TEXT_SCALE", true)) {
      val prefKey = if (key.equals("APP_ACTIVE_THEME", true)) "app_active_theme" else key.lowercase()
      try {
        context?.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
          ?.edit()?.putString(prefKey, value)?.apply()
      } catch (_: Exception) {}
    }
    applicationScope.launch {
      syncSettingUpsertToGoogleSheets(key, value)
      triggerGoogleSheetsSync()
    }
    Result.success(Unit)
  }

  suspend fun deleteSetting(key: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access denied: settings can only be deleted by Admin or Mentor."))
    }

    database.appSettingDao().deleteSetting(key)
    deletedPresetKeysCache.add(key)
    deletedPresetKeysCache.add(key.uppercase())
    val curDel = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
    val updatedDel = if (curDel.isBlank()) key else "$curDel,$key"
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_preset_keys", updatedDel, currentUser.rmCode, DateUtils.currentDhakaMillis())
    )
    syncSettingDeletionToGoogleSheets(key)
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

  // -------------------------------------------------------------
  // File & Application Statuses Management (Admin customizable, visible to all RMs)
  // -------------------------------------------------------------
  fun getFileStatusOptionsFlow(): Flow<List<String>> {
    return database.appSettingDao().getSettingFlow("FILE_STATUSES_LIST").map { setting: AppSettingEntity? ->
      val raw = setting?.settingValue
      if (!raw.isNullOrBlank()) {
        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
      } else {
        listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC")
      }
    }
  }

  suspend fun getFileStatusOptions(): List<String> = withContext(Dispatchers.IO) {
    val setting = database.appSettingDao().getSetting("FILE_STATUSES_LIST")
    val raw = setting?.settingValue
    if (!raw.isNullOrBlank()) {
      raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    } else {
      listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC")
    }
  }

  suspend fun updateFileStatusOptions(newStatuses: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
    if (currentUser?.role != "ADMIN") {
      return@withContext Result.failure(Exception("Only Central Administrators can modify application status categories."))
    }
    val clean = newStatuses.split(",")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .joinToString(", ")
    val now = DateUtils.currentDhakaMillis()
    val finalVal = clean.ifBlank {
      "Collected, Submitted, Analyst Receive, Approved, Declined, Query, Return to Source, Condition, STC"
    }
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity(
        settingKey = "FILE_STATUSES_LIST",
        settingValue = finalVal,
        updatedBy = currentUser.rmCode,
        updatedAt = now
      )
    )
    applicationScope.launch { triggerGoogleSheetsSync() }
    Result.success(Unit)
  }

  suspend fun updateFileStatus(
    fileId: String,
    newStatus: String,
    newRemarks: String? = null
  ): Result<CustomerFileEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized operation."))

    val existing = database.customerFileDao().getFileById(fileId)
      ?: database.customerFileDao().getFileByAnyId(fileId)
      ?: return@withContext Result.failure(Exception("Customer file not found."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR" && !existing.assignedRmCode.equals(currentUser.rmCode, ignoreCase = true)) {
      return@withContext Result.failure(Exception("Access Denied: Only Administrator or Mentor can update file status."))
    }

    val now = DateUtils.currentDhakaMillis()
    val updatedRemarks = if (!newRemarks.isNullOrBlank()) newRemarks.trim() else existing.remarks
    val updated = existing.copy(
      applicationStatus = newStatus.trim(),
      remarks = updatedRemarks,
      updatedAt = now,
      updatedBy = currentUser.rmCode,
      isSynced = false
    )

    database.customerFileDao().updateFile(updated)

    // Audit log
    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "UPDATE_STATUS",
        fileId = existing.fileId,
        details = "Status updated from '${existing.applicationStatus}' to '$newStatus' by ${currentUser.role} (${currentUser.name})",
        timestamp = now
      )
    )

    // Send SMS Notification to assigned RM so RM is immediately informed!
    val targetRmUser = database.userDao().getUser(updated.assignedRmCode)
    val rmMobile = targetRmUser?.mobile ?: ""
    val rmName = targetRmUser?.name ?: updated.assignedRmCode
    val formattedTime = DateUtils.formatDateTime(now)
    val prevStatus = existing.applicationStatus
    val presStatus = newStatus
    val changePerson = "${currentUser.name.ifBlank { currentUser.rmCode }} (${currentUser.role})"
    val fileType = updated.productType.ifBlank { "Credit Card" }
    val smsMessage = "Dear $rmName your $fileType file customer name '${updated.customerName}' is change by $changePerson from '$prevStatus' to '$presStatus' time is $formattedTime"

    var smsStatus = "DELIVERED"
    if (context != null && rmMobile.isNotBlank()) {
      val sent = SmsService.sendSms(context, rmMobile, smsMessage)
      smsStatus = if (sent) "DELIVERED" else "SENT_IN_APP"
    }

    database.smsNotificationDao().insertSms(
      SmsNotificationEntity(
        recipientRmCode = updated.assignedRmCode,
        recipientMobile = rmMobile,
        recipientName = rmName,
        triggeredByRole = currentUser.role,
        triggeredByCode = currentUser.rmCode,
        actionType = "STATUS_UPDATE",
        targetType = "CUSTOMER_FILE",
        fileId = updated.fileId,
        customerName = updated.customerName,
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
        ccNumber = updated.ccNumber.ifBlank { updated.fileId },
        customerName = updated.customerName,
        changeDetails = "Status updated to: $newStatus",
        updatedByRole = currentUser.role,
        customMessage = smsMessage,
        dedupeKey = "status_change_${updated.fileId}_${newStatus}_${now / 15000}"
      )
    }

    // Trigger instant background sync to Google Sheets so ALL users (RM, Admin, Mentor) see the change live!
    applicationScope.launch {
      triggerGoogleSheetsSync()
    }

    Result.success(updated)
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
      val filesToPush = allFiles.filter {
        !it.isDeleted &&
        !it.fileId.startsWith("demo_", ignoreCase = true) &&
        !(it.fileId.startsWith("cust-00", ignoreCase = true) && it.customerName.contains("Sample Customer", ignoreCase = true)) &&
        !(it.customerName.contains("Demo Customer", ignoreCase = true))
      }.sortedWith(
        compareBy<CustomerFileEntity> { it.serialNumber.toIntOrNull() ?: Int.MAX_VALUE }
          .thenBy { it.createdAt }
      )

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
        for ((idx, f) in filesToPush.withIndex()) {
          val fObj = JSONObject().apply {
            put("serialNumber", f.serialNumber.ifBlank { (idx + 1).toString() })
            put("sl", f.serialNumber.ifBlank { (idx + 1).toString() })
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
            put("remarks", f.remarks)
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
            put("lastLatitude", rm.lastLatitude ?: 0.0)
            put("lastLongitude", rm.lastLongitude ?: 0.0)
            put("lastLocationAddress", rm.lastLocationAddress ?: "")
            put("lastLocationTime", if (rm.lastLocationTime != null) DateUtils.formatDateTime(rm.lastLocationTime) else "")
            put("isOnline", rm.isOnline)
          })
        }

        val delPresetsSetting = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
        val delPresetSet = (delPresetsSetting.split(",").map { it.trim().uppercase() } + deletedPresetKeysCache.map { it.uppercase() }).toSet()

        val settingsArray = org.json.JSONArray()
        val hasHdrKey = allSettings.any { it.settingKey == "Checklist Header" }
        val hasFtrKey = allSettings.any { it.settingKey == "Checklist Footer" }

        for (s in allSettings) {
          // Requirement 3: Kono button ba command chara app thake data sheet a jabe na.
          // Never push default pre-set SYSTEM settings or GoogleSheets_Universal pulled settings back to sheet!
          if (s.updatedBy == "SYSTEM" || s.updatedBy == "GoogleSheets_Universal") continue
          if (s.settingKey.uppercase() in delPresetSet) continue
          // Filter internal password update sync state and duplicate legacy template keys
          if (s.settingKey.startsWith("RM_PASS_UPDATED_AT_")) continue
          if (s.settingKey == "CHECKLIST_HEADER_TEMPLATE" && hasHdrKey) continue
          if (s.settingKey == "CHECKLIST_REGARDS_TEMPLATE" && hasFtrKey) continue
          // If a clean preset name exists, omit the uppercase underscore legacy alias
          if (s.settingKey.startsWith("CHECKLIST_PRESET_")) {
            val friendly = s.settingKey.removePrefix("CHECKLIST_PRESET_").replace("_", " ")
            if (allSettings.any { it.settingKey.equals(friendly, ignoreCase = true) }) continue
          }

          settingsArray.put(JSONObject().apply {
            put("settingKey", s.settingKey)
            put("settingValue", s.settingValue)
            put("description", "Universal Setting - Auto-synced across all mobile apps")
            put("updatedBy", s.updatedBy)
            put("updatedAt", DateUtils.formatDateTime(s.updatedAt))
          })
        }

        // Guarantee Checklist Header row exists
        if (!hasHdrKey) {
          val hdr = allSettings.find { it.settingKey == "CHECKLIST_HEADER_TEMPLATE" }?.settingValue
            ?: "Dear {CUSTOMER_NAME},\nGreetings from Eastern Bank PLC (EBL).\nTo process your application for {PRESET_NAME}, please provide the following required documents:"
          settingsArray.put(JSONObject().apply {
            put("settingKey", "Checklist Header")
            put("settingValue", hdr)
            put("description", "Universal Checklist Header Greeting")
            put("updatedBy", "App")
            put("updatedAt", DateUtils.formatDateTime(DateUtils.currentDhakaMillis()))
          })
        }
        // Guarantee Checklist Footer row exists
        if (!hasFtrKey) {
          val ftr = allSettings.find { it.settingKey == "CHECKLIST_REGARDS_TEMPLATE" }?.settingValue
            ?: "For any query or assistance, please contact:\n{RM_NAME}\nRM Code: {RM_CODE}\nMobile: {RM_PHONE}\nEastern Bank PLC"
          settingsArray.put(JSONObject().apply {
            put("settingKey", "Checklist Footer")
            put("settingValue", ftr)
            put("description", "Universal Checklist Footer / Regards")
            put("updatedBy", "App")
            put("updatedAt", DateUtils.formatDateTime(DateUtils.currentDhakaMillis()))
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
          val aObj = JSONObject().apply {
            put("attachmentId", att.attachmentId)
            put("fileId", att.fileId)
            put("fileName", att.fileName)
            put("category", att.category)
            put("fileSize", att.fileSizeBytes)
            put("uploadedBy", att.uploadedBy)
            put("uploadedAt", DateUtils.formatDateTime(att.uploadedAt))
            put("fileUri", att.fileUri)
            put("storagePath", att.storagePath)
          }
          // If attachment file is not yet saved to Google Drive, upload base64 via Apps Script connector
          if (!att.fileUri.startsWith("http")) {
            val f = if (att.storagePath.isNotBlank()) java.io.File(att.storagePath) else null
            if (f != null && f.exists() && f.length() > 0 && f.length() < 12 * 1024 * 1024) {
              val b64 = com.example.util.AttachmentHelper.fileToBase64(f)
              if (b64 != null) {
                aObj.put("fileBase64", b64)
                aObj.put("fileType", att.fileType)
              }
            }
          }
          attachmentsArray.put(aObj)
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

        val currentUserCode = authRepository.currentUser.value?.rmCode ?: ""
        val now = DateUtils.currentDhakaMillis()
        val freshSms = database.smsNotificationDao().getAllSms().filter { s ->
          (now - s.sentTimestamp) < 15_000L && s.triggeredByCode == currentUserCode
        }
        val smsArray = org.json.JSONArray()
        for (s in freshSms) {
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

        // Gather all deleted event IDs so Google Sheets deletes them from Team_Events sheet
        val delEventsSetting = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
        val deletedEventIds = org.json.JSONArray()
        delEventsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedEventIds.put(it)
        }

        // Gather all deleted SMS IDs so Google Sheets deletes them from SMS_Notifications sheet
        val delSmsSetting = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
        val deletedSmsIds = org.json.JSONArray()
        delSmsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedSmsIds.put(it)
        }

        // Gather all deleted Important Document IDs so Google Sheets deletes them from Important_Documents sheet
        val delDocsSetting = database.appSettingDao().getSetting("deleted_doc_ids")?.settingValue ?: ""
        val deletedDocIds = org.json.JSONArray()
        delDocsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedDocIds.put(it)
        }

        // Gather all deleted Chat IDs so Google Sheets deletes them from Chat_And_Calls sheet
        val delChatsSetting = database.appSettingDao().getSetting("deleted_chat_ids")?.settingValue ?: ""
        val deletedChatIds = org.json.JSONArray()
        delChatsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedChatIds.put(it)
        }

        // Gather all deleted preset keys so Google Sheets deletes them from Universal_Settings
        val deletedPresetKeys = org.json.JSONArray()
        delPresetsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
          deletedPresetKeys.put(it)
        }

        // Gather all deleted attachment IDs so Google Sheets removes them from Attachments sheet
        val delAttsSetting = database.appSettingDao().getSetting("deleted_attachment_ids")?.settingValue ?: ""
        val deletedAttachmentIds = org.json.JSONArray()
        (delAttsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() } + deletedAttachmentIdsCache).distinct().forEach {
          deletedAttachmentIds.put(it)
        }

        // Gather all Important Documents so they are synced to Google Sheets for all RMs to view!
        val allImportantDocs = database.importantDocumentDao().getAllActiveDocuments()
        val docsArray = org.json.JSONArray()
        for (doc in allImportantDocs) {
          val dObj = JSONObject().apply {
            put("docId", doc.docId)
            put("title", doc.title)
            put("category", doc.category)
            put("description", doc.description)
            put("fileName", doc.fileName)
            put("fileType", doc.fileType)
            put("fileSizeBytes", doc.fileSizeBytes)
            put("fileUri", doc.fileUri)
            put("storagePath", doc.storagePath)
            put("uploadedBy", doc.uploadedBy)
            put("uploaderName", doc.uploaderName)
            put("uploaderRole", doc.uploaderRole)
            put("createdAt", DateUtils.formatDateTime(doc.createdAt))
            put("updatedAt", DateUtils.formatDateTime(doc.updatedAt))
          }
          if (!doc.fileUri.startsWith("http")) {
            val f = if (doc.storagePath.isNotBlank()) java.io.File(doc.storagePath) else null
            if (f != null && f.exists() && f.length() > 0 && f.length() < 12 * 1024 * 1024) {
              val b64 = com.example.util.AttachmentHelper.fileToBase64(f)
              if (b64 != null) {
                dObj.put("fileBase64", b64)
                dObj.put("fileType", doc.fileType)
              }
            }
          }
          docsArray.put(dObj)
        }

        // Gather chat and communication messages
        val allMessages = database.communicationDao().getAllMessages()
        val chatMessagesArray = org.json.JSONArray()
        for (m in allMessages) {
          chatMessagesArray.put(JSONObject().apply {
            put("id", m.id)
            put("senderRmCode", m.senderRmCode)
            put("senderName", m.senderName)
            put("senderRole", m.senderRole)
            put("recipientRmCode", m.recipientRmCode ?: "")
            put("messageText", m.messageText)
            put("timestamp", DateUtils.formatDateTime(m.timestamp))
            put("messageType", m.messageType)
            put("eventId", m.eventId ?: "")
          })
        }

        // Gather Team Events
        val allEvents = database.communicationDao().getAllEventsList()
        val eventsArray = org.json.JSONArray()
        for (ev in allEvents) {
          eventsArray.put(JSONObject().apply {
            put("eventId", ev.eventId)
            put("title", ev.title)
            put("description", ev.description)
            put("creatorRmCode", ev.creatorRmCode)
            put("creatorName", ev.creatorName)
            put("targetDate", ev.targetDate)
            put("createdAt", DateUtils.formatDateTime(ev.createdAt))
            put("status", ev.status)
            put("allowedFields", ev.allowedFields)
            put("closeTime", ev.closeTime)
          })
        }

        // Gather Event Responses
        val allResponses = database.communicationDao().getAllResponsesList()
        val responsesArray = org.json.JSONArray()
        for (r in allResponses) {
          responsesArray.put(JSONObject().apply {
            put("responseId", r.responseId)
            put("eventId", r.eventId)
            put("rmCode", r.rmCode)
            put("rmName", r.rmName)
            put("filesCount", r.filesCount)
            put("requestedDate", r.requestedDate)
            put("location", r.location)
            put("remarks", r.remarks)
            put("customerEntriesJson", r.customerEntriesJson)
            put("submittedAt", DateUtils.formatDateTime(r.submittedAt))
          })
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
          put("deletedEventIds", deletedEventIds)
          put("deletedSmsIds", deletedSmsIds)
          put("deletedDocIds", deletedDocIds)
          put("deletedChatIds", deletedChatIds)
          put("deletedPresetKeys", deletedPresetKeys)
          put("rms", rmsArray)
          put("settings", settingsArray)
          put("auditLogs", auditLogsArray)
          put("attachments", attachmentsArray)
          put("sms", smsArray)
          put("locations", locationsArray)
          put("importantDocuments", docsArray)
          put("chatMessages", chatMessagesArray)
          put("events", eventsArray)
          put("eventResponses", responsesArray)
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
                val sheetDocs = respJson.optJSONArray("importantDocuments") ?: respJson.optJSONArray("documents")
                if (sheetDocs != null && sheetDocs.length() > 0) {
                  processSheetDocuments(sheetDocs)
                }
                val sheetChat = respJson.optJSONArray("chatMessages") ?: respJson.optJSONArray("messages")
                if (sheetChat != null && sheetChat.length() > 0) {
                  processSheetChatMessages(sheetChat)
                }
                val sheetEvents = respJson.optJSONArray("events") ?: respJson.optJSONArray("teamEvents")
                if (sheetEvents != null && sheetEvents.length() > 0) {
                  processSheetEvents(sheetEvents)
                }
                val sheetResponses = respJson.optJSONArray("eventResponses") ?: respJson.optJSONArray("responses")
                if (sheetResponses != null && sheetResponses.length() > 0) {
                  processSheetEventResponses(sheetResponses)
                }
                val sheetAtts = respJson.optJSONArray("attachments")
                if (sheetAtts != null && sheetAtts.length() > 0) {
                  processSheetAttachments(sheetAtts)
                }
                val sheetSms = respJson.optJSONArray("smsNotifications") ?: respJson.optJSONArray("sms")
                if (sheetSms != null && sheetSms.length() > 0) {
                  processSheetSmsNotifications(sheetSms)
                }
                val sheetLocations = respJson.optJSONArray("locations") ?: respJson.optJSONArray("locationLogs")
                if (sheetLocations != null && sheetLocations.length() > 0) {
                  processSheetLocations(sheetLocations)
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
      // Process settings FIRST so all deletion tombstones (deleted_doc_ids, deleted_event_ids, deleted_file_ids, deleted_sms_ids, deleted_preset_keys)
      // are applied to the local DB and cache BEFORE processing files, docs, events, and SMS notifications!
      val settingsArray = json.optJSONArray("settings") ?: JSONArray()
      val updatedSettingsCount = processSheetSettings(settingsArray)

      val filesArray = json.optJSONArray("files") ?: json.optJSONArray("latestFiles") ?: JSONArray()
      val updatedFilesCount = processSheetFiles(filesArray)

      val rmsArray = json.optJSONArray("rms") ?: JSONArray()
      val updatedRmsCount = processSheetRms(rmsArray)

      val docsArray = json.optJSONArray("importantDocuments") ?: json.optJSONArray("documents") ?: JSONArray()
      val updatedDocsCount = processSheetDocuments(docsArray)

      val chatArray = json.optJSONArray("chatMessages") ?: json.optJSONArray("messages") ?: JSONArray()
      processSheetChatMessages(chatArray)

      val eventsArray = json.optJSONArray("events") ?: json.optJSONArray("teamEvents") ?: JSONArray()
      processSheetEvents(eventsArray)

      val responsesArray = json.optJSONArray("eventResponses") ?: json.optJSONArray("responses") ?: JSONArray()
      processSheetEventResponses(responsesArray)

      val attachmentsArray = json.optJSONArray("attachments") ?: JSONArray()
      val updatedAttsCount = processSheetAttachments(attachmentsArray)

      val smsArray = json.optJSONArray("smsNotifications") ?: json.optJSONArray("sms") ?: JSONArray()
      val updatedSmsCount = processSheetSmsNotifications(smsArray)

      val now = DateUtils.currentDhakaMillis()
      val msg = "Pulled from Google Sheets: $updatedFilesCount file(s), $updatedRmsCount RM(s), $updatedSettingsCount setting(s), $updatedDocsCount document(s), $updatedAttsCount attachment(s), $updatedSmsCount notification(s) updated."
      database.appSettingDao().insertOrUpdateSyncStatus(
        currentStatus.copy(
          appsScriptUrl = activeWebAppUrl,
          lastSyncTimestamp = now,
          lastSyncStatus = "SUCCESS",
          lastSyncMessage = msg
        )
      )
      database.appSettingDao().insertOrUpdateSetting(
        com.example.data.model.AppSettingEntity(
          settingKey = "is_initial_sync_done",
          settingValue = "true",
          updatedBy = "SYSTEM",
          updatedAt = now
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

    // 2. Enforce persistent tombstone deletion ONLY for RMs explicitly deleted by Admin or Mentor
    val delRmsSetting = database.appSettingDao().getSetting("deleted_rm_codes")?.settingValue ?: ""
    val deletedCodes = (delRmsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() } + deletedRmCodesCache).toSet()

    for (delCode in deletedCodes) {
      if (delCode != "104393" && delCode != "ADMIN" && delCode != "MENTOR") {
        database.userDao().deleteUser(delCode)
        database.rmTargetDao().deleteTargetForRm(delCode)
      }
    }

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
        val isRecentLocalPassChange = (now - lastLocalPassChangeTime) < 300_000L // 5 minutes immunity window

        val hashToUse = if (isRecentLocalPassChange) existing.passwordHash
                        else if (sheetHash.isNotBlank() && sheetSalt.isNotBlank()) sheetHash
                        else if (sheetHash.isNotBlank()) SecurityUtils.hashPassword(sheetHash, existing.salt.ifBlank { "salt_$code" })
                        else existing.passwordHash

        val saltToUse = if (isRecentLocalPassChange) existing.salt
                        else if (sheetSalt.isNotBlank()) sheetSalt
                        else existing.salt

        val sheetLat = if (obj.has("lastLatitude")) obj.optDouble("lastLatitude", 0.0) else obj.optDouble("latitude", 0.0)
        val sheetLng = if (obj.has("lastLongitude")) obj.optDouble("lastLongitude", 0.0) else obj.optDouble("longitude", 0.0)
        val sheetAddress = obj.optString("lastLocationAddress", obj.optString("address", "")).trim()
        val sheetLocTimeRaw = obj.optString("lastLocationTime", obj.optString("locationTime", "")).trim()
        val sheetLocTime = if (sheetLocTimeRaw.isNotBlank()) DateUtils.parseDateTime(sheetLocTimeRaw) else null
        val sheetIsOnline = obj.optBoolean("isOnline", false)

        val hasLocationUpdate = (sheetLat != 0.0 && sheetLat != existing.lastLatitude) ||
                                (sheetLng != 0.0 && sheetLng != existing.lastLongitude) ||
                                (sheetAddress.isNotBlank() && sheetAddress != existing.lastLocationAddress) ||
                                (sheetLocTime != null && sheetLocTime != existing.lastLocationTime)

        val hasChanges = existing.name != name || existing.mobile != mobile ||
                         existing.email != email || existing.officeAddress != office ||
                         existing.accountStatus != status || hasLocationUpdate ||
                         (obj.has("isOnline") && existing.isOnline != sheetIsOnline) ||
                         (!isRecentLocalPassChange && sheetHash.isNotBlank() && existing.passwordHash != hashToUse)
        if (hasChanges) {
          database.userDao().updateUser(
            existing.copy(
              name = name,
              mobile = mobile,
              email = email,
              officeAddress = office,
              accountStatus = status,
              passwordHash = hashToUse,
              salt = saltToUse,
              lastLatitude = if (sheetLat != 0.0) sheetLat else existing.lastLatitude,
              lastLongitude = if (sheetLng != 0.0) sheetLng else existing.lastLongitude,
              lastLocationAddress = if (sheetAddress.isNotBlank()) sheetAddress else existing.lastLocationAddress,
              lastLocationTime = sheetLocTime ?: existing.lastLocationTime,
              isOnline = if (obj.has("isOnline")) sheetIsOnline else existing.isOnline
            )
          )
          updatedCount++
        }
      } else {
        val sheetLat = if (obj.has("lastLatitude")) obj.optDouble("lastLatitude", 0.0) else obj.optDouble("latitude", 0.0)
        val sheetLng = if (obj.has("lastLongitude")) obj.optDouble("lastLongitude", 0.0) else obj.optDouble("longitude", 0.0)
        val sheetAddress = obj.optString("lastLocationAddress", obj.optString("address", "")).trim()
        val sheetLocTimeRaw = obj.optString("lastLocationTime", obj.optString("locationTime", "")).trim()
        val sheetLocTime = if (sheetLocTimeRaw.isNotBlank()) DateUtils.parseDateTime(sheetLocTimeRaw) else null
        val sheetIsOnline = obj.optBoolean("isOnline", false)

        val (finalHash, finalSalt) = if (sheetHash.isNotBlank() && sheetSalt.isNotBlank()) {
          Pair(sheetHash, sheetSalt)
        } else if (sheetHash.isNotBlank()) {
          // If password was entered directly in plain text in Google Sheet
          val salt = SecurityUtils.generateSalt()
          Pair(SecurityUtils.hashPassword(sheetHash, salt), salt)
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
          authUid = "AUTH_RM_$code",
          lastLatitude = if (sheetLat != 0.0) sheetLat else null,
          lastLongitude = if (sheetLng != 0.0) sheetLng else null,
          lastLocationAddress = if (sheetAddress.isNotBlank()) sheetAddress else null,
          lastLocationTime = sheetLocTime,
          isOnline = sheetIsOnline
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
      // If local setting was edited within last 30s by a user, protect it from stale sheet overwrites
      val wasRecentlyEditedLocally = existing != null && (now - existing.updatedAt < 30_000L) && (existing.updatedBy != "GoogleSheets_Universal")
      if (wasRecentlyEditedLocally && !key.startsWith("deleted_")) {
        continue
      }

      if (key.equals("deleted_file_ids", true)) {
        val delList = value.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedFileIdsCache).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_file_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedFileIdsCache.addAll(merged)
        for (delId in merged) {
          val f = database.customerFileDao().getFileById(delId) ?: database.customerFileDao().getFileByAnyId(delId)
          if (f != null && !f.isDeleted) {
            database.customerFileDao().softDeleteFile(f.fileId, "GoogleSheets_Universal_Sync", now)
          }
        }
      } else if (key.equals("deleted_doc_ids", true)) {
        val delList = value.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedDocIdsCache.map { it.uppercase() }).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_doc_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedDocIdsCache.addAll(merged)
        for (docId in merged) {
          val d = database.importantDocumentDao().getDocumentById(docId)
          if (d != null) {
            database.importantDocumentDao().softDeleteDocument(d.docId, now)
            database.importantDocumentDao().permanentDeleteDocument(d.docId)
          }
        }
      } else if (key.equals("deleted_event_ids", true)) {
        val delList = value.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedEventIdsCache.map { it.uppercase() }).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_event_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedEventIdsCache.addAll(merged)
        for (evtId in merged) {
          database.communicationDao().deleteEvent(evtId)
          database.communicationDao().deleteResponsesForEvent(evtId)
          database.communicationDao().deleteMessagesByEventId(evtId)
        }
      } else if (key.equals("deleted_chat_ids", true)) {
        val delList = value.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedChatIdsCache.map { it.uppercase() }).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_chat_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedChatIdsCache.addAll(merged)
        for (chatId in merged) {
          database.communicationDao().deleteMessage(chatId)
        }
      } else if (key.equals("deleted_sms_ids", true)) {
        val delList = value.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedSmsIdsCache).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_sms_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedSmsIdsCache.addAll(merged)
        for (sId in merged) {
          sId.toLongOrNull()?.let { database.smsNotificationDao().deleteSms(it) }
        }
      } else if (key.equals("deleted_rm_codes", true)) {
        val delList = value.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedRmCodesCache).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_rm_codes", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedRmCodesCache.addAll(merged)
        for (rm in merged) {
          database.userDao().deleteUser(rm)
          database.rmTargetDao().deleteTargetForRm(rm)
        }
      } else if (key.equals("deleted_preset_keys", true)) {
        val delList = value.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedPresetKeysCache.map { it.uppercase() }).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_preset_keys", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedPresetKeysCache.addAll(merged)
        for (pKey in merged) {
          database.appSettingDao().deleteSetting(pKey)
          val friendly = pKey.removePrefix("CHECKLIST_CUSTOM_").replace("_", " ")
          database.appSettingDao().deleteSetting(friendly)
        }
      } else if (key.equals("deleted_attachment_ids", true)) {
        val delList = value.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val currentDel = existing?.settingValue ?: ""
        val currentList = currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val merged = (currentList + delList + deletedAttachmentIdsCache).distinct()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity("deleted_attachment_ids", merged.joinToString(","), "GoogleSheets_Universal", now)
        )
        deletedAttachmentIdsCache.addAll(merged)
        deletedAttachmentIdsCache.addAll(merged.map { it.uppercase() })
        for (attId in merged) {
          database.fileAttachmentDao().deleteAttachment(attId)
          database.fileAttachmentDao().deleteAttachment(attId.uppercase())
          database.fileAttachmentDao().deleteAttachment(attId.lowercase())
        }
      } else {
        val delPresetsSetting = database.appSettingDao().getSetting("deleted_preset_keys")?.settingValue ?: ""
        val delPresetSet = (delPresetsSetting.split(",").map { it.trim().uppercase() } + deletedPresetKeysCache.map { it.uppercase() }).toSet()
        if (key.uppercase() in delPresetSet || key.removePrefix("CHECKLIST_CUSTOM_").replace("_", " ").uppercase() in delPresetSet) {
          continue
        }

        if (key.equals("APP_ACTIVE_THEME", true) ||
            key.equals("UI_FONT_STYLE", true) ||
            key.equals("UI_FONT_COLOR", true) ||
            key.equals("UI_TEXT_SCALE", true)) {
          val prefKey = if (key.equals("APP_ACTIVE_THEME", true)) "app_active_theme" else key.lowercase()
          val localPrefVal = try {
            context?.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)?.getString(prefKey, null)
          } catch (_: Exception) { null }
          if (!localPrefVal.isNullOrBlank() && !localPrefVal.equals(value, ignoreCase = true)) {
            // Local user selected a value! Push it back to sheet and do not overwrite locally with stale remote data!
            applicationScope.launch {
              syncSettingUpsertToGoogleSheets(key, localPrefVal)
            }
            continue
          }
        }

        if (existing == null || existing.settingValue != value) {
          database.appSettingDao().insertOrUpdateSetting(
            AppSettingEntity(
              settingKey = key,
              settingValue = value,
              updatedBy = "GoogleSheets_Universal",
              updatedAt = now
            )
          )

          // Keep Checklist Header & Footer synchronized between friendly and legacy keys
          if (key.equals("Checklist Header", true)) {
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("CHECKLIST_HEADER_TEMPLATE", value, "GoogleSheets_Universal", now)
            )
          } else if (key.equals("CHECKLIST_HEADER_TEMPLATE", true)) {
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("Checklist Header", value, "GoogleSheets_Universal", now)
            )
          } else if (key.equals("Checklist Footer", true)) {
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("CHECKLIST_REGARDS_TEMPLATE", value, "GoogleSheets_Universal", now)
            )
          } else if (key.equals("CHECKLIST_REGARDS_TEMPLATE", true)) {
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("Checklist Footer", value, "GoogleSheets_Universal", now)
            )
          } else if (key.equals("App Name", true) || key.equals("app_name", true) || key.equals("Application Name", true) || key.equals("app_custom_name", true)) {
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("app_custom_name", value, "GoogleSheets_Universal", now)
            )
          } else if (key.startsWith("CHECKLIST_PRESET_") || key.startsWith("CHECKLIST_CUSTOM_")) {
            if (key != "CHECKLIST_CUSTOM_PRESETS_LIST" && key != "CHECKLIST_PRESETS_VISIBILITY" &&
                key != "CHECKLIST_CORP_COMPANY_DOCS" && key != "CHECKLIST_CORP_EMPLOYEE_DOCS" &&
                key != "CHECKLIST_SALARIED_DOCS" && key != "CHECKLIST_BUSINESS_DOCS" &&
                key != "CHECKLIST_LOAN_DOCS" && key != "CHECKLIST_ENHANCE_DOCS" &&
                key != "CHECKLIST_HEADER_TEMPLATE" && key != "CHECKLIST_REGARDS_TEMPLATE") {
              val friendly = key.removePrefix("CHECKLIST_CUSTOM_").removePrefix("CHECKLIST_PRESET_").replace("_", " ")
              if (friendly.uppercase() !in delPresetSet && key.uppercase() !in delPresetSet) {
                val existingCustom = database.appSettingDao().getSetting("CHECKLIST_CUSTOM_PRESETS_LIST")?.settingValue ?: ""
                val customList = try {
                  val arr = org.json.JSONArray(existingCustom)
                  (0 until arr.length()).map { arr.getString(it).trim() }
                } catch (_: Exception) {
                  existingCustom.split(",").map { it.trim() }
                }.filter { it.isNotBlank() }
                if (friendly !in customList) {
                  val updatedList = (customList + friendly).distinct()
                  database.appSettingDao().insertOrUpdateSetting(
                    AppSettingEntity("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(updatedList).toString(), "GoogleSheets_Universal", now)
                  )
                }
              }
            }
          }
          updatedCount++
        }
      }
    }
    return updatedCount
  }

  private suspend fun processSheetAttachments(sheetAttsJson: JSONArray): Int {
    var count = 0
    val delAttsSetting = database.appSettingDao().getSetting("deleted_attachment_ids")?.settingValue ?: ""
    val deletedAttIds = (delAttsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() } + deletedAttachmentIdsCache.map { it.uppercase() }).toSet()
    for (delId in deletedAttIds) {
      database.fileAttachmentDao().deleteAttachment(delId)
      database.fileAttachmentDao().deleteAttachment(delId.lowercase())
    }

    for (i in 0 until sheetAttsJson.length()) {
      val obj = sheetAttsJson.optJSONObject(i) ?: continue
      val attId = obj.optString("attachmentId").trim()
      val fileId = obj.optString("fileId").trim()
      if (attId.isBlank() || fileId.isBlank() || attId.uppercase() in deletedAttIds || attId in deletedAttachmentIdsCache) {
        if (attId.isNotBlank()) {
          database.fileAttachmentDao().deleteAttachment(attId)
          database.fileAttachmentDao().deleteAttachment(attId.uppercase())
        }
        continue
      }

      val fileName = obj.optString("fileName", "photo.jpg").trim()
      val category = obj.optString("category", "General").trim()
      val fileSize = obj.optLong("fileSize", obj.optLong("fileSizeBytes", 1024L))
      val uploadedBy = obj.optString("uploadedBy", "RM").trim()
      val uploadedAt = DateUtils.parseDateTime(obj.optString("uploadedAt")) ?: DateUtils.currentDhakaMillis()
      val fileUri = obj.optString("fileUri", obj.optString("driveLink", "")).trim()
      val storagePath = obj.optString("storagePath", fileUri).trim()

      val existing = database.fileAttachmentDao().getAttachmentById(attId)
      if (existing == null) {
        val entity = FileAttachmentEntity(
          attachmentId = attId,
          fileId = fileId,
          category = category,
          fileName = fileName,
          fileType = if (fileName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg",
          storagePath = if (storagePath.isNotBlank()) storagePath else fileUri,
          fileUri = fileUri,
          fileSizeBytes = fileSize,
          uploadedBy = uploadedBy,
          uploadedAt = uploadedAt
        )
        database.fileAttachmentDao().insertAttachment(entity)
        count++
      } else if (fileUri.startsWith("http") && !existing.fileUri.startsWith("http")) {
        database.fileAttachmentDao().insertAttachment(
          existing.copy(
            fileUri = fileUri,
            storagePath = if (storagePath.isNotBlank()) storagePath else fileUri
          )
        )
        count++
      }
    }
    return count
  }

  private suspend fun processSheetDocuments(sheetDocsJson: JSONArray): Int {
    var count = 0
    val now = DateUtils.currentDhakaMillis()
    val delDocsSetting = database.appSettingDao().getSetting("deleted_doc_ids")?.settingValue ?: ""
    val deletedDocIds = (delDocsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() } + deletedDocIdsCache.map { it.uppercase() }).toSet()

    // 1. Immediately delete any local documents in deletedDocIds
    for (delId in deletedDocIds) {
      val d = database.importantDocumentDao().getDocumentById(delId)
      if (d != null) {
        database.importantDocumentDao().softDeleteDocument(d.docId, now)
        database.importantDocumentDao().permanentDeleteDocument(d.docId)
      }
    }

    // 2. Gather active sheet doc IDs
    val activeSheetDocIds = HashSet<String>()
    for (i in 0 until sheetDocsJson.length()) {
      val obj = sheetDocsJson.optJSONObject(i) ?: continue
      val did = obj.optString("docId").trim().uppercase()
      if (did.isNotBlank() && did !in deletedDocIds && did !in deletedDocIdsCache) activeSheetDocIds.add(did)
    }

    // 3. Reconcile with local database: If sheet returned active documents, any local document marked isSynced that is no longer in sheetDocsJson should be removed!
    if (sheetDocsJson.length() > 0) {
      val localDocs = database.importantDocumentDao().getAllActiveDocuments()
      for (ld in localDocs) {
        if (ld.isSynced && ld.docId.trim().uppercase() !in activeSheetDocIds) {
          database.importantDocumentDao().softDeleteDocument(ld.docId, now)
          database.importantDocumentDao().permanentDeleteDocument(ld.docId)
        }
      }
    }

    // 4. Upsert valid active documents from Google Sheets
    for (i in 0 until sheetDocsJson.length()) {
      val obj = sheetDocsJson.optJSONObject(i) ?: continue
      val docId = obj.optString("docId").trim()
      if (docId.isBlank() || docId.uppercase() in deletedDocIds || docId in deletedDocIdsCache || docId.lowercase() in deletedDocIdsCache) continue

      val existingDoc = database.importantDocumentDao().getDocumentById(docId)
      if (existingDoc != null && existingDoc.isDeleted) continue

      val title = obj.optString("title", "Document").trim()
      val category = obj.optString("category", "Policies & Circulars").trim()
      val description = obj.optString("description", "").trim()
      val fileName = obj.optString("fileName", "$title.pdf").trim()
      val fileType = obj.optString("fileType", "application/pdf").trim()
      val fileSizeBytes = obj.optLong("fileSizeBytes", 1024L)
      val fileUri = obj.optString("fileUri", "").trim().ifBlank { existingDoc?.fileUri ?: "" }
      val storagePath = obj.optString("storagePath", "").trim().ifBlank { existingDoc?.storagePath ?: "" }
      val uploadedBy = obj.optString("uploadedBy", "ADMIN").trim()
      val uploaderName = obj.optString("uploaderName", "Bank Official").trim()
      val uploaderRole = obj.optString("uploaderRole", "ADMIN").trim()
      val createdAt = DateUtils.parseDateTime(obj.optString("createdAt")) ?: (existingDoc?.createdAt ?: now)
      val updatedAt = DateUtils.parseDateTime(obj.optString("updatedAt")) ?: now

      val entity = com.example.data.model.ImportantDocumentEntity(
        docId = docId,
        title = title,
        category = category,
        description = description,
        fileName = fileName,
        fileType = fileType,
        fileSizeBytes = fileSizeBytes,
        fileUri = fileUri,
        storagePath = storagePath,
        uploadedBy = uploadedBy,
        uploaderName = uploaderName,
        uploaderRole = uploaderRole,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = false,
        isSynced = true
      )
      database.importantDocumentDao().insertDocument(entity)
      count++
    }
    return count
  }

  private suspend fun processSheetSmsNotifications(sheetSmsJson: JSONArray): Int {
    var count = 0
    val currentUser = authRepository.currentUser.value
    val userRmCode = currentUser?.rmCode?.trim()?.uppercase() ?: ""
    val isPrivileged = currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR"
    val now = DateUtils.currentDhakaMillis()

    val delSmsSetting = database.appSettingDao().getSetting("deleted_sms_ids")?.settingValue ?: ""
    val deletedSmsIdSet = (delSmsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() } + deletedSmsIdsCache).toSet()
    val lastClearedTimeStr = database.appSettingDao().getSetting("last_cleared_sms_timestamp_$userRmCode")?.settingValue
      ?: database.appSettingDao().getSetting("last_cleared_sms_timestamp")?.settingValue ?: "0"
    val lastClearedTime = lastClearedTimeStr.toLongOrNull() ?: 0L

    val notifPrefs = context?.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    val permDeletedKeys = (notifPrefs?.getStringSet("permanently_deleted_sms_keys", emptySet()) ?: emptySet()) + deletedSmsIdSet
    val seenSmsKeys = (notifPrefs?.getStringSet("seen_sms_signatures", emptySet()) ?: emptySet()).toMutableSet()

    for (i in 0 until sheetSmsJson.length()) {
      val obj = sheetSmsJson.optJSONObject(i) ?: continue
      val smsId = obj.optString("id", "").trim()
      val recipientRm = obj.optString("recipientRmCode").trim().uppercase()
      val messageText = obj.optString("messageText", "").trim()
      val rawSentTime = obj.optString("sentTimestamp", "").trim()
      val sentTime = DateUtils.parseDateTime(rawSentTime) ?: 0L
      val targetId = obj.optString("targetId", "").trim()

      val dedupeSignature = "${recipientRm}_${targetId}_${messageText.take(25).hashCode()}"

      if (smsId.isNotBlank() && smsId in permDeletedKeys) continue
      if (dedupeSignature in permDeletedKeys || rawSentTime in permDeletedKeys) continue
      if (smsId.isNotBlank() && smsId in seenSmsKeys) continue
      if (dedupeSignature in seenSmsKeys) continue

      if (recipientRm.isBlank() || messageText.isBlank()) continue

      // If user previously cleared SMS at or after this time, do NOT resurrect or notify
      if (lastClearedTime > 0L && sentTime > 0L && sentTime <= lastClearedTime) {
        continue
      }

      // Only store/notify if it belongs to current user or user is privileged admin/mentor
      if (recipientRm != userRmCode && !isPrivileged && recipientRm != "ALL") {
        continue
      }

      // Check if duplicate in local database
      val matchCount = if (sentTime > 0L) database.smsNotificationDao().countMatchingSms(recipientRm, sentTime, messageText) else 0
      val recipientName = obj.optString("recipientName", "").trim()
      val recipientMobile = obj.optString("recipientMobile", "").trim()
      val triggeredRole = obj.optString("triggeredByRole", "SYSTEM").trim()
      val triggeredCode = obj.optString("triggeredByCode", "").trim()
      val actionType = obj.optString("actionType", "UPDATE").trim()
      val status = obj.optString("status", "DELIVERED").trim()

      if (matchCount == 0) {
        val entity = SmsNotificationEntity(
          recipientRmCode = recipientRm,
          recipientMobile = recipientMobile,
          recipientName = recipientName,
          triggeredByRole = triggeredRole,
          triggeredByCode = triggeredCode,
          actionType = actionType,
          targetType = "SYNC_ALERT",
          fileId = targetId.ifBlank { null },
          customerName = obj.optString("customerName").ifBlank { null },
          messageText = messageText,
          sentTimestamp = if (sentTime > 0L) sentTime else now,
          status = status,
          isRead = false
        )
        database.smsNotificationDao().insertSms(entity)
        count++

        if (smsId.isNotBlank()) seenSmsKeys.add(smsId)
        seenSmsKeys.add(dedupeSignature)
        if (rawSentTime.isNotBlank()) seenSmsKeys.add(rawSentTime)
      }

      // DEVICE NOTIFICATION RULES:
      // 1. Initial sync seeds historical records as shown so old items don't buzz the phone.
      // 2. Any SMS arriving within 24 hours that hasn't been shown alerts the device with high priority.
      // 3. Delivers reliably even when the app was closed or off.
      val isInitialSyncDone = database.appSettingDao().getSetting("is_initial_sync_done")?.settingValue == "true"
      val isRecent = sentTime <= 0L || (now - sentTime) < 24 * 60 * 60 * 1000L
      val notifKey = "sms_notif_${smsId.ifBlank { "${recipientRm}_${rawSentTime}_${actionType}_${messageText.take(20).hashCode()}" }}"

      val shouldNotifyAdmin = isPrivileged && (actionType == "NEW_FILE" || recipientRm == userRmCode || recipientRm == "ADMIN" || recipientRm == "MENTOR" || recipientRm == "ALL") && triggeredCode != userRmCode && isRecent
      val shouldNotifyRm = !isPrivileged && (recipientRm == userRmCode || recipientRm == "ALL") && triggeredCode != userRmCode && isRecent

      if ((shouldNotifyAdmin || shouldNotifyRm) && isRecent) {
        context?.let { ctx ->
          if (!isInitialSyncDone) {
            NotificationHelper.markNotificationShown(ctx, notifKey)
          } else if (!NotificationHelper.hasNotificationBeenShown(ctx, notifKey)) {
            NotificationHelper.markNotificationShown(ctx, notifKey)
            if (shouldNotifyAdmin) {
              if (actionType == "NEW_FILE") {
                val rmUser = database.userDao().getUser(triggeredCode)
                val rmDisplayName = rmUser?.name ?: recipientName.ifBlank { triggeredCode }
                NotificationHelper.sendAdminNewFileNotification(
                  context = ctx,
                  rmCode = triggeredCode,
                  rmName = rmDisplayName,
                  fileId = targetId,
                  customerName = obj.optString("customerName", "Customer File"),
                  productType = obj.optString("productType", "Loan/Card")
                )
              } else {
                NotificationHelper.sendIncomingSmsNotification(
                  context = ctx,
                  senderTitle = "$triggeredRole $triggeredCode",
                  messageText = messageText
                )
              }
            } else {
              // Recipient is RM
              if (actionType == "EVENT") {
                NotificationHelper.sendNewEventNotification(
                  context = ctx,
                  eventTitle = targetId.ifBlank { "Team Event" },
                  creatorName = triggeredRole,
                  targetDate = DateUtils.formatDateOnly(sentTime)
                )
              } else if (actionType == "TRANSFER" || actionType == "UPDATE") {
                NotificationHelper.sendRmFileUpdateNotification(
                  context = ctx,
                  targetRmCode = recipientRm,
                  ccNumber = targetId,
                  customerName = recipientName,
                  changeDetails = messageText,
                  updatedByRole = triggeredRole
                )
              } else {
                NotificationHelper.sendIncomingSmsNotification(
                  context = ctx,
                  senderTitle = "$triggeredRole $triggeredCode",
                  messageText = messageText
                )
              }
            }
          }
        }
      }
    }
    try {
      notifPrefs?.edit()?.putStringSet("seen_sms_signatures", seenSmsKeys.toList().takeLast(1000).toSet())?.apply()
    } catch (_: Exception) {}
    return count
  }

  private suspend fun processSheetLocations(sheetLocationsJson: JSONArray): Int {
    var count = 0
    val now = DateUtils.currentDhakaMillis()
    for (i in 0 until sheetLocationsJson.length()) {
      val obj = sheetLocationsJson.optJSONObject(i) ?: continue
      val rmCode = obj.optString("rmCode").trim().uppercase()
      if (rmCode.isBlank()) continue
      val userName = obj.optString("userName", rmCode).trim()
      val lat = obj.optDouble("latitude", 0.0)
      val lng = obj.optDouble("longitude", 0.0)
      val address = obj.optString("address", "").trim()
      val timeStr = obj.optString("timestamp").trim()
      val timestamp = if (timeStr.isNotBlank()) DateUtils.parseDateTime(timeStr) ?: now else now
      val source = obj.optString("sourceAction", "GPS").trim()

      if (lat != 0.0 || lng != 0.0 || address.isNotBlank()) {
        database.userLocationLogDao().insertLocationLog(
          com.example.data.model.UserLocationLogEntity(
            rmCode = rmCode,
            userName = userName,
            latitude = lat,
            longitude = lng,
            address = address,
            sourceAction = source,
            timestamp = timestamp
          )
        )
        val user = database.userDao().getUser(rmCode)
        if (user != null && (user.lastLocationTime == null || timestamp >= user.lastLocationTime)) {
          database.userDao().updateUserLocation(rmCode, lat, lng, address, timestamp)
        }
        count++
      }
    }
    return count
  }

  private suspend fun processSheetChatMessages(sheetChatJson: JSONArray): Int {
    var count = 0
    val now = DateUtils.currentDhakaMillis()
    val delEventsSetting = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
    val deletedEventIds = delEventsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
    val delChatsSetting = database.appSettingDao().getSetting("deleted_chat_ids")?.settingValue ?: ""
    val deletedChatIds = delChatsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()

    for (i in 0 until sheetChatJson.length()) {
      val obj = sheetChatJson.optJSONObject(i) ?: continue
      val id = obj.optString("id").trim()
      if (id.isBlank() || id in deletedChatIds || id.uppercase() in deletedChatIds) continue
      val eventId = obj.optString("eventId").trim().ifBlank { null }
      if (eventId != null && (eventId in deletedEventIds || eventId.uppercase() in deletedEventIds || "EVENT_$eventId" in deletedChatIds || "EVENT_${eventId.uppercase()}" in deletedChatIds)) continue

      val senderRmCode = obj.optString("senderRmCode").trim()
      val senderName = obj.optString("senderName", "Officer").trim()
      val senderRole = obj.optString("senderRole", "RM").trim()
      val recipientRmCode = obj.optString("recipientRmCode").trim().ifBlank { null }
      val messageText = obj.optString("messageText").trim()
      val timestamp = DateUtils.parseDateTime(obj.optString("timestamp")) ?: now
      val messageType = obj.optString("messageType", "TEXT").trim()

      if (messageType == "EVENT" && deletedEventIds.any { dEvt -> messageText.contains(dEvt, ignoreCase = true) }) continue

      val entity = com.example.data.model.ChatMessageEntity(
        id = id,
        senderRmCode = senderRmCode,
        senderName = senderName,
        senderRole = senderRole,
        recipientRmCode = recipientRmCode,
        messageText = messageText,
        timestamp = timestamp,
        messageType = messageType,
        eventId = eventId
      )
      database.communicationDao().insertMessage(entity)
      count++
    }
    return count
  }

  private suspend fun processSheetEvents(sheetEventsJson: JSONArray): Int {
    var count = 0
    val now = DateUtils.currentDhakaMillis()
    val delEventsSetting = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
    val deletedEventIds = (delEventsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() } + deletedEventIdsCache.map { it.uppercase() }).toSet()

    // 1. Immediately delete any local events that are in deletedEventIds
    for (delId in deletedEventIds) {
      database.communicationDao().deleteEvent(delId)
      database.communicationDao().deleteResponsesForEvent(delId)
      database.communicationDao().deleteMessagesByEventId(delId)
    }

    val currentUser = authRepository.currentUser.value
    val userRmCode = currentUser?.rmCode?.trim()?.uppercase() ?: ""

    for (i in 0 until sheetEventsJson.length()) {
      val obj = sheetEventsJson.optJSONObject(i) ?: continue
      val eventId = obj.optString("eventId").trim()
      if (eventId.isBlank() || eventId.uppercase() in deletedEventIds || eventId in deletedEventIdsCache || eventId.lowercase() in deletedEventIdsCache) continue
      val title = obj.optString("title").trim()
      val description = obj.optString("description").trim()
      val creatorRmCode = obj.optString("creatorRmCode").trim()
      val creatorName = obj.optString("creatorName").trim()
      val targetDate = obj.optString("targetDate").trim()
      val createdAt = DateUtils.parseDateTime(obj.optString("createdAt")) ?: now
      val status = obj.optString("status", "ACTIVE").trim()
      val allowedFields = obj.optString("allowedFields", "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS").trim()
      val closeTime = obj.optString("closeTime", "").trim()

      val existingEvent = database.communicationDao().getEventById(eventId)
      val entity = com.example.data.model.TeamEventEntity(
        eventId = eventId,
        title = title,
        description = description,
        creatorRmCode = creatorRmCode,
        creatorName = creatorName,
        targetDate = targetDate,
        createdAt = createdAt,
        status = status,
        allowedFields = allowedFields,
        closeTime = closeTime
      )
      database.communicationDao().insertEvent(entity)
      count++

      // If new event created recently and user is RM, trigger event notification!
      val isInitialSyncDone = database.appSettingDao().getSetting("is_initial_sync_done")?.settingValue == "true"
      val isEventRecent = (now - createdAt) < 24 * 60 * 60 * 1000L
      if (existingEvent == null && isEventRecent && creatorRmCode != userRmCode) {
        context?.let { ctx ->
          val notifKey = "event_$eventId"
          if (!isInitialSyncDone) {
            NotificationHelper.markNotificationShown(ctx, notifKey)
          } else if (!NotificationHelper.hasNotificationBeenShown(ctx, notifKey)) {
            NotificationHelper.markNotificationShown(ctx, notifKey)
            NotificationHelper.sendNewEventNotification(
              context = ctx,
              eventTitle = title,
              creatorName = creatorName.ifBlank { "Management" },
              targetDate = targetDate
            )
          }
        }
      }
    }
    return count
  }

  private suspend fun processSheetEventResponses(sheetResponsesJson: JSONArray): Int {
    var count = 0
    val now = DateUtils.currentDhakaMillis()
    val delEventsSetting = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
    val deletedEventIds = delEventsSetting.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()

    for (i in 0 until sheetResponsesJson.length()) {
      val obj = sheetResponsesJson.optJSONObject(i) ?: continue
      val respId = obj.optString("responseId").trim()
      if (respId.isBlank()) continue
      val eventId = obj.optString("eventId").trim()
      if (eventId.isBlank() || eventId.uppercase() in deletedEventIds) continue
      val rmCode = obj.optString("rmCode").trim()
      val rmName = obj.optString("rmName").trim()
      val filesCount = obj.optInt("filesCount", 1)
      val requestedDate = obj.optString("requestedDate").trim()
      val location = obj.optString("location").trim()
      val remarks = obj.optString("remarks").trim()
      val customerEntriesJson = obj.optString("customerEntriesJson").trim()
      val submittedAt = DateUtils.parseDateTime(obj.optString("submittedAt")) ?: now

      val entity = com.example.data.model.EventResponseEntity(
        responseId = respId,
        eventId = eventId,
        rmCode = rmCode,
        rmName = rmName,
        filesCount = filesCount,
        requestedDate = requestedDate,
        location = location,
        remarks = remarks,
        customerEntriesJson = customerEntriesJson,
        submittedAt = submittedAt
      )
      database.communicationDao().insertOrUpdateResponse(entity)
      count++
    }
    return count
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

    // 2. Protect local customer files & detect genuine deletions:
    // NEVER delete an unsynced file (!localFile.isSynced), as it was recently created/edited in-app and is waiting to sync.
    // NEVER delete a file created or updated recently.
    // A file missing from Google Sheet is ONLY considered deleted if:
    // - It was previously confirmed synced (isSynced == true)
    // - Created > 24 hours ago AND updated > 2 hours ago
    // - The sheet returned a healthy set of records (>= 3 rows) so network partial responses don't wipe local files
    if (sheetFilesJson.length() >= 3) {
      val allLocalActive = database.customerFileDao().getAllActiveFiles()
      for (localFile in allLocalActive) {
        // Strict protection for unsynced and recently modified local customer records!
        if (!localFile.isSynced) continue
        if (now - localFile.createdAt < 24 * 60 * 60 * 1000L) continue
        if (now - localFile.updatedAt < 2 * 60 * 60 * 1000L) continue

        val fMatch = localFile.fileId.trim().lowercase() in sheetIds
        val ccMatch = localFile.ccNumber.isNotBlank() && localFile.ccNumber.trim().lowercase() in sheetIds
        if (!fMatch && !ccMatch) {
          database.customerFileDao().softDeleteFile(localFile.fileId, "GoogleSheets_Delete", now)
          database.customerFileDao().markFileSynced(localFile.fileId)
          updatedCount++
        }
      }
    }

    // 3. Upsert / update files present in Google Sheets
    val delFilesSetting = database.appSettingDao().getSetting("deleted_file_ids")?.settingValue ?: ""
    val deletedIdsSet = (delFilesSetting.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() } + deletedFileIdsCache.map { it.lowercase() }).toSet()

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
      if (targetId.lowercase() in deletedIdsSet || (ccNumber.isNotBlank() && ccNumber.lowercase() in deletedIdsSet) || targetId.lowercase() in deletedFileIdsCache) {
        continue
      }
      val appStatus = obj.optString("applicationStatus", existing?.applicationStatus ?: "Submitted")
      val activeStatus = obj.optString("activeStatus", existing?.activeStatus ?: "Y")
      val isExplicitlyDeleted = obj.optBoolean("isDeleted", false) ||
                                appStatus.equals("DELETED", ignoreCase = true) ||
                                activeStatus.equals("DELETED", ignoreCase = true)
      if (isExplicitlyDeleted) {
        if (existing != null && !existing.isDeleted) {
          database.customerFileDao().softDeleteFile(existing.fileId, "GoogleSheets_Sync", now)
        }
        continue
      }
      val remarks = obj.optString("remarks", existing?.remarks ?: "")
      val pendingDocs = obj.optString("pendingDocuments", existing?.pendingDocuments ?: "")
      val rmCode = obj.optString("assignedRmCode", existing?.assignedRmCode ?: "104393").trim().uppercase()
      val custName = obj.optString("customerName", existing?.customerName ?: "Customer")

      if (existing != null) {
        val sheetUpdatedBy = obj.optString("updatedBy", "")
        val isManualSheetEdit = sheetUpdatedBy.equals("Manual_Sheet_Edit", ignoreCase = true)
        val wasRecentlyEditedLocally = (now - existing.updatedAt < 6_000) && (existing.updatedBy != "GoogleSheets_Sync")
        if (wasRecentlyEditedLocally && !isManualSheetEdit) {
          continue
        }

        val custName = obj.optString("customerName", existing.customerName)
        val compName = obj.optString("companyName", existing.companyName)
        val offAddr = obj.optString("officeAddress", existing.officeAddress)
        val mob = obj.optString("mobile", existing.mobile)
        val em = obj.optString("email", existing.email)
        val prodType = obj.optString("productType", existing.productType)
        val cpvStatus = obj.optString("cpvStatus", existing.cpvStatus)
        val cpvRemarks = if (obj.has("cpvRemarks")) obj.optString("cpvRemarks", existing.cpvRemarks) else existing.cpvRemarks
        val remarksVal = if (obj.has("remarks")) obj.optString("remarks", existing.remarks) else existing.remarks
        val sheetSl = obj.optString("serialNumber", obj.optString("sl", existing.serialNumber))

        val hasStatusChanged = !existing.applicationStatus.trim().equals(appStatus.trim(), ignoreCase = true)
        val isBothActive = (existing.activeStatus.equals("Y", true) || existing.activeStatus.equals("ACTIVE", true)) &&
                           (activeStatus.equals("Y", true) || activeStatus.equals("ACTIVE", true))
        val isBothInactive = (existing.activeStatus.equals("N", true) || existing.activeStatus.equals("INACTIVE", true)) &&
                             (activeStatus.equals("N", true) || activeStatus.equals("INACTIVE", true))
        val hasActiveChanged = !isBothActive && !isBothInactive && !existing.activeStatus.trim().equals(activeStatus.trim(), ignoreCase = true)
        val hasRemarksChanged = existing.remarks.trim() != remarksVal.trim() || existing.cpvRemarks.trim() != cpvRemarks.trim()
        val hasDocsChanged = existing.pendingDocuments.trim() != pendingDocs.trim()
        val hasCcChanged = ccNumber.isNotBlank() && existing.ccNumber.trim() != ccNumber.trim()
        val hasRmChanged = rmCode.isNotBlank() && !existing.assignedRmCode.trim().equals(rmCode.trim(), ignoreCase = true)
        val hasSlChanged = sheetSl.isNotBlank() && existing.serialNumber.trim() != sheetSl.trim()
        val hasDetailsChanged = existing.customerName.trim() != custName.trim() || existing.companyName.trim() != compName.trim() ||
                                existing.mobile.trim() != mob.trim() || existing.email.trim() != em.trim() || existing.officeAddress.trim() != offAddr.trim() ||
                                existing.productType.trim() != prodType.trim() || existing.cpvStatus.trim() != cpvStatus.trim()

        if (hasStatusChanged || hasActiveChanged || hasRemarksChanged || hasDocsChanged || hasCcChanged || hasRmChanged || hasDetailsChanged || hasSlChanged) {
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
            serialNumber = if (sheetSl.isNotBlank()) sheetSl else existing.serialNumber,
            updatedAt = now,
            updatedBy = "GoogleSheets_Sync",
            isSynced = true
          )
          database.customerFileDao().updateFile(updated)
          updatedCount++
        }
      } else {
        // New file row added in Google Sheets - prevent duplicate mobile numbers
        val sheetMobile = obj.optString("mobile", "").trim()
        val normSheetMobile = sheetMobile.replace(Regex("[^0-9]"), "").removePrefix("88").trimStart('0')
        if (normSheetMobile.length >= 6) {
          val isDuplicate = database.customerFileDao().getAllActiveFiles().any { f ->
            f.mobile.replace(Regex("[^0-9]"), "").removePrefix("88").trimStart('0') == normSheetMobile
          }
          if (isDuplicate) {
            // Skip inserting duplicate mobile number from sheet
            continue
          }
        }

        val sheetSl = obj.optString("serialNumber", obj.optString("sl", ""))
        val newFile = CustomerFileEntity(
          fileId = targetId,
          customerName = custName,
          companyName = obj.optString("companyName", "N/A"),
          officeAddress = obj.optString("officeAddress", ""),
          mobile = sheetMobile,
          altMobile = "",
          email = obj.optString("email", ""),
          productType = obj.optString("productType", "Credit Card"),
          applicationStatus = appStatus,
          activeStatus = activeStatus,
          assignedRmCode = rmCode,
          ccNumber = ccNumber,
          pendingDocuments = pendingDocs,
          remarks = remarks,
          cpvStatus = obj.optString("cpvStatus", "Pending"),
          serialNumber = sheetSl,
          createdAt = DateUtils.parseDateTime(obj.optString("createdAt")) ?: now,
          updatedAt = DateUtils.parseDateTime(obj.optString("updatedAt")) ?: now,
          createdBy = "GoogleSheets",
          updatedBy = "GoogleSheets_Sync",
          isSynced = true
        )
        database.customerFileDao().insertFile(newFile)
        updatedCount++

        // Requirement 2: When RM enters new customer file, Admin and Mentor receive SMS/notification even when app is closed
        val currentUser = authRepository.currentUser.value
        val isPrivileged = currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR"
        val isInitialSyncDone = database.appSettingDao().getSetting("is_initial_sync_done")?.settingValue == "true"
        val isRecent = (now - newFile.createdAt) < 24 * 60 * 60 * 1000L
        if (isPrivileged && isRecent && newFile.assignedRmCode != currentUser?.rmCode) {
          context?.let { ctx ->
            val notifKey = "new_file_notif_${newFile.fileId}"
            if (!isInitialSyncDone) {
              NotificationHelper.markNotificationShown(ctx, notifKey)
            } else if (!NotificationHelper.hasNotificationBeenShown(ctx, notifKey)) {
              NotificationHelper.markNotificationShown(ctx, notifKey)
              val rmUser = database.userDao().getUser(newFile.assignedRmCode)
              val rmDisplayName = rmUser?.name ?: newFile.assignedRmCode
              NotificationHelper.sendAdminNewFileNotification(
                context = ctx,
                rmCode = newFile.assignedRmCode,
                rmName = rmDisplayName,
                fileId = newFile.fileId,
                customerName = newFile.customerName,
                productType = newFile.productType
              )
            }
          }
        }
      }
    }
    return updatedCount
  }

  // 7. CSV Export Utility
  fun generateCustomerFilesCsv(files: List<CustomerFileEntity>, isRmUser: Boolean): String {
    val sb = StringBuilder()
    val headers = if (isRmUser) {
      listOf(
        "SL", "CC-Number", "File ID", "Customer Name", "Company", "Mobile", "Email",
        "Product Type", "Status", "Active", "Pending Docs", "Remarks",
        "CPV Status", "Created Date", "Last Updated"
      )
    } else {
      listOf(
        "SL", "CC-Number", "File ID", "Customer Name", "Company", "Mobile", "Email",
        "Product Type", "Status", "Active", "RM Code", "Pending Docs", "Remarks",
        "CPV Status", "Created Date", "Last Updated"
      )
    }
    sb.append(headers.joinToString(",")).append("\n")

    for (f in files) {
      val ccDisplay = if (f.ccNumber.isNotBlank()) f.ccNumber else f.fileId
      val row = if (isRmUser) {
        listOf(
          escapeCsv(f.serialNumber),
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
          escapeCsv(f.serialNumber),
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
    applicationScope.launch { triggerGoogleSheetsSync() }
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

  // 8. Communication: Messages, Team Events & Submissions
  fun getTeamHubMessagesFlow(): Flow<List<com.example.data.model.ChatMessageEntity>> {
    return database.communicationDao().getTeamHubMessagesFlow()
  }

  fun getVisibleMessagesFlow(myRmCode: String): Flow<List<com.example.data.model.ChatMessageEntity>> {
    return database.communicationDao().getVisibleMessagesFlow(myRmCode)
  }

  suspend fun sendChatMessage(
    messageText: String,
    recipientRmCode: String? = null,
    messageType: String = "TEXT",
    eventId: String? = null
  ): Result<com.example.data.model.ChatMessageEntity> = withContext(Dispatchers.IO) {
    val user = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    val now = DateUtils.currentDhakaMillis()
    val msg = com.example.data.model.ChatMessageEntity(
      id = "MSG-${System.currentTimeMillis()}-${SecurityUtils.generateUniqueId().take(4)}",
      senderRmCode = user.rmCode,
      senderName = user.name,
      senderRole = user.role,
      recipientRmCode = recipientRmCode,
      messageText = messageText.trim(),
      timestamp = now,
      messageType = messageType,
      eventId = eventId
    )
    database.communicationDao().insertMessage(msg)
    applicationScope.launch {
      syncChatUpsertToGoogleSheets(msg)
      try { triggerGoogleSheetsSync() } catch (_: Exception) {}
    }
    Result.success(msg)
  }

  suspend fun deleteChatMessage(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
    val now = DateUtils.currentDhakaMillis()
    database.communicationDao().deleteMessage(id)
    deletedChatIdsCache.add(id)
    deletedChatIdsCache.add(id.uppercase())
    syncChatDeletionToGoogleSheets(id)
    Result.success(Unit)
  }

  fun getAllTeamEventsFlow(): Flow<List<com.example.data.model.TeamEventEntity>> {
    return database.communicationDao().getAllEventsFlow()
  }

  suspend fun createTeamEvent(
    title: String,
    description: String,
    targetDate: String,
    allowedFields: String = "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS",
    closeTime: String = ""
  ): Result<com.example.data.model.TeamEventEntity> = withContext(Dispatchers.IO) {
    val user = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    val now = DateUtils.currentDhakaMillis()
    val eventId = "EVT-${System.currentTimeMillis()}"
    val event = com.example.data.model.TeamEventEntity(
      eventId = eventId,
      title = title.trim(),
      description = description.trim(),
      creatorRmCode = user.rmCode,
      creatorName = user.name,
      targetDate = targetDate.trim(),
      createdAt = now,
      status = "ACTIVE",
      allowedFields = allowedFields,
      closeTime = closeTime.trim()
    )
    database.communicationDao().insertEvent(event)

    // Automatically post announcement message into General Team Chat so everyone gets notified!
    val closeTimeText = if (event.closeTime.isNotBlank()) "\nClose Time: ${event.closeTime}" else ""
    val broadcastText = "📢 NEW EVENT: '${event.title}'\nTarget Date: ${event.targetDate}$closeTimeText\n${event.description}\n(Tap this event below to submit your details)"
    sendChatMessage(
      messageText = broadcastText,
      recipientRmCode = null,
      messageType = "EVENT",
      eventId = eventId
    )
    applicationScope.launch {
      syncEventUpsertToGoogleSheets(event)
      try { triggerGoogleSheetsSync() } catch (_: Exception) {}
    }
    Result.success(event)
  }

  fun isEventExpired(event: com.example.data.model.TeamEventEntity): Boolean {
    if (event.status.equals("CLOSED", ignoreCase = true) || event.status.equals("COMPLETED", ignoreCase = true) || event.status.equals("EXPIRED", ignoreCase = true)) {
      return true
    }
    return DateUtils.isEventExpired(event.closeTime, event.targetDate)
  }

  suspend fun updateEventSettings(
    eventId: String,
    title: String,
    description: String,
    targetDate: String,
    status: String,
    allowedFields: String,
    closeTime: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized operation."))

    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access Denied: Only authorized Admins can manage event settings."))
    }

    val existing = database.communicationDao().getEventById(eventId)
      ?: return@withContext Result.failure(Exception("Event not found."))

    val updated = existing.copy(
      title = title.trim(),
      description = description.trim(),
      targetDate = targetDate.trim(),
      status = status.trim(),
      allowedFields = allowedFields.trim(),
      closeTime = if (closeTime.isNotBlank()) closeTime.trim() else existing.closeTime
    )
    database.communicationDao().insertEvent(updated)
    applicationScope.launch {
      syncEventUpsertToGoogleSheets(updated)
      try { triggerGoogleSheetsSync() } catch (_: Exception) {}
    }
    Result.success(Unit)
  }

  suspend fun submitEventResponse(
    eventId: String,
    filesCount: Int,
    requestedDate: String,
    location: String,
    remarks: String,
    customerEntriesJson: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val user = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))

    val event = database.communicationDao().getEventById(eventId)
    if (event != null && isEventExpired(event)) {
      return@withContext Result.failure(Exception("এই ইভেন্টের সময়সীমা শেষ হয়েছে। ডাটা এন্ট্রি বন্ধ রয়েছে।"))
    }

    val now = DateUtils.currentDhakaMillis()
    val respId = "RESP-$eventId-${user.rmCode.trim().uppercase()}"
    val resp = com.example.data.model.EventResponseEntity(
      responseId = respId,
      eventId = eventId,
      rmCode = user.rmCode,
      rmName = user.name,
      filesCount = filesCount,
      requestedDate = requestedDate.trim(),
      location = location.trim(),
      remarks = remarks.trim(),
      customerEntriesJson = customerEntriesJson.trim(),
      submittedAt = now
    )
    database.communicationDao().insertOrUpdateResponse(resp)
    applicationScope.launch {
      syncEventResponseUpsertToGoogleSheets(resp)
      try { triggerGoogleSheetsSync() } catch (_: Exception) {}
    }
    Result.success(Unit)
  }

  fun getResponsesForEventFlow(eventId: String): Flow<List<com.example.data.model.EventResponseEntity>> {
    return database.communicationDao().getResponsesForEventFlow(eventId)
  }

  suspend fun getResponsesForEvent(eventId: String): List<com.example.data.model.EventResponseEntity> = withContext(Dispatchers.IO) {
    database.communicationDao().getResponsesForEvent(eventId)
  }

  suspend fun getUserResponseForEvent(eventId: String, rmCode: String): com.example.data.model.EventResponseEntity? = withContext(Dispatchers.IO) {
    database.communicationDao().getUserResponseForEvent(eventId, rmCode)
  }

  suspend fun deleteTeamEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized."))
    val event = database.communicationDao().getEventById(eventId)

    // User requirement: Admin and Mentor have permission to create and delete events
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR" && event != null && !event.creatorRmCode.equals(currentUser.rmCode, ignoreCase = true)) {
      return@withContext Result.failure(Exception("Access Denied: Only Admin or Mentor can delete team events."))
    }

    val now = DateUtils.currentDhakaMillis()
    val allMsgs = database.communicationDao().getAllMessages()
    val relatedMsgIds = allMsgs.filter {
      it.eventId == eventId || (it.messageType == "EVENT" && it.messageText.contains(eventId)) || (event != null && it.messageText.contains(event.title))
    }.map { it.id }

    database.communicationDao().deleteEvent(eventId)
    database.communicationDao().deleteResponsesForEvent(eventId)
    database.communicationDao().deleteMessagesByEventId(eventId)
    relatedMsgIds.forEach { mId ->
      database.communicationDao().deleteMessage(mId)
      syncChatDeletionToGoogleSheets(mId)
    }

    // Store in persistent deleted_event_ids setting so it never gets restored
    val currentDel = database.appSettingDao().getSetting("deleted_event_ids")?.settingValue ?: ""
    val updatedDel = (currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() } + eventId + eventId.uppercase() + eventId.lowercase()).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_event_ids", updatedDel, currentUser.rmCode, now)
    )

    // Also track in deleted_chat_ids so event announcement message is never restored from sheet
    val currentDelChats = database.appSettingDao().getSetting("deleted_chat_ids")?.settingValue ?: ""
    val updatedDelChats = (currentDelChats.split(",").map { it.trim() }.filter { it.isNotBlank() } + "EVENT_$eventId" + relatedMsgIds).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_chat_ids", updatedDelChats, currentUser.rmCode, now)
    )

    deletedEventIdsCache.add(eventId)
    deletedEventIdsCache.add(eventId.uppercase())
    deletedEventIdsCache.add(eventId.lowercase())

    // Synchronize Event deletion directly to Google Sheets
    syncEventDeletionToGoogleSheets(eventId)
    Result.success(Unit)
  }

  // Important Documents Repository Methods (Admin & Mentor upload/edit/delete, everyone can view/download)
  fun getAllImportantDocumentsFlow(): Flow<List<com.example.data.model.ImportantDocumentEntity>> {
    return database.importantDocumentDao().getAllDocumentsFlow()
  }

  suspend fun getAllImportantDocuments(): List<com.example.data.model.ImportantDocumentEntity> = withContext(Dispatchers.IO) {
    database.importantDocumentDao().getAllActiveDocuments()
  }

  suspend fun saveImportantDocument(
    docId: String?,
    title: String,
    category: String,
    description: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String,
    storagePath: String,
    docDate: String? = null
  ): Result<com.example.data.model.ImportantDocumentEntity> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized operation."))

    // User requirement: Admin, Mentor, and RM can all upload important documents & circulars
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR" && currentUser.role != "RM") {
      return@withContext Result.failure(Exception("Access Denied: Only bank personnel can upload documents."))
    }

    val now = DateUtils.currentDhakaMillis()
    val cleanId = docId?.trim().orEmpty()
    val existing = if (cleanId.isNotBlank()) database.importantDocumentDao().getDocumentById(cleanId) else null
    val targetDocId = existing?.docId ?: (if (cleanId.isNotBlank()) cleanId else "DOC_${System.currentTimeMillis()}")

    var finalFileUri = fileUri.ifBlank { existing?.fileUri ?: "" }
    var finalStoragePath = storagePath.ifBlank { existing?.storagePath ?: "" }

    // If file is on local device, upload to Google Drive so ALL users across all devices can view it!
    if (!finalFileUri.startsWith("http") && finalStoragePath.isNotBlank()) {
      val localF = java.io.File(finalStoragePath)
      if (localF.exists() && localF.length() > 0) {
        try {
          val driveRes = uploadFileToGoogleDrive(localF, fileName.ifBlank { existing?.fileName ?: "doc.pdf" }, fileType)
          if (driveRes.isSuccess) {
            val link = driveRes.getOrNull()
            if (!link.isNullOrBlank()) {
              finalFileUri = link
              finalStoragePath = link
            }
          }
        } catch (_: Exception) {}
      }
    }

    val customDateMillis = if (!docDate.isNullOrBlank()) DateUtils.parseDateTime(docDate) else null
    val docCreationTimestamp = customDateMillis ?: (existing?.createdAt ?: now)

    val entity = com.example.data.model.ImportantDocumentEntity(
      docId = targetDocId,
      title = title.trim(),
      category = category.trim().ifBlank { "Policies & Circulars" },
      description = description.trim(),
      fileName = fileName.trim().ifBlank { existing?.fileName ?: "Document_$targetDocId.pdf" },
      fileType = fileType.ifBlank { existing?.fileType ?: "application/pdf" },
      fileSizeBytes = if (fileSizeBytes > 0) fileSizeBytes else (existing?.fileSizeBytes ?: 1024L),
      fileUri = finalFileUri,
      storagePath = finalStoragePath,
      uploadedBy = existing?.uploadedBy ?: currentUser.rmCode,
      uploaderName = existing?.uploaderName ?: currentUser.name,
      uploaderRole = existing?.uploaderRole ?: currentUser.role,
      createdAt = docCreationTimestamp,
      updatedAt = now,
      isDeleted = false,
      isSynced = false
    )

    if (existing == null) {
      database.importantDocumentDao().insertDocument(entity)
    } else {
      database.importantDocumentDao().updateDocument(entity)
    }

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = if (existing == null) "CREATE_DOC" else "UPDATE_DOC",
        fileId = entity.docId,
        details = "Important Document '${entity.title}' saved by ${currentUser.role} (${currentUser.name})",
        timestamp = now
      )
    )

    // Remove from deleted_doc_ids if previously deleted
    val currentDelDocs = database.appSettingDao().getSetting("deleted_doc_ids")?.settingValue ?: ""
    if (currentDelDocs.isNotBlank()) {
      val remaining = currentDelDocs.split(",").map { it.trim() }.filter {
        it.isNotBlank() && !it.equals(targetDocId, ignoreCase = true)
      }.joinToString(",")
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity("deleted_doc_ids", remaining, currentUser.rmCode, now)
      )
    }

    // Direct synchronization of Document upsert to Google Sheets
    syncDocumentUpsertToGoogleSheets(entity)
    CoroutineScope(Dispatchers.IO).launch {
      try { triggerGoogleSheetsSync() } catch (_: Exception) {}
    }

    Result.success(entity)
  }

  suspend fun deleteImportantDocument(docId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val currentUser = authRepository.currentUser.value
      ?: return@withContext Result.failure(Exception("Unauthorized operation."))

    // User prompt constraint: ONLY Admin or Mentor can delete
    if (currentUser.role != "ADMIN" && currentUser.role != "MENTOR") {
      return@withContext Result.failure(Exception("Access Denied: Only Admin or Mentor can delete important documents."))
    }

    val now = DateUtils.currentDhakaMillis()
    database.importantDocumentDao().softDeleteDocument(docId, now)
    database.importantDocumentDao().permanentDeleteDocument(docId)

    // Store in persistent deleted_doc_ids setting so it never gets restored from sheet
    val currentDel = database.appSettingDao().getSetting("deleted_doc_ids")?.settingValue ?: ""
    val updatedDel = (currentDel.split(",").map { it.trim() }.filter { it.isNotBlank() } + docId + docId.uppercase() + docId.lowercase()).distinct().joinToString(",")
    database.appSettingDao().insertOrUpdateSetting(
      AppSettingEntity("deleted_doc_ids", updatedDel, currentUser.rmCode, now)
    )

    database.auditLogDao().insertLog(
      AuditLogEntity(
        logId = "LOG-${SecurityUtils.generateUniqueId().take(8)}",
        userId = currentUser.rmCode,
        role = currentUser.role,
        action = "DELETE_DOC",
        fileId = docId,
        timestamp = System.currentTimeMillis(),
        details = "Important Document '$docId' deleted by ${currentUser.role} (${currentUser.name})",
      )
    )

    deletedDocIdsCache.add(docId)
    deletedDocIdsCache.add(docId.uppercase())
    deletedDocIdsCache.add(docId.lowercase())

    // Synchronize Document deletion directly to Google Sheets
    syncDocumentDeletionToGoogleSheets(docId)
    Result.success(Unit)
  }
}
