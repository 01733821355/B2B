package com.example.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RmTargetEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.util.DateUtils

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
  onSaveCustomTargets: ((quarter: String, targets: Map<String, Int>) -> Unit)? = null,
  onSaveAllQuarterCustomTargets: ((targets: Map<String, Map<String, Int>>) -> Unit)? = null,
  onSaveAssignedCriteria: ((List<String>) -> Unit)? = null
) {
  var selectedQuarter by remember { mutableStateOf(DateUtils.getCurrentQuarter()) }
  var selectedYear by remember { mutableStateOf(2026) }

  // Which criteria are currently checked/assigned to this RM
  var selectedAssignedCriteria by remember(assignedCriteria, allCriteria) {
    val initialSet = if (assignedCriteria.isNotEmpty()) {
      assignedCriteria.filter { a -> allCriteria.any { it.equals(a, ignoreCase = true) } }.toSet()
    } else {
      allCriteria.toSet()
    }
    mutableStateOf(initialSet)
  }

  // Quarterly isolated inputs state: Quarter -> (Criterion -> TargetNumberString)
  val targetsByQuarterState = remember(allQuarterCustomTargets, currentTarget, initialCustomTargets) {
    val qMap = mutableMapOf<String, MutableMap<String, String>>()
    listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
      val inner = mutableMapOf<String, String>()
      val qT = currentTarget?.getTargetForQuarter(q) ?: Triple(20, 10, 15)
      inner["Credit Card"] = if (qT.first > 0) qT.first.toString() else "20"
      inner["Corporate Card"] = if (qT.second > 0) qT.second.toString() else "10"
      inner["B2B"] = if (qT.third > 0) qT.third.toString() else "15"

      // Load specific custom targets for this quarter
      val qSpecificCustom = allQuarterCustomTargets[q]
      if (qSpecificCustom != null && qSpecificCustom.isNotEmpty()) {
        qSpecificCustom.forEach { (crit, v) ->
          inner[crit] = if (v > 0) v.toString() else "10"
        }
      } else {
        // Fallback to ALL or initial
        val fallbackCustom = allQuarterCustomTargets["ALL"] ?: initialCustomTargets
        fallbackCustom.forEach { (crit, v) ->
          inner[crit] = if (v > 0) v.toString() else "10"
        }
      }

      // Ensure any criterion in allCriteria has an entry
      allCriteria.forEach { crit ->
        if (!inner.containsKey(crit)) {
          inner[crit] = "10"
        }
      }
      qMap[q] = inner
    }
    mutableStateOf(qMap)
  }

  var showAddCriteriaRow by remember { mutableStateOf(false) }
  var newCriteriaNameInput by remember { mutableStateOf("") }
  var criteriaToDelete by remember { mutableStateOf<String?>(null) }

  val quarters = listOf(
    Pair("Q1", "Q1 (Jan-Mar)"),
    Pair("Q2", "Q2 (Apr-Jun)"),
    Pair("Q3", "Q3 (Jul-Sep)"),
    Pair("Q4", "Q4 (Oct-Dec)"),
    Pair("ALL", "All Quarters (বার্ষিক)")
  )

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
          modifier = Modifier.size(34.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EblGold, modifier = Modifier.size(20.dp))
          }
        }
        Column {
          Text("Set Target (লক্ষ্যমাত্রা নির্ধারণ)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("$rmName (RM $rmCode)", fontSize = 11.sp, color = Color.Gray)
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
        // Quarter Selector Pills
        Text("কোয়ার্টার বা বার্ষিক সময়কাল নির্বাচন করুন:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EblNavyDark)
        Spacer(modifier = Modifier.height(6.dp))

        val scrollState = rememberScrollState()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quarters.forEach { (qKey, qLabel) ->
            val isSelected = selectedQuarter.equals(qKey, ignoreCase = true)
            FilterChip(
              selected = isSelected,
              onClick = { selectedQuarter = qKey },
              label = {
                Text(
                  text = qLabel,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1,
                  softWrap = false
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EblNavyPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("chip_quarter_$qKey")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Period Info Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (selectedQuarter == "ALL") "All 4 Quarters • Year 2026 (Annual)" else DateUtils.getQuarterDateLabel(selectedQuarter, selectedYear),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3A8A)
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDCFCE7)
            ) {
              Text(if (selectedQuarter == "ALL") "Annual" else selectedQuarter, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "টার্গেট ক্রাইটেরিয়া ও $selectedQuarter লক্ষ্যমাত্রা:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Text(
          text = "প্রতিটি কোয়ার্টারের (Q1, Q2, Q3, Q4) লক্ষ্যমাত্রা সম্পূর্ণ আলাদাভাবে সেট হবে।",
          fontSize = 10.sp,
          color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val periodLabel = if (selectedQuarter == "ALL") "All Year" else selectedQuarter

        // Display all available criteria
        allCriteria.forEach { crit ->
          val isAssigned = selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
          val norm = crit.trim().lowercase()

          val (icon, iconColor, containerBg, borderClr) = when {
            norm.contains("credit") -> Quad(Icons.Default.CreditCard, Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
            norm.contains("corp") -> Quad(Icons.Default.Business, Color(0xFF1D4ED8), Color(0xFFEFF6FF), Color(0xFFBFDBFE))
            norm.contains("b2b") -> Quad(Icons.Default.Handshake, Color(0xFF7E22CE), Color(0xFFFAF5FF), Color(0xFFE9D5FF))
            else -> Quad(Icons.Default.Stars, Color(0xFFD97706), Color(0xFFFFFBEB), Color(0xFFFDE68A))
          }

          val currentVal = targetsByQuarterState.value[selectedQuarter]?.get(crit) ?: ""

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isAssigned) containerBg else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAssigned) borderClr else Color(0xFFE2E8F0)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
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
                      val updatedSet = selectedAssignedCriteria.toMutableSet()
                      if (checked) {
                        updatedSet.add(crit)
                        listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
                          val qMap = targetsByQuarterState.value.toMutableMap()
                          val inner = (qMap[q] ?: mutableMapOf()).toMutableMap()
                          if (inner[crit].isNullOrBlank()) {
                            inner[crit] = when {
                              crit.equals("Credit Card", ignoreCase = true) -> "20"
                              crit.equals("Corporate Card", ignoreCase = true) -> "10"
                              crit.equals("B2B", ignoreCase = true) -> "15"
                              else -> "0"
                            }
                          }
                          qMap[q] = inner
                          targetsByQuarterState.value = qMap
                        }
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
                  Text(
                    text = crit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isAssigned) EblNavyDark else Color.Gray
                  )
                }

                // Universal Delete button for this criterion
                if (onDeleteCriteria != null) {
                  IconButton(
                    onClick = { criteriaToDelete = crit },
                    modifier = Modifier.size(32.dp).testTag("btn_delete_criteria_${crit.replace(" ", "_")}")
                  ) {
                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "সার্বজনীনভাবে মুছুন",
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }

              if (isAssigned) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = currentVal,
                  onValueChange = { newVal ->
                    val digits = newVal.filter { ch -> ch.isDigit() }
                    val qMap = targetsByQuarterState.value.toMutableMap()
                    val inner = (qMap[selectedQuarter] ?: mutableMapOf()).toMutableMap()
                    inner[crit] = digits
                    qMap[selectedQuarter] = inner
                    targetsByQuarterState.value = qMap
                  },
                  label = { Text("$crit Target ($periodLabel)", fontSize = 11.sp) },
                  placeholder = { Text("লক্ষ্যমাত্রা দিন (e.g. 10)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_target_${crit.replace(" ", "_")}"),
                  singleLine = true
                )
              } else {
                Text(
                  text = "❌ এই RM-কে টার্গেট দেওয়া হয়নি (অ্যাপে প্রদর্শিত হবে না)",
                  fontSize = 10.sp,
                  color = Color.Gray,
                  modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                )
              }
            }
          }
        }

        // Add New Criteria Section
        Spacer(modifier = Modifier.height(10.dp))
        if (showAddCriteriaRow) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("নতুন সার্বজনীন টার্গেট ক্রাইটেরিয়া যোগ করুন (Add Universal Criteria):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = newCriteriaNameInput,
                  onValueChange = { newCriteriaNameInput = it },
                  placeholder = { Text("e.g. CASA, SME, Auto Loan", fontSize = 11.sp) },
                  modifier = Modifier.weight(1f).testTag("input_new_criteria_name"),
                  singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    val clean = newCriteriaNameInput.trim()
                    if (clean.isNotBlank()) {
                      onAddCriteria?.invoke(clean)
                      val updatedSet = selectedAssignedCriteria.toMutableSet()
                      updatedSet.add(clean)
                      selectedAssignedCriteria = updatedSet

                      // Populate new criterion in each quarter state
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
                  Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else if (onAddCriteria != null) {
          OutlinedButton(
            onClick = { showAddCriteriaRow = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("btn_show_add_criteria")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EblNavyPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ নতুন টার্গেট ক্রাইটেরিয়া যোগ করুন (Add Criteria)", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = if (selectedQuarter == "ALL")
            "Note: Saves annual goal & distributes across quarters. Auto-syncs to Google Sheets."
          else
            "Note: Target saves specifically for $selectedQuarter $selectedYear and syncs live to Google Sheets.",
          fontSize = 10.sp,
          color = Color.Gray
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val isCcAssigned = selectedAssignedCriteria.any { it.equals("Credit Card", ignoreCase = true) }
          val isCorpAssigned = selectedAssignedCriteria.any { it.equals("Corporate Card", ignoreCase = true) }
          val isB2bAssigned = selectedAssignedCriteria.any { it.equals("B2B", ignoreCase = true) }

          val currentInner = targetsByQuarterState.value[selectedQuarter] ?: emptyMap()
          val cc = if (isCcAssigned) (currentInner["Credit Card"]?.toIntOrNull() ?: 20) else 0
          val corp = if (isCorpAssigned) (currentInner["Corporate Card"]?.toIntOrNull() ?: 10) else 0
          val b2b = if (isB2bAssigned) (currentInner["B2B"]?.toIntOrNull() ?: 15) else 0

          onSave(selectedQuarter, selectedYear, cc, corp, b2b)

          // Build all-quarter custom targets map
          val allQuartersResult = mutableMapOf<String, Map<String, Int>>()

          listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
            val innerMap = mutableMapOf<String, Int>()
            val qInputs = targetsByQuarterState.value[q] ?: emptyMap()
            customCriteriaList.forEach { crit ->
              if (selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }) {
                val rawVal = qInputs[crit]?.trim() ?: ""
                val num = rawVal.toIntOrNull() ?: 0
                innerMap[crit] = num
              }
            }
            allQuartersResult[q] = innerMap
          }

          if (onSaveAllQuarterCustomTargets != null) {
            onSaveAllQuarterCustomTargets.invoke(allQuartersResult)
          } else {
            onSaveCustomTargets?.invoke(selectedQuarter, allQuartersResult[selectedQuarter] ?: emptyMap())
          }

          // Save assigned criteria list
          val assignedList = allCriteria.filter { crit ->
            selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
          }
          onSaveAssignedCriteria?.invoke(assignedList)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("btn_save_quarter_target")
      ) {
        Text(if (selectedQuarter == "ALL") "Save Annual Target" else "Save $selectedQuarter Target", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )

  // Universal Criteria Delete Confirmation Dialog
  if (criteriaToDelete != null) {
    val critName = criteriaToDelete!!
    AlertDialog(
      onDismissRequest = { criteriaToDelete = null },
      title = { Text("সার্বজনীনভাবে ক্রাইটেরিয়া মুছে ফেলতে চান?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
      text = {
        Text("'$critName' ক্রাইটেরিয়া মুছে ফেললে এটি সার্বজনীনভাবে (Universal) সমস্ত RM ও অ্যাডমিনের তালিকা থেকে সম্পূর্ণভাবে মুছে যাবে। আপনি কি নিশ্চিত?")
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
          Text("মুছে ফেলুন (Delete)", fontWeight = FontWeight.Bold, color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { criteriaToDelete = null }) {
          Text("বাতিল")
        }
      }
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
