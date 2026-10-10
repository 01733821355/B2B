package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecentActors
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.SmsService
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class ExistingCreditCardEntry(
  val id: Long = System.currentTimeMillis(),
  val bankName: String = "",
  val limit: String = "",
  val outstanding: String = ""
)

data class ExistingLoanEntry(
  val id: Long = System.currentTimeMillis(),
  val bankName: String = "",
  val loanType: String = "Personal Loan",
  val monthlyEmi: String = ""
)

data class ChecklistItem(
  val id: Int,
  val title: String,
  val isChecked: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbrAndChecklistScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigateBack: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val context = LocalContext.current
  val allFiles by viewModel.filteredFiles.collectAsState()

  BackHandler {
    onNavigateBack()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .testTag("dbr_checklist_screen")
  ) {
    // Top Bar Header
    Surface(
      color = EblNavyDark,
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_dbr_back")
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "DBR & Document Checklist Tool",
              color = Color.White,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Eastern Bank PLC Retail Credit Assessment",
              color = EblGold,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    // Tabs: 1. DBR Calculator, 2. Document Checklist Sender
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.White,
      contentColor = EblNavyPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = EblNavyPrimary,
          height = 3.dp
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DBR Calculator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        selectedContentColor = EblNavyPrimary,
        unselectedContentColor = Color.Gray,
        modifier = Modifier.testTag("tab_dbr_calculator")
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Checklist Sender", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        selectedContentColor = EblNavyPrimary,
        unselectedContentColor = Color.Gray,
        modifier = Modifier.testTag("tab_checklist_sender")
      )
    }

    // Tab Contents
    if (selectedTab == 0) {
      DbrCalculatorTab(currentUser = currentUser)
    } else {
      DocumentChecklistSenderTab(
        currentUser = currentUser,
        viewModel = viewModel,
        availableFiles = allFiles
      )
    }
  }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: DBR CALCULATOR
// -------------------------------------------------------------------------------------------------

