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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
  var id: Long = System.currentTimeMillis(),
  var bankName: String = "",
  var limit: String = "",
  var outstanding: String = ""
)

data class ExistingLoanEntry(
  var id: Long = System.currentTimeMillis(),
  var bankName: String = "",
  var loanType: String = "Personal Loan",
  var monthlyEmi: String = ""
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
        availableFiles = allFiles
      )
    }
  }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: DBR CALCULATOR
// -------------------------------------------------------------------------------------------------

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

  // Credit Cards List
  val creditCards = remember {
    mutableStateListOf(
      ExistingCreditCardEntry(id = 1, bankName = "EBL", limit = "", outstanding = "")
    )
  }

  // Loans List
  val loans = remember {
    mutableStateListOf<ExistingLoanEntry>()
  }

  // Calculations
  val salary = salaryText.toDoubleOrNull() ?: 0.0
  val proposedLimit = proposedLimitText.toDoubleOrNull() ?: 0.0

  // Existing Cards Total & 3% / 5%
  val totalCardLimit = creditCards.sumOf { it.limit.toDoubleOrNull() ?: 0.0 }
  val totalCardOutstanding = creditCards.sumOf { it.outstanding.toDoubleOrNull() ?: 0.0 }

  val cardLimit3Percent = totalCardLimit * 0.03
  val cardOutstanding5Percent = totalCardOutstanding * 0.05
  val consideredCardObligation = max(cardLimit3Percent, cardOutstanding5Percent)

  // Total Loan EMI
  val totalLoanEmi = loans.sumOf { it.monthlyEmi.toDoubleOrNull() ?: 0.0 }

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
            onValueChange = { salaryText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Monthly Net Salary (Banks Pay Part) *") },
            placeholder = { Text("e.g. 35000") },
            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF059669)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

    // 2. Existing Credit Cards Card (Multiple cards with limit & outstanding per card)
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
            Column {
              Text(
                text = "2. Existing Credit Cards",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = "Add all credit cards with limits & outstandings",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
            OutlinedButton(
              onClick = {
                creditCards.add(ExistingCreditCardEntry(id = System.currentTimeMillis()))
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_add_credit_card")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Card", fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          creditCards.forEachIndexed { index, card ->
            Card(
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
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
                  if (creditCards.size > 1) {
                    IconButton(
                      onClick = { creditCards.removeAt(index) },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedTextField(
                    value = card.bankName,
                    onValueChange = { card.bankName = it },
                    label = { Text("Bank Name", fontSize = 10.sp) },
                    placeholder = { Text("e.g. SCB / City", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                  )
                  OutlinedTextField(
                    value = card.limit,
                    onValueChange = { card.limit = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Limit (BDT) *", fontSize = 10.sp) },
                    placeholder = { Text("100000", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                  )
                  OutlinedTextField(
                    value = card.outstanding,
                    onValueChange = { card.outstanding = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("O/S (BDT)", fontSize = 10.sp) },
                    placeholder = { Text("25000", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                  )
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

    // 3. Existing Loans Card (Multiple loans)
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
            Column {
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
                loans.add(ExistingLoanEntry(id = System.currentTimeMillis()))
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_add_existing_loan")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Loan", fontSize = 11.sp)
            }
          }

          if (loans.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "No existing loans added. Tap '+ Add Loan' if customer has existing bank loan EMIs.",
              fontSize = 11.sp,
              color = Color.Gray,
              modifier = Modifier.padding(vertical = 6.dp)
            )
          } else {
            Spacer(modifier = Modifier.height(8.dp))
            loans.forEachIndexed { index, loan ->
              Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
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

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    OutlinedTextField(
                      value = loan.bankName,
                      onValueChange = { loan.bankName = it },
                      label = { Text("Bank / FI", fontSize = 10.sp) },
                      placeholder = { Text("e.g. BRAC / DBBL", fontSize = 10.sp) },
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                    )
                    OutlinedTextField(
                      value = loan.loanType,
                      onValueChange = { loan.loanType = it },
                      label = { Text("Type", fontSize = 10.sp) },
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                    )
                    OutlinedTextField(
                      value = loan.monthlyEmi,
                      onValueChange = { loan.monthlyEmi = it.filter { ch -> ch.isDigit() || ch == '.' } },
                      label = { Text("EMI (BDT) *", fontSize = 10.sp) },
                      placeholder = { Text("12500", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier.weight(1.2f),
                      colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
                    )
                  }
                }
              }
            }
          }

          if (loans.isNotEmpty()) {
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
            onValueChange = { proposedLimitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Proposed Limit (BDT) *") },
            placeholder = { Text("e.g. 100000") },
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = EblNavyPrimary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
// TAB 2: DOCUMENT CHECKLIST SENDER
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentChecklistSenderTab(
  currentUser: UserEntity,
  availableFiles: List<CustomerFileEntity>
) {
  val context = LocalContext.current

  var customerName by remember { mutableStateOf("") }
  var customerMobile by remember { mutableStateOf("") }

  val segmentOptions = listOf(
    "Salaried Executive / Govt. Employee",
    "Business Person / Proprietorship / Ltd. Co.",
    "Landlord / Landlady (Rental Income)",
    "Doctor / CA / Lawyer / Professional"
  )
  var selectedSegment by remember { mutableStateOf(segmentOptions[0]) }
  var segmentExpanded by remember { mutableStateOf(false) }

  val productOptions = listOf(
    "Credit Card",
    "Personal Loan",
    "Auto Loan",
    "Home Loan"
  )
  var selectedProduct by remember { mutableStateOf(productOptions[0]) }
  var productExpanded by remember { mutableStateOf(false) }

  // Dynamic Checklists based on segment & product
  val checklistItems = remember(selectedSegment, selectedProduct) {
    mutableStateListOf<ChecklistItem>().apply {
      // Common docs
      add(ChecklistItem(1, "NID / Smart Card / Valid Passport photocopy"))
      add(ChecklistItem(2, "2 copies recent Passport size lab-print photographs"))
      add(ChecklistItem(3, "Latest E-TIN Certificate & Tax Return Assessment Ack Slip"))
      add(ChecklistItem(4, "Utility Bill photocopy (Electricity / WASA / Gas for residence)"))

      when {
        selectedSegment.contains("Salaried") -> {
          add(ChecklistItem(5, "Latest Salary Certificate / Original Pay Slips (last 3 months)"))
          add(ChecklistItem(6, "6-month Salary Account Statement (with bank seal & signature)"))
          add(ChecklistItem(7, "Office ID Card photocopy & Business Visiting Card"))
          if (selectedProduct == "Credit Card") {
            add(ChecklistItem(8, "Letter of Introduction (LOI) on corporate letterhead"))
          }
        }
        selectedSegment.contains("Business") -> {
          add(ChecklistItem(5, "Valid Trade License (last 3-5 years renewal copies)"))
          add(ChecklistItem(6, "12-month Business & Personal Bank Account Statement (sealed)"))
          add(ChecklistItem(7, "Visiting card & Memorandum of Association / Partnership Deed"))
          add(ChecklistItem(8, "Office rental agreement or ownership papers"))
        }
        selectedSegment.contains("Landlord") -> {
          add(ChecklistItem(5, "Valid Rental Agreement copies with tenants"))
          add(ChecklistItem(6, "Title Deed (Dalil), Mutation (Namjari), DCR & latest Khajana receipt"))
          add(ChecklistItem(7, "6-12 month Rental Credit Bank Account Statement"))
          add(ChecklistItem(8, "Holding Tax receipt photocopy"))
        }
        selectedSegment.contains("Doctor") || selectedSegment.contains("Professional") -> {
          add(ChecklistItem(5, "BMDC / Bar Council / Professional Membership Certificate"))
          add(ChecklistItem(6, "6-month Practice / Professional Bank Account Statement"))
          add(ChecklistItem(7, "Visiting Card & Hospital / Chamber prescription pad or appointment letter"))
        }
      }

      if (selectedProduct == "Auto Loan") {
        add(ChecklistItem(9, "Vehicle Quotation from recognized dealership"))
      } else if (selectedProduct == "Home Loan") {
        add(ChecklistItem(9, "Approved Building Plan from RAJUK / CDA / Authority"))
        add(ChecklistItem(10, "All original property title documents"))
      }
    }
  }

  var newCustomDocText by remember { mutableStateOf("") }
  var selectedFilePickerExpanded by remember { mutableStateOf(false) }

  // Generated SMS / WhatsApp Message Text
  val checkedTitles = checklistItems.filter { it.isChecked }.mapIndexed { idx, it -> "${idx + 1}. ${it.title}" }
  val formattedMessage = """
Dear ${customerName.ifBlank { "Customer" }},
Greetings from Eastern Bank PLC (EBL).
To process your $selectedProduct application ($selectedSegment), please provide the following required documents:

${checkedTitles.joinToString("\n")}

For any query or assistance, please contact:
${currentUser.name}
RM Code: ${currentUser.rmCode}
Mobile: ${currentUser.mobile.ifBlank { "EBL Sales Hotline" }}
Eastern Bank PLC
  """.trimIndent()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      // Guidance header
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Document Checklist Dispatcher",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E3A8A)
            )
            Text(
              text = "Send customized checklist directly to customer via SMS, WhatsApp, or Clipboard",
              fontSize = 10.sp,
              color = Color(0xFF3B82F6)
            )
          }
        }
      }
    }

    // 1. Recipient Information & Customer Quick Pick
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
              text = "Customer Details",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )

            // Pick from existing files button
            if (availableFiles.isNotEmpty()) {
              ExposedDropdownMenuBox(
                expanded = selectedFilePickerExpanded,
                onExpandedChange = { selectedFilePickerExpanded = !selectedFilePickerExpanded }
              ) {
                OutlinedButton(
                  onClick = { selectedFilePickerExpanded = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.menuAnchor()
                ) {
                  Text("Pick from Files", fontSize = 11.sp)
                }
                ExposedDropdownMenu(
                  expanded = selectedFilePickerExpanded,
                  onDismissRequest = { selectedFilePickerExpanded = false }
                ) {
                  availableFiles.take(15).forEach { f ->
                    DropdownMenuItem(
                      text = { Text("${f.customerName} (${f.mobile})", fontSize = 12.sp) },
                      onClick = {
                        customerName = f.customerName
                        customerMobile = f.mobile
                        selectedFilePickerExpanded = false
                      }
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name *") },
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
              label = { Text("Customer Segment") },
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

          // Product Dropdown
          ExposedDropdownMenuBox(
            expanded = productExpanded,
            onExpandedChange = { productExpanded = !productExpanded }
          ) {
            OutlinedTextField(
              value = selectedProduct,
              onValueChange = {},
              readOnly = true,
              label = { Text("Applying Product") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
            )
            ExposedDropdownMenu(
              expanded = productExpanded,
              onDismissRequest = { productExpanded = false }
            ) {
              productOptions.forEach { prod ->
                DropdownMenuItem(
                  text = { Text(prod, fontSize = 13.sp) },
                  onClick = {
                    selectedProduct = prod
                    productExpanded = false
                  }
                )
              }
            }
          }
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Required Documents (${checklistItems.count { it.isChecked }}/${checklistItems.size})",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
            Text(
              text = "Check/uncheck items",
              fontSize = 11.sp,
              color = Color.Gray
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          checklistItems.forEachIndexed { index, item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  checklistItems[index] = item.copy(isChecked = !item.isChecked)
                }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = item.isChecked,
                onCheckedChange = { checked ->
                  checklistItems[index] = item.copy(isChecked = checked)
                },
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
              IconButton(
                onClick = { checklistItems.removeAt(index) },
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
                  checklistItems.add(
                    ChecklistItem(
                      id = checklistItems.size + 1,
                      title = newCustomDocText.trim(),
                      isChecked = true
                    )
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
          Text(
            text = "Dispatched Message Preview",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )

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
}
