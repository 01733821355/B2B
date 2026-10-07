package com.example.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.util.BiometricHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun AppSettingsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.appSettings.collectAsState()
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
          onClick = { viewModel.updateSetting("PENDING_DOCS_LIST", pendingDocsInput.trim()) },
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

    // Biometric & Fingerprint Security Status Card
    val biometricAvailability = BiometricHelper.checkBiometricAvailability(LocalContext.current)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Biometric Security (ফিঙ্গারপ্রিন্ট লগইন)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = when (biometricAvailability) {
            BiometricHelper.BiometricAvailability.AVAILABLE -> "Device biometric sensor is active and ready for Fingerprint Sign In."
            BiometricHelper.BiometricAvailability.NONE_ENROLLED -> "No fingerprint registered on this phone. Please add a fingerprint in Android Settings to enable quick sign-in."
            BiometricHelper.BiometricAvailability.NO_HARDWARE -> "Fingerprint hardware is not available on this device."
            else -> "Biometric status: Unavailable."
          },
          fontSize = 12.sp,
          color = if (biometricAvailability == BiometricHelper.BiometricAvailability.AVAILABLE) Color(0xFF059669) else Color(0xFFD97706)
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}
