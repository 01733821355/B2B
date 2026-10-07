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
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
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
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        EblMainApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun EblMainApp(viewModel: AppViewModel) {
  val currentUser by viewModel.currentUser.collectAsState()
  val currentScreen by viewModel.currentScreen.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val unreadSmsCount by viewModel.unreadSmsCount.collectAsState()
  val rmSmsList by viewModel.rmSmsNotifications.collectAsState()
  val allSmsList by viewModel.allSmsNotifications.collectAsState()
  val lastLoggedRmCode by viewModel.lastLoggedRmCode.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showPasswordDialog by remember { mutableStateOf(false) }
  var showRmSmsInboxModal by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) {
    NotificationHelper.markPermissionAsked(context)
  }

  // Prompt RM for notification permission only once
  LaunchedEffect(currentUser?.rmCode) {
    NotificationHelper.createNotificationChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (!NotificationHelper.hasAskedPermissionOnce(context)) {
        NotificationHelper.markPermissionAsked(context)
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Listen for uiMessages and show toast snackbar
  LaunchedEffect(Unit) {
    viewModel.uiMessage.collectLatest { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  // If user is not authenticated, show universal login screen
  if (currentUser == null) {
    LoginScreen(
      appCustomName = appCustomName,
      lastLoggedRmCode = lastLoggedRmCode,
      onLogin = { username, password, lat, lng, addr, callback ->
        viewModel.login(username, password, lat, lng, addr, callback)
      },
      onBiometricLogin = { rmCode, lat, lng, addr, callback ->
        viewModel.loginWithBiometrics(rmCode, lat, lng, addr, callback)
      }
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

      EblTopBar(
        user = user,
        appCustomName = appCustomName,
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
        onOpenDbrChecklist = { viewModel.navigateTo(Screen.DbrChecklist) }
      )
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

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f)
      ) {
        when (val screen = currentScreen) {
          is Screen.Login -> {
            // Already handled above
          }

          is Screen.RmDashboard -> {
            val stats by viewModel.kpiStats.collectAsState()
            val timeFilter by viewModel.selectedTimeFilter.collectAsState()
            val files by viewModel.filteredFiles.collectAsState()
            val allTargets by viewModel.allTargets.collectAsState()
            val rmTarget = allTargets.find { it.rmCode == user.rmCode }

            RmDashboardScreen(
              user = user,
              stats = stats,
              target = rmTarget,
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
              onUpdateLocation = { lat, lng, addr ->
                viewModel.updateUserLocation(user.rmCode, lat, lng, addr, "LIVE_DASHBOARD_BEACON")
              }
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
              onEditFile = { fileId -> viewModel.navigateTo(Screen.CustomerForm(fileId)) }
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
            DbrAndChecklistScreen(
              viewModel = viewModel,
              currentUser = user,
              onNavigateBack = { viewModel.navigateBack() }
            )
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
        selected = currentScreen is Screen.AppSettings,
        onClick = { onSelectScreen(Screen.AppSettings) },
        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp)) },
        label = { Text("Settings", fontSize = 9.sp) },
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
        selected = currentScreen is Screen.MentorUserLocationTracking,
        onClick = { onSelectScreen(Screen.MentorUserLocationTracking) },
        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Locations", modifier = Modifier.size(20.dp)) },
        label = { Text("Locations", fontSize = 9.sp) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = EblNavyPrimary)
      )
      NavigationBarItem(
        selected = currentScreen is Screen.GoogleSheetsSync,
        onClick = { onSelectScreen(Screen.GoogleSheetsSync) },
        icon = { Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(20.dp)) },
        label = { Text("Sheets", fontSize = 9.sp) },
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
        NavigationRailItem(
          selected = currentScreen is Screen.MentorUserLocationTracking,
          onClick = { onSelectScreen(Screen.MentorUserLocationTracking) },
          icon = { Icon(Icons.Default.LocationOn, contentDescription = "Locations") },
          label = { Text("Locations") }
        )
      }
    }
  }
}
// End of MainActivity: Auto-refreshed for Live Streaming Preview

