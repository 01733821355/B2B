package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricHelper
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.appSettings.collectAsState()
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val productSetting = settings.find { it.settingKey == "PRODUCT_TYPES" }?.settingValue
    ?: "Credit Card,B2B,Corporate Card,Split,Limit Enhancement"

  val pendingDocsSetting = settings.find { it.settingKey == "PENDING_DOCS_LIST" }?.settingValue
    ?: "NID,TIN,Office ID,Salary Certificate,BS (Bank Statement),BIN,Trade License 2024-25,Trade License 2025-26,Trade License 2026-27,Loan Certificate,Card Statement (Month),Card Copy"

  val weekStartSetting = settings.find { it.settingKey == "WEEK_START_DAY" }?.settingValue ?: "SATURDAY"

  var productsInput by remember(productSetting) { mutableStateOf(productSetting) }
  var pendingDocsInput by remember(pendingDocsSetting) { mutableStateOf(pendingDocsSetting) }
  var weekStartInput by remember(weekStartSetting) { mutableStateOf(weekStartSetting) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("app_settings_screen")
  ) {
    // Header
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = EblNavyDark)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "System Configuration & Dropdowns",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Manage global lists, reporting week rules, and constants",
            fontSize = 11.sp,
            color = Color.LightGray
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Product Types
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Product Types (Comma Separated)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Options available to RMs in the customer file entry form", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        com.example.ui.common.VoiceInputField(
          value = productsInput,
          onValueChange = { productsInput = it },
          label = "Product Types",
          singleLine = false,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_setting_products"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { viewModel.updateSetting("PRODUCT_TYPES", productsInput.trim()) },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save Products")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Pending Documents List
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Pending Documents Checklist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Checklist items available during customer file entry", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        com.example.ui.common.VoiceInputField(
          value = pendingDocsInput,
          onValueChange = { pendingDocsInput = it },
          label = "Documents Checklist",
          singleLine = false,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_setting_pending_docs"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = {
            viewModel.updateSetting("PENDING_DOCS_LIST", pendingDocsInput.trim())
            Toast.makeText(context, "Pending documents checklist saved & synced!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save Documents")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Checklist Sender Items Management
    val defaultChecklistPresets = listOf(
      Triple("PENDING_DOCS_LIST", "Pending Docs Checklist", "NID\nTIN\nOffice ID\nSalary Certificate\nAccount Statement (6 Months)\nBIN\nTrade License 2024-25\nTrade License 2025-26\nTrade License 2026-27\nLoan Certificate\nCard Statement (Month)\nCard Copy"),
      Triple("CHECKLIST_ENHANCE_DOCS", "Limit Enhancement Docs", "Front & back photocopy of existing Credit Card\nLatest 6-month Salary / Business Bank Account Statement (sealed)\nLatest Salary Certificate / Original Pay Slips / Trade License renewal copy\nLatest E-TIN Certificate & Tax Return Assessment Acknowledgement Slip\nPhotocopy of National ID Card (NID) / Smart Card"),
      Triple("CHECKLIST_CORP_COMPANY_DOCS", "Corporate Card (Company)", "Valid Trade License (last 3-5 years renewal copies)\nMemorandum & Articles of Association (MOA & AOA) / Partnership Deed\nBoard Resolution authorizing Corporate Card facility & authorized signatories\nLatest 2 consecutive years Audited Financial Statements & Balance Sheet\nForm XII / Schedule X / List of Directors certified copy\nCompany E-TIN Certificate & BIN / VAT Registration Certificate\n12-Month Company Bank Account Statement (with bank seal & signature)"),
      Triple("CHECKLIST_CORP_EMPLOYEE_DOCS", "Corporate Card (Employee)", "Applicant Employee NID / Smart Card / Valid Passport photocopy\nEmployee Office ID Card photocopy & Business Visiting Card\nLetter of Introduction (LOI) / Corporate Card authorization on official company letterhead\n2 copies recent Passport size lab-print photographs of applicant\nApplicant Employee E-TIN Certificate photocopy\nLatest 6-Month Salary Account Bank Statement"),
      Triple("CHECKLIST_SALARIED_DOCS", "Salaried Credit Card", "NID / Smart Card / Valid Passport photocopy\n2 copies recent Passport size photographs\nLatest E-TIN Certificate & Tax Return Acknowledgment Slip\nLatest Salary Certificate / Original Pay Slips (last 3 months)\n6-month Salary Account Statement (with bank seal & signature)\nOffice ID Card photocopy & Visiting Card\nUtility Bill photocopy (Electricity / WASA / Gas residence)"),
      Triple("CHECKLIST_BUSINESS_DOCS", "Business Person Credit Card", "National ID Card (NID) photocopy\n2 copies recent Passport size photographs\nValid Trade License (last 3-5 years renewal copies)\n12-month Business & Personal Bank Account Statement (sealed)\nLatest E-TIN Certificate & Tax Return Acknowledgment Slip\nVisiting card & Memorandum of Association / Partnership Deed\nUtility bill of residence & business premises"),
      Triple("CHECKLIST_LOAN_DOCS", "Loan Application Docs", "National ID Card (NID) photocopy\n2 copies recent Passport size photographs\nLatest E-TIN Certificate & Tax Return Assessment Slip\nIncome proof (Salary Certificate / 6-12 month Bank Statement)\nOffice ID / Trade License photocopy\nUtility Bill photocopy (residence)"),
      Triple("CHECKLIST_HEADER_TEMPLATE", "Checklist Header Greeting", "Dear {CUSTOMER_NAME},\nGreetings from Eastern Bank PLC (EBL).\nTo process your application for {PRESET_NAME}, please provide the following required documents:"),
      Triple("CHECKLIST_REGARDS_TEMPLATE", "Checklist Footer / Regards", "For any query or assistance, please contact:\n{RM_NAME}\nRM Code: {RM_CODE}\nMobile: {RM_PHONE}\nEastern Bank PLC")
    )

    // Gather any additional custom checklist items stored in Universal Settings
    val customChecklistPresets = remember(settings) {
      settings.filter { it.settingKey.startsWith("CHECKLIST_CUSTOM_") }.map {
        Triple(it.settingKey, it.settingKey.removePrefix("CHECKLIST_CUSTOM_").replace("_", " "), it.settingValue)
      }
    }
    val checklistPresets = defaultChecklistPresets + customChecklistPresets

    var selectedPresetKey by remember { mutableStateOf(checklistPresets[0].first) }
    val currentPresetObj = checklistPresets.find { it.first == selectedPresetKey } ?: checklistPresets[0]
    val savedPresetVal = settings.find { it.settingKey == selectedPresetKey }?.settingValue ?: currentPresetObj.third
    var checklistPresetInput by remember(selectedPresetKey, savedPresetVal) { mutableStateOf(savedPresetVal) }

    var showAddNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryKeyInput by remember { mutableStateOf("") }
    var newCategoryTitleInput by remember { mutableStateOf("") }
    var newCategoryItemsInput by remember { mutableStateOf("") }

    Card(
      modifier = Modifier.fillMaxWidth().testTag("card_checklist_sender_settings"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Checklist Sender & Pending Docs Items (চেকলিস্ট সেন্ডার ও পেন্ডিং ডক্স)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
        }
        Text("পেন্ডিং ডক্স এবং চেকলিস্ট সেন্ডারের প্রতিটি অপশনের আইটেম অ্যাড বা এডিট করুন। সেভ করলে গুগল শিটের Universal_Settings ট্যাবে নতুন রো হিসেবে স্বয়ংক্রিয়ভাবে লোড হবে।", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))

        // Preset selector chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Select Category / Docs to Edit:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
          androidx.compose.material3.TextButton(
            onClick = { showAddNewCategoryDialog = true },
            modifier = Modifier.testTag("btn_add_new_checklist_category")
          ) {
            Text("+ Add New Category / Row", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          checklistPresets.forEach { preset ->
            val isSelected = preset.first == selectedPresetKey
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) EblNavyPrimary else Color(0xFFF1F5F9),
              border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
              modifier = Modifier
                .clickable { selectedPresetKey = preset.first }
                .padding(vertical = 2.dp)
            ) {
              Text(
                text = preset.second,
                color = if (isSelected) Color.White else Color.DarkGray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Items for '${currentPresetObj.second}' (Key: ${currentPresetObj.first}):",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        com.example.ui.common.VoiceInputField(
          value = checklistPresetInput,
          onValueChange = { checklistPresetInput = it },
          label = "${currentPresetObj.second} Items",
          singleLine = false,
          maxLines = 8,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_setting_checklist_preset"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Total ${checklistPresetInput.lines().count { it.isNotBlank() }} items configured",
            fontSize = 11.sp,
            color = Color.Gray
          )
          Button(
            onClick = {
              val cleanVal = checklistPresetInput.trim()
              viewModel.updateSetting(selectedPresetKey, cleanVal)
              // If PENDING_DOCS_LIST was updated, keep pendingDocsInput in sync
              if (selectedPresetKey == "PENDING_DOCS_LIST") {
                pendingDocsInput = cleanVal
              }
              Toast.makeText(context, "'${currentPresetObj.second}' saved & synced as a row in Universal Settings tab!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            modifier = Modifier.testTag("btn_save_checklist_preset")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save & Sync to Sheet")
          }
        }
      }
    }

    // Add New Custom Checklist Category Dialog
    if (showAddNewCategoryDialog) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showAddNewCategoryDialog = false },
        title = { Text("Add New Checklist / Universal Setting Row", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("একটি নতুন চেকলিস্ট অপশন যোগ করুন। এটি সেভ করলে গুগল শিটের Universal_Settings ট্যাবে নতুন একটি রো হিসেবে লোড হবে।", fontSize = 12.sp, color = Color.Gray)
            OutlinedTextField(
              value = newCategoryTitleInput,
              onValueChange = { newCategoryTitleInput = it },
              label = { Text("Category Title (যেমন: Special Loan Docs)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = newCategoryKeyInput,
              onValueChange = { newCategoryKeyInput = it },
              label = { Text("Setting Key (যেমন: CHECKLIST_CUSTOM_SPECIAL_DOCS)") },
              placeholder = { Text("CHECKLIST_CUSTOM_...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            com.example.ui.common.VoiceInputField(
              value = newCategoryItemsInput,
              onValueChange = { newCategoryItemsInput = it },
              label = "Item List (One per line)",
              singleLine = false,
              maxLines = 6,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val key = if (newCategoryKeyInput.isNotBlank()) {
                newCategoryKeyInput.trim().uppercase()
              } else {
                "CHECKLIST_CUSTOM_" + newCategoryTitleInput.trim().replace("\\s+".toRegex(), "_").uppercase()
              }
              if (key.isNotBlank() && newCategoryItemsInput.isNotBlank()) {
                viewModel.updateSetting(key, newCategoryItemsInput.trim())
                selectedPresetKey = key
                showAddNewCategoryDialog = false
                newCategoryKeyInput = ""
                newCategoryTitleInput = ""
                newCategoryItemsInput = ""
                Toast.makeText(context, "New category added and loaded into Universal Settings tab!", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
          ) {
            Text("Add & Sync to Sheet")
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showAddNewCategoryDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // UI Appearance, Theme Color & Typography
    val currentColorTheme = settings.find { it.settingKey == "UI_COLOR_THEME" }?.settingValue ?: "ROYAL_NAVY"
    val currentFontStyle = settings.find { it.settingKey == "UI_FONT_STYLE" }?.settingValue ?: "DEFAULT_SANS"
    val currentTextScale = settings.find { it.settingKey == "UI_TEXT_SCALE" }?.settingValue ?: "STANDARD"

    var selectedColorTheme by remember(currentColorTheme) { mutableStateOf(currentColorTheme) }
    var selectedFontStyle by remember(currentFontStyle) { mutableStateOf(currentFontStyle) }
    var selectedTextScale by remember(currentTextScale) { mutableStateOf(currentTextScale) }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("UI Appearance & Font Styling (রং ও ফন্ট শৈলী)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
        }
        Text("Customize primary theme colors, typography styling, and reading size", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Primary Theme Color Palette
        Text("Primary Brand Color Palette:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val palettes = listOf(
          Triple("ROYAL_NAVY", "Royal Navy", Color(0xFF0A192F)),
          Triple("EMERALD", "Emerald Green", Color(0xFF065F46)),
          Triple("CRIMSON", "Deep Crimson", Color(0xFF9F1239)),
          Triple("MIDNIGHT", "Midnight Slate", Color(0xFF0F172A)),
          Triple("SAPPHIRE", "Sapphire Indigo", Color(0xFF312E81))
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          palettes.forEach { (key, label, color) ->
            val isSelected = selectedColorTheme == key
            androidx.compose.material3.Surface(
              onClick = {
                selectedColorTheme = key
                viewModel.updateSetting("UI_COLOR_THEME", key)
              },
              shape = RoundedCornerShape(8.dp),
              color = color,
              border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFDE047)) else null,
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (isSelected) {
                  Icon(Icons.Default.Save, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Active Theme: ${palettes.find { it.first == selectedColorTheme }?.second ?: "Royal Navy"}",
          fontSize = 11.sp,
          color = EblNavyPrimary,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Font Style / Typography Preset
        Text("Typography & Font Style:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val fontOptions = listOf(
          Pair("DEFAULT_SANS", "Clean Modern Sans"),
          Pair("EXECUTIVE_SERIF", "Executive Formal"),
          Pair("TECHNICAL_MONO", "Technical Dense")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          fontOptions.forEach { (key, label) ->
            val isSelected = selectedFontStyle == key
            androidx.compose.material3.FilterChip(
              selected = isSelected,
              onClick = {
                selectedFontStyle = key
                viewModel.updateSetting("UI_FONT_STYLE", key)
              },
              label = { Text(label, fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. UI Text Scale / Font Size
        Text("UI Text Reading Scale:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val scaleOptions = listOf(
          Pair("COMPACT", "Compact (90%)"),
          Pair("STANDARD", "Standard (100%)"),
          Pair("COMFORTABLE", "Comfortable (115%)")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          scaleOptions.forEach { (key, label) ->
            val isSelected = selectedTextScale == key
            androidx.compose.material3.FilterChip(
              selected = isSelected,
              onClick = {
                selectedTextScale = key
                viewModel.updateSetting("UI_TEXT_SCALE", key)
              },
              label = { Text(label, fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Week Start & Timezone
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Reporting Week Rule & Timezone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        com.example.ui.common.VoiceInputField(
          value = weekStartInput,
          onValueChange = { weekStartInput = it },
          label = "Week Start Day (e.g. SATURDAY)",
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_setting_week_start"
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = "Asia/Dhaka (GMT+06:00)",
          onValueChange = {},
          readOnly = true,
          label = { Text("Reporting Timezone") },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { viewModel.updateSetting("WEEK_START_DAY", weekStartInput.trim().uppercase()) },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save Week Rule")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Biometric & Fingerprint Security Card (Interactive Activation Toggle)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val biometricAvailability = BiometricHelper.checkBiometricAvailability(context)
    val isBiometricActive = viewModel.isBiometricEnabled(currentUser.rmCode)
    val isPasswordVerified = viewModel.isPasswordLoginVerified(currentUser.rmCode)

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Fingerprint Sign In (ফিঙ্গারপ্রিন্ট লগইন)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
              Text("RM: ${currentUser.rmCode} (${currentUser.name})", fontSize = 11.sp, color = Color.Gray)
            }
          }

          Switch(
            checked = isBiometricActive,
            onCheckedChange = { enable ->
              if (enable) {
                if (!isPasswordVerified) {
                  Toast.makeText(
                    context,
                    "অনুগ্রহ করে প্রথমে আপনার পাসওয়ার্ড দিয়ে লগইন করুন, তারপর ফিঙ্গারপ্রিন্ট চালু করুন।",
                    Toast.LENGTH_LONG
                  ).show()
                  return@Switch
                }

                if (biometricAvailability != BiometricHelper.BiometricAvailability.AVAILABLE) {
                  Toast.makeText(
                    context,
                    "ডিভাইসে ফিঙ্গারপ্রিন্ট সেন্সর সক্রিয় নেই বা ফিঙ্গারপ্রিন্ট সেটআপ করা হয়নি।",
                    Toast.LENGTH_LONG
                  ).show()
                  return@Switch
                }

                val activity = generateSequence(context) { ctx ->
                  if (ctx is ContextWrapper) ctx.baseContext else null
                }.filterIsInstance<FragmentActivity>().firstOrNull()

                if (activity == null) {
                  Toast.makeText(context, "Cannot open biometric prompt.", Toast.LENGTH_SHORT).show()
                  return@Switch
                }

                BiometricHelper.promptBiometricLogin(
                  activity = activity,
                  title = "Activate Fingerprint Login",
                  subtitle = "RM ${currentUser.rmCode}",
                  description = "Touch the fingerprint sensor to confirm enrollment",
                  negativeButtonText = "Cancel",
                  onSuccess = {
                    viewModel.setBiometricEnabled(currentUser.rmCode, true) { success, _ ->
                      if (success) {
                        Toast.makeText(context, "✓ ফিঙ্গারপ্রিন্ট লগইন সফলভাবে সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
                      }
                    }
                  },
                  onError = { err ->
                    Toast.makeText(context, "Biometric verification failed: $err", Toast.LENGTH_SHORT).show()
                  }
                )
              } else {
                viewModel.setBiometricEnabled(currentUser.rmCode, false) { success, _ ->
                  if (success) {
                    Toast.makeText(context, "ফিঙ্গারপ্রিন্ট লগইন নিষ্ক্রিয় করা হয়েছে।", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFF059669)
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isBiometricActive) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (isBiometricActive) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isBiometricActive) Icons.Default.Fingerprint else Icons.Default.Settings,
              contentDescription = null,
              tint = if (isBiometricActive) Color(0xFF059669) else Color(0xFFD97706),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isBiometricActive) {
                "✓ Fingerprint Sign In is ACTIVE for RM ${currentUser.rmCode}. You can now unlock the app using your fingerprint directly from login screen!"
              } else {
                "নিয়ম: প্রথমে পাসওয়ার্ড দিয়ে একবার লগইন করতে হবে। এরপর সেটিংস থেকে এই টগলটি অন করলে ফিঙ্গারপ্রিন্ট দিয়ে ওয়ান-টাচ লগইন করা যাবে।"
              },
              fontSize = 11.sp,
              color = if (isBiometricActive) Color(0xFF065F46) else Color(0xFF92400E)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = when (biometricAvailability) {
            BiometricHelper.BiometricAvailability.AVAILABLE -> "• Hardware Status: Fingerprint sensor is ready on this phone."
            BiometricHelper.BiometricAvailability.NONE_ENROLLED -> "• Hardware Alert: No fingerprint enrolled in Android phone settings."
            BiometricHelper.BiometricAvailability.NO_HARDWARE -> "• Hardware Alert: Fingerprint scanner hardware is not available on this device."
            else -> "• Hardware Status: Sensor unavailable."
          },
          fontSize = 11.sp,
          color = if (biometricAvailability == BiometricHelper.BiometricAvailability.AVAILABLE) Color.Gray else Color(0xFFDC2626)
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}
