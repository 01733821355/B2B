package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanySearchResult
import com.example.data.model.CompanySheetData
import com.example.data.model.CompanyWorkbookData
import com.example.data.model.UserEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils

@Composable
fun CompanyProfileScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigateBack: () -> Unit
) {
  BackHandler { onNavigateBack() }
  val context = LocalContext.current
  val workbook by viewModel.companyWorkbook.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedTabFilter by remember { mutableStateOf("ALL") }
  var isUploading by remember { mutableStateOf(false) }
  var showAdminUploadDialog by remember { mutableStateOf(false) }
  var selectedResultForDetails by remember { mutableStateOf<CompanySearchResult?>(null) }

  val canManageExcel = currentUser.role == "ADMIN" || currentUser.role == "MENTOR"

  // Excel File Picker
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      isUploading = true
      val fileName = try {
        var name = "Company_Profile_Upload.xlsx"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0) name = cursor.getString(nameIdx)
          }
        }
        name
      } catch (_: Exception) {
        "Company_Profile_Upload.xlsx"
      }

      viewModel.uploadCompanyWorkbook(
        context = context,
        uri = uri,
        fileName = fileName,
        uploadedBy = "${currentUser.name} (${currentUser.role})"
      ) { success, msg ->
        isUploading = false
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        if (success) {
          showAdminUploadDialog = false
        }
      }
    }
  }

  // Cross-Tab Search Algorithm
  val searchResults by remember(workbook, searchQuery, selectedTabFilter) {
    derivedStateOf {
      val query = searchQuery.trim().lowercase()
      val results = mutableListOf<CompanySearchResult>()

      val targetSheets = if (selectedTabFilter == "ALL") {
        workbook.sheets
      } else {
        workbook.sheets.filter { it.sheetName == selectedTabFilter }
      }

      for (sheet in targetSheets) {
        for (row in sheet.rows) {
          val rowCombined = row.joinToString(" ").lowercase()
          if (query.isBlank() || rowCombined.contains(query)) {
            results.add(
              CompanySearchResult(
                sheetName = sheet.sheetName,
                headers = sheet.headers,
                rowData = row,
                matchedQuery = query
              )
            )
          }
        }
      }
      results
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
  ) {
    // Top Bar
    Surface(
      color = EblNavyDark,
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_back_company_profile")
          ) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column {
            Text(
              text = "Company Profile (কোম্পানি প্রোফাইল)",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "মাল্টি-ট্যাব এক্সেল ডিরেক্টরি • ${workbook.sheets.size} টি শিট • ${workbook.totalRows} টি কোম্পানি",
              fontSize = 11.sp,
              color = Color(0xFFCBD5E1)
            )
          }
        }

        if (canManageExcel) {
          Button(
            onClick = { showAdminUploadDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_open_excel_manager")
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("আপলোড", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }

    // Prominent Search Bar
    Surface(
      color = Color.White,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "কোম্পানির নাম, ইন্ডাস্ট্রি, এড্রেস বা ফোন লিখুন...",
              fontSize = 13.sp,
              color = Color.Gray
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = EblNavyPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_company_profile_search")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-Sheet Filter Tabs
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = selectedTabFilter == "ALL",
            onClick = { selectedTabFilter = "ALL" },
            label = {
              Text("সকল শিট (${workbook.totalRows})", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EblNavyPrimary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("chip_tab_all")
          )

          workbook.sheets.forEachIndexed { idx, sheet ->
            FilterChip(
              selected = selectedTabFilter == sheet.sheetName,
              onClick = { selectedTabFilter = sheet.sheetName },
              label = {
                Text(
                  text = "${sheet.sheetName.take(24)} (${sheet.rows.size})",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EblNavyPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("chip_tab_$idx")
            )
          }
        }
      }
    }

    // Status & Result Count Banner
    Surface(
      color = Color(0xFFF1F5F9),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (searchQuery.isNotBlank()) {
            "🔍 '${searchQuery}' এর জন্য ${searchResults.size} টি ফলাফল পাওয়া গেছে"
          } else {
            "মোট ${searchResults.size} টি কোম্পানির প্রোফাইল প্রদর্শিত হচ্ছে"
          },
          fontSize = 11.5.sp,
          fontWeight = FontWeight.SemiBold,
          color = EblNavyDark
        )

        Text(
          text = "শিট: ${workbook.fileName.take(22)}",
          fontSize = 10.sp,
          color = Color.Gray,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Search Results List
    if (searchResults.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(60.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "কোনো কোম্পানি পাওয়া যায়নি",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (searchQuery.isNotBlank()) "বানান পরীক্ষা করুন অথবা অন্য কোনো কীওয়ার্ড দিয়ে খুঁজুন।" else "এই শিটে কোনো ডাটা নেই।",
            fontSize = 12.sp,
            color = Color.Gray
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
      ) {
        itemsIndexed(searchResults) { index, result ->
          CompanyResultCard(
            result = result,
            onCardClick = { selectedResultForDetails = result },
            onCopy = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val text = result.headers.zip(result.rowData).joinToString("\n") { "${it.first}: ${it.second}" }
              cm.setPrimaryClip(ClipData.newPlainText("Company Info", text))
              Toast.makeText(context, "কোম্পানির তথ্য কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
            },
            onCall = { phone ->
              try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
                context.startActivity(intent)
              } catch (_: Exception) {}
            }
          )
        }
      }
    }
  }

  // Admin / Mentor Upload & Replace Dialog
  if (showAdminUploadDialog) {
    AlertDialog(
      onDismissRequest = { if (!isUploading) showAdminUploadDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EblNavyPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("কোম্পানি প্রোফাইল শিট ম্যানেজমেন্ট", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "এডমিন বা মেন্টর হিসেবে আপনি নতুন মাল্টি-ট্যাব এক্সেল ফাইল (.xlsx বা .csv) আপলোড করতে পারেন।",
            fontSize = 12.sp,
            color = Color(0xFF475569)
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "⚠️ নতুন এক্সেল শিট আপলোড করলে পূর্বের ফাইলটি স্বয়ংক্রিয়ভাবে মুছে যাবে এবং নতুন ডাটাবেজ কার্যকর হবে।",
                fontSize = 11.5.sp,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          // Current file status
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("বর্তমান সক্রিয় ফাইল:", fontSize = 11.sp, color = Color.Gray)
              Text(workbook.fileName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
              Spacer(modifier = Modifier.height(4.dp))
              Text("ট্যাব সংখ্যা: ${workbook.sheets.size} টি • মোট রেকর্ড: ${workbook.totalRows} টি", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
              Text("আপলোডকারী: ${workbook.uploadedBy}", fontSize = 10.5.sp, color = Color.Gray)
              Text("আপলোড সময়: ${DateUtils.formatDateTime(workbook.uploadedAt)}", fontSize = 10.sp, color = Color.Gray)
            }
          }

          if (isUploading) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = EblNavyPrimary)
              Spacer(modifier = Modifier.width(10.dp))
              Text("ফাইল পার্সিং ও সেভ হচ্ছে...", fontSize = 12.sp, color = EblNavyPrimary)
            }
          } else {
            // Upload Button
            Button(
              onClick = {
                filePickerLauncher.launch("*/*")
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_select_excel_file")
            ) {
              Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("নতুন ফাইল নির্বাচন করুন (.xlsx / .csv)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }

            // Reset to default sample
            OutlinedButton(
              onClick = {
                viewModel.resetCompanyWorkbookToDefault(
                  uploadedBy = "${currentUser.name} (${currentUser.role})"
                ) { success ->
                  if (success) {
                    Toast.makeText(context, "ডিফল্ট EBL কর্পোরেট ডাটাবেজ রিলোড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    showAdminUploadDialog = false
                  }
                }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_reset_default_excel")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ডিফল্ট EBL ডাটাবেজ রিলোড করুন", fontSize = 11.5.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = { showAdminUploadDialog = false },
          enabled = !isUploading
        ) {
          Text("বন্ধ করুন")
        }
      }
    )
  }

  // Row Details Full View Dialog
  if (selectedResultForDetails != null) {
    val res = selectedResultForDetails!!
    AlertDialog(
      onDismissRequest = { selectedResultForDetails = null },
      title = {
        Column {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(0.8.dp, Color(0xFF93C5FD))
          ) {
            Text(
              text = "📑 ${res.sheetName}",
              fontSize = 11.sp,
              color = Color(0xFF1D4ED8),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = res.rowData.firstOrNull() ?: "Company Details",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
        }
      },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(res.headers) { idx, header ->
            val value = if (idx < res.rowData.size) res.rowData[idx] else ""
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(header, fontSize = 10.5.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value.ifBlank { "N/A" }, fontSize = 12.5.sp, fontWeight = FontWeight.Normal, color = EblNavyDark)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = res.headers.zip(res.rowData).joinToString("\n") { "${it.first}: ${it.second}" }
            cm.setPrimaryClip(ClipData.newPlainText("Company Details", text))
            Toast.makeText(context, "তথ্য কপি হয়েছে!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("সব কপি করুন", fontSize = 11.5.sp)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { selectedResultForDetails = null },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("বন্ধ করুন")
        }
      }
    )
  }
}

@Composable
private fun CompanyResultCard(
  result: CompanySearchResult,
  onCardClick: () -> Unit,
  onCopy: () -> Unit,
  onCall: (String) -> Unit
) {
  val title = result.rowData.firstOrNull() ?: "অজ্ঞাত কোম্পানি"
  val subTitle = if (result.rowData.size > 1) result.rowData[1] else ""
  val address = result.rowData.firstOrNull { it.contains("Dhaka", ignoreCase = true) || it.contains("Chittagong", ignoreCase = true) || it.contains("Road", ignoreCase = true) || it.contains("House", ignoreCase = true) || it.contains("Plot", ignoreCase = true) }
  val phone = result.rowData.firstOrNull { it.contains("+880") || (it.startsWith("01") && it.length >= 11) }

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCardClick() }
      .testTag("card_company_${title.take(15)}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tab / Sheet Badge
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFFEFF6FF),
          border = BorderStroke(0.8.dp, Color(0xFF93C5FD))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = result.sheetName,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1D4ED8)
            )
          }
        }

        // Quick Copy Action
        IconButton(
          onClick = onCopy,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = EblNavyDark
      )

      if (subTitle.isNotBlank()) {
        Text(
          text = subTitle,
          fontSize = 11.5.sp,
          color = Color(0xFF475569)
        )
      }

      if (!address.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "📍 $address",
          fontSize = 11.sp,
          color = Color.Gray,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9))
      Spacer(modifier = Modifier.height(6.dp))

      // Bottom Row: Phone Call & Details link
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!phone.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFECFDF5),
            border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
            modifier = Modifier.clickable { onCall(phone) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(phone, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
            }
          }
        } else {
          Text(
            text = "${result.rowData.size} টি কলাম তথ্য",
            fontSize = 10.5.sp,
            color = Color.Gray
          )
        }

        Text(
          text = "বিস্তারিত দেখুন →",
          fontSize = 11.sp,
          color = EblNavyPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
