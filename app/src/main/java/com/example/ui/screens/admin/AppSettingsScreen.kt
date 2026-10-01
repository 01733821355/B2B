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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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

    Spacer(modifier = Modifier.height(28.dp))
  }
}
