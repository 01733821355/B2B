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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import com.example.data.model.FileAttachmentEntity
import com.example.ui.common.AttachmentViewerDialog
import com.example.util.AttachmentHelper
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

@OptIn(ExperimentalLayoutApi::class)
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
  var selectedAttachmentForView by remember { mutableStateOf<FileAttachmentEntity?>(null) }

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
          // Status row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ApplicationStatusBadge(file.applicationStatus)
            ActiveStatusBadge(file.activeStatus)
            CpvStatusBadge(file.cpvStatus)
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

          // Attachments
          Text("Attached Documents (${attachments.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

        // Action Buttons: Edit or Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Close")
          }

          Button(
            onClick = {
              onDismiss()
              onEdit(file.fileId)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            modifier = Modifier.weight(1f).testTag("btn_detail_edit")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit File")
          }
        }
      }
    }
  }

  // Attachment Viewer & Downloader Dialog
  val currentAttachment = selectedAttachmentForView
  if (currentAttachment != null) {
    AttachmentViewerDialog(
      attachment = currentAttachment,
      onDismiss = { selectedAttachmentForView = null }
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
