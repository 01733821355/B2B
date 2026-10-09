package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
      // Backend info & Google Sheet sync is ONLY visible to Mentor
      if (user != null && user.role == "MENTOR") {
        IconButton(
          onClick = onSyncClicked,
          modifier = Modifier.testTag("top_bar_sync_button")
        ) {
          if (pendingSyncCount > 0) {
            BadgedBox(
              badge = {
                Badge(
                  containerColor = Color(0xFFEAB308),
                  contentColor = Color.Black
                ) {
                  Text(pendingSyncCount.toString(), fontSize = 9.sp)
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Google Sheets Sync",
                tint = Color.White
              )
            }
          } else {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = "Google Sheets Synced",
              tint = Color(0xFF4ADE80)
            )
          }
        }
      }

      if (user != null) {
        // SMS Notifications Action Button
        IconButton(
          onClick = { showSmsDialog = true },
          modifier = Modifier.testTag("top_bar_sms_button")
        ) {
          if (unreadSmsCount > 0 && user.role == "RM") {
            BadgedBox(
              badge = {
                Badge(
                  containerColor = Color(0xFFDC2626),
                  contentColor = Color.White
                ) {
                  Text(unreadSmsCount.toString(), fontSize = 9.sp)
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = "SMS Inbox",
                tint = Color.White
              )
            }
          } else {
            Icon(
              imageVector = Icons.Default.Sms,
              contentDescription = if (user.role == "RM") "SMS Inbox" else "RM SMS Logs",
              tint = Color.White.copy(alpha = 0.9f)
            )
          }
        }

        // Quick DBR & Checklist Tool Button
        IconButton(
          onClick = onOpenDbrChecklist,
          modifier = Modifier.testTag("top_bar_dbr_button")
        ) {
          Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = "DBR & Checklist Tool",
            tint = EblGold
          )
        }

        // Quick Team Communication & Calling Button
        IconButton(
          onClick = onOpenCommunication,
          modifier = Modifier.testTag("top_bar_communication_button")
        ) {
          Icon(
            imageVector = Icons.Default.PhoneInTalk,
            contentDescription = "Team Communication & Net Calling",
            tint = Color(0xFF38BDF8)
          )
        }

        // Quick Important Documents Repository Button
        IconButton(
          onClick = onOpenImportantDocuments,
          modifier = Modifier.testTag("top_bar_important_docs_button")
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Important Documents Repository",
            tint = Color(0xFF67E8F9)
          )
        }

        // Profile Menu
        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.testTag("top_bar_profile_button")
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "User Profile",
              tint = Color.White
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = {
                Column {
                  Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text("RM Code: ${user.rmCode} (${user.role})", fontSize = 11.sp, color = Color.Gray)
                  Text(user.officeAddress, fontSize = 10.sp, color = Color.Gray)
                }
              },
              onClick = {},
              enabled = false
            )
            DropdownMenuItem(
              text = { Text("DBR & Checklist Tool", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.Calculate, contentDescription = "DBR Tool", tint = EblNavyPrimary)
              },
              onClick = {
                showMenu = false
                onOpenDbrChecklist()
              },
              modifier = Modifier.testTag("menu_dbr_checklist")
            )
            DropdownMenuItem(
              text = { Text("Important Documents", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = "Important Documents", tint = EblNavyPrimary)
              },
              onClick = {
                showMenu = false
                onOpenImportantDocuments()
              },
              modifier = Modifier.testTag("menu_important_documents")
            )
            DropdownMenuItem(
              text = { Text("Change Password", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.LockReset, contentDescription = "Change Password")
              },
              onClick = {
                showMenu = false
                onChangePassword()
              },
              modifier = Modifier.testTag("menu_change_password")
            )
            DropdownMenuItem(
              text = { Text("Sign Out", color = Color(0xFFDC2626), fontSize = 13.sp) },
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
