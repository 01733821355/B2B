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

    // Ensure SyncStatus is always configured with user's Web App URL on any phone
    val existingStatus = database.appSettingDao().getSyncStatus()
    if (existingStatus == null || existingStatus.appsScriptUrl != targetWebAppUrl) {
      database.appSettingDao().insertOrUpdateSyncStatus(
        (existingStatus ?: SyncStatusEntity()).copy(
          appsScriptUrl = targetWebAppUrl,
          lastSyncStatus = "READY",
          lastSyncMessage = "Ready for live bi-directional sync."
        )
      )
    }

    // Ensure old non-auth audit logs are purged so only login/logout logs are kept
    database.auditLogDao().purgeNonAuthLogs()

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
      spreadsheetId = "",
      lastSyncTimestamp = null,
      lastSyncStatus = "READY",
      lastSyncMessage = "Ready for live bi-directional sync with Google Sheets.",
      pendingRecordsCount = 0,
      appsScriptUrl = targetWebAppUrl,
      syncSecretKey = ""
    )
    database.appSettingDao().insertOrUpdateSyncStatus(syncStatus)
  }
}
