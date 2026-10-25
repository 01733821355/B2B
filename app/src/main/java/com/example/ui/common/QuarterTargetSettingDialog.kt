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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
  onAddCriteria: ((String) -> Unit)? = null,
  onDeleteCriteria: ((String) -> Unit)? = null,
  initialCustomTargets: Map<String, Int> = emptyMap(),
  onDismiss: () -> Unit,
  onSave: (quarter: String, year: Int, cc: Int, corp: Int, b2b: Int) -> Unit,
  onSaveCustomTargets: ((Map<String, Int>) -> Unit)? = null
) {
  var selectedQuarter by remember { mutableStateOf(DateUtils.getCurrentQuarter()) }
  var selectedYear by remember { mutableStateOf(2026) }

  // Load existing targets for selected quarter
  val existingQTargets = currentTarget?.getTargetForQuarter(selectedQuarter) ?: Triple(20, 10, 15)

  var ccInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.first.toString()) }
  var corpInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.second.toString()) }
  var b2bInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.third.toString()) }

  val customTargetInputs = remember(selectedQuarter, initialCustomTargets) {
    mutableStateMapOf<String, String>().apply {
      initialCustomTargets.forEach { (k, v) -> put(k, v.toString()) }
    }
  }

  var showAddCriteriaRow by remember { mutableStateOf(false) }
  var newCriteriaNameInput by remember { mutableStateOf("") }

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
        // Annual Target Summary Banner
        val annCc = currentTarget?.annualCreditCardTarget ?: 80
        val annCorp = currentTarget?.annualCorporateCardTarget ?: 40
        val annB2b = currentTarget?.annualB2bTarget ?: 60
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("2026 বার্ষিক মোট লক্ষ্যমাত্রা (Annual Goal):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("CC: $annCc • Corp: $annCorp • B2B: $annB2b", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2563EB))
          }
        }

        Text("Select Quarter or Annual to Set Target:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EblNavyDark)
        Spacer(modifier = Modifier.height(8.dp))

        // Quarter Selector Pills
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

        Spacer(modifier = Modifier.height(12.dp))

        // Year and Period info
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF1F5F9),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (selectedQuarter == "ALL") "All 4 Quarters • Year 2026 (Annual)" else DateUtils.getQuarterDateLabel(selectedQuarter, selectedYear),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = EblNavyDark
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDCFCE7)
            ) {
              Text(if (selectedQuarter == "ALL") "Annual" else "Quarter", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val periodLabel = if (selectedQuarter == "ALL") "All Year" else selectedQuarter

        // Credit Card Input
        OutlinedTextField(
          value = ccInput,
          onValueChange = { ccInput = it.filter { ch -> ch.isDigit() } },
          label = { Text("Credit Card STC Target ($periodLabel)") },
          placeholder = { Text("e.g. 20") },
          leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("input_quarter_cc_target"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Corporate Card Input
        OutlinedTextField(
          value = corpInput,
          onValueChange = { corpInput = it.filter { ch -> ch.isDigit() } },
          label = { Text("Corporate Card Target ($periodLabel)") },
          placeholder = { Text("e.g. 10") },
          leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("input_quarter_corp_target"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // B2B / Loan Input
        OutlinedTextField(
          value = b2bInput,
          onValueChange = { b2bInput = it.filter { ch -> ch.isDigit() } },
          label = { Text("B2B / Payroll Target ($periodLabel)") },
          placeholder = { Text("e.g. 15") },
          leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("input_quarter_b2b_target"),
          singleLine = true
        )

        // Dynamic Custom Criteria List (Added by Admin)
        val customCriteria = allCriteria.filter { crit ->
          val norm = crit.trim().lowercase()
          norm != "credit card" && norm != "corporate card" && norm != "b2b"
        }

        if (customCriteria.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text("অন্যান্য টার্গেট ক্রাইটেরিয়া (Custom Criteria):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          Spacer(modifier = Modifier.height(6.dp))

          customCriteria.forEach { crit ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = customTargetInputs[crit] ?: "",
                onValueChange = { customTargetInputs[crit] = it.filter { ch -> ch.isDigit() } },
                label = { Text("$crit Target ($periodLabel)") },
                placeholder = { Text("e.g. 10") },
                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("input_custom_target_${crit.replace(" ", "_")}"),
                singleLine = true
              )
              if (onDeleteCriteria != null) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                  onClick = { onDeleteCriteria(crit) },
                  modifier = Modifier.size(40.dp).testTag("btn_delete_criteria_${crit.replace(" ", "_")}")
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Criteria", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }

        // Add New Target Criteria Section for Admin
        Spacer(modifier = Modifier.height(10.dp))
        if (showAddCriteriaRow) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("নতুন টার্গেট ক্রাইটেরিয়া যোগ করুন (Add Target Criteria):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
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
                    if (newCriteriaNameInput.isNotBlank()) {
                      onAddCriteria?.invoke(newCriteriaNameInput.trim())
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

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (selectedQuarter == "ALL")
            "Note: Saves annual goal & distributes across all quarters. Auto-syncs to Google Sheets."
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
          val cc = ccInput.toIntOrNull() ?: 20
          val corp = corpInput.toIntOrNull() ?: 10
          val b2b = b2bInput.toIntOrNull() ?: 15
          onSave(selectedQuarter, selectedYear, cc, corp, b2b)
          val customMap = customTargetInputs.mapNotNull { (k, v) ->
            val num = v.toIntOrNull()
            if (num != null) k to num else null
          }.toMap()
          onSaveCustomTargets?.invoke(customMap)
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
}
