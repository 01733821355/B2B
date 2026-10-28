package com.example.ui.screens.rm

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.CustomerFileEntity
import com.example.data.model.RmTargetEntity
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.EblPullRefreshBox
import com.example.ui.common.FloatableRmNavButton
import com.example.ui.common.KpiGridSection
import com.example.ui.common.StatusDistributionChart
import com.example.ui.common.TimeFilterBar
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RmDashboardScreen(
  user: UserEntity,
  stats: KpiStats,
  selectedTimeFilter: DateUtils.TimeFilter,
  recentFiles: List<CustomerFileEntity>,
  onTimeFilterChange: (DateUtils.TimeFilter) -> Unit,
  onAddNewFile: () -> Unit,
  onViewAllFiles: () -> Unit,
  onViewPendingDocs: () -> Unit = {},
  onFileClick: (CustomerFileEntity) -> Unit,
  onDownloadReport: () -> Unit,
  target: RmTargetEntity? = null,
  targetCriteria: List<String> = listOf("Credit Card", "Corporate Card", "B2B"),
  assignedCriteria: List<String> = emptyList(),
  customTargets: Map<String, Int> = emptyMap(),
  allQuarterCustomTargets: Map<String, Map<String, Int>> = emptyMap(),
  unreadSmsCount: Int = 0,
  onOpenSmsInbox: () -> Unit = {},
  onOpenDbrChecklist: () -> Unit = {},
  onOpenCommunication: () -> Unit = {},
  onOpenImportantDocuments: () -> Unit = {},
  onOpenRoutePlanner: () -> Unit = {},
  onUpdateLocation: (Double, Double, String) -> Unit = { _, _, _ -> },
  isBiometricEnabled: Boolean = false,
  onToggleBiometric: ((Boolean) -> Unit)? = null,
  onRefresh: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isRefreshing by remember { mutableStateOf(false) }

  // Auto-detect and sync RM live location in background
  LaunchedEffect(Unit) {
    if (com.example.util.LocationHelper.hasLocationPermission(context)) {
      try {
        val loc = com.example.util.LocationHelper.getCurrentLocation(context)
        onUpdateLocation(loc.latitude, loc.longitude, loc.address)
      } catch (_: Exception) {}
    }
  }

  var selectedKpiQuarter by remember { mutableStateOf(DateUtils.getCurrentQuarter()) }
  val isAnnual = selectedKpiQuarter.equals("Annual", ignoreCase = true)
  val currentQuarterTargets = target?.getTargetForQuarter(selectedKpiQuarter) ?: Triple(20, 10, 15)
  val ccTarget = currentQuarterTargets.first
  val corpTarget = currentQuarterTargets.second
  val b2bTarget = currentQuarterTargets.third

  // Custom targets specific to the selected quarter
  val currentQuarterCustomTargets = remember(selectedKpiQuarter, allQuarterCustomTargets, customTargets) {
    val qKey = selectedKpiQuarter.uppercase()
    when {
      qKey.startsWith("Q") -> {
        val qMap = allQuarterCustomTargets[qKey]
        if (!qMap.isNullOrEmpty()) qMap else customTargets
      }
      qKey in listOf("ANNUAL", "ALL", "BARSHIK") -> {
        val allMap = allQuarterCustomTargets["ALL"]
        if (!allMap.isNullOrEmpty()) {
          allMap
        } else {
          val sumMap = mutableMapOf<String, Int>()
          listOf("Q1", "Q2", "Q3", "Q4").forEach { q ->
            allQuarterCustomTargets[q]?.forEach { (crit, v) ->
              sumMap[crit] = (sumMap[crit] ?: 0) + v
            }
          }
          if (sumMap.isNotEmpty()) sumMap else customTargets
        }
      }
      else -> customTargets
    }
  }

  // Only show criteria assigned specifically to this RM (and not universally deleted)
  val activeRmCriteria = remember(targetCriteria, assignedCriteria, target, customTargets, allQuarterCustomTargets) {
    val assigned = if (assignedCriteria.isNotEmpty()) {
      assignedCriteria
    } else {
      val list = mutableListOf<String>()
      if ((target?.creditCardTarget ?: 0) > 0) list.add("Credit Card")
      if ((target?.corporateCardTarget ?: 0) > 0) list.add("Corporate Card")
      if ((target?.b2bTarget ?: 0) > 0) list.add("B2B")
      customTargets.forEach { (k, v) -> if (v > 0) list.add(k) }
      allQuarterCustomTargets.values.forEach { qm ->
        qm.forEach { (k, v) -> if (v > 0 && !list.contains(k)) list.add(k) }
      }
      if (list.isEmpty()) listOf("Credit Card", "Corporate Card", "B2B") else list
    }
    assigned.filter { a -> targetCriteria.any { it.equals(a, ignoreCase = true) } }
  }
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    EblPullRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        onRefresh?.invoke()
        coroutineScope.launch {
          kotlinx.coroutines.delay(1000)
          isRefreshing = false
        }
      }
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("rm_dashboard_screen")
      ) {
    // Welcome Theme Photo Banner
    item {
      com.example.ui.common.ThemeVisualBanner(
        title = "Welcome, ${user.name}",
        subtitle = "RM Code: ${user.rmCode} • ${user.officeAddress}",
        badgeText = "Relationship Manager • EBL Sales Suite",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
      )
    }

    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val isDark = com.example.ui.theme.LocalAppTheme.current.isDark


        // Quick action buttons with high-contrast color-wise styling
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. New File (Emerald Green theme)
          OutlinedButton(
            onClick = onAddNewFile,
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
              contentColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("rm_dashboard_add_file_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New File", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
          }

          // 2. My Files (Blue theme)
          OutlinedButton(
            onClick = onViewAllFiles,
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
              contentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("rm_dashboard_view_all_btn")
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("My Files", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, softWrap = false)
          }

          // 3. DBR Tool (Amber theme)
          OutlinedButton(
            onClick = onOpenDbrChecklist,
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
              contentColor = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B) else Color(0xFFFDE68A)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("rm_dashboard_dbr_btn")
          ) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("DBR Tool", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
          }

          // 4. Report (Rose theme)
          OutlinedButton(
            onClick = onDownloadReport,
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF881337).copy(alpha = 0.35f) else Color(0xFFFFF1F2),
              contentColor = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFF43F5E) else Color(0xFFFECDD3)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("rm_dashboard_reports_btn")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Report", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, softWrap = false)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Field Route Planner Smart Banner Card
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF0F2642) else Color(0xFFF0FDF4),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF1E40AF) else Color(0xFF86EFAC)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenRoutePlanner() }
            .testTag("rm_dashboard_route_planner_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(Color(0xFF16A34A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Navigation,
                  contentDescription = "Route Planner",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Field Route Planner",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF14532D)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF15803D)
                  ) {
                    Text(
                      text = "NEW AI",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                }
                Text(
                  text = "Want to Go • গুগল ম্যাপ মাল্টি-স্টপ রুট ও অটো আপডেট",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFF94A3B8) else Color(0xFF166534)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF16A34A)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "প্ল্যান করুন",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                  Icons.Default.ArrowForward,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(12.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

          // Biometric Fingerprint Login Switch for RM
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
                tint = if (isBiometricEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Fingerprint Login (ফিঙ্গারপ্রিন্ট লগইন)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = if (isBiometricEnabled) "Enabled (লগইনে ফিঙ্গারপ্রিন্ট সক্রিয়)" else "Disabled (পাসওয়ার্ড লগইন প্রয়োজন)",
                  fontSize = 9.sp,
                  color = if (isBiometricEnabled) Color(0xFF34D399) else Color.LightGray
                )
              }
            }
            androidx.compose.material3.Switch(
              checked = isBiometricEnabled,
              onCheckedChange = { onToggleBiometric?.invoke(it) },
              modifier = Modifier.testTag("switch_rm_biometric")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Important Documents Repository Quick Banner
          Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2642)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenImportantDocuments() }
              .testTag("rm_important_docs_quick_card")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF0284C7),
                  modifier = Modifier.size(30.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Description,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = "Important Documents (জরুরি ডকুমেন্টস)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "View bank policies, forms & product circulars",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1E3A8A)
              ) {
                Text(
                  text = "View",
                  color = Color(0xFF67E8F9),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

    // Unread SMS Alert Card from Admin/Mentor
    if (unreadSmsCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenSmsInbox() }
            .testTag("rm_unread_sms_alert_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Text(
                  text = "SMS Alert from Admin/Mentor ($unreadSmsCount Unread)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = "Your customer file or profile was updated/deleted. Tap to view details.",
                  fontSize = 11.sp,
                  color = Color(0xFF7F1D1D)
                )
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFDC2626))
          }
        }
      }
    }

    // Time filter pills
    item {
      TimeFilterBar(
        selectedFilter = selectedTimeFilter,
        onFilterSelected = onTimeFilterChange
      )
    }

    // TARGET VS ACHIEVEMENT SECTION (Admin-Assigned Monthly Quotas)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = "Target vs Achievement",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark,
                  maxLines = 1,
                  softWrap = false
                )
                Text(
                  text = "Achievement = STC (Production Done)",
                  fontSize = 10.sp,
                  color = Color(0xFF0F766E),
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFEFF6FF),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
              Text(
                text = if (isAnnual) "2026 বার্ষিক লক্ষ্যমাত্রা (Annual)" else "$selectedKpiQuarter 2026 Target",
                fontSize = 11.sp,
                color = Color(0xFF1D4ED8),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Annual Target Summary Banner
          val annualCc = target?.annualCreditCardTarget ?: (20 * 4)
          val annualCorp = target?.annualCorporateCardTarget ?: (10 * 4)
          val annualB2b = target?.annualB2bTarget ?: (15 * 4)

          val annualSummaryParts = mutableListOf<String>()
          if (activeRmCriteria.any { it.equals("Credit Card", ignoreCase = true) }) {
            annualSummaryParts.add("CC $annualCc")
          }
          if (activeRmCriteria.any { it.equals("Corporate Card", ignoreCase = true) }) {
            annualSummaryParts.add("Corp $annualCorp")
          }
          if (activeRmCriteria.any { it.equals("B2B", ignoreCase = true) }) {
            annualSummaryParts.add("B2B $annualB2b")
          }
          val standardCritSet = setOf("Credit Card", "Corporate Card", "B2B")
          activeRmCriteria.filter { crit ->
            standardCritSet.none { it.equals(crit.trim(), ignoreCase = true) }
          }.forEach { crit ->
            val sumQ = ((allQuarterCustomTargets["Q1"]?.get(crit) ?: allQuarterCustomTargets["Q1"]?.entries?.firstOrNull { it.key.equals(crit, true) }?.value ?: 0) +
                        (allQuarterCustomTargets["Q2"]?.get(crit) ?: allQuarterCustomTargets["Q2"]?.entries?.firstOrNull { it.key.equals(crit, true) }?.value ?: 0) +
                        (allQuarterCustomTargets["Q3"]?.get(crit) ?: allQuarterCustomTargets["Q3"]?.entries?.firstOrNull { it.key.equals(crit, true) }?.value ?: 0) +
                        (allQuarterCustomTargets["Q4"]?.get(crit) ?: allQuarterCustomTargets["Q4"]?.entries?.firstOrNull { it.key.equals(crit, true) }?.value ?: 0))
            val annualCustom = if (sumQ > 0) sumQ else (allQuarterCustomTargets["ALL"]?.get(crit) ?: allQuarterCustomTargets["ALL"]?.entries?.firstOrNull { it.key.equals(crit, true) }?.value ?: (customTargets[crit] ?: 0) * 4)
            annualSummaryParts.add("$crit $annualCustom")
          }
          val annualSummaryText = if (annualSummaryParts.isNotEmpty()) {
            "মোট: " + annualSummaryParts.joinToString(" • ")
          } else {
            "কোনো নির্ধারিত লক্ষ্যমাত্রা নেই"
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "📅 2026 বার্ষিক লক্ষ্যমাত্রা (Annual Goal):",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Text(
                  text = annualSummaryText,
                  fontSize = 10.sp,
                  color = Color(0xFF475569)
                )
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isAnnual) EblNavyPrimary else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnnual) EblNavyPrimary else Color(0xFFCBD5E1)),
                modifier = Modifier.clickable { selectedKpiQuarter = "Annual" }
              ) {
                Text(
                  text = if (isAnnual) "✓ Viewing Annual" else "View Annual",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isAnnual) Color.White else EblNavyPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Quarter selection bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("Q1", "Q2", "Q3", "Q4", "Annual").forEach { q ->
              val isSelected = selectedKpiQuarter.equals(q, ignoreCase = true)
              androidx.compose.material3.FilterChip(
                selected = isSelected,
                onClick = { selectedKpiQuarter = q },
                label = {
                  Text(
                    text = if (q == "Annual") "বার্ষিক (Yr)" else q,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EblNavyPrimary,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.weight(if (q == "Annual") 1.3f else 1f).testTag("kpi_chip_quarter_$q")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Dynamic Target Cards for ONLY this RM's assigned criteria
          if (activeRmCriteria.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFF8FAFC),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
              Text(
                text = "এই কোয়ার্টারের জন্য কোনো লক্ষ্যমাত্রা সক্রিয় নেই।",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          } else {
            val chunks = if (activeRmCriteria.size <= 3) listOf(activeRmCriteria) else activeRmCriteria.chunked(2)
            chunks.forEachIndexed { chunkIdx, chunk ->
              if (chunkIdx > 0) Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                chunk.forEach { crit ->
                  val norm = crit.trim().lowercase()
                  Box(modifier = Modifier.weight(1f)) {
                    when {
                      crit.equals("Credit Card", ignoreCase = true) -> {
                        val ccAchieved = stats.stcCreditCardCount
                        val ccProgress = (ccAchieved.toFloat() / ccTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = Color(0xFFF0FDF4),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("Credit Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                              Text("$ccAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                              Text(" / $ccTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                              progress = { ccProgress },
                              modifier = Modifier.fillMaxWidth().height(6.dp),
                              color = Color(0xFF16A34A),
                              trackColor = Color(0xFFDCFCE7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                              text = "${(ccProgress * 100).toInt()}% Done",
                              fontSize = 10.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = Color(0xFF15803D),
                              maxLines = 1,
                              softWrap = false
                            )
                          }
                        }
                      }
                      crit.equals("Corporate Card", ignoreCase = true) -> {
                        val corpAchieved = stats.stcCorporateCardCount
                        val corpProgress = (corpAchieved.toFloat() / corpTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = Color(0xFFEFF6FF),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("Corp Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                              Text("$corpAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                              Text(" / $corpTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                              progress = { corpProgress },
                              modifier = Modifier.fillMaxWidth().height(6.dp),
                              color = Color(0xFF2563EB),
                              trackColor = Color(0xFFDBEAFE)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                              text = "${(corpProgress * 100).toInt()}% Done",
                              fontSize = 10.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = Color(0xFF1D4ED8),
                              maxLines = 1,
                              softWrap = false
                            )
                          }
                        }
                      }
                      crit.equals("B2B", ignoreCase = true) -> {
                        val b2bAchieved = stats.stcB2bCount
                        val b2bProgress = (b2bAchieved.toFloat() / b2bTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = Color(0xFFFAF5FF),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(14.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("B2B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                              Text("$b2bAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7E22CE))
                              Text(" / $b2bTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                              progress = { b2bProgress },
                              modifier = Modifier.fillMaxWidth().height(6.dp),
                              color = Color(0xFF9333EA),
                              trackColor = Color(0xFFF3E8FF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                              text = "${(b2bProgress * 100).toInt()}% Done",
                              fontSize = 10.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = Color(0xFF7E22CE),
                              maxLines = 1,
                              softWrap = false
                            )
                          }
                        }
                      }
                      else -> {
                        val targetVal = currentQuarterCustomTargets[crit]
                          ?: currentQuarterCustomTargets.entries.firstOrNull { it.key.equals(crit.trim(), ignoreCase = true) }?.value
                          ?: 0
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = Color(0xFFFFFBEB),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text(crit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), maxLines = 1, softWrap = false)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                              Text("Target:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 1.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("$targetVal", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // KPI Metrics Section
    item {
      KpiGridSection(
        stats = stats,
        onKpiClick = { onViewAllFiles() }
      )
    }

    // Status Distribution Chart
    item {
      StatusDistributionChart(stats = stats)
    }

    // Pending Documents Backlog Alert Card (If any files have missing docs)
    if (stats.pendingDocumentsCount > 0) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onViewPendingDocs() }
            .testTag("rm_pending_docs_backlog_card"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
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
                  .size(38.dp)
                  .background(Color(0xFFDC2626), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Pending Documents Backlog",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = "${stats.pendingDocumentsCount} file(s) require missing documents",
                  fontSize = 11.sp,
                  color = Color(0xFFB91C1C)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFDC2626)
            ) {
              Text(
                text = "View Backlog →",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
              )
            }
          }
        }
      }
    }

    // Recent Files Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "My Recent Customer Files",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "View All (${recentFiles.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = EblNavyPrimary,
          modifier = Modifier.clickable { onViewAllFiles() }
        )
      }
    }

    // List of recent customer files
    if (recentFiles.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No customer files found for this time period.", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = onAddNewFile,
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
            ) {
              Text("Create First Customer File")
            }
          }
        }
      }
    } else {
      items(recentFiles.take(5)) { file ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onFileClick(file) }
            .testTag("recent_file_${file.fileId}"),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = file.customerName,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                ApplicationStatusBadge(file.applicationStatus)
                Spacer(modifier = Modifier.width(4.dp))
                ActiveStatusBadge(file.activeStatus)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${file.fileId} • ${file.companyName} • ${file.productType}${if (file.ccNumber.isNotBlank()) " • CC: ${file.ccNumber}" else ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Created: ${DateUtils.formatDateTime(file.createdAt)}",
                fontSize = 10.sp,
                color = Color.Gray
              )
              Text(
                text = "Mobile: ${file.mobile} • Updated: ${DateUtils.formatDateTime(file.updatedAt)}",
                fontSize = 11.sp,
                color = EblNavyPrimary,
                fontWeight = FontWeight.Medium
              )
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "View Details",
              tint = Color.Gray
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(36.dp))
    }
  }
}

  // Floatable & Draggable Quick Nav Button for RM
  FloatableRmNavButton(
    onNavigateNewFile = onAddNewFile,
    onNavigateMyFiles = onViewAllFiles,
    onNavigatePendingDocs = onViewPendingDocs,
    onNavigateDashboard = { /* Already on dashboard */ },
    onNavigateCommunication = onOpenCommunication,
    onNavigateDbrChecklist = onOpenDbrChecklist,
    onNavigateImportantDocuments = onOpenImportantDocuments,
    onNavigateRoutePlanner = onOpenRoutePlanner
  )
}
}
