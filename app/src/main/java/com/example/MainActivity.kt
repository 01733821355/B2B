package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.ActiveEventSlidingBanner
import com.example.ui.common.EblTopBar
import com.example.ui.screens.ChangePasswordDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AppSettingsScreen
import com.example.ui.screens.admin.AuditLogsScreen
import com.example.ui.screens.admin.GoogleSheetsSyncScreen
import com.example.ui.screens.admin.RmMappingScreen
import com.example.ui.screens.mentor.MentorDashboardScreen
import com.example.ui.screens.mentor.MentorUserLocationScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.tools.DbrAndChecklistScreen
import com.example.ui.screens.rm.CustomerFileFormScreen
import com.example.ui.screens.rm.CustomerFileListScreen
import com.example.ui.screens.rm.RmDashboardScreen
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    enableEdgeToEdge()
    NotificationHelper.createNotificationChannels(applicationContext)
    com.example.util.BackgroundSyncWorker.schedulePeriodicSync(applicationContext)
    com.example.util.SyncAlarmReceiver.scheduleNextAlarm(applicationContext, 10_000L)
    setContent {
      val activeThemeConfig by viewModel.activeThemeConfig.collectAsState()
      val universalFontStyle by viewModel.universalFontStyle.collectAsState()
      val universalTextScale by viewModel.universalTextScale.collectAsState()
      MyApplicationTheme(
        themeConfig = activeThemeConfig,
        fontStyle = universalFontStyle,
        textScale = universalTextScale
      ) {
        EblMainApp(viewModel = viewModel)
      }
    }
  }

  override fun onStop() {
    super.onStop()
    try {
      com.example.util.BackgroundSyncWorker.triggerImmediateSync(applicationContext)
      com.example.util.BackgroundSyncWorker.schedulePeriodicSync(applicationContext)
      com.example.util.SyncAlarmReceiver.scheduleNextAlarm(applicationContext, 10_000L)
    } catch (_: Exception) {}
  }
}

