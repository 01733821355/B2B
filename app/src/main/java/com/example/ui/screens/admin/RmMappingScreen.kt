package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.common.AccountStatusBadge
import com.example.ui.common.VoiceInputField
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils
import com.example.util.SecurityUtils

@Composable
fun RmMappingScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val allRms by viewModel.allRms.collectAsState()
  val allTargets by viewModel.allTargets.collectAsState()
  var searchRmQuery by remember { mutableStateOf("") }

  var showAddDialog by remember { mutableStateOf(false) }
  var editingRm by remember { mutableStateOf<UserEntity?>(null) }
  var resetPasswordRm by remember { mutableStateOf<UserEntity?>(null) }
  var settingTargetsRm by remember { mutableStateOf<UserEntity?>(null) }
  var rmToDelete by remember { mutableStateOf<UserEntity?>(null) }
  var showExportDialog by remember { mutableStateOf(false) }
  var exportedCsvContent by remember { mutableStateOf("") }

  val filteredRms = allRms.filter { rm ->
    if (searchRmQuery.isBlank()) true
    else {
      val q = searchRmQuery.lowercase()
      rm.name.lowercase().contains(q) || rm.rmCode.lowercase().contains(q) || rm.mobile.contains(q)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("rm_mapping_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "RM Mapping & Accounts",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${allRms.size} Relationship Officers Registered",
              fontSize = 11.sp,
              color = Color.Gray
            )
          }

          OutlinedButton(
            onClick = {
              exportedCsvContent = viewModel.eblRepository.generateRmMappingsCsv(filteredRms)
              showExportDialog = true
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_export_rm_csv")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export", fontSize = 11.sp, maxLines = 1, softWrap = false)
          }
        }

        // Admin or Mentor can assign/provision new RM accounts - ALWAYS VISIBLE PROMINENT FULL-WIDTH BUTTON!
        if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("btn_add_new_rm")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("+ Add New RM Officer", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }
      }

    // Voice-enabled Search bar
    VoiceInputField(
      value = searchRmQuery,
      onValueChange = { searchRmQuery = it },
      label = "Search RM",
      placeholder = "Search RM by Name, RM Code, or Mobile...",
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      testTag = "search_rm_input"
    )

    Spacer(modifier = Modifier.height(10.dp))

    // List of RMs
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      items(filteredRms, key = { it.rmCode }) { rm ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("rm_card_${rm.rmCode}"),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = rm.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EblNavyDark
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  AccountStatusBadge(rm.accountStatus)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "RM Code: ${rm.rmCode} • ${rm.officeAddress}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              // Actions menu
              var menuExpanded by remember { mutableStateOf(false) }
              Box {
                IconButton(onClick = { menuExpanded = true }) {
                  Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                  expanded = menuExpanded,
                  onDismissRequest = { menuExpanded = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("Edit Details", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                      menuExpanded = false
                      editingRm = rm
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Reset Password", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    onClick = {
                      menuExpanded = false
                      resetPasswordRm = rm
                    }
                  )
                  if (rm.accountStatus == "ACTIVE") {
                    DropdownMenuItem(
                      text = { Text("Deactivate Account", color = Color(0xFFDC2626), fontSize = 12.sp) },
                      onClick = {
                        menuExpanded = false
                        viewModel.setRmStatus(rm.rmCode, "INACTIVE")
                      }
                    )
                    DropdownMenuItem(
                      text = { Text("Suspend Account", color = Color(0xFFDC2626), fontSize = 12.sp) },
                      onClick = {
                        menuExpanded = false
                        viewModel.setRmStatus(rm.rmCode, "SUSPENDED")
                      }
                    )
                  } else {
                    DropdownMenuItem(
                      text = { Text("Activate Account", color = Color(0xFF15803D), fontSize = 12.sp) },
                      onClick = {
                        menuExpanded = false
                        viewModel.setRmStatus(rm.rmCode, "ACTIVE")
                      }
                    )
                  }

                  if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                      text = { Text("Delete RM Profile", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                      onClick = {
                        menuExpanded = false
                        rmToDelete = rm
                      }
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mentor Approval Action Strip
            if (currentUser.role == "MENTOR" && rm.accountStatus == "PENDING_APPROVAL") {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Pending Your Approval", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text("Assigned by Admin", fontSize = 10.sp, color = Color(0xFF92400E))
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                      onClick = { viewModel.approveRm(rm.rmCode) { _, _ -> } },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.testTag("btn_approve_rm_${rm.rmCode}")
                    ) {
                      Text("✓ Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                      onClick = { viewModel.rejectRm(rm.rmCode) { _, _ -> } },
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.testTag("btn_reject_rm_${rm.rmCode}")
                    ) {
                      Text("✕ Reject", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Mobile: ${rm.mobile} • Email: ${rm.email.ifBlank { "N/A" }}",
                fontSize = 11.sp,
                color = Color.DarkGray
              )
              Text(
                text = "Last Login: ${DateUtils.formatDateTime(rm.lastLogin)}",
                fontSize = 10.sp,
                color = Color.Gray
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Targets summary for this RM
            val thisTarget = allTargets.find { it.rmCode == rm.rmCode }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF1F5F9),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Targets -> CC: ${thisTarget?.creditCardTarget ?: 20} | Corp: ${thisTarget?.corporateCardTarget ?: 10} | B2B: ${thisTarget?.b2bTarget ?: 15}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = EblNavyDark
                )
                if (currentUser.role == "ADMIN") {
                  Text(
                    text = "Set Target",
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { settingTargetsRm = rm }
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Direct Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { editingRm = rm },
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Edit Info", fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = { resetPasswordRm = rm },
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Password", fontSize = 11.sp)
              }

              if (currentUser.role == "ADMIN") {
                OutlinedButton(
                  onClick = { settingTargetsRm = rm },
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Targets", fontSize = 11.sp)
                }
              }

              if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
                IconButton(
                  onClick = { rmToDelete = rm },
                  modifier = Modifier.size(32.dp).testTag("btn_delete_rm_${rm.rmCode}")
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete RM", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }

  // Floating Action Button for Adding RM (Always visible in bottom corner)
  if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
    ExtendedFloatingActionButton(
      onClick = { showAddDialog = true },
      icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White) },
      text = { Text("Add RM", fontWeight = FontWeight.Bold, color = Color.White) },
      containerColor = EblNavyPrimary,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("fab_add_new_rm")
    )
  }
}

  // Add RM Modal Dialog
  if (showAddDialog) {
    var rmCodeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var initialPassInput by remember { mutableStateOf(SecurityUtils.generateTemporaryPassword()) }
    var addError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add New RM Officer", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (addError != null) {
            Text(addError!!, color = Color.Red, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
          }
          VoiceInputField(
            value = rmCodeInput,
            onValueChange = { rmCodeInput = it; addError = null },
            label = "Unique RM Code *",
            placeholder = "e.g. 104396",
            testTag = "add_rm_code"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = nameInput,
            onValueChange = { nameInput = it; addError = null },
            label = "RM Full Name *",
            placeholder = "e.g. Shakil Hossain",
            testTag = "add_rm_name"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = mobileInput,
            onValueChange = { mobileInput = it },
            label = "Mobile Number",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTag = "add_rm_mobile"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = "Email Address",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            testTag = "add_rm_email"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = addressInput,
            onValueChange = { addressInput = it },
            label = "Office / Branch Location",
            testTag = "add_rm_address"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = initialPassInput,
            onValueChange = { initialPassInput = it },
            label = "Assigned Initial Password",
            testTag = "add_rm_password"
          )
          Text(
            text = "Notice: Assigned RM will be created as ACTIVE and immediately synced to Google Sheets in 'RM_Details' tab. The RM can log in on ANY mobile device with this RM Code and password.",
            fontSize = 11.sp,
            color = Color(0xFF047857),
            modifier = Modifier.padding(top = 6.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rmCodeInput.isBlank() || nameInput.isBlank()) {
              addError = "RM Code and Name are mandatory."
              return@Button
            }
            viewModel.createRm(
              rmCode = rmCodeInput,
              name = nameInput,
              mobile = mobileInput,
              email = emailInput,
              officeAddress = addressInput,
              initialPassword = initialPassInput
            ) { success, err ->
              if (success) {
                showAddDialog = false
              } else {
                addError = err ?: "Failed to assign RM account."
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          modifier = Modifier.testTag("btn_confirm_add_rm")
        ) {
          Text("Create & Sync to Sheet")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Edit RM Dialog
  if (editingRm != null) {
    var editName by remember { mutableStateOf(editingRm!!.name) }
    var editMobile by remember { mutableStateOf(editingRm!!.mobile) }
    var editEmail by remember { mutableStateOf(editingRm!!.email) }
    var editAddress by remember { mutableStateOf(editingRm!!.officeAddress) }
    var editNewPassword by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { editingRm = null },
      title = { Text("Edit RM: ${editingRm!!.rmCode}", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          VoiceInputField(
            value = editName,
            onValueChange = { editName = it },
            label = "Full Name",
            modifier = Modifier.fillMaxWidth(),
            testTag = "edit_rm_name"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = editMobile,
            onValueChange = { editMobile = it },
            label = "Mobile",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            testTag = "edit_rm_mobile"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = editEmail,
            onValueChange = { editEmail = it },
            label = "Email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            testTag = "edit_rm_email"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = editAddress,
            onValueChange = { editAddress = it },
            label = "Branch / Office Address",
            modifier = Modifier.fillMaxWidth(),
            testTag = "edit_rm_address"
          )
          Spacer(modifier = Modifier.height(8.dp))
          VoiceInputField(
            value = editNewPassword,
            onValueChange = { editNewPassword = it },
            label = "Change Password (leave blank to keep current)",
            placeholder = "Type new password if changing...",
            modifier = Modifier.fillMaxWidth(),
            testTag = "edit_rm_password"
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateRm(
              rmCode = editingRm!!.rmCode,
              name = editName,
              mobile = editMobile,
              email = editEmail,
              officeAddress = editAddress,
              newPassword = editNewPassword.ifBlank { null }
            ) { success, _ ->
              if (success) editingRm = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { editingRm = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Set RM Targets Dialog
  if (settingTargetsRm != null) {
    val existingTarget = allTargets.find { it.rmCode == settingTargetsRm!!.rmCode }
    var ccTargetInput by remember { mutableStateOf((existingTarget?.creditCardTarget ?: 20).toString()) }
    var corpTargetInput by remember { mutableStateOf((existingTarget?.corporateCardTarget ?: 10).toString()) }
    var b2bTargetInput by remember { mutableStateOf((existingTarget?.b2bTarget ?: 15).toString()) }

    AlertDialog(
      onDismissRequest = { settingTargetsRm = null },
      title = { Text("Set Targets for ${settingTargetsRm!!.name}", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Define monthly KPI quotas for RM Code: ${settingTargetsRm!!.rmCode}", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = ccTargetInput,
            onValueChange = { ccTargetInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Credit Card Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = corpTargetInput,
            onValueChange = { corpTargetInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Corporate Card Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = b2bTargetInput,
            onValueChange = { b2bTargetInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("B2B Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cc = ccTargetInput.toIntOrNull() ?: 20
            val corp = corpTargetInput.toIntOrNull() ?: 10
            val b2b = b2bTargetInput.toIntOrNull() ?: 15
            viewModel.setRmTargets(settingTargetsRm!!.rmCode, cc, corp, b2b) { success, _ ->
              if (success) settingTargetsRm = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Targets")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { settingTargetsRm = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Reset Password Dialog
  if (resetPasswordRm != null) {
    var generatedPass by remember { mutableStateOf(SecurityUtils.generateTemporaryPassword()) }

    AlertDialog(
      onDismissRequest = { resetPasswordRm = null },
      title = { Text("Reset RM Password", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Assign a new temporary password for RM ${resetPasswordRm!!.name} (${resetPasswordRm!!.rmCode}):")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = generatedPass,
            onValueChange = { generatedPass = it },
            label = { Text("New Temporary Password") },
            modifier = Modifier.fillMaxWidth().testTag("input_reset_password")
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "RM will be required to change this upon next login. Never send credentials over unencrypted channels.",
            fontSize = 11.sp,
            color = Color.Gray
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.resetRmPassword(resetPasswordRm!!.rmCode, generatedPass) { success, _ ->
              if (success) resetPasswordRm = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Confirm Reset")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { resetPasswordRm = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Export CSV Dialog
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text("RM Mapping Export (CSV)", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("The table data has been prepared in standard CSV format:", fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
              .padding(10.dp)
          ) {
            Text(exportedCsvContent, fontSize = 10.sp, color = Color.DarkGray)
          }
        }
      },
      confirmButton = {
        Button(onClick = { showExportDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // Delete RM Profile Confirmation Dialog
  val rmItemToDelete = rmToDelete
  if (rmItemToDelete != null) {
    AlertDialog(
      onDismissRequest = { rmToDelete = null },
      title = { Text("Delete RM Profile", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
      text = {
        Column {
          Text(
            text = "Are you sure you want to permanently delete RM Profile '${rmItemToDelete.name}' (Code: ${rmItemToDelete.rmCode})?",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "This action will delete the officer's user credentials and target assignments locally, and synchronize deletion with Google Sheets. This cannot be undone.",
            fontSize = 12.sp,
            color = Color.DarkGray
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteRmProfile(rmItemToDelete.rmCode)
            rmToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          modifier = Modifier.testTag("btn_confirm_delete_rm")
        ) {
          Text("Delete Profile")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { rmToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
