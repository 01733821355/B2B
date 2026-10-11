package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmsNotificationEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EblTopBar(
  user: UserEntity?,
  appCustomName: String = "RM File Management Suite",
  canNavigateBack: Boolean,
  onNavigateBack: () -> Unit,
  onLogout: () -> Unit,
  onChangePassword: () -> Unit,
  onSyncClicked: () -> Unit = {},
  pendingSyncCount: Int = 0,
  unreadSmsCount: Int = 0,
  smsList: List<SmsNotificationEntity> = emptyList(),
  onMarkSmsRead: (Long) -> Unit = {},
  onMarkAllSmsRead: () -> Unit = {},
  onClearSms: () -> Unit = {},
  onDeleteSms: (Long) -> Unit = {},
  onOpenDbrChecklist: () -> Unit = {},
  onOpenCommunication: () -> Unit = {},
  onOpenImportantDocuments: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  var showSmsDialog by remember { mutableStateOf(false) }
  var dhakaTimeText by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    while (true) {
      val now = LocalTime.now(DateUtils.DHAKA_ZONE)
      dhakaTimeText = now.format(formatter)
      delay(10000)
    }
  }

  TopAppBar(
    modifier = modifier.testTag("ebl_top_bar"),
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = EblNavyDark,
      titleContentColor = Color.White,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White
    ),
    navigationIcon = {
      if (canNavigateBack) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("top_bar_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White
          )
        }
      } else {
        // App Monogram Badge
        Box(
          modifier = Modifier
            .padding(start = 12.dp)
            .size(36.dp)
            .background(EblGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "App Icon",
            tint = EblNavyDark,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    },
    title = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = appCustomName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (user != null) {
            val roleBg = when (user.role) {
              "ADMIN" -> Color(0xFFEF4444)
              "MENTOR" -> Color(0xFF8B5CF6)
              else -> EblNavyPrimary
            }
            Box(
              modifier = Modifier
                .background(roleBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${user.role}: ${user.rmCode}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
        Text(
          text = "Dhaka BST: $dhakaTimeText | Operations",
          fontSize = 10.sp,
          color = Color.White.copy(alpha = 0.7f)
        )
      }
    },
    actions = {
      if (user != null) {
        val totalBadgeCount = (if (user.role == "RM") unreadSmsCount else 0) + pendingSyncCount

        // Clean Profile Icon with optional notification badge
        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.testTag("top_bar_profile_button")
          ) {
            if (totalBadgeCount > 0) {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                  ) {
                    Text(totalBadgeCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = "User Profile & Menu",
                  tint = Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
            } else {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "User Profile & Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
            }
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            // User identity header
            DropdownMenuItem(
              text = {
                Column {
                  Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
                  Text("Code: ${user.rmCode} (${user.role})", fontSize = 11.sp, color = Color.Gray)
                  if (user.officeAddress.isNotBlank()) {
                    Text(user.officeAddress, fontSize = 10.sp, color = Color(0xFF64748B))
                  }
                }
              },
              onClick = {},
              enabled = false
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // 1. Team Communication & Net Calling
            DropdownMenuItem(
              text = { Text("Team Chat & Calling", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
              leadingIcon = {
                Icon(Icons.Default.Call, contentDescription = "Communication", tint = Color(0xFF0284C7))
              },
              onClick = {
                showMenu = false
                onOpenCommunication()
              },
              modifier = Modifier.testTag("menu_communication_hub")
            )

            // 2. DBR & Checklist Tool
            DropdownMenuItem(
              text = { Text("DBR & Checklist Tool", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
              leadingIcon = {
                Icon(Icons.Default.Calculate, contentDescription = "DBR Tool", tint = EblGold)
              },
              onClick = {
                showMenu = false
                onOpenDbrChecklist()
              },
              modifier = Modifier.testTag("menu_dbr_checklist")
            )

            // 3. Important Documents Repository
            DropdownMenuItem(
              text = { Text("Important Documents", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
              leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = "Important Documents", tint = EblNavyPrimary)
              },
              onClick = {
                showMenu = false
                onOpenImportantDocuments()
              },
              modifier = Modifier.testTag("menu_important_documents")
            )

            // 4. SMS Inbox / Notifications
            DropdownMenuItem(
              text = {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(if (user.role == "RM") "SMS Inbox" else "RM SMS Logs", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  if (unreadSmsCount > 0 && user.role == "RM") {
                    Surface(
                      color = Color(0xFFDC2626),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text(
                        text = "$unreadSmsCount new",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
              },
              leadingIcon = {
                Icon(Icons.Default.Sms, contentDescription = "SMS Notifications", tint = Color(0xFF10B981))
              },
              onClick = {
                showMenu = false
                showSmsDialog = true
              },
              modifier = Modifier.testTag("menu_sms_notifications")
            )

            // 5. Google Sheets Sync Console (Mentor Only)
            if (user.role == "MENTOR") {
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Sheets Auto-Sync", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (pendingSyncCount > 0) {
                      Surface(
                        color = Color(0xFFEAB308),
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text(
                          text = "$pendingSyncCount pending",
                          color = Color.Black,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                },
                leadingIcon = {
                  Icon(Icons.Default.Sync, contentDescription = "Google Sheets Sync", tint = Color(0xFF059669))
                },
                onClick = {
                  showMenu = false
                  onSyncClicked()
                },
                modifier = Modifier.testTag("menu_google_sheets_sync")
              )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // 6. Change Password
            DropdownMenuItem(
              text = { Text("Change Password", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.LockReset, contentDescription = "Change Password", tint = Color(0xFF64748B))
              },
              onClick = {
                showMenu = false
                onChangePassword()
              },
              modifier = Modifier.testTag("menu_change_password")
            )

            // 7. Sign Out
            DropdownMenuItem(
              text = { Text("Sign Out", color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
              leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFFDC2626))
              },
              onClick = {
                showMenu = false
                onLogout()
              },
              modifier = Modifier.testTag("menu_sign_out")
            )
          }
        }
      }
    }
  )

  if (showSmsDialog && user != null) {
    SmsNotificationsDialog(
      isRmView = user.role == "RM",
      smsList = smsList,
      onDismiss = { showSmsDialog = false },
      onMarkRead = onMarkSmsRead,
      onMarkAllRead = onMarkAllSmsRead,
      onClearAll = onClearSms,
      onDeleteSms = onDeleteSms
    )
  }
}
