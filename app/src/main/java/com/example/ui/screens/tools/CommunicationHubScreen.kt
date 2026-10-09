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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

  var messageInput by remember { mutableStateOf("") }
  var showCreateEventDialog by remember { mutableStateOf(false) }
  var showStartCallDialog by remember { mutableStateOf(false) }
  var showStartGroupCallDialog by remember { mutableStateOf(false) }

  var activeResponseEvent by remember { mutableStateOf<TeamEventEntity?>(null) }
  var activeViewEventSubmissions by remember { mutableStateOf<TeamEventEntity?>(null) }
  var eventResponsesList by remember { mutableStateOf<List<EventResponseEntity>>(emptyList()) }

  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  // Active Call Overlay (for single call or group call)
  if (callState !is CallUiState.Idle) {
    ActiveCallModal(
      callState = callState,
      onToggleMute = { viewModel.toggleMute() },
      onToggleSpeaker = { viewModel.toggleSpeaker() },
      onEndCall = { viewModel.endCall() }
    )
  }

  // Dialog: Create Delivery / Team Event
  if (showCreateEventDialog) {
    CreateEventDialog(
      onDismiss = { showCreateEventDialog = false },
      onSubmit = { title, desc, date ->
        viewModel.createTeamEvent(title, desc, date) { success, _ ->
          if (success) {
            showCreateEventDialog = false
          }
        }
      }
    )
  }

  // Dialog: Submit Delivery Response for an Event
  if (activeResponseEvent != null) {
    val evt = activeResponseEvent!!
    SubmitDeliveryResponseDialog(
      event = evt,
      onDismiss = { activeResponseEvent = null },
      onSubmit = { filesCount, reqDate, loc, remarks ->
        viewModel.submitEventResponse(evt.eventId, filesCount, reqDate, loc, remarks) { success, _ ->
          if (success) {
            activeResponseEvent = null
            Toast.makeText(context, "✓ আপনার হ্যান্ড ডেলিভারির তথ্য সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
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
      onDismiss = { activeViewEventSubmissions = null },
      onCopySummary = {
        val summaryText = buildString {
          appendLine("=== EBL HAND DELIVERY EVENT SUMMARY ===")
          appendLine("Event: ${evt.title}")
          appendLine("Target Date: ${evt.targetDate}")
          appendLine("Total Submissions: ${eventResponsesList.size} RMs")
          appendLine("Total Files: ${eventResponsesList.sumOf { it.filesCount }}")
          appendLine("----------------------------------------")
          eventResponsesList.forEachIndexed { i, r ->
            appendLine("${i + 1}. RM: ${r.rmName} (${r.rmCode}) | Date: ${r.requestedDate} | Files: ${r.filesCount}")
            if (r.location.isNotBlank()) appendLine("   Location: ${r.location}")
            if (r.remarks.isNotBlank()) appendLine("   Remarks: ${r.remarks}")
          }
        }
        clipboardManager.setText(AnnotatedString(summaryText))
        Toast.makeText(context, "✓ রিমার্কস ও ডেলিভারি তালিকা ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Dialog: Choose user to Call
  if (showStartCallDialog) {
    SelectUserToCallDialog(
      users = allUsers.filter { it.rmCode != currentUser?.rmCode },
      onDismiss = { showStartCallDialog = false },
      onSelectUser = { target ->
        showStartCallDialog = false
        viewModel.startCall(target)
      }
    )
  }

  // Dialog: Start Group Voice Call
  if (showStartGroupCallDialog) {
    StartGroupCallDialog(
      onDismiss = { showStartGroupCallDialog = false },
      onStart = { groupTitle ->
        showStartGroupCallDialog = false
        viewModel.startGroupCall(groupTitle)
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
          Column {
            Text(
              text = "Team Communication & Net Calling",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "লাইভ মেসেজ, নেট ভয়েস কলিং এবং হ্যান্ড ডেলিভারি ইভেন্ট",
              fontSize = 11.sp,
              color = EblGold
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Group Call Button
            Button(
              onClick = { showStartGroupCallDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_group_call")
            ) {
              Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("গ্রুপ কল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Direct Call Button
            Button(
              onClick = { showStartCallDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EblGold),
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_direct_call")
            ) {
              Icon(Icons.Default.Call, contentDescription = null, tint = EblNavyDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("কল করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Online Team Members Horizontal Strip
        Text("Online Team Directory:", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(allUsers) { u ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.12f),
              modifier = Modifier.clickable { viewModel.startCall(u) }
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
                Icon(
                  Icons.Default.Call,
                  contentDescription = "Call",
                  tint = EblGold,
                  modifier = Modifier.size(12.dp)
                )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Event, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Hand Delivery Event / হ্যান্ড ডেলিভারি নোটিশ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EblNavyDark)
        }

        OutlinedButton(
          onClick = { showCreateEventDialog = true },
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.testTag("btn_create_event")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ নতুন ইভেন্ট তৈরি", fontSize = 11.sp, color = EblNavyPrimary, fontWeight = FontWeight.Bold)
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
              Text("নিচের বাটন দিয়ে মেসেজ বা হ্যান্ড ডেলিভারি ইভেন্ট তৈরি করুন।", color = Color.Gray, fontSize = 11.sp)
            }
          }
        }
      }

      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderRmCode.equals(currentUser?.rmCode, ignoreCase = true)

        if (msg.messageType == "EVENT" && msg.eventId != null) {
          val evt = events.find { it.eventId == msg.eventId }
          EventCardItem(
            event = evt,
            fallbackMessage = msg,
            isCreator = isMine,
            currentUser = currentUser,
            onOpenSubmit = { targetEvt -> activeResponseEvent = targetEvt },
            onViewSubmissions = { targetEvt -> activeViewEventSubmissions = targetEvt }
          )
        } else if (msg.messageType == "CALL_LOG") {
          // Call Log Pill
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFFF1F5F9),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(msg.messageText, fontSize = 11.sp, color = Color.DarkGray)
              }
            }
          }
        } else {
          // Standard Message Bubble
          ChatBubbleItem(
            message = msg,
            isMine = isMine,
            onCallSender = {
              val target = allUsers.find { it.rmCode.equals(msg.senderRmCode, ignoreCase = true) }
              if (target != null) {
                viewModel.startCall(target)
              }
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
  onCallSender: () -> Unit
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
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Quick Call",
            tint = Color(0xFF0284C7),
            modifier = Modifier
              .size(14.dp)
              .clickable { onCallSender() }
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
          Text(
            text = DateUtils.formatDateTime(message.timestamp).takeLast(8),
            color = if (isMine) Color.White.copy(alpha = 0.7f) else Color.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
          )
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
  onViewSubmissions: (TeamEventEntity) -> Unit
) {
  val title = event?.title ?: fallbackMessage.messageText.lines().firstOrNull() ?: "Team Hand Delivery Event"
  val targetDate = event?.targetDate ?: "As announced"
  val desc = event?.description ?: fallbackMessage.messageText
  val creatorName = event?.creatorName ?: fallbackMessage.senderName
  val isPrivileged = currentUser?.role == "ADMIN" || currentUser?.role == "MENTOR" || isCreator

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("event_card_${event?.eventId ?: fallbackMessage.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
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
              .background(Color(0xFF2563EB), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("HAND DELIVERY EVENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "By $creatorName",
            fontSize = 10.sp,
            color = Color.Gray
          )
        }

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

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = EblNavyDark
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = desc,
        fontSize = 12.sp,
        color = Color(0xFF334155)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // RM Button to submit date & delivery file details
        Button(
          onClick = {
            if (event != null) onOpenSubmit(event)
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_submit_event_details")
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("তথ্য এন্ট্রি দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Admin & Mentor (or creator) button to view summary table of all RMs
        if (isPrivileged && event != null) {
          OutlinedButton(
            onClick = { onViewSubmissions(event) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_view_all_submissions")
          ) {
            Text("সব RM এর তালিকা", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
          }
        }
      }
    }
  }
}

// Dialog to create a new delivery event
@Composable
fun CreateEventDialog(
  onDismiss: () -> Unit,
  onSubmit: (String, String, String) -> Unit
) {
  var title by remember { mutableStateOf("কার কার হ্যান্ড ডেলিভারি লাগবে?") }
  var targetDate by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("অনুগ্রহ করে যে যে RM এর কার্ড বা ফাইলের হ্যান্ড ডেলিভারি লাগবে, তারিখ ও ফাইলের সংখ্যা সহ বিস্তারিত এন্ট্রি দিন।") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Hand Delivery Event", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Event Title / বিষয়") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = targetDate,
          onValueChange = { targetDate = it },
          label = { Text("Target Date (e.g. 12/10/2026)") },
          placeholder = { Text("DD/MM/YYYY") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Instructions / বিবরণ") },
          maxLines = 4,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onSubmit(title.trim(), description.trim(), targetDate.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
      ) {
        Text("ইভেন্ট পোস্ট করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    }
  )
}

// Dialog for RMs to submit their delivery response
@Composable
fun SubmitDeliveryResponseDialog(
  event: TeamEventEntity,
  onDismiss: () -> Unit,
  onSubmit: (Int, String, String, String) -> Unit
) {
  var filesCountText by remember { mutableStateOf("1") }
  var requestedDate by remember { mutableStateOf(event.targetDate) }
  var location by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("হ্যান্ড ডেলিভারি তথ্য সাবমিট করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ইভেন্ট: ${event.title}", fontSize = 12.sp, color = Color.Gray)

        OutlinedTextField(
          value = filesCountText,
          onValueChange = { filesCountText = it },
          label = { Text("কয়টি ফাইল / কার্ড ডেলিভারি লাগবে?") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = requestedDate,
          onValueChange = { requestedDate = it },
          label = { Text("প্রয়োজনীয় তারিখ (Target Date)") },
          placeholder = { Text("e.g. 12-10-2026") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("ডেলিভারি লোকেশন / ব্রাঞ্চ") },
          placeholder = { Text("e.g. Gulshan Branch / Customer Office") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("কাস্টমার নাম / ফাইল আইডি / রিমার্কস") },
          maxLines = 3,
          placeholder = { Text("ফাইল নম্বর ও স্পেশাল নোট") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cnt = filesCountText.toIntOrNull() ?: 1
          onSubmit(cnt, requestedDate.trim(), location.trim(), remarks.trim())
        },
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
      ) {
        Text("সাবমিট করুন")
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
  onDismiss: () -> Unit,
  onCopySummary: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("RM Delivery Submissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Total: ${responses.size} RMs | ${responses.sumOf { it.filesCount }} Files", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onCopySummary) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = EblNavyPrimary)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("ইভেন্ট: ${event.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EblNavyDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (responses.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("এখনও কোনো RM রেসপন্স সাবমিট করেনি।", color = Color.Gray, fontSize = 12.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(responses, key = { it.responseId }) { r ->
              Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("${r.rmName} (${r.rmCode})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EblNavyDark)
                    Surface(
                      color = Color(0xFFDCFCE7),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text("${r.filesCount} Files", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("তারিখ: ${r.requestedDate}", fontSize = 11.sp, color = Color.DarkGray)
                  if (r.location.isNotBlank()) {
                    Text("লোকেশন: ${r.location}", fontSize = 11.sp, color = Color.DarkGray)
                  }
                  if (r.remarks.isNotBlank()) {
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
      Button(onClick = onCopySummary, colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)) {
        Text("কপি করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
    }
  )
}

// Dialog to select user for 1-on-1 Voice Call
@Composable
fun SelectUserToCallDialog(
  users: List<UserEntity>,
  onDismiss: () -> Unit,
  onSelectUser: (UserEntity) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ইন্টারনেট ভয়েস কল করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(280.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(users) { u ->
          Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectUser(u) }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${u.role}: ${u.rmCode} | ${u.officeAddress.take(20)}", fontSize = 10.sp, color = Color.Gray)
              }
              Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF10B981))
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    }
  )
}

// Dialog to start group call
@Composable
fun StartGroupCallDialog(
  onDismiss: () -> Unit,
  onStart: (String) -> Unit
) {
  var callTitle by remember { mutableStateOf("EBL Team Morning Sync") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("গ্রুপ ভয়েস কল শুরু করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("টিমের যেকোনো সদস্য এই গ্রুপ কলে সরাসরি অংশ নিতে পারবে।", fontSize = 11.sp, color = Color.Gray)
        OutlinedTextField(
          value = callTitle,
          onValueChange = { callTitle = it },
          label = { Text("Group Call Title / বিষয়") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onStart(callTitle.trim()) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
      ) {
        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("গ্রুপ কল চালু করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    }
  )
}

// Active Full Screen / Overlay Call Screen
@Composable
fun ActiveCallModal(
  callState: CallUiState,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onEndCall: () -> Unit
) {
  Surface(
    color = Color(0xFF0F172A),
    modifier = Modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      when (callState) {
        is CallUiState.Calling -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(110.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = callState.targetUser.name.take(2).uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(callState.targetUser.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${callState.targetUser.role}: ${callState.targetUser.rmCode}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Calling via EBL Internet Network...", color = EblGold, fontSize = 13.sp)
          }
        }

        is CallUiState.Connected -> {
          val min = callState.durationSeconds / 60
          val sec = callState.durationSeconds % 60
          val timeStr = String.format("%02d:%02d", min, sec)

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(110.dp)
                .border(3.dp, Color(0xFF22C55E), CircleShape)
                .background(Color(0xFF1E293B), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = callState.targetUser.name.take(2).uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(callState.targetUser.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${callState.targetUser.role}: ${callState.targetUser.rmCode}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connected - HD Audio ($timeStr)", color = Color(0xFF4ADE80), fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
        }

        is CallUiState.GroupCall -> {
          val min = callState.durationSeconds / 60
          val sec = callState.durationSeconds % 60
          val timeStr = String.format("%02d:%02d", min, sec)

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(callState.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Live Voice Room • $timeStr", color = Color(0xFF4ADE80), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            // Participant Grid
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.padding(horizontal = 8.dp)
            ) {
              items(callState.participants) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Box(
                    modifier = Modifier
                      .size(68.dp)
                      .border(2.dp, Color(0xFF38BDF8), CircleShape)
                      .background(Color(0xFF334155), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(p.name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(p.name.take(8), color = Color.White, fontSize = 11.sp)
                  Text("Speaking...", color = Color(0xFF22C55E), fontSize = 9.sp)
                }
              }
            }
          }
        }
        else -> {}
      }

      // Call Control Buttons (Mute, Speaker, End)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val isMuted = when (callState) {
          is CallUiState.Connected -> callState.isMuted
          is CallUiState.GroupCall -> callState.isMuted
          else -> false
        }
        val isSpeaker = when (callState) {
          is CallUiState.Connected -> callState.isSpeakerOn
          is CallUiState.GroupCall -> callState.isSpeakerOn
          else -> false
        }

        // Mute Button
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier
            .size(56.dp)
            .background(if (isMuted) Color(0xFFDC2626) else Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
          Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
        }

        // End Call Button
        IconButton(
          onClick = onEndCall,
          modifier = Modifier
            .size(68.dp)
            .background(Color(0xFFEF4444), CircleShape)
            .testTag("btn_end_call")
        ) {
          Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Speaker Button
        IconButton(
          onClick = onToggleSpeaker,
          modifier = Modifier
            .size(56.dp)
            .background(if (isSpeaker) Color(0xFF2563EB) else Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
          Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
        }
      }
    }
  }
}
