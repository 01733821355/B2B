package com.example.ui.screens.rm

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.draw.clip
import com.example.data.model.FileAttachmentEntity
import com.example.ui.common.AttachmentViewerDialog
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedTextField
import android.widget.Toast
import com.example.util.AttachmentHelper
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.CpvStatusBadge
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailDialog(
  file: CustomerFileEntity,
  currentUser: UserEntity,
  viewModel: AppViewModel,
  onDismiss: () -> Unit,
  onEdit: (String) -> Unit
) {
  val scrollState = rememberScrollState()
  val attachmentsFlow = viewModel.eblRepository.getAttachmentsForFileFlow(file.fileId)
  val attachments by attachmentsFlow.collectAsState(initial = emptyList())
  val allRms by viewModel.allRms.collectAsState()
  var selectedAttachmentForView by remember { mutableStateOf<FileAttachmentEntity?>(null) }
  var showReassignModal by remember { mutableStateOf(false) }
  var showConfirmDeleteModal by remember { mutableStateOf(false) }
  var newSelectedRmCode by remember(file.assignedRmCode) { mutableStateOf(file.assignedRmCode) }
  var isSubmittingReassign by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()
  val context = androidx.compose.ui.platform.LocalContext.current
  val availableStatuses by viewModel.fileStatusOptions.collectAsState()
  var showUpdateStatusDialog by remember { mutableStateOf(false) }
  var selectedNewStatus by remember(file.applicationStatus) { mutableStateOf(file.applicationStatus) }
  var statusRemarksInput by remember(file.remarks) { mutableStateOf(file.remarks) }
  var isUpdatingStatus by remember { mutableStateOf(false) }

  var showAddAttachmentModal by remember { mutableStateOf(false) }
  var selectedUploadCategory by remember { mutableStateOf("Status Update Photo") }
  var uploadDocNameInput by remember { mutableStateOf("") }
  var isProcessingAttachmentUpload by remember { mutableStateOf(false) }

  var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
  var tempCameraFile by remember { mutableStateOf<File?>(null) }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    if (success && tempCameraUri != null) {
      val uri = tempCameraUri!!
      scope.launch {
        isProcessingAttachmentUpload = true
        try {
          val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
            context = context,
            sourceUri = uri,
            preferredCategory = selectedUploadCategory
          )
          val docName = if (uploadDocNameInput.isBlank()) saved.fileName else uploadDocNameInput.trim()
          viewModel.addAttachment(
            fileId = file.fileId,
            category = selectedUploadCategory,
            fileName = docName,
            fileType = saved.fileType,
            fileSizeBytes = saved.fileSizeBytes,
            fileUri = saved.fileUri,
            localFile = saved.file
          )
          showAddAttachmentModal = false
          uploadDocNameInput = ""
        } catch (e: Exception) {
          Toast.makeText(context, "Camera upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
          isProcessingAttachmentUpload = false
          tempCameraFile?.delete()
          tempCameraUri = null
          tempCameraFile = null
        }
      }
    } else {
      tempCameraFile?.delete()
      tempCameraUri = null
      tempCameraFile = null
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      try {
        val (uri, fileObj) = com.example.util.AttachmentHelper.createTempCameraUri(context, "doc_photo")
        tempCameraUri = uri
        tempCameraFile = fileObj
        cameraLauncher.launch(uri)
      } catch (e: Exception) {
        Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
    }
  }

  fun launchCameraFlow() {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      try {
        val (uri, fileObj) = com.example.util.AttachmentHelper.createTempCameraUri(context, "doc_photo")
        tempCameraUri = uri
        tempCameraFile = fileObj
        cameraLauncher.launch(uri)
      } catch (e: Exception) {
        Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isProcessingAttachmentUpload = true
        try {
          val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
            context = context,
            sourceUri = uri,
            preferredCategory = selectedUploadCategory
          )
          val docName = if (uploadDocNameInput.isBlank()) saved.fileName else uploadDocNameInput.trim()
          viewModel.addAttachment(
            fileId = file.fileId,
            category = selectedUploadCategory,
            fileName = docName,
            fileType = saved.fileType,
            fileSizeBytes = saved.fileSizeBytes,
            fileUri = saved.fileUri,
            localFile = saved.file
          )
          showAddAttachmentModal = false
          uploadDocNameInput = ""
        } catch (e: Exception) {
          Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
          isProcessingAttachmentUpload = false
        }
      }
    }
  }

  val docPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isProcessingAttachmentUpload = true
        try {
          val saved = com.example.util.AttachmentHelper.saveUriToInternalStorage(
            context = context,
            sourceUri = uri,
            preferredCategory = selectedUploadCategory
          )
          val docName = if (uploadDocNameInput.isBlank()) saved.fileName else uploadDocNameInput.trim()
          viewModel.addAttachment(
            fileId = file.fileId,
            category = selectedUploadCategory,
            fileName = docName,
            fileType = saved.fileType,
            fileSizeBytes = saved.fileSizeBytes,
            fileUri = saved.fileUri,
            localFile = saved.file
          )
          showAddAttachmentModal = false
          uploadDocNameInput = ""
        } catch (e: Exception) {
          Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
          isProcessingAttachmentUpload = false
        }
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 24.dp)
        .testTag("customer_detail_dialog"),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = file.customerName,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
            Text(
              text = "File ID: ${file.fileId} • RM Code: ${file.assignedRmCode}",
              fontSize = 12.sp,
              color = Color.Gray
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable content
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(scrollState)
        ) {
          // Status row with quick Edit Status affordance
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ApplicationStatusBadge(file.applicationStatus)
              ActiveStatusBadge(file.activeStatus)
              CpvStatusBadge(file.cpvStatus)
            }

            if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR" || currentUser.rmCode == file.assignedRmCode) {
              OutlinedButton(
                onClick = {
                  selectedNewStatus = file.applicationStatus
                  statusRemarksInput = file.remarks
                  showUpdateStatusDialog = true
                },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                  .height(28.dp)
                  .testTag("btn_change_file_status")
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Edit Status", fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Information Table
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              DetailRow("Office / Company", file.companyName)
              DetailRow("Office Address", file.officeAddress)
              if (file.ccNumber.isNotBlank()) DetailRow("CC-Number", file.ccNumber)
              DetailRow("Mobile Number", file.mobile)
              if (file.altMobile.isNotBlank()) DetailRow("Alt Mobile", file.altMobile)
              if (file.email.isNotBlank()) DetailRow("Email Address", file.email)
              DetailRow("Product Type", file.productType)
              DetailRow("Created Date", DateUtils.formatDateTime(file.createdAt))
              DetailRow("Last Updated", DateUtils.formatDateTime(file.updatedAt))
              if (file.submittedAt != null) DetailRow("Submitted Date", DateUtils.formatDateTime(file.submittedAt))
              if (file.approvedAt != null) DetailRow("Approved Date", DateUtils.formatDateTime(file.approvedAt))
              DetailRow("Created By", file.createdBy)
              DetailRow("Updated By", file.updatedBy)
            }
          }

          // Mentor Only: Submission GPS Location Audit
          if (currentUser.role == "MENTOR" && (file.submissionAddress != null || file.submissionLatitude != null)) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFF0FDF4),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Mentor GPS Field Audit (Confidential)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Entry Address: ${file.submissionAddress ?: "Captured via GPS File Entry"}", fontSize = 12.sp, color = Color(0xFF15803D))
                if (file.submissionLatitude != null && file.submissionLongitude != null) {
                  Text("GPS Coords: %.5f, %.5f".format(file.submissionLatitude, file.submissionLongitude), fontSize = 11.sp, color = Color(0xFF166534))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Pending Documents
          Text("Pending Documents Checklist:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(6.dp))
          if (file.pendingDocuments.isBlank()) {
            Text("None. All documents submitted.", fontSize = 12.sp, color = Color(0xFF15803D))
          } else {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              file.pendingDocuments.split(",").filter { it.isNotBlank() }.forEach { doc ->
                Box(
                  modifier = Modifier
                    .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(text = "⚠ $doc", color = Color(0xFFB45309), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Remarks
          if (file.remarks.isNotBlank()) {
            Text("Remarks & Requirements:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Text(file.remarks, fontSize = 12.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          // CPV Details
          Text("Contact Point Verification (CPV):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              DetailRow("CPV Status", file.cpvStatus)
              DetailRow("CPV Date", file.cpvDate.ifBlank { "N/A" })
              DetailRow("CPV Address", file.cpvAddress.ifBlank { "N/A" })
              DetailRow("CPV Remarks", file.cpvRemarks.ifBlank { "N/A" })
              DetailRow("Verified By", file.cpvLastUpdatedBy.ifBlank { file.assignedRmCode })
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Attachments Header with Drive Upload Action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Attached Documents & Photos (${attachments.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
              onClick = { showAddAttachmentModal = true },
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp).testTag("btn_detail_add_attachment")
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Upload Photo / Doc", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          if (attachments.isEmpty()) {
            Text("No attachments uploaded for this file.", fontSize = 12.sp, color = Color.Gray)
          } else {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            attachments.forEach { att ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                  .clickable { selectedAttachmentForView = att }
                  .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (att.fileType.contains("pdf")) Icons.Default.Description else Icons.Default.Image,
                  contentDescription = null,
                  tint = EblNavyPrimary,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  Text("${att.category} • ${(att.fileSizeBytes / 1024).coerceAtLeast(1)} KB", fontSize = 10.sp, color = Color.Gray)
                }

                // Quick View button
                IconButton(
                  onClick = { selectedAttachmentForView = att },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Visibility, contentDescription = "View", tint = EblNavyPrimary, modifier = Modifier.size(16.dp))
                }

                // Quick Download button
                IconButton(
                  onClick = { AttachmentHelper.downloadAttachment(ctx, att) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Reassign RM (Admin/Mentor), Edit or Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Close")
          }

          if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
            OutlinedButton(
              onClick = { showReassignModal = true },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
              modifier = Modifier.weight(1.3f).testTag("btn_detail_reassign_rm")
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reassign RM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          // Delete button for RM, Admin, Mentor
          IconButton(
            onClick = { showConfirmDeleteModal = true },
            modifier = Modifier.size(38.dp).testTag("btn_detail_delete")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
          }

          Button(
            onClick = {
              onDismiss()
              onEdit(file.fileId)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            modifier = Modifier.weight(1.2f).testTag("btn_detail_edit")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit File")
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (showConfirmDeleteModal) {
    AlertDialog(
      onDismissRequest = { showConfirmDeleteModal = false },
      title = { Text("Delete Customer Record", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
      text = {
        Text("Are you sure you want to delete customer file '${file.customerName}' (${file.fileId})? It will be removed from the active list and synchronized with Google Sheets.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteFile(file.fileId)
            showConfirmDeleteModal = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          modifier = Modifier.testTag("btn_confirm_detail_delete")
        ) {
          Text("Delete Record")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showConfirmDeleteModal = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Attachment Viewer & Downloader Dialog
  val currentAttachment = selectedAttachmentForView
  if (currentAttachment != null) {
    AttachmentViewerDialog(
      attachment = currentAttachment,
      onDismiss = { selectedAttachmentForView = null }
    )
  }

  // Reassign RM Confirmation Dialog (Admin & Mentor)
  if (showReassignModal) {
    AlertDialog(
      onDismissRequest = { if (!isSubmittingReassign) showReassignModal = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reassign Customer File RM", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column {
          Text(
            text = "Select the new Relationship Manager (RM) for '${file.customerName}' (${file.fileId}). Once reassigned, the file will be removed from RM ${file.assignedRmCode}'s mobile app, and automatically delivered to the new RM.",
            fontSize = 12.sp,
            color = Color.DarkGray
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text("Currently Assigned: RM ${file.assignedRmCode}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EblNavyDark)
          Spacer(modifier = Modifier.height(8.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            allRms.forEach { rm ->
              val isSelected = rm.rmCode.equals(newSelectedRmCode, ignoreCase = true)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                  .clickable { newSelectedRmCode = rm.rmCode }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                androidx.compose.material3.RadioButton(
                  selected = isSelected,
                  onClick = { newSelectedRmCode = rm.rmCode },
                  colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = EblNavyPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "${rm.name} (${rm.rmCode})",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) EblNavyPrimary else Color.Black
                  )
                  Text(
                    text = "Mobile: ${rm.mobile.ifBlank { "N/A" }} • Status: ${rm.accountStatus}",
                    fontSize = 10.sp,
                    color = Color.Gray
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newSelectedRmCode.isNotBlank() && !newSelectedRmCode.equals(file.assignedRmCode, ignoreCase = true)) {
              isSubmittingReassign = true
              viewModel.reassignCustomerFileRm(file.fileId, newSelectedRmCode) { success, _ ->
                isSubmittingReassign = false
                showReassignModal = false
                if (success) {
                  onDismiss()
                }
              }
            } else {
              showReassignModal = false
            }
          },
          enabled = !isSubmittingReassign && newSelectedRmCode.isNotBlank() && !newSelectedRmCode.equals(file.assignedRmCode, ignoreCase = true),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
          modifier = Modifier.testTag("btn_confirm_reassign_rm")
        ) {
          Text(if (isSubmittingReassign) "Reassigning..." else "Confirm Transfer")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showReassignModal = false },
          enabled = !isSubmittingReassign
        ) {
          Text("Cancel")
        }
      }
    )
  }

  // Update Status Dialog (Admin, Mentor & RM)
  if (showUpdateStatusDialog) {
    AlertDialog(
      onDismissRequest = { if (!isUpdatingStatus) showUpdateStatusDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Update Application Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Customer: ${file.customerName} (${file.fileId})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = EblNavyDark
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text("Select new status (Synced to Google Sheets for all RMs):", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))

          // Status Chips selection
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            availableStatuses.forEach { st ->
              val isSelected = selectedNewStatus.equals(st, ignoreCase = true)
              FilterChip(
                selected = isSelected,
                onClick = { selectedNewStatus = st },
                label = { Text(st, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EblNavyPrimary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = statusRemarksInput,
            onValueChange = { statusRemarksInput = it },
            label = { Text("Admin Remarks / Update Notes", fontSize = 11.sp) },
            placeholder = { Text("e.g. Approved with condition, verified by admin...", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isUpdatingStatus = true
            viewModel.updateFileStatus(file.fileId, selectedNewStatus, statusRemarksInput) { success, err ->
              isUpdatingStatus = false
              if (success) {
                showUpdateStatusDialog = false
                Toast.makeText(context, "Status updated to '$selectedNewStatus' and synced!", Toast.LENGTH_SHORT).show()
                onDismiss()
              } else {
                Toast.makeText(context, err ?: "Failed to update status", Toast.LENGTH_SHORT).show()
              }
            }
          },
          enabled = !isUpdatingStatus,
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          if (isUpdatingStatus) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text("Update Status & Sync")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showUpdateStatusDialog = false },
          enabled = !isUpdatingStatus
        ) {
          Text("Cancel")
        }
      }
    )
  }

  // Upload Photo / Document Modal to Google Drive
  if (showAddAttachmentModal) {
    val categories = listOf("Status Update Photo", "CPV Photo", "NID", "Salary Certificate", "Cheque Leaf", "Other Document")
    AlertDialog(
      onDismissRequest = { if (!isProcessingAttachmentUpload) showAddAttachmentModal = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Upload Photo / Doc to Drive", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Files are saved to your Google Drive folder so all officers and admins can immediately view them.",
            fontSize = 11.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(10.dp))

          Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EblNavyDark)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            categories.forEach { cat ->
              val isSel = selectedUploadCategory == cat
              FilterChip(
                selected = isSel,
                onClick = { selectedUploadCategory = cat },
                label = { Text(cat, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EblNavyPrimary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = uploadDocNameInput,
            onValueChange = { uploadDocNameInput = it },
            label = { Text("Note / Custom Name (Optional)", fontSize = 11.sp) },
            placeholder = { Text("e.g. Salary_PaySlip_June.pdf", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          if (isProcessingAttachmentUpload) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EblNavyPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Uploading file to Google Drive...", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Medium)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 1. Prominent Camera Capture Button
          Button(
            onClick = { launchCameraFlow() },
            enabled = !isProcessingAttachmentUpload,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .testTag("btn_capture_camera_photo")
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("📷 ক্যামেরা দিয়ে ছবি তুলুন (Take Photo)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2. Secondary Options: Gallery Photo & Document File
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                photoPickerLauncher.launch(
                  androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              enabled = !isProcessingAttachmentUpload,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .testTag("btn_pick_photo_gallery")
            ) {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("গ্যালারি (Gallery)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                docPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
              },
              enabled = !isProcessingAttachmentUpload,
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .testTag("btn_pick_doc_file")
            ) {
              Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ফাইল (Doc/PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        OutlinedButton(
          onClick = { showAddAttachmentModal = false },
          enabled = !isProcessingAttachmentUpload
        ) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.width(12.dp))
    Text(value, fontSize = 11.sp, color = EblNavyDark, fontWeight = FontWeight.SemiBold)
  }
}
