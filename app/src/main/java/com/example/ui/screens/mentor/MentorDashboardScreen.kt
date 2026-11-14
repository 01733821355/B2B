package com.example.ui.screens.mentor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.common.EblPullRefreshBox
import com.example.ui.common.KpiGridSection
import com.example.ui.common.TimeFilterBar
import com.example.ui.common.VoiceInputField
import com.example.ui.common.interactiveBounce
import com.example.ui.common.rememberBounceInteraction
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.DateUtils

@Composable
fun MentorDashboardScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.kpiStats.collectAsState()
  val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
  val auditLogs by viewModel.auditLogs.collectAsState()
  val allRms by viewModel.allRms.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val appCustomIconUrl by viewModel.appCustomIconUrl.collectAsState()
  val rmPerformanceList by viewModel.rmPerformanceList.collectAsState()
  val allFiles by viewModel.allFiles.collectAsState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var showDeletedRecordsDialog by remember { mutableStateOf(false) }
  var showEditAppNameDialog by remember { mutableStateOf(false) }
  var newAppNameInput by remember { mutableStateOf("") }
  var showEditAppIconDialog by remember { mutableStateOf(false) }
  var newAppIconUrlInput by remember { mutableStateOf("") }
  var mentorTargetSettingRow by remember { mutableStateOf<com.example.ui.viewmodel.RmPerformanceRow?>(null) }

  var showEditBannerUrlDialog by remember { mutableStateOf(false) }
  var newBannerUrlInput by remember { mutableStateOf("") }
  val bannerBackgroundUrl by viewModel.dashboardBannerBackgroundUrl.collectAsState()

  val iconPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
          context,
          uri,
          "app_icon_${System.currentTimeMillis()}.png"
        )
        if (saved != null) {
          viewModel.setAppCustomIcon(saved.storagePath) { success, _ ->
            if (success) {
              Toast.makeText(context, "Universal App Icon updated successfully!", Toast.LENGTH_SHORT).show()
            }
          }
        }
      }
    }
  }

  val bannerPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
          context,
          uri,
          "banner_${System.currentTimeMillis()}.jpg"
        )
        if (saved != null) {
          val localFile = java.io.File(saved.storagePath)
          Toast.makeText(context, "Uploading banner photo to Drive...", Toast.LENGTH_SHORT).show()
          viewModel.uploadAndSetDashboardBannerBackground(localFile, "banner_${System.currentTimeMillis()}.jpg", "image/jpeg") { ok, msg ->
            Toast.makeText(context, if (ok) "Universal Banner Background updated!" else "Upload failed: $msg", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(20_000)
      viewModel.refreshData()
    }
  }

  EblPullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refreshData() },
    modifier = modifier.fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("mentor_dashboard_screen")
    ) {
    // Mentor Command Theme Photo Banner
    item {
      com.example.ui.common.ThemeVisualBanner(
        title = "Operations Mentor Console",
        subtitle = "Executive Oversight • Authority Level (${currentUser.rmCode})",
        badgeText = "Mentor Governance • Operational Command",
        customBackgroundUrl = bannerBackgroundUrl,
        onUploadBackground = {
          bannerPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
      )
    }

    // Primary Mentor Action: Add New Customer File
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .interactiveBounce(pressedScale = 0.97f, onClick = { onNavigate(Screen.CustomerForm(null)) })
          .testTag("mentor_btn_add_customer"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F766E).copy(alpha = 0.25f) else Color(0xFFCCFBF1)),
        border = BorderStroke(1.25.dp, if (isDark) Color(0xFF0D9488) else Color(0xFF5EEAD4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Color(0xFF0F766E),
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "+ Add New Customer File",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF0F766E)
              )
              Text(
                text = "Register customer file & assign directly to RM officer",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF99F6E4) else Color(0xFF115E59)
              )
            }
          }
          Icon(
            Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = if (isDark) Color(0xFF99F6E4) else Color(0xFF0F766E),
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    // 3D Banking Card Portfolio Deck (VISA / Mastercard / UnionPay)
    item {
      com.example.ui.common.CardProductPortfolioStrip(
        files = allFiles,
        onCardClick = { onNavigate(Screen.GlobalDatabase) }
      )
    }

    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {




          Spacer(modifier = Modifier.height(10.dp))
          val isBioMentor = viewModel.isBiometricEnabled(currentUser.rmCode)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF0F2642))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint Login",
                tint = if (isBioMentor) Color(0xFF34D399) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Fingerprint Login",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = if (isBioMentor) "Enabled (Biometric Active)" else "Disabled (Password Required)",
                  fontSize = 9.sp,
                  color = if (isBioMentor) Color(0xFF34D399) else Color.LightGray
                )
              }
            }
            Switch(
              checked = isBioMentor,
              onCheckedChange = { viewModel.setBiometricEnabled(currentUser.rmCode, it) },
              modifier = Modifier.testTag("switch_mentor_biometric")
            )
          }
        }
      }

    // Pending RM Approvals Alert Banner (Only for Mentor)
    val pendingRms = allRms.filter { it.accountStatus == "PENDING_APPROVAL" }
    if (pendingRms.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFFB45309))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "${pendingRms.size} RM Assigned - Pending Approval",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E)
                )
                Text(
                  text = "Assigned by Admin • Requires Mentor authorization",
                  fontSize = 10.sp,
                  color = Color(0xFFB45309)
                )
              }
            }
            Button(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Approve", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Google Sheets Auto-Sync Card (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .interactiveBounce(pressedScale = 0.97f, onClick = { onNavigate(Screen.GoogleSheetsSync) })
          .testTag("mentor_card_sheets_sync"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.28f))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF064E3B), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Google Sheets Auto-Sync Console",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = Color(0xFF10B981).copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "MENTOR EXCLUSIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Auto-create tabs, formatted headers, and real-time data sync.",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
          }
        }
      }
    }

    // Mentor Live Location Tracking Feature Card (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .interactiveBounce(pressedScale = 0.97f, onClick = { onNavigate(Screen.MentorUserLocationTracking) })
          .testTag("mentor_card_user_location_radar"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, EblGold.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF0F325E), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = EblGold,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "User & RM Live Location Monitor",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = Color(0xFF10B981).copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "MENTOR ONLY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Real-time GPS tracking & field monitoring of RM locations in Dhaka.",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
          }
        }
      }
    }

    // Universal App Name Configuration (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF312E81), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = EblGold,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Universal App Name",
                fontSize = 11.sp,
                color = Color.Gray
              )
              Text(
                text = appCustomName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Text(
                text = "Universal across whole app • Configurable by Mentor",
                fontSize = 11.sp,
                color = Color(0xFF059669)
              )
            }
          }

          Button(
            onClick = {
              newAppNameInput = appCustomName
              showEditAppNameDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_mentor_edit_app_name")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Change", fontSize = 12.sp)
          }
        }
      }
    }

    // Universal App Icon / Logo Configuration (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF312E81)),
              contentAlignment = Alignment.Center
            ) {
              if (appCustomIconUrl.isNotBlank()) {
                AsyncImage(
                  model = appCustomIconUrl,
                  contentDescription = "Current App Icon",
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Image,
                  contentDescription = null,
                  tint = EblGold,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Universal App Icon / Logo",
                fontSize = 11.sp,
                color = Color.Gray
              )
              Text(
                text = if (appCustomIconUrl.isNotBlank()) "Custom Icon Active" else "Default Icon",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Text(
                text = "Syncs universally to all users' devices • Configured by Mentor",
                fontSize = 10.sp,
                color = Color(0xFF059669)
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = {
                iconPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_mentor_pick_app_icon")
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Upload", fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = {
                newAppIconUrlInput = appCustomIconUrl
                showEditAppIconDialog = true
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_mentor_edit_app_icon_url")
            ) {
              Text("URL", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Universal Dashboard Banner Background Photo Card
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp)
          .testTag("mentor_banner_bg_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF0284C7).copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AddPhotoAlternate,
                  contentDescription = null,
                  tint = Color(0xFF0284C7),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Dashboard Banner Photo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Text(
                text = if (bannerBackgroundUrl.isNotBlank()) "Photo active • Uploaded to Drive" else "Default corporate artwork active",
                fontSize = 10.sp,
                color = if (bannerBackgroundUrl.isNotBlank()) Color(0xFF059669) else Color.Gray
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = {
                bannerPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_mentor_pick_banner_photo")
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Upload", fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = {
                newBannerUrlInput = bannerBackgroundUrl
                showEditBannerUrlDialog = true
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_mentor_edit_banner_url")
            ) {
              Text("URL", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Time filter pills
    item {
      TimeFilterBar(
        selectedFilter = selectedTimeFilter,
        onFilterSelected = { viewModel.selectedTimeFilter.value = it }
      )
    }

    // Executive Metrics
    item {
      KpiGridSection(
        stats = stats,
        onKpiClick = { onNavigate(Screen.GlobalDatabase) }
      )
    }

    // RM-Wise Performance Breakdown for Mentor
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "RM-Wise Performance & Target Achievement",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Text(
                text = "STC, Approvals, Declines, Query/RTS and category quotas",
                fontSize = 10.sp,
                color = Color.Gray
              )
            }
          }

          // Table Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE8EEF5))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("RM Officer", modifier = Modifier.weight(1.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark, maxLines = 1, softWrap = false)
            Text("STC", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), maxLines = 1, softWrap = false)
            Text("Subm", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else EblNavyDark, maxLines = 1, softWrap = false)
            Text("Apprv", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), maxLines = 1, softWrap = false)
            Text("Query", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), maxLines = 1, softWrap = false)
            Text("RTS", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), maxLines = 1, softWrap = false)
            Text("Decl", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), maxLines = 1, softWrap = false)
          }

          if (rmPerformanceList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
              Text("No RM officers found.", color = Color.Gray, fontSize = 12.sp)
            }
          } else {
            val rankByRmCode = remember(rmPerformanceList) {
              rmPerformanceList.sortedByDescending { it.stats.stc }
                .mapIndexed { idx, r -> r.rmCode to (idx + 1) }
                .toMap()
            }
            rmPerformanceList.forEach { row ->
              val rank = rankByRmCode[row.rmCode] ?: 99
              val (rankBg, rankFg, rankBorder) = when (rank) {
                1 -> Triple(
                  if (isDark) Color(0xFF78350F).copy(alpha = 0.55f) else Color(0xFFFEF3C7),
                  if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                  if (isDark) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFFDE68A)
                )
                2 -> Triple(
                  if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                  if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569),
                  if (isDark) Color(0xFF64748B).copy(alpha = 0.6f) else Color(0xFFCBD5E1)
                )
                3 -> Triple(
                  if (isDark) Color(0xFF7C2D12).copy(alpha = 0.5f) else Color(0xFFFFEDD5),
                  if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C),
                  if (isDark) Color(0xFFEA580C).copy(alpha = 0.5f) else Color(0xFFFED7AA)
                )
                else -> Triple(
                  if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                  if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                  if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                )
              }

              val ccTarget = row.target?.getEffectiveCcTarget() ?: 20
              val corpTarget = row.target?.getEffectiveCorpTarget() ?: 10
              val b2bTarget = row.target?.getEffectiveB2bTarget() ?: 15
              val annualCc = row.target?.annualCreditCardTarget ?: (ccTarget * 4)
              val annualCorp = row.target?.annualCorporateCardTarget ?: (corpTarget * 4)
              val annualB2b = row.target?.annualB2bTarget ?: (b2bTarget * 4)
              val curQ = DateUtils.getCurrentQuarter()
              val customQuarter = row.allQuarterCustomTargets[curQ] ?: emptyMap()
              val customAnnual = row.allQuarterCustomTargets["ALL"] ?: emptyMap()
              val standardSet = setOf("Credit Card", "Corporate Card", "B2B")
              val customCriteriaEntries = (customQuarter.keys + customAnnual.keys + row.assignedCriteria)
                .map { com.example.data.repository.EblRepository.normalizeCriteria(it) }
                .distinctBy { it.lowercase() }
                .filter { k -> standardSet.none { it.equals(k, ignoreCase = true) } }
                .mapNotNull { critName ->
                  val qVal = customQuarter.entries.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: (customQuarter[critName] ?: 0)
                  val sumQ = (row.allQuarterCustomTargets["Q1"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                    (row.allQuarterCustomTargets["Q2"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                    (row.allQuarterCustomTargets["Q3"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                    (row.allQuarterCustomTargets["Q4"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0)
                  val yrVal = if (sumQ > 0) sumQ else (customAnnual.entries.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: (customAnnual[critName] ?: 0))
                  if (qVal > 0 || yrVal > 0 || row.assignedCriteria.any { it.trim().equals(critName.trim(), ignoreCase = true) }) {
                    critName to (qVal to yrVal)
                  } else null
                }

              val totalGoal = (ccTarget + corpTarget + b2bTarget + customCriteriaEntries.sumOf { it.second.first }).coerceAtLeast(1)
              val rawProgress = (row.stats.stc.toFloat() / totalGoal.toFloat()).coerceIn(0f, 1f)
              val animatedProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                label = "mentor_rm_progress_${row.rmCode}"
              )
              val pct = ((row.stats.stc.toFloat() / totalGoal.toFloat()) * 100f).toInt()
              val progressColor = when {
                pct >= 100 -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                pct >= 60 -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
                pct >= 30 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
              }

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .interactiveBounce(
                    pressedScale = 0.985f,
                    onClick = {
                      viewModel.selectedRmCodeFilter.value = row.rmCode
                      onNavigate(Screen.GlobalDatabase)
                    }
                  )
                  .padding(horizontal = 12.dp, vertical = 10.dp)
              ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Row(
                    modifier = Modifier.weight(1.7f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(7.dp),
                      color = rankBg,
                      border = BorderStroke(1.dp, rankBorder)
                    ) {
                      Text(
                        text = "#$rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = rankFg,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                      Text(row.rmName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark, maxLines = 1, softWrap = false)
                      Text("Code: ${row.rmCode}", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), maxLines = 1, softWrap = false)
                    }
                  }
                  Text("${row.stats.stc}", modifier = Modifier.weight(0.7f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E))
                  Text("${row.stats.submitted}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFFE2E8F0) else EblNavyDark)
                  Text("${row.stats.approved}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D))
                  Text("${row.stats.query}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
                  Text("${row.stats.returnToSource}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48))
                  Text("${row.stats.declined}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily, color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Compact STC Quota Progress Bar
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                      .weight(1f)
                      .height(5.dp)
                      .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                  )
                  Text(
                    text = "${row.stats.stc}/$totalGoal ($pct%)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = progressColor,
                    maxLines = 1,
                    softWrap = false
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFF0FDF4),
                      border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.55f) else Color(0xFFBBF7D0))
                    ) {
                      Text(
                        "CC: ${row.stats.stcCreditCardCount}/$ccTarget (Yr:$annualCc)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEFF6FF),
                      border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.55f) else Color(0xFFBFDBFE))
                    ) {
                      Text(
                        "Corp: ${row.stats.stcCorporateCardCount}/$corpTarget (Yr:$annualCorp)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isDark) Color(0xFF4C1D95).copy(alpha = 0.45f) else Color(0xFFFAF5FF),
                      border = BorderStroke(1.dp, if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.55f) else Color(0xFFE9D5FF))
                    ) {
                      Text(
                        "B2B: ${row.stats.stcB2bCount}/$b2bTarget (Yr:$annualB2b)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6B21A8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                    customCriteriaEntries.forEach { (critName, pair) ->
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFFDE68A))
                      ) {
                        Text(
                          "$critName: 0/${pair.first} (Yr:${pair.second})",
                          fontSize = 10.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1D4ED8).copy(alpha = 0.25f) else Color(0xFF2563EB).copy(alpha = 0.12f),
                    border = BorderStroke(0.75.dp, if (isDark) Color(0xFF60A5FA).copy(alpha = 0.45f) else Color(0xFF2563EB).copy(alpha = 0.3f)),
                    modifier = Modifier.interactiveBounce(pressedScale = 0.92f, onClick = { mentorTargetSettingRow = row })
                  ) {
                    Text(
                      text = "⚙ Set Target",
                      fontSize = 10.5.sp,
                      color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
          }
        }
      }
    }

    // Mentor Governance Panel (Soft Delete Recovery / Permanent Purge)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Mentor Data Integrity & Recovery Controls",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Inspect soft-deleted records across all RMs, restore accidentally deleted customer files, or permanently expunge audit-cleared files.",
            fontSize = 11.sp,
            color = Color.Gray
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                showDeletedRecordsDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_trash_bin")
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Recover Deleted Files", fontSize = 11.sp)
            }

            Button(
              onClick = { onNavigate(Screen.GoogleSheetsSync) },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_sync_now")
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Sheets", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Recent Audit Logs Live Stream
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Real-Time Security Audit Stream",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "View All",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyPrimary,
          modifier = Modifier.clickable { onNavigate(Screen.AuditLogs) }
        )
      }
    }

    items(auditLogs.take(6)) { log ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = log.action,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("• ${log.userId} (${log.role})", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(log.details, fontSize = 11.sp, color = Color.DarkGray)
            Text(
              text = DateUtils.formatDateTime(log.timestamp),
              fontSize = 10.sp,
              color = Color.Gray
            )
          }
        }
      }
    }

    }

    val mentorHaptics = LocalHapticFeedback.current
    val (mentorFabInteraction, _) = rememberBounceInteraction(0.92f)
    FloatingActionButton(
      onClick = {
        mentorHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onNavigate(Screen.CustomerForm(null))
      },
      interactionSource = mentorFabInteraction,
      containerColor = Color(0xFF0F766E),
      contentColor = Color.White,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .border(1.5.dp, EblGold.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
        .interactiveBounce(pressedScale = 0.92f, interactionSource = mentorFabInteraction)
        .testTag("mentor_fab_add_customer")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Customer")
        Spacer(modifier = Modifier.width(6.dp))
        Text("New File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }

  // Universal App Name Edit Dialog
  if (showEditAppNameDialog) {
    AlertDialog(
      onDismissRequest = { showEditAppNameDialog = false },
      title = { Text("Universal App Name", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            "Change the name of the app manually. This name will immediately reflect across all screens, headers, and portals universally for all users.",
            fontSize = 12.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(12.dp))
          VoiceInputField(
            value = newAppNameInput,
            onValueChange = { newAppNameInput = it },
            label = "Universal App Name *",
            placeholder = "Enter custom app name...",
            testTag = "input_custom_app_name"
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAppNameInput.isNotBlank()) {
              viewModel.setAppCustomName(newAppNameInput.trim()) { _, _ -> }
              showEditAppNameDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Universal Name")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showEditAppNameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Universal App Icon / Logo Edit Dialog
  if (showEditAppIconDialog) {
    AlertDialog(
      onDismissRequest = { showEditAppIconDialog = false },
      title = { Text("Universal App Icon / Logo Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
      text = {
        Column {
          Text(
            "Enter an image URL or Google Drive link, or upload an image directly from device:",
            fontSize = 12.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newAppIconUrlInput,
            onValueChange = { newAppIconUrlInput = it },
            placeholder = { Text("https://example.com/logo.png or drive link", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().testTag("input_new_app_icon_url"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              showEditAppIconDialog = false
              iconPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pick from Device Gallery", fontSize = 11.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newAppIconUrlInput.trim()
            viewModel.setAppCustomIcon(clean) { success, _ ->
              if (success) {
                showEditAppIconDialog = false
                Toast.makeText(context, "Universal App Icon updated successfully!", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Icon")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showEditAppIconDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Mentor Deleted Records Recovery Dialog
  if (showDeletedRecordsDialog) {
    var searchQuery by remember { mutableStateOf("") }
    val deletedFiles = remember(allFiles) { allFiles.filter { it.isDeleted } }
    val filteredDeleted = remember(deletedFiles, searchQuery) {
      if (searchQuery.isBlank()) deletedFiles else {
        val q = searchQuery.trim().lowercase()
        deletedFiles.filter {
          it.customerName.lowercase().contains(q) ||
          it.fileId.lowercase().contains(q) ||
          it.mobile.lowercase().contains(q) ||
          it.assignedRmCode.lowercase().contains(q) ||
          it.companyName.lowercase().contains(q)
        }
      }
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showDeletedRecordsDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.88f)
          .testTag("mentor_deleted_records_dialog")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          // Dialog Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .background(Color(0xFF7C3AED).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                  .padding(8.dp)
              ) {
                Icon(
                  Icons.Default.RestoreFromTrash,
                  contentDescription = null,
                  tint = Color(0xFF7C3AED),
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Deleted Records Recovery",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Text(
                  text = "Recover deleted records & reload to sheet (${deletedFiles.size} records)",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }

            if (deletedFiles.isNotEmpty()) {
              OutlinedButton(
                onClick = {
                  deletedFiles.forEach { file ->
                    viewModel.restoreFile(file.fileId)
                  }
                  Toast.makeText(context, "✓ ${deletedFiles.size} records recovered successfully!", Toast.LENGTH_SHORT).show()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_recover_all_entries")
              ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recover All", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Search Field
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, phone, RM code...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_search_deleted_records")
          )

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
          Spacer(modifier = Modifier.height(8.dp))

          // List of Deleted Entries
          if (filteredDeleted.isEmpty()) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(48.dp),
                  tint = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = if (deletedFiles.isEmpty()) "No deleted records found" else "No matching records found",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Text(
                  text = if (deletedFiles.isEmpty()) "All files are currently active." else "Try searching with a different name or phone.",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredDeleted, key = { it.fileId }) { file ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC)),
                  border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deleted_file_item_${file.fileId}")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = file.customerName,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          color = Color(0xFFFEE2E2),
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = "DELETED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "File ID: ${file.fileId} ${if (file.ccNumber.isNotBlank()) "• CC: ${file.ccNumber}" else ""} • ${file.productType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                      )

                      if (file.companyName.isNotBlank()) {
                        Text(
                          text = "Company: ${file.companyName}",
                          fontSize = 11.sp,
                          color = Color.Gray
                        )
                      }

                      Text(
                        text = "Mobile: ${file.mobile} • Assigned RM: ${file.assignedRmCode}",
                        fontSize = 11.sp,
                        color = Color(0xFF334155)
                      )

                      Text(
                        text = "Deleted by: ${file.updatedBy} • ${DateUtils.formatDateTime(file.updatedAt)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Individual Recover Button
                    Button(
                      onClick = {
                        viewModel.restoreFile(file.fileId)
                        Toast.makeText(
                          context,
                          "✓ '${file.customerName}' recovered & synced to sheet!",
                          Toast.LENGTH_SHORT
                        ).show()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("btn_recover_file_${file.fileId}")
                    ) {
                      Icon(
                        Icons.Default.Restore,
                        contentDescription = "Recover",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Recover",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFE2E8F0))
          Spacer(modifier = Modifier.height(8.dp))

          // Bottom Close Action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { showDeletedRecordsDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_close_deleted_dialog")
            ) {
              Text("Close")
            }
          }
        }
      }
    }
  }

  // Set RM Quarter Targets Dialog for Mentor
  if (mentorTargetSettingRow != null) {
    val row = mentorTargetSettingRow!!
    val targetCriteria by viewModel.targetCriteriaOptions.collectAsState()
    val customTargets by viewModel.getRmCustomTargetsFlow(row.rmCode).collectAsState(initial = row.allQuarterCustomTargets["ALL"] ?: emptyMap())
    val allQuarterCustomTargets by viewModel.getRmAllQuarterCustomTargetsFlow(row.rmCode).collectAsState(initial = row.allQuarterCustomTargets)
    val assignedCriteria by viewModel.getRmAssignedCriteriaFlow(row.rmCode).collectAsState(initial = row.assignedCriteria)

    com.example.ui.common.QuarterTargetSettingDialog(
      rmCode = row.rmCode,
      rmName = row.rmName,
      currentTarget = row.target,
      allCriteria = targetCriteria,
      assignedCriteria = if (assignedCriteria.isNotEmpty()) assignedCriteria else row.assignedCriteria,
      onAddCriteria = { newCrit -> viewModel.addTargetCriterion(newCrit) },
      onDeleteCriteria = { crit -> viewModel.deleteTargetCriterion(crit) },
      initialCustomTargets = if (customTargets.isNotEmpty()) customTargets else (row.allQuarterCustomTargets["ALL"] ?: emptyMap()),
      allQuarterCustomTargets = if (allQuarterCustomTargets.isNotEmpty()) allQuarterCustomTargets else row.allQuarterCustomTargets,
      onDismiss = { mentorTargetSettingRow = null },
      onSave = { quarter, year, cc, corp, b2b ->
        viewModel.setRmQuarterTargets(row.rmCode, quarter, year, cc, corp, b2b) { success, _ ->
          if (success) mentorTargetSettingRow = null
        }
      },
      onSaveAllQuarters = { year, q1Cc, q1Corp, q1B2b, q2Cc, q2Corp, q2B2b, q3Cc, q3Corp, q3B2b, q4Cc, q4Corp, q4B2b ->
        viewModel.setRmAllQuarterTargets(
          rmCode = row.rmCode,
          year = year,
          q1Cc = q1Cc, q1Corp = q1Corp, q1B2b = q1B2b,
          q2Cc = q2Cc, q2Corp = q2Corp, q2B2b = q2B2b,
          q3Cc = q3Cc, q3Corp = q3Corp, q3B2b = q3B2b,
          q4Cc = q4Cc, q4Corp = q4Corp, q4B2b = q4B2b
        ) { success, _ ->
          if (success) mentorTargetSettingRow = null
        }
      },
      onSaveCustomTargets = { quarter, customMap ->
        viewModel.saveRmCustomTargets(row.rmCode, customMap, quarter)
      },
      onSaveAllQuarterCustomTargets = { quarterlyMap ->
        viewModel.saveRmCustomTargetsAllQuarters(row.rmCode, quarterlyMap)
      },
      onSaveAssignedCriteria = { assignedList ->
        viewModel.saveRmAssignedCriteria(row.rmCode, assignedList)
      },
      onSaveAllTargetsUnified = { year, quarterlyMap, assignedList ->
        viewModel.saveAllQuarterTargetsUnified(row.rmCode, year, quarterlyMap, assignedList) { success ->
          if (success) mentorTargetSettingRow = null
        }
      }
    )
  }

  if (showEditBannerUrlDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showEditBannerUrlDialog = false },
      title = { Text("Set Banner Background URL", fontWeight = FontWeight.Bold) },
      text = {
        androidx.compose.material3.OutlinedTextField(
          value = newBannerUrlInput,
          onValueChange = { newBannerUrlInput = it },
          label = { Text("Banner Image URL") },
          modifier = Modifier.fillMaxWidth().testTag("input_mentor_banner_url")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.setDashboardBannerBackground(newBannerUrlInput.trim()) { ok, msg ->
              Toast.makeText(context, if (ok) "Banner URL updated!" else "Error: $msg", Toast.LENGTH_SHORT).show()
              if (ok) showEditBannerUrlDialog = false
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { showEditBannerUrlDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
