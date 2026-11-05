package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import com.example.util.BiometricHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.LocalAppTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun MoreScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = LocalAppTheme.current.isDark

  val screenBg = MaterialTheme.colorScheme.background
  val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
  val primaryText = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
  val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(screenBg)
      .testTag("more_tools_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header Banner
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isDark) Color(0xFF1E293B) else EblNavyDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("more_screen_hero_card")
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0xFF2563EB).copy(alpha = 0.25f),
            modifier = Modifier.size(46.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Business,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Tools & Operations Hub",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Access calculators, checklists, routes, files and utilities",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }
    }

    // Security & Biometric Sign In
    item {
      val context = LocalContext.current
      val isBiometricEnabled = viewModel.isBiometricEnabled(currentUser.rmCode)
      val bioStatus = remember { BiometricHelper.checkBiometricAvailability(context) }

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("more_tool_biometric_card")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF065F46).copy(alpha = 0.3f) else Color(0xFFD1FAE5),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint Login",
                tint = Color(0xFF059669),
                modifier = Modifier.size(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Fingerprint Login",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isBiometricEnabled) Color(0xFF059669).copy(alpha = 0.15f) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
              ) {
                Text(
                  text = if (isBiometricEnabled) "Enabled" else "Off",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isBiometricEnabled) Color(0xFF059669) else secondaryText,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isBiometricEnabled) "Fast biometric sign-in enabled for RM ${currentUser.rmCode}" else "Enable instant biometric sign-in with your fingerprint",
              fontSize = 11.sp,
              color = secondaryText,
              lineHeight = 15.sp
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Switch(
            checked = isBiometricEnabled,
            onCheckedChange = { enabled ->
              if (enabled && bioStatus != BiometricHelper.BiometricAvailability.AVAILABLE) {
                val errorMsg = when (bioStatus) {
                  BiometricHelper.BiometricAvailability.NO_HARDWARE -> "Fingerprint sensor is not available on this device."
                  BiometricHelper.BiometricAvailability.NONE_ENROLLED -> "No fingerprint enrolled. Please enroll a fingerprint in Android Settings."
                  else -> "Biometric authentication is not supported or currently unavailable."
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
              } else {
                viewModel.setBiometricEnabled(currentUser.rmCode, enabled)
                val msg = if (enabled) "✓ Fingerprint login enabled" else "Fingerprint login disabled"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("switch_more_biometric")
          )
        }
      }
    }

    // Section 1: Financial & Calculation Tools
    item {
      Text(
        text = "Financial & Calculation Tools",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = primaryText,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
      )
    }

    // 1. DBR Calculator Card
    item {
      ToolActionCard(
        title = "DBR Calculator",
        subtitle = "Debt Burden Ratio calculation for salaried, business & self-employed applicants",
        badge = "Smart Formula",
        icon = Icons.Default.Calculate,
        iconTint = Color(0xFFD97706),
        iconBg = Color(0xFFFEF3C7),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = {
          viewModel.dbrChecklistInitialTab.value = 0
          onNavigate(Screen.DbrChecklist)
        },
        testTag = "more_tool_dbr_calculator"
      )
    }

    // 2. Checklist Sender Card
    item {
      ToolActionCard(
        title = "Checklist Sender",
        subtitle = "Generate customized document requirement lists & instant WhatsApp / SMS checklist",
        badge = "WhatsApp / SMS",
        icon = Icons.Default.Checklist,
        iconTint = Color(0xFF059669),
        iconBg = Color(0xFFD1FAE5),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = {
          viewModel.dbrChecklistInitialTab.value = 1
          onNavigate(Screen.DbrChecklist)
        },
        testTag = "more_tool_checklist_sender"
      )
    }

    // 3. Age & Product Eligibility Calculator Card
    item {
      ToolActionCard(
        title = "Age Calculator",
        subtitle = "Exact current age calculation from date of birth with bank loan & card eligibility limits",
        badge = "Eligibility Check",
        icon = Icons.Default.CalendarMonth,
        iconTint = Color(0xFF7C3AED),
        iconBg = Color(0xFFEDE9FE),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = {
          viewModel.dbrChecklistInitialTab.value = 2
          onNavigate(Screen.DbrChecklist)
        },
        testTag = "more_tool_age_calculator"
      )
    }

    // Section 2: Operations & Field Work
    item {
      Text(
        text = "Field & Sales Operations",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = primaryText,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
      )
    }

    // 4. Company Profile Search Card
    item {
      ToolActionCard(
        title = "Company Profile Search",
        subtitle = "Search approved corporate companies, categorized segments (A/B/C) & requirements",
        badge = "EBL Approved",
        icon = Icons.Default.Business,
        iconTint = Color(0xFF2563EB),
        iconBg = Color(0xFFDBEAFE),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.CompanyProfile) },
        testTag = "more_tool_company_profile"
      )
    }

    // 5. Field Route Planner Card
    item {
      ToolActionCard(
        title = "Field Route Planner",
        subtitle = "Optimize customer visits & pending document collections with GPS map routing",
        badge = "GPS Routing",
        icon = Icons.Default.Map,
        iconTint = Color(0xFF0284C7),
        iconBg = Color(0xFFE0F2FE),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.FieldRoutePlanner()) },
        testTag = "more_tool_route_planner"
      )
    }

    // 6. Important Documents Card
    item {
      ToolActionCard(
        title = "Important Documents",
        subtitle = "Access bank policies, product circulars, application forms & official attachments",
        badge = "Cloud Docs",
        icon = Icons.Default.Description,
        iconTint = Color(0xFF0D9488),
        iconBg = Color(0xFFCCFBF1),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.ImportantDocuments) },
        testTag = "more_tool_important_docs"
      )
    }

    // 7. Team Communication Hub Card
    item {
      ToolActionCard(
        title = "Team Connect & Noticeboard",
        subtitle = "Real-time communication, events, noticeboard announcements & broadcast updates",
        badge = "Live Hub",
        icon = Icons.Default.Forum,
        iconTint = Color(0xFFE11D48),
        iconBg = Color(0xFFFFE4E6),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.CommunicationHub) },
        testTag = "more_tool_communication_hub"
      )
    }

    // Section 3: Performance & System Management
    item {
      Text(
        text = "Analytics & System",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = primaryText,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
      )
    }

    // 8. Performance Reports Card
    item {
      ToolActionCard(
        title = "Performance Reports",
        subtitle = "Filter files by date, product & status; generate CSV exports & detailed summaries",
        badge = "Analytics",
        icon = Icons.Default.Assessment,
        iconTint = Color(0xFF4F46E5),
        iconBg = Color(0xFFEEF2FF),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.Reports) },
        testTag = "more_tool_reports"
      )
    }

    // 9. Google Sheets Sync Card
    item {
      ToolActionCard(
        title = "Google Sheets Sync",
        subtitle = "Real-time bi-directional live sync with Google Spreadsheet & Drive storage",
        badge = "Live Sync",
        icon = Icons.Default.Sync,
        iconTint = Color(0xFF059669),
        iconBg = Color(0xFFD1FAE5),
        cardBg = cardBg,
        primaryText = primaryText,
        secondaryText = secondaryText,
        onClick = { onNavigate(Screen.GoogleSheetsSync) },
        testTag = "more_tool_google_sheets_sync"
      )
    }

    // Admin & Mentor exclusive tools
    if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
      item {
        ToolActionCard(
          title = "Live Location Radar",
          subtitle = "Monitor real-time field locations, active pings & distance to branch for all RMs",
          badge = "Field Radar",
          icon = Icons.Default.LocationOn,
          iconTint = Color(0xFFEA580C),
          iconBg = Color(0xFFFFEDD5),
          cardBg = cardBg,
          primaryText = primaryText,
          secondaryText = secondaryText,
          onClick = { onNavigate(Screen.MentorUserLocationTracking) },
          testTag = "more_tool_live_location"
        )
      }
    }

    if (currentUser.role == "ADMIN") {
      item {
        ToolActionCard(
          title = "System Settings & Preferences",
          subtitle = "Configure app branding, name, custom criteria, font styles, sync & system configurations",
          badge = "Admin Config",
          icon = Icons.Default.Settings,
          iconTint = Color(0xFF0F766E),
          iconBg = Color(0xFFCCFBF1),
          cardBg = cardBg,
          primaryText = primaryText,
          secondaryText = secondaryText,
          onClick = { onNavigate(Screen.AppSettings) },
          testTag = "more_tool_app_settings"
        )
      }

      item {
        ToolActionCard(
          title = "Audit Logs & Security",
          subtitle = "Detailed security logs, login history, data modifications & audit trail",
          badge = "Security",
          icon = Icons.Default.History,
          iconTint = Color(0xFF7C2D12),
          iconBg = Color(0xFFFFEDD5),
          cardBg = cardBg,
          primaryText = primaryText,
          secondaryText = secondaryText,
          onClick = { onNavigate(Screen.AuditLogs) },
          testTag = "more_tool_audit_logs"
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun ToolActionCard(
  title: String,
  subtitle: String,
  badge: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  cardBg: Color,
  primaryText: Color,
  secondaryText: Color,
  onClick: () -> Unit,
  testTag: String
) {
  val isDark = LocalAppTheme.current.isDark

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) iconTint.copy(alpha = 0.2f) else iconBg,
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) iconTint else iconTint,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText
          )
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isDark) iconTint.copy(alpha = 0.25f) else iconBg
          ) {
            Text(
              text = badge,
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold,
              color = iconTint,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = secondaryText,
          lineHeight = 15.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "Open",
        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
