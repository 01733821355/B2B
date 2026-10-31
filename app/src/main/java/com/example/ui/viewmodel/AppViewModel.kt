package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.flow.Flow
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
import com.example.ui.theme.AppThemeConfig
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
  object CompanyProfile : Screen()
  data class FieldRoutePlanner(val initialSelectedFileIds: List<String> = emptyList()) : Screen()
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

  val appCustomIconUrl: StateFlow<String> = database.appSettingDao().getSettingFlow("app_custom_icon_url")
    .map { it?.settingValue ?: "" }
    .stateIn(viewModelScope, SharingStarted.Eagerly, "")

  val fileStatusOptions: StateFlow<List<String>> = eblRepository.getFileStatusOptionsFlow()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC")
    )

  private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val authPrefs by lazy {
    getApplication<Application>().getSharedPreferences("ebl_auth_preferences", android.content.Context.MODE_PRIVATE)
  }

  val lastLoggedRmCode = MutableStateFlow("")

  // Active Field Route Planner State
  val activeRouteCustomerIds = MutableStateFlow<List<String>>(emptyList())
  val completedRouteCustomerIds = MutableStateFlow<List<String>>(emptyList())

  init {
    val cachedRm = authPrefs.getString("last_logged_rm_code", "") ?: ""
    if (cachedRm.isNotBlank()) {
      lastLoggedRmCode.value = cachedRm
    }
    val cachedRoute = authPrefs.getString("active_field_route_ids", "") ?: ""
    if (cachedRoute.isNotBlank()) {
      activeRouteCustomerIds.value = cachedRoute.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    viewModelScope.launch {
      val dbLastRm = authRepository.getLastLoggedRmCode()
      if (dbLastRm.isNotBlank()) {
        lastLoggedRmCode.value = dbLastRm
        authPrefs.edit().putString("last_logged_rm_code", dbLastRm).apply()
      }
    }

    // Immediately pull latest data from Google Sheets on application launch
    viewModelScope.launch {
      try {
        eblRepository.pullDataFromGoogleSheets()
      } catch (_: Exception) {}
    }
  }

  // Application & File Status Options flow (Admin customizable, dynamically visible to all RMs)
  val applicationStatusOptions: StateFlow<List<String>> = eblRepository.getFileStatusOptionsFlow()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC")
    )

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

  // Multi-tab Company Profile Workbook Flow for all users (RM, Admin, Mentor)
  val companyWorkbook: StateFlow<com.example.data.model.CompanyWorkbookData> = eblRepository.getCompanyWorkbookFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.ExcelWorkbookParser.getDefaultSampleWorkbook())


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

  val allOfficers: StateFlow<List<UserEntity>> = eblRepository.getAllOfficersFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allMentors: StateFlow<List<UserEntity>> = eblRepository.getAllMentorsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allUsers: StateFlow<List<UserEntity>> = eblRepository.getAllUsersFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentLocationLogs: StateFlow<List<com.example.data.model.UserLocationLogEntity>> = eblRepository.getRecentLocationLogsFlow(100)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val auditLogs: StateFlow<List<AuditLogEntity>> = eblRepository.getAuditLogsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appSettings: StateFlow<List<AppSettingEntity>> = eblRepository.getAppSettingsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _manualThemeFlow = MutableStateFlow<String?>(
    try {
      getApplication<Application>().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        .getString("app_active_theme", null)
    } catch (_: Exception) { null }
  )

  val selectedThemeMode: StateFlow<String> = combine(appSettings, _manualThemeFlow) { list, manual ->
    val universal = list.find { it.settingKey.equals("APP_ACTIVE_THEME", ignoreCase = true) }?.settingValue
    if (!universal.isNullOrBlank()) {
      universal
    } else if (!manual.isNullOrBlank()) {
      manual
    } else {
      "AUTO"
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")

  val universalFontStyle: StateFlow<String> = appSettings.map { list ->
    list.find { it.settingKey == "UI_FONT_STYLE" }?.settingValue ?: "DEFAULT_SANS"
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT_SANS")

  val universalTextScale: StateFlow<String> = appSettings.map { list ->
    list.find { it.settingKey == "UI_TEXT_SCALE" }?.settingValue ?: "STANDARD"
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD")

  val universalFontColor: StateFlow<String> = appSettings.map { list ->
    list.find { it.settingKey == "UI_FONT_COLOR" }?.settingValue ?: "#0A192F"
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#0A192F")

  val activeThemeConfig: StateFlow<AppThemeConfig> = combine(
    selectedThemeMode,
    appSettings,
    universalFontColor
  ) { mode, settingsList, fontColorHex ->
    val effectiveMode = if (mode.equals("AUTO", true)) {
      ThemeManager.determineSeasonalTheme().id.code
    } else {
      mode
    }
    val customKey = "THEME_CUSTOM_COLORS_${effectiveMode.uppercase()}"
    val customJson = settingsList.find { it.settingKey == customKey }?.settingValue
    ThemeManager.getThemeById(effectiveMode, customJson, fontColorHex)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeManager.DefaultDayTheme)

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

  val selectedKpiQuarter = MutableStateFlow(DateUtils.getCurrentQuarter())
  val selectedKpiYear = MutableStateFlow(2026)

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
      // Requirement 3: On startup or new install, immediately pull real data from Google Sheets into the app
      // Data flows from sheet to app ("sheet thake data app a jabe") without pushing default app presets to sheet.
      try {
        eblRepository.pullDataFromGoogleSheets()
      } catch (_: Exception) {}
    }

    // Periodic Auto-Pull Loop (Data flows from Google Sheets to App)
    // Only pulls external changes from Google Sheets at a steady, reliable interval.
    // Does NOT push local preset data to Google Sheets unless explicitly requested by the user via button.
    viewModelScope.launch {
      while (true) {
        delay(1_500) // 1.5-second ultra-fast cross-device background pull interval
        if (isRealtimeAutoSyncEnabled.value && !isSyncingInProgress.value) {
          try {
            isSyncingInProgress.value = true
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

  fun setAppCustomIcon(iconUrl: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = runCatching { eblRepository.setAppCustomIcon(iconUrl) }
      res.onSuccess {
        _uiMessage.emit("Universal App Icon updated.")
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
    docDate: String? = null,
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
        storagePath = storagePath,
        docDate = docDate
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
    role: String = "RM",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.createRm(rmCode, name, mobile, email, officeAddress, initialPassword, role)
      res.onSuccess {
        _uiMessage.emit("${if (role.equals("MENTOR", true)) "Mentor" else "RM"} $rmCode created and activated.")
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
    role: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateRm(rmCode, name, mobile, email, officeAddress, newPassword, role)
      res.onSuccess {
        _uiMessage.emit("Officer $rmCode details updated.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun setThemeMode(themeCode: String) {
    _manualThemeFlow.value = themeCode
    try {
      getApplication<Application>().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        .edit().putString("app_active_theme", themeCode).apply()
    } catch (_: Exception) {}
    updateSetting("APP_ACTIVE_THEME", themeCode)
    viewModelScope.launch {
      eblRepository.syncSettingUpsertToGoogleSheets("APP_ACTIVE_THEME", themeCode)
    }
  }

  fun setUniversalFont(fontStyle: String, fontColorHex: String, textScale: String = "STANDARD") {
    updateSetting("UI_FONT_STYLE", fontStyle)
    updateSetting("UI_FONT_COLOR", fontColorHex)
    updateSetting("UI_TEXT_SCALE", textScale)
    viewModelScope.launch {
      eblRepository.syncSettingUpsertToGoogleSheets("UI_FONT_STYLE", fontStyle)
      eblRepository.syncSettingUpsertToGoogleSheets("UI_FONT_COLOR", fontColorHex)
      eblRepository.syncSettingUpsertToGoogleSheets("UI_TEXT_SCALE", textScale)
      _uiMessage.emit("Universal Font & Color updated across app.")
    }
  }

  fun saveCustomThemeColors(themeCode: String, primaryHex: String, secondaryHex: String, tertiaryHex: String, bgHex: String, surfaceHex: String) {
    val obj = org.json.JSONObject().apply {
      put("primary", primaryHex)
      put("secondary", secondaryHex)
      put("tertiary", tertiaryHex)
      put("background", bgHex)
      put("surface", surfaceHex)
    }
    updateSetting("THEME_CUSTOM_COLORS_${themeCode.uppercase()}", obj.toString())
  }

  fun resetThemeColors(themeCode: String) {
    viewModelScope.launch {
      eblRepository.deleteSetting("THEME_CUSTOM_COLORS_${themeCode.uppercase()}")
      _uiMessage.emit("Theme '$themeCode' reset to default colors.")
    }
  }

  fun setRmTargets(
    rmCode: String,
    creditCardTarget: Int,
    corporateCardTarget: Int,
    b2bTarget: Int,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    setRmQuarterTargets(
      rmCode = rmCode,
      quarter = selectedKpiQuarter.value,
      year = selectedKpiYear.value,
      creditCardTarget = creditCardTarget,
      corporateCardTarget = corporateCardTarget,
      b2bTarget = b2bTarget,
      onResult = onResult
    )
  }

  fun setRmQuarterTargets(
    rmCode: String,
    quarter: String,
    year: Int = 2026,
    creditCardTarget: Int,
    corporateCardTarget: Int,
    b2bTarget: Int,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      val res = eblRepository.setRmQuarterTargets(rmCode, quarter, year, creditCardTarget, corporateCardTarget, b2bTarget)
      res.onSuccess {
        _uiMessage.emit("Targets updated for RM $rmCode ($quarter $year).")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to set targets: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  val targetCriteriaOptions: StateFlow<List<String>> = eblRepository.getTargetCriteriaOptionsFlow()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = listOf("Credit Card", "Corporate Card", "B2B")
    )

  fun addTargetCriterion(name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.addTargetCriterion(name)
      res.onSuccess {
        _uiMessage.emit("New target criteria '$name' added successfully.")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to add target criteria: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun deleteTargetCriterion(name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.deleteTargetCriterion(name)
      res.onSuccess {
        _uiMessage.emit("Target criteria '$name' deleted.")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to delete target criteria: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun getRmCustomTargetsFlow(rmCode: String, quarter: String? = null): Flow<Map<String, Int>> =
    eblRepository.getRmCustomTargetsFlow(rmCode, quarter)

  fun saveRmCustomTargets(rmCode: String, targets: Map<String, Int>, quarter: String? = null) {
    viewModelScope.launch {
      eblRepository.saveRmCustomTargets(rmCode, targets, quarter)
    }
  }

  fun getRmAllQuarterCustomTargetsFlow(rmCode: String): Flow<Map<String, Map<String, Int>>> =
    eblRepository.getRmAllQuarterCustomTargetsFlow(rmCode)

  fun saveRmCustomTargetsAllQuarters(rmCode: String, quarterlyTargets: Map<String, Map<String, Int>>) {
    viewModelScope.launch {
      eblRepository.saveRmCustomTargetsAllQuarters(rmCode, quarterlyTargets)
    }
  }

  fun getRmAssignedCriteriaFlow(rmCode: String): Flow<List<String>> =
    eblRepository.getRmAssignedCriteriaFlow(rmCode)

  fun saveRmAssignedCriteria(rmCode: String, criteria: List<String>) {
    viewModelScope.launch {
      eblRepository.saveRmAssignedCriteria(rmCode, criteria)
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

  fun savePresetRow(presetName: String, itemsText: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.savePresetRow(presetName, itemsText)
      res.onSuccess {
        _uiMessage.emit("Preset '$presetName' saved as row in Universal Settings & synced!")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to save preset: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun deletePresetRow(presetName: String, presetKey: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.deletePresetRow(presetName, presetKey)
      res.onSuccess {
        _uiMessage.emit("Preset '$presetName' deleted from Universal Settings & Google Sheets.")
        onResult(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to delete preset: ${err.message}")
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
      _uiMessage.emit("Starting Google Sheets push...")
      val res = eblRepository.triggerGoogleSheetsSync(isExplicitPushCommand = true)
      res.onSuccess { msg ->
        _uiMessage.emit(msg)
      }.onFailure { err ->
        _uiMessage.emit("Sync Error: ${err.message}")
      }
    }
  }

  fun triggerGoogleSheetsSync(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.triggerGoogleSheetsSync(isExplicitPushCommand = true)
      res.onSuccess { msg ->
        _uiMessage.emit(msg)
        onResult(true, msg)
      }.onFailure { err ->
        _uiMessage.emit("Sync Error: ${err.message}")
        onResult(false, err.message)
      }
    }
  }

  fun pullDataFromGoogleSheets(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
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

  fun updateFileStatusOptions(newStatuses: String, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.updateFileStatusOptions(newStatuses)
      res.onSuccess {
        _uiMessage.emit("Application status categories updated universally.")
        onDone(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to update status categories: ${err.message}")
        onDone(false, err.message)
      }
    }
  }

  fun updateFileStatus(
    fileId: String,
    newStatus: String,
    newRemarks: String? = null,
    newCpvStatus: String? = null,
    newCpvRemarks: String? = null,
    newCpvDate: String? = null,
    newCpvAddress: String? = null,
    onDone: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateFileStatus(
        fileId = fileId,
        newStatus = newStatus,
        newRemarks = newRemarks,
        newCpvStatus = newCpvStatus,
        newCpvRemarks = newCpvRemarks,
        newCpvDate = newCpvDate,
        newCpvAddress = newCpvAddress
      )
      res.onSuccess { updated ->
        val cpvMsg = if (!newCpvStatus.isNullOrBlank()) " [CPV: ${updated.cpvStatus}]" else ""
        _uiMessage.emit("Customer file status updated to '${updated.applicationStatus}'$cpvMsg.")
        onDone(true, null)
      }.onFailure { err ->
        _uiMessage.emit("Failed to update status: ${err.message}")
        onDone(false, err.message)
      }
    }
  }

  fun addAttachment(
    fileId: String,
    category: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String,
    localFile: java.io.File? = null
  ) {
    viewModelScope.launch {
      val res = eblRepository.addAttachment(fileId, category, fileName, fileType, fileSizeBytes, fileUri, localFile)
      res.onSuccess { att ->
        if (att.fileUri.startsWith("http")) {
          _uiMessage.emit("Attachment '$fileName' Google Drive-এ সফলভাবে সেভ হয়েছে।")
        } else {
          _uiMessage.emit("Attachment '$fileName' লোকাল ডিভাইসে সেভ হয়েছে (ড্রাইভে আপলোড হয়নি: Apps Script-এ Drive পারমিশন অ্যালাউ করুন)।")
        }
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

  val isRefreshing = MutableStateFlow(false)

  fun refreshData(onComplete: (() -> Unit)? = null) {
    viewModelScope.launch {
      isRefreshing.value = true
      try {
        eblRepository.pullDataFromGoogleSheets()
      } catch (_: Exception) {} finally {
        isRefreshing.value = false
        onComplete?.invoke()
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

  private val _incomingCall = MutableStateFlow<IncomingCallInfo?>(null)
  val incomingCall: StateFlow<IncomingCallInfo?> = _incomingCall.asStateFlow()

  private var callTimerJob: kotlinx.coroutines.Job? = null

  val teamHubMessages: StateFlow<List<com.example.data.model.ChatMessageEntity>> = eblRepository.getTeamHubMessagesFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val teamEvents: StateFlow<List<com.example.data.model.TeamEventEntity>> = eblRepository.getAllTeamEventsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeTeamEvents: StateFlow<List<com.example.data.model.TeamEventEntity>> = eblRepository.getAllTeamEventsFlow()
    .map { list -> list.filter { !eblRepository.isEventExpired(it) } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun sendChatMessage(
    text: String,
    recipientRmCode: String? = null,
    messageType: String = "TEXT",
    eventId: String? = null,
    attachmentUrl: String? = null,
    replyToMessageId: String? = null,
    replyToSenderName: String? = null,
    replyToText: String? = null,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    if (text.isBlank() && attachmentUrl.isNullOrBlank()) return
    viewModelScope.launch {
      val res = eblRepository.sendChatMessage(
        text, recipientRmCode, messageType, eventId, attachmentUrl,
        replyToMessageId, replyToSenderName, replyToText
      )
      res.onSuccess {
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  suspend fun uploadFileToGoogleDrive(file: java.io.File, fileName: String, fileType: String): Result<String> {
    return eblRepository.uploadFileToGoogleDrive(file, fileName, fileType)
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

  fun deleteChatMessageForMe(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = eblRepository.deleteChatMessageForMe(id)
      res.onSuccess {
        _uiMessage.emit("Message deleted for you.")
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
    closeTime: String = "",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.createTeamEvent(title, description, targetDate, allowedFields, closeTime)
      res.onSuccess {
        _uiMessage.emit("✓ Event '${it.title}' created & shared with all team members!")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
      }
    }
  }

  fun updateEventSettings(
    eventId: String,
    title: String,
    description: String,
    targetDate: String,
    status: String,
    allowedFields: String = "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS",
    closeTime: String = "",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateEventSettings(eventId, title, description, targetDate, status, allowedFields, closeTime)
      res.onSuccess {
        _uiMessage.emit("✓ Event settings updated!")
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

  fun deleteTeamEvent(
    eventId: String,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val res = eblRepository.deleteTeamEvent(eventId)
      res.onSuccess {
        _uiMessage.emit("✓ Event deleted and removed from Google Sheets!")
        onResult?.invoke(true, null)
      }.onFailure { err ->
        _uiMessage.emit(err.message ?: "Failed to delete event.")
        onResult?.invoke(false, err.message)
      }
    }
  }

  fun closeTeamEvent(
    eventId: String,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val res = eblRepository.closeTeamEvent(eventId)
      res.onSuccess {
        _uiMessage.emit("✓ ইভেন্টটি সমাপ্ত ও ডাটা এন্ট্রি বন্ধ করা হয়েছে।")
        onResult?.invoke(true, null)
      }.onFailure { err ->
        _uiMessage.emit(err.message ?: "Failed to close event.")
        onResult?.invoke(false, err.message)
      }
    }
  }

  fun notifySmsDispatchedToAll(customerName: String, customerMobile: String, messageText: String) {
    viewModelScope.launch {
      eblRepository.notifySmsDispatchedToAll(customerName, customerMobile, messageText)
    }
  }

  // Calling features have been removed per user instruction
  fun startCall(targetUser: com.example.data.model.UserEntity) {}
  fun startGroupCall(title: String) {}
  fun acceptIncomingCall() {}
  fun declineIncomingCall() {}
  fun toggleMute() {}
  fun toggleSpeaker() {}
  // Field Visit Route Planning
  fun startRoute(fileIds: List<String>) {
    val clean = fileIds.distinct()
    activeRouteCustomerIds.value = clean
    completedRouteCustomerIds.value = emptyList()
    authPrefs.edit().putString("active_field_route_ids", clean.joinToString(",")).apply()
  }

  fun completeCustomerInRoute(fileId: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      eblRepository.updateFileStatus(
        fileId = fileId,
        newStatus = "Collected",
        newRemarks = "Collected via Field Visit Route Plan"
      )
      val updatedActive = activeRouteCustomerIds.value.filter { it != fileId }
      activeRouteCustomerIds.value = updatedActive
      completedRouteCustomerIds.value = (completedRouteCustomerIds.value + fileId).distinct()
      authPrefs.edit().putString("active_field_route_ids", updatedActive.joinToString(",")).apply()
      _uiMessage.emit("✓ ফাইল কালেকশন সম্পন্ন! রুট থেকে বাদ দেওয়া হয়েছে।")
      onDone()
    }
  }

  fun removeCustomerFromRoute(fileId: String) {
    val updated = activeRouteCustomerIds.value.filter { it != fileId }
    activeRouteCustomerIds.value = updated
    authPrefs.edit().putString("active_field_route_ids", updated.joinToString(",")).apply()
  }

  fun addCustomerToRoute(fileId: String) {
    if (!activeRouteCustomerIds.value.contains(fileId)) {
      val updated = activeRouteCustomerIds.value + fileId
      activeRouteCustomerIds.value = updated
      authPrefs.edit().putString("active_field_route_ids", updated.joinToString(",")).apply()
    }
  }

  fun clearRoute() {
    activeRouteCustomerIds.value = emptyList()
    completedRouteCustomerIds.value = emptyList()
    authPrefs.edit().remove("active_field_route_ids").apply()
  }

  // -----------------------------------------------------------------------------------------------
  // COMPANY PROFILE WORKBOOK OPERATIONS (Excel .xlsx / CSV Upload & Auto-Replace)
  // -----------------------------------------------------------------------------------------------
  fun uploadCompanyWorkbook(
    context: android.content.Context,
    uri: android.net.Uri,
    fileName: String,
    uploadedBy: String,
    onComplete: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val rawBytes = withContext(Dispatchers.IO) {
          try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          } catch (_: Exception) {
            null
          }
        }
        val parsed = withContext(Dispatchers.IO) {
          if (rawBytes != null && rawBytes.isNotEmpty()) {
            com.example.util.ExcelWorkbookParser.parseWorkbookFromBytes(
              bytes = rawBytes,
              fileName = fileName,
              uploadedBy = uploadedBy
            )
          } else {
            com.example.util.ExcelWorkbookParser.parseWorkbookFromUri(
              context = context,
              uri = uri,
              fileName = fileName,
              uploadedBy = uploadedBy
            )
          }
        }
        if (parsed.sheets.isEmpty() || parsed.totalRows == 0) {
          onComplete(false, "ফাইলটিতে কোনো ডাটা বা শিট পাওয়া যায়নি!")
          return@launch
        }
        // Save to internal storage file and sync to Google Sheets & Drive as universal master
        eblRepository.saveCompanyWorkbook(parsed, rawFileBytes = rawBytes)
        onComplete(true, "সফলভাবে ${parsed.sheets.size} টি শিট এবং মোট ${parsed.totalRows} টি রেকর্ড আপলোড ও ক্লাউডে সিঙ্ক হয়েছে! সকল RM এবং ব্যবহারকারী এই আপডেট দেখতে পাবেন।")
      } catch (e: Exception) {
        onComplete(false, "ফাইল আপলোড ব্যর্থ হয়েছে: ${e.localizedMessage}")
      }
    }
  }

  fun syncCompanyWorkbookFromCloud(onComplete: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      try {
        eblRepository.syncCompanyWorkbookFromCloud()
        val current = eblRepository.getCompanyWorkbook()
        onComplete(true, "ক্লাউড থেকে সফলভাবে কোম্পানির তথ্য সিঙ্ক করা হয়েছে (${current.totalRows} টি রেকর্ড)!")
      } catch (e: Exception) {
        onComplete(false, "সিঙ্ক ব্যর্থ হয়েছে: ${e.localizedMessage}")
      }
    }
  }

  fun resetCompanyWorkbookToDefault(uploadedBy: String, onComplete: (Boolean) -> Unit) {
    viewModelScope.launch {
      try {
        eblRepository.resetCompanyWorkbook(uploadedBy)
        onComplete(true)
      } catch (_: Exception) {
        onComplete(false)
      }
    }
  }
}


data class IncomingCallInfo(
  val callerName: String,
  val callerRmCode: String,
  val callerRole: String,
  val isGroup: Boolean = false,
  val groupTitle: String = "",
  val callId: String = ""
)

sealed class CallUiState {
  object Idle : CallUiState()
}
