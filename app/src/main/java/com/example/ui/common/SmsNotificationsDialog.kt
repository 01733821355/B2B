package com.example.ui.common

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SmsNotificationEntity
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
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.85f)
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
                text = if (isRmView) "RM SMS Inbox" else "Dispatched RM SMS Logs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isRmView) "Alerts received from Admin & Mentor" else "Live record of all SMS dispatched to RMs",
                fontSize = 12.sp,
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

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Action Row with Clear SMS Option
        if (smsList.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (isRmView && smsList.any { !it.isRead }) {
              Text(
                text = "${smsList.count { !it.isRead }} unread alert(s)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFDC2626)
              )
            } else {
              Text(
                text = "${smsList.size} SMS notification(s)",
                fontSize = 12.sp,
                color = Color.Gray
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (isRmView && smsList.any { !it.isRead }) {
                TextButton(
                  onClick = onMarkAllRead,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Mark All Read", fontSize = 11.sp)
                }
              }

              TextButton(
                onClick = onClearAll,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("btn_clear_all_sms")
              ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // List
        if (smsList.isEmpty()) {
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
                modifier = Modifier.size(48.dp),
                tint = Color.LightGray
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (isRmView) "No SMS notifications" else "No SMS records dispatched yet",
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                fontSize = 14.sp
              )
              Text(
                text = "When Admin or Mentor updates or deletes files, SMS alerts appear here.",
                fontSize = 12.sp,
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
            items(smsList, key = { it.id }) { sms ->
              SmsItemCard(
                sms = sms,
                isRmView = isRmView,
                onMarkRead = { onMarkRead(sms.id) },
                onDelete = { onDeleteSms(sms.id) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (smsList.isNotEmpty()) {
            OutlinedButton(
              onClick = onClearAll,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_bottom_clear_sms")
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFDC2626))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear All SMS", fontSize = 12.sp)
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

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
}

@Composable
private fun SmsItemCard(
  sms: SmsNotificationEntity,
  isRmView: Boolean,
  onMarkRead: () -> Unit,
  onDelete: () -> Unit
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
      // Header: Sender & Action Badge & Timestamp & Delete icon
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
            "UPDATE" -> Color(0xFF2563EB)
            "RESTORE" -> Color(0xFF16A34A)
            "TARGET" -> Color(0xFF9333EA)
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
            onClick = onDelete,
            modifier = Modifier.size(24.dp).padding(start = 4.dp).testTag("btn_delete_sms_${sms.id}")
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
          fontSize = 13.sp,
          lineHeight = 18.sp,
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
