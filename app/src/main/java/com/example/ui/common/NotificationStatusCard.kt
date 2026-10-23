package com.example.ui.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblNavyPrimary
import com.example.util.NotificationHelper

/**
 * Universal Mobile Notification Status & Control Card.
 * Accessible to ALL user roles (Admin, Mentor, RM).
 * Allows checking permission, launching device settings, and firing a live test alert.
 */
@Composable
fun NotificationStatusCard(
  modifier: Modifier = Modifier,
  userRole: String = "RM",
  userName: String = "User"
) {
  val context = LocalContext.current
  var areNotifsEnabled by remember { mutableStateOf(NotificationHelper.areNotificationsEnabled(context)) }

  val containerBg = if (areNotifsEnabled) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
  val borderColor = if (areNotifsEnabled) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
  val titleColor = if (areNotifsEnabled) Color(0xFF15803D) else Color(0xFFB91C1C)
  val descColor = if (areNotifsEnabled) Color(0xFF166534) else Color(0xFF991B1B)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("universal_notification_status_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = containerBg),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (areNotifsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
          contentDescription = null,
          tint = if (areNotifsEnabled) Color(0xFF16A34A) else Color(0xFFDC2626),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Mobile Notification Status (App-Off Alerts)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = titleColor
          )
          Text(
            text = if (areNotifsEnabled)
              "✓ সক্রিয়: অ্যাপ বন্ধ থাকলেও গুগল শিট ও ক্লাউড নোটিফিকেশন সাউন্ড ও ভাইব্রেশন সহ মোবাইলে আসবে এবং অ্যাপের লোকাল স্টোরেজে সেভ হবে। সিম কার্ড বা মোবাইল ব্যালেন্স লাগে না।"
            else
              "✕ বন্ধ: আপনার ডিভাইসে নোটিফিকেশন অফ আছে। অনুগ্রহ করে সেটিংসে গিয়ে পারমিশন চালু করুন।",
            fontSize = 11.sp,
            color = descColor
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            try {
              val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
              }
              context.startActivity(intent)
            } catch (_: Exception) {
              try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
              } catch (_: Exception) {
                Toast.makeText(context, "Open Device Settings -> Apps -> Notifications", Toast.LENGTH_LONG).show()
              }
            }
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1E293B)
          ),
          border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
          modifier = Modifier.weight(1f).testTag("btn_notif_device_settings")
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = {
            NotificationHelper.createNotificationChannels(context)
            NotificationHelper.sendTestAlertNotification(
              context = context,
              title = "🔔 Test Notification Alert",
              message = "Hello $userName ($userRole)! Your background notification system is fully operational.",
              notificationId = (System.currentTimeMillis() % 100000).toInt()
            )
            areNotifsEnabled = NotificationHelper.areNotificationsEnabled(context)
            Toast.makeText(context, "টেস্ট নোটিফিকেশন পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.weight(1f).testTag("btn_notif_test_alert")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
