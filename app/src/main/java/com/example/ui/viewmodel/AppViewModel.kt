package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.model.AppSettingEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CustomerFileEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.SmsNotificationEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.EblRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
  object Login : Screen()
  object RmDashboard : Screen()
  data class CustomerForm(val editFileId: String? = null) : Screen()
  object CustomerList : Screen()
  object AdminDashboard : Screen()
  object GlobalDatabase : Screen()
  object RmMapping : Screen()
  object Reports : Screen()
  object GoogleSheetsSync : Screen()
  object AuditLogs : Screen()
  object AppSettings : Screen()
  object MentorDashboard : Screen()
  object MentorUserLocationTracking : Screen()
  object ProfilePassword : Screen()
  object DbrChecklist : Screen()
  object CommunicationHub : Screen()
  object ImportantDocuments : Screen()
}

data class KpiStats(
  val totalFiles: Int = 0,
  val stc: Int = 0, // STC / Production Done (Primary Achievement)
  val submitted: Int = 0,
  val analystReceive: Int = 0, // Analyst Receive (After Collected & Submitted)
  val approved: Int = 0,
  val collected: Int = 0,
  val query: Int = 0,
  val returnToSource: Int = 0,
  val declined: Int = 0,
  val condition: Int = 0,
  val pendingDocumentsCount: Int = 0,
  val activeY: Int = 0,
  val activeN: Int = 0,
  val activeC: Int = 0,
  val creditCardCount: Int = 0,
  val corporateCardCount: Int = 0,
  val b2bCount: Int = 0,
  val stcCreditCardCount: Int = 0,
  val stcCorporateCardCount: Int = 0,
  val stcB2bCount: Int = 0
)

