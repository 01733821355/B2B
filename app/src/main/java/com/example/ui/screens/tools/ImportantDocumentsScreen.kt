package com.example.ui.screens.tools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.ImportantDocumentEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.AttachmentHelper
import com.example.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportantDocumentsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val documents by viewModel.importantDocuments.collectAsState()

  val isPrivileged = currentUser.role == "ADMIN" || currentUser.role == "MENTOR"

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  var showUploadDialog by remember { mutableStateOf(false) }
  var documentToEdit by remember { mutableStateOf<ImportantDocumentEntity?>(null) }
  var documentToDelete by remember { mutableStateOf<ImportantDocumentEntity?>(null) }
  var previewDocument by remember { mutableStateOf<ImportantDocumentEntity?>(null) }
  var isSyncing by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    isSyncing = true
    try {
      viewModel.triggerGoogleSheetsSync()
    } catch (_: Exception) {}
    isSyncing = false
  }

  val categories = listOf(
    "All",
    "Policies & Circulars",
    "Forms & Formats",
    "Product Guidelines",
    "CPV & Compliance",
    "Notices & Announcements",
    "Other"
  )

  val filteredDocs = remember(documents, searchQuery, selectedCategory) {
    val q = searchQuery.trim().lowercase()
    documents.filter { doc ->
      !doc.isDeleted &&
        (selectedCategory == "All" || doc.category.equals(selectedCategory, ignoreCase = true)) &&
        (q.isBlank() ||
          doc.title.lowercase().contains(q) ||
          doc.fileName.lowercase().contains(q) ||
          doc.description.lowercase().contains(q) ||
          doc.category.lowercase().contains(q))
    }.sortedByDescending { it.updatedAt }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .testTag("important_documents_screen")
  ) {
    // Header Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("btn_back_important_docs")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EblNavyDark)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
          Text(
            text = "Important Documents",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Text(
            text = "Bank Policies, Forms & Guidelines Repository",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Sync with Google Sheets button
        IconButton(
          onClick = {
            coroutineScope.launch {
              isSyncing = true
              viewModel.triggerGoogleSheetsSync()
              isSyncing = false
              Toast.makeText(context, "গুগল শিট থেকে ডকুমেন্টস সিঙ্ক হয়েছে!", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.testTag("btn_sync_important_docs")
        ) {
          Icon(
            Icons.Default.Sync,
            contentDescription = "Sync from Sheets",
            tint = if (isSyncing) EblGold else EblNavyPrimary,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Upload button for Admin & Mentor ONLY
        if (isPrivileged) {
          Button(
            onClick = {
              documentToEdit = null
              showUploadDialog = true
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = EblGold,
              contentColor = EblNavyDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_add_important_doc")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Upload Doc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          // Read-only indicator badge for RM officers
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Verified Repository", fontSize = 10.sp, color = EblNavyPrimary, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_important_docs"),
      placeholder = { Text("Search by document title, keywords or circular...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EblNavyPrimary) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(10.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EblNavyPrimary,
        unfocusedBorderColor = Color.LightGray,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Category Filter Chips
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      categories.forEach { cat ->
        val isSelected = selectedCategory == cat
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EblNavyDark,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.testTag("filter_chip_${cat.replace(" ", "_")}")
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Documents Count & Privileges Banner
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Available Documents (${filteredDocs.size})",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = EblNavyDark
      )

      if (isPrivileged) {
        Text(
          text = "• Admin/Mentor Manage Mode",
          fontSize = 11.sp,
          color = Color(0xFF059669),
          fontWeight = FontWeight.SemiBold
        )
      } else {
        Text(
          text = "• Download & View Mode",
          fontSize = 11.sp,
          color = Color(0xFF6B7280),
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Document List
    if (filteredDocs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Description, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(54.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No documents match your filter." else "No documents uploaded yet.",
            color = Color.Gray,
            fontSize = 14.sp
          )
          if (isPrivileged && searchQuery.isBlank() && selectedCategory == "All") {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { showUploadDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
            ) {
              Text("Upload First Document")
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredDocs, key = { it.docId }) { doc ->
          ImportantDocCard(
            doc = doc,
            isPrivileged = isPrivileged,
            onOpen = {
              val attachmentEntity = FileAttachmentEntity(
                attachmentId = doc.docId,
                fileId = "IMPORTANT_DOC",
                category = doc.category,
                fileName = doc.fileName,
                fileType = doc.fileType,
                fileSizeBytes = doc.fileSizeBytes,
                fileUri = doc.fileUri,
                storagePath = doc.storagePath,
                uploadedBy = doc.uploadedBy,
                uploadedAt = doc.createdAt
              )
              AttachmentHelper.viewAttachment(context, attachmentEntity)
            },
            onDownload = {
              if (!isPrivileged) {
                Toast.makeText(context, "Download restricted: View-only access. Only Admin or Mentor can download official documents.", Toast.LENGTH_LONG).show()
                return@ImportantDocCard
              }
              val attachmentEntity = FileAttachmentEntity(
                attachmentId = doc.docId,
                fileId = "IMPORTANT_DOC",
                category = doc.category,
                fileName = doc.fileName,
                fileType = doc.fileType,
                fileSizeBytes = doc.fileSizeBytes,
                fileUri = doc.fileUri,
                storagePath = doc.storagePath,
                uploadedBy = doc.uploadedBy,
                uploadedAt = doc.createdAt
              )
              AttachmentHelper.downloadAttachment(context, attachmentEntity)
            },
            onShare = {
              if (!isPrivileged) {
                Toast.makeText(context, "Share restricted: Only Admin or Mentor can share official documents.", Toast.LENGTH_SHORT).show()
                return@ImportantDocCard
              }
              val attachmentEntity = FileAttachmentEntity(
                attachmentId = doc.docId,
                fileId = "IMPORTANT_DOC",
                category = doc.category,
                fileName = doc.fileName,
                fileType = doc.fileType,
                fileSizeBytes = doc.fileSizeBytes,
                fileUri = doc.fileUri,
                storagePath = doc.storagePath,
                uploadedBy = doc.uploadedBy,
                uploadedAt = doc.createdAt
              )
              AttachmentHelper.shareAttachment(context, attachmentEntity)
            },
            onEdit = {
              documentToEdit = doc
              showUploadDialog = true
            },
            onDelete = {
              documentToDelete = doc
            }
          )
        }
      }
    }
  }

  // Upload or Edit Dialog (Admin & Mentor Only)
  if (showUploadDialog) {
    UploadOrEditDocDialog(
      existingDoc = documentToEdit,
      currentUser = currentUser,
      onDismiss = {
        showUploadDialog = false
        documentToEdit = null
      },
      onSave = { title, category, description, fileName, fileType, fileSizeBytes, fileUri, storagePath, docId ->
        viewModel.saveImportantDocument(
          title = title,
          category = category,
          description = description,
          fileName = fileName,
          fileType = fileType,
          fileSizeBytes = fileSizeBytes,
          fileUri = fileUri,
          storagePath = storagePath,
          docId = docId
        ) { success, err ->
          if (success) {
            showUploadDialog = false
            documentToEdit = null
            Toast.makeText(context, "ডকুমেন্ট সার্বজনীনভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, err ?: "ডকুমেন্ট সেভ করতে সমস্যা হয়েছে।", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  // Delete Confirmation Dialog (Admin & Mentor Only)
  if (documentToDelete != null) {
    AlertDialog(
      onDismissRequest = { documentToDelete = null },
      title = { Text("Confirm Document Deletion", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to delete '${documentToDelete?.title}' universally for all users?\nThis will remove it from all RM officers' screens and Google Sheets.")
      },
      confirmButton = {
        Button(
          onClick = {
            val targetId = documentToDelete?.docId ?: return@Button
            viewModel.deleteImportantDocument(targetId) { success, err ->
              documentToDelete = null
              if (success) {
                Toast.makeText(context, "ডকুমেন্ট মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, err ?: "ডিলিট করতে সমস্যা হয়েছে।", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Delete Universally", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { documentToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun ImportantDocCard(
  doc: ImportantDocumentEntity,
  isPrivileged: Boolean,
  onOpen: () -> Unit,
  onDownload: () -> Unit,
  onShare: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isPdf = doc.fileName.endsWith(".pdf", ignoreCase = true) || doc.fileType.contains("pdf", ignoreCase = true)
  val formattedSize = remember(doc.fileSizeBytes) {
    when {
      doc.fileSizeBytes >= 1024 * 1024 -> String.format("%.1f MB", doc.fileSizeBytes / (1024.0 * 1024.0))
      doc.fileSizeBytes >= 1024 -> String.format("%.0f KB", doc.fileSizeBytes / 1024.0)
      else -> "${doc.fileSizeBytes} B"
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("important_doc_${doc.docId}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.Top
        ) {
          // File Icon Container
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isPdf) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPdf) Icons.Default.Description else Icons.Default.Image,
                contentDescription = null,
                tint = if (isPdf) Color(0xFFDC2626) else Color(0xFF0284C7),
                modifier = Modifier.size(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            // Custom Name / Title
            Text(
              text = doc.title,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Category badge & File size
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFF1F5F9)
              ) {
                Text(
                  text = doc.category,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = EblNavyPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }

              Text(
                text = "${if (isPdf) "PDF" else "IMAGE"} • $formattedSize",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Action Buttons for Edit/Delete (Privileged only)
        if (isPrivileged) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onEdit,
              modifier = Modifier.size(32.dp).testTag("btn_edit_${doc.docId}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Document", tint = EblNavyPrimary, modifier = Modifier.size(16.dp))
            }
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(32.dp).testTag("btn_delete_${doc.docId}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Document", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Optional Description
      if (doc.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = doc.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer Meta: Uploader and Timestamp
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Saved by: ${doc.uploaderName} (${doc.uploaderRole})",
            fontSize = 10.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.Medium
          )
          Text(
            text = DateUtils.formatDateTime(doc.updatedAt),
            fontSize = 10.sp,
            color = Color(0xFF64748B)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Actions Row: Open is available to everyone; Download & Share ONLY for Admin & Mentor
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onOpen,
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier.weight(1.2f).testTag("btn_open_${doc.docId}")
        ) {
          Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("View / Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }

        if (isPrivileged) {
          OutlinedButton(
            onClick = onDownload,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("btn_download_${doc.docId}")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }

          IconButton(
            onClick = onShare,
            modifier = Modifier.size(36.dp).border(1.dp, Color.LightGray, CircleShape).testTag("btn_share_${doc.docId}")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = EblNavyDark, modifier = Modifier.size(16.dp))
          }
        } else {
          // View-only indicator for non-admin/non-mentor users (downloading is strictly disabled)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "View Only",
                fontSize = 11.sp,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadOrEditDocDialog(
  existingDoc: ImportantDocumentEntity?,
  currentUser: UserEntity,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    category: String,
    description: String,
    fileName: String,
    fileType: String,
    fileSizeBytes: Long,
    fileUri: String,
    storagePath: String,
    docId: String?
  ) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var title by remember { mutableStateOf(existingDoc?.title ?: "") }
  var category by remember { mutableStateOf(existingDoc?.category ?: "Policies & Circulars") }
  var description by remember { mutableStateOf(existingDoc?.description ?: "") }

  var selectedFileName by remember { mutableStateOf(existingDoc?.fileName ?: "") }
  var selectedFileType by remember { mutableStateOf(existingDoc?.fileType ?: "application/pdf") }
  var selectedFileSizeBytes by remember { mutableStateOf(existingDoc?.fileSizeBytes ?: 0L) }
  var selectedFileUri by remember { mutableStateOf(existingDoc?.fileUri ?: "") }
  var selectedStoragePath by remember { mutableStateOf(existingDoc?.storagePath ?: "") }

  var isPickingFile by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var categoryExpanded by remember { mutableStateOf(false) }

  val categories = listOf(
    "Policies & Circulars",
    "Forms & Formats",
    "Product Guidelines",
    "CPV & Compliance",
    "Notices & Announcements",
    "Other"
  )

  // Photo / Picture Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isPickingFile = true
        try {
          val saved = AttachmentHelper.saveUriToInternalStorage(
            context = context,
            sourceUri = uri,
            preferredCategory = category
          )
          selectedFileName = saved.fileName
          selectedFileType = saved.fileType
          selectedFileSizeBytes = saved.fileSizeBytes
          selectedFileUri = saved.fileUri
          selectedStoragePath = saved.storagePath
          if (title.isBlank()) {
            title = saved.fileName.substringBeforeLast('.')
          }
        } catch (e: Exception) {
          errorMessage = "Failed to load picture: ${e.message}"
        } finally {
          isPickingFile = false
        }
      }
    }
  }

  // Document (PDF / Doc) Picker
  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isPickingFile = true
        try {
          val saved = AttachmentHelper.saveUriToInternalStorage(
            context = context,
            sourceUri = uri,
            preferredCategory = category
          )
          selectedFileName = saved.fileName
          selectedFileType = saved.fileType
          selectedFileSizeBytes = saved.fileSizeBytes
          selectedFileUri = saved.fileUri
          selectedStoragePath = saved.storagePath
          if (title.isBlank()) {
            title = saved.fileName.substringBeforeLast('.')
          }
        } catch (e: Exception) {
          errorMessage = "Failed to load document: ${e.message}"
        } finally {
          isPickingFile = false
        }
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingDoc == null) "Upload Important Document" else "Edit Important Document",
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = EblNavyDark
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Saved documents are universally accessible to all RM sales officers in real-time.",
          fontSize = 11.sp,
          color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Custom Title / Document Name
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            errorMessage = null
          },
          label = { Text("Custom Document Name *") },
          placeholder = { Text("e.g. EBL Credit Card Policy Circular 2026") },
          modifier = Modifier.fillMaxWidth().testTag("input_doc_title"),
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Dropdown
        ExposedDropdownMenuBox(
          expanded = categoryExpanded,
          onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Document Category *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_doc_category"),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
          )

          ExposedDropdownMenu(
            expanded = categoryExpanded,
            onDismissRequest = { categoryExpanded = false }
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat, fontSize = 13.sp) },
                onClick = {
                  category = cat
                  categoryExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Description / Summary
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Summary (Optional)") },
          placeholder = { Text("Summary of the circular, policy guidelines, or required format...") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("input_doc_description"),
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EblNavyPrimary)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // File Selection Section (Picture or PDF)
        Text("Attach File (Picture or PDF) *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("btn_pick_picture")
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pick Picture", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              documentPickerLauncher.launch(
                arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*")
              )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("btn_pick_pdf")
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pick PDF/Doc", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
          }
        }

        if (isPickingFile) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Processing file...", fontSize = 11.sp, color = Color.Gray)
          }
        }

        // Selected File Preview Card
        if (selectedFileName.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val isPdf = selectedFileName.endsWith(".pdf", ignoreCase = true)
              Icon(
                imageVector = if (isPdf) Icons.Default.Description else Icons.Default.Image,
                contentDescription = null,
                tint = if (isPdf) Color(0xFFDC2626) else Color(0xFF0284C7),
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(selectedFileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                  text = if (selectedFileSizeBytes > 0) "${selectedFileSizeBytes / 1024} KB" else "Ready to upload",
                  fontSize = 10.sp,
                  color = Color.Gray
                )
              }
            }
          }
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            errorMessage = "Please enter a Custom Document Name."
            return@Button
          }
          if (selectedFileName.isBlank()) {
            errorMessage = "Please pick a Picture or PDF document."
            return@Button
          }

          onSave(
            title.trim(),
            category.trim(),
            description.trim(),
            selectedFileName.trim(),
            selectedFileType.trim(),
            selectedFileSizeBytes,
            selectedFileUri.trim(),
            selectedStoragePath.trim(),
            existingDoc?.docId
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyDark),
        modifier = Modifier.testTag("btn_confirm_save_doc")
      ) {
        Text(if (existingDoc == null) "Save & Publish Universally" else "Update Document")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
