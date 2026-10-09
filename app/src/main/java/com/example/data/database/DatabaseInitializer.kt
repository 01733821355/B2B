package com.example.data.database

import com.example.data.model.AppSettingEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {

  suspend fun initializeIfNeeded(database: AppDatabase) = withContext(Dispatchers.IO) {
    val targetWebAppUrl = "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"

    // Ensure SyncStatus is always configured with user's Web App URL & Spreadsheet ID on any phone
    val existingStatus = database.appSettingDao().getSyncStatus()
    val defaultSpreadsheetId = "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI"
    val defaultSecret = "ebl_secure_sync_token_2026"
    if (existingStatus == null || existingStatus.appsScriptUrl != targetWebAppUrl || existingStatus.spreadsheetId.isBlank()) {
      database.appSettingDao().insertOrUpdateSyncStatus(
        (existingStatus ?: SyncStatusEntity()).copy(
          appsScriptUrl = targetWebAppUrl,
          spreadsheetId = if (existingStatus?.spreadsheetId.isNullOrBlank()) defaultSpreadsheetId else existingStatus!!.spreadsheetId,
          syncSecretKey = if (existingStatus?.syncSecretKey.isNullOrBlank()) defaultSecret else existingStatus!!.syncSecretKey,
          lastSyncStatus = "READY",
          lastSyncMessage = "Ready for live bi-directional sync."
        )
      )
    }

    // Ensure old non-auth audit logs are purged so only login/logout logs are kept
    database.auditLogDao().purgeNonAuthLogs()

    // Seed default important banking documents if not yet present
    if (database.importantDocumentDao().getAllActiveDocuments().isEmpty()) {
      val now = DateUtils.currentDhakaMillis()
      val sampleDocs = listOf(
        com.example.data.model.ImportantDocumentEntity(
          docId = "DOC_CIRCULAR_2026_01",
          title = "EBL Retail Lending & Credit Card Policy 2026",
          category = "Policies & Circulars",
          description = "Official operational circular containing eligibility criteria, minimum salary threshold, and credit assessment rules.",
          fileName = "EBL_Retail_Credit_Policy_2026.pdf",
          fileType = "application/pdf",
          fileSizeBytes = 284000L,
          fileUri = "",
          storagePath = "",
          uploadedBy = "Admin0",
          uploaderName = "System Administrator",
          uploaderRole = "ADMIN",
          createdAt = now - 86400000L * 5,
          updatedAt = now - 86400000L * 5,
          isDeleted = false,
          isSynced = true
        ),
        com.example.data.model.ImportantDocumentEntity(
          docId = "DOC_CPV_SOP_02",
          title = "Customer Physical Verification (CPV) Guidelines",
          category = "CPV & Compliance",
          description = "Step-by-step Standard Operating Procedure (SOP) for residential and office verification by sales field agents.",
          fileName = "CPV_Verification_Guidelines_v2.pdf",
          fileType = "application/pdf",
          fileSizeBytes = 192000L,
          fileUri = "",
          storagePath = "",
          uploadedBy = "12345",
          uploaderName = "Senior Operations Mentor",
          uploaderRole = "MENTOR",
          createdAt = now - 86400000L * 3,
          updatedAt = now - 86400000L * 3,
          isDeleted = false,
          isSynced = true
        ),
        com.example.data.model.ImportantDocumentEntity(
          docId = "DOC_SALARY_CERT_03",
          title = "Standard Salary Certificate & Pay Slip Format",
          category = "Forms & Formats",
          description = "Standard corporate salary certificate and HR declaration template required for salaried professionals.",
          fileName = "Standard_Salary_Certificate_Format.pdf",
          fileType = "application/pdf",
          fileSizeBytes = 145000L,
          fileUri = "",
          storagePath = "",
          uploadedBy = "Admin0",
          uploaderName = "System Administrator",
          uploaderRole = "ADMIN",
          createdAt = now - 86400000L * 2,
          updatedAt = now - 86400000L * 2,
          isDeleted = false,
          isSynced = true
        ),
        com.example.data.model.ImportantDocumentEntity(
          docId = "DOC_RATES_CHART_04",
          title = "Schedule of Charges & Product Rates Chart 2026",
          category = "Product Guidelines",
          description = "Updated interest rates, annual fees, card issuance charges, and reward points structure for all EBL cards.",
          fileName = "Schedule_of_Charges_Rates_2026.pdf",
          fileType = "application/pdf",
          fileSizeBytes = 312000L,
          fileUri = "",
          storagePath = "",
          uploadedBy = "12345",
          uploaderName = "Senior Operations Mentor",
          uploaderRole = "MENTOR",
          createdAt = now - 86400000L,
          updatedAt = now - 86400000L,
          isDeleted = false,
          isSynced = true
        )
      )
      database.importantDocumentDao().insertDocuments(sampleDocs)
    }

    val existingUsers = database.userDao().getAllUsers()
    if (existingUsers.isNotEmpty()) {
      return@withContext
    }

    val now = DateUtils.currentDhakaMillis()

    // 1. Seed Initial Admin & Mentor Accounts (Fresh installation, no demo customer files)
    // Admin0 (Initial default password: #123456A)
    val adminSalt = SecurityUtils.generateSalt()
    val adminHash = SecurityUtils.hashPassword("#123456A", adminSalt)
    val adminUser = UserEntity(
      rmCode = "Admin0",
      name = "System Administrator",
      role = "ADMIN",
      passwordHash = adminHash,
      salt = adminSalt,
      mobile = "+8801700000001",
      email = "admin0@suite.local",
      officeAddress = "Corporate Office, 100 Gulshan Ave, Dhaka",
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = now,
      authUid = "AUTH_ADMIN_0"
    )

    // Mentor (Initial default password: 12345)
    val mentorSalt = SecurityUtils.generateSalt()
    val mentorHash = SecurityUtils.hashPassword("12345", mentorSalt)
    val mentorUser = UserEntity(
      rmCode = "12345",
      name = "Senior Operations Mentor",
      role = "MENTOR",
      passwordHash = mentorHash,
      salt = mentorSalt,
      mobile = "+8801700000002",
      email = "mentor12345@suite.local",
      officeAddress = "Operations Center, Motijheel C/A, Dhaka",
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = now,
      authUid = "AUTH_MENTOR_12345"
    )

    database.userDao().insertUsers(listOf(adminUser, mentorUser))

    // 2. Seed Core App Settings
    val settings = listOf(
      AppSettingEntity(
        settingKey = "app_custom_name",
        settingValue = "RM File Management Suite",
        updatedBy = "SYSTEM",
        updatedAt = now
      ),
      AppSettingEntity(
        settingKey = "PRODUCT_TYPES",
        settingValue = "Credit Card,B2B,Corporate Card,Split,Limit Enhancement",
        updatedBy = "SYSTEM",
        updatedAt = now
      ),
      AppSettingEntity(
        settingKey = "PENDING_DOCS_LIST",
        settingValue = "NID,TIN,Office ID,Salary Certificate,Account Statement (6 Months),BIN,Trade License 2024-25,Trade License 2025-26,Trade License 2026-27,Loan Certificate,Card Statement (Month),Card Copy",
        updatedBy = "SYSTEM",
        updatedAt = now
      ),
      AppSettingEntity(
        settingKey = "WEEK_START_DAY",
        settingValue = "SATURDAY",
        updatedBy = "SYSTEM",
        updatedAt = now
      ),
      AppSettingEntity(
        settingKey = "REPORTING_TIMEZONE",
        settingValue = "Asia/Dhaka",
        updatedBy = "SYSTEM",
        updatedAt = now
      )
    )
    database.appSettingDao().insertOrUpdateSettings(settings)

    // 3. Seed Initial Sync Status
    val syncStatus = SyncStatusEntity(
      id = 1,
      spreadsheetId = defaultSpreadsheetId,
      lastSyncTimestamp = null,
      lastSyncStatus = "READY",
      lastSyncMessage = "Ready for live bi-directional sync with Google Sheets.",
      pendingRecordsCount = 0,
      appsScriptUrl = targetWebAppUrl,
      syncSecretKey = defaultSecret
    )
    database.appSettingDao().insertOrUpdateSyncStatus(syncStatus)
  }
}
