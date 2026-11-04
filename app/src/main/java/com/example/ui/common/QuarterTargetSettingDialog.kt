package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RmTargetEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.LocalAppTheme
import com.example.util.DateUtils

// Case-insensitive target value extractor
fun getQuarterTargetValue(qMap: Map<String, String>?, criterion: String): String {
  if (qMap == null) return "0"
  val exact = qMap[criterion]
  if (exact != null && exact.isNotBlank()) return exact
  val match = qMap.entries.firstOrNull { it.key.trim().equals(criterion.trim(), ignoreCase = true) }
  return match?.value?.ifBlank { "0" } ?: "0"
}

@Composable
fun QuarterTargetSettingDialog(
  rmCode: String,
  rmName: String,
  currentTarget: RmTargetEntity?,
  allCriteria: List<String> = listOf("Credit Card", "Corporate Card", "B2B"),
  assignedCriteria: List<String> = allCriteria,
  onAddCriteria: ((String) -> Unit)? = null,
  onDeleteCriteria: ((String) -> Unit)? = null,
  initialCustomTargets: Map<String, Int> = emptyMap(),
  allQuarterCustomTargets: Map<String, Map<String, Int>> = emptyMap(),
  onDismiss: () -> Unit,
  onSave: (quarter: String, year: Int, cc: Int, corp: Int, b2b: Int) -> Unit,
  onSaveAllQuarters: ((
    year: Int,
    q1Cc: Int, q1Corp: Int, q1B2b: Int,
    q2Cc: Int, q2Corp: Int, q2B2b: Int,
    q3Cc: Int, q3Corp: Int, q3B2b: Int,
    q4Cc: Int, q4Corp: Int, q4B2b: Int
  ) -> Unit)? = null,
  onSaveCustomTargets: ((quarter: String, targets: Map<String, Int>) -> Unit)? = null,
  onSaveAllQuarterCustomTargets: ((targets: Map<String, Map<String, Int>>) -> Unit)? = null,
  onSaveAssignedCriteria: ((List<String>) -> Unit)? = null,
  onSaveAllTargetsUnified: ((year: Int, quarterlyMap: Map<String, Map<String, Int>>, assignedList: List<String>) -> Unit)? = null
) {
  val isDark = LocalAppTheme.current.isDark
  val selectedYear = 2026

  val primaryTextColor = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
  val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

  var hasUserModified by remember(rmCode) { mutableStateOf(false) }

  // Track dynamically added criteria in this session so they never vanish
  var localAddedCriteria by remember(rmCode) { mutableStateOf(listOf<String>()) }

  // Which criteria are currently checked/assigned to this RM
  var selectedAssignedCriteria by remember(rmCode) {
    val initialSet = if (assignedCriteria.isNotEmpty()) {
      assignedCriteria.toSet()
    } else {
      allCriteria.toSet()
    }
    mutableStateOf(initialSet)
  }

  // Quarterly isolated inputs state: Quarter -> (Criterion -> TargetNumberString)
  val targetsByQuarterState = remember(rmCode) {
    val qMap = mutableMapOf<String, MutableMap<String, String>>()
    listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
      qMap[q] = mutableMapOf()
    }
    mutableStateOf(qMap)
  }

  // Compute effective all criteria from external list, local additions, assigned list, and current target keys
  val effectiveAllCriteria = remember(allCriteria, localAddedCriteria, targetsByQuarterState.value, selectedAssignedCriteria) {
    val combined = (allCriteria + localAddedCriteria + selectedAssignedCriteria + targetsByQuarterState.value.values.flatMap { it.keys })
      .map { com.example.data.repository.EblRepository.normalizeCriteria(it.trim()) }
      .filter { it.isNotBlank() }
      .distinctBy { it.lowercase() }
    if (combined.isNotEmpty()) combined else listOf("Credit Card", "Corporate Card", "B2B")
  }

  // Synchronize state when data loads from Room / Cloud or changes
  LaunchedEffect(rmCode, currentTarget, allQuarterCustomTargets, allCriteria, assignedCriteria) {
    if (!hasUserModified) {
      if (assignedCriteria.isNotEmpty()) {
        selectedAssignedCriteria = assignedCriteria.toSet()
      } else {
        selectedAssignedCriteria = effectiveAllCriteria.toSet()
      }

      val newMap = mutableMapOf<String, MutableMap<String, String>>()
      listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
        val inner = mutableMapOf<String, String>()
        val qT = currentTarget?.getTargetForQuarter(q) ?: Triple(0, 0, 0)
        inner["Credit Card"] = qT.first.toString()
        inner["Corporate Card"] = qT.second.toString()
        inner["B2B"] = qT.third.toString()

        val qSpecificCustom = allQuarterCustomTargets[q]
        if (!qSpecificCustom.isNullOrEmpty()) {
          qSpecificCustom.forEach { (crit, v) ->
            inner[crit] = v.toString()
          }
        } else if (q == "ALL") {
          val fallbackCustom = allQuarterCustomTargets["ALL"] ?: initialCustomTargets
          fallbackCustom.forEach { (crit, v) ->
            inner[crit] = v.toString()
          }
        }

        effectiveAllCriteria.forEach { crit ->
          val normCrit = crit.trim()
          val hasKey = inner.keys.any { it.trim().equals(normCrit, ignoreCase = true) }
          if (!hasKey) {
            val qVal = allQuarterCustomTargets[q]?.entries?.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value
            val vToUse = if (qVal != null) {
              qVal
            } else if (q == "ALL") {
              allQuarterCustomTargets["ALL"]?.entries?.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value
                ?: initialCustomTargets.entries.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value
                ?: 0
            } else 0
            inner[crit] = vToUse.toString()
          }
        }
        newMap[q] = inner
      }
      targetsByQuarterState.value = newMap
    } else {
      // If user is editing, ensure newly added criteria are safely populated
      val currentMap = targetsByQuarterState.value.toMutableMap()
      effectiveAllCriteria.forEach { crit ->
        val normCrit = crit.trim()
        listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
          val inner = (currentMap[q] ?: mutableMapOf()).toMutableMap()
          val hasKey = inner.keys.any { it.trim().equals(normCrit, ignoreCase = true) }
          if (!hasKey) {
            val loadedVal = allQuarterCustomTargets[q]?.entries?.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value
              ?: (if (q == "ALL") (allQuarterCustomTargets["ALL"]?.entries?.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value ?: initialCustomTargets.entries.firstOrNull { it.key.trim().equals(normCrit, ignoreCase = true) }?.value) else 0)
              ?: 0
            inner[crit] = loadedVal.toString()
            currentMap[q] = inner
          }
        }
      }
      targetsByQuarterState.value = currentMap
    }
  }

  var showAddCriteriaRow by remember { mutableStateOf(false) }
  var newCriteriaNameInput by remember { mutableStateOf("") }
  var criteriaToDelete by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("dialog_quarter_target_setting"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = EblNavyPrimary,
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EblGold, modifier = Modifier.size(20.dp))
          }
        }
        Column {
          Text("Target Setup (Q1, Q2, Q3, Q4 & Annual)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryTextColor)
          Text("$rmName (RM $rmCode) • Year $selectedYear", fontSize = 11.sp, color = secondaryTextColor)
        }
      }
    },
    text = {
      val verticalScrollState = rememberScrollState()
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(verticalScrollState)
      ) {
        // Guidance Box
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB) else Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Target columns for each criterion (Q1, Q2, Q3, Q4 & Annual):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)
              )
              Text(
                text = "Enter quarterly targets. Annual total is computed automatically and synced live.",
                fontSize = 10.sp,
                color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF2563EB)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display each criterion with 4 quarterly columns + Annual
        effectiveAllCriteria.forEach { crit ->
          val isAssigned = selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
          val norm = crit.trim().lowercase()

          val (icon, iconColor, containerBg, borderClr) = when {
            norm.contains("credit") -> Quad(
              Icons.Default.CreditCard,
              if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
              if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
              if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)
            )
            norm.contains("corp") -> Quad(
              Icons.Default.Business,
              if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
              if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
              if (isDark) Color(0xFF2563EB) else Color(0xFFBFDBFE)
            )
            norm.contains("b2b") -> Quad(
              Icons.Default.Handshake,
              if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
              if (isDark) Color(0xFF581C87).copy(alpha = 0.35f) else Color(0xFFFAF5FF),
              if (isDark) Color(0xFF9333EA) else Color(0xFFE9D5FF)
            )
            else -> Quad(
              Icons.Default.Stars,
              if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
              if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
              if (isDark) Color(0xFFD97706) else Color(0xFFFDE68A)
            )
          }

          val q1Val = getQuarterTargetValue(targetsByQuarterState.value["Q1"], crit)
          val q2Val = getQuarterTargetValue(targetsByQuarterState.value["Q2"], crit)
          val q3Val = getQuarterTargetValue(targetsByQuarterState.value["Q3"], crit)
          val q4Val = getQuarterTargetValue(targetsByQuarterState.value["Q4"], crit)
          val allVal = getQuarterTargetValue(targetsByQuarterState.value["ALL"], crit)

          val q1Int = q1Val.toIntOrNull() ?: 0
          val q2Int = q2Val.toIntOrNull() ?: 0
          val q3Int = q3Val.toIntOrNull() ?: 0
          val q4Int = q4Val.toIntOrNull() ?: 0
          val autoAnnual = q1Int + q2Int + q3Int + q4Int
          val isAllZero = (q1Int == 0 && q2Int == 0 && q3Int == 0 && q4Int == 0 && (allVal.toIntOrNull() ?: 0) == 0)

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isAssigned) containerBg else if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isAssigned) borderClr else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 5.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Checkbox(
                    checked = isAssigned,
                    onCheckedChange = { checked ->
                      hasUserModified = true
                      val updatedSet = selectedAssignedCriteria.toMutableSet()
                      if (checked) {
                        updatedSet.add(crit)
                      } else {
                        updatedSet.removeIf { it.equals(crit, ignoreCase = true) }
                      }
                      selectedAssignedCriteria = updatedSet
                    },
                    colors = CheckboxDefaults.colors(checkedColor = EblNavyPrimary),
                    modifier = Modifier.testTag("chk_criteria_${crit.replace(" ", "_")}")
                  )
                  Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text(
                      text = crit,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = if (isAssigned) primaryTextColor else secondaryTextColor
                    )
                    if (isAssigned) {
                      if (isAllZero) {
                        Text(
                          text = "Target 0 (Will not be displayed in RM's app)",
                          fontSize = 10.sp,
                          color = Color(0xFFDC2626)
                        )
                      } else {
                        Text(
                          text = "Active Target • Annual Goal: $autoAnnual (Displayed in RM's app ✓)",
                          fontSize = 10.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                        )
                      }
                    }
                  }
                }

                // Delete custom criterion universally
                val isStandard = setOf("Credit Card", "Corporate Card", "B2B").any { it.equals(crit, ignoreCase = true) }
                if (!isStandard && onDeleteCriteria != null) {
                  IconButton(
                    onClick = { criteriaToDelete = crit },
                    modifier = Modifier
                      .size(28.dp)
                      .testTag("btn_delete_criteria_${crit.replace(" ", "_")}")
                  ) {
                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "Delete Criterion",
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }

              if (isAssigned) {
                Spacer(modifier = Modifier.height(8.dp))

                // Helper function to update quarter and annual
                fun updateQuarterValue(quarterKey: String, newVal: String) {
                  hasUserModified = true
                  val digits = newVal.filter { ch -> ch.isDigit() }
                  val formatted = if (digits.length > 1 && digits.startsWith("0")) {
                    digits.trimStart('0').ifEmpty { "0" }
                  } else {
                    digits
                  }
                  val qMap = targetsByQuarterState.value.toMutableMap()
                  val inner = (qMap[quarterKey] ?: mutableMapOf()).toMutableMap()
                  inner[crit] = formatted
                  qMap[quarterKey] = inner

                  // Recalculate annual automatically
                  val nQ1 = (if (quarterKey == "Q1") formatted else qMap["Q1"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                  val nQ2 = (if (quarterKey == "Q2") formatted else qMap["Q2"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                  val nQ3 = (if (quarterKey == "Q3") formatted else qMap["Q3"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                  val nQ4 = (if (quarterKey == "Q4") formatted else qMap["Q4"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                  val innerAll = (qMap["ALL"] ?: mutableMapOf()).toMutableMap()
                  innerAll[crit] = (nQ1 + nQ2 + nQ3 + nQ4).toString()
                  qMap["ALL"] = innerAll

                  targetsByQuarterState.value = qMap
                }

                // Horizontally scrollable row ensuring Q1, Q2, Q3, Q4 & Annual never get squeezed
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Q1 Input Column
                  Column(modifier = Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                      value = q1Val,
                      onValueChange = { updateQuarterValue("Q1", it) },
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                      ),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        focusedBorderColor = EblNavyPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                      ),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .width(62.dp)
                        .testTag("input_${crit.replace(" ", "_")}_q1")
                    )
                  }

                  // Q2 Input Column
                  Column(modifier = Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                      value = q2Val,
                      onValueChange = { updateQuarterValue("Q2", it) },
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                      ),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        focusedBorderColor = EblNavyPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                      ),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .width(62.dp)
                        .testTag("input_${crit.replace(" ", "_")}_q2")
                    )
                  }

                  // Q3 Input Column
                  Column(modifier = Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                      value = q3Val,
                      onValueChange = { updateQuarterValue("Q3", it) },
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                      ),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        focusedBorderColor = EblNavyPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                      ),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .width(62.dp)
                        .testTag("input_${crit.replace(" ", "_")}_q3")
                    )
                  }

                  // Q4 Input Column - Fully editable and clear touch target
                  Column(modifier = Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                      value = q4Val,
                      onValueChange = { updateQuarterValue("Q4", it) },
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                      ),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        focusedBorderColor = EblNavyPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                      ),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .width(62.dp)
                        .testTag("input_${crit.replace(" ", "_")}_q4")
                    )
                  }

                  // Annual (Total) Input Column - auto-calculated sum
                  Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Annual", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D))
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                      value = autoAnnual.toString(),
                      onValueChange = {},
                      readOnly = true,
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                      ),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                        unfocusedTextColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                        focusedBorderColor = if (isDark) Color(0xFF059669) else Color(0xFF86EFAC),
                        unfocusedBorderColor = if (isDark) Color(0xFF059669) else Color(0xFF86EFAC)
                      ),
                      singleLine = true,
                      modifier = Modifier
                        .width(72.dp)
                        .testTag("input_${crit.replace(" ", "_")}_annual")
                    )
                  }
                }
              } else {
                Text(
                  text = "❌ Not assigned (Target 0, will not be displayed in RM's app)",
                  fontSize = 10.sp,
                  color = secondaryTextColor,
                  modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                )
              }
            }
          }
        }

        // Add New Universal Criteria Row
        Spacer(modifier = Modifier.height(10.dp))
        if (showAddCriteriaRow) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDark) Color(0xFF14532D).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A) else Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Add Universal Target Criteria:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534))
              Text("Adding criteria automatically creates Q1, Q2, Q3, Q4 & Annual columns.", fontSize = 10.sp, color = if (isDark) Color(0xFFBBF7D0) else Color(0xFF15803D))
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = newCriteriaNameInput,
                  onValueChange = { newCriteriaNameInput = it },
                  placeholder = { Text("e.g. CASA, SME, Auto Loan", fontSize = 11.sp) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("input_new_criteria_name"),
                  singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    val clean = newCriteriaNameInput.trim()
                    if (clean.isNotBlank()) {
                      hasUserModified = true
                      localAddedCriteria = (localAddedCriteria + clean).distinct()
                      onAddCriteria?.invoke(clean)
                      val updatedSet = selectedAssignedCriteria.toMutableSet()
                      updatedSet.add(clean)
                      selectedAssignedCriteria = updatedSet

                      // Populate new criterion in each quarter state with 0
                      val qMap = targetsByQuarterState.value.toMutableMap()
                      listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
                        val inner = (qMap[q] ?: mutableMapOf()).toMutableMap()
                        if (!inner.containsKey(clean)) inner[clean] = "0"
                        qMap[q] = inner
                      }
                      targetsByQuarterState.value = qMap

                      newCriteriaNameInput = ""
                      showAddCriteriaRow = false
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("btn_confirm_add_criteria")
                ) {
                  Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        } else if (onAddCriteria != null) {
          OutlinedButton(
            onClick = { showAddCriteriaRow = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
              .testTag("btn_show_add_criteria")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ Add Universal Criteria", fontSize = 11.sp, color = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Note: 4 Quarters (Q1, Q2, Q3, Q4) and Annual targets sync live with Google Sheets.",
          fontSize = 10.sp,
          color = secondaryTextColor
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val allQuartersResult = mutableMapOf<String, MutableMap<String, Int>>()
          listOf("Q1", "Q2", "Q3", "Q4").forEach { q ->
            val innerMap = mutableMapOf<String, Int>()
            val qInputs = targetsByQuarterState.value[q] ?: emptyMap()
            effectiveAllCriteria.forEach { crit ->
              val rawVal = getQuarterTargetValue(qInputs, crit)
              val inputNum = rawVal.toIntOrNull() ?: 0
              val isAssigned = selectedAssignedCriteria.any { it.trim().equals(crit.trim(), ignoreCase = true) } || inputNum > 0
              val num = if (isAssigned) inputNum else 0
              innerMap[crit] = num
            }
            allQuartersResult[q] = innerMap
          }

          // Calculate ALL as exact mathematical sum of Q1 + Q2 + Q3 + Q4
          val annualMap = mutableMapOf<String, Int>()
          effectiveAllCriteria.forEach { crit ->
            val sumAnnual = (allQuartersResult["Q1"]?.get(crit) ?: 0) +
              (allQuartersResult["Q2"]?.get(crit) ?: 0) +
              (allQuartersResult["Q3"]?.get(crit) ?: 0) +
              (allQuartersResult["Q4"]?.get(crit) ?: 0)
            annualMap[crit] = sumAnnual
          }
          allQuartersResult["ALL"] = annualMap

          val curQ = DateUtils.getCurrentQuarter()
          val curMap = allQuartersResult[curQ] ?: allQuartersResult["Q1"] ?: emptyMap()
          val cc = curMap["Credit Card"] ?: 0
          val corp = curMap["Corporate Card"] ?: 0
          val b2b = curMap["B2B"] ?: 0

          val q1Map = allQuartersResult["Q1"] ?: emptyMap()
          val q2Map = allQuartersResult["Q2"] ?: emptyMap()
          val q3Map = allQuartersResult["Q3"] ?: emptyMap()
          val q4Map = allQuartersResult["Q4"] ?: emptyMap()

          // Save assigned criteria list (criteria checked or target > 0)
          val assignedList = effectiveAllCriteria.filter { crit ->
            val isAssigned = selectedAssignedCriteria.any { it.trim().equals(crit.trim(), ignoreCase = true) }
            val hasTarget = (annualMap[crit] ?: 0) > 0 ||
              listOf("Q1", "Q2", "Q3", "Q4").any { q -> (allQuartersResult[q]?.get(crit) ?: 0) > 0 }
            isAssigned || hasTarget
          }

          if (onSaveAllTargetsUnified != null) {
            onSaveAllTargetsUnified.invoke(selectedYear, allQuartersResult, assignedList)
          } else {
            if (onSaveAllQuarters != null) {
              onSaveAllQuarters.invoke(
                selectedYear,
                q1Map["Credit Card"] ?: 0, q1Map["Corporate Card"] ?: 0, q1Map["B2B"] ?: 0,
                q2Map["Credit Card"] ?: 0, q2Map["Corporate Card"] ?: 0, q2Map["B2B"] ?: 0,
                q3Map["Credit Card"] ?: 0, q3Map["Corporate Card"] ?: 0, q3Map["B2B"] ?: 0,
                q4Map["Credit Card"] ?: 0, q4Map["Corporate Card"] ?: 0, q4Map["B2B"] ?: 0
              )
            } else {
              onSave(curQ, selectedYear, cc, corp, b2b)
            }

            if (onSaveAllQuarterCustomTargets != null) {
              onSaveAllQuarterCustomTargets.invoke(allQuartersResult)
            } else {
              onSaveCustomTargets?.invoke(curQ, curMap)
            }

            onSaveAssignedCriteria?.invoke(assignedList)
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("btn_save_quarter_target")
      ) {
        Text("Save All Quarter Targets", fontWeight = FontWeight.Bold, color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel", color = primaryTextColor)
      }
    }
  )

  // Universal Criteria Delete Confirmation Dialog
  if (criteriaToDelete != null) {
    val critName = criteriaToDelete!!
    AlertDialog(
      onDismissRequest = { criteriaToDelete = null },
      title = { Text("Delete Universal Criterion?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryTextColor) },
      text = {
        Text("Deleting '$critName' will permanently remove it universally for all RMs and Admin. Are you sure?", color = secondaryTextColor)
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCriteria?.invoke(critName)
            val updated = selectedAssignedCriteria.toMutableSet()
            updated.removeIf { it.equals(critName, ignoreCase = true) }
            selectedAssignedCriteria = updated
            criteriaToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { criteriaToDelete = null }) {
          Text("Cancel", color = primaryTextColor)
        }
      }
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