private fun sanitizeNumberInput(raw: String): String {
  val sb = StringBuilder()
  var hasDot = false
  for (ch in raw) {
    when {
      ch in '0'..'9' -> sb.append(ch)
      ch in '০'..'৯' -> sb.append((ch - '০' + '0'.code).toChar())
      (ch == '.' || ch == '·') && !hasDot -> {
        sb.append('.')
        hasDot = true
      }
    }
  }
  return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbrCalculatorTab(currentUser: UserEntity) {
  val context = LocalContext.current
  val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

  // Customer Profile Inputs
  var customerName by remember { mutableStateOf("") }
  val segmentOptions = listOf(
    "Govt. Employee / Autonomous",
    "MNC / Top Corporate (A-Tier)",
    "Other Salaried / Private Ltd.",
    "Business Person / Self-Employed"
  )
  var selectedSegment by remember { mutableStateOf(segmentOptions[0]) }
  var segmentExpanded by remember { mutableStateOf(false) }

  var salaryText by remember { mutableStateOf("") }
  var proposedLimitText by remember { mutableStateOf("") }

  // Direct Master Input Fields for Credit Limit, Outstanding, and Loan EMI
  var creditCardLimitInput by remember { mutableStateOf("") }
  var creditCardOutstandingInput by remember { mutableStateOf("") }
  var loanEmiInput by remember { mutableStateOf("") }
  var showItemizedBreakdown by remember { mutableStateOf(false) }
  var showItemizedLoans by remember { mutableStateOf(false) }

  // Credit Cards List (for optional detailed card-by-card breakdown)
  val creditCards = remember {
    mutableStateListOf(
      ExistingCreditCardEntry(id = 1, bankName = "EBL", limit = "", outstanding = "")
    )
  }

  // Loans List (for optional detailed loan-by-loan breakdown)
  val loans = remember {
    mutableStateListOf<ExistingLoanEntry>()
  }

  // Calculations
  val salary = sanitizeNumberInput(salaryText).toDoubleOrNull() ?: 0.0
  val proposedLimit = sanitizeNumberInput(proposedLimitText).toDoubleOrNull() ?: 0.0

  val itemizedCardLimit = creditCards.sumOf { sanitizeNumberInput(it.limit).toDoubleOrNull() ?: 0.0 }
  val itemizedCardOutstanding = creditCards.sumOf { sanitizeNumberInput(it.outstanding).toDoubleOrNull() ?: 0.0 }
  val itemizedLoanEmi = loans.sumOf { sanitizeNumberInput(it.monthlyEmi).toDoubleOrNull() ?: 0.0 }

  val totalCardLimit = if (creditCardLimitInput.isNotBlank()) {
    sanitizeNumberInput(creditCardLimitInput).toDoubleOrNull() ?: 0.0
  } else itemizedCardLimit

  val totalCardOutstanding = if (creditCardOutstandingInput.isNotBlank()) {
    sanitizeNumberInput(creditCardOutstandingInput).toDoubleOrNull() ?: 0.0
  } else itemizedCardOutstanding

  val cardLimit3Percent = totalCardLimit * 0.03
  val cardOutstanding5Percent = totalCardOutstanding * 0.05
  val consideredCardObligation = max(cardLimit3Percent, cardOutstanding5Percent)

  val totalLoanEmi = if (loanEmiInput.isNotBlank()) {
    sanitizeNumberInput(loanEmiInput).toDoubleOrNull() ?: 0.0
  } else itemizedLoanEmi

  // Proposed Commitment: Proposed Limit * 3%
  val proposedCommitment = proposedLimit * 0.03

  // Total Debt Obligation
  val totalDebtObligation = consideredCardObligation + totalLoanEmi + proposedCommitment

  // DBR Percentage
  val dbrPercent = if (salary > 0) {
    (totalDebtObligation / salary) * 100.0
  } else 0.0

  // Maximum Allowable DBR determination based on banking policy
  val isGovt = selectedSegment.startsWith("Govt")
  val isMnc = selectedSegment.startsWith("MNC")

  val maxAllowableDbr: Double = when {
    salary < 20000 -> 0.0 // Below minimum threshold
    salary in 20000.0..24999.0 -> {
      if (isGovt) 25.0 else 0.0 // Exactly user instruction: 20000 to 24999 max DBR 25% only govt employees
    }
    salary in 25000.0..49999.0 -> {
      if (isGovt) 40.0 else if (isMnc) 35.0 else 30.0
    }
    salary in 50000.0..99999.0 -> {
      if (isGovt) 50.0 else if (isMnc) 45.0 else 40.0
    }
    else -> 50.0 // 100k+
  }

  // Eligibility Verdict
  val isMinSalaryMet = (salary >= 20000 && isGovt) || (salary >= 25000)
  val isEligible = salary > 0 && isMinSalaryMet && (dbrPercent <= maxAllowableDbr)

  // Max Safe Limit Calculation (Reverse Calculator)
  val maxAllowableObligation = salary * (maxAllowableDbr / 100.0)
  val availableForProposed = maxAllowableObligation - (consideredCardObligation + totalLoanEmi)
  val maxSafeProposedLimit = if (availableForProposed > 0) {
    (availableForProposed / 0.03)
  } else 0.0

  val disposableIncome = max(0.0, salary - totalDebtObligation)

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      // Policy Header Banner
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Formula: DBR = [ { max(Card Limit × 3%, O/S × 5%) + Loan EMI + (Proposed × 3%) } / Salary ] × 100%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E3A8A)
            )
            Text(
              text = "• Salary 20k-24.9k: Max 25% DBR (Govt. employees only) • Max policy cap: 50%",
              fontSize = 10.sp,
              color = Color(0xFF3B82F6)
            )
          }
        }
      }
    }

    // 1. Customer & Salary Details Card
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "1. Customer & Income Details",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name (Optional)") },
            placeholder = { Text("e.g. Md. Tanvir Ahmed") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_dbr_customer_name"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Segment Dropdown
          ExposedDropdownMenuBox(
            expanded = segmentExpanded,
            onExpandedChange = { segmentExpanded = !segmentExpanded }
          ) {
            OutlinedTextField(
              value = selectedSegment,
              onValueChange = {},
              readOnly = true,
              label = { Text("Employment Category") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = segmentExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
            )
            ExposedDropdownMenu(
              expanded = segmentExpanded,
              onDismissRequest = { segmentExpanded = false }
            ) {
              segmentOptions.forEach { seg ->
                DropdownMenuItem(
                  text = { Text(seg, fontSize = 13.sp) },
                  onClick = {
                    selectedSegment = seg
                    segmentExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Salary Input
          OutlinedTextField(
            value = salaryText,
            onValueChange = { salaryText = sanitizeNumberInput(it) },
            label = { Text("Monthly Net Salary (Banks Pay Part) *") },
            placeholder = { Text("e.g. 35000") },
            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF059669)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_dbr_salary"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          if (salary in 20000.0..24999.0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isGovt) "✓ Govt Employee: Allowed up to max 25% DBR" else "⚠ Attention: Salary BDT 20k-24.9k is eligible ONLY for Govt. Employees",
              fontSize = 11.sp,
              color = if (isGovt) Color(0xFF059669) else Color(0xFFDC2626),
              fontWeight = FontWeight.Medium
            )
          } else if (salary > 0 && salary < 20000) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "⚠ Minimum net salary requirement not met (Min BDT 20,000 for Govt, BDT 25,000 for Non-Govt)",
              fontSize = 11.sp,
              color = Color(0xFFDC2626),
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // 2. Existing Credit Cards Card (Direct Input + Optional Multiple Cards Breakdown)
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "2. Existing Credit Cards",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = "Enter total limits & outstandings directly or add card-by-card",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
            OutlinedButton(
              onClick = {
                showItemizedBreakdown = !showItemizedBreakdown
                if (showItemizedBreakdown && creditCards.isEmpty()) {
                  creditCards.add(ExistingCreditCardEntry(id = System.currentTimeMillis()))
                }
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_toggle_itemized_cards")
            ) {
              Icon(
                if (showItemizedBreakdown) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showItemizedBreakdown) "Hide Details" else "+ Itemize Cards", fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // DIRECT MASTER QUICK INPUTS (Type total limit & outstanding immediately!)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = creditCardLimitInput,
              onValueChange = { input ->
                creditCardLimitInput = sanitizeNumberInput(input)
              },
              label = { Text("Total Card Limit (BDT) *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
              placeholder = { Text(if (itemizedCardLimit > 0) nf.format(itemizedCardLimit.roundToInt()) else "e.g. 150000", fontSize = 11.sp) },
              supportingText = {
                if (totalCardLimit > 0) {
                  Text("3% = BDT ${nf.format(cardLimit3Percent.roundToInt())}", fontSize = 10.sp, color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                }
              },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_dbr_total_card_limit"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
            )
            OutlinedTextField(
              value = creditCardOutstandingInput,
              onValueChange = { input ->
                creditCardOutstandingInput = sanitizeNumberInput(input)
              },
              label = { Text("Total O/S (BDT)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
              placeholder = { Text(if (itemizedCardOutstanding > 0) nf.format(itemizedCardOutstanding.roundToInt()) else "e.g. 35000", fontSize = 11.sp) },
              supportingText = {
                if (totalCardOutstanding > 0) {
                  Text("5% = BDT ${nf.format(cardOutstanding5Percent.roundToInt())}", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                }
              },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_dbr_total_card_outstanding"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
            )
          }

          // Optional Card-by-Card Itemized Breakdown
          if (showItemizedBreakdown) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Itemized Card Breakdown (${creditCards.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = EblNavyPrimary
              )
              TextButton(
                onClick = {
                  creditCards.add(ExistingCreditCardEntry(id = System.currentTimeMillis()))
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ Add Another Card", fontSize = 11.sp)
              }
            }

            creditCards.forEachIndexed { index, card ->
              androidx.compose.runtime.key(card.id) {
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Card #${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = EblNavyPrimary
                      )
                      IconButton(
                        onClick = {
                          creditCards.removeAt(index)
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                      value = card.bankName,
                      onValueChange = { newBank ->
                        creditCards[index] = card.copy(bankName = newBank)
                      },
                      label = { Text("Bank Name", fontSize = 10.sp) },
                      placeholder = { Text("e.g. SCB / City / BRAC", fontSize = 10.sp) },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      OutlinedTextField(
                        value = card.limit,
                        onValueChange = { newLimit ->
                          val sanitized = sanitizeNumberInput(newLimit)
                          creditCards[index] = card.copy(limit = sanitized)
                        },
                        label = { Text("Limit (BDT) *", fontSize = 10.sp) },
                        placeholder = { Text("100000", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                      )
                      OutlinedTextField(
                        value = card.outstanding,
                        onValueChange = { newOs ->
                          val sanitized = sanitizeNumberInput(newOs)
                          creditCards[index] = card.copy(outstanding = sanitized)
                        },
                        label = { Text("O/S (BDT)", fontSize = 10.sp) },
                        placeholder = { Text("25000", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Credit Card Sub-totals preview
          Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Total Credit Card Limit:", fontSize = 11.sp, color = Color.Gray)
                Text("BDT ${nf.format(totalCardLimit.roundToInt())} (3% = BDT ${nf.format(cardLimit3Percent.roundToInt())})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Total Current Outstanding:", fontSize = 11.sp, color = Color.Gray)
                Text("BDT ${nf.format(totalCardOutstanding.roundToInt())} (5% = BDT ${nf.format(cardOutstanding5Percent.roundToInt())})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Considered Card Obligation (Higher of 3% or 5%):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
                Text("BDT ${nf.format(consideredCardObligation.roundToInt())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
              }
            }
          }
        }
      }
    }

    // 3. Existing Loans Card (Direct Input + Optional Multiple Loans Breakdown)
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "3. Existing Loans",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = "Personal, Auto, Home or other bank loan EMIs",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
            OutlinedButton(
              onClick = {
                showItemizedLoans = !showItemizedLoans
                if (showItemizedLoans && loans.isEmpty()) {
                  loans.add(ExistingLoanEntry(id = System.currentTimeMillis()))
                }
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_toggle_itemized_loans")
            ) {
              Icon(
                if (showItemizedLoans) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showItemizedLoans) "Hide Details" else "+ Itemize Loans", fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // DIRECT MASTER QUICK INPUT FOR LOAN EMI (Zero friction)
          OutlinedTextField(
            value = loanEmiInput,
            onValueChange = { input ->
              loanEmiInput = sanitizeNumberInput(input)
            },
            label = { Text("Total Existing Loan Monthly EMI (BDT) *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            placeholder = { Text(if (itemizedLoanEmi > 0) nf.format(itemizedLoanEmi.roundToInt()) else "e.g. 15000", fontSize = 11.sp) },
            supportingText = {
              if (totalLoanEmi > 0) {
                Text("Monthly Loan Obligation: BDT ${nf.format(totalLoanEmi.roundToInt())}", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_dbr_total_loan_emi"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          // Optional Loan-by-Loan Itemized Breakdown
          if (showItemizedLoans) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Itemized Loans Breakdown (${loans.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = EblNavyPrimary
              )
              TextButton(
                onClick = {
                  loans.add(ExistingLoanEntry(id = System.currentTimeMillis()))
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ Add Another Loan", fontSize = 11.sp)
              }
            }

            loans.forEachIndexed { index, loan ->
              androidx.compose.runtime.key(loan.id) {
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Loan #${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = EblNavyPrimary
                      )
                      IconButton(
                        onClick = { loans.removeAt(index) },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Loan", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      OutlinedTextField(
                        value = loan.bankName,
                        onValueChange = { newBank ->
                          loans[index] = loan.copy(bankName = newBank)
                        },
                        label = { Text("Bank / FI", fontSize = 10.sp) },
                        placeholder = { Text("e.g. BRAC / DBBL", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                      )
                      OutlinedTextField(
                        value = loan.loanType,
                        onValueChange = { newType ->
                          loans[index] = loan.copy(loanType = newType)
                        },
                        label = { Text("Type", fontSize = 10.sp) },
                        placeholder = { Text("Personal / Auto", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                      )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                      value = loan.monthlyEmi,
                      onValueChange = { newEmi ->
                        val sanitized = sanitizeNumberInput(newEmi)
                        loans[index] = loan.copy(monthlyEmi = sanitized)
                      },
                      label = { Text("Monthly EMI (BDT) *", fontSize = 10.sp) },
                      placeholder = { Text("12500", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Total Existing Loan EMIs:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
            Text("BDT ${nf.format(totalLoanEmi.roundToInt())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
          }
        }
      }
    }

    // 4. Proposed Facility Card
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "4. Proposed Limit & Obligation",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = proposedLimitText,
            onValueChange = { proposedLimitText = sanitizeNumberInput(it) },
            label = { Text("Proposed Limit (BDT) *") },
            placeholder = { Text("e.g. 100000") },
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = EblNavyPrimary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_dbr_proposed_limit"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Proposed Monthly Obligation (3%):", fontSize = 11.sp, color = Color.Gray)
            Text("BDT ${nf.format(proposedCommitment.roundToInt())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
          }
        }
      }
    }

    // 5. LIVE DBR ASSESSMENT RESULT
    item {
      val resultBgColor = when {
        salary <= 0 -> Color(0xFFF1F5F9)
        !isMinSalaryMet -> Color(0xFFFEF3C7)
        dbrPercent <= maxAllowableDbr -> Color(0xFFF0FDF4)
        else -> Color(0xFFFEF2F2)
      }

      val resultBorderColor = when {
        salary <= 0 -> Color(0xFFCBD5E1)
        !isMinSalaryMet -> Color(0xFFFCD34D)
        dbrPercent <= maxAllowableDbr -> Color(0xFF86EFAC)
        else -> Color(0xFFFCA5A5)
      }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = resultBgColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, resultBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().testTag("dbr_result_card")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "DEBT BURDEN RATIO (DBR) RESULT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark,
            letterSpacing = 1.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Hero DBR %
          Text(
            text = if (salary > 0) String.format(Locale.US, "%.2f%%", dbrPercent) else "-- %",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
              salary <= 0 -> Color.Gray
              !isMinSalaryMet -> Color(0xFFD97706)
              dbrPercent <= maxAllowableDbr -> Color(0xFF16A34A)
              else -> Color(0xFFDC2626)
            }
          )

          // Status Badge
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = when {
              salary <= 0 -> Color.LightGray
              !isMinSalaryMet -> Color(0xFFD97706)
              dbrPercent <= maxAllowableDbr -> Color(0xFF16A34A)
              else -> Color(0xFFDC2626)
            },
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Text(
              text = when {
                salary <= 0 -> "ENTER SALARY TO CALCULATE"
                !isMinSalaryMet -> "INELIGIBLE - MIN SALARY NOT MET"
                dbrPercent <= maxAllowableDbr -> "ELIGIBLE / APPROVED (DBR <= ${maxAllowableDbr.roundToInt()}%)"
                else -> "OVER-BURDENED / HIGH DBR (> ${maxAllowableDbr.roundToInt()}%)"
              },
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = resultBorderColor)
          Spacer(modifier = Modifier.height(10.dp))

          // Key Metrics Breakdown
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            BreakdownLine("Monthly Net Salary:", "BDT ${nf.format(salary.roundToInt())}")
            BreakdownLine("Considered Card Obligation:", "BDT ${nf.format(consideredCardObligation.roundToInt())}")
            BreakdownLine("Total Loan EMIs:", "BDT ${nf.format(totalLoanEmi.roundToInt())}")
            BreakdownLine("Proposed Commitment (3%):", "BDT ${nf.format(proposedCommitment.roundToInt())}")
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            BreakdownLine("Total Monthly Debt Obligation:", "BDT ${nf.format(totalDebtObligation.roundToInt())}", isBold = true)
            BreakdownLine("Max Allowable DBR Cap:", "${String.format(Locale.US, "%.1f", maxAllowableDbr)}%", isBold = true)
            BreakdownLine("Remaining Disposable Income:", "BDT ${nf.format(disposableIncome.roundToInt())}")
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            BreakdownLine(
              "Max Safe Eligible Proposed Limit:",
              "BDT ${nf.format(maxSafeProposedLimit.roundToInt())}",
              isBold = true,
              valueColor = if (maxSafeProposedLimit > 0) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action Buttons: Copy / Share / Reset
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val report = """
[EBL Sales Suite - DBR Assessment]
Customer: ${customerName.ifBlank { "Applicant" }}
Segment: $selectedSegment
Monthly Salary: BDT ${nf.format(salary.roundToInt())}
---------------------------------
• Card Obligation (max 3% limit / 5% O/S): BDT ${nf.format(consideredCardObligation.roundToInt())}
• Existing Loan EMI: BDT ${nf.format(totalLoanEmi.roundToInt())}
• Proposed Limit: BDT ${nf.format(proposedLimit.roundToInt())} (3% = BDT ${nf.format(proposedCommitment.roundToInt())})
• Total Monthly Debt: BDT ${nf.format(totalDebtObligation.roundToInt())}
---------------------------------
Calculated DBR: ${String.format(Locale.US, "%.2f", dbrPercent)}%
Max Allowable DBR: ${String.format(Locale.US, "%.1f", maxAllowableDbr)}%
Status: ${if (isEligible) "ELIGIBLE / APPROVED" else "OVER-BURDENED / HIGH DBR"}
Max Safe Proposed Limit: BDT ${nf.format(maxSafeProposedLimit.roundToInt())}
Assessed by RM: ${currentUser.name} (${currentUser.rmCode})
                """.trimIndent()

                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("EBL DBR Assessment", report)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "DBR Assessment copied to clipboard!", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("btn_copy_dbr_report")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Summary", fontSize = 11.sp)
            }

            Button(
              onClick = {
                val shareText = """
EBL Retail Credit Assessment:
Customer: ${customerName.ifBlank { "Applicant" }}
Salary: BDT ${nf.format(salary.roundToInt())}
Total Debt Obligation: BDT ${nf.format(totalDebtObligation.roundToInt())}
Calculated DBR: ${String.format(Locale.US, "%.2f", dbrPercent)}% (Max Cap: ${maxAllowableDbr.roundToInt()}%)
Verdict: ${if (isEligible) "ELIGIBLE" else "HIGH DBR"}
Safe Proposed Limit: BDT ${nf.format(maxSafeProposedLimit.roundToInt())}
                """.trimIndent()
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, shareText)
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share DBR Assessment"))
              },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("btn_share_dbr_report")
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share DBR", fontSize = 11.sp)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun BreakdownLine(
  label: String,
  value: String,
  isBold: Boolean = false,
  valueColor: Color = Color.Unspecified
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
      color = if (isBold) EblNavyDark else Color(0xFF475569)
    )
    Text(
      text = value,
      fontSize = 11.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
      color = if (valueColor != Color.Unspecified) valueColor else if (isBold) EblNavyDark else Color(0xFF1E293B)
    )
  }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: DOCUMENT CHECKLIST SENDER (Presets, Corporate Card 2-Part Docs, Universal Templates & Admin Customization)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentChecklistSenderTab(
  currentUser: UserEntity,
  availableFiles: List<CustomerFileEntity>,
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var customerName by remember { mutableStateOf("") }
  var customerMobile by remember { mutableStateOf("") }

  // Presets requested by user: Credit Card Limit Enhance, Corporate Card, etc.
  val defaultPresets = listOf(
    "Credit Card Limit Enhance",
    "Corporate Card",
    "New Credit Card (Salaried)",
    "New Credit Card (Business Person)",
    "Personal / Auto / Home Loan"
  )
  val appSettingsList = viewModel?.appSettings?.collectAsState()?.value ?: emptyList()
  val customPresetsSetting = appSettingsList.find { it.settingKey == "CHECKLIST_CUSTOM_PRESETS_LIST" }?.settingValue
  val customPresetsList = remember(customPresetsSetting) {
    if (!customPresetsSetting.isNullOrBlank()) {
      try {
        val arr = org.json.JSONArray(customPresetsSetting)
        val l = mutableListOf<String>()
        for (i in 0 until arr.length()) l.add(arr.getString(i))
        l
      } catch (_: Exception) {
        customPresetsSetting.split(",").map { it.trim() }.filter { it.isNotBlank() }
      }
    } else emptyList()
  }
  val presetOptions = remember(customPresetsList) { defaultPresets + customPresetsList.filter { it !in defaultPresets } }

  var selectedPreset by remember { mutableStateOf(presetOptions[0]) }
  var presetExpanded by remember { mutableStateOf(false) }

  var showCreatePresetDialog by remember { mutableStateOf(false) }
  var editingItemTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
  var editingCorpCompanyTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
  var editingCorpEmployeeTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }

  // Universal Templates (synced from AppSettings if available)
  val savedHeaderTpl = appSettingsList.find { it.settingKey == "CHECKLIST_HEADER_TEMPLATE" }?.settingValue
    ?: "Dear {CUSTOMER_NAME},\nGreetings from Eastern Bank PLC (EBL).\nTo process your application for {PRESET_NAME}, please provide the following required documents:"

  val savedRegardsTpl = appSettingsList.find { it.settingKey == "CHECKLIST_REGARDS_TEMPLATE" }?.settingValue
    ?: "For any query or assistance, please contact:\n{RM_NAME}\nRM Code: {RM_CODE}\nMobile: {RM_PHONE}\nEastern Bank PLC"

  val savedCorpDocsJson = appSettingsList.find { it.settingKey == "CHECKLIST_CORP_DOCS" }?.settingValue
  val savedCorpCompanyDocs = appSettingsList.find { it.settingKey == "CHECKLIST_CORP_COMPANY_DOCS" }?.settingValue
  val savedCorpEmployeeDocs = appSettingsList.find { it.settingKey == "CHECKLIST_CORP_EMPLOYEE_DOCS" }?.settingValue
  val savedEnhanceDocsJson = appSettingsList.find { it.settingKey == "CHECKLIST_ENHANCE_DOCS" }?.settingValue
  val savedSalariedDocs = appSettingsList.find { it.settingKey == "CHECKLIST_SALARIED_DOCS" }?.settingValue
  val savedBusinessDocs = appSettingsList.find { it.settingKey == "CHECKLIST_BUSINESS_DOCS" }?.settingValue
  val savedLoanDocs = appSettingsList.find { it.settingKey == "CHECKLIST_LOAN_DOCS" }?.settingValue

  var headerTemplate by remember(savedHeaderTpl) { mutableStateOf(savedHeaderTpl) }
  var regardsTemplate by remember(savedRegardsTpl) { mutableStateOf(savedRegardsTpl) }
  var showAdminTemplateDialog by remember { mutableStateOf(false) }

  fun parseCustomDocs(raw: String?, fallback: List<String>): List<String> {
    if (raw.isNullOrBlank()) return fallback
    val parsed = if (raw.trim().startsWith("[")) {
      try {
        val arr = org.json.JSONArray(raw)
        val l = mutableListOf<String>()
        for (i in 0 until arr.length()) l.add(arr.getString(i))
        l
      } catch (_: Exception) {
        raw.lines().map { it.trim() }.filter { it.isNotBlank() }
      }
    } else {
      raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    }
    return if (parsed.isNotEmpty()) parsed else fallback
  }

  // 1. Credit Card Limit Enhance Docs
  val defaultEnhanceDocs = remember(savedEnhanceDocsJson) {
    parseCustomDocs(
      savedEnhanceDocsJson,
      listOf(
        "Front & back photocopy of existing Credit Card",
        "Latest 6-month Salary / Business Bank Account Statement (sealed)",
        "Latest Salary Certificate / Original Pay Slips / Trade License renewal copy",
        "Latest E-TIN Certificate & Tax Return Assessment Acknowledgement Slip",
        "Photocopy of National ID Card (NID) / Smart Card"
      )
    )
  }

  // 2. Corporate Card Docs: TWO DISTINCT PARTS (Company Document & Employee Document)
  val defaultCorporateCompanyDocs = remember(savedCorpCompanyDocs, savedCorpDocsJson) {
    if (!savedCorpCompanyDocs.isNullOrBlank()) {
      parseCustomDocs(savedCorpCompanyDocs, emptyList())
    } else if (!savedCorpDocsJson.isNullOrBlank()) {
      try {
        val obj = org.json.JSONObject(savedCorpDocsJson)
        val arr = obj.optJSONArray("company")
        if (arr != null && arr.length() > 0) {
          val list = mutableListOf<String>()
          for (i in 0 until arr.length()) list.add(arr.getString(i))
          list
        } else listOf(
          "Valid Trade License (last 3-5 years renewal copies)",
          "Memorandum & Articles of Association (MOA & AOA) / Partnership Deed",
          "Board Resolution authorizing Corporate Card facility & authorized signatories",
          "Latest 2 consecutive years Audited Financial Statements & Balance Sheet",
          "Form XII / Schedule X / List of Directors certified copy",
          "Company E-TIN Certificate & BIN / VAT Registration Certificate",
          "12-Month Company Bank Account Statement (with bank seal & signature)"
        )
      } catch (_: Exception) {
        listOf(
          "Valid Trade License (last 3-5 years renewal copies)",
          "Memorandum & Articles of Association (MOA & AOA) / Partnership Deed",
          "Board Resolution authorizing Corporate Card facility & authorized signatories",
          "Latest 2 consecutive years Audited Financial Statements & Balance Sheet",
          "Form XII / Schedule X / List of Directors certified copy",
          "Company E-TIN Certificate & BIN / VAT Registration Certificate",
          "12-Month Company Bank Account Statement (with bank seal & signature)"
        )
      }
    } else {
      listOf(
        "Valid Trade License (last 3-5 years renewal copies)",
        "Memorandum & Articles of Association (MOA & AOA) / Partnership Deed",
        "Board Resolution authorizing Corporate Card facility & authorized signatories",
        "Latest 2 consecutive years Audited Financial Statements & Balance Sheet",
        "Form XII / Schedule X / List of Directors certified copy",
        "Company E-TIN Certificate & BIN / VAT Registration Certificate",
        "12-Month Company Bank Account Statement (with bank seal & signature)"
      )
    }
  }

  val defaultCorporateEmployeeDocs = remember(savedCorpEmployeeDocs, savedCorpDocsJson) {
    if (!savedCorpEmployeeDocs.isNullOrBlank()) {
      parseCustomDocs(savedCorpEmployeeDocs, emptyList())
    } else if (!savedCorpDocsJson.isNullOrBlank()) {
      try {
        val obj = org.json.JSONObject(savedCorpDocsJson)
        val arr = obj.optJSONArray("employee")
        if (arr != null && arr.length() > 0) {
          val list = mutableListOf<String>()
          for (i in 0 until arr.length()) list.add(arr.getString(i))
          list
        } else listOf(
          "Applicant Employee NID / Smart Card / Valid Passport photocopy",
          "Employee Office ID Card photocopy & Business Visiting Card",
          "Letter of Introduction (LOI) / Corporate Card authorization on official company letterhead",
          "2 copies recent Passport size lab-print photographs of applicant",
          "Applicant Employee E-TIN Certificate photocopy",
          "Latest 6-Month Salary Account Bank Statement"
        )
      } catch (_: Exception) {
        listOf(
          "Applicant Employee NID / Smart Card / Valid Passport photocopy",
          "Employee Office ID Card photocopy & Business Visiting Card",
          "Letter of Introduction (LOI) / Corporate Card authorization on official company letterhead",
          "2 copies recent Passport size lab-print photographs of applicant",
          "Applicant Employee E-TIN Certificate photocopy",
          "Latest 6-Month Salary Account Bank Statement"
        )
      }
    } else {
      listOf(
        "Applicant Employee NID / Smart Card / Valid Passport photocopy",
        "Employee Office ID Card photocopy & Business Visiting Card",
        "Letter of Introduction (LOI) / Corporate Card authorization on official company letterhead",
        "2 copies recent Passport size lab-print photographs of applicant",
        "Applicant Employee E-TIN Certificate photocopy",
        "Latest 6-Month Salary Account Bank Statement"
      )
    }
  }

  // 3. New Credit Card Salaried Docs
  val defaultSalariedDocs = remember(savedSalariedDocs) {
    parseCustomDocs(
      savedSalariedDocs,
      listOf(
        "NID / Smart Card / Valid Passport photocopy",
        "2 copies recent Passport size photographs",
        "Latest E-TIN Certificate & Tax Return Acknowledgment Slip",
        "Latest Salary Certificate / Original Pay Slips (last 3 months)",
        "6-month Salary Account Statement (with bank seal & signature)",
        "Office ID Card photocopy & Visiting Card",
        "Utility Bill photocopy (Electricity / WASA / Gas residence)"
      )
    )
  }

  // 4. New Credit Card Business Docs
  val defaultBusinessDocs = remember(savedBusinessDocs) {
    parseCustomDocs(
      savedBusinessDocs,
      listOf(
        "National ID Card (NID) photocopy",
        "2 copies recent Passport size photographs",
        "Valid Trade License (last 3-5 years renewal copies)",
        "12-month Business & Personal Bank Account Statement (sealed)",
        "Latest E-TIN Certificate & Tax Return Acknowledgment Slip",
        "Visiting card & Memorandum of Association / Partnership Deed",
        "Utility bill of residence & business premises"
      )
    )
  }

  // 5. Loan Docs
  val defaultLoanDocs = remember(savedLoanDocs) {
    parseCustomDocs(
      savedLoanDocs,
      listOf(
        "National ID Card (NID) photocopy",
        "2 copies recent Passport size photographs",
        "Latest E-TIN Certificate & Tax Return Assessment Slip",
        "Income proof (Salary Certificate / 6-12 month Bank Statement)",
        "Office ID / Trade License photocopy",
        "Utility Bill photocopy (residence)"
      )
    )
  }

  fun getPresetItems(presetName: String): List<String> {
    val cleanKey = "CHECKLIST_PRESET_" + presetName.replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_").uppercase()
    val saved = appSettingsList.find { it.settingKey == cleanKey }?.settingValue
    if (!saved.isNullOrBlank()) {
      val parsed = parseCustomDocs(saved, emptyList())
      if (parsed.isNotEmpty()) return parsed
    }
    return when (presetName) {
      "Credit Card Limit Enhance" -> defaultEnhanceDocs
      "New Credit Card (Salaried)" -> defaultSalariedDocs
      "New Credit Card (Business Person)" -> defaultBusinessDocs
      "Personal / Auto / Home Loan" -> defaultLoanDocs
      else -> emptyList()
    }
  }

  // Active state lists
  val singleChecklistItems = remember(selectedPreset, appSettingsList) {
    mutableStateListOf<ChecklistItem>().apply {
      val items = getPresetItems(selectedPreset)
      items.forEachIndexed { i, t -> add(ChecklistItem(i + 1, t)) }
    }
  }

  // Corporate Card Two-Part state lists
  val corporateCompanyItems = remember(defaultCorporateCompanyDocs) {
    mutableStateListOf<ChecklistItem>().apply {
      defaultCorporateCompanyDocs.forEachIndexed { i, t -> add(ChecklistItem(i + 1, t)) }
    }
  }
  val corporateEmployeeItems = remember(defaultCorporateEmployeeDocs) {
    mutableStateListOf<ChecklistItem>().apply {
      defaultCorporateEmployeeDocs.forEachIndexed { i, t -> add(ChecklistItem(100 + i + 1, t)) }
    }
  }

  var newCustomDocText by remember { mutableStateOf("") }
  var newCorpCustomDocText by remember { mutableStateOf("") }
  var selectedFilePickerExpanded by remember { mutableStateOf(false) }
  var customerSearchQuery by remember { mutableStateOf("") }

  // Header and Regards auto-fill
  val custDisplayName = customerName.trim().ifBlank { "Customer" }
  val presetDisplayName = when (selectedPreset) {
    "Credit Card Limit Enhance" -> "Credit Card Limit Enhancement"
    "Corporate Card" -> "Corporate Credit Card"
    else -> selectedPreset
  }

  val resolvedHeader = headerTemplate
    .replace("{CUSTOMER_NAME}", custDisplayName)
    .replace("{PRESET_NAME}", presetDisplayName)

  val resolvedRegards = regardsTemplate
    .replace("{RM_NAME}", currentUser.name.ifBlank { "Relationship Manager" })
    .replace("{RM_CODE}", currentUser.rmCode)
    .replace("{RM_PHONE}", currentUser.mobile.ifBlank { "017XXXXXXXX" })

  // Construct formatted message
  val formattedMessage = remember(
    selectedPreset,
    resolvedHeader,
    resolvedRegards,
    singleChecklistItems.map { it.isChecked },
    corporateCompanyItems.map { it.isChecked },
    corporateEmployeeItems.map { it.isChecked }
  ) {
    val sb = StringBuilder()
    sb.appendLine(resolvedHeader)
    sb.appendLine()

    if (selectedPreset == "Corporate Card") {
      val checkedCompany = corporateCompanyItems.filter { it.isChecked }
      val checkedEmp = corporateEmployeeItems.filter { it.isChecked }

      sb.appendLine("🏢 [PART 1: COMPANY DOCUMENTS / কোম্পানি ডকুমেন্টস]")
      if (checkedCompany.isEmpty()) {
        sb.appendLine("• (No company documents selected)")
      } else {
        checkedCompany.forEachIndexed { idx, item ->
          sb.appendLine("${idx + 1}. ${item.title}")
        }
      }
      sb.appendLine()
      sb.appendLine("👤 [PART 2: EMPLOYEE DOCUMENTS / এমপ্লয়ি ডকুমেন্টস]")
      if (checkedEmp.isEmpty()) {
        sb.appendLine("• (No employee documents selected)")
      } else {
        checkedEmp.forEachIndexed { idx, item ->
          sb.appendLine("${idx + 1}. ${item.title}")
        }
      }
    } else {
      val checkedList = singleChecklistItems.filter { it.isChecked }
      if (checkedList.isEmpty()) {
        sb.appendLine("• (No documents selected)")
      } else {
        checkedList.forEachIndexed { idx, item ->
          sb.appendLine("${idx + 1}. ${item.title}")
        }
      }
    }

    sb.appendLine()
    sb.append(resolvedRegards)
    sb.toString()
  }

  // Admin / Mentor Template Editor Dialog
  if (showAdminTemplateDialog) {
    var editHeaderInput by remember { mutableStateOf(headerTemplate) }
    var editRegardsInput by remember { mutableStateOf(regardsTemplate) }
    var isSavingTpl by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showAdminTemplateDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Description, contentDescription = null, tint = EblNavyPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Universal Checklist Settings (এডমিন সেটিংস)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Set universal templates for all mobile devices. RM name and mobile will be automatically replaced with active RM credentials.",
            fontSize = 11.sp,
            color = Color.Gray
          )

          OutlinedTextField(
            value = editHeaderInput,
            onValueChange = { editHeaderInput = it },
            label = { Text("Universal Header Template", fontSize = 11.sp) },
            supportingText = { Text("Placeholders: {CUSTOMER_NAME}, {PRESET_NAME}", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          OutlinedTextField(
            value = editRegardsInput,
            onValueChange = { editRegardsInput = it },
            label = { Text("Universal Regards Template", fontSize = 11.sp) },
            supportingText = { Text("Placeholders: {RM_NAME}, {RM_CODE}, {RM_PHONE}", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isSavingTpl = true
            headerTemplate = editHeaderInput
            regardsTemplate = editRegardsInput
            viewModel?.saveUniversalChecklistSettings(
              headerTemplate = editHeaderInput,
              regardsTemplate = editRegardsInput
            ) { success, _ ->
              isSavingTpl = false
              showAdminTemplateDialog = false
              if (success) {
                Toast.makeText(context, "Universal templates updated and synced!", Toast.LENGTH_SHORT).show()
              }
            }
          },
          enabled = !isSavingTpl,
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save & Sync Universal")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAdminTemplateDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      // Guidance header banner
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Document Checklist Dispatcher",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E3A8A)
            )
            Text(
              text = "Preset auto-loads headers & 2-part Corporate Card docs. Sends via SMS / WhatsApp.",
              fontSize = 11.sp,
              color = Color(0xFF3B82F6)
            )
          }

          if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
            IconButton(
              onClick = { showAdminTemplateDialog = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Description, contentDescription = "Universal Template Settings", tint = EblNavyPrimary)
            }
          }
        }
      }
    }

    // 1. Preset Selector & Recipient Information
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "1. Select Checklist Preset & Customer",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
            if (availableFiles.isNotEmpty()) {
              Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = "${availableFiles.size} Recent Files",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = EblNavyPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quick Pick from Recent Customer Files (NO TEXT WRAPPING)
          if (availableFiles.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Select Recent Customer (সাম্প্রতিক কাস্টমার):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              if (customerName.isNotBlank() || customerMobile.isNotBlank()) {
                TextButton(
                  onClick = {
                    customerName = ""
                    customerMobile = ""
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Text("Clear", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
              expanded = selectedFilePickerExpanded,
              onExpandedChange = { selectedFilePickerExpanded = !selectedFilePickerExpanded },
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = if (customerName.isNotBlank() && customerMobile.isNotBlank()) {
                  "$customerName ($customerMobile)"
                } else if (customerName.isNotBlank()) {
                  customerName
                } else {
                  ""
                },
                onValueChange = {},
                readOnly = true,
                placeholder = {
                  Text(
                    "Choose a recent customer to auto-fill...",
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                },
                leadingIcon = {
                  Icon(
                    Icons.Default.RecentActors,
                    contentDescription = null,
                    tint = EblNavyPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                },
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectedFilePickerExpanded)
                },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .menuAnchor()
                  .testTag("dropdown_recent_customer_picker"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EblNavyPrimary,
                  unfocusedBorderColor = Color(0xFFCBD5E1)
                )
              )

              ExposedDropdownMenu(
                expanded = selectedFilePickerExpanded,
                onDismissRequest = {
                  selectedFilePickerExpanded = false
                  customerSearchQuery = ""
                },
                modifier = Modifier.heightIn(max = 350.dp)
              ) {
                if (availableFiles.size > 5) {
                  OutlinedTextField(
                    value = customerSearchQuery,
                    onValueChange = { customerSearchQuery = it },
                    placeholder = { Text("Search name, mobile or card...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = EblNavyPrimary,
                      unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                  )
                  HorizontalDivider(color = Color(0xFFE2E8F0))
                }

                val filteredList = if (customerSearchQuery.isBlank()) {
                  availableFiles
                } else {
                  availableFiles.filter {
                    it.customerName.contains(customerSearchQuery, ignoreCase = true) ||
                    it.mobile.contains(customerSearchQuery) ||
                    it.productType.contains(customerSearchQuery, ignoreCase = true) ||
                    it.ccNumber.contains(customerSearchQuery, ignoreCase = true)
                  }
                }

                if (filteredList.isEmpty()) {
                  DropdownMenuItem(
                    text = { Text("No matching customer file found", fontSize = 12.sp, color = Color.Gray) },
                    onClick = {}
                  )
                } else {
                  filteredList.take(25).forEach { f ->
                    DropdownMenuItem(
                      text = {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                              text = f.customerName.ifBlank { "Unnamed Applicant" },
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp,
                              color = EblNavyDark,
                              maxLines = 1,
                              softWrap = false,
                              overflow = TextOverflow.Ellipsis
                            )
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.padding(top = 2.dp)
                            ) {
                              Text(
                                text = f.mobile.ifBlank { "No Mobile" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF059669),
                                maxLines = 1,
                                softWrap = false
                              )
                              if (f.productType.isNotBlank()) {
                                Text(
                                  text = " • ${f.productType}",
                                  fontSize = 11.sp,
                                  color = Color(0xFF64748B),
                                  maxLines = 1,
                                  softWrap = false,
                                  overflow = TextOverflow.Ellipsis
                                )
                              }
                            }
                          }
                          if (f.applicationStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                              color = when (f.applicationStatus.uppercase()) {
                                "APPROVED" -> Color(0xFFDEF7EC)
                                "DECLINED" -> Color(0xFFFDE8E8)
                                else -> Color(0xFFEFF6FF)
                              },
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(
                                text = f.applicationStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (f.applicationStatus.uppercase()) {
                                  "APPROVED" -> Color(0xFF03543F)
                                  "DECLINED" -> Color(0xFF9B1C1C)
                                  else -> Color(0xFF1E40AF)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                              )
                            }
                          }
                        }
                      },
                      onClick = {
                        customerName = f.customerName
                        customerMobile = f.mobile
                        val prod = f.productType.lowercase()
                        if (prod.contains("corporate")) {
                          selectedPreset = "Corporate Card"
                        } else if (prod.contains("enhance")) {
                          selectedPreset = "Credit Card Limit Enhance"
                        } else if (prod.contains("business")) {
                          if ("New Credit Card (Business Person)" in presetOptions) {
                            selectedPreset = "New Credit Card (Business Person)"
                          }
                        } else if (prod.contains("salaried")) {
                          if ("New Credit Card (Salaried)" in presetOptions) {
                            selectedPreset = "New Credit Card (Salaried)"
                          }
                        } else if (prod.contains("loan")) {
                          if ("Personal / Auto / Home Loan" in presetOptions) {
                            selectedPreset = "Personal / Auto / Home Loan"
                          }
                        }
                        selectedFilePickerExpanded = false
                        customerSearchQuery = ""
                      },
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          // Preset Selection & Admin "+ New Preset" action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Checklist Preset (প্রিসেট সিলেক্ট করুন):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
            if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
              TextButton(
                onClick = { showCreatePresetDialog = true },
                modifier = Modifier.testTag("btn_create_new_preset_dialog")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ New Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))

          // Preset Dropdown
          ExposedDropdownMenuBox(
            expanded = presetExpanded,
            onExpandedChange = { presetExpanded = !presetExpanded }
          ) {
            OutlinedTextField(
              value = selectedPreset,
              onValueChange = {},
              readOnly = true,
              label = { Text("Checklist Preset *", fontWeight = FontWeight.SemiBold) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_checklist_preset"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EblNavyPrimary,
                unfocusedBorderColor = EblGold
              )
            )
            ExposedDropdownMenu(
              expanded = presetExpanded,
              onDismissRequest = { presetExpanded = false }
            ) {
              presetOptions.forEach { opt ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(opt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      if (opt == "Corporate Card") {
                        Text("2-Part: Company Docs & Employee Docs", fontSize = 10.sp, color = Color.Gray)
                      }
                    }
                  },
                  onClick = {
                    selectedPreset = opt
                    presetExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer / Applicant Name *") },
            placeholder = { Text("e.g. Md. Kabir Hossain") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_checklist_customer_name"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customerMobile,
            onValueChange = { customerMobile = it },
            label = { Text("Customer Mobile Number *") },
            placeholder = { Text("017XXXXXXXX or 018XXXXXXXX") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF059669)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_checklist_customer_mobile"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )
        }
      }
    }

    // 2. Interactive Document Checklist Items
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          if (selectedPreset == "Corporate Card") {
            // CORPORATE CARD: TWO SEPARATE PARTS
            // Part 1: Company Documents
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🏢 Part 1: Company Documents (${corporateCompanyItems.count { it.isChecked }}/${corporateCompanyItems.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text("কোম্পানি ডকুমেন্টস", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(6.dp))

            corporateCompanyItems.forEachIndexed { index, item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { corporateCompanyItems[index] = item.copy(isChecked = !item.isChecked) }
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = item.isChecked,
                  onCheckedChange = { checked -> corporateCompanyItems[index] = item.copy(isChecked = checked) },
                  colors = CheckboxDefaults.colors(checkedColor = EblNavyPrimary),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = item.title,
                  fontSize = 12.sp,
                  color = if (item.isChecked) Color(0xFF1E293B) else Color.Gray,
                  fontWeight = if (item.isChecked) FontWeight.Medium else FontWeight.Normal,
                  modifier = Modifier.weight(1f)
                )
                if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                  IconButton(
                    onClick = { editingCorpCompanyTarget = Pair(index, item.title) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                  }
                }
                IconButton(
                  onClick = { corporateCompanyItems.removeAt(index) },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Add custom Company doc
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedTextField(
                value = newCorpCustomDocText,
                onValueChange = { newCorpCustomDocText = it },
                placeholder = { Text("+ Add custom company document...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
              )
              Button(
                onClick = {
                  if (newCorpCustomDocText.isNotBlank()) {
                    corporateCompanyItems.add(
                      ChecklistItem(id = corporateCompanyItems.size + 1, title = newCorpCustomDocText.trim(), isChecked = true)
                    )
                    newCorpCustomDocText = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Add", fontSize = 11.sp)
              }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Part 2: Employee Documents
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "👤 Part 2: Employee Documents (${corporateEmployeeItems.count { it.isChecked }}/${corporateEmployeeItems.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text("এমপ্লয়ি ডকুমেন্টস", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(6.dp))

            corporateEmployeeItems.forEachIndexed { index, item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { corporateEmployeeItems[index] = item.copy(isChecked = !item.isChecked) }
                  .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = item.isChecked,
                  onCheckedChange = { checked -> corporateEmployeeItems[index] = item.copy(isChecked = checked) },
                  colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = item.title,
                  fontSize = 12.sp,
                  color = if (item.isChecked) Color(0xFF1E293B) else Color.Gray,
                  fontWeight = if (item.isChecked) FontWeight.Medium else FontWeight.Normal,
                  modifier = Modifier.weight(1f)
                )
                if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                  IconButton(
                    onClick = { editingCorpEmployeeTarget = Pair(index, item.title) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                  }
                }
                IconButton(
                  onClick = { corporateEmployeeItems.removeAt(index) },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Add custom Employee doc
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedTextField(
                value = newCustomDocText,
                onValueChange = { newCustomDocText = it },
                placeholder = { Text("+ Add custom employee document...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
              )
              Button(
                onClick = {
                  if (newCustomDocText.isNotBlank()) {
                    corporateEmployeeItems.add(
                      ChecklistItem(id = 100 + corporateEmployeeItems.size + 1, title = newCustomDocText.trim(), isChecked = true)
                    )
                    newCustomDocText = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Add", fontSize = 11.sp)
              }
            }

          } else {
            // STANDARD / LIMIT ENHANCE CHECKLIST
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "$selectedPreset (${singleChecklistItems.count { it.isChecked }}/${singleChecklistItems.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text("Check/uncheck items", fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            singleChecklistItems.forEachIndexed { index, item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { singleChecklistItems[index] = item.copy(isChecked = !item.isChecked) }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = item.isChecked,
                  onCheckedChange = { checked -> singleChecklistItems[index] = item.copy(isChecked = checked) },
                  colors = CheckboxDefaults.colors(checkedColor = EblNavyPrimary),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = item.title,
                  fontSize = 12.sp,
                  color = if (item.isChecked) Color(0xFF1E293B) else Color.Gray,
                  fontWeight = if (item.isChecked) FontWeight.Medium else FontWeight.Normal,
                  modifier = Modifier.weight(1f)
                )
                if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                  IconButton(
                    onClick = { editingItemTarget = Pair(index, item.title) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                  }
                }
                IconButton(
                  onClick = { singleChecklistItems.removeAt(index) },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add custom document item
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedTextField(
                value = newCustomDocText,
                onValueChange = { newCustomDocText = it },
                placeholder = { Text("Add custom required document...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
              )
              Button(
                onClick = {
                  if (newCustomDocText.isNotBlank()) {
                    singleChecklistItems.add(
                      ChecklistItem(id = singleChecklistItems.size + 1, title = newCustomDocText.trim(), isChecked = true)
                    )
                    newCustomDocText = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Add", fontSize = 12.sp)
              }
            }
          }

          // Admin / Mentor button to save preset items universally
          if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
              onClick = {
                val corpJson = org.json.JSONObject().apply {
                  put("company", org.json.JSONArray(corporateCompanyItems.map { it.title }))
                  put("employee", org.json.JSONArray(corporateEmployeeItems.map { it.title }))
                }.toString()
                val enhanceJson = if (selectedPreset == "Credit Card Limit Enhance") {
                  org.json.JSONArray(singleChecklistItems.map { it.title }).toString()
                } else ""

                // Save active preset items string
                val activeItemsStr = singleChecklistItems.map { it.title }.joinToString("\n")
                val activePresetKey = "CHECKLIST_PRESET_" + selectedPreset.replace(" ", "_").uppercase()
                viewModel?.updateSetting(activePresetKey, activeItemsStr)

                // Ensure custom preset name is recorded in CHECKLIST_CUSTOM_PRESETS_LIST
                val updatedCustomList = (customPresetsList + selectedPreset).distinct()
                viewModel?.updateSetting("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(updatedCustomList).toString())

                viewModel?.saveUniversalChecklistSettings(
                  headerTemplate = headerTemplate,
                  regardsTemplate = regardsTemplate,
                  corporateDocsJson = corpJson,
                  enhancementDocsJson = enhanceJson
                ) { success, _ ->
                  if (success) {
                    Toast.makeText(context, "✓ প্রিসেট '${selectedPreset}' ও ডকুমেন্টস ইউনিভার্সাল হিসেবে গুগল শিটে সেভ ও সিঙ্ক হয়েছে!", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_save_universal_checklist")
            ) {
              Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Preset Items as Universal (সকল RM এর জন্য সেভ করুন)", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3. Message Preview & Sending Options
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Dispatched Message Preview",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
            Text(
              text = "Universal Header & RM Info Auto-Applied",
              fontSize = 10.sp,
              color = Color(0xFF059669),
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = formattedMessage,
              fontSize = 11.sp,
              color = Color(0xFF334155),
              lineHeight = 16.sp,
              modifier = Modifier.padding(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Dispatch Buttons Grid
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // 1. Send SMS Button
              Button(
                onClick = {
                  if (customerMobile.isBlank()) {
                    Toast.makeText(context, "Please enter customer mobile number!", Toast.LENGTH_SHORT).show()
                    return@Button
                  }
                  // Try hardware SMS first
                  val sent = SmsService.sendSms(context, customerMobile, formattedMessage)
                  if (!sent) {
                    // Fallback to launching device SMS app
                    try {
                      val smsUri = Uri.parse("smsto:$customerMobile")
                      val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", formattedMessage)
                      }
                      context.startActivity(smsIntent)
                    } catch (e: Exception) {
                      Toast.makeText(context, "Could not launch SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                  } else {
                    Toast.makeText(context, "SMS sent to $customerMobile successfully!", Toast.LENGTH_LONG).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_send_sms_checklist")
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              // 2. Send via WhatsApp Button
              Button(
                onClick = {
                  val cleanNumber = customerMobile.trim().replace(" ", "").replace("-", "")
                  val targetNumber = if (cleanNumber.startsWith("01")) {
                    "88$cleanNumber"
                  } else cleanNumber

                  try {
                    val url = "https://api.whatsapp.com/send?phone=$targetNumber&text=${Uri.encode(formattedMessage)}"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                      data = Uri.parse(url)
                    }
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    Toast.makeText(context, "WhatsApp is not installed or invalid number: ${e.message}", Toast.LENGTH_SHORT).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_send_whatsapp_checklist")
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // 3. Copy to Clipboard Button
              OutlinedButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("EBL Document Checklist", formattedMessage)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Checklist copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_copy_checklist")
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Text", fontSize = 12.sp)
              }

              // 4. Share Sheet Button
              OutlinedButton(
                onClick = {
                  val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, formattedMessage)
                    type = "text/plain"
                  }
                  context.startActivity(Intent.createChooser(sendIntent, "Share Document Checklist"))
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_share_checklist")
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Dialog: Edit Item Name for Single Checklist
  if (editingItemTarget != null) {
    val target = editingItemTarget!!
    var editedName by remember(target) { mutableStateOf(target.second) }
    AlertDialog(
      onDismissRequest = { editingItemTarget = null },
      title = { Text("Edit Item Name", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        OutlinedTextField(
          value = editedName,
          onValueChange = { editedName = it },
          label = { Text("Item Name") },
          modifier = Modifier.fillMaxWidth().testTag("input_edit_checklist_item_name")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val idx = target.first
            if (idx in 0 until singleChecklistItems.size && editedName.isNotBlank()) {
              singleChecklistItems[idx] = singleChecklistItems[idx].copy(title = editedName.trim())
              if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                val cleanKey = "CHECKLIST_PRESET_" + selectedPreset.replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_").uppercase()
                val updatedStr = singleChecklistItems.map { it.title }.joinToString("\n")
                viewModel?.updateSetting(cleanKey, updatedStr)
                viewModel?.triggerGoogleSheetsSync()
              }
            }
            editingItemTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Update Name")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingItemTarget = null }) { Text("Cancel") }
      }
    )
  }

  // Dialog: Edit Item Name for Corporate Company Item
  if (editingCorpCompanyTarget != null) {
    val target = editingCorpCompanyTarget!!
    var editedName by remember(target) { mutableStateOf(target.second) }
    AlertDialog(
      onDismissRequest = { editingCorpCompanyTarget = null },
      title = { Text("Edit Company Doc Name", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        OutlinedTextField(
          value = editedName,
          onValueChange = { editedName = it },
          label = { Text("Document Name") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val idx = target.first
            if (idx in 0 until corporateCompanyItems.size && editedName.isNotBlank()) {
              corporateCompanyItems[idx] = corporateCompanyItems[idx].copy(title = editedName.trim())
            }
            editingCorpCompanyTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Update Name")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCorpCompanyTarget = null }) { Text("Cancel") }
      }
    )
  }

  // Dialog: Edit Item Name for Corporate Employee Item
  if (editingCorpEmployeeTarget != null) {
    val target = editingCorpEmployeeTarget!!
    var editedName by remember(target) { mutableStateOf(target.second) }
    AlertDialog(
      onDismissRequest = { editingCorpEmployeeTarget = null },
      title = { Text("Edit Employee Doc Name", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        OutlinedTextField(
          value = editedName,
          onValueChange = { editedName = it },
          label = { Text("Document Name") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val idx = target.first
            if (idx in 0 until corporateEmployeeItems.size && editedName.isNotBlank()) {
              corporateEmployeeItems[idx] = corporateEmployeeItems[idx].copy(title = editedName.trim())
            }
            editingCorpEmployeeTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Update Name")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCorpEmployeeTarget = null }) { Text("Cancel") }
      }
    )
  }

  // Dialog: Create Brand New Preset by Admin
  if (showCreatePresetDialog) {
    var newPresetName by remember { mutableStateOf("") }
    var newPresetItemsText by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showCreatePresetDialog = false },
      title = { Text("Create New Checklist Preset", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("অ্যাডমিন নতুন প্রিসেট যোগ করলে তা ইউনিভার্সাল প্রিসেট হিসেবে সব ইউজারের কাছে চলে যাবে।", fontSize = 11.sp, color = Color.Gray)
          OutlinedTextField(
            value = newPresetName,
            onValueChange = { newPresetName = it },
            label = { Text("Preset Name (e.g. SME Business Loan)") },
            modifier = Modifier.fillMaxWidth().testTag("input_new_preset_name")
          )
          OutlinedTextField(
            value = newPresetItemsText,
            onValueChange = { newPresetItemsText = it },
            label = { Text("Required Documents (প্রতি লাইনে একটি আইটেম)") },
            placeholder = { Text("Trade License\nBank Statement\nNID Copy\n...") },
            maxLines = 6,
            modifier = Modifier.fillMaxWidth().testTag("input_new_preset_items")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cleanName = newPresetName.trim()
            if (cleanName.isNotBlank()) {
              val itemsList = newPresetItemsText.lines().map { it.trim() }.filter { it.isNotBlank() }
              val updatedCustom = (customPresetsList + cleanName).distinct()
              viewModel?.updateSetting("CHECKLIST_CUSTOM_PRESETS_LIST", org.json.JSONArray(updatedCustom).toString())

              val presetKey = "CHECKLIST_PRESET_" + cleanName.replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_").uppercase()
              viewModel?.updateSetting(presetKey, itemsList.joinToString("\n"))

              // Switch to this new preset immediately
              selectedPreset = cleanName
              singleChecklistItems.clear()
              itemsList.forEachIndexed { i, doc ->
                singleChecklistItems.add(ChecklistItem(id = i + 1, title = doc, isChecked = true))
              }

              // Trigger sync to Google Sheets immediately so all RMs receive it!
              viewModel?.triggerGoogleSheetsSync()

              showCreatePresetDialog = false
              Toast.makeText(context, "✓ নতুন প্রিসেট '$cleanName' সফলভাবে তৈরি ও সিঙ্ক হয়েছে!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.testTag("btn_save_new_preset")
        ) {
          Text("সেভ করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreatePresetDialog = false }) { Text("বাতিল") }
      }
    )
  }
}
