package com.example.ui.screens.rm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.RmTargetEntity
import com.example.data.model.UserEntity
import com.example.data.repository.EblRepository
import com.example.ui.common.AnimatedCountText
import com.example.ui.common.interactiveBounce
import com.example.ui.common.rememberBounceInteraction
import com.example.ui.common.rememberShimmerBrush
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppTheme
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetVsAchievementScreen(
  user: UserEntity,
  stats: KpiStats,
  target: RmTargetEntity?,
  targetCriteria: List<String> = listOf("Credit Card", "Corporate Card", "B2B"),
  assignedCriteria: List<String> = emptyList(),
  customTargets: Map<String, Int> = emptyMap(),
  allQuarterCustomTargets: Map<String, Map<String, Int>> = emptyMap(),
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val isDark = LocalAppTheme.current.isDark
  var selectedQuarter by remember { mutableStateOf(DateUtils.getCurrentQuarter()) }
  val isAnnual = selectedQuarter.equals("Annual", ignoreCase = true)

  val activeRmCriteria = remember(targetCriteria, assignedCriteria) {
    val assignedClean = assignedCriteria.map { it.trim() }.filter { it.isNotBlank() }
    if (assignedClean.isNotEmpty()) {
      assignedClean.distinctBy { EblRepository.normalizeCriteria(it).lowercase() }
    } else {
      listOf("Credit Card", "Corporate Card", "B2B")
    }
  }

  // Determine quarterly targets for CC, Corp, and B2B
  val ccTarget = when {
    isAnnual -> target?.annualCreditCardTarget ?: 0
    selectedQuarter == "Q1" -> target?.q1CreditCardTarget ?: 0
    selectedQuarter == "Q2" -> target?.q2CreditCardTarget ?: 0
    selectedQuarter == "Q3" -> target?.q3CreditCardTarget ?: 0
    selectedQuarter == "Q4" -> target?.q4CreditCardTarget ?: 0
    else -> target?.creditCardTarget ?: 0
  }
  val corpTarget = when {
    isAnnual -> target?.annualCorporateCardTarget ?: 0
    selectedQuarter == "Q1" -> target?.q1CorporateCardTarget ?: 0
    selectedQuarter == "Q2" -> target?.q2CorporateCardTarget ?: 0
    selectedQuarter == "Q3" -> target?.q3CorporateCardTarget ?: 0
    selectedQuarter == "Q4" -> target?.q4CorporateCardTarget ?: 0
    else -> target?.corporateCardTarget ?: 0
  }
  val b2bTarget = when {
    isAnnual -> target?.annualB2bTarget ?: 0
    selectedQuarter == "Q1" -> target?.q1B2bTarget ?: 0
    selectedQuarter == "Q2" -> target?.q2B2bTarget ?: 0
    selectedQuarter == "Q3" -> target?.q3B2bTarget ?: 0
    selectedQuarter == "Q4" -> target?.q4B2bTarget ?: 0
    else -> target?.b2bTarget ?: 0
  }

  val currentTheme = LocalAppTheme.current
  val haptics = LocalHapticFeedback.current

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              listOf(currentTheme.primary, currentTheme.secondary, currentTheme.primary)
            )
          )
      ) {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "Target vs Achievement",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = "RM: ${user.name} (${user.rmCode})",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
              )
            }
          },
          navigationIcon = {
            val (backInteraction, _) = rememberBounceInteraction(0.88f)
            IconButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBack()
              },
              interactionSource = backInteraction,
              modifier = Modifier.interactiveBounce(pressedScale = 0.88f, interactionSource = backInteraction)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
          )
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
              Brush.horizontalGradient(
                listOf(Color.Transparent, currentTheme.tertiary.copy(alpha = 0.75f), Color.Transparent)
              )
            )
        )
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("target_vs_achievement_screen"),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Heading & Indicator
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = EblNavyPrimary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Quota & Performance",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Text(
                  text = "Achievement = STC (Production Done)",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                  fontWeight = FontWeight.Medium
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
              border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE))
            ) {
              Text(
                text = if (isAnnual) "2026 Annual Target" else "$selectedQuarter 2026 Target",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // Annual Goal Banner
      item {
        val annualCc = target?.annualCreditCardTarget ?: 0
        val annualCorp = target?.annualCorporateCardTarget ?: 0
        val annualB2b = target?.annualB2bTarget ?: 0

        val annualSummaryParts = mutableListOf<String>()
        if (annualCc > 0 && activeRmCriteria.any { it.equals("Credit Card", ignoreCase = true) }) {
          annualSummaryParts.add("CC $annualCc")
        }
        if (annualCorp > 0 && activeRmCriteria.any { it.equals("Corporate Card", ignoreCase = true) }) {
          annualSummaryParts.add("Corp $annualCorp")
        }
        if (annualB2b > 0 && activeRmCriteria.any { it.equals("B2B", ignoreCase = true) }) {
          annualSummaryParts.add("B2B $annualB2b")
        }
        val standardCritSet = setOf("Credit Card", "Corporate Card", "B2B")
        activeRmCriteria.filter { crit ->
          standardCritSet.none { it.equals(crit.trim(), ignoreCase = true) }
        }.distinctBy { EblRepository.normalizeCriteria(it).lowercase() }.forEach { crit ->
          val normCrit = EblRepository.normalizeCriteria(crit)
          val sumQ = ((allQuarterCustomTargets["Q1"]?.get(normCrit) ?: allQuarterCustomTargets["Q1"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                      (allQuarterCustomTargets["Q2"]?.get(normCrit) ?: allQuarterCustomTargets["Q2"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                      (allQuarterCustomTargets["Q3"]?.get(normCrit) ?: allQuarterCustomTargets["Q3"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                      (allQuarterCustomTargets["Q4"]?.get(normCrit) ?: allQuarterCustomTargets["Q4"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0))
          val annualCustom = if (sumQ > 0) sumQ else (allQuarterCustomTargets["ALL"]?.get(normCrit) ?: allQuarterCustomTargets["ALL"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: (customTargets[normCrit] ?: 0) * 4)
          annualSummaryParts.add("$normCrit $annualCustom")
        }
        val annualSummaryText = if (annualSummaryParts.isNotEmpty()) {
          "Total: " + annualSummaryParts.joinToString(" • ")
        } else {
          "No target assigned"
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
            ) {
              Text(
                text = "📅 2026 Annual Goal:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
              )
              Text(
                text = annualSummaryText,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isAnnual) EblNavyPrimary else if (isDark) Color(0xFF0F172A) else Color.White,
              border = BorderStroke(1.dp, if (isAnnual) EblNavyPrimary else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable { selectedQuarter = "Annual" }
            ) {
              Text(
                text = if (isAnnual) "✓ Viewing Annual" else "View Annual",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAnnual) Color.White else if (isDark) Color(0xFF93C5FD) else EblNavyPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // Quarter selection tabs with tactile bounce
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Q1", "Q2", "Q3", "Q4", "Annual").forEach { q ->
            val isSelected = selectedQuarter.equals(q, ignoreCase = true)
            val bgColor by animateColorAsState(
              targetValue = if (isSelected) currentTheme.primary else MaterialTheme.colorScheme.surface,
              animationSpec = tween(200),
              label = "q_chip_bg"
            )
            val scale by animateFloatAsState(
              targetValue = if (isSelected) 1.03f else 1f,
              animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
              label = "q_chip_scale"
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = bgColor,
              border = BorderStroke(
                1.dp,
                if (isSelected) currentTheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
              shadowElevation = if (isSelected) 4.dp else 0.dp,
              modifier = Modifier
                .weight(if (q == "Annual") 1.3f else 1f)
                .graphicsLayer {
                  scaleX = scale
                  scaleY = scale
                }
                .interactiveBounce(pressedScale = 0.93f) { selectedQuarter = q }
                .testTag("tva_chip_quarter_$q")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
              ) {
                Text(
                  text = if (q == "Annual") "Annual (Yr)" else q,
                  fontSize = 11.5.sp,
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      // Dynamic Target Cards for assigned criteria
      if (activeRmCriteria.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
          ) {
            Text(
              text = "No active target assigned for this period.",
              fontSize = 12.sp,
              color = if (isDark) Color(0xFF94A3B8) else Color.Gray,
              modifier = Modifier.padding(16.dp),
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        val chunks = if (activeRmCriteria.size <= 3) listOf(activeRmCriteria) else activeRmCriteria.chunked(2)
        chunks.forEach { chunk ->
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              chunk.forEach { crit ->
                Box(modifier = Modifier.weight(1f)) {
                  when {
                    crit.equals("Credit Card", ignoreCase = true) -> {
                      val ccAchieved = stats.stcCreditCardCount
                      val ccTargetVal = (ccAchieved.toFloat() / ccTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                      val ccProgress by animateFloatAsState(
                        targetValue = ccTargetVal,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "cc_progress"
                      )
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("Credit Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534))
                            }
                            com.example.ui.common.CardNetworkLogoBadge(com.example.ui.common.CardNetwork.VISA, compact = true)
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCountText(targetCount = ccAchieved, color = Color(0xFF16A34A), fontSize = 20.sp, fontFamily = JetBrainsMonoFontFamily)
                            Text(" / $ccTarget", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          LinearProgressIndicator(
                            progress = { ccProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF16A34A),
                            trackColor = if (isDark) Color(0xFF065F46) else Color(0xFFDCFCE7),
                          )
                          Spacer(modifier = Modifier.height(5.dp))
                          Text("${(ccTargetVal * 100).toInt()}% Done", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                      }
                    }
                    crit.equals("Corporate Card", ignoreCase = true) -> {
                      val corpAchieved = stats.stcCorporateCardCount
                      val corpTargetVal = (corpAchieved.toFloat() / corpTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                      val corpProgress by animateFloatAsState(
                        targetValue = corpTargetVal,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "corp_progress"
                      )
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("Corp Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF))
                            }
                            com.example.ui.common.CardNetworkLogoBadge(com.example.ui.common.CardNetwork.MASTERCARD, compact = true)
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCountText(targetCount = corpAchieved, color = Color(0xFF2563EB), fontSize = 20.sp, fontFamily = JetBrainsMonoFontFamily)
                            Text(" / $corpTarget", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          LinearProgressIndicator(
                            progress = { corpProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2563EB),
                            trackColor = if (isDark) Color(0xFF1E40AF) else Color(0xFFDBEAFE),
                          )
                          Spacer(modifier = Modifier.height(5.dp))
                          Text("${(corpTargetVal * 100).toInt()}% Done", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                      }
                    }
                    crit.equals("B2B", ignoreCase = true) -> {
                      val b2bAchieved = stats.stcB2bCount
                      val b2bTargetVal = (b2bAchieved.toFloat() / b2bTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                      val b2bProgress by animateFloatAsState(
                        targetValue = b2bTargetVal,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "b2b_progress"
                      )
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF581C87).copy(alpha = 0.35f) else Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF9333EA) else Color(0xFFE9D5FF)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(16.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("B2B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6B21A8))
                            }
                            com.example.ui.common.CardNetworkLogoBadge(com.example.ui.common.CardNetwork.UNIONPAY, compact = true)
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCountText(targetCount = b2bAchieved, color = Color(0xFF9333EA), fontSize = 20.sp, fontFamily = JetBrainsMonoFontFamily)
                            Text(" / $b2bTarget", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          LinearProgressIndicator(
                            progress = { b2bProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF9333EA),
                            trackColor = if (isDark) Color(0xFF6B21A8) else Color(0xFFF3E8FF),
                          )
                          Spacer(modifier = Modifier.height(5.dp))
                          Text("${(b2bTargetVal * 100).toInt()}% Done", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                        }
                      }
                    }
                    else -> {
                      val normCrit = EblRepository.normalizeCriteria(crit)
                      val targetVal = when {
                        isAnnual -> {
                          val sumQ = ((allQuarterCustomTargets["Q1"]?.get(normCrit) ?: allQuarterCustomTargets["Q1"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                                      (allQuarterCustomTargets["Q2"]?.get(normCrit) ?: allQuarterCustomTargets["Q2"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                                      (allQuarterCustomTargets["Q3"]?.get(normCrit) ?: allQuarterCustomTargets["Q3"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0) +
                                      (allQuarterCustomTargets["Q4"]?.get(normCrit) ?: allQuarterCustomTargets["Q4"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: 0))
                          if (sumQ > 0) sumQ else (allQuarterCustomTargets["ALL"]?.get(normCrit) ?: allQuarterCustomTargets["ALL"]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value ?: (customTargets[normCrit] ?: 0) * 4)
                        }
                        else -> {
                          allQuarterCustomTargets[selectedQuarter]?.get(normCrit)
                            ?: allQuarterCustomTargets[selectedQuarter]?.entries?.firstOrNull { it.key.equals(normCrit, true) }?.value
                            ?: customTargets[normCrit] ?: 0
                        }
                      }
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFD97706) else Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(normCrit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E))
                          }
                          Spacer(modifier = Modifier.height(6.dp))
                          Row(verticalAlignment = Alignment.Bottom) {
                            Text("Target: ", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Color.Gray)
                            Text("$targetVal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
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

      // STC Primary Achievement KPI Banner with gradient + shimmer
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          border = BorderStroke(1.dp, currentTheme.tertiary.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.linearGradient(
                  listOf(Color(0xFF064E3B), Color(0xFF0F766E))
                )
              )
              .background(rememberShimmerBrush(Color.White.copy(alpha = 0.1f)))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(24.dp),
                  color = Color.White.copy(alpha = 0.2f),
                  modifier = Modifier.size(46.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Star,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "STC (Achievement / Done)",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0xFFD97706)
                    ) {
                      Text(
                        text = "TARGET KPI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "Production Done • Official Goal Metric",
                    fontSize = 11.sp,
                    color = Color(0xFF6EE7B7)
                  )
                }
              }
              AnimatedCountText(
                targetCount = stats.stc,
                color = Color.White,
                fontSize = 30.sp,
                fontFamily = JetBrainsMonoFontFamily
              )
            }
          }
        }
      }
    }
  }
}
