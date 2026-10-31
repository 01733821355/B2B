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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.ui.theme.AppThemeId
import com.example.ui.theme.AppThemeConfig
import com.example.ui.theme.ThemeManager
import com.example.util.NotificationHelper
import com.example.util.BackgroundSyncWorker
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricHelper
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

  val fileStatusesSetting = settings.find { it.settingKey == "FILE_STATUSES_LIST" }?.settingValue
    ?: "Collected, Submitted, Analyst Receive, Approved, Declined, Query, Return to Source, Condition, STC"

  var productsInput by remember(productSetting) { mutableStateOf(productSetting) }
  var pendingDocsInput by remember(pendingDocsSetting) { mutableStateOf(pendingDocsSetting) }
  var weekStartInput by remember(weekStartSetting) { mutableStateOf(weekStartSetting) }
  var fileStatusesInput by remember(fileStatusesSetting) { mutableStateOf(fileStatusesSetting) }

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

    // Device Notifications & Background Sync Card (Ensures App-Off delivery works)
    val areNotifsEnabled = remember { NotificationHelper.areNotificationsEnabled(context) }
    Card(
      modifier = Modifier.fillMaxWidth().testTag("card_notification_settings"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (areNotifsEnabled) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
      ),
      border = BorderStroke(1.dp, if (areNotifsEnabled) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (areNotifsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
            contentDescription = null,
            tint = if (areNotifsEnabled) Color(0xFF16A34A) else Color(0xFFDC2626),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "Mobile Notification Status (App Off Mode)",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = if (areNotifsEnabled) Color(0xFF15803D) else Color(0xFFB91C1C)
            )
            Text(
              if (areNotifsEnabled)
                "✓ সক্রিয়: অ্যাপ বন্ধ থাকলেও এসএমএস, নতুন ফাইল ও ইভেন্ট নোটিফিকেশন মোবাইলে রিং ও ভাইব্রেট করবে।"
              else
                "✕ বন্ধ: আপনার ডিভাইসে নোটিফিকেশন অফ আছে। অনুগ্রহ করে সেটিংসে গিয়ে পারমিশন চালু করুন।",
              fontSize = 11.sp,
              color = if (areNotifsEnabled) Color(0xFF166534) else Color(0xFF991B1B)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                  putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
              } catch (_: Exception) {
                try {
                  val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                  }
                  context.startActivity(intent)
                } catch (_: Exception) {
                  Toast.makeText(context, "Please open device Settings -> Apps -> Notifications", Toast.LENGTH_LONG).show()
                }
              }
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("App Settings", fontSize = 12.sp)
          }

          Button(
            onClick = {
              NotificationHelper.createNotificationChannels(context)
              NotificationHelper.sendIncomingSmsNotification(
                context = context,
                senderTitle = "System Test Alert",
                messageText = "Notification is working properly! Alerts will arrive even when the app is closed."
              )
              BackgroundSyncWorker.triggerImmediateSync(context)
              BackgroundSyncWorker.schedulePeriodicSync(context)
              Toast.makeText(context, "Test notification triggered! Check your notification bar.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (areNotifsEnabled) Color(0xFF16A34A) else EblNavyPrimary),
            modifier = Modifier.weight(1f).testTag("btn_test_notification")
          ) {
            Text("Test Notification", fontSize = 12.sp)
          }
        }
      }
    }

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

    // File & Application Statuses (Admin Customizable)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("File & Application Status Categories (Comma Separated)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Admin can add or edit file statuses (e.g. Collected, Approved, Disbursed). Updates are visible to all RMs.", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        com.example.ui.common.VoiceInputField(
          value = fileStatusesInput,
          onValueChange = { fileStatusesInput = it },
          label = "Application Statuses",
          singleLine = false,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_setting_file_statuses"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Active Status Items (Tap ✕ to remove):", fontSize = 11.sp, color = EblNavyDark, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          fileStatusesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { st ->
            FilterChip(
              selected = true,
              onClick = {
                val remaining = fileStatusesInput.split(",").map { it.trim() }.filter { it.isNotBlank() && !it.equals(st, ignoreCase = true) }.joinToString(", ")
                fileStatusesInput = remaining
              },
              label = { Text("$st ✕", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EblNavyPrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Quick Status Suggestions (Tap to add):", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("Collected", "Approved", "Submitted", "Under Review", "Analyst Receive", "Declined", "Query", "Return to Source", "Condition", "STC", "Disbursed").forEach { suggestion ->
            val alreadyPresent = fileStatusesInput.split(",").any { it.trim().equals(suggestion, ignoreCase = true) }
            if (!alreadyPresent) {
              FilterChip(
                selected = false,
                onClick = {
                  fileStatusesInput = if (fileStatusesInput.isBlank()) suggestion else "$fileStatusesInput, $suggestion"
                },
                label = { Text("+ $suggestion", fontSize = 10.sp) }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = {
            viewModel.updateFileStatusOptions(fileStatusesInput.trim()) { success, err ->
              if (success) {
                Toast.makeText(context, "স্ট্যাটাস ক্যাটাগরি সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, err ?: "Error saving statuses", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.align(Alignment.End).testTag("btn_save_file_statuses")
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save & Sync Statuses to All RMs")
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
      Triple("CHECKLIST_HEADER_TEMPLATE", "Checklist Header Greeting", "Dear {CUSTOMER_NAME},\nGreetings from B2B Banking Suite.\nTo process your application for {PRESET_NAME}, please provide the following required documents:"),
      Triple("CHECKLIST_REGARDS_TEMPLATE", "Checklist Footer / Regards", "For any query or assistance, please contact:\n{RM_NAME}\nRM Code: {RM_CODE}\nMobile: {RM_PHONE}\nB2B Banking & Operations")
    )

    val nonPresetKeywords = setOf(
      "OFFICE", "LAT", "LNG", "RADIUS", "ADDRESS", "BANK", "APP_NAME", "APPLICATION_NAME",
      "COMPANY", "SUPPORT", "PHONE", "EMAIL", "URL", "TERMS", "POLICY", "PASSWORD", "SECRET",
      "TOMBSTONE", "DELETED", "SYNC", "MAINTENANCE", "VERSION", "PORT", "IP", "HOURS", "STATUS",
      "THEME", "SCALE", "FONT", "COLOR", "STYLE", "TEXT_SCALE", "COLOR_THEME", "ACTIVE_THEME", "CUSTOM_NAME"
    )

    fun normalizePresetName(s: String): String = s.lowercase().replace("[^a-z0-9]".toRegex(), "")

    val deletedPresetKeys = remember(settings) {
      settings.find { it.settingKey == "deleted_preset_keys" }
        ?.settingValue?.split(",")?.map { it.trim().uppercase() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    // Gather any additional custom checklist items stored in Universal Settings (excluding non-preset and deleted keys)
    val customChecklistPresets = remember(settings, deletedPresetKeys) {
      val defaultNormSet = defaultChecklistPresets.map { normalizePresetName(it.second) }.toSet()
      val defaultKeySet = defaultChecklistPresets.map { it.first.uppercase() }.toSet()

      val customListRaw = settings.find { it.settingKey == "CHECKLIST_CUSTOM_PRESETS_LIST" }?.settingValue ?: ""
      val customNames = if (customListRaw.startsWith("[")) {
        try {
          val arr = org.json.JSONArray(customListRaw)
          (0 until arr.length()).map { arr.getString(it).trim().trim('"', '[', ']') }.filter { it.isNotBlank() }
        } catch (_: Exception) {
          customListRaw.split(",").map { it.trim().trim('"', '[', ']') }.filter { it.isNotBlank() }
        }
      } else {
        customListRaw.split(",").map { it.trim().trim('"', '[', ']') }.filter { it.isNotBlank() }
      }.filter { name ->
        val upper = name.uppercase()
        val norm = normalizePresetName(name)
        !nonPresetKeywords.any { kw -> upper.contains(kw) } &&
        norm !in defaultNormSet && upper !in deletedPresetKeys
      }

      val presetsFromList = customNames.map { name ->
        val cleanKey = "CHECKLIST_CUSTOM_" + name.replace("\\s+".toRegex(), "_").uppercase()
        val legacyKey = "CHECKLIST_PRESET_" + name.replace("\\s+".toRegex(), "_").uppercase()
        val value = settings.find { it.settingKey == name || it.settingKey == cleanKey || it.settingKey == legacyKey }?.settingValue ?: ""
        Triple(cleanKey, name, value)
      }

      val presetsFromKeys = settings.filter {
        (it.settingKey.startsWith("CHECKLIST_CUSTOM_") || it.settingKey.startsWith("CHECKLIST_PRESET_")) &&
        it.settingKey != "CHECKLIST_CUSTOM_PRESETS_LIST" &&
        it.settingKey != "CHECKLIST_PRESETS_VISIBILITY" &&
        it.settingKey != "CHECKLIST_CORP_COMPANY_DOCS" &&
        it.settingKey != "CHECKLIST_CORP_EMPLOYEE_DOCS" &&
        it.settingKey != "CHECKLIST_SALARIED_DOCS" &&
        it.settingKey != "CHECKLIST_BUSINESS_DOCS" &&
        it.settingKey != "CHECKLIST_LOAN_DOCS" &&
        it.settingKey != "CHECKLIST_ENHANCE_DOCS" &&
        it.settingKey != "CHECKLIST_HEADER_TEMPLATE" &&
        it.settingKey != "CHECKLIST_REGARDS_TEMPLATE" &&
        it.settingKey.uppercase() !in defaultKeySet &&
        it.settingKey.uppercase() !in deletedPresetKeys &&
        !nonPresetKeywords.any { kw -> it.settingKey.uppercase().contains(kw) }
      }.map {
        val friendly = it.settingKey.removePrefix("CHECKLIST_CUSTOM_").removePrefix("CHECKLIST_PRESET_").replace("_", " ")
        Triple(it.settingKey, friendly, it.settingValue)
      }.filter {
        val norm = normalizePresetName(it.second)
        norm !in defaultNormSet && it.second.uppercase() !in deletedPresetKeys
      }

      (presetsFromList + presetsFromKeys).distinctBy { normalizePresetName(it.second) }
    }

    val checklistPresets = remember(defaultChecklistPresets, customChecklistPresets) {
      (defaultChecklistPresets + customChecklistPresets).distinctBy { normalizePresetName(it.second) }
    }

    val visibilitySetting = settings.find { it.settingKey == "CHECKLIST_PRESETS_VISIBILITY" }?.settingValue.orEmpty()
    val visibilityMap = remember(visibilitySetting) {
      if (visibilitySetting.startsWith("{")) {
        try {
          val json = org.json.JSONObject(visibilitySetting)
          val map = mutableMapOf<String, Boolean>()
          json.keys().forEach { k ->
            map[k] = json.optBoolean(k, true)
            map[k.uppercase()] = json.optBoolean(k, true)
          }
          map
        } catch (_: Exception) {
          emptyMap<String, Boolean>()
        }
      } else emptyMap()
    }

    fun getPresetAliases(key: String, title: String): List<String> {
      val aliases = mutableListOf(key, title, key.uppercase(), title.uppercase())
      val cleanKey = "CHECKLIST_CUSTOM_" + title.replace("\\s+".toRegex(), "_").uppercase()
      val legacyKey = "CHECKLIST_PRESET_" + title.replace("\\s+".toRegex(), "_").uppercase()
      aliases.add(cleanKey)
      aliases.add(legacyKey)
      val norm = normalizePresetName(title)
      if (norm.contains("enhance") || norm.contains("limit")) {
        aliases.addAll(listOf("CHECKLIST_ENHANCE_DOCS", "Limit Enhancement Docs", "Credit Card Limit Enhance", "Credit Card Limit Enhancement", "CHECKLIST_CUSTOM_CREDIT_CARD_LIMIT_ENHANCE", "CHECKLIST_PRESET_CREDIT_CARD_LIMIT_ENHANCE"))
      } else if (norm.contains("corporate") || norm.contains("corp")) {
        aliases.addAll(listOf("Corporate Card", "CHECKLIST_CORP_COMPANY_DOCS", "CHECKLIST_CORP_EMPLOYEE_DOCS", "Corporate Card (Company)", "Corporate Card (Employee)", "CORPORATE CARD EMPLOYEE", "CHECKLIST_CUSTOM_CORPORATE_CARD"))
      } else if (norm.contains("salaried")) {
        aliases.addAll(listOf("CHECKLIST_SALARIED_DOCS", "Salaried Credit Card", "New Credit Card (Salaried)", "NEW CREDIT CARD SALARIED", "NEW CREDIT CARD (SALARIED)", "CHECKLIST_CUSTOM_NEW_CREDIT_CARD_SALARIED", "CHECKLIST_PRESET_NEW_CREDIT_CARD_SALARIED"))
      } else if (norm.contains("business")) {
        aliases.addAll(listOf("CHECKLIST_BUSINESS_DOCS", "Business Person Credit Card", "New Credit Card (Business Person)", "CHECKLIST_CUSTOM_NEW_CREDIT_CARD_BUSINESS_PERSON"))
      } else if (norm.contains("loan")) {
        aliases.addAll(listOf("CHECKLIST_LOAN_DOCS", "Loan Application Docs", "Personal / Auto / Home Loan", "CHECKLIST_CUSTOM_PERSONAL_AUTO_HOME_LOAN"))
      } else if (norm.contains("pending")) {
        aliases.addAll(listOf("PENDING_DOCS_LIST", "Pending Docs Checklist", "CHECKLIST_CUSTOM_PENDING_DOCS_CHECKLIST"))
      }
      return aliases.distinct()
    }

    fun isPresetVisibleToAll(key: String, title: String): Boolean {
      val aliases = getPresetAliases(key, title)
      for (a in aliases) {
        if (visibilityMap[a] == false || visibilityMap[a.uppercase()] == false) return false
      }
      return true
    }

    fun setPresetVisibility(key: String, title: String, isVisible: Boolean) {
      val json = try {
        if (visibilitySetting.startsWith("{")) org.json.JSONObject(visibilitySetting) else org.json.JSONObject()
      } catch (_: Exception) {
        org.json.JSONObject()
      }
      val aliases = getPresetAliases(key, title)
      for (a in aliases) {
        json.put(a, isVisible)
        json.put(a.uppercase(), isVisible)
      }
      val newStr = json.toString()
      viewModel.updateSetting("CHECKLIST_PRESETS_VISIBILITY", newStr)
    }

    var showVisibilityMatrixDialog by remember { mutableStateOf(false) }

    var selectedPresetKey by remember { mutableStateOf(checklistPresets[0].first) }
    val currentPresetObj = checklistPresets.find { it.first == selectedPresetKey } ?: checklistPresets[0]
    val savedPresetVal = settings.find { it.settingKey == selectedPresetKey }?.settingValue ?: currentPresetObj.third
    var checklistPresetInput by remember(selectedPresetKey, savedPresetVal) { mutableStateOf(savedPresetVal) }

    var showAddNewCategoryDialog by remember { mutableStateOf(false) }
    var showDeletePresetDialog by remember { mutableStateOf(false) }
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
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.TextButton(
              onClick = { showVisibilityMatrixDialog = true },
              modifier = Modifier.testTag("btn_open_visibility_matrix")
            ) {
              Text("👁️ দৃশ্যমানতা তালিকা", fontSize = 11.sp, color = Color(0xFF0D9488), fontWeight = FontWeight.Bold)
            }
            androidx.compose.material3.TextButton(
              onClick = { showAddNewCategoryDialog = true },
              modifier = Modifier.testTag("btn_add_new_checklist_category")
            ) {
              Text("+ Add Category", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Bold)
            }
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
            val isVis = isPresetVisibleToAll(preset.first, preset.second)
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) EblNavyPrimary else (if (isVis) Color(0xFFF1F5F9) else Color(0xFFFEF3C7)),
              border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, if (isVis) Color(0xFFCBD5E1) else Color(0xFFF59E0B)),
              modifier = Modifier
                .clickable { selectedPresetKey = preset.first }
                .padding(vertical = 2.dp)
            ) {
              Text(
                text = (if (isVis) "🟢 " else "🔒 ") + preset.second,
                color = if (isSelected) Color.White else (if (isVis) Color.DarkGray else Color(0xFF92400E)),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Visibility Control Card (Requirement: kono kon preset sobai dekhte parbe ta set korar babostha)
        val currentIsVisibleToAll = isPresetVisibleToAll(currentPresetObj.first, currentPresetObj.second)
        Card(
          modifier = Modifier.fillMaxWidth().testTag("card_preset_visibility_control"),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (currentIsVisibleToAll) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (currentIsVisibleToAll) Color(0xFF86EFAC) else Color(0xFFFDE68A)
          )
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  if (currentIsVisibleToAll) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = null,
                  tint = if (currentIsVisibleToAll) Color(0xFF16A34A) else Color(0xFFD97706),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (currentIsVisibleToAll) "সবাই দেখতে পারবে (Visible to All RMs)" else "শুধুমাত্র অ্যাডমিন ও মেন্টর (Admin & Mentor Only)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (currentIsVisibleToAll) Color(0xFF15803D) else Color(0xFFB45309)
                )
              }
              Text(
                text = if (currentIsVisibleToAll) "সাধারণ সকল RM চেকলিস্ট সেন্ডারে '${currentPresetObj.second}' দেখতে পাবে।"
                       else "সাধারণ RM-রা এই প্রিসেট দেখতে পাবে না, কেবল অ্যাডমিন ও মেন্টরদের তালিকায় থাকবে।",
                fontSize = 10.sp,
                color = Color.DarkGray
              )
            }
            Switch(
              checked = currentIsVisibleToAll,
              onCheckedChange = { checked ->
                setPresetVisibility(currentPresetObj.first, currentPresetObj.second, checked)
                Toast.makeText(context, if (checked) "✓ '${currentPresetObj.second}' এখন সবাই দেখতে পারবে" else "🔒 '${currentPresetObj.second}' এখন শুধুমাত্র অ্যাডমিন ও মেন্টর দেখতে পারবে", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.testTag("switch_preset_visibility")
            )
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
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val corePresetKeys = setOf(
              "PENDING_DOCS_LIST", "CHECKLIST_ENHANCE_DOCS", "CHECKLIST_CORP_COMPANY_DOCS",
              "CHECKLIST_CORP_EMPLOYEE_DOCS", "CHECKLIST_SALARIED_DOCS", "CHECKLIST_BUSINESS_DOCS",
              "CHECKLIST_LOAN_DOCS", "CHECKLIST_HEADER_TEMPLATE", "CHECKLIST_REGARDS_TEMPLATE"
            )
            val canDeleteCategory = selectedPresetKey !in corePresetKeys
            if (canDeleteCategory) {
              androidx.compose.material3.OutlinedButton(
                onClick = { showDeletePresetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                modifier = Modifier.testTag("btn_delete_checklist_preset")
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Category", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
              }
            }
            Button(
              onClick = {
                val cleanVal = checklistPresetInput.trim()
                if (selectedPresetKey == "CHECKLIST_HEADER_TEMPLATE") {
                  viewModel.updateSetting("Checklist Header", cleanVal)
                  viewModel.updateSetting("CHECKLIST_HEADER_TEMPLATE", cleanVal)
                } else if (selectedPresetKey == "CHECKLIST_REGARDS_TEMPLATE") {
                  viewModel.updateSetting("Checklist Footer", cleanVal)
                  viewModel.updateSetting("CHECKLIST_REGARDS_TEMPLATE", cleanVal)
                } else {
                  viewModel.savePresetRow(currentPresetObj.second, cleanVal)
                  viewModel.updateSetting(selectedPresetKey, cleanVal)
                }
                // If PENDING_DOCS_LIST was updated, keep pendingDocsInput in sync
                if (selectedPresetKey == "PENDING_DOCS_LIST") {
                  pendingDocsInput = cleanVal
                }
                Toast.makeText(context, "'${currentPresetObj.second}' saved & synced as row in Universal Settings!", Toast.LENGTH_SHORT).show()
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
    }

    // Delete Custom Preset Confirmation Dialog
    if (showDeletePresetDialog) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showDeletePresetDialog = false },
        title = { Text("Delete Category / Row?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
        text = {
          Text("আপনি কি নিশ্চিত যে '${currentPresetObj.second}' ক্যাটাগরি এবং এর সমস্ত আইটেম অ্যাপ এবং গুগল শিটের Universal Settings ট্যাব থেকে সম্পূর্ণ মুছে ফেলতে চান?")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deletePresetRow(currentPresetObj.second, selectedPresetKey) { success, _ ->
                if (success) {
                  selectedPresetKey = checklistPresets[0].first
                  showDeletePresetDialog = false
                  Toast.makeText(context, "Category deleted successfully!", Toast.LENGTH_SHORT).show()
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
          ) {
            Text("Delete Permanently")
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showDeletePresetDialog = false }) {
            Text("Cancel")
          }
        }
      )
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
              val presetTitle = newCategoryTitleInput.trim()
              val key = if (newCategoryKeyInput.isNotBlank()) {
                newCategoryKeyInput.trim().uppercase()
              } else {
                "CHECKLIST_CUSTOM_" + presetTitle.replace("\\s+".toRegex(), "_").uppercase()
              }
              if (presetTitle.isNotBlank() && newCategoryItemsInput.isNotBlank()) {
                val itemsStr = newCategoryItemsInput.trim()
                viewModel.savePresetRow(presetTitle, itemsStr)
                viewModel.updateSetting(key, itemsStr)
                selectedPresetKey = key
                showAddNewCategoryDialog = false
                newCategoryKeyInput = ""
                newCategoryTitleInput = ""
                newCategoryItemsInput = ""
                Toast.makeText(context, "New preset '$presetTitle' added as row in Universal Settings!", Toast.LENGTH_SHORT).show()
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

    // Preset Visibility Matrix Dialog (Requirement: kono kon preset sobai dekhte parbe ta set korar babostha)
    if (showVisibilityMatrixDialog) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showVisibilityMatrixDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("প্রিসেট দৃশ্যমানতা পারমিশন (Preset Visibility)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EblNavyDark)
          }
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
              "এখানে প্রতিটি প্রিসেটের পাশে সুইচ অন/অফ করে ঠিক করুন কোন কোন প্রিসেট সাধারণ RM-রা চেকলিস্ট সেন্ডারে দেখতে পাবে আর কোনটি শুধুমাত্র অ্যাডমিন ও মেন্টরদের তালিকায় থাকবে।",
              fontSize = 12.sp,
              color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            checklistPresets.forEach { preset ->
              val isVis = isPresetVisibleToAll(preset.first, preset.second)
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isVis) Color(0xFFF8FAFC) else Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isVis) Color(0xFFE2E8F0) else Color(0xFFFDE68A))
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                      text = (if (isVis) "🟢 " else "🔒 ") + preset.second,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isVis) EblNavyDark else Color(0xFF92400E)
                    )
                    Text(
                      text = if (isVis) "সকল RM দেখতে পারবে" else "শুধুমাত্র অ্যাডমিন ও মেন্টর",
                      fontSize = 11.sp,
                      color = if (isVis) Color(0xFF16A34A) else Color(0xFFD97706),
                      fontWeight = FontWeight.Medium
                    )
                  }
                  Switch(
                    checked = isVis,
                    onCheckedChange = { checked ->
                      setPresetVisibility(preset.first, preset.second, checked)
                    },
                    modifier = Modifier.testTag("switch_matrix_${preset.first}")
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showVisibilityMatrixDialog = false
              Toast.makeText(context, "প্রিসেট দৃশ্যমানতা আপডেট করা হয়েছে ও শিটে সিঙ্ক হচ্ছে!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
          ) {
            Text("Done / সমাপ্ত")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Seasonal, Festival & Day/Night Themes Management
    val activeThemeMode = settings.find { it.settingKey == "APP_ACTIVE_THEME" }?.settingValue ?: "AUTO"
    var selectedThemeMode by remember(activeThemeMode) { mutableStateOf(activeThemeMode) }

    val currentFontStyle = settings.find { it.settingKey == "UI_FONT_STYLE" }?.settingValue ?: "DEFAULT_SANS"
    var selectedFontStyle by remember(currentFontStyle) { mutableStateOf(currentFontStyle) }

    val currentTextScale = settings.find { it.settingKey == "UI_TEXT_SCALE" }?.settingValue ?: "STANDARD"
    var selectedTextScale by remember(currentTextScale) { mutableStateOf(currentTextScale) }

    val currentFontColor = settings.find { it.settingKey == "UI_FONT_COLOR" }?.settingValue ?: "#0A192F"
    var selectedFontColorHex by remember(currentFontColor) { mutableStateOf(currentFontColor) }

    // Theme Customizer State
    val themeOptions = listOf(
      AppThemeId.AUTO,
      AppThemeId.DAY,
      AppThemeId.NIGHT,
      AppThemeId.SUMMER,
      AppThemeId.RAINY,
      AppThemeId.WINTER,
      AppThemeId.PUJA,
      AppThemeId.EID,
      AppThemeId.LANGUAGE_DAY,
      AppThemeId.INDEPENDENCE_DAY
    )

    var themeToEdit by remember { mutableStateOf(AppThemeId.SUMMER) }
    val currentEditBase = remember(themeToEdit, settings) {
      val customJson = settings.find { it.settingKey == "THEME_CUSTOM_COLORS_${themeToEdit.code}" }?.settingValue
      ThemeManager.getThemeById(themeToEdit.code, customJson)
    }

    var editPrimaryHex by remember(currentEditBase) { mutableStateOf(AppThemeConfig.colorToHex(currentEditBase.primary)) }
    var editSecondaryHex by remember(currentEditBase) { mutableStateOf(AppThemeConfig.colorToHex(currentEditBase.secondary)) }
    var editBgHex by remember(currentEditBase) { mutableStateOf(AppThemeConfig.colorToHex(currentEditBase.background)) }

    val currentSeasonal = remember { ThemeManager.determineSeasonalTheme() }

    Card(
      modifier = Modifier.fillMaxWidth().testTag("card_theme_settings"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Seasonal & Festival Themes (ঋতু ও উৎসবভিত্তিক থিম)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
        }
        Text("Day/Night, Summer, Rainy, Winter, Puja, Eid, Language Day, and Independence Day with automatic seasonal switching", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        // Active Theme Mode Selector Cards with Avatars & Motifs
        Text("Active App Theme Mode (থিম সিলেক্ট করুন):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EblNavyDark)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          themeOptions.forEach { t ->
            val isSelected = selectedThemeMode.equals(t.code, true)
            val themeBase = remember(t, settings) {
              if (t == AppThemeId.AUTO) currentSeasonal
              else {
                val customJson = settings.find { it.settingKey == "THEME_CUSTOM_COLORS_${t.code}" }?.settingValue
                ThemeManager.getThemeById(t.code, customJson)
              }
            }

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedThemeMode = t.code
                  viewModel.setThemeMode(t.code)
                  Toast.makeText(context, "${t.banglaName} সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
                }
                .testTag("theme_card_${t.code}"),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) themeBase.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
              ),
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) t.accentColor else Color(0xFFE2E8F0)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Theme Avatar / Illustrated Picture Design
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                      Brush.linearGradient(
                        colors = listOf(themeBase.primary, themeBase.secondary)
                      )
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = t.emoji,
                    fontSize = 22.sp
                  )
                }

                // Theme Info
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = t.banglaName,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) themeBase.primary else EblNavyDark
                    )
                    if (t == AppThemeId.AUTO) {
                      Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          "★ RECOMMENDED",
                          fontSize = 8.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFFB45309),
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Text(
                    text = t.displayName,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                  )

                  Text(
                    text = t.motif,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  // Color Palette Preview Dots
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(themeBase.primary))
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(themeBase.secondary))
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(themeBase.tertiary))
                    Surface(
                      shape = CircleShape,
                      color = themeBase.background,
                      border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                      modifier = Modifier.size(12.dp)
                    ) {}
                  }
                }

                // Selection Radio / Checkmark
                if (isSelected) {
                  Surface(
                    color = t.accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                } else {
                  Surface(
                    color = Color.Transparent,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color.LightGray),
                    modifier = Modifier.size(24.dp)
                  ) {}
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Status indicator
        androidx.compose.material3.Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (selectedThemeMode == "AUTO") Color(0xFFF0FDF4) else Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedThemeMode == "AUTO") Color(0xFF86EFAC) else Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = if (selectedThemeMode == "AUTO") "স্বয়ংক্রিয় মোড সক্রিয়: বর্তমান ঋতু অনুযায়ী '${currentSeasonal.banglaName}' (${currentSeasonal.name}) থিম চলমান।"
                     else "ম্যানুয়াল থিম সক্রিয়: ${themeOptions.find { it.code.equals(selectedThemeMode, true) }?.displayName ?: selectedThemeMode}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = if (selectedThemeMode == "AUTO") Color(0xFF166534) else Color(0xFF1E40AF)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Customizer & Color Editor
        Text("Customize Theme Colors (যেকোনো থিমের নিজস্ব রং নির্ধারণ করুন):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EblNavyDark)
        Spacer(modifier = Modifier.height(6.dp))

        // Selector for which theme to edit
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          themeOptions.filter { it != AppThemeId.AUTO }.forEach { t ->
            val isEditing = themeToEdit == t
            androidx.compose.material3.SuggestionChip(
              onClick = { themeToEdit = t },
              label = {
                Text(
                  t.displayName,
                  fontSize = 10.sp,
                  fontWeight = if (isEditing) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                containerColor = if (isEditing) EblNavyPrimary.copy(alpha = 0.15f) else Color.Transparent
              ),
              border = if (isEditing) androidx.compose.foundation.BorderStroke(1.5.dp, EblNavyPrimary) else null
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Color Inputs for themeToEdit
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = editPrimaryHex,
            onValueChange = { editPrimaryHex = it },
            label = { Text("Primary Color", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = editSecondaryHex,
            onValueChange = { editSecondaryHex = it },
            label = { Text("Accent Color", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = editBgHex,
            onValueChange = { editBgHex = it },
            label = { Text("Background", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Preview Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
              AppThemeConfig.parseHexColor(editBgHex, Color(0xFFF8FAFC)),
              shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Theme Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppThemeConfig.parseHexColor(editPrimaryHex, Color.Black))
          androidx.compose.material3.Surface(
            shape = RoundedCornerShape(4.dp),
            color = AppThemeConfig.parseHexColor(editSecondaryHex, Color.DarkGray)
          ) {
            Text("Accent Badge", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              viewModel.resetThemeColors(themeToEdit.code)
              val def = ThemeManager.allDefaultThemes.find { it.id == themeToEdit } ?: ThemeManager.DefaultDayTheme
              editPrimaryHex = AppThemeConfig.colorToHex(def.primary)
              editSecondaryHex = AppThemeConfig.colorToHex(def.secondary)
              editBgHex = AppThemeConfig.colorToHex(def.background)
              Toast.makeText(context, "${themeToEdit.displayName} reset to default!", Toast.LENGTH_SHORT).show()
            }
          ) {
            Text("Reset Default", fontSize = 11.sp)
          }

          Button(
            onClick = {
              viewModel.saveCustomThemeColors(
                themeCode = themeToEdit.code,
                primaryHex = editPrimaryHex.trim(),
                secondaryHex = editSecondaryHex.trim(),
                tertiaryHex = editSecondaryHex.trim(),
                bgHex = editBgHex.trim(),
                surfaceHex = "#FFFFFF"
              )
              Toast.makeText(context, "'${themeToEdit.displayName}' colors saved & synced!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Theme & Sync", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Font Style / Typography Preset
        Text("Typography & Universal Font Style (সর্বজনীন ফন্ট স্টাইল):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                viewModel.setUniversalFont(key, selectedFontColorHex, selectedTextScale)
              },
              label = { Text(label, fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Universal Font Color Customizer
        Text("Universal Font Color (সর্বজনীন টেক্সট রং):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val fontColorPresets = listOf(
          Pair("#0A192F", "Deep Navy"),
          Pair("#0F172A", "Midnight"),
          Pair("#1E293B", "Slate Dark"),
          Pair("#064E3B", "Emerald"),
          Pair("#1E1B4B", "Royal Indigo"),
          Pair("#881337", "Burgundy")
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          fontColorPresets.forEach { (hex, name) ->
            val isColorSelected = selectedFontColorHex.equals(hex, ignoreCase = true)
            val parsedColor = AppThemeConfig.parseHexColor(hex, Color.Black)
            Surface(
              onClick = {
                selectedFontColorHex = hex
                viewModel.setUniversalFont(selectedFontStyle, hex, selectedTextScale)
                Toast.makeText(context, "$name ফন্ট রং সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(8.dp),
              color = if (isColorSelected) parsedColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
              border = BorderStroke(if (isColorSelected) 2.dp else 1.dp, if (isColorSelected) parsedColor else Color(0xFFCBD5E1)),
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
                )
                Text(name, fontSize = 10.sp, fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Normal, color = parsedColor)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Font Color Hex & Live Preview
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = selectedFontColorHex,
            onValueChange = { selectedFontColorHex = it },
            label = { Text("Custom Hex Color (যেমন: #0A192F)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          val parsedPreviewColor = AppThemeConfig.parseHexColor(selectedFontColorHex, Color.Black)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = parsedPreviewColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, parsedPreviewColor),
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(parsedPreviewColor)
              )
              Text("Sample Font", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = parsedPreviewColor)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. UI Text Scale / Font Size
        Text("UI Text Reading Scale (টেক্সট আকার):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                viewModel.setUniversalFont(selectedFontStyle, selectedFontColorHex, key)
              },
              label = { Text(label, fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save All Font Settings Button
        Button(
          onClick = {
            viewModel.setUniversalFont(
              fontStyle = selectedFontStyle,
              fontColorHex = selectedFontColorHex.trim(),
              textScale = selectedTextScale
            )
            Toast.makeText(context, "সর্বজনীন ফন্ট স্টাইল ও রং সংরক্ষিত হয়েছে এবং সিঙ্ক হচ্ছে!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Universal Font & Color (সর্বজনীন ফন্ট সংরক্ষণ)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
