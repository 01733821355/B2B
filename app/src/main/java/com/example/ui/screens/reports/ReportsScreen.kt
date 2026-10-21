package com.example.ui.screens.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.ThemeVisualBanner
import com.example.ui.common.TimeFilterBar
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils
import com.example.util.ExcelReportGenerator
import com.example.util.PdfReportGenerator
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val files by viewModel.filteredFiles.collectAsState()
  val allFiles by viewModel.allFiles.collectAsState()
  val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
  val allRms by viewModel.allRms.collectAsState()
  val selectedRmCode by viewModel.selectedRmCodeFilter.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val context = LocalContext.current

  // Date to Date Report Builder State
  var isDateToDateMode by remember { mutableStateOf(false) }

  val todayIso = remember { DateUtils.formatIsoDate(DateUtils.currentDhakaMillis()).ifBlank { "2026-10-21" } }
  val firstDayMonthIso = remember {
    try {
      val ld = LocalDate.now(DateUtils.DHAKA_ZONE).withDayOfMonth(1)
      ld.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } catch (_: Exception) {
      "2026-10-01"
    }
  }

  var fromDateStr by remember { mutableStateOf(firstDayMonthIso) }
  var toDateStr by remember { mutableStateOf(todayIso) }

  // DatePickerDialog launcher helper
  fun showDatePicker(initialIso: String, onDatePicked: (String) -> Unit) {
    val parts = initialIso.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
    val picker = DatePickerDialog(
      context,
      { _, y, m, d ->
        val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
        onDatePicked(formatted)
      },
      year,
      month,
      day
    )
    picker.show()
  }

  val isPrivileged = currentUser.role == "ADMIN" || currentUser.role == "MENTOR"

  // Compute displayed files based on mode: Quick Period vs Date to Date
  val displayedFiles = remember(files, allFiles, isDateToDateMode, fromDateStr, toDateStr, selectedRmCode, isPrivileged) {
    if (!isDateToDateMode) {
      files
    } else {
      val fromMillis = DateUtils.parseStartOfDayMillis(fromDateStr) ?: 0L
      val toMillis = DateUtils.parseEndOfDayMillis(toDateStr) ?: Long.MAX_VALUE
      allFiles.filter { file ->
        if (file.isDeleted) return@filter false
        if (isPrivileged && selectedRmCode != "All" && !file.assignedRmCode.equals(selectedRmCode, true)) {
          return@filter false
        }
        val fileTime = if (file.updatedAt > 0) file.updatedAt else file.createdAt
        fileTime in fromMillis..toMillis
      }
    }
  }

  val effectivePeriodLabel = remember(isDateToDateMode, fromDateStr, toDateStr, selectedTimeFilter) {
    if (isDateToDateMode) {
      "Date Range: $fromDateStr to $toDateStr"
    } else {
      selectedTimeFilter.label
    }
  }

  var generatedPdfFile by remember { mutableStateOf<File?>(null) }
  var showPdfSuccessDialog by remember { mutableStateOf(false) }
  var generatedExcelFile by remember { mutableStateOf<File?>(null) }
  var showExcelSuccessDialog by remember { mutableStateOf(false) }
  var showPrintPreviewDialog by remember { mutableStateOf(false) }

  // Quick stats for Date to Date
  val stcCount = remember(displayedFiles) { displayedFiles.count { it.applicationStatus.equals("STC", true) } }
  val approvedCount = remember(displayedFiles) { displayedFiles.count { it.applicationStatus.equals("Approved", true) } }
  val submittedCount = remember(displayedFiles) { displayedFiles.count { it.applicationStatus.equals("Submitted", true) } }
  val queryCount = remember(displayedFiles) { displayedFiles.count { it.applicationStatus.equals("Query", true) || it.applicationStatus.equals("Return to Source", true) } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("reports_screen")
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f)
    ) {
      // Dynamic Theme & Time Photo Banner
      item {
        ThemeVisualBanner(
          title = if (isPrivileged) "Operations & Audited Reports" else "My RM Performance Report",
          subtitle = "${displayedFiles.size} records • $effectivePeriodLabel",
          badgeText = if (isDateToDateMode) "📅 Custom Date-to-Date Mode Active" else "📊 Standard Time Filter Mode",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }

      // Action Export Buttons: PDF and Excel
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val pdf = PdfReportGenerator.generatePdfReport(
                  context = context,
                  files = displayedFiles,
                  currentUser = currentUser,
                  periodLabel = effectivePeriodLabel,
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
                  files = displayedFiles,
                  currentUser = currentUser,
                  periodLabel = effectivePeriodLabel,
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

            OutlinedButton(
              onClick = { showPrintPreviewDialog = true },
              modifier = Modifier.height(42.dp).testTag("btn_print_preview_report"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Print", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
          }
        }
      }

      // Mode Selector: Quick Filter vs Date to Date
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Report Period Selection",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
              }

              // Toggle Mode Chips
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                  selected = !isDateToDateMode,
                  onClick = { isDateToDateMode = false },
                  label = { Text("Quick Filters", fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EblNavyPrimary,
                    selectedLabelColor = Color.White
                  ),
                  modifier = Modifier.testTag("mode_chip_quick_filter")
                )
                FilterChip(
                  selected = isDateToDateMode,
                  onClick = { isDateToDateMode = true },
                  label = { Text("Date to Date (তারিখ হতে)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0F766E),
                    selectedLabelColor = Color.White
                  ),
                  modifier = Modifier.testTag("mode_chip_date_to_date")
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isDateToDateMode) {
              // Standard Time Filter Bar
              TimeFilterBar(
                selectedFilter = selectedTimeFilter,
                onFilterSelected = { viewModel.selectedTimeFilter.value = it }
              )
            } else {
              // Date to Date Custom Picker Section
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFF0FDFA), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0xFFCCFBF1), RoundedCornerShape(10.dp))
                  .padding(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // From Date Picker Field
                  OutlinedTextField(
                    value = fromDateStr,
                    onValueChange = { fromDateStr = it },
                    label = { Text("From Date (শুরুর তারিখ)", fontSize = 11.sp) },
                    readOnly = true,
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "From Date",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.clickable { showDatePicker(fromDateStr) { fromDateStr = it } }
                      )
                    },
                    modifier = Modifier
                      .weight(1f)
                      .clickable { showDatePicker(fromDateStr) { fromDateStr = it } }
                      .testTag("input_from_date")
                  )

                  // To Date Picker Field
                  OutlinedTextField(
                    value = toDateStr,
                    onValueChange = { toDateStr = it },
                    label = { Text("To Date (শেষ তারিখ)", fontSize = 11.sp) },
                    readOnly = true,
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "To Date",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.clickable { showDatePicker(toDateStr) { toDateStr = it } }
                      )
                    },
                    modifier = Modifier
                      .weight(1f)
                      .clickable { showDatePicker(toDateStr) { toDateStr = it } }
                      .testTag("input_to_date")
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Quarter & Range Preset Buttons
                Text("Quick Quarter Presets (কোয়ার্টার নির্বাচন):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF115E59))
                Spacer(modifier = Modifier.height(4.dp))
                val presetScrollState = rememberScrollState()
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetScrollState),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF14B8A6)),
                    modifier = Modifier.clickable {
                      fromDateStr = "2026-10-01"
                      toDateStr = "2026-12-31"
                    }
                  ) {
                    Text("Q4 (Oct-Dec)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF14B8A6)),
                    modifier = Modifier.clickable {
                      fromDateStr = "2026-07-01"
                      toDateStr = "2026-09-30"
                    }
                  ) {
                    Text("Q3 (Jul-Sep)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF14B8A6)),
                    modifier = Modifier.clickable {
                      fromDateStr = "2026-04-01"
                      toDateStr = "2026-06-30"
                    }
                  ) {
                    Text("Q2 (Apr-Jun)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF14B8A6)),
                    modifier = Modifier.clickable {
                      fromDateStr = "2026-01-01"
                      toDateStr = "2026-03-31"
                    }
                  ) {
                    Text("Q1 (Jan-Mar)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier.clickable {
                      fromDateStr = firstDayMonthIso
                      toDateStr = todayIso
                    }
                  ) {
                    Text("This Month", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B5563)),
                    modifier = Modifier.clickable {
                      fromDateStr = "2026-01-01"
                      toDateStr = "2026-12-31"
                    }
                  ) {
                    Text("Full Year 2026", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date-to-Date Performance Breakdown Metrics
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFCCFBF1), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("STC (Done)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                      Text("$stcCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F766E))
                    }
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFF6FF), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("Submitted", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                      Text("$submittedCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                    }
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("Approved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                      Text("$approvedCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                    }
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("Query/RTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                      Text("$queryCount", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // RM Filter for Admin / Mentor
      if (isPrivileged) {
        item {
          var rmFilterExpanded by remember { mutableStateOf(false) }
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
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
      }

      // Report Table
      item {
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

            if (displayedFiles.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("No records match $effectivePeriodLabel parameters.", color = Color.Gray, fontSize = 12.sp)
              }
            } else {
              Column {
                displayedFiles.forEach { f ->
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
          Text("Period: $effectivePeriodLabel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          Text("Records Included: ${displayedFiles.size}", fontSize = 11.sp, color = Color(0xFF0F766E))
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
          Text("Period: $effectivePeriodLabel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
          Text("Records Exported: ${displayedFiles.size}", fontSize = 11.sp, color = EblNavyDark)
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
          Text("Timezone: Asia/Dhaka GMT+6 • Period: $effectivePeriodLabel", fontSize = 11.sp, color = Color.Gray)
          Text("Generated By: ${currentUser.name} (${currentUser.role} ${currentUser.rmCode})", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(10.dp))
          Text("Total Records Count: ${displayedFiles.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          Text("STC (Done): $stcCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
          Text("Approved: $approvedCount", fontSize = 11.sp)
          Text("Submitted: $submittedCount", fontSize = 11.sp)
          Text("Query / Return to Source: $queryCount", fontSize = 11.sp)
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
