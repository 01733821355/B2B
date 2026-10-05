package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import com.example.util.ExcelReportGenerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.TimeFilterBar
import com.example.ui.theme.EblNavyDark
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils
import com.example.util.PdfReportGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val files by viewModel.filteredFiles.collectAsState()
  val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
  val allRms by viewModel.allRms.collectAsState()
  val selectedRmCode by viewModel.selectedRmCodeFilter.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val context = LocalContext.current

  var generatedPdfFile by remember { mutableStateOf<File?>(null) }
  var showPdfSuccessDialog by remember { mutableStateOf(false) }
  var generatedExcelFile by remember { mutableStateOf<File?>(null) }
  var showExcelSuccessDialog by remember { mutableStateOf(false) }
  var showPrintPreviewDialog by remember { mutableStateOf(false) }

  val isPrivileged = currentUser.role == "ADMIN" || currentUser.role == "MENTOR"

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("reports_screen")
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
            text = if (isPrivileged) "Operations & File Reports" else "My RM Performance Report",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Text(
            text = "${files.size} records in selected period",
            fontSize = 11.sp,
            color = Color.Gray
          )
        }

        OutlinedButton(
          onClick = { showPrintPreviewDialog = true },
          modifier = Modifier.testTag("btn_print_preview_report"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Print", fontSize = 11.sp, maxLines = 1, softWrap = false)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Dedicated Action Row for PDF and Excel (No CSV!)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = {
            val pdf = PdfReportGenerator.generatePdfReport(
              context = context,
              files = files,
              currentUser = currentUser,
              periodLabel = selectedTimeFilter.label,
              customAppName = appCustomName
            )
            generatedPdfFile = pdf
            showPdfSuccessDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f).height(42.dp).testTag("btn_export_pdf_report")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        Button(
          onClick = {
            val excel = ExcelReportGenerator.generateExcelReport(
              context = context,
              files = files,
              currentUser = currentUser,
              periodLabel = selectedTimeFilter.label,
              customAppName = appCustomName
            )
            generatedExcelFile = excel
            showExcelSuccessDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f).height(42.dp).testTag("btn_export_excel_report")
        ) {
          Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
      }
    }

    // Time Filter
    TimeFilterBar(
      selectedFilter = selectedTimeFilter,
      onFilterSelected = { viewModel.selectedTimeFilter.value = it }
    )

    // RM Filter for Admin / Mentor
    if (isPrivileged) {
      var rmFilterExpanded by remember { mutableStateOf(false) }
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        ExposedDropdownMenuBox(
          expanded = rmFilterExpanded,
          onExpandedChange = { rmFilterExpanded = !rmFilterExpanded }
        ) {
          OutlinedTextField(
            value = if (selectedRmCode == "All") "Filter by RM: All Officers" else "Filter by RM: $selectedRmCode",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rmFilterExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = rmFilterExpanded,
            onDismissRequest = { rmFilterExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("All Officers") },
              onClick = {
                viewModel.selectedRmCodeFilter.value = "All"
                rmFilterExpanded = false
              }
            )
            allRms.forEach { rm ->
              DropdownMenuItem(
                text = { Text("${rm.name} (${rm.rmCode})") },
                onClick = {
                  viewModel.selectedRmCodeFilter.value = rm.rmCode
                  rmFilterExpanded = false
                }
              )
            }
          }
        }
      }
    }

    // Report Table
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8EEF5))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("File ID & Customer", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          Text("Product", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          Text("Status", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          if (isPrivileged) {
            Text("RM", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          }
          Text("Date", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
        }

        if (files.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No records match report parameters.", color = Color.Gray, fontSize = 12.sp)
          }
        } else {
          LazyColumn(modifier = Modifier.height(400.dp)) {
            items(files, key = { it.fileId }) { f ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(2f)) {
                  Text(f.customerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  Text(f.fileId, fontSize = 10.sp, color = Color.Gray)
                }
                Text(f.productType, modifier = Modifier.weight(1.2f), fontSize = 10.sp, maxLines = 1)
                Box(modifier = Modifier.weight(1.2f)) {
                  ApplicationStatusBadge(f.applicationStatus)
                }
                if (isPrivileged) {
                  Text(f.assignedRmCode, modifier = Modifier.weight(0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
                }
                Text(DateUtils.formatDateOnly(f.updatedAt), modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.Gray)
              }
              HorizontalDivider(color = Color(0xFFF1F5F9))
            }
          }
        }
      }
    }
  }

  // PDF Generated Success Dialog
  if (showPdfSuccessDialog && generatedPdfFile != null) {
    AlertDialog(
      onDismissRequest = { showPdfSuccessDialog = false },
      title = { Text("PDF Report Generated", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Formal Executive PDF report has been generated successfully.", fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text("File: ${generatedPdfFile!!.name} (${generatedPdfFile!!.length() / 1024} KB)", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          Text("Ready to share via WhatsApp, Gmail, or open in PDF viewer.", fontSize = 11.sp, color = EblNavyPrimary)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            PdfReportGenerator.shareOrViewPdf(context, generatedPdfFile!!)
          }
        ) {
          Text("Open / Share PDF")
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
        Button(
          onClick = {
            ExcelReportGenerator.shareOrViewExcel(context, generatedExcelFile!!)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
        ) {
          Text("Open / Share Excel")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showExcelSuccessDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // Print Preview Dialog
  if (showPrintPreviewDialog) {
    AlertDialog(
      onDismissRequest = { showPrintPreviewDialog = false },
      title = { Text("Print-Friendly Summary", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
          Text("PERFORMANCE SUMMARY REPORT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
          Text("Timezone: Asia/Dhaka GMT+6 • Period: ${selectedTimeFilter.label}", fontSize = 11.sp, color = Color.Gray)
          Text("Generated By: ${currentUser.name} (${currentUser.role} ${currentUser.rmCode})", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(10.dp))
          Text("Total Records Count: ${files.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          Text("Approved: ${files.count { it.applicationStatus.equals("Approved", true) }}", fontSize = 11.sp)
          Text("Submitted: ${files.count { it.applicationStatus.equals("Submitted", true) }}", fontSize = 11.sp)
          Text("Collected: ${files.count { it.applicationStatus.equals("Collected", true) }}", fontSize = 11.sp)
          Text("Query / Return to Source: ${files.count { it.applicationStatus.equals("Query", true) || it.applicationStatus.equals("Return to Source", true) }}", fontSize = 11.sp)
        }
      },
      confirmButton = {
        Button(onClick = { showPrintPreviewDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}
