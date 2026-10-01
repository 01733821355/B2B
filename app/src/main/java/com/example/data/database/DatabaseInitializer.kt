package com.example.data.database

import com.example.data.model.AppSettingEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CustomerFileEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {

  suspend fun initializeIfNeeded(database: AppDatabase) = withContext(Dispatchers.IO) {
    val existingUsers = database.userDao().getAllUsers()
    if (existingUsers.isNotEmpty()) {
      return@withContext
    }

    val now = DateUtils.currentDhakaMillis()
    val todayDate = DateUtils.currentDhakaDate()
    val todayMillis = now
    val threeDaysAgoMillis = now - (3 * 24 * 60 * 60 * 1000L)
    val tenDaysAgoMillis = now - (10 * 24 * 60 * 60 * 1000L)

    // 1. Seed Bootstrap Users
    // Admin0 (Initial: #123456A)
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
      mustChangePassword = true,
      createdAt = now,
      authUid = "AUTH_ADMIN_0"
    )

    // Mentor (Initial: 12345 / 12345)
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
      mustChangePassword = true,
      createdAt = now,
      authUid = "AUTH_MENTOR_12345"
    )

    // RM 104393
    val rm1Salt = SecurityUtils.generateSalt()
    val rm1Hash = SecurityUtils.hashPassword("password123", rm1Salt)
    val rm1User = UserEntity(
      rmCode = "104393",
      name = "Tanvir Ahmed",
      role = "RM",
      passwordHash = rm1Hash,
      salt = rm1Salt,
      mobile = "01711223344",
      email = "tanvir.104393@suite.local",
      officeAddress = "Gulshan Branch, Dhaka",
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = tenDaysAgoMillis,
      lastLogin = now - 15 * 60 * 1000L,
      authUid = "AUTH_RM_104393",
      lastLatitude = 23.7808,
      lastLongitude = 90.4192,
      lastLocationAddress = "Gulshan-1 Circle, Dhaka-1212",
      lastLocationTime = now - 10 * 60 * 1000L,
      isOnline = true
    )

    // RM 104394
    val rm2Salt = SecurityUtils.generateSalt()
    val rm2Hash = SecurityUtils.hashPassword("password123", rm2Salt)
    val rm2User = UserEntity(
      rmCode = "104394",
      name = "Nusrat Jahan",
      role = "RM",
      passwordHash = rm2Hash,
      salt = rm2Salt,
      mobile = "01819556677",
      email = "nusrat.104394@suite.local",
      officeAddress = "Principal Branch, Motijheel, Dhaka",
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = tenDaysAgoMillis,
      lastLogin = now - 45 * 60 * 1000L,
      authUid = "AUTH_RM_104394",
      lastLatitude = 23.7314,
      lastLongitude = 90.4184,
      lastLocationAddress = "Dilkusha C/A, Motijheel, Dhaka-1000",
      lastLocationTime = now - 30 * 60 * 1000L,
      isOnline = true
    )

    // RM 104395
    val rm3Salt = SecurityUtils.generateSalt()
    val rm3Hash = SecurityUtils.hashPassword("password123", rm3Salt)
    val rm3User = UserEntity(
      rmCode = "104395",
      name = "Rafiqul Islam",
      role = "RM",
      passwordHash = rm3Hash,
      salt = rm3Salt,
      mobile = "01912334455",
      email = "rafiqul.104395@suite.local",
      officeAddress = "Dhanmondi Branch, Road 27, Dhaka",
      accountStatus = "ACTIVE",
      mustChangePassword = false,
      createdAt = tenDaysAgoMillis,
      lastLogin = now - 120 * 60 * 1000L,
      authUid = "AUTH_RM_104395",
      lastLatitude = 23.7509,
      lastLongitude = 90.3752,
      lastLocationAddress = "Road 27, Dhanmondi R/A, Dhaka-1209",
      lastLocationTime = now - 90 * 60 * 1000L,
      isOnline = false
    )

    database.userDao().insertUsers(listOf(adminUser, mentorUser, rm1User, rm2User, rm3User))

    // Seed initial location logs for Mentor monitoring
    database.userLocationLogDao().insertLocationLog(
      com.example.data.model.UserLocationLogEntity(
        rmCode = "104393",
        userName = "Tanvir Ahmed",
        latitude = 23.7808,
        longitude = 90.4192,
        address = "Gulshan-1 Circle, Dhaka-1212",
        sourceAction = "GPS_AUTO_DETECT",
        timestamp = now - 10 * 60 * 1000L
      )
    )
    database.userLocationLogDao().insertLocationLog(
      com.example.data.model.UserLocationLogEntity(
        rmCode = "104394",
        userName = "Nusrat Jahan",
        latitude = 23.7314,
        longitude = 90.4184,
        address = "Dilkusha C/A, Motijheel, Dhaka-1000",
        sourceAction = "LOGIN",
        timestamp = now - 30 * 60 * 1000L
      )
    )
    database.userLocationLogDao().insertLocationLog(
      com.example.data.model.UserLocationLogEntity(
        rmCode = "104395",
        userName = "Rafiqul Islam",
        latitude = 23.7509,
        longitude = 90.3752,
        address = "Road 27, Dhanmondi R/A, Dhaka-1209",
        sourceAction = "CUSTOMER_FILE_ENTRY",
        timestamp = now - 90 * 60 * 1000L
      )
    )

    // 2. Seed Customer Files
    val sampleFiles = listOf(
      CustomerFileEntity(
        fileId = "DOC-2026-4393-101",
        customerName = "Kazi Mahbubur Rahman",
        companyName = "Square Pharmaceuticals Ltd",
        officeAddress = "Square Centre, 48 Mohakhali C/A, Dhaka",
        mobile = "01713001122",
        altMobile = "01819001122",
        email = "mahbub.kazi@squarepharma.com",
        productType = "Credit Card",
        applicationStatus = "Approved",
        activeStatus = "Y",
        assignedRmCode = "104393",
        ccNumber = "4532-8901-2345-101",
        pendingDocuments = "",
        remarks = "Pre-approved corporate executive card. Limit 350,000 BDT. VIP client.",
        cpvStatus = "Completed",
        cpvDate = DateUtils.formatIsoDate(threeDaysAgoMillis),
        cpvAddress = "Square Centre, 48 Mohakhali C/A",
        cpvRemarks = "CPV verified in person. Company HR confirmed employment.",
        cpvLastUpdatedBy = "104393",
        createdAt = tenDaysAgoMillis,
        updatedAt = todayMillis,
        submittedAt = tenDaysAgoMillis + 86400000L,
        approvedAt = threeDaysAgoMillis,
        createdBy = "104393",
        updatedBy = "Admin0",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4393-102",
        customerName = "Farhana Yasmin",
        companyName = "Grameenphone Ltd",
        officeAddress = "GPHouse, Bashundhara, Baridhara, Dhaka",
        mobile = "01711889900",
        altMobile = "",
        email = "farhana.y@grameenphone.com",
        productType = "Corporate Card",
        applicationStatus = "Submitted",
        activeStatus = "N",
        assignedRmCode = "104393",
        ccNumber = "4532-8901-2345-102",
        pendingDocuments = "Salary Certificate,Account Statement (6 Months)",
        remarks = "Application forwarded to Head Office credit risk team. Awaiting 6-month account statement.",
        cpvStatus = "Completed",
        cpvDate = DateUtils.formatIsoDate(todayMillis),
        cpvAddress = "GPHouse, Bashundhara",
        cpvRemarks = "Office verification complete with GP HR department.",
        cpvLastUpdatedBy = "104393",
        createdAt = threeDaysAgoMillis,
        updatedAt = todayMillis,
        submittedAt = todayMillis,
        createdBy = "104393",
        updatedBy = "104393",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4393-103",
        customerName = "Shahidul Alam Chowdhury",
        companyName = "Bengal Group of Industries",
        officeAddress = "Bengal House, 75 Gulshan Ave, Dhaka",
        mobile = "01911445566",
        altMobile = "01712445566",
        email = "shahidul@bengalgroup.com",
        productType = "Limit Enhancement",
        applicationStatus = "Query",
        activeStatus = "N",
        assignedRmCode = "104393",
        ccNumber = "4532-8901-2345-103",
        pendingDocuments = "TIN,Trade License 2025-26",
        remarks = "Query raised by Credit Division: updated Trade License required for limit enhancement to 700k BDT.",
        cpvStatus = "Pending",
        cpvDate = "",
        cpvAddress = "75 Gulshan Ave, Dhaka",
        cpvRemarks = "Scheduled for next working day.",
        cpvLastUpdatedBy = "104393",
        createdAt = threeDaysAgoMillis,
        updatedAt = todayMillis,
        submittedAt = threeDaysAgoMillis,
        createdBy = "104393",
        updatedBy = "104393",
        isSynced = false
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4393-104",
        customerName = "Syed Ariful Haque",
        companyName = "Apex Footwear Ltd",
        officeAddress = "House 6, Road 137, Gulshan 1, Dhaka",
        mobile = "01819223344",
        altMobile = "",
        email = "arif.haque@apexfootwearltd.com",
        productType = "Credit Card",
        applicationStatus = "Collected",
        activeStatus = "N",
        assignedRmCode = "104393",
        ccNumber = "4532-8901-2345-104",
        pendingDocuments = "NID,Office ID,Card Copy",
        remarks = "Application form signed. Customer requested credit card copy of existing card.",
        cpvStatus = "Not Required",
        cpvDate = "",
        cpvAddress = "",
        cpvRemarks = "",
        cpvLastUpdatedBy = "104393",
        createdAt = todayMillis,
        updatedAt = todayMillis,
        createdBy = "104393",
        updatedBy = "104393",
        isSynced = false
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4393-105",
        customerName = "Tariqul Islam Babul",
        companyName = "Beximco Communications",
        officeAddress = "SAMU Tower, Gulshan-1, Dhaka",
        mobile = "01611002233",
        email = "tariqul.islam@akashdth.com",
        productType = "Split",
        applicationStatus = "Return to Source",
        activeStatus = "N",
        assignedRmCode = "104393",
        ccNumber = "4532-8901-2345-105",
        pendingDocuments = "Account Statement (6 Months)",
        remarks = "CIB report shows delayed installment on another facility. Returned to RM for customer clarification.",
        cpvStatus = "Failed",
        cpvDate = DateUtils.formatIsoDate(threeDaysAgoMillis),
        cpvAddress = "SAMU Tower, Gulshan-1",
        cpvRemarks = "Customer relocated to new department.",
        cpvLastUpdatedBy = "Admin0",
        createdAt = tenDaysAgoMillis,
        updatedAt = threeDaysAgoMillis,
        submittedAt = tenDaysAgoMillis + 86400000L,
        createdBy = "104393",
        updatedBy = "Admin0",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4394-201",
        customerName = "Anisur Rahman",
        companyName = "Unilever Bangladesh Ltd",
        officeAddress = "ZN Tower, Plot 2, Road 8, Gulshan-1",
        mobile = "01715667788",
        email = "anisur.rahman@unilever.com",
        productType = "B2B",
        applicationStatus = "Approved",
        activeStatus = "Y",
        assignedRmCode = "104394",
        ccNumber = "4532-8901-2345-201",
        pendingDocuments = "",
        remarks = "Corporate vendor supplier card limit 1.2M BDT approved.",
        cpvStatus = "Completed",
        cpvDate = DateUtils.formatIsoDate(threeDaysAgoMillis),
        cpvAddress = "ZN Tower, Gulshan-1",
        cpvRemarks = "Physical verification successful.",
        cpvLastUpdatedBy = "104394",
        createdAt = tenDaysAgoMillis,
        updatedAt = threeDaysAgoMillis,
        submittedAt = tenDaysAgoMillis + 86400000L,
        approvedAt = threeDaysAgoMillis,
        createdBy = "104394",
        updatedBy = "104394",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4394-202",
        customerName = "Mehzabin Akhter",
        companyName = "British American Tobacco BD",
        officeAddress = "Mohakhali New DOHS Road, Dhaka",
        mobile = "01817554433",
        email = "mehzabin@batbd.com",
        productType = "Credit Card",
        applicationStatus = "Condition",
        activeStatus = "N",
        assignedRmCode = "104394",
        ccNumber = "4532-8901-2345-202",
        pendingDocuments = "Salary Certificate",
        remarks = "Approved subject to submission of latest original pay slip with seal.",
        cpvStatus = "Completed",
        cpvDate = DateUtils.formatIsoDate(threeDaysAgoMillis),
        cpvAddress = "BAT Mohakhali Office",
        cpvRemarks = "Employment verified with HR.",
        cpvLastUpdatedBy = "104394",
        createdAt = threeDaysAgoMillis,
        updatedAt = todayMillis,
        submittedAt = threeDaysAgoMillis,
        createdBy = "104394",
        updatedBy = "104394",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4395-301",
        customerName = "Zubair Al Mahmud",
        companyName = "Navana Group",
        officeAddress = "Islam Chamber, 125/A Motijheel C/A",
        mobile = "01712998877",
        email = "zubair@navana.com",
        productType = "Credit Card",
        applicationStatus = "STC",
        activeStatus = "N",
        assignedRmCode = "104395",
        ccNumber = "4532-8901-2345-301",
        pendingDocuments = "Account Statement (6 Months),Loan Certificate",
        remarks = "Subject To Clearance (STC) - requires branch manager sign-off.",
        cpvStatus = "Pending",
        cpvDate = "",
        cpvAddress = "Motijheel C/A",
        cpvRemarks = "Scheduled.",
        cpvLastUpdatedBy = "104395",
        createdAt = threeDaysAgoMillis,
        updatedAt = todayMillis,
        submittedAt = threeDaysAgoMillis,
        createdBy = "104395",
        updatedBy = "104395",
        isSynced = true
      ),
      CustomerFileEntity(
        fileId = "DOC-2026-4395-302",
        customerName = "Naimul Hasan",
        companyName = "Standard Chartered BackOffice",
        officeAddress = "67 Gulshan Avenue, Dhaka",
        mobile = "01918332211",
        email = "naimul.hasan@sc.com",
        productType = "Credit Card",
        applicationStatus = "Declined",
        activeStatus = "C",
        assignedRmCode = "104395",
        ccNumber = "4532-8901-2345-302",
        pendingDocuments = "",
        remarks = "Declined due to internal policy limit threshold.",
        cpvStatus = "Completed",
        cpvDate = DateUtils.formatIsoDate(tenDaysAgoMillis),
        cpvAddress = "67 Gulshan Avenue",
        cpvRemarks = "Office verified.",
        cpvLastUpdatedBy = "Admin0",
        createdAt = tenDaysAgoMillis,
        updatedAt = threeDaysAgoMillis,
        submittedAt = tenDaysAgoMillis,
        createdBy = "104395",
        updatedBy = "Admin0",
        isSynced = true
      )
    )

    database.customerFileDao().insertFiles(sampleFiles)

    // 3. Seed Sample Attachments
    val sampleAttachments = listOf(
      FileAttachmentEntity(
        attachmentId = "ATT-101-1",
        fileId = "DOC-2026-4393-101",
        category = "NID",
        fileName = "NID_Kazi_Mahbubur_Rahman.pdf",
        fileType = "application/pdf",
        storagePath = "files/104393/DOC-2026-4393-101/nid.pdf",
        fileSizeBytes = 524288L,
        uploadedBy = "104393",
        uploadedAt = tenDaysAgoMillis
      ),
      FileAttachmentEntity(
        attachmentId = "ATT-101-2",
        fileId = "DOC-2026-4393-101",
        category = "Salary Certificate",
        fileName = "Salary_Certificate_Square.pdf",
        fileType = "application/pdf",
        storagePath = "files/104393/DOC-2026-4393-101/salary.pdf",
        fileSizeBytes = 245760L,
        uploadedBy = "104393",
        uploadedAt = tenDaysAgoMillis
      ),
      FileAttachmentEntity(
        attachmentId = "ATT-101-3",
        fileId = "DOC-2026-4393-101",
        category = "CPV",
        fileName = "CPV_Photo_Square_Center.jpg",
        fileType = "image/jpeg",
        storagePath = "files/104393/DOC-2026-4393-101/cpv.jpg",
        fileSizeBytes = 1048576L,
        uploadedBy = "104393",
        uploadedAt = threeDaysAgoMillis
      ),
      FileAttachmentEntity(
        attachmentId = "ATT-102-1",
        fileId = "DOC-2026-4393-102",
        category = "Office ID",
        fileName = "GP_Office_ID_Farhana.jpg",
        fileType = "image/jpeg",
        storagePath = "files/104393/DOC-2026-4393-102/office_id.jpg",
        fileSizeBytes = 720000L,
        uploadedBy = "104393",
        uploadedAt = threeDaysAgoMillis
      )
    )
    database.fileAttachmentDao().insertAttachments(sampleAttachments)

    // 4. Seed Audit Logs
    val sampleLogs = listOf(
      AuditLogEntity(
        logId = "LOG-001",
        userId = "SYSTEM",
        role = "SYSTEM",
        action = "DATABASE_BOOTSTRAP",
        fileId = null,
        rmCode = null,
        timestamp = now - 86400000L,
        details = "Initial database bootstrap completed with secure role definitions."
      ),
      AuditLogEntity(
        logId = "LOG-002",
        userId = "104393",
        role = "RM",
        action = "FILE_CREATE",
        fileId = "DOC-2026-4393-101",
        rmCode = "104393",
        timestamp = tenDaysAgoMillis,
        details = "Created customer file for Kazi Mahbubur Rahman (Square Pharma)."
      ),
      AuditLogEntity(
        logId = "LOG-003",
        userId = "Admin0",
        role = "ADMIN",
        action = "FILE_UPDATE",
        fileId = "DOC-2026-4393-101",
        rmCode = "104393",
        timestamp = threeDaysAgoMillis,
        details = "Approved customer file. Active card status marked as Y."
      ),
      AuditLogEntity(
        logId = "LOG-004",
        userId = "104393",
        role = "RM",
        action = "CPV_UPDATE",
        fileId = "DOC-2026-4393-102",
        rmCode = "104393",
        timestamp = todayMillis,
        details = "Completed CPV verification for Farhana Yasmin at GPHouse."
      )
    )
    database.auditLogDao().insertLogs(sampleLogs)

    // 5. Seed App Settings
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

    // 6. Seed Sync Status
    val syncStatus = SyncStatusEntity(
      id = 1,
      spreadsheetId = "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI",
      lastSyncTimestamp = threeDaysAgoMillis,
      lastSyncStatus = "SUCCESS",
      lastSyncMessage = "Synchronized customer files and RM mapping entries to Google Sheets.",
      pendingRecordsCount = 0,
      appsScriptUrl = "",
      syncSecretKey = ""
    )
    database.appSettingDao().insertOrUpdateSyncStatus(syncStatus)
  }
}