data class RmPerformanceRow(
  val rmCode: String,
  val rmName: String,
  val stats: KpiStats,
  val target: com.example.data.model.RmTargetEntity? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  val authRepository = AuthRepository(database)
  val eblRepository = EblRepository(database, authRepository, application)

  val currentUser: StateFlow<UserEntity?> = authRepository.currentUser

  val appCustomName: StateFlow<String> = eblRepository.getAppCustomNameFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, "RM File Management Suite")

  private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val authPrefs by lazy {
    getApplication<Application>().getSharedPreferences("ebl_auth_preferences", android.content.Context.MODE_PRIVATE)
  }

  val lastLoggedRmCode = MutableStateFlow("")

  init {
    val cachedRm = authPrefs.getString("last_logged_rm_code", "") ?: ""
    if (cachedRm.isNotBlank()) {
      lastLoggedRmCode.value = cachedRm
    }
    viewModelScope.launch {
      val dbLastRm = authRepository.getLastLoggedRmCode()
      if (dbLastRm.isNotBlank()) {
        lastLoggedRmCode.value = dbLastRm
        authPrefs.edit().putString("last_logged_rm_code", dbLastRm).apply()
      }
    }
  }

  // Navigation backstack support
  private val screenBackstack = mutableListOf<Screen>()

  private val _uiMessage = MutableSharedFlow<String>()
  val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

  // Search & Filter state
  val searchQuery = MutableStateFlow("")
  val selectedTimeFilter = MutableStateFlow(DateUtils.TimeFilter.ALL_TIME)
  val selectedProductFilter = MutableStateFlow("All")
  val selectedAppStatusFilter = MutableStateFlow("All")
  val selectedActiveStatusFilter = MutableStateFlow("All")
  val selectedCpvStatusFilter = MutableStateFlow("All")
  val selectedRmCodeFilter = MutableStateFlow("All")
  val showDeletedFilesOnly = MutableStateFlow(false)
  val pendingDocsOnlyFilter = MutableStateFlow(false)

  // Raw authorized files
  private val authorizedFilesFlow = eblRepository.getAllFilesIncludingDeletedFlow()
  val allFiles: StateFlow<List<CustomerFileEntity>> = authorizedFilesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Important Documents Flow for all users
  val importantDocuments: StateFlow<List<com.example.data.model.ImportantDocumentEntity>> = eblRepository.getAllImportantDocumentsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Filtered files according to user search & filters
  val filteredFiles: StateFlow<List<CustomerFileEntity>> = combine(
    authorizedFilesFlow,
    searchQuery,
    selectedTimeFilter,
    selectedProductFilter,
    selectedAppStatusFilter,
    selectedActiveStatusFilter,
    selectedCpvStatusFilter,
    selectedRmCodeFilter,
    showDeletedFilesOnly,
    pendingDocsOnlyFilter
  ) { args ->
    val files = args[0] as List<CustomerFileEntity>
    val query = (args[1] as String).trim().lowercase()
    val timeFilter = args[2] as DateUtils.TimeFilter
    val prodFilter = args[3] as String
    val appStatFilter = args[4] as String
    val actStatFilter = args[5] as String
    val cpvStatFilter = args[6] as String
    val rmFilter = args[7] as String
    val deletedOnly = args[8] as Boolean
    val pendingDocsOnly = args[9] as Boolean

    files.filter { file ->
      // Pending Docs Backlog filter
      if (pendingDocsOnly && file.pendingDocuments.isBlank()) {
        return@filter false
      }

      // Soft deletion filter
      if (deletedOnly) {
        if (!file.isDeleted) return@filter false
      } else {
        if (file.isDeleted) return@filter false
      }

      // Time filter
      if (!DateUtils.matchesTimeFilter(file.updatedAt, timeFilter)) {
        return@filter false
      }

      // Dropdown filters
      if (prodFilter != "All" && !file.productType.equals(prodFilter, ignoreCase = true)) {
        return@filter false
      }
      if (appStatFilter != "All" && !file.applicationStatus.equals(appStatFilter, ignoreCase = true)) {
        return@filter false
      }
      if (actStatFilter != "All" && !file.activeStatus.equals(actStatFilter, ignoreCase = true)) {
        return@filter false
      }
      if (cpvStatFilter != "All" && !file.cpvStatus.equals(cpvStatFilter, ignoreCase = true)) {
        return@filter false
      }
      if (rmFilter != "All" && !file.assignedRmCode.equals(rmFilter, ignoreCase = true)) {
        return@filter false
      }

      // Search matching: name, mobile, company, fileId, address, product, status, rmCode
      if (query.isNotEmpty()) {
        val matches = file.customerName.lowercase().contains(query) ||
          file.companyName.lowercase().contains(query) ||
          file.mobile.contains(query) ||
          file.altMobile.contains(query) ||
          file.fileId.lowercase().contains(query) ||
          file.officeAddress.lowercase().contains(query) ||
          file.productType.lowercase().contains(query) ||
          file.applicationStatus.lowercase().contains(query) ||
          file.assignedRmCode.lowercase().contains(query)
        if (!matches) return@filter false
      }

      true
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allRms: StateFlow<List<UserEntity>> = eblRepository.getAllRmsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allUsers: StateFlow<List<UserEntity>> = eblRepository.getAllUsersFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentLocationLogs: StateFlow<List<com.example.data.model.UserLocationLogEntity>> = eblRepository.getRecentLocationLogsFlow(100)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val auditLogs: StateFlow<List<AuditLogEntity>> = eblRepository.getAuditLogsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appSettings: StateFlow<List<AppSettingEntity>> = eblRepository.getAppSettingsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val productTypes: StateFlow<List<String>> = appSettings.map { list ->
    val raw = list.find { it.settingKey == "PRODUCT_TYPES" }?.settingValue
    if (!raw.isNullOrBlank()) {
      raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    } else {
      listOf("Credit Card", "B2B", "Corporate Card", "Split", "Limit Enhancement")
    }
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    listOf("Credit Card", "B2B", "Corporate Card", "Split", "Limit Enhancement")
  )

  val pendingDocsOptions: StateFlow<List<String>> = appSettings.map { list ->
    val raw = list.find { it.settingKey == "PENDING_DOCS_LIST" }?.settingValue
    if (!raw.isNullOrBlank()) {
      raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    } else {
      listOf("NID", "TIN", "Office ID", "Salary Certificate", "Account Statement (6 Months)", "BIN", "Trade License 2024-25", "Trade License 2025-26", "Trade License 2026-27", "Loan Certificate", "Card Statement (Month)", "Card Copy")
    }
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    listOf("NID", "TIN", "Office ID", "Salary Certificate", "Account Statement (6 Months)", "BIN", "Trade License 2024-25", "Trade License 2025-26", "Trade License 2026-27", "Loan Certificate", "Card Statement (Month)", "Card Copy")
  )

  val syncStatus: StateFlow<SyncStatusEntity?> = eblRepository.getSyncStatusFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // KPI Stats for current filter and current user
  val kpiStats: StateFlow<KpiStats> = combine(
    authorizedFilesFlow,
    selectedTimeFilter
  ) { files, timeFilter ->
    val activeFiles = files.filter { !it.isDeleted && DateUtils.matchesTimeFilter(it.updatedAt, timeFilter) }
    computeStats(activeFiles)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KpiStats())

  val allTargets: StateFlow<List<com.example.data.model.RmTargetEntity>> = database.rmTargetDao().getAllTargetsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // RM-wise performance breakdown for Admin / Mentor
  val rmPerformanceList: StateFlow<List<RmPerformanceRow>> = combine(
    authorizedFilesFlow,
    allRms,
    selectedTimeFilter,
    allTargets
  ) { files, rms, timeFilter, targets ->
    val activeFiles = files.filter { !it.isDeleted && DateUtils.matchesTimeFilter(it.updatedAt, timeFilter) }
    rms.map { rm ->
      val rmFiles = activeFiles.filter { it.assignedRmCode == rm.rmCode }
      val target = targets.find { it.rmCode == rm.rmCode }
      RmPerformanceRow(
        rmCode = rm.rmCode,
        rmName = rm.name,
        stats = computeStats(rmFiles),
        target = target
      )
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // SMS Notifications State
  @OptIn(ExperimentalCoroutinesApi::class)
  val rmSmsNotifications: StateFlow<List<SmsNotificationEntity>> = currentUser.flatMapLatest { user ->
    if (user != null && user.role == "RM") {
      eblRepository.getSmsForRmFlow(user.rmCode)
    } else {
      emptyFlow()
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val unreadSmsCount: StateFlow<Int> = currentUser.flatMapLatest { user ->
    if (user != null && user.role == "RM") {
      eblRepository.getUnreadSmsCountFlow(user.rmCode)
    } else {
      emptyFlow()
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val allSmsNotifications: StateFlow<List<SmsNotificationEntity>> = eblRepository.getAllSmsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun markSmsAsRead(id: Long) {
    viewModelScope.launch {
      eblRepository.markSmsAsRead(id)
    }
  }

  fun markAllSmsAsRead() {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      eblRepository.markAllSmsAsReadForRm(user.rmCode)
    }
  }

  fun clearSmsForCurrentRm() {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      eblRepository.clearSmsForRm(user.rmCode)
    }
  }

  fun clearAllSms() {
    viewModelScope.launch {
      eblRepository.clearAllSms()
    }
  }

  fun deleteSms(id: Long) {
    viewModelScope.launch {
      eblRepository.deleteSms(id)
    }
  }

  val isRealtimeAutoSyncEnabled = MutableStateFlow(true)
  val isSyncingInProgress = MutableStateFlow(false)

  fun toggleRealtimeAutoSync(enabled: Boolean) {
    isRealtimeAutoSyncEnabled.value = enabled
  }

  init {
    viewModelScope.launch {
      DatabaseInitializer.initializeIfNeeded(database)
      database.auditLogDao().purgeNonAuthLogs()
    }

    // Continuous Real-Time Bi-Directional Auto-Sync Loop
    // Automatically synchronizes both ways with Google Sheets in the background every 0.05 seconds (50ms) (All-Time Live)
    viewModelScope.launch {
      while (true) {
        delay(50) // 0.05 second (50ms) continuous live sync interval
        if (isRealtimeAutoSyncEnabled.value && !isSyncingInProgress.value) {
          try {
            isSyncingInProgress.value = true
            // 1. First push local updates, deletions, and RM credentials
            eblRepository.triggerGoogleSheetsSync()

            // 2. Then pull external changes from Google Sheets
            eblRepository.pullDataFromGoogleSheets()
          } catch (_: Exception) {
          } finally {
            isSyncingInProgress.value = false
          }
        }
      }
    }
  }

  private fun computeStats(files: List<CustomerFileEntity>): KpiStats {
    var collected = 0
    var submitted = 0
    var analystReceive = 0
    var approved = 0
    var declined = 0
    var query = 0
    var returnToSource = 0
    var condition = 0
    var stc = 0
    var pendingDocsCount = 0
    var activeY = 0
    var activeN = 0
    var activeC = 0
    var creditCardCount = 0
    var corporateCardCount = 0
    var b2bCount = 0
    var stcCreditCardCount = 0
    var stcCorporateCardCount = 0
    var stcB2bCount = 0

    for (f in files) {
      val p = f.productType.trim().lowercase()
      val isCorp = p.contains("corporate")
      val isCredit = p.contains("credit") && !isCorp
      val isB2b = p.contains("b2b")

      if (isCorp) corporateCardCount++
      else if (isCredit) creditCardCount++
      if (isB2b) b2bCount++

      val statusLower = f.applicationStatus.trim().lowercase()
      val isStc = statusLower == "stc"

      if (isStc) {
        stc++
        if (isCorp) stcCorporateCardCount++
        else if (isCredit) stcCreditCardCount++
        if (isB2b) stcB2bCount++
      }

      when (statusLower) {
        "collected" -> collected++
        "submitted" -> submitted++
        "analyst receive", "analyst received" -> analystReceive++
        "approved" -> approved++
        "declined" -> declined++
        "query" -> query++
        "return to source", "rts" -> returnToSource++
        "condition" -> condition++
      }

      when (f.activeStatus.uppercase()) {
        "Y" -> activeY++
        "N" -> activeN++
        "C" -> activeC++
      }

      if (f.pendingDocuments.isNotBlank()) {
        val count = f.pendingDocuments.split(",").filter { it.isNotBlank() }.size
        pendingDocsCount += count
      }
    }

    return KpiStats(
      totalFiles = files.size,
      stc = stc,
      submitted = submitted,
      analystReceive = analystReceive,
      approved = approved,
      collected = collected,
      query = query,
      returnToSource = returnToSource,
      declined = declined,
      condition = condition,
      pendingDocumentsCount = pendingDocsCount,
      activeY = activeY,
      activeN = activeN,
      activeC = activeC,
      creditCardCount = creditCardCount,
      corporateCardCount = corporateCardCount,
      b2bCount = b2bCount,
      stcCreditCardCount = stcCreditCardCount,
      stcCorporateCardCount = stcCorporateCardCount,
      stcB2bCount = stcB2bCount
    )
  }

  fun navigateTo(screen: Screen) {
    if (_currentScreen.value != screen) {
      screenBackstack.add(_currentScreen.value)
      _currentScreen.value = screen
    }
  }

  fun navigateBack(): Boolean {
    if (screenBackstack.isNotEmpty()) {
      val prev = screenBackstack.removeAt(screenBackstack.size - 1)
      _currentScreen.value = prev
      return true
    }
    return false
  }

  fun isBiometricEnabled(rmCode: String): Boolean {
    val clean = rmCode.trim().uppercase()
    if (clean.isBlank()) return false
    if (authPrefs.contains("fingerprint_enabled_$clean")) {
      return authPrefs.getBoolean("fingerprint_enabled_$clean", false)
    }
    val savedSetting = appSettings.value.find { it.settingKey == "fingerprint_enabled_$clean" }?.settingValue
    // Automatically enable for any user once they have logged in with their password on this device
    return if (savedSetting != null) savedSetting == "true" else isPasswordLoginVerified(clean)
  }

  fun isPasswordLoginVerified(rmCode: String): Boolean {
    val clean = rmCode.trim().uppercase()
    if (clean.isBlank()) return false
    val lastPwId = authPrefs.getString("last_password_logged_id", "")?.trim()?.uppercase() ?: ""
    if (clean == lastPwId && lastPwId.isNotBlank()) return true
    if (authPrefs.getBoolean("password_login_verified_$clean", false)) return true
    return appSettings.value.find { it.settingKey == "password_login_verified_$clean" }?.settingValue == "true"
  }

  fun setBiometricEnabled(rmCode: String, enabled: Boolean, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    val clean = rmCode.trim().uppercase()
    authPrefs.edit().putBoolean("fingerprint_enabled_$clean", enabled).apply()
    viewModelScope.launch {
      database.appSettingDao().insertOrUpdateSetting(
        AppSettingEntity(
          settingKey = "fingerprint_enabled_$clean",
          settingValue = if (enabled) "true" else "false",
          updatedBy = clean,
          updatedAt = DateUtils.currentDhakaMillis()
        )
      )
      if (enabled) {
        authPrefs.edit().putString("last_logged_rm_code", clean).apply()
        database.appSettingDao().insertOrUpdateSetting(
          AppSettingEntity(
            settingKey = "last_logged_rm_code",
            settingValue = clean,
            updatedBy = clean,
            updatedAt = DateUtils.currentDhakaMillis()
          )
        )
      }
      onResult(true, null)
    }
  }

  fun login(
    usernameInput: String,
    passwordInput: String,
    latitude: Double? = null,
    longitude: Double? = null,
    address: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      var res = authRepository.login(usernameInput, passwordInput, latitude, longitude, address)
      if (res.isFailure) {
        // If login failed, pull fresh data (RMs, passwords, settings) from Google Sheets and retry!
        // This ensures ANY RM or Admin/Mentor logging in on ANY phone succeeds immediately!
        try {
          eblRepository.pullDataFromGoogleSheets()
          res = authRepository.login(usernameInput, passwordInput, latitude, longitude, address)
        } catch (_: Exception) {}
      }
      res.onSuccess { user ->
        val cleanCode = user.rmCode.trim().uppercase()
        lastLoggedRmCode.value = cleanCode
        // Record password verification AND automatically enable fingerprint for this user in SharedPreferences and DB!
        authPrefs.edit()
          .putBoolean("password_login_verified_$cleanCode", true)
          .putBoolean("fingerprint_enabled_$cleanCode", true)
          .putString("last_logged_rm_code", cleanCode)
          .putString("last_password_logged_id", cleanCode)
          .apply()
        try {
          val now = DateUtils.currentDhakaMillis()
          database.appSettingDao().insertOrUpdateSetting(
            AppSettingEntity(
              settingKey = "password_login_verified_$cleanCode",
              settingValue = "true",
              updatedBy = cleanCode,
              updatedAt = now
            )
          )
          database.appSettingDao().insertOrUpdateSetting(
            AppSettingEntity(
              settingKey = "fingerprint_enabled_$cleanCode",
              settingValue = "true",
              updatedBy = cleanCode,
              updatedAt = now
            )
          )
          database.appSettingDao().insertOrUpdateSetting(
            AppSettingEntity(
              settingKey = "last_logged_rm_code",
              settingValue = cleanCode,
              updatedBy = cleanCode,
              updatedAt = now
            )
          )
          database.appSettingDao().insertOrUpdateSetting(
            AppSettingEntity(
              settingKey = "last_password_logged_id",
              settingValue = cleanCode,
              updatedBy = cleanCode,
              updatedAt = now
            )
          )
        } catch (_: Exception) {}

        // Pull latest sheet data immediately upon successful login so all files, stats and SMS are fresh!
        viewModelScope.launch {
          try {
            eblRepository.pullDataFromGoogleSheets()
          } catch (_: Exception) {}
        }
        screenBackstack.clear()
        when (user.role) {
          "RM" -> _currentScreen.value = Screen.RmDashboard
          "ADMIN" -> _currentScreen.value = Screen.AdminDashboard
          "MENTOR" -> _currentScreen.value = Screen.MentorDashboard
          else -> _currentScreen.value = Screen.RmDashboard
        }
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message ?: "Authentication failed.")
      }
    }
  }

  fun loginWithBiometrics(
    rmCodeInput: String,
    latitude: Double? = null,
    longitude: Double? = null,
    address: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    val clean = rmCodeInput.trim().uppercase()
    val lastPwId = authPrefs.getString("last_password_logged_id", "")?.trim()?.uppercase() ?: ""
    val isVerified = (clean == lastPwId && lastPwId.isNotBlank()) || isPasswordLoginVerified(clean)
    if (!isVerified) {
      onResult(false, "এই আইডির জন্য একবার পাসওয়ার্ড দিয়ে লগইন বাধ্যতামূলক। একবার পাসওয়ার্ড দিয়ে ঢুকলে পরবর্তীতে ফিঙ্গারপ্রিন্ট স্বয়ংক্রিয়ভাবে সক্রিয় হবে।")
      return
    }

    viewModelScope.launch {
      var res = authRepository.loginWithBiometrics(rmCodeInput, latitude, longitude, address)
      if (res.isFailure) {
        try {
          eblRepository.pullDataFromGoogleSheets()
          res = authRepository.loginWithBiometrics(rmCodeInput, latitude, longitude, address)
        } catch (_: Exception) {}
      }
      res.onSuccess { user ->
        lastLoggedRmCode.value = user.rmCode
        viewModelScope.launch {
          try {
            eblRepository.pullDataFromGoogleSheets()
          } catch (_: Exception) {}
        }
        screenBackstack.clear()
        when (user.role) {
          "RM" -> _currentScreen.value = Screen.RmDashboard
          "ADMIN" -> _currentScreen.value = Screen.AdminDashboard
          "MENTOR" -> _currentScreen.value = Screen.MentorDashboard
          else -> _currentScreen.value = Screen.RmDashboard
        }
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message ?: "Fingerprint login failed.")
      }
    }
  }

  fun setAppCustomName(name: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.setAppCustomName(name)
      res.onSuccess {
        _uiMessage.emit("Universal App name set to '$name'.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun approveRm(rmCode: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.approveRm(rmCode)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode approved and activated successfully.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun rejectRm(rmCode: String, reason: String = "", onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.rejectRm(rmCode, reason)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode rejected.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      val user = authRepository.currentUser.value
      if (user != null) {
        val clean = user.rmCode.trim().uppercase()
        lastLoggedRmCode.value = clean
        authPrefs.edit().putString("last_logged_rm_code", clean).apply()
      }
      authRepository.logout()
      screenBackstack.clear()
      _currentScreen.value = Screen.Login
      _uiMessage.emit("You have been signed out safely.")
    }
  }

  fun changePassword(oldPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val currentUser = authRepository.currentUser.value
      val res = authRepository.changePassword(oldPass, newPass)
      res.onSuccess {
        if (currentUser != null) {
          val clean = currentUser.rmCode.trim().uppercase()
          // Invalidate password login verification and fingerprint bypass for this ID
          authPrefs.edit()
            .putBoolean("password_login_verified_$clean", false)
            .putBoolean("fingerprint_enabled_$clean", false)
            .apply()
          try {
            val now = DateUtils.currentDhakaMillis()
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("password_login_verified_$clean", "false", clean, now)
            )
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("fingerprint_enabled_$clean", "false", clean, now)
            )
            database.appSettingDao().insertOrUpdateSetting(
              AppSettingEntity("RM_PASS_UPDATED_AT_$clean", now.toString(), clean, now)
            )
          } catch (_: Exception) {}

          // Synchronize new password to Google Sheets
          val updatedUser = database.userDao().getUser(clean)
          if (updatedUser != null) {
            eblRepository.syncRmPasswordToGoogleSheets(clean, updatedUser.passwordHash, updatedUser.salt)
            eblRepository.triggerGoogleSheetsSync()
          }
        }
        onResult(true, "Password changed successfully! You must use this new password on your next login.")
      }.onFailure { err ->
        onResult(false, err.message ?: "Failed to change password.")
      }
    }
  }

  fun saveCustomerFile(
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
    serialNumber: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.saveCustomerFile(
        fileId = fileId,
        customerName = customerName,
        companyName = companyName,
        officeAddress = officeAddress,
        mobile = mobile,
        altMobile = altMobile,
        email = email,
        productType = productType,
        applicationStatus = applicationStatus,
        activeStatus = activeStatus,
        assignedRmCode = assignedRmCode,
        ccNumber = ccNumber,
        pendingDocuments = pendingDocuments,
        remarks = remarks,
        cpvStatus = cpvStatus,
        cpvDate = cpvDate,
        cpvAddress = cpvAddress,
        cpvRemarks = cpvRemarks,
        cpvPhotoUri = cpvPhotoUri,
        cpvSupportingDocUri = cpvSupportingDocUri,
        submissionLatitude = submissionLatitude,
        submissionLongitude = submissionLongitude,
        submissionAddress = submissionAddress,
        serialNumber = serialNumber
      )
      res.onSuccess { entity ->
        _uiMessage.emit("File ${entity.fileId} saved successfully.")
        onResult(true, entity.fileId)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun setGoogleSheetUrl(urlOrId: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.setGoogleSheetUrl(urlOrId)
      res.onSuccess { id ->
        _uiMessage.emit("Linked to Google Sheet: $id")
        onResult(true, "Successfully linked to Google Sheet ($id). Real-time auto-sync is active!")
      }.onFailure { err ->
        onResult(false, err.message ?: "Failed to set Google Sheet URL.")
      }
    }
  }

  fun deleteFile(fileId: String) {
    viewModelScope.launch {
      val res = eblRepository.softDeleteCustomerFile(fileId)
      res.onSuccess {
        _uiMessage.emit("File $fileId deleted.")
      }.onFailure { err ->
        _uiMessage.emit("Delete failed: ${err.message}")
      }
    }
  }

  fun restoreFile(fileId: String) {
    viewModelScope.launch {
      val res = eblRepository.restoreCustomerFile(fileId)
      res.onSuccess {
        _uiMessage.emit("File $fileId restored successfully.")
      }.onFailure { err ->
        _uiMessage.emit("Restore failed: ${err.message}")
      }
    }
  }

  fun permanentDeleteFile(fileId: String) {
    viewModelScope.launch {
      val res = eblRepository.permanentDeleteCustomerFile(fileId)
      res.onSuccess {
        _uiMessage.emit("File $fileId permanently deleted by Mentor.")
      }.onFailure { err ->
        _uiMessage.emit("Failed: ${err.message}")
      }
    }
  }

  fun saveImportantDocument(
    title: String,
    category: String,
    description: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String,
    storagePath: String,
    docId: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.saveImportantDocument(
        docId = docId,
        title = title,
        category = category,
        description = description,
        fileName = fileName,
        fileType = fileType,
        fileSizeBytes = fileSizeBytes,
        fileUri = fileUri,
        storagePath = storagePath
      )
      res.onSuccess {
        _uiMessage.emit("ডকুমেন্ট '${it.title}' সফলভাবে সেভ করা হয়েছে।")
        onResult(true, null)
      }.onFailure {
        onResult(false, it.message ?: "Failed to save document.")
      }
    }
  }

  fun deleteImportantDocument(docId: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.deleteImportantDocument(docId)
      res.onSuccess {
        _uiMessage.emit("ডকুমেন্ট সফলভাবে মুছে ফেলা হয়েছে।")
        onResult(true, null)
      }.onFailure {
        onResult(false, it.message ?: "Failed to delete document.")
      }
    }
  }

  fun deleteRmProfile(rmCode: String, onResult: ((Boolean, String?) -> Unit)? = null) {
    viewModelScope.launch {
      val res = eblRepository.deleteRmProfile(rmCode)
      res.onSuccess {
        _uiMessage.emit("RM Profile $rmCode deleted successfully.")
        onResult?.invoke(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to delete RM: ${err.message}")
        onResult?.invoke(false, err.message)
      }
    }
  }

  fun reassignCustomerFileRm(fileId: String, newRmCode: String, onResult: ((Boolean, String?) -> Unit)? = null) {
    viewModelScope.launch {
      val res = eblRepository.reassignCustomerFileRm(fileId, newRmCode)
      res.onSuccess {
        _uiMessage.emit("File $fileId successfully reassigned to RM $newRmCode.")
        onResult?.invoke(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Reassign failed: ${err.message}")
        onResult?.invoke(false, err.message)
      }
    }
  }

  fun updateUserLocation(
    rmCode: String,
    lat: Double,
    lng: Double,
    address: String,
    sourceAction: String = "GPS_AUTO_DETECT",
    fileId: String? = null
  ) {
    viewModelScope.launch {
      eblRepository.updateUserLocation(rmCode, lat, lng, address, sourceAction, fileId)
    }
  }

  fun createRm(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    initialPassword: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.createRm(rmCode, name, mobile, email, officeAddress, initialPassword)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode created and activated.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun updateRm(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    newPassword: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateRm(rmCode, name, mobile, email, officeAddress, newPassword)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode details updated.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun setRmTargets(
    rmCode: String,
    creditCardTarget: Int,
    corporateCardTarget: Int,
    b2bTarget: Int,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      val res = eblRepository.setRmTargets(rmCode, creditCardTarget, corporateCardTarget, b2bTarget)
      res.onSuccess {
        _uiMessage.emit("Monthly targets updated for RM $rmCode.")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to set targets: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun setRmStatus(rmCode: String, status: String) {
    viewModelScope.launch {
      val res = eblRepository.setRmStatus(rmCode, status)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode status set to $status.")
      }.onFailure { err ->
        _uiMessage.emit("Failed: ${err.message}")
      }
    }
  }

  fun resetRmPassword(rmCode: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.resetRmPassword(rmCode, newPassword)
      res.onSuccess {
        _uiMessage.emit("Password reset successfully for RM $rmCode.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun saveUniversalChecklistSettings(
    headerTemplate: String,
    regardsTemplate: String,
    corporateDocsJson: String = "",
    enhancementDocsJson: String = "",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      val res = eblRepository.saveUniversalChecklistSettings(headerTemplate, regardsTemplate, corporateDocsJson, enhancementDocsJson)
      res.onSuccess {
        _uiMessage.emit("Universal checklist templates saved and synced!")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to save checklist settings: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun updateRmProfile(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateRm(rmCode, name, mobile, email, officeAddress)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode updated successfully.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun getTargetForRmFlow(rmCode: String) = eblRepository.getTargetForRmFlow(rmCode)

  fun triggerSyncNow() {
    viewModelScope.launch {
      _uiMessage.emit("Starting Google Sheets synchronization...")
      val res = eblRepository.triggerGoogleSheetsSync()
      res.onSuccess { msg ->
        _uiMessage.emit(msg)
      }.onFailure { err ->
        _uiMessage.emit("Sync Error: ${err.message}")
      }
    }
  }

  fun triggerGoogleSheetsSync(onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.triggerGoogleSheetsSync()
      res.onSuccess { msg ->
        _uiMessage.emit(msg)
        onResult(true, msg)
      }.onFailure { err ->
        _uiMessage.emit("Sync Error: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun pullDataFromGoogleSheets(onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.pullDataFromGoogleSheets()
      res.onSuccess { msg ->
        _uiMessage.emit(msg)
        onResult(true, msg)
      }.onFailure { err ->
        _uiMessage.emit("Pull Error: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun updateAppsScriptConfig(url: String, secretKey: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.updateAppsScriptConfig(url, secretKey)
      res.onSuccess {
        _uiMessage.emit("Google Sheets connector config saved.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun updateSetting(key: String, value: String) {
    viewModelScope.launch {
      val res = eblRepository.updateAppSetting(key, value)
      res.onSuccess {
        _uiMessage.emit("Setting '$key' updated.")
      }.onFailure { err ->
        _uiMessage.emit("Failed to update setting: ${err.message}")
      }
    }
  }

  fun addAttachment(
    fileId: String,
    category: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String
  ) {
    viewModelScope.launch {
      val res = eblRepository.addAttachment(fileId, category, fileName, fileType, fileSizeBytes, fileUri)
      res.onSuccess {
        _uiMessage.emit("Attachment '$fileName' added.")
      }.onFailure { err ->
        _uiMessage.emit("Failed to add attachment: ${err.message}")
      }
    }
  }

  fun deleteAttachment(attachmentId: String, fileId: String) {
    viewModelScope.launch {
      val res = eblRepository.deleteAttachment(attachmentId, fileId)
      res.onSuccess {
        _uiMessage.emit("Attachment removed.")
      }.onFailure { err ->
        _uiMessage.emit("Failed to remove attachment: ${err.message}")
      }
    }
  }

  fun resetFilters() {
    searchQuery.value = ""
    selectedTimeFilter.value = DateUtils.TimeFilter.ALL_TIME
    selectedProductFilter.value = "All"
    selectedAppStatusFilter.value = "All"
    selectedActiveStatusFilter.value = "All"
    selectedCpvStatusFilter.value = "All"
    selectedRmCodeFilter.value = "All"
    showDeletedFilesOnly.value = false
    pendingDocsOnlyFilter.value = false
  }

  // ==========================================
  // Communication & Internet Calling Features
  // ==========================================
  private val _callState = MutableStateFlow<CallUiState>(CallUiState.Idle)
  val callState: StateFlow<CallUiState> = _callState.asStateFlow()

  private var callTimerJob: kotlinx.coroutines.Job? = null

  val teamHubMessages: StateFlow<List<com.example.data.model.ChatMessageEntity>> = eblRepository.getTeamHubMessagesFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val teamEvents: StateFlow<List<com.example.data.model.TeamEventEntity>> = eblRepository.getAllTeamEventsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun sendChatMessage(
    text: String,
    recipientRmCode: String? = null,
    messageType: String = "TEXT",
    eventId: String? = null,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    if (text.isBlank()) return
    viewModelScope.launch {
      val res = eblRepository.sendChatMessage(text, recipientRmCode, messageType, eventId)
      res.onSuccess {
        try {
          com.example.util.NotificationHelper.sendIncomingSmsNotification(
            getApplication(),
            currentUser.value?.name ?: "EBL Hub",
            text
          )
        } catch (_: Exception) {}
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun deleteChatMessage(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.deleteChatMessage(id)
      res.onSuccess {
        _uiMessage.emit("Message deleted.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun createTeamEvent(
    title: String,
    description: String,
    targetDate: String,
    allowedFields: String = "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.createTeamEvent(title, description, targetDate, allowedFields)
      res.onSuccess {
        _uiMessage.emit("✓ Event '${it.title}' created & shared with all team members!")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun submitEventResponse(
    eventId: String,
    filesCount: Int,
    requestedDate: String,
    location: String,
    remarks: String,
    customerEntriesJson: String = "",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.submitEventResponse(eventId, filesCount, requestedDate, location, remarks, customerEntriesJson)
      res.onSuccess {
        _uiMessage.emit("✓ Your event response details have been submitted!")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun getEventResponses(eventId: String, onResult: (List<com.example.data.model.EventResponseEntity>) -> Unit) {
    viewModelScope.launch {
      val list = eblRepository.getResponsesForEvent(eventId)
      onResult(list)
    }
  }

  // Internet Calling
  fun startCall(targetUser: com.example.data.model.UserEntity) {
    callTimerJob?.cancel()
    _callState.value = CallUiState.Calling(targetUser)
    com.example.util.CallingService.playRingtone(getApplication())

    // Automatically transition to Connected after 2.5s ringing
    callTimerJob = viewModelScope.launch {
      kotlinx.coroutines.delay(2500)
      com.example.util.CallingService.stopRingtone()
      com.example.util.CallingService.playConnectedTone()
      var duration = 0
      while (true) {
        _callState.value = CallUiState.Connected(targetUser, duration, isMuted = false, isSpeakerOn = false)
        kotlinx.coroutines.delay(1000)
        duration++
      }
    }
  }

  fun startGroupCall(title: String) {
    callTimerJob?.cancel()
    val myUser = currentUser.value
    val allActiveUsers = allRms.value.take(4)
    val participants = if (myUser != null) listOf(myUser) + allActiveUsers.filter { it.rmCode != myUser.rmCode } else allActiveUsers

    com.example.util.CallingService.playRingtone(getApplication())
    callTimerJob = viewModelScope.launch {
      kotlinx.coroutines.delay(1500)
      com.example.util.CallingService.stopRingtone()
      com.example.util.CallingService.playConnectedTone()
      var duration = 0
      while (true) {
        _callState.value = CallUiState.GroupCall(
          title = title.ifBlank { "Team Live Huddle" },
          participants = participants,
          durationSeconds = duration,
          isMuted = false,
          isSpeakerOn = true
        )
        kotlinx.coroutines.delay(1000)
        duration++
      }
    }

    // Broadcast into chat
    sendChatMessage(
      text = "📞 Active Group Call: '$title' started by ${myUser?.name ?: "Team"}. Tap to join!",
      recipientRmCode = null,
      messageType = "CALL_LOG"
    )
  }

  fun toggleMute() {
    val current = _callState.value
    if (current is CallUiState.Connected) {
      _callState.value = current.copy(isMuted = !current.isMuted)
    } else if (current is CallUiState.GroupCall) {
      _callState.value = current.copy(isMuted = !current.isMuted)
    }
  }

  fun toggleSpeaker() {
    val current = _callState.value
    if (current is CallUiState.Connected) {
      _callState.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    } else if (current is CallUiState.GroupCall) {
      _callState.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    }
  }

  fun endCall() {
    callTimerJob?.cancel()
    callTimerJob = null
    com.example.util.CallingService.stopRingtone()
    com.example.util.CallingService.playEndCallTone()
    val current = _callState.value
    if (current is CallUiState.Connected) {
      val min = current.durationSeconds / 60
      val sec = current.durationSeconds % 60
      val timeStr = String.format("%02d:%02d", min, sec)
      sendChatMessage(
        text = "📞 Voice Call with ${current.targetUser.name} (${current.targetUser.rmCode}) ended. Duration: $timeStr",
        recipientRmCode = null,
        messageType = "CALL_LOG"
      )
    }
    _callState.value = CallUiState.Idle
  }
}

sealed class CallUiState {
  object Idle : CallUiState()
  data class Calling(val targetUser: com.example.data.model.UserEntity) : CallUiState()
  data class Connected(
    val targetUser: com.example.data.model.UserEntity,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
  ) : CallUiState()
  data class GroupCall(
    val title: String,
    val participants: List<com.example.data.model.UserEntity>,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
  ) : CallUiState()
}
