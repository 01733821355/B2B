package com.example.ui.screens.mentor

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserLocationLogEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.EblNavySecondary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils
import com.example.util.LocationHelper

@Composable
fun MentorUserLocationScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Strictly enforce mentor authorization
  if (currentUser.role != "MENTOR") {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Access Restricted: Mentor Only",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFFDC2626)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Live user and RM location tracking is strictly confidential and restricted to Mentor authority.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
          ) {
            Text("Return to Safety")
          }
        }
      }
    }
    return
  }

  val allUsers by viewModel.allUsers.collectAsState()
  val allRms by viewModel.allRms.collectAsState()
  val locationLogs by viewModel.recentLocationLogs.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var filterStatus by remember { mutableStateOf("ALL") } // ALL, ONLINE, LOCATED
  var selectedUserForHistory by remember { mutableStateOf<UserEntity?>(null) }
  var selectedUserForMapPin by remember { mutableStateOf<UserEntity?>(null) }

  // Target users to monitor: primarily RMs and system users
  val targetUsers = remember(allUsers, allRms) {
    if (allRms.isNotEmpty()) allRms else allUsers.filter { it.role == "RM" }
  }

  val filteredUsers = remember(targetUsers, searchQuery, filterStatus) {
    targetUsers.filter { u ->
      val matchesSearch = searchQuery.isBlank() ||
        u.name.contains(searchQuery, ignoreCase = true) ||
        u.rmCode.contains(searchQuery, ignoreCase = true) ||
        u.officeAddress.contains(searchQuery, ignoreCase = true) ||
        (u.lastLocationAddress?.contains(searchQuery, ignoreCase = true) == true)

      val matchesFilter = when (filterStatus) {
        "ONLINE" -> u.isOnline
        "LOCATED" -> u.lastLatitude != null && u.lastLongitude != null
        else -> true
      }

      matchesSearch && matchesFilter
    }
  }

  val onlineCount = targetUsers.count { it.isOnline }
  val withLocationCount = targetUsers.count { it.lastLatitude != null && it.lastLongitude != null }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("mentor_location_tracking_screen")
  ) {
    // Header Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onNavigateBack) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = "Back",
                  tint = Color.White
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = "User & RM Live Location Monitor",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Mentor Exclusive Oversight • Real-Time Field Tracking",
                  fontSize = 12.sp,
                  color = EblGold
                )
              }
            }

            Surface(
              color = Color(0xFF10B981).copy(alpha = 0.2f),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "LIVE RADAR",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF10B981)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Summary KPI Pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              modifier = Modifier.weight(1f),
              color = Color.White.copy(alpha = 0.1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(text = "${targetUsers.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Total RMs", fontSize = 11.sp, color = Color.LightGray)
              }
            }

            Surface(
              modifier = Modifier.weight(1f),
              color = Color(0xFF10B981).copy(alpha = 0.15f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(text = "$onlineCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                Text(text = "Active Online", fontSize = 11.sp, color = Color(0xFFA7F3D0))
              }
            }

            Surface(
              modifier = Modifier.weight(1f),
              color = Color(0xFF3B82F6).copy(alpha = 0.15f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(text = "$withLocationCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                Text(text = "GPS Located", fontSize = 11.sp, color = Color(0xFFBFDBFE))
              }
            }
          }
        }
      }
    }

    // Visual Radar / Live Map Canvas Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = EblNavyPrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Dhaka City Field Tracking Radar",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
            }
            Text(
              text = "Tap Pin to Inspect",
              fontSize = 11.sp,
              color = Color.Gray
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Compose Custom Canvas Radar Simulation for Dhaka Hubs
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF0F172A))
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val w = size.width
              val h = size.height
              val center = Offset(w / 2f, h / 2f)

              // Draw radar concentric circles
              val maxRadius = minOf(w, h) * 0.44f
              drawCircle(Color(0xFF1E293B), radius = maxRadius, center = center, style = Stroke(width = 1.5f))
              drawCircle(Color(0xFF1E293B), radius = maxRadius * 0.66f, center = center, style = Stroke(width = 1.2f))
              drawCircle(Color(0xFF1E293B), radius = maxRadius * 0.33f, center = center, style = Stroke(width = 1.2f))

              // Crosshairs
              drawLine(Color(0xFF1E293B), Offset(center.x, 10f), Offset(center.x, h - 10f), strokeWidth = 1f)
              drawLine(Color(0xFF1E293B), Offset(10f, center.y), Offset(w - 10f, center.y), strokeWidth = 1f)

              // Target City Hubs in Dhaka
              val hubMotijheel = Offset(center.x + w * 0.15f, center.y + h * 0.22f)
              val hubGulshan = Offset(center.x - w * 0.05f, center.y - h * 0.25f)
              val hubDhanmondi = Offset(center.x - w * 0.28f, center.y + h * 0.05f)
              val hubUttara = Offset(center.x + w * 0.02f, center.y - h * 0.38f)

              // Draw Hub Labels / Dots
              drawCircle(Color(0xFF64748B), radius = 3f, center = hubMotijheel)
              drawCircle(Color(0xFF64748B), radius = 3f, center = hubGulshan)
              drawCircle(Color(0xFF64748B), radius = 3f, center = hubDhanmondi)
              drawCircle(Color(0xFF64748B), radius = 3f, center = hubUttara)

              // Draw RM Active Locations with glowing halos
              targetUsers.forEachIndexed { idx, u ->
                val pos = when (idx % 4) {
                  0 -> Offset(hubGulshan.x + 18f, hubGulshan.y + 12f)
                  1 -> Offset(hubMotijheel.x - 12f, hubMotijheel.y - 14f)
                  2 -> Offset(hubDhanmondi.x + 14f, hubDhanmondi.y + 10f)
                  else -> Offset(hubUttara.x - 15f, hubUttara.y + 20f)
                }

                val pinColor = if (u.isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)

                // Halo
                drawCircle(pinColor.copy(alpha = 0.25f), radius = 12f, center = pos)
                // Core
                drawCircle(pinColor, radius = 5.5f, center = pos)
                drawCircle(Color.White, radius = 2f, center = pos)
              }
            }

            // Radar Zone Overlay Text
            Column(modifier = Modifier.padding(10.dp)) {
              Text("📍 Gulshan-1 / Banani Zone", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Spacer(modifier = Modifier.height(2.dp))
              Text("📍 Motijheel Commercial Area", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Spacer(modifier = Modifier.height(2.dp))
              Text("📍 Dhanmondi Financial Hub", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }

            // Quick legend in corner
            Row(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Online RM", color = Color.White, fontSize = 10.sp)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Last Known", color = Color.White, fontSize = 10.sp)
              }
            }
          }
        }
      }
    }

    // Filter Chips & Search Bar
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        com.example.ui.common.VoiceInputField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = "Search RM Location",
          placeholder = "Search by RM Name, Code, or Location...",
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EblNavyPrimary) },
          modifier = Modifier.fillMaxWidth(),
          testTag = "search_user_locations"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = filterStatus == "ALL",
            onClick = { filterStatus = "ALL" },
            label = { Text("All RMs (${targetUsers.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EblNavyPrimary,
              selectedLabelColor = Color.White
            )
          )
          FilterChip(
            selected = filterStatus == "ONLINE",
            onClick = { filterStatus = "ONLINE" },
            label = { Text("Online ($onlineCount)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF10B981),
              selectedLabelColor = Color.White
            )
          )
          FilterChip(
            selected = filterStatus == "LOCATED",
            onClick = { filterStatus = "LOCATED" },
            label = { Text("GPS Located ($withLocationCount)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF3B82F6),
              selectedLabelColor = Color.White
            )
          )
        }
      }
    }

    // List of Users / RMs with Location
    items(filteredUsers, key = { it.rmCode }) { user ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("rm_location_card_${user.rmCode}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Top Row: Avatar, Name, RM Code, Online Status
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(EblNavyPrimary),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = user.name.take(2).uppercase(),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = user.name,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Text(
                  text = "RM Code: ${user.rmCode} • ${user.officeAddress}",
                  fontSize = 12.sp,
                  color = Color.Gray
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (user.isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (user.isOnline) Color(0xFF10B981) else Color.Gray)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (user.isOnline) "ONLINE" else "OFFLINE",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (user.isOnline) Color(0xFF047857) else Color.DarkGray
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Location Box
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = EblNavyPrimary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(
                    text = user.lastLocationAddress ?: "Location not updated yet",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = EblNavyDark
                  )
                  if (user.lastLatitude != null && user.lastLongitude != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "GPS Coordinates: %.5f, %.5f".format(user.lastLatitude, user.lastLongitude),
                      fontSize = 11.sp,
                      color = Color.Gray
                    )
                  }
                  if (user.lastLocationTime != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Last Ping: ${DateUtils.formatDateTime(user.lastLocationTime)}",
                      fontSize = 10.sp,
                      color = Color(0xFF6B7280)
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Operational Actions for Mentor
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Open in Google Maps
            Button(
              onClick = {
                val lat = user.lastLatitude ?: 23.7808
                val lng = user.lastLongitude ?: 90.4192
                LocationHelper.openGoogleMaps(context, lat, lng, "RM ${user.name} (${user.rmCode})")
              },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("btn_maps_${user.rmCode}")
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Google Maps", fontSize = 12.sp)
            }

            // View Location History
            OutlinedButton(
              onClick = { selectedUserForHistory = user },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("btn_history_${user.rmCode}")
            ) {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Log History", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Bottom padding
    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }

  // Location Log History Dialog
  if (selectedUserForHistory != null) {
    val target = selectedUserForHistory!!
    val userLogs = locationLogs.filter { it.rmCode == target.rmCode }

    AlertDialog(
      onDismissRequest = { selectedUserForHistory = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, contentDescription = null, tint = EblNavyPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Location Audit Trail: ${target.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
        ) {
          Text(
            text = "Recorded check-ins and GPS auto-detect events for RM ${target.rmCode}:",
            fontSize = 12.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(8.dp))

          if (userLogs.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text("No location logs recorded yet for this user.", fontSize = 13.sp, color = Color.Gray)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(userLogs) { log ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = when (log.sourceAction) {
                          "GPS_AUTO_DETECT" -> "📍 GPS Auto-Detect"
                          "LOGIN" -> "🔑 Portal Login"
                          "CUSTOMER_FILE_ENTRY" -> "📄 Customer File Input"
                          else -> log.sourceAction
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EblNavyPrimary
                      )
                      Text(
                        text = DateUtils.formatDateTime(log.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.address, fontSize = 12.sp, color = EblNavyDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Coords: %.5f, %.5f".format(log.latitude, log.longitude),
                      fontSize = 10.sp,
                      color = Color(0xFF64748B)
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { selectedUserForHistory = null },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Close")
        }
      }
    )
  }
}
