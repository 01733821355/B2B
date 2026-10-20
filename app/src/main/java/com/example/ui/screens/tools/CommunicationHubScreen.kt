package com.example.ui.screens.tools

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.common.EblPullRefreshBox
import com.example.util.PdfReportGenerator
import com.example.util.PdfReportGenerator.EventPdfRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.EventResponseEntity
import com.example.data.model.TeamEventEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.CallUiState
import com.example.util.DateUtils

@Composable
fun CommunicationHubScreen(
  viewModel: AppViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val currentUser by viewModel.currentUser.collectAsState()
  val allUsers by viewModel.allRms.collectAsState()
  val messages by viewModel.teamHubMessages.collectAsState()
  val events by viewModel.teamEvents.collectAsState()
  val callState by viewModel.callState.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()

  var messageInput by remember { mutableStateOf("") }
  var showCreateEventDialog by remember { mutableStateOf(false) }

  var activeResponseEvent by remember { mutableStateOf<TeamEventEntity?>(null) }
  var activeViewEventSubmissions by remember { mutableStateOf<TeamEventEntity?>(null) }
  var eventResponsesList by remember { mutableStateOf<List<EventResponseEntity>>(emptyList()) }

  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  // Dialog: Create Delivery / Team Event
  if (showCreateEventDialog) {
    CreateEventDialog(
      onDismiss = { showCreateEventDialog = false },
      onSubmit = { title, desc, date, allowedFields, closeTime ->
        viewModel.createTeamEvent(title, desc, date, allowedFields, closeTime) { success, _ ->
          if (success) {
            showCreateEventDialog = false
          }
        }
      }
    )
  }

  // Dialog: Submit Response for an Event
  if (activeResponseEvent != null) {
    val evt = activeResponseEvent!!
    SubmitDeliveryResponseDialog(
      event = evt,
      onDismiss = { activeResponseEvent = null },
      onSubmit = { filesCount, reqDate, loc, remarks, customerEntriesJson ->
        viewModel.submitEventResponse(evt.eventId, filesCount, reqDate, loc, remarks, customerEntriesJson) { success, _ ->
          if (success) {
            activeResponseEvent = null
            Toast.makeText(context, "✓ আপনার এন্ট্রি ও কাস্টমার তালিকা সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }

  // Dialog: Admin & Mentor Submissions Detail Table
  if (activeViewEventSubmissions != null) {
    val evt = activeViewEventSubmissions!!
    LaunchedEffect(evt.eventId) {
      viewModel.getEventResponses(evt.eventId) { list ->
        eventResponsesList = list
      }
    }
    ViewSubmissionsDialog(
      event = evt,
      responses = eventResponsesList,
      currentUser = currentUser,
      customAppName = appCustomName,
      onDismiss = { activeViewEventSubmissions = null },
      onCopySummary = {
        val summaryText = buildString {
          appendLine("=== EBL ${evt.title.uppercase()} RESULT SUMMARY ===")
          appendLine("Event: ${evt.title}")
          appendLine("Target Date: ${evt.targetDate}")
          appendLine("Total Submissions: ${eventResponsesList.size} RMs")
          appendLine("Total Files: ${eventResponsesList.sumOf { it.filesCount }}")
          appendLine("----------------------------------------")
          var sl = 1
          eventResponsesList.forEach { r ->
            val customers = try {
              if (r.customerEntriesJson.isNotBlank()) {
                val arr = org.json.JSONArray(r.customerEntriesJson)
                val list = mutableListOf<Pair<String, String>>()
                for (ci in 0 until arr.length()) {
                  val obj = arr.getJSONObject(ci)
                  val n = obj.optString("name").trim()
                  val m = obj.optString("mobile").trim()
                  if (n.isNotBlank() || m.isNotBlank()) {
                    list.add(Pair(n, m))
                  }
                }
                list
              } else emptyList()
            } catch (_: Exception) { emptyList() }

            if (customers.isNotEmpty()) {
              customers.forEach { (cName, cMobile) ->
                appendLine("${sl++}. RM: ${r.rmName} (${r.rmCode}) | Customer: $cName | Mobile: $cMobile | Files: ${r.filesCount} | Date: ${r.requestedDate}")
                val extra = listOf(r.location, r.remarks).filter { it.isNotBlank() }.joinToString(" • ")
                if (extra.isNotBlank()) appendLine("   Loc/Note: $extra")
              }
            } else {
              appendLine("${sl++}. RM: ${r.rmName} (${r.rmCode}) | Customer: - | Mobile: - | Files: ${r.filesCount} | Date: ${r.requestedDate}")
              val extra = listOf(r.location, r.remarks).filter { it.isNotBlank() }.joinToString(" • ")
              if (extra.isNotBlank()) appendLine("   Loc/Note: $extra")
            }
          }
          appendLine("----------------------------------------")
        }
        clipboardManager.setText(AnnotatedString(summaryText))
        Toast.makeText(context, "✓ ইভেন্ট ও কাস্টমার তালিকা (১ কাস্টমার ১ রো) কপি হয়েছে!", Toast.LENGTH_SHORT).show()
      },
      onDownloadPdf = {
        if (currentUser != null) {
          try {
            val pdfFile = PdfReportGenerator.generateEventResultPdfReport(
              context = context,
              event = evt,
              responses = eventResponsesList,
              currentUser = currentUser!!,
              customAppName = appCustomName
            )
            PdfReportGenerator.shareOrViewPdf(context, pdfFile)
            Toast.makeText(context, "✓ Event result PDF report generated & opened!", Toast.LENGTH_SHORT).show()
          } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .testTag("communication_hub_screen")
  ) {
    // Top Bar Header
    Surface(
      color = EblNavyDark,
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
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
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Team Communication Hub",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "লাইভ মেসেজিং ও টিম ইভেন্ট প্ল্যাটফর্ম",
              fontSize = 11.sp,
              color = EblGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          if (currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR") {
            Button(
              onClick = { showCreateEventDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EblGold),
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_top_create_event")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = EblNavyDark, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ নতুন ইভেন্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark, maxLines = 1, softWrap = false)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Team Members Horizontal Strip
        Text("Active Team Members (${allUsers.size} members):", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        val otherUsers = remember(allUsers, currentUser) {
          allUsers.filter { !it.rmCode.trim().equals(currentUser?.rmCode?.trim() ?: "", ignoreCase = true) }
        }
        if (otherUsers.isEmpty()) {
          Text("No other colleagues online right now", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        } else {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(otherUsers) { u ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(Color(0xFF22C55E), CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${u.name.take(12)} (${u.rmCode})",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = u.role,
                    color = EblGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }

    // Quick Action Bar for Creating Event
    Card(
      shape = RoundedCornerShape(0.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f, fill = false),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Event, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Team Events / টিম ইভেন্ট:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = EblNavyDark,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR") {
          OutlinedButton(
            onClick = { showCreateEventDialog = true },
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_create_event")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ নতুন ইভেন্ট", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }
      }
    }

    // Chat Stream & Event Cards
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (messages.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.People, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No messages yet in Team Hub.", color = Color.Gray, fontSize = 13.sp)
              Text("নিচের বাটন দিয়ে মেসেজ বা টিম ইভেন্ট তৈরি করুন।", color = Color.Gray, fontSize = 11.sp)
            }
          }
        }
      }

      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderRmCode.equals(currentUser?.rmCode, ignoreCase = true)

        if (msg.messageType == "EVENT") {
          val evt = if (msg.eventId != null) events.find { it.eventId.equals(msg.eventId, ignoreCase = true) } else null
          if (evt != null) {
            EventCardItem(
              event = evt,
              fallbackMessage = msg,
              isCreator = isMine,
              currentUser = currentUser,
              onOpenSubmit = { targetEvt -> activeResponseEvent = targetEvt },
              onViewSubmissions = { targetEvt -> activeViewEventSubmissions = targetEvt },
              onDeleteEvent = { eventIdToDelete ->
                viewModel.deleteTeamEvent(eventIdToDelete)
              }
            )
          }
        } else if (msg.messageType == "CALL_LOG") {
          // Skip or display as info log
        } else {
          // Standard Message Bubble
          ChatBubbleItem(
            message = msg,
            isMine = isMine,
            onCallSender = {},
            onDelete = {
              viewModel.deleteChatMessage(msg.id)
            }
          )
        }
      }
    }

    // Bottom Message Input Field
    Surface(
      color = Color.White,
      shadowElevation = 8.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = messageInput,
          onValueChange = { messageInput = it },
          placeholder = { Text("টিমের সবার জন্য মেসেজ লিখুন...", fontSize = 13.sp) },
          maxLines = 3,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("input_team_chat_message")
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (messageInput.isNotBlank()) {
              val txt = messageInput.trim()
              messageInput = ""
              viewModel.sendChatMessage(txt)
            }
          },
          modifier = Modifier
            .size(44.dp)
            .background(EblNavyPrimary, CircleShape)
            .testTag("btn_send_chat_message")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
fun ChatBubbleItem(
  message: ChatMessageEntity,
  isMine: Boolean,
  onCallSender: () -> Unit,
  onDelete: () -> Unit = {}
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
  ) {
    if (!isMine) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(EblNavyPrimary, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = message.senderName.take(1).uppercase(),
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Column(
      horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
      modifier = Modifier.widthIn(max = 280.dp)
    ) {
      // Sender Info
      if (!isMine) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${message.senderName} (${message.senderRole})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
      }

      Surface(
        shape = RoundedCornerShape(
          topStart = 12.dp,
          topEnd = 12.dp,
          bottomStart = if (isMine) 12.dp else 2.dp,
          bottomEnd = if (isMine) 2.dp else 12.dp
        ),
        color = if (isMine) EblNavyPrimary else Color.White,
        border = if (isMine) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = message.messageText,
            color = if (isMine) Color.White else Color(0xFF1E293B),
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Message",
                tint = if (isMine) Color.White.copy(alpha = 0.8f) else Color.Gray,
                modifier = Modifier.size(14.dp)
              )
            }
            Text(
              text = DateUtils.formatDateTime(message.timestamp).takeLast(8),
              color = if (isMine) Color.White.copy(alpha = 0.8f) else Color.Gray,
              fontSize = 9.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun EventCardItem(
  event: TeamEventEntity?,
  fallbackMessage: ChatMessageEntity,
  isCreator: Boolean,
  currentUser: UserEntity?,
  onOpenSubmit: (TeamEventEntity) -> Unit,
  onViewSubmissions: (TeamEventEntity) -> Unit,
  onDeleteEvent: (String) -> Unit = {}
) {
  val title = event?.title ?: fallbackMessage.messageText.lines().firstOrNull() ?: "Team Event"
  val targetDate = event?.targetDate ?: "As announced"
  val desc = event?.description ?: fallbackMessage.messageText
  val creatorName = event?.creatorName ?: fallbackMessage.senderName
  val isPrivileged = currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR" || isCreator
  val canDeleteEvent = currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR" || isCreator
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val isExpired = event != null && DateUtils.isEventExpired(event.closeTime, event.targetDate)

  if (showDeleteConfirmDialog && event != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
      title = { Text("ইভেন্ট মুছে ফেলবেন?", fontWeight = FontWeight.Bold) },
      text = {
        Text("আপনি কি নিশ্চিত যে '${event.title}' ইভেন্টটি মুছে ফেলতে চান? এটি অ্যাপ এবং গুগল শিট (Google Sheet) উভয় থেকেই সম্পূর্ণ মুছে যাবে।")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
            onDeleteEvent(event.eventId)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("হ্যাঁ, ডিলিট করুন")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = false }) {
          Text("বাতিল")
        }
      }
    )
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("event_card_${event?.eventId ?: fallbackMessage.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isExpired) Color(0xFFCBD5E1) else Color(0xFF3B82F6)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .background(if (isExpired) Color(0xFF64748B) else Color(0xFF2563EB), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("TEAM EVENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "By $creatorName",
            fontSize = 10.sp,
            color = Color.Gray
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isExpired) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFEE2E2)
            ) {
              Text(
                text = "🔴 CLOSED (সময় শেষ)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          } else if (event != null && event.closeTime.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFEF3C7)
            ) {
              Text(
                text = "⏳ শেষ সময়: ${event.closeTime}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB45309),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          } else if (targetDate.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFDCFCE7)
            ) {
              Text(
                text = "Target: $targetDate",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (canDeleteEvent && event != null) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
              onClick = { showDeleteConfirmDialog = true },
              modifier = Modifier.size(24.dp).testTag("btn_delete_event_${event.eventId}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Event",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = EblNavyDark
      )

      if (desc.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = desc,
          fontSize = 12.sp,
          color = Color(0xFF334155)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // RM Button to submit details
        if (isExpired) {
          Button(
            onClick = {
              Toast.makeText(context, "এই ইভেন্টের সময়সীমা শেষ হয়েছে। নতুন এন্ট্রি বন্ধ রয়েছে।", Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94A3B8)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_submit_event_closed")
          ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("সময় শেষ (এন্ট্রি বন্ধ)", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
          }
        } else {
          Button(
            onClick = {
              if (event != null) onOpenSubmit(event)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_submit_event_details")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("তথ্য এন্ট্রি দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
          }
        }

        // Admin & Mentor (or creator) button to view summary table of all RMs
        if (isPrivileged && event != null) {
          OutlinedButton(
            onClick = { onViewSubmissions(event) },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_view_all_submissions")
          ) {
            Text("সব RM এর তালিকা", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}

// Dialog to create a new team event
@Composable
fun CreateEventDialog(
  onDismiss: () -> Unit,
  onSubmit: (String, String, String, String, String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var targetDate by remember { mutableStateOf("") }
  var closeTime by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }

  // Allowed entry fields configurable by Creator/Admin/Mentor
  var allowCustomers by remember { mutableStateOf(true) }
  var allowCount by remember { mutableStateOf(true) }
  var allowDate by remember { mutableStateOf(true) }
  var allowLocation by remember { mutableStateOf(true) }
  var allowRemarks by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Team Event / নতুন ইভেন্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Event Name / বিষয়ের নাম *") },
          placeholder = { Text("যেমন: Customer Audit / কার্ড বিতরণ / Urgent Notice") },
          modifier = Modifier.fillMaxWidth().testTag("input_event_title")
        )
        OutlinedTextField(
          value = targetDate,
          onValueChange = { targetDate = it },
          label = { Text("Target Date (e.g. 15/10/2026)") },
          placeholder = { Text("DD/MM/YYYY") },
          modifier = Modifier.fillMaxWidth().testTag("input_event_date")
        )
        OutlinedTextField(
          value = closeTime,
          onValueChange = { closeTime = it },
          label = { Text("Event Close Time / সমাপ্তির সময়") },
          placeholder = { Text("যেমন: 17/10/2026 06:00 PM বা 17/10/2026") },
          supportingText = { Text("এই সময় পার হলে আর কেউ নতুন তথ্য এন্ট্রি দিতে পারবে না।", fontSize = 10.sp, color = Color(0xFFDC2626)) },
          modifier = Modifier.fillMaxWidth().testTag("input_event_close_time")
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.clickable {
              val today = DateUtils.formatDate(DateUtils.currentDhakaMillis())
              closeTime = "$today 06:00 PM"
            }
          ) {
            Text(
              text = "আজ ৬টা",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = EblNavyPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.clickable {
              val today = DateUtils.formatDate(DateUtils.currentDhakaMillis())
              closeTime = "$today 11:59 PM"
            }
          ) {
            Text(
              text = "আজ রাত ১২টা",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = EblNavyPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.clickable {
              val tmrw = DateUtils.formatDate(DateUtils.currentDhakaMillis() + 86400000L)
              closeTime = "$tmrw 06:00 PM"
            }
          ) {
            Text(
              text = "কাল ৬টা",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = EblNavyPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Instructions / বিবরণ") },
          placeholder = { Text("ইভেন্টের নিয়মাবলী ও আরএমদের জন্য নির্দেশনা") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("input_event_desc")
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "RM-রা কি কি তথ্য এন্ট্রি দিতে পারবে তা নির্বাচন করুন:",
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = EblNavyDark
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = allowCustomers, onCheckedChange = { allowCustomers = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("একাধিক কাস্টমার নাম ও মোবাইল নম্বর", fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = allowCount, onCheckedChange = { allowCount = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("ফাইলের / কার্ডের সংখ্যা", fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = allowDate, onCheckedChange = { allowDate = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("তারিখ নির্বাচন (Target Date)", fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = allowLocation, onCheckedChange = { allowLocation = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("লোকেশন / ব্রাঞ্চের নাম", fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = allowRemarks, onCheckedChange = { allowRemarks = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("নোট / বিশেষ মন্তব্য", fontSize = 11.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val fieldsList = mutableListOf<String>()
            if (allowCustomers) fieldsList.add("CUSTOMERS")
            if (allowCount) fieldsList.add("COUNT")
            if (allowDate) fieldsList.add("DATE")
            if (allowLocation) fieldsList.add("LOCATION")
            if (allowRemarks) fieldsList.add("REMARKS")
            val allowedFieldsStr = if (fieldsList.isNotEmpty()) fieldsList.joinToString(",") else "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS"
            onSubmit(title.trim(), description.trim(), targetDate.trim(), allowedFieldsStr, closeTime.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("btn_confirm_create_event")
      ) {
        Text("ইভেন্ট পোস্ট করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    }
  )
}

data class CustomerEntryItem(
  val name: String = "",
  val mobile: String = ""
)

// Dialog for RMs to submit their response (with Multiple Customers support)
@Composable
fun SubmitDeliveryResponseDialog(
  event: TeamEventEntity,
  onDismiss: () -> Unit,
  onSubmit: (Int, String, String, String, String) -> Unit
) {
  var filesCountText by remember { mutableStateOf("1") }
  var requestedDate by remember { mutableStateOf(event.targetDate) }
  var location by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  // Check allowed fields configured by Admin/Mentor
  val allowed = event.allowedFields.ifBlank { "CUSTOMERS,COUNT,DATE,LOCATION,REMARKS" }
  val showCustomers = allowed.contains("CUSTOMERS")
  val showCount = allowed.contains("COUNT")
  val showDate = allowed.contains("DATE")
  val showLocation = allowed.contains("LOCATION")
  val showRemarks = allowed.contains("REMARKS")

  // Dynamic customer list (Name and Mobile)
  val customerList = remember {
    mutableStateListOf(CustomerEntryItem())
  }
  val isExpired = DateUtils.isEventExpired(event.closeTime, event.targetDate)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ইভেন্ট তথ্য এন্ট্রি দিন: ${event.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (isExpired) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEE2E2),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("এই ইভেন্টের সময়সীমা শেষ হয়েছে। আর কোনো এন্ট্রি দেওয়া সম্ভব নয়।", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        if (showCustomers) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "কাস্টমার তালিকা (Customer List):",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = EblNavyDark
            )
            TextButton(
              onClick = {
                customerList.add(CustomerEntryItem())
              }
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("+ কাস্টমার যোগ করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          customerList.forEachIndexed { idx, item ->
            Card(
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("গ্রাহক #${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
                  if (customerList.size > 1) {
                    IconButton(
                      onClick = { customerList.removeAt(idx) },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(14.dp))
                    }
                  }
                }
                OutlinedTextField(
                  value = item.name,
                  onValueChange = { customerList[idx] = item.copy(name = it) },
                  label = { Text("Customer Name") },
                  placeholder = { Text("কাস্টমার নাম") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = item.mobile,
                  onValueChange = { customerList[idx] = item.copy(mobile = it) },
                  label = { Text("Mobile Number") },
                  placeholder = { Text("01XXXXXXXXX") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        if (showCount) {
          OutlinedTextField(
            value = filesCountText,
            onValueChange = { filesCountText = it },
            label = { Text("কয়টি ফাইল / কার্ড লাগবে?") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (showDate) {
          OutlinedTextField(
            value = requestedDate,
            onValueChange = { requestedDate = it },
            label = { Text("প্রয়োজনীয় তারিখ (Target Date)") },
            placeholder = { Text("DD/MM/YYYY") },
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (showLocation) {
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("ডেলিভারি লোকেশন / ব্রাঞ্চ") },
            placeholder = { Text("e.g. Gulshan Branch / Customer Office") },
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (showRemarks) {
          OutlinedTextField(
            value = remarks,
            onValueChange = { remarks = it },
            label = { Text("বিশেষ নোট / মন্তব্য") },
            maxLines = 3,
            placeholder = { Text("ফাইল নম্বর ও অতিরিক্ত তথ্য") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!isExpired) {
            val cnt = filesCountText.toIntOrNull() ?: 1
            val validCustomers = customerList.filter {
              it.name.isNotBlank() || it.mobile.isNotBlank()
            }
            val customersJson = if (validCustomers.isNotEmpty()) {
              val arr = org.json.JSONArray()
              for (c in validCustomers) {
                arr.put(org.json.JSONObject().apply {
                  put("name", c.name.trim())
                  put("mobile", c.mobile.trim())
                })
              }
              arr.toString()
            } else ""

            onSubmit(cnt, requestedDate.trim(), location.trim(), remarks.trim(), customersJson)
          }
        },
        enabled = !isExpired,
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
      ) {
        Text(if (isExpired) "সময়সীমা শেষ (এন্ট্রি বন্ধ)" else "সাবমিট করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    }
  )
}

// Dialog for Admin & Mentor to view all RM submissions in detail
@Composable
fun ViewSubmissionsDialog(
  event: TeamEventEntity,
  responses: List<EventResponseEntity>,
  currentUser: UserEntity?,
  customAppName: String = "",
  onDismiss: () -> Unit,
  onCopySummary: () -> Unit,
  onDownloadPdf: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val totalFiles = remember(responses) { responses.sumOf { it.filesCount } }

  // Calculate total customers
  val totalCustomers = remember(responses) {
    responses.sumOf { r ->
      if (r.customerEntriesJson.isNotBlank()) {
        try {
          org.json.JSONArray(r.customerEntriesJson).length()
        } catch (_: Exception) { 0 }
      } else 0
    }
  }

  val filteredResponses = remember(responses, searchQuery) {
    if (searchQuery.isBlank()) responses else {
      val q = searchQuery.trim().lowercase()
      responses.filter { r ->
        r.rmName.lowercase().contains(q) ||
        r.rmCode.lowercase().contains(q) ||
        r.location.lowercase().contains(q) ||
        r.customerEntriesJson.lowercase().contains(q) ||
        r.remarks.lowercase().contains(q)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Event Results: ${event.title}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = EblNavyDark
          )
          Text(
            text = "Target Date: ${event.targetDate.ifBlank { "Not set" }} • Created by ${event.creatorName}",
            fontSize = 10.sp,
            color = Color.Gray
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = onDownloadPdf,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_download_event_pdf")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
          }
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(onClick = onCopySummary) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = EblNavyPrimary)
          }
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // 1. KPI Statistics Overview Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFEFF6FF),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Submissions", fontSize = 9.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Medium)
              Text("${responses.size} RMs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
            }
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFDCFCE7),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Total Files", fontSize = 9.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
              Text("$totalFiles Files", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
            }
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Customers", fontSize = 9.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
              Text("$totalCustomers Listed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Search Field
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filter by RM name, code, customer, location...", fontSize = 11.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
          singleLine = true,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_filter_event_submissions")
        )

        Spacer(modifier = Modifier.height(8.dp))

        var viewMode by remember { mutableStateOf(0) } // 0 = Customer Table (১ কাস্টমার ১ রো), 1 = RM কার্ড ভিউ

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = viewMode == 0,
            onClick = { viewMode = 0 },
            label = { Text("কাস্টমার তালিকা (১ রো ১ জন)", fontSize = 11.sp, fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = {
              Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(14.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EblNavyPrimary,
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = Color.White
            ),
            modifier = Modifier.weight(1f).testTag("tab_event_customer_table")
          )
          FilterChip(
            selected = viewMode == 1,
            onClick = { viewMode = 1 },
            label = { Text("RM কার্ড ভিউ", fontSize = 11.sp, fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EblNavyPrimary,
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = Color.White
            ),
            modifier = Modifier.weight(1f).testTag("tab_event_rm_cards")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Submissions Stream
        if (filteredResponses.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (responses.isEmpty()) "এখনও কোনো RM রেসপন্স সাবমিট করেনি।" else "কোনো রেসপন্স ফিল্টারে মিল পাওয়া যায়নি।",
              color = Color.Gray,
              fontSize = 12.sp
            )
          }
        } else if (viewMode == 0) {
          // Flat 1-Customer-per-Row Table View
          val flatCustomerList = remember(filteredResponses) {
            val list = mutableListOf<EventPdfRow>()
            var sl = 1
            filteredResponses.forEach { r ->
              val customers = mutableListOf<Pair<String, String>>()
              if (r.customerEntriesJson.isNotBlank()) {
                try {
                  val arr = org.json.JSONArray(r.customerEntriesJson)
                  for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val n = o.optString("name").trim()
                    val m = o.optString("mobile").trim()
                    if (n.isNotBlank() || m.isNotBlank()) {
                      customers.add(Pair(n, m))
                    }
                  }
                } catch (_: Exception) {}
              }
              val locRem = listOf(r.location.trim(), r.remarks.trim()).filter { it.isNotBlank() }.joinToString(" • ")
              if (customers.isNotEmpty()) {
                customers.forEach { (cName, cMobile) ->
                  list.add(
                    EventPdfRow(
                      sl = sl++,
                      rmDisplay = "${r.rmName} (${r.rmCode})",
                      customerName = cName.ifBlank { "-" },
                      customerMobile = cMobile.ifBlank { "-" },
                      filesCount = r.filesCount,
                      targetDate = r.requestedDate.ifBlank { "-" },
                      locationAndRemarks = locRem.ifBlank { "-" }
                    )
                  )
                }
              } else {
                list.add(
                  EventPdfRow(
                    sl = sl++,
                    rmDisplay = "${r.rmName} (${r.rmCode})",
                    customerName = "-",
                    customerMobile = "-",
                    filesCount = r.filesCount,
                    targetDate = r.requestedDate.ifBlank { "-" },
                    locationAndRemarks = locRem.ifBlank { "-" }
                  )
                )
              }
            }
            list
          }

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(320.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(flatCustomerList, key = { it.sl }) { cItem ->
              Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("event_customer_row_${cItem.sl}")
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(end = 6.dp)
                      ) {
                        Text(
                          text = "#${cItem.sl}",
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.DarkGray,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                      Text(
                        text = cItem.customerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                      )
                    }

                    if (cItem.customerMobile != "-") {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDBEAFE)
                      ) {
                        Text(
                          text = "📞 ${cItem.customerMobile}",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF1E40AF),
                          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "RM: ${cItem.rmDisplay}",
                      fontSize = 10.sp,
                      color = EblNavyPrimary,
                      fontWeight = FontWeight.Medium
                    )
                    Text(
                      text = "${cItem.filesCount} Files • Date: ${cItem.targetDate}",
                      fontSize = 10.sp,
                      color = Color.Gray
                    )
                  }

                  if (cItem.locationAndRemarks != "-") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "📍 ${cItem.locationAndRemarks}",
                      fontSize = 10.sp,
                      color = Color.DarkGray
                    )
                  }
                }
              }
            }
          }
        } else {
          // Grouped RM Cards View
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredResponses, key = { it.responseId }) { r ->
              val customers = remember(r.customerEntriesJson) {
                if (r.customerEntriesJson.isNotBlank()) {
                  try {
                    val arr = org.json.JSONArray(r.customerEntriesJson)
                    val l = mutableListOf<Pair<String, String>>()
                    for (i in 0 until arr.length()) {
                      val o = arr.getJSONObject(i)
                      l.add(Pair(o.optString("name"), o.optString("mobile")))
                    }
                    l
                  } catch (_: Exception) { emptyList() }
                } else emptyList()
              }

              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("event_submission_item_${r.responseId}")
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text("${r.rmName} (${r.rmCode})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EblNavyDark)
                      Text("Submitted: ${DateUtils.formatDateTime(r.submittedAt)}", fontSize = 9.sp, color = Color.Gray)
                    }
                    Surface(
                      color = Color(0xFFDCFCE7),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        "${r.filesCount} Files",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }

                  if (r.requestedDate.isNotBlank() || r.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      if (r.requestedDate.isNotBlank()) {
                        Text("📅 তারিখ: ${r.requestedDate}", fontSize = 11.sp, color = Color.DarkGray)
                      }
                      if (r.location.isNotBlank()) {
                        Text("📍 লোকেশন: ${r.location}", fontSize = 11.sp, color = Color.DarkGray)
                      }
                    }
                  }

                  // Multiple Customers Section - 1 Row per Customer with complete name & number
                  if (customers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFFEFF6FF),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(8.dp)) {
                        Text("👥 কাস্টমার তালিকা (${customers.size} জন):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        customers.forEachIndexed { cIdx, cust ->
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "${cIdx + 1}. ${cust.first}",
                              fontSize = 11.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = Color(0xFF0F172A),
                              modifier = Modifier.weight(1f)
                            )
                            if (cust.second.isNotBlank()) {
                              Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDBEAFE),
                                modifier = Modifier.padding(start = 6.dp)
                              ) {
                                Text(
                                  text = "📞 ${cust.second}",
                                  fontSize = 10.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = Color(0xFF1E40AF),
                                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  if (r.remarks.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("নোট: ${r.remarks}", fontSize = 11.sp, color = Color(0xFF0369A1))
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDownloadPdf,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_modal_download_pdf")
      ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("PDF রিপোর্ট ডাউনলোড", fontSize = 12.sp)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
    }
  )
}