@Composable
fun EblMainApp(viewModel: AppViewModel) {
  val currentUser by viewModel.currentUser.collectAsState()
  val currentScreen by viewModel.currentScreen.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val appCustomIconUrl by viewModel.appCustomIconUrl.collectAsState()
  val unreadSmsCount by viewModel.unreadSmsCount.collectAsState()
  val rmSmsList by viewModel.rmSmsNotifications.collectAsState()
  val allSmsList by viewModel.allSmsNotifications.collectAsState()
  val lastLoggedRmCode by viewModel.lastLoggedRmCode.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showPasswordDialog by remember { mutableStateOf(false) }
  var showRmSmsInboxModal by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val bannerBackgroundUrl by viewModel.dashboardBannerBackgroundUrl.collectAsState()

  val bannerPhotoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
          context,
          uri,
          "banner_${System.currentTimeMillis()}.jpg"
        )
        if (saved != null) {
          val localFile = java.io.File(saved.storagePath)
          val fileName = "banner_${System.currentTimeMillis()}.jpg"
          Toast.makeText(context, "Uploading banner photo to Drive...", Toast.LENGTH_SHORT).show()
          viewModel.uploadAndSetDashboardBannerBackground(localFile, fileName, "image/jpeg") { success, msg ->
            Toast.makeText(context, if (success) "Banner background updated!" else "Upload failed: $msg", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  // Unified Permission Requester: Requests Location, SMS, and Notifications TOGETHER
  val unifiedPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    NotificationHelper.markPermissionAsked(context)
    val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    if (fine || coarse) {
      scope.launch {
        try {
          val loc = com.example.util.LocationHelper.getCurrentLocation(context)
          val uCode = currentUser?.rmCode ?: lastLoggedRmCode
          if (uCode.isNotBlank()) {
            viewModel.updateUserLocation(uCode, loc.latitude, loc.longitude, loc.address, "APP_OPEN_GPS")
          }
        } catch (_: Exception) {}
      }
    }
  }

  // Request SMS, Location, and Notifications together on app launch & capture immediate startup GPS
  LaunchedEffect(Unit) {
    NotificationHelper.createNotificationChannels(context)
    com.example.util.BackgroundSyncWorker.schedulePeriodicSync(context.applicationContext)

    // Requirement: Rm real time location jokhon app open hobe tokhonoi auto nia nibe
    if (com.example.util.LocationHelper.hasLocationPermission(context)) {
      scope.launch {
        try {
          val loc = com.example.util.LocationHelper.getCurrentLocation(context)
          val uCode = currentUser?.rmCode ?: lastLoggedRmCode
          if (uCode.isNotBlank()) {
            viewModel.updateUserLocation(uCode, loc.latitude, loc.longitude, loc.address, "APP_OPEN_GPS")
          }
        } catch (_: Exception) {}
      }
    }

    val neededPermissions = mutableListOf<String>()
    if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    if (neededPermissions.isNotEmpty()) {
      unifiedPermissionLauncher.launch(neededPermissions.toTypedArray())
    }
  }

  // Real-time location auto-beacon when user is active
  LaunchedEffect(currentUser?.rmCode) {
    val userCode = currentUser?.rmCode
    if (!userCode.isNullOrBlank()) {
      // Immediate location ping on user login/session restore
      if (com.example.util.LocationHelper.hasLocationPermission(context)) {
        try {
          val loc = com.example.util.LocationHelper.getCurrentLocation(context)
          viewModel.updateUserLocation(userCode, loc.latitude, loc.longitude, loc.address, "LOGIN_GPS_BEACON")
        } catch (_: Exception) {}
      }
      while (true) {
        kotlinx.coroutines.delay(60_000) // auto beacon every 60 seconds
        if (com.example.util.LocationHelper.hasLocationPermission(context)) {
          try {
            val loc = com.example.util.LocationHelper.getCurrentLocation(context)
            viewModel.updateUserLocation(userCode, loc.latitude, loc.longitude, loc.address, "AUTO_BACKGROUND_BEACON")
          } catch (_: Exception) {}
        }
      }
    }
  }

  // Near real-time foreground sync: poll latest updates from Google Sheets every 5 seconds
  LaunchedEffect(currentUser?.rmCode) {
    val userCode = currentUser?.rmCode
    if (!userCode.isNullOrBlank()) {
      // Immediate pull on screen load / user session restore
      try {
        viewModel.pullDataFromGoogleSheets()
      } catch (_: Exception) {}
      while (true) {
        kotlinx.coroutines.delay(5_000)
        try {
          viewModel.pullDataFromGoogleSheets()
        } catch (_: Exception) {}
      }
    }
  }

  // Listen for uiMessages and show toast snackbar (background sync messages run silently)
  LaunchedEffect(Unit) {
    viewModel.uiMessage.collectLatest { msg ->
      if (!msg.startsWith("Pulled from Google Sheets", ignoreCase = true) &&
          !msg.contains("notification(s) updated", ignoreCase = true)) {
        snackbarHostState.showSnackbar(msg)
      }
    }
  }

  // If user is not authenticated, show universal login screen
  if (currentUser == null) {
    LoginScreen(
      appCustomName = appCustomName,
      appCustomIconUrl = appCustomIconUrl,
      lastLoggedRmCode = lastLoggedRmCode,
      onLogin = { username, password, lat, lng, addr, callback ->
        viewModel.login(username, password, lat, lng, addr, callback)
      },
      onBiometricLogin = { rmCode, lat, lng, addr, callback ->
        viewModel.loginWithBiometrics(rmCode, lat, lng, addr, callback)
      },
      isBiometricEnabled = { viewModel.isBiometricEnabled(it) },
      isPasswordVerified = { viewModel.isPasswordLoginVerified(it) }
    )
    return
  }

  val user = currentUser!!

  // Check if forced password change on first login
  val isForcedPasswordChange = user.mustChangePassword

  // Handle hardware back press gracefully
  BackHandler(enabled = currentScreen !is Screen.RmDashboard && currentScreen !is Screen.AdminDashboard && currentScreen !is Screen.MentorDashboard) {
    if (!viewModel.navigateBack()) {
      // If backstack empty, return to role home dashboard
      when (user.role) {
        "ADMIN" -> viewModel.navigateTo(Screen.AdminDashboard)
        "MENTOR" -> viewModel.navigateTo(Screen.MentorDashboard)
        else -> viewModel.navigateTo(Screen.RmDashboard)
      }
    }
  }

  val configuration = LocalConfiguration.current
  val isWideScreen = configuration.screenWidthDp >= 600

  Scaffold(
    modifier = Modifier.fillMaxSize().testTag("ebl_main_scaffold"),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      val canGoBack = currentScreen !is Screen.RmDashboard &&
        currentScreen !is Screen.AdminDashboard &&
        currentScreen !is Screen.MentorDashboard

      Column(modifier = Modifier.fillMaxWidth()) {
        EblTopBar(
          user = user,
          appCustomName = appCustomName,
          appCustomIconUrl = appCustomIconUrl,
          canNavigateBack = canGoBack,
          onNavigateBack = {
            if (!viewModel.navigateBack()) {
              when (user.role) {
                "ADMIN" -> viewModel.navigateTo(Screen.AdminDashboard)
                "MENTOR" -> viewModel.navigateTo(Screen.MentorDashboard)
                else -> viewModel.navigateTo(Screen.RmDashboard)
              }
            }
          },
          onLogout = { viewModel.logout() },
          onChangePassword = { showPasswordDialog = true },
          onSyncClicked = { viewModel.navigateTo(Screen.GoogleSheetsSync) },
          pendingSyncCount = syncStatus?.pendingRecordsCount ?: 0,
          unreadSmsCount = unreadSmsCount,
          smsList = if (user.role == "RM") rmSmsList else allSmsList,
          onMarkSmsRead = { viewModel.markSmsAsRead(it) },
          onMarkAllSmsRead = { viewModel.markAllSmsAsRead() },
          onClearSms = {
            if (user.role == "RM") viewModel.clearSmsForCurrentRm() else viewModel.clearAllSms()
          },
          onDeleteSms = { viewModel.deleteSms(it) },
          onOpenDbrChecklist = { viewModel.navigateTo(Screen.DbrChecklist) },
          onOpenCommunication = { viewModel.navigateTo(Screen.CommunicationHub) },
          onOpenImportantDocuments = { viewModel.navigateTo(Screen.ImportantDocuments) }
        )

        val activeEvents by viewModel.activeTeamEvents.collectAsState()
        if (activeEvents.isNotEmpty()) {
          ActiveEventSlidingBanner(
            activeEvents = activeEvents,
            onEventClick = { viewModel.navigateTo(Screen.CommunicationHub) }
          )
        }
      }
    },
    bottomBar = {
      if (!isWideScreen) {
        EblBottomNav(
          userRole = user.role,
          currentScreen = currentScreen,
          onSelectScreen = { viewModel.navigateTo(it) }
        )
      }
    }
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (isWideScreen) {
        EblNavigationRail(
          userRole = user.role,
          currentScreen = currentScreen,
          onSelectScreen = { viewModel.navigateTo(it) }
        )
      }

      val currentTabs = remember(user.role) {
        if (user.role == "ADMIN") {
          listOf(
            Screen.AdminDashboard,
            Screen.GlobalDatabase,
            Screen.RmMapping,
            Screen.GoogleSheetsSync,
            Screen.More
          )
        } else if (user.role == "MENTOR") {
          listOf(
            Screen.MentorDashboard,
            Screen.GlobalDatabase,
            Screen.RmMapping,
            Screen.GoogleSheetsSync,
            Screen.More
          )
        } else {
          listOf(
            Screen.RmDashboard,
            Screen.CustomerForm(null),
            Screen.CustomerList,
            Screen.Reports,
            Screen.More
          )
        }
      }

      val activeTabIndex = remember(currentScreen, currentTabs) {
        currentTabs.indexOfFirst { tab ->
          when (tab) {
            is Screen.CustomerForm -> currentScreen is Screen.CustomerForm && (currentScreen as Screen.CustomerForm).editFileId == null
            else -> tab::class == currentScreen::class
          }
        }
      }

      var totalDragX by remember { mutableFloatStateOf(0f) }

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f)
          .then(
            if (activeTabIndex != -1) {
              Modifier.pointerInput(activeTabIndex) {
                detectHorizontalDragGestures(
                  onDragStart = { totalDragX = 0f },
                  onDragEnd = {
                    if (totalDragX < -90f && activeTabIndex < currentTabs.size - 1) {
                      viewModel.navigateTo(currentTabs[activeTabIndex + 1])
                    } else if (totalDragX > 90f && activeTabIndex > 0) {
                      viewModel.navigateTo(currentTabs[activeTabIndex - 1])
                    }
                    totalDragX = 0f
                  },
                  onDragCancel = { totalDragX = 0f },
                  onHorizontalDrag = { _, dragAmount ->
                    totalDragX += dragAmount
                  }
                )
              }
            } else Modifier
          )
      ) {
        AnimatedContent(
          targetState = currentScreen,
          transitionSpec = {
            val fromIndex = currentTabs.indexOfFirst { tab ->
              when (tab) {
                is Screen.CustomerForm -> initialState is Screen.CustomerForm && (initialState as Screen.CustomerForm).editFileId == null
                else -> tab::class == initialState::class
              }
            }
            val toIndex = currentTabs.indexOfFirst { tab ->
              when (tab) {
                is Screen.CustomerForm -> targetState is Screen.CustomerForm && (targetState as Screen.CustomerForm).editFileId == null
                else -> tab::class == targetState::class
              }
            }
            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
              if (toIndex > fromIndex) {
                (slideInHorizontally(animationSpec = tween(280)) { width -> width } + fadeIn(animationSpec = tween(280)))
                  .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width } + fadeOut(animationSpec = tween(200)))
              } else {
                (slideInHorizontally(animationSpec = tween(280)) { width -> -width } + fadeIn(animationSpec = tween(280)))
                  .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width } + fadeOut(animationSpec = tween(200)))
              }
            } else {
              fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(160)))
            }
          },
          label = "ScreenSwipeTransition",
          modifier = Modifier.fillMaxSize()
        ) { targetScreen ->
          when (val screen = targetScreen) {
          is Screen.Login -> {
            // Already handled above
          }

          is Screen.RmDashboard -> {
            val stats by viewModel.kpiStats.collectAsState()
            val timeFilter by viewModel.selectedTimeFilter.collectAsState()
            val files by viewModel.filteredFiles.collectAsState()
            val allTargets by viewModel.allTargets.collectAsState()
            val rmTarget = allTargets.find { it.rmCode == user.rmCode }
            val targetCriteria by viewModel.targetCriteriaOptions.collectAsState()
            val customTargets by viewModel.getRmCustomTargetsFlow(user.rmCode).collectAsState(initial = emptyMap())
            val allQuarterCustomTargets by viewModel.getRmAllQuarterCustomTargetsFlow(user.rmCode).collectAsState(initial = emptyMap())
            val assignedCriteria by viewModel.getRmAssignedCriteriaFlow(user.rmCode).collectAsState(initial = emptyList())

            RmDashboardScreen(
              user = user,
              stats = stats,
              target = rmTarget,
              targetCriteria = targetCriteria,
              assignedCriteria = assignedCriteria,
              customTargets = customTargets,
              allQuarterCustomTargets = allQuarterCustomTargets,
              selectedTimeFilter = timeFilter,
              recentFiles = files,
              unreadSmsCount = unreadSmsCount,
              onOpenSmsInbox = { showRmSmsInboxModal = true },
              onTimeFilterChange = { viewModel.selectedTimeFilter.value = it },
              onAddNewFile = { viewModel.navigateTo(Screen.CustomerForm(null)) },
              onViewAllFiles = {
                viewModel.pendingDocsOnlyFilter.value = false
                viewModel.navigateTo(Screen.CustomerList)
              },
              onViewPendingDocs = {
                viewModel.pendingDocsOnlyFilter.value = true
                viewModel.navigateTo(Screen.CustomerList)
              },
              onFileClick = { f -> viewModel.navigateTo(Screen.CustomerForm(f.fileId)) },
              onDownloadReport = { viewModel.navigateTo(Screen.Reports) },
              onOpenDbrChecklist = { viewModel.navigateTo(Screen.DbrChecklist) },
              onOpenCommunication = { viewModel.navigateTo(Screen.CommunicationHub) },
              onOpenImportantDocuments = { viewModel.navigateTo(Screen.ImportantDocuments) },
              onOpenCompanyProfile = { viewModel.navigateTo(Screen.CompanyProfile) },
              onOpenRoutePlanner = { viewModel.navigateTo(Screen.FieldRoutePlanner()) },
              onUpdateLocation = { lat, lng, addr ->
                viewModel.updateUserLocation(user.rmCode, lat, lng, addr, "LIVE_DASHBOARD_BEACON")
              },
              isBiometricEnabled = viewModel.isBiometricEnabled(user.rmCode),
              onToggleBiometric = { viewModel.setBiometricEnabled(user.rmCode, it) },
              onOpenTargetVsAchievement = { viewModel.navigateTo(Screen.TargetVsAchievement) },
              bannerBackgroundUrl = bannerBackgroundUrl,
              onUploadBannerBackground = {
                bannerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              onRefresh = { viewModel.refreshData() }
            )
          }

          is Screen.CustomerForm -> {
            CustomerFileFormScreen(
              viewModel = viewModel,
              editFileId = screen.editFileId,
              currentUser = user,
              onCancel = { viewModel.navigateBack() },
              onSaveSuccess = { _ ->
                viewModel.navigateBack()
              }
            )
          }

          is Screen.CustomerList, Screen.GlobalDatabase -> {
            CustomerFileListScreen(
              viewModel = viewModel,
              currentUser = user,
              onAddNewFile = { viewModel.navigateTo(Screen.CustomerForm(null)) },
              onEditFile = { fileId -> viewModel.navigateTo(Screen.CustomerForm(fileId)) },
              onOpenRoutePlanner = { selectedIds -> viewModel.navigateTo(Screen.FieldRoutePlanner(selectedIds)) }
            )
          }

          is Screen.AdminDashboard -> {
            AdminDashboardScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigate = { target -> viewModel.navigateTo(target) },
              onRmRowClicked = { _ -> viewModel.navigateTo(Screen.GlobalDatabase) }
            )
          }

          is Screen.MentorDashboard -> {
            MentorDashboardScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigate = { target -> viewModel.navigateTo(target) }
            )
          }

          is Screen.MentorUserLocationTracking -> {
            MentorUserLocationScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigateBack = { viewModel.navigateBack() }
            )
          }

          is Screen.RmMapping -> {
            RmMappingScreen(
              viewModel = viewModel,
              currentUser = user
            )
          }

          is Screen.Reports -> {
            ReportsScreen(
              viewModel = viewModel,
              currentUser = user
            )
          }

          is Screen.GoogleSheetsSync -> {
            if (currentUser?.role == "MENTOR") {
              GoogleSheetsSyncScreen(
                viewModel = viewModel
              )
            } else {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Access Restricted: Google Sheets Sync is exclusively managed by the Operations Mentor.",
                  color = Color.Gray,
                  fontSize = 13.sp
                )
              }
            }
          }

          is Screen.AuditLogs -> {
            AuditLogsScreen(
              viewModel = viewModel,
              currentUser = user
            )
          }

          is Screen.AppSettings -> {
            AppSettingsScreen(
              viewModel = viewModel,
              currentUser = user
            )
          }

          is Screen.ProfilePassword -> {
            showPasswordDialog = true
          }

          is Screen.DbrChecklist -> {
            val initTab by viewModel.dbrChecklistInitialTab.collectAsState()
            DbrAndChecklistScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigateBack = { viewModel.navigateBack() },
              initialTab = initTab
            )
          }

          is Screen.More -> {
            com.example.ui.screens.more.MoreScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigate = { viewModel.navigateTo(it) }
            )
          }

          is Screen.CommunicationHub -> {
            com.example.ui.screens.tools.CommunicationHubScreen(
              viewModel = viewModel
            )
          }

          is Screen.ImportantDocuments -> {
            com.example.ui.screens.tools.ImportantDocumentsScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigateBack = { viewModel.navigateBack() }
            )
          }

          is Screen.CompanyProfile -> {
            com.example.ui.screens.tools.CompanyProfileScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigateBack = { viewModel.navigateBack() }
            )
          }

          is Screen.FieldRoutePlanner -> {
            com.example.ui.screens.rm.FieldRoutePlannerScreen(
              viewModel = viewModel,
              currentUser = user,
              initialSelectedFileIds = screen.initialSelectedFileIds,
              onBack = { viewModel.navigateBack() },
              onEditFile = { fileId -> viewModel.navigateTo(Screen.CustomerForm(fileId)) }
            )
          }

          is Screen.TargetVsAchievement -> {
            val stats by viewModel.kpiStats.collectAsState()
            val allTargets by viewModel.allTargets.collectAsState()
            val rmTarget = allTargets.find { it.rmCode == user.rmCode }
            val targetCriteria by viewModel.targetCriteriaOptions.collectAsState()
            val customTargets by viewModel.getRmCustomTargetsFlow(user.rmCode).collectAsState(initial = emptyMap())
            val allQuarterCustomTargets by viewModel.getRmAllQuarterCustomTargetsFlow(user.rmCode).collectAsState(initial = emptyMap())
            val assignedCriteria by viewModel.getRmAssignedCriteriaFlow(user.rmCode).collectAsState(initial = emptyList())

            com.example.ui.screens.rm.TargetVsAchievementScreen(
              user = user,
              stats = stats,
              target = rmTarget,
              targetCriteria = targetCriteria,
              assignedCriteria = assignedCriteria,
              customTargets = customTargets,
              allQuarterCustomTargets = allQuarterCustomTargets,
              onBack = { viewModel.navigateBack() }
            )
          }
        }
      }
      }
    }
  }

  // Force password change on first login, or user-triggered change
  if (isForcedPasswordChange || showPasswordDialog) {
    ChangePasswordDialog(
      isForced = isForcedPasswordChange,
      onDismiss = { showPasswordDialog = false },
      onSubmit = { oldPass, newPass, cb ->
        viewModel.changePassword(oldPass, newPass) { success, err ->
          cb(success, err)
          if (success) {
            showPasswordDialog = false
          }
        }
      }
    )
  }

  if (showRmSmsInboxModal) {
    com.example.ui.common.SmsNotificationsDialog(
      isRmView = true,
      smsList = rmSmsList,
      onDismiss = { showRmSmsInboxModal = false },
      onMarkRead = { viewModel.markSmsAsRead(it) },
      onMarkAllRead = { viewModel.markAllSmsAsRead() },
      onClearAll = { viewModel.clearSmsForCurrentRm() },
      onDeleteSms = { viewModel.deleteSms(it) }
    )
  }
}

