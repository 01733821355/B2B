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
  val selectedYear = 2026

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
  val targetsByQuarterState = remember(allQuarterCustomTargets, currentTarget, initialCustomTargets, allCriteria) {
    val qMap = mutableMapOf<String, MutableMap<String, String>>()
    listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
      val inner = mutableMapOf<String, String>()
      val qT = currentTarget?.getTargetForQuarter(q) ?: Triple(0, 0, 0)
      inner["Credit Card"] = qT.first.toString()
      inner["Corporate Card"] = qT.second.toString()
      inner["B2B"] = qT.third.toString()

      // Load specific custom targets for this quarter
      val qSpecificCustom = allQuarterCustomTargets[q]
      if (qSpecificCustom != null && qSpecificCustom.isNotEmpty()) {
        qSpecificCustom.forEach { (crit, v) ->
          inner[crit] = v.toString()
        }
      } else {
        val fallbackCustom = allQuarterCustomTargets["ALL"] ?: initialCustomTargets
        fallbackCustom.forEach { (crit, v) ->
          inner[crit] = v.toString()
        }
      }

      // Ensure every criterion in allCriteria has an entry (defaulting to 0)
      allCriteria.forEach { crit ->
        if (!inner.containsKey(crit)) {
          inner[crit] = "0"
        }
      }
      qMap[q] = inner
    }
    mutableStateOf(qMap)
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
          Text("Target Setup (Q1, Q2, Q3, Q4 & Annual)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text("$rmName (RM $rmCode) • Year $selectedYear", fontSize = 11.sp, color = Color.Gray)
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
          color = Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "প্রতিটি ক্রাইটেরিয়ার জন্য Q1, Q2, Q3, Q4 ও বার্ষিক টার্গেট কলাম:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
              )
              Text(
                text = "যেকোনো ক্রাইটেরিয়াতে টার্গেট 0 হলে ওই RM-এর অ্যাপে সেই ক্রাইটেরিয়া প্রদর্শিত হবে না।",
                fontSize = 10.sp,
                color = Color(0xFF2563EB)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display each criterion with 4 quarterly columns + Annual
        allCriteria.forEach { crit ->
          val isAssigned = selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
          val norm = crit.trim().lowercase()

          val (icon, iconColor, containerBg, borderClr) = when {
            norm.contains("credit") -> Quad(Icons.Default.CreditCard, Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
            norm.contains("corp") -> Quad(Icons.Default.Business, Color(0xFF1D4ED8), Color(0xFFEFF6FF), Color(0xFFBFDBFE))
            norm.contains("b2b") -> Quad(Icons.Default.Handshake, Color(0xFF7E22CE), Color(0xFFFAF5FF), Color(0xFFE9D5FF))
            else -> Quad(Icons.Default.Stars, Color(0xFFD97706), Color(0xFFFFFBEB), Color(0xFFFDE68A))
          }

          val q1Val = targetsByQuarterState.value["Q1"]?.get(crit) ?: "0"
          val q2Val = targetsByQuarterState.value["Q2"]?.get(crit) ?: "0"
          val q3Val = targetsByQuarterState.value["Q3"]?.get(crit) ?: "0"
          val q4Val = targetsByQuarterState.value["Q4"]?.get(crit) ?: "0"
          val allVal = targetsByQuarterState.value["ALL"]?.get(crit) ?: "0"

          val q1Int = q1Val.toIntOrNull() ?: 0
          val q2Int = q2Val.toIntOrNull() ?: 0
          val q3Int = q3Val.toIntOrNull() ?: 0
          val q4Int = q4Val.toIntOrNull() ?: 0
          val autoAnnual = q1Int + q2Int + q3Int + q4Int
          val isAllZero = (q1Int == 0 && q2Int == 0 && q3Int == 0 && q4Int == 0 && (allVal.toIntOrNull() ?: 0) == 0)

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isAssigned) containerBg else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAssigned) borderClr else Color(0xFFE2E8F0)),
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
                      color = if (isAssigned) EblNavyDark else Color.Gray
                    )
                    if (isAssigned && isAllZero) {
                      Text(
                        text = "টার্গেট 0 (এই RM-এর অ্যাপে এটি প্রদর্শিত হবে না)",
                        fontSize = 10.sp,
                        color = Color(0xFFDC2626)
                      )
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
                      contentDescription = "সার্বজনীনভাবে মুছুন",
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }

              if (isAssigned) {
                Spacer(modifier = Modifier.height(6.dp))

                // 4 Quarters + Annual Columns in a neat row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  // Helper function to update quarter and annual
                  fun updateQuarterValue(quarterKey: String, newVal: String) {
                    val digits = newVal.filter { ch -> ch.isDigit() }
                    val qMap = targetsByQuarterState.value.toMutableMap()
                    val inner = (qMap[quarterKey] ?: mutableMapOf()).toMutableMap()
                    inner[crit] = digits
                    qMap[quarterKey] = inner

                    // Recalculate annual automatically
                    val nQ1 = (if (quarterKey == "Q1") digits else qMap["Q1"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                    val nQ2 = (if (quarterKey == "Q2") digits else qMap["Q2"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                    val nQ3 = (if (quarterKey == "Q3") digits else qMap["Q3"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                    val nQ4 = (if (quarterKey == "Q4") digits else qMap["Q4"]?.get(crit) ?: "0").toIntOrNull() ?: 0
                    val innerAll = (qMap["ALL"] ?: mutableMapOf()).toMutableMap()
                    innerAll[crit] = (nQ1 + nQ2 + nQ3 + nQ4).toString()
                    qMap["ALL"] = innerAll

                    targetsByQuarterState.value = qMap
                  }

                  // Q1 Input Column
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                    OutlinedTextField(
                      value = q1Val,
                      onValueChange = { updateQuarterValue("Q1", it) },
                      textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${crit.replace(" ", "_")}_q1")
                    )
                  }

                  // Q2 Input Column
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                    OutlinedTextField(
                      value = q2Val,
                      onValueChange = { updateQuarterValue("Q2", it) },
                      textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${crit.replace(" ", "_")}_q2")
                    )
                  }

                  // Q3 Input Column
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                    OutlinedTextField(
                      value = q3Val,
                      onValueChange = { updateQuarterValue("Q3", it) },
                      textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${crit.replace(" ", "_")}_q3")
                    )
                  }

                  // Q4 Input Column
                  Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Q4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                    OutlinedTextField(
                      value = q4Val,
                      onValueChange = { updateQuarterValue("Q4", it) },
                      textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${crit.replace(" ", "_")}_q4")
                    )
                  }

                  // Annual (Total) Input Column
                  Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Annual", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    OutlinedTextField(
                      value = if (allVal != "0" && allVal.isNotBlank()) allVal else autoAnnual.toString(),
                      onValueChange = { newVal ->
                        val digits = newVal.filter { ch -> ch.isDigit() }
                        val qMap = targetsByQuarterState.value.toMutableMap()
                        val innerAll = (qMap["ALL"] ?: mutableMapOf()).toMutableMap()
                        innerAll[crit] = digits
                        qMap["ALL"] = innerAll
                        targetsByQuarterState.value = qMap
                      },
                      textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF15803D)),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${crit.replace(" ", "_")}_annual")
                    )
                  }
                }
              } else {
                Text(
                  text = "❌ কোনো টার্গেট বরাদ্দ করা হয়নি (টার্গেট 0, এই RM-এর অ্যাপে এটি প্রদর্শিত হবে না)",
                  fontSize = 10.sp,
                  color = Color.Gray,
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
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("নতুন সার্বজনীন টার্গেট ক্রাইটেরিয়া যোগ করুন (Add Universal Criteria):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
              Text("নতুন ক্রাইটেরিয়া যুক্ত করলে স্বয়ংক্রিয়ভাবে Q1, Q2, Q3, Q4 ও Annual কলাম যুক্ত হবে।", fontSize = 10.sp, color = Color(0xFF15803D))
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
                  Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EblNavyPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ নতুন টার্গেট ক্রাইটেরিয়া যোগ করুন (Add Universal Criteria)", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Note: ৪টি কোয়ার্টার (Q1, Q2, Q3, Q4) এবং বার্ষিক টার্গেটের ডেটা Google Sheets-এ লাইভ সিঙ্ক হবে।",
          fontSize = 10.sp,
          color = Color.Gray
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val allQuartersResult = mutableMapOf<String, MutableMap<String, Int>>()
          listOf("Q1", "Q2", "Q3", "Q4", "ALL").forEach { q ->
            val innerMap = mutableMapOf<String, Int>()
            val qInputs = targetsByQuarterState.value[q] ?: emptyMap()
            allCriteria.forEach { crit ->
              val isAssigned = selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
              val rawVal = qInputs[crit]?.trim() ?: "0"
              val num = if (isAssigned) (rawVal.toIntOrNull() ?: 0) else 0
              innerMap[crit] = num
            }
            allQuartersResult[q] = innerMap
          }

          val curQ = DateUtils.getCurrentQuarter()
          val curMap = allQuartersResult[curQ] ?: allQuartersResult["Q1"] ?: emptyMap()
          val cc = curMap["Credit Card"] ?: 0
          val corp = curMap["Corporate Card"] ?: 0
          val b2b = curMap["B2B"] ?: 0

          onSave(curQ, selectedYear, cc, corp, b2b)

          if (onSaveAllQuarterCustomTargets != null) {
            onSaveAllQuarterCustomTargets.invoke(allQuartersResult)
          } else {
            onSaveCustomTargets?.invoke(curQ, curMap)
          }

          // Save assigned criteria list (only checked criteria with target > 0)
          val assignedList = allCriteria.filter { crit ->
            selectedAssignedCriteria.any { it.equals(crit, ignoreCase = true) }
          }
          onSaveAssignedCriteria?.invoke(assignedList)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("btn_save_quarter_target")
      ) {
        Text("Save All Quarter Targets (সংরক্ষণ করুন)", fontWeight = FontWeight.Bold)
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
