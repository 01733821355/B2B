package com.example.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SmsNotificationEntity
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.util.DateUtils

@Composable
fun SmsNotificationsDialog(
  isRmView: Boolean,
  smsList: List<SmsNotificationEntity>,
  onDismiss: () -> Unit,
  onMarkRead: (Long) -> Unit = {},
  onMarkAllRead: () -> Unit = {},
  onClearAll: () -> Unit = {},
  onDeleteSms: (Long) -> Unit = {}
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var showConfirmClearDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val unreadCount = smsList.count { !it.isRead }

  val filteredList = smsList.filter { sms ->
    // Category Filter
    val matchesCategory = when (selectedCategoryFilter) {
      "Unread" -> !sms.isRead
      "Transfers" -> sms.actionType.equals("TRANSFER", ignoreCase = true)
      "Deletions" -> sms.actionType.contains("DELETE", ignoreCase = true)
      "Updates" -> sms.actionType.equals("UPDATE", ignoreCase = true)
      else -> true
    }

    // Search Query
    val matchesQuery = if (searchQuery.isBlank()) true else {
      val q = searchQuery.trim().lowercase()
      sms.messageText.lowercase().contains(q) ||
        sms.recipientName.lowercase().contains(q) ||
        sms.recipientRmCode.lowercase().contains(q) ||
        (sms.customerName?.lowercase()?.contains(q) == true) ||
        (sms.fileId?.lowercase()?.contains(q) == true)
    }

    matchesCategory && matchesQuery
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .testTag("sms_notifications_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Sms,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = if (isRmView) "RM SMS Notification Center" else "Dispatched RM SMS Logs",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = if (isRmView) "Real-time alerts from Admin & Mentor" else "Audit trail of SMS dispatched across teams",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_close_sms_dialog")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by customer, RM, file ID or keyword...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("input_search_sms"),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EblNavyPrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1)
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Organized Filter Chips Row
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          item {
            FilterChip(
              selected = selectedCategoryFilter == "All",
              onClick = { selectedCategoryFilter = "All" },
              label = { Text("All (${smsList.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EblNavyPrimary, selectedLabelColor = Color.White)
            )
          }
          if (isRmView && unreadCount > 0) {
            item {
              FilterChip(
                selected = selectedCategoryFilter == "Unread",
                onClick = { selectedCategoryFilter = "Unread" },
                label = { Text("Unread ($unreadCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDC2626), selectedLabelColor = Color.White)
              )
            }
          }
          item {
            FilterChip(
              selected = selectedCategoryFilter == "Transfers",
              onClick = { selectedCategoryFilter = "Transfers" },
              label = { Text("Transfers", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF9333EA), selectedLabelColor = Color.White)
            )
          }
          item {
            FilterChip(
              selected = selectedCategoryFilter == "Updates",
              onClick = { selectedCategoryFilter = "Updates" },
              label = { Text("Updates", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF2563EB), selectedLabelColor = Color.White)
            )
          }
          item {
            FilterChip(
              selected = selectedCategoryFilter == "Deletions",
              onClick = { selectedCategoryFilter = "Deletions" },
              label = { Text("Deletions", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDC2626), selectedLabelColor = Color.White)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(6.dp))

        // Action Toolbar: Mark Read / Clear SMS
        if (smsList.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Showing ${filteredList.size} of ${smsList.size} alerts",
              fontSize = 11.sp,
              color = Color.Gray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (isRmView && unreadCount > 0) {
                TextButton(
                  onClick = onMarkAllRead,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Mark All Read", fontSize = 11.sp)
                }
              }

              TextButton(
                onClick = { showConfirmClearDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("btn_clear_all_sms")
              ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // SMS List
        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = Color.LightGray
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotEmpty()) "No alerts matching '$searchQuery'" else "No SMS notifications in this filter",
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                fontSize = 13.sp
              )
              Text(
                text = "When customer files or RM assignments are updated, notifications appear here.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp)
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredList, key = { it.id }) { sms ->
              OrganizedSmsCard(
                sms = sms,
                isRmView = isRmView,
                onMarkRead = { onMarkRead(sms.id) },
                onDelete = { onDeleteSms(sms.id) },
                onCopy = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("EBL SMS Alert", sms.messageText)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "SMS text copied!", Toast.LENGTH_SHORT).show()
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Dismiss
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Close")
          }
        }
      }
    }
  }

  // Confirm Clear All Dialog
  if (showConfirmClearDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmClearDialog = false },
      title = { Text("Clear All SMS Alerts?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
      text = {
        Text("Are you sure you want to clear your SMS notification list? This will remove all records from this view.")
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmClearDialog = false
            onClearAll()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showConfirmClearDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun OrganizedSmsCard(
  sms: SmsNotificationEntity,
  isRmView: Boolean,
  onMarkRead: () -> Unit,
  onDelete: () -> Unit,
  onCopy: () -> Unit
) {
  val isUnread = isRmView && !sms.isRead
  val cardBg = if (isUnread) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  val borderColor = if (isUnread) Color(0xFF93C5FD) else Color.Transparent

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = if (isUnread) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("sms_card_${sms.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Header: Sender & Action Badge & Timestamp & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          val badgeColor = when (sms.actionType.uppercase()) {
            "DELETE", "PERMANENT_DELETE" -> Color(0xFFDC2626)
            "TRANSFER" -> Color(0xFF9333EA)
            "UPDATE" -> Color(0xFF2563EB)
            "RESTORE" -> Color(0xFF16A34A)
            "TARGET" -> Color(0xFF0F766E)
            else -> Color(0xFF0284C7)
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor
          ) {
            Text(
              text = sms.actionType.uppercase(),
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = "From: ${sms.triggeredByRole} (${sms.triggeredByCode})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = DateUtils.formatDateTime(sms.sentTimestamp),
            fontSize = 10.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
          )
          IconButton(
            onClick = onCopy,
            modifier = Modifier.size(24.dp).padding(start = 2.dp)
          ) {
            Icon(
              Icons.Default.ContentCopy,
              contentDescription = "Copy SMS",
              tint = Color.Gray,
              modifier = Modifier.size(14.dp)
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp).padding(start = 2.dp).testTag("btn_delete_sms_${sms.id}")
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Delete",
              tint = Color.Gray.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Recipient info if Admin/Mentor view
      if (!isRmView) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Gray)
          Text(
            text = "Recipient: ${sms.recipientName} (RM: ${sms.recipientRmCode}) • ${sms.recipientMobile}",
            fontSize = 11.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
      }

      // Message Body
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = sms.messageText,
          fontSize = 12.sp,
          lineHeight = 17.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(10.dp)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Status Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (sms.status == "DELIVERED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
          ) {
            Text(
              text = if (sms.status == "DELIVERED") "✓ SMS Delivered" else "• In-App Alert",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (sms.status == "DELIVERED") Color(0xFF166534) else Color(0xFF92400E),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (sms.recipientMobile.isNotBlank()) {
            Text(
              text = "Sent to: ${sms.recipientMobile}",
              fontSize = 10.sp,
              color = Color.Gray
            )
          }
        }

        if (isUnread) {
          TextButton(
            onClick = onMarkRead,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark Read", fontSize = 11.sp)
          }
        }
      }
    }
  }
}
