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
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.EblRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
}

data class KpiStats(
  val totalFiles: Int = 0,
  val collected: Int = 0,
  val submitted: Int = 0,
  val approved: Int = 0,
  val declined: Int = 0,
  val query: Int = 0,
  val returnToSource: Int = 0,
  val condition: Int = 0,
  val stc: Int = 0,
  val pendingDocumentsCount: Int = 0,
  val activeY: Int = 0,
  val activeN: Int = 0,
  val activeC: Int = 0,
  val creditCardCount: Int = 0,
  val corporateCardCount: Int = 0,
  val b2bCount: Int = 0
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

  init {
    viewModelScope.launch {
      DatabaseInitializer.initializeIfNeeded(database)
    }
  }

  private fun computeStats(files: List<CustomerFileEntity>): KpiStats {
    var collected = 0
    var submitted = 0
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

    for (f in files) {
      val p = f.productType.lowercase()
      when {
        p.contains("corporate") -> corporateCardCount++
        p.contains("credit") -> creditCardCount++
        p.contains("b2b") -> b2bCount++
      }
      when (f.applicationStatus.lowercase()) {
        "collected" -> collected++
        "submitted" -> submitted++
        "approved" -> approved++
        "declined" -> declined++
        "query" -> query++
        "return to source" -> returnToSource++
        "condition" -> condition++
        "stc" -> stc++
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
      collected = collected,
      submitted = submitted,
      approved = approved,
      declined = declined,
      query = query,
      returnToSource = returnToSource,
      condition = condition,
      stc = stc,
      pendingDocumentsCount = pendingDocsCount,
      activeY = activeY,
      activeN = activeN,
      activeC = activeC,
      creditCardCount = creditCardCount,
      corporateCardCount = corporateCardCount,
      b2bCount = b2bCount
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

  fun login(
    usernameInput: String,
    passwordInput: String,
    latitude: Double? = null,
    longitude: Double? = null,
    address: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = authRepository.login(usernameInput, passwordInput, latitude, longitude, address)
      res.onSuccess { user ->
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
      authRepository.logout()
      screenBackstack.clear()
      _currentScreen.value = Screen.Login
      _uiMessage.emit("You have been signed out safely.")
    }
  }

  fun changePassword(oldPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = authRepository.changePassword(oldPass, newPass)
      res.onSuccess {
        onResult(true, "Password changed successfully!")
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
        submissionAddress = submissionAddress
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

  fun resetRmPassword(rmCode: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val res = eblRepository.resetRmPassword(rmCode, newPass)
      res.onSuccess {
        _uiMessage.emit("Password reset for RM $rmCode.")
        onResult(true, null)
      }.onFailure { err ->
        onResult(false, err.message)
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

  fun updateRmProfile(
    rmCode: String,
    name: String,
    mobile: String,
    email: String,
    officeAddress: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.updateRmUser(rmCode, name, mobile, email, officeAddress)
      res.onSuccess {
        _uiMessage.emit("RM $rmCode updated successfully.")
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
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = eblRepository.setRmTargets(rmCode, creditCardTarget, corporateCardTarget, b2bTarget)
      res.onSuccess {
        _uiMessage.emit("Target updated for RM $rmCode.")
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
}
