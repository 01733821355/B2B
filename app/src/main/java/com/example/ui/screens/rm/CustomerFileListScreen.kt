package com.example.ui.screens.rm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import com.example.util.PdfReportGenerator
import com.example.util.ExcelReportGenerator
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.common.VoiceInputField
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
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.CpvStatusBadge
import com.example.ui.common.EblPullRefreshBox
import com.example.ui.common.FloatableRmNavButton
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerFileListScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onAddNewFile: () -> Unit,
  onEditFile: (String) -> Unit,
  onOpenRoutePlanner: (List<String>) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val files by viewModel.filteredFiles.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedProduct by viewModel.selectedProductFilter.collectAsState()
  val selectedAppStatus by viewModel.selectedAppStatusFilter.collectAsState()
  val selectedActiveStatus by viewModel.selectedActiveStatusFilter.collectAsState()
  val selectedCpvStatus by viewModel.selectedCpvStatusFilter.collectAsState()
  val selectedRmCode by viewModel.selectedRmCodeFilter.collectAsState()
  val pendingDocsOnly by viewModel.pendingDocsOnlyFilter.collectAsState()
  val activeRouteCustomerIds by viewModel.activeRouteCustomerIds.collectAsState()

  val selectedFileIdsForRoute = remember { mutableStateListOf<String>() }

  var viewingFile by remember { mutableStateOf<CustomerFileEntity?>(null) }
  var fileToDelete by remember { mutableStateOf<CustomerFileEntity?>(null) }
  var showFiltersDialog by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val appCustomName by viewModel.appCustomName.collectAsState()
  var generatedPdfFile by remember { mutableStateOf<File?>(null) }
  var showPdfSuccessDialog by remember { mutableStateOf(false) }
  var generatedExcelFile by remember { mutableStateOf<File?>(null) }
  var showExcelSuccessDialog by remember { mutableStateOf(false) }

  val isPrivileged = currentUser.role == "ADMIN" || currentUser.role == "MENTOR"

  val isRefreshing by viewModel.isRefreshing.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    EblPullRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = { viewModel.refreshData() }
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .testTag("customer_file_list_screen")
      ) {
      // Header & Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (isPrivileged) "Global Customer Files" else "My Customer Files",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "${files.size} record(s) matching criteria",
          fontSize = 12.sp,
          color = Color.Gray
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // PDF Report Download Button
        OutlinedButton(
          onClick = {
            val pdf = PdfReportGenerator.generatePdfReport(
              context = context,
              files = files,
              currentUser = currentUser,
              periodLabel = "Filtered Customer Database",
              customAppName = appCustomName
            )
            generatedPdfFile = pdf
            showPdfSuccessDialog = true
          },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
          modifier = Modifier.height(34.dp).testTag("btn_list_export_pdf")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Excel Report Download Button
        OutlinedButton(
          onClick = {
            val excel = ExcelReportGenerator.generateExcelReport(
              context = context,
              files = files,
              currentUser = currentUser,
              periodLabel = "Filtered Customer Database",
              customAppName = appCustomName
            )
            generatedExcelFile = excel
            showExcelSuccessDialog = true
          },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D)),
          modifier = Modifier.height(34.dp).testTag("btn_list_export_excel")
        ) {
          Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Field Visit Route Plan Button
        OutlinedButton(
          onClick = {
            onOpenRoutePlanner(selectedFileIdsForRoute.toList())
          },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D4ED8)),
          border = BorderStroke(1.dp, Color(0xFF93C5FD)),
          modifier = Modifier.height(34.dp).testTag("btn_list_open_route_planner")
        ) {
          Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (activeRouteCustomerIds.isNotEmpty()) "Route (${activeRouteCustomerIds.size})" else "Route",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(
          onClick = { viewModel.resetFilters() },
          modifier = Modifier.size(34.dp).testTag("btn_refresh_files")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EblNavyPrimary, modifier = Modifier.size(18.dp))
        }

        Button(
          onClick = onAddNewFile,
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(34.dp).testTag("btn_list_add_file")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Search bar & Filter Trigger
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      VoiceInputField(
        value = searchQuery,
        onValueChange = { viewModel.searchQuery.value = it },
        label = "Search Files",
        placeholder = "Search by Name, CC-Number, Mobile, Company...",
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("file_search_input")
      )

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = { showFiltersDialog = true },
        modifier = Modifier.testTag("btn_open_filters")
      ) {
        Icon(
          Icons.Default.FilterList,
          contentDescription = "Filters",
          tint = if (selectedProduct != "All" || selectedAppStatus != "All" || selectedActiveStatus != "All") EblNavyPrimary else Color.Gray
        )
      }
    }

    // Active Filter Chips indicator
    val scrollState = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = pendingDocsOnly,
        onClick = { viewModel.pendingDocsOnlyFilter.value = !pendingDocsOnly },
        label = { Text(if (pendingDocsOnly) "✓ Pending Docs Backlog" else "⚠️ Pending Docs Backlog", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = Color(0xFFDC2626),
          selectedLabelColor = Color.White
        )
      )

      if (selectedProduct != "All") {
        FilterChip(
          selected = true,
          onClick = { viewModel.selectedProductFilter.value = "All" },
          label = { Text("Prod: $selectedProduct ✕", fontSize = 10.sp) }
        )
      }
      if (selectedAppStatus != "All") {
        FilterChip(
          selected = true,
          onClick = { viewModel.selectedAppStatusFilter.value = "All" },
          label = { Text("Status: $selectedAppStatus ✕", fontSize = 10.sp) }
        )
      }
      if (selectedActiveStatus != "All") {
        FilterChip(
          selected = true,
          onClick = { viewModel.selectedActiveStatusFilter.value = "All" },
          label = { Text("Active: $selectedActiveStatus ✕", fontSize = 10.sp) }
        )
      }
      if (selectedCpvStatus != "All") {
        FilterChip(
          selected = true,
          onClick = { viewModel.selectedCpvStatusFilter.value = "All" },
          label = { Text("CPV: $selectedCpvStatus ✕", fontSize = 10.sp) }
        )
      }
      if (isPrivileged && selectedRmCode != "All") {
        FilterChip(
          selected = true,
          onClick = { viewModel.selectedRmCodeFilter.value = "All" },
          label = { Text("RM: $selectedRmCode ✕", fontSize = 10.sp) }
        )
      }
    }

    // Files List
    if (files.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("No customer records match your filter criteria.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = { viewModel.resetFilters() }) {
          Text("Clear All Filters")
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
      ) {
        items(files, key = { it.fileId }) { file ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 5.dp)
              .testTag("file_row_${file.fileId}"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              // Top line: Checkbox + Name & Status badges
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val isRouteSelected = selectedFileIdsForRoute.contains(file.fileId)
                  Checkbox(
                    checked = isRouteSelected,
                    onCheckedChange = { checked ->
                      if (checked) selectedFileIdsForRoute.add(file.fileId)
                      else selectedFileIdsForRoute.remove(file.fileId)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = EblNavyPrimary),
                    modifier = Modifier.size(24.dp).testTag("chk_route_file_${file.fileId}")
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (file.serialNumber.isNotBlank()) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = EblNavyDark,
                          modifier = Modifier.padding(end = 6.dp)
                        ) {
                          Text(
                            text = "SL #${file.serialNumber}",
                            color = EblGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = file.customerName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EblNavyDark
                      )
                    }
                    Text(
                      text = "${file.ccNumber.ifBlank { file.fileId }} • ${file.companyName}",
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  ApplicationStatusBadge(file.applicationStatus)
                  ActiveStatusBadge(file.activeStatus)
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Middle line: Details
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(
                    text = "Product: ${file.productType} • Mobile: ${file.mobile}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                  )
                  if (file.ccNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "CC-Number: ${file.ccNumber}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = EblNavyPrimary
                    )
                  }
                }
                if (isPrivileged) {
                  Text(
                    text = "RM: ${file.assignedRmCode}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EblNavyPrimary
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Created: ${DateUtils.formatDateTime(file.createdAt)}",
                  fontSize = 10.sp,
                  color = Color.Gray
                )
                Text(
                  text = "Updated: ${DateUtils.formatDateTime(file.updatedAt)}",
                  fontSize = 10.sp,
                  color = EblNavyPrimary,
                  fontWeight = FontWeight.Medium
                )
              }

              if (file.pendingDocuments.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFFEF2F2),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                      text = "⚠️ Missing / Pending Documents:",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      file.pendingDocuments.split(",").filter { it.isNotBlank() }.forEach { doc ->
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = Color(0xFFFEE2E2)
                        ) {
                          Text(
                            text = "• $doc",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Action buttons row (compact and wrap-safe)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                  CpvStatusBadge(file.cpvStatus)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedButton(
                    onClick = { viewingFile = file },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp).testTag("btn_view_${file.fileId}")
                  ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Details", fontSize = 11.sp, maxLines = 1, softWrap = false)
                  }

                  OutlinedButton(
                    onClick = { onEditFile(file.fileId) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp).testTag("btn_edit_${file.fileId}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Edit", fontSize = 11.sp, maxLines = 1, softWrap = false)
                  }

                  IconButton(
                    onClick = { fileToDelete = file },
                    modifier = Modifier.size(30.dp).testTag("btn_delete_${file.fileId}")
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(32.dp))
        }
      }
    }
  }
}

    // Floating Want to Go Action Bar when 1 or more files are selected
    if (selectedFileIdsForRoute.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${selectedFileIdsForRoute.size} জন কাস্টমার নির্বাচিত",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = EblNavyDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
              onClick = { selectedFileIdsForRoute.clear() },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("Clear", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
          }

          Button(
            onClick = {
              onOpenRoutePlanner(selectedFileIdsForRoute.toList())
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_want_to_go_floating")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Want to Go 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
          }
        }
      }
    }

  // Floatable & Draggable Quick Navigation Menu for RM
  if (currentUser.role == "RM") {
    FloatableRmNavButton(
      onNavigateNewFile = onAddNewFile,
      onNavigateMyFiles = { viewModel.pendingDocsOnlyFilter.value = false },
      onNavigatePendingDocs = { viewModel.pendingDocsOnlyFilter.value = true },
      onNavigateDashboard = { viewModel.navigateTo(Screen.RmDashboard) },
      onNavigateCommunication = { viewModel.navigateTo(Screen.CommunicationHub) },
      onNavigateDbrChecklist = { viewModel.navigateTo(Screen.DbrChecklist) },
      onNavigateImportantDocuments = { viewModel.navigateTo(Screen.ImportantDocuments) }
    )
  }
}

  // Delete Confirmation Dialog
  if (fileToDelete != null) {
    AlertDialog(
      onDismissRequest = { fileToDelete = null },
      title = { Text("Confirm Record Deletion", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to delete file '${fileToDelete!!.customerName}' (${fileToDelete!!.fileId})? This action will be recorded in the security audit log.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteFile(fileToDelete!!.fileId)
            fileToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          modifier = Modifier.testTag("btn_confirm_delete")
        ) {
          Text("Delete Record")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { fileToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // View Customer Details Dialog
  if (viewingFile != null) {
    val liveViewingFile = files.find { it.fileId == viewingFile!!.fileId } ?: viewingFile!!
    CustomerDetailDialog(
      file = liveViewingFile,
      currentUser = currentUser,
      viewModel = viewModel,
      onDismiss = { viewingFile = null },
      onEdit = { id ->
        viewingFile = null
        onEditFile(id)
      }
    )
  }

  // Filter Selector Dialog
  if (showFiltersDialog) {
    val dynamicProductTypes by viewModel.productTypes.collectAsState()
    val productOptions = listOf("All") + (if (dynamicProductTypes.isNotEmpty()) dynamicProductTypes else listOf("Credit Card", "B2B", "Corporate Card", "Split", "Limit Enhancement"))
    val dynamicStatuses by viewModel.applicationStatusOptions.collectAsState()
    val statusOptions = listOf("All") + (if (dynamicStatuses.isNotEmpty()) dynamicStatuses else listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC"))
    val activeOptions = listOf("All", "Y", "N", "C")
    val cpvOptions = listOf("All", "Pending", "Completed", "Failed", "Not Required")

    AlertDialog(
      onDismissRequest = { showFiltersDialog = false },
      title = { Text("Filter Customer Records", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Product Type:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            productOptions.forEach { p ->
              FilterChip(
                selected = selectedProduct == p,
                onClick = { viewModel.selectedProductFilter.value = p },
                label = { Text(p, fontSize = 11.sp) },
                modifier = Modifier.padding(end = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text("Application Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            statusOptions.forEach { s ->
              FilterChip(
                selected = selectedAppStatus == s,
                onClick = { viewModel.selectedAppStatusFilter.value = s },
                label = { Text(s, fontSize = 11.sp) },
                modifier = Modifier.padding(end = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text("Active Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Row {
            activeOptions.forEach { a ->
              FilterChip(
                selected = selectedActiveStatus == a,
                onClick = { viewModel.selectedActiveStatusFilter.value = a },
                label = { Text(a, fontSize = 11.sp) },
                modifier = Modifier.padding(end = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text("CPV Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            cpvOptions.forEach { c ->
              FilterChip(
                selected = selectedCpvStatus == c,
                onClick = { viewModel.selectedCpvStatusFilter.value = c },
                label = { Text(c, fontSize = 11.sp) },
                modifier = Modifier.padding(end = 4.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showFiltersDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Apply Filters")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            viewModel.resetFilters()
            showFiltersDialog = false
          }
        ) {
          Text("Reset")
        }
      }
    )
  }

  // PDF Generated Success Dialog
  if (showPdfSuccessDialog && generatedPdfFile != null) {
    AlertDialog(
      onDismissRequest = { showPdfSuccessDialog = false },
      title = { Text("PDF Report Generated", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Official Executive PDF report has been generated successfully.", fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text("File: ${generatedPdfFile!!.name} (${generatedPdfFile!!.length() / 1024} KB)", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          Text("Saved locally. You can download directly to device storage or share via WhatsApp / Email.", fontSize = 11.sp, color = EblNavyPrimary)
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              val savedUri = PdfReportGenerator.savePdfToDownloads(context, generatedPdfFile!!, generatedPdfFile!!.name)
              if (savedUri != null) {
                Toast.makeText(context, "PDF Report ডাউনলোড হয়েছে!\nপাথ: Downloads/${generatedPdfFile!!.name}", Toast.LENGTH_LONG).show()
              } else {
                Toast.makeText(context, "ডাউনলোড সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
              }
            }
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save to Downloads", fontSize = 11.sp)
          }

          Button(
            onClick = {
              PdfReportGenerator.shareOrViewPdf(context, generatedPdfFile!!)
            }
          ) {
            Text("Open / Share", fontSize = 11.sp)
          }
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showPdfSuccessDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // Excel Generated Success Dialog
  if (showExcelSuccessDialog && generatedExcelFile != null) {
    AlertDialog(
      onDismissRequest = { showExcelSuccessDialog = false },
      title = { Text("Excel Spreadsheet Generated", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Official Operations Excel spreadsheet has been generated successfully.", fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text("File: ${generatedExcelFile!!.name} (${generatedExcelFile!!.length() / 1024} KB)", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          Text("Compatible with Microsoft Excel, Google Sheets, and WPS Office.", fontSize = 11.sp, color = Color(0xFF15803D))
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              val savedUri = ExcelReportGenerator.saveExcelToDownloads(context, generatedExcelFile!!, generatedExcelFile!!.name)
              if (savedUri != null) {
                Toast.makeText(context, "Excel Report ডাউনলোড হয়েছে!\nপাথ: Downloads/${generatedExcelFile!!.name}", Toast.LENGTH_LONG).show()
              } else {
                Toast.makeText(context, "ডাউনলোড সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D))
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save to Downloads", fontSize = 11.sp)
          }

          Button(
            onClick = {
              ExcelReportGenerator.shareOrViewExcel(context, generatedExcelFile!!)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
          ) {
            Text("Open / Share", fontSize = 11.sp)
          }
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showExcelSuccessDialog = false }) {
          Text("Done")
        }
      }
    )
  }
}
