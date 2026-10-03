package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileAttachmentEntity
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.util.AttachmentHelper
import com.example.util.DateUtils
import java.io.File

@Composable
fun AttachmentViewerDialog(
  attachment: FileAttachmentEntity,
  onDismiss: () -> Unit,
  onDelete: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  // Ensure file is available locally
  val resolvedFile = remember(attachment) {
    AttachmentHelper.resolveFile(context, attachment)
  }

  var showDeleteConfirm by remember { mutableStateOf(false) }

  val isImage = attachment.fileType.startsWith("image/", ignoreCase = true) ||
    listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp").any { attachment.fileName.endsWith(it, ignoreCase = true) }
  val isPdf = attachment.fileType.contains("pdf", ignoreCase = true) ||
    attachment.fileName.endsWith(".pdf", ignoreCase = true)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 20.dp)
        .testTag("attachment_viewer_dialog"),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Top Bar: Category badge, title, close button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = when (attachment.category) {
                "CPV Photo" -> Color(0xFFFAF5FF)
                "Status Update Photo" -> Color(0xFFF0F9FF)
                "NID" -> Color(0xFFECFDF5)
                "Salary Certificate" -> Color(0xFFFFFBEB)
                else -> Color(0xFFF1F5F9)
              },
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (attachment.category) {
                  "CPV Photo" -> Color(0xFFC084FC)
                  "Status Update Photo" -> Color(0xFF7DD3FC)
                  "NID" -> Color(0xFF6EE7B7)
                  "Salary Certificate" -> Color(0xFFFCD34D)
                  else -> Color(0xFFCBD5E1)
                }
              )
            ) {
              Text(
                text = attachment.category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when (attachment.category) {
                  "CPV Photo" -> Color(0xFF7E22CE)
                  "Status Update Photo" -> Color(0xFF0369A1)
                  "NID" -> Color(0xFF047857)
                  "Salary Certificate" -> Color(0xFFB45309)
                  else -> Color(0xFF475569)
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isImage) "Photo Preview" else if (isPdf) "PDF Document" else "Document Preview",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.Gray
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).testTag("btn_close_viewer")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Document Title & Metadata
        Text(
          text = attachment.fileName,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Size: ${(attachment.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
            fontSize = 11.sp,
            color = Color.Gray
          )
          Text(
            text = "Uploaded: ${DateUtils.formatDateTime(attachment.uploadedAt)}",
            fontSize = 11.sp,
            color = Color.Gray
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Preview Box
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(scrollState),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (isImage) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable {
                  AttachmentHelper.viewAttachment(context, attachment)
                },
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(resolvedFile)
                  .crossfade(true)
                  .build(),
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 320.dp)
              )

              // Tap hint badge
              Surface(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.65f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Tap to Zoom / Fullscreen", color = Color.White, fontSize = 10.sp)
                }
              }
            }
          } else if (isPdf) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFEF2F2))
                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                .clickable {
                  AttachmentHelper.viewAttachment(context, attachment)
                },
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFFEE2E2),
                  modifier = Modifier.size(60.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Description,
                      contentDescription = "PDF Document",
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(36.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "PDF Document Ready",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = "ট্যাপ করে PDF রিডারে ওপেন করুন (Drive / Adobe)",
                  fontSize = 11.sp,
                  color = Color(0xFFB91C1C)
                )
              }
            }
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                .clickable {
                  AttachmentHelper.viewAttachment(context, attachment)
                },
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
              ) {
                Icon(
                  Icons.Default.Description,
                  contentDescription = null,
                  tint = EblNavyPrimary,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Banking Document File",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Text(
                  text = "Format: ${attachment.fileType.ifBlank { "Binary File" }}",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Information box
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Uploaded By RM:", fontSize = 11.sp, color = Color.Gray)
                Text(attachment.uploadedBy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
              Spacer(modifier = Modifier.height(3.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Customer File ID:", fontSize = 11.sp, color = Color.Gray)
                Text(attachment.fileId, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
              Spacer(modifier = Modifier.height(3.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Sync Status:", fontSize = 11.sp, color = Color.Gray)
                Text("Cloud & Local Synced ✓", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Bar: View / Download / Share
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Open External Viewer
          Button(
            onClick = {
              AttachmentHelper.viewAttachment(context, attachment)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("btn_view_external")
          ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ওপেন করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Download / Save Button
          Button(
            onClick = {
              AttachmentHelper.downloadAttachment(context, attachment)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("btn_download_attachment")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ডাউনলোড", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Share Button
          OutlinedButton(
            onClick = {
              AttachmentHelper.shareAttachment(context, attachment)
            },
            modifier = Modifier
              .height(44.dp)
              .testTag("btn_share_attachment")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
          }

          // Delete Button (if authorized)
          if (onDelete != null) {
            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
              modifier = Modifier
                .height(44.dp)
                .testTag("btn_delete_attachment")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (showDeleteConfirm && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("ডকুমেন্ট মুছে ফেলবেন?", fontWeight = FontWeight.Bold) },
      text = { Text("'${attachment.fileName}' ফাইলটি মুছে ফেলতে চান?") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
          Text("হ্যাঁ, মুছুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("বাতিল")
        }
      }
    )
  }
}