@Composable
fun EblBottomNav(
  userRole: String,
  currentScreen: Screen,
  onSelectScreen: (Screen) -> Unit
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.height(58.dp)
  ) {
    if (userRole == "RM") {
      NavigationBarItem(
        selected = currentScreen is Screen.RmDashboard,
        onClick = { onSelectScreen(Screen.RmDashboard) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(20.dp)) },
        label = { Text("Dashboard", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.CustomerForm && currentScreen.editFileId == null,
        onClick = { onSelectScreen(Screen.CustomerForm(null)) },
        icon = { Icon(Icons.Default.AddCircle, contentDescription = "New File", modifier = Modifier.size(20.dp)) },
        label = { Text("New File", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.CustomerList,
        onClick = { onSelectScreen(Screen.CustomerList) },
        icon = { Icon(Icons.Default.Folder, contentDescription = "My Files", modifier = Modifier.size(20.dp)) },
        label = { Text("My Files", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.Reports,
        onClick = { onSelectScreen(Screen.Reports) },
        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports", modifier = Modifier.size(20.dp)) },
        label = { Text("Reports", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.More,
        onClick = { onSelectScreen(Screen.More) },
        icon = { Icon(Icons.Default.Widgets, contentDescription = "More", modifier = Modifier.size(20.dp)) },
        label = { Text("More", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
    } else if (userRole == "ADMIN") {
      NavigationBarItem(
        selected = currentScreen is Screen.AdminDashboard,
        onClick = { onSelectScreen(Screen.AdminDashboard) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(20.dp)) },
        label = { Text("Dashboard", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.GlobalDatabase,
        onClick = { onSelectScreen(Screen.GlobalDatabase) },
        icon = { Icon(Icons.Default.Storage, contentDescription = "Database", modifier = Modifier.size(20.dp)) },
        label = { Text("Database", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.RmMapping,
        onClick = { onSelectScreen(Screen.RmMapping) },
        icon = { Icon(Icons.Default.Group, contentDescription = "RM Mapping", modifier = Modifier.size(20.dp)) },
        label = { Text("RM Mapping", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.Reports,
        onClick = { onSelectScreen(Screen.Reports) },
        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports", modifier = Modifier.size(20.dp)) },
        label = { Text("Reports", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.More,
        onClick = { onSelectScreen(Screen.More) },
        icon = { Icon(Icons.Default.Widgets, contentDescription = "More", modifier = Modifier.size(20.dp)) },
        label = { Text("More", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
    } else { // MENTOR
      NavigationBarItem(
        selected = currentScreen is Screen.MentorDashboard,
        onClick = { onSelectScreen(Screen.MentorDashboard) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Console", modifier = Modifier.size(20.dp)) },
        label = { Text("Console", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.GlobalDatabase,
        onClick = { onSelectScreen(Screen.GlobalDatabase) },
        icon = { Icon(Icons.Default.Storage, contentDescription = "Database", modifier = Modifier.size(20.dp)) },
        label = { Text("Database", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.RmMapping,
        onClick = { onSelectScreen(Screen.RmMapping) },
        icon = { Icon(Icons.Default.Group, contentDescription = "RM Mapping", modifier = Modifier.size(20.dp)) },
        label = { Text("RM Mapping", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.GoogleSheetsSync,
        onClick = { onSelectScreen(Screen.GoogleSheetsSync) },
        icon = { Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(20.dp)) },
        label = { Text("Sheets", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.More,
        onClick = { onSelectScreen(Screen.More) },
        icon = { Icon(Icons.Default.Widgets, contentDescription = "More", modifier = Modifier.size(20.dp)) },
        label = { Text("More", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
    }
  }
}

@Composable
fun EblNavigationRail(
  userRole: String,
  currentScreen: Screen,
  onSelectScreen: (Screen) -> Unit
) {
  NavigationRail(
    containerColor = EblNavyDark,
    contentColor = Color.White
  ) {
    if (userRole == "RM") {
      NavigationRailItem(
        selected = currentScreen is Screen.RmDashboard,
        onClick = { onSelectScreen(Screen.RmDashboard) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
        label = { Text("Dashboard") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.CustomerForm && currentScreen.editFileId == null,
        onClick = { onSelectScreen(Screen.CustomerForm(null)) },
        icon = { Icon(Icons.Default.AddCircle, contentDescription = "New File") },
        label = { Text("New File") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.CustomerList,
        onClick = { onSelectScreen(Screen.CustomerList) },
        icon = { Icon(Icons.Default.Folder, contentDescription = "My Files") },
        label = { Text("Files") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.Reports,
        onClick = { onSelectScreen(Screen.Reports) },
        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
        label = { Text("Reports") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.More,
        onClick = { onSelectScreen(Screen.More) },
        icon = { Icon(Icons.Default.Widgets, contentDescription = "More") },
        label = { Text("More") }
      )
    } else {
      NavigationRailItem(
        selected = currentScreen is Screen.AdminDashboard || currentScreen is Screen.MentorDashboard,
        onClick = { onSelectScreen(if (userRole == "ADMIN") Screen.AdminDashboard else Screen.MentorDashboard) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
        label = { Text("Dashboard") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.GlobalDatabase,
        onClick = { onSelectScreen(Screen.GlobalDatabase) },
        icon = { Icon(Icons.Default.Storage, contentDescription = "Database") },
        label = { Text("Database") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.RmMapping,
        onClick = { onSelectScreen(Screen.RmMapping) },
        icon = { Icon(Icons.Default.Group, contentDescription = "RM Mapping") },
        label = { Text("RM Mapping") }
      )
      NavigationRailItem(
        selected = currentScreen is Screen.Reports,
        onClick = { onSelectScreen(Screen.Reports) },
        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
        label = { Text("Reports") }
      )
      if (userRole == "MENTOR") {
        NavigationRailItem(
          selected = currentScreen is Screen.GoogleSheetsSync,
          onClick = { onSelectScreen(Screen.GoogleSheetsSync) },
          icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
          label = { Text("Sync") }
        )
      }
      NavigationRailItem(
        selected = currentScreen is Screen.More,
        onClick = { onSelectScreen(Screen.More) },
        icon = { Icon(Icons.Default.Widgets, contentDescription = "More") },
        label = { Text("More") }
      )
    }
  }
}
// End of MainActivity: Auto-refreshed for Live Streaming Preview

