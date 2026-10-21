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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
  onDismiss: () -> Unit,
  onSave: (quarter: String, year: Int, cc: Int, corp: Int, b2b: Int) -> Unit
) {
  var selectedQuarter by remember { mutableStateOf(DateUtils.getCurrentQuarter()) }
  var selectedYear by remember { mutableStateOf(2026) }

  // Load existing targets for selected quarter
  val existingQTargets = currentTarget?.getTargetForQuarter(selectedQuarter) ?: Triple(20, 10, 15)

  var ccInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.first.toString()) }
  var corpInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.second.toString()) }
  var b2bInput by remember(selectedQuarter) { mutableStateOf(existingQTargets.third.toString()) }

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
          Text("Set KPI Targets (কোয়ার্টার লক্ষ্যমাত্রা)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("$rmName (RM $rmCode)", fontSize = 11.sp, color = Color.Gray)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Quarter & Assign Production Goals:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EblNavyDark)
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
                text = if (selectedQuarter == "ALL") "All 4 Quarters • Year 2026" else DateUtils.getQuarterDateLabel(selectedQuarter, selectedYear),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = EblNavyDark
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDCFCE7)
            ) {
              Text("STC Target", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Credit Card Input
        OutlinedTextField(
          value = ccInput,
          onValueChange = { ccInput = it.filter { ch -> ch.isDigit() } },
          label = { Text("Credit Card STC Target ($selectedQuarter)") },
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
          label = { Text("Corporate Card Target ($selectedQuarter)") },
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
          label = { Text("B2B / Payroll Target ($selectedQuarter)") },
          placeholder = { Text("e.g. 15") },
          leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("input_quarter_b2b_target"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Note: Target saves specifically for $selectedQuarter $selectedYear. RM will receive an automated alert.",
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
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("btn_save_quarter_target")
      ) {
        Text("Save $selectedQuarter Target", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
