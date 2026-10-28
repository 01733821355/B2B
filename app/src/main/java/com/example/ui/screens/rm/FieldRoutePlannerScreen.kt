package com.example.ui.screens.rm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel

@Composable
fun FieldRoutePlannerScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  initialSelectedFileIds: List<String> = emptyList(),
  onBack: () -> Unit,
  onEditFile: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  val allFiles by viewModel.allFiles.collectAsState()
  val activeRouteIds by viewModel.activeRouteCustomerIds.collectAsState()
  val completedRouteIds by viewModel.completedRouteCustomerIds.collectAsState()

  // Filter relevant files: either assigned to RM, or all if privileged
  val userFiles = remember(allFiles, currentUser) {
    if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
      allFiles.filter { !it.isDeleted }
    } else {
      allFiles.filter { !it.isDeleted && it.assignedRmCode.equals(currentUser.rmCode, ignoreCase = true) }
    }
  }

  // Active route customers currently pending collection
  val remainingRouteCustomers = remember(activeRouteIds, userFiles) {
    activeRouteIds.mapNotNull { id -> userFiles.firstOrNull { it.fileId == id } }
  }

  // Completed customers from current route
  val completedRouteCustomers = remember(completedRouteIds, userFiles) {
    completedRouteIds.mapNotNull { id -> userFiles.firstOrNull { it.fileId == id } }
  }

  // In selection mode: which customer IDs are selected
  val selectedForRouteIds = remember(initialSelectedFileIds) {
    mutableStateListOf<String>().apply {
      addAll(initialSelectedFileIds)
    }
  }

  var isSelectingNewStops by remember(activeRouteIds) {
    mutableStateOf(activeRouteIds.isEmpty())
  }

  var searchQuery by remember { mutableStateOf("") }
  var categoryFilter by remember { mutableStateOf("INTERESTED") } // INTERESTED, PENDING_DOCS, CPV, ALL

  var customerToCompleteConfirm by remember { mutableStateOf<CustomerFileEntity?>(null) }
  var showResetRouteDialog by remember { mutableStateOf(false) }

  // Filtered list of customers available for selection
  val eligibleSelectionCustomers = remember(userFiles, categoryFilter, searchQuery, activeRouteIds) {
    userFiles.filter { f ->
      // Exclude already active in route
      !activeRouteIds.contains(f.fileId)
    }.filter { f ->
      when (categoryFilter) {
        "INTERESTED" -> {
          // Interested / In-progress files (not yet final approved or declined)
          f.applicationStatus.equals("Interested", ignoreCase = true) ||
          f.applicationStatus.equals("In Progress", ignoreCase = true) ||
          f.applicationStatus.equals("Pending", ignoreCase = true) ||
          f.activeStatus.equals("Y", ignoreCase = true) ||
          f.pendingDocuments.isNotBlank()
        }
        "PENDING_DOCS" -> f.pendingDocuments.isNotBlank()
        "CPV" -> f.cpvStatus.equals("Pending", ignoreCase = true)
        else -> true
      }
    }.filter { f ->
      if (searchQuery.isBlank()) true
      else {
        val q = searchQuery.trim().lowercase()
        f.customerName.lowercase().contains(q) ||
        f.companyName.lowercase().contains(q) ||
        f.officeAddress.lowercase().contains(q) ||
        f.cpvAddress.lowercase().contains(q) ||
        f.mobile.contains(q)
      }
    }
  }

  // AI-optimized order helper: clusters by area/thana
  val optimizedRouteCustomers = remember(remainingRouteCustomers) {
    sortStopsByCluster(remainingRouteCustomers)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .testTag("screen_field_route_planner")
  ) {
    // 1. Top App Bar
    Surface(
      color = EblNavyPrimary,
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp).testTag("btn_route_back")
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Directions, contentDescription = null, tint = EblGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Field Visit & Route Plan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Text(
              text = if (activeRouteIds.isNotEmpty() && !isSelectingNewStops)
                "${remainingRouteCustomers.size} Stop(s) Pending • ${completedRouteCustomers.size} Done"
              else
                "Select Interested Customers & Plan Route",
              fontSize = 11.sp,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        }

        if (activeRouteIds.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!isSelectingNewStops) {
              OutlinedButton(
                onClick = { isSelectingNewStops = true },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.height(32.dp).testTag("btn_add_stops_to_route")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ Add", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            } else {
              OutlinedButton(
                onClick = { isSelectingNewStops = false },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.height(32.dp).testTag("btn_view_active_route")
              ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = EblGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("View Route (${remainingRouteCustomers.size})", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            }

            IconButton(
              onClick = { showResetRouteDialog = true },
              modifier = Modifier.size(32.dp).testTag("btn_reset_route")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset Route", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    // Main Content: Active Route Plan vs Customer Selector
    if (activeRouteIds.isNotEmpty() && !isSelectingNewStops) {
      // -------------------------------------------------------------
      // VIEW A: ACTIVE ROUTE PLAN (গুগল ম্যাপ ও এআই ডিরেকশন সহ চলমান রুট)
      // -------------------------------------------------------------
      ActiveRoutePlanView(
        customers = optimizedRouteCustomers,
        completedCount = completedRouteCustomers.size,
        totalInitialCount = remainingRouteCustomers.size + completedRouteCustomers.size,
        onCompleteCustomer = { customerToCompleteConfirm = it },
        onRemoveFromRoute = { viewModel.removeCustomerFromRoute(it.fileId) },
        onOpenGoogleMapsFullRoute = { openGoogleMapsFullRoute(context, optimizedRouteCustomers) },
        onOpenSingleLocationMap = { addr -> openSingleLocationOnMap(context, addr) },
        onCallCustomer = { phone -> dialPhoneNumber(context, phone) },
        onWhatsAppCustomer = { phone, name -> sendWhatsApp(context, phone, name) },
        onEditFile = onEditFile,
        onStartNewRoute = { isSelectingNewStops = true }
      )
    } else {
      // -------------------------------------------------------------
      // VIEW B: CUSTOMER SELECTION & 'WANT TO GO' (কাস্টমার নির্বাচন)
      // -------------------------------------------------------------
      CustomerSelectionView(
        eligibleCustomers = eligibleSelectionCustomers,
        selectedIds = selectedForRouteIds,
        activeRouteCount = activeRouteIds.size,
        searchQuery = searchQuery,
        categoryFilter = categoryFilter,
        onSearchQueryChange = { searchQuery = it },
        onCategoryFilterChange = { categoryFilter = it },
        onToggleSelectCustomer = { fileId ->
          if (selectedForRouteIds.contains(fileId)) {
            selectedForRouteIds.remove(fileId)
          } else {
            selectedForRouteIds.add(fileId)
          }
        },
        onSelectAll = {
          eligibleSelectionCustomers.forEach { f ->
            if (!selectedForRouteIds.contains(f.fileId)) selectedForRouteIds.add(f.fileId)
          }
        },
        onDeselectAll = {
          selectedForRouteIds.clear()
        },
        onWantToGo = {
          if (selectedForRouteIds.isEmpty()) {
            Toast.makeText(context, "অনুগ্রহ করে কমপক্ষে ১ জন কাস্টমার নির্বাচন করুন!", Toast.LENGTH_SHORT).show()
          } else {
            // Add to active route or start new route
            val combined = (activeRouteIds + selectedForRouteIds).distinct()
            viewModel.startRoute(combined)
            selectedForRouteIds.clear()
            isSelectingNewStops = false
            Toast.makeText(context, "🚀 ${combined.size} টি স্পটের রুট প্ল্যান তৈরি হয়েছে!", Toast.LENGTH_SHORT).show()
          }
        }
      )
    }
  }

  // Confirmation dialog to mark customer as collected and remove from route
  if (customerToCompleteConfirm != null) {
    val cust = customerToCompleteConfirm!!
    AlertDialog(
      onDismissRequest = { customerToCompleteConfirm = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
          Spacer(modifier = Modifier.width(8.dp))
          Text("ফাইল কালেকশন সম্পন্ন?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("গ্রাহক: ${cust.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EblNavyDark)
          Text("কোম্পানি: ${cust.companyName.ifBlank { "N/A" }}", fontSize = 12.sp, color = Color(0xFF475569))
          Text("ঠিকানা: ${cust.officeAddress.ifBlank { cust.cpvAddress }.ifBlank { "ঠিকানা পাওয়া যায়নি" }}", fontSize = 11.5.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFECFDF5),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
          ) {
            Text(
              text = "✓ স্ট্যাটাস 'Collected' এ আপডেট হবে এবং এই গ্রাহকটি বর্তমান রুট থেকে স্বয়ংক্রিয়ভাবে বাদ পড়বে। বাকি গ্রাহকদের নিয়ে রুট আপডেট হবে।",
              fontSize = 11.sp,
              color = Color(0xFF065F46),
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.completeCustomerInRoute(cust.fileId) {
              customerToCompleteConfirm = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("btn_confirm_complete_collection")
        ) {
          Text("হ্যাঁ, কালেকশন সম্পন্ন", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { customerToCompleteConfirm = null },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("বাতিল")
        }
      }
    )
  }

  // Reset Route confirmation dialog
  if (showResetRouteDialog) {
    AlertDialog(
      onDismissRequest = { showResetRouteDialog = false },
      title = { Text("রুট প্ল্যান রিসেট করতে চান?", fontWeight = FontWeight.Bold) },
      text = { Text("বর্তমান রুট প্ল্যান বাতিল হয়ে যাবে। আপনি আবার নতুন করে কাস্টমার সিলেক্ট করতে পারবেন।") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearRoute()
            isSelectingNewStops = true
            showResetRouteDialog = false
            Toast.makeText(context, "রুট প্ল্যান সফলভাবে রিসেট করা হয়েছে।", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("রিসেট করুন", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showResetRouteDialog = false }) {
          Text("বাতিল")
        }
      }
    )
  }
}

// -------------------------------------------------------------------------
// COMPONENT 1: ACTIVE ROUTE PLAN VIEW
// -------------------------------------------------------------------------
@Composable
private fun ActiveRoutePlanView(
  customers: List<CustomerFileEntity>,
  completedCount: Int,
  totalInitialCount: Int,
  onCompleteCustomer: (CustomerFileEntity) -> Unit,
  onRemoveFromRoute: (CustomerFileEntity) -> Unit,
  onOpenGoogleMapsFullRoute: () -> Unit,
  onOpenSingleLocationMap: (String) -> Unit,
  onCallCustomer: (String) -> Unit,
  onWhatsAppCustomer: (String, String) -> Unit,
  onEditFile: (String) -> Unit,
  onStartNewRoute: () -> Unit
) {
  if (customers.isEmpty()) {
    // All stops completed celebration
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = Color(0xFFECFDF5),
        border = BorderStroke(2.dp, Color(0xFF10B981)),
        modifier = Modifier.size(80.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(44.dp))
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text("🎉 দারুণ কাজ!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "আজকের রুট প্ল্যানের সকল ($completedCount টি) ফাইল সফলভাবে কালেকশন সম্পন্ন হয়েছে!",
        fontSize = 13.5.sp,
        color = Color(0xFF475569),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onStartNewRoute,
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(44.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("নতুন রুট প্ল্যান তৈরি করুন (Plan New Route)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    }
    return
  }

  var activeViewTab by remember { mutableStateOf("MAP") } // "MAP" or "LIST"

  Column(modifier = Modifier.fillMaxSize()) {
    // Mode Switcher: 🗺️ রুট ম্যাপ ভিউ (Map) vs 📋 স্টপ তালিকা (Stop List)
    Surface(
      color = Color.White,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (activeViewTab == "MAP") Color(0xFF1D4ED8) else Color(0xFFF1F5F9),
          border = BorderStroke(1.dp, if (activeViewTab == "MAP") Color(0xFF1D4ED8) else Color(0xFFCBD5E1)),
          modifier = Modifier
            .weight(1f)
            .clickable { activeViewTab = "MAP" }
            .testTag("tab_route_map_view")
        ) {
          Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Map,
              contentDescription = null,
              tint = if (activeViewTab == "MAP") Color.White else Color(0xFF475569),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "🗺️ রুট ম্যাপ ভিউ (Map)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (activeViewTab == "MAP") Color.White else Color(0xFF475569)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (activeViewTab == "LIST") Color(0xFF1D4ED8) else Color(0xFFF1F5F9),
          border = BorderStroke(1.dp, if (activeViewTab == "LIST") Color(0xFF1D4ED8) else Color(0xFFCBD5E1)),
          modifier = Modifier
            .weight(1f)
            .clickable { activeViewTab = "LIST" }
            .testTag("tab_route_list_view")
        ) {
          Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Navigation,
              contentDescription = null,
              tint = if (activeViewTab == "LIST") Color.White else Color(0xFF475569),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "📋 স্টপ তালিকা (${customers.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (activeViewTab == "LIST") Color.White else Color(0xFF475569)
            )
          }
        }
      }
    }

    if (activeViewTab == "MAP") {
      FieldRouteInteractiveMapView(
        customers = customers,
        completedCount = completedCount,
        totalInitialCount = totalInitialCount,
        onOpenGoogleMaps = onOpenGoogleMapsFullRoute,
        onCompleteCustomer = onCompleteCustomer,
        onCallCustomer = onCallCustomer,
        onWhatsAppCustomer = onWhatsAppCustomer,
        onOpenSingleLocationMap = onOpenSingleLocationMap,
        modifier = Modifier.weight(1f)
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
      ) {
    // Progress & Summary Banner
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("চলমান রুট প্ল্যান অগ্রগতি", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
              Text("বাকি ${customers.size} টি স্টপ • সম্পন্ন $completedCount টি", fontSize = 11.5.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFEFF6FF),
              border = BorderStroke(0.8.dp, Color(0xFF93C5FD))
            ) {
              Text(
                text = "${((completedCount.toFloat() / totalInitialCount.coerceAtLeast(1)) * 100).toInt()}% Done",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4ED8),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          val progressFraction = (completedCount.toFloat() / totalInitialCount.coerceAtLeast(1)).coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFE2E8F0)
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Big Google Maps Master Direction Button
          Button(
            onClick = onOpenGoogleMapsFullRoute,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("btn_open_google_maps_full_route")
          ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "🗺️ Google Maps-এ সম্পূর্ণ রুট ওপেন করুন",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }

    // AI Smart Route Direction & Order Tips
    item {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("🤖 AI রুট অপটিমাইজেশন ও ফিল্ড গাইড", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "কাছাকাছি এলাকা ও ট্রাফিকের সুবিধার জন্য স্টপগুলো ক্রমানুসারে সাজানো হয়েছে। যেকোনো স্টপে ফাইল নেওয়া শেষে '✓ কালেকশন সম্পন্ন' ট্যাপ করলে রুট স্বয়ংক্রিয়ভাবে আপডেট হবে।",
              fontSize = 10.5.sp,
              color = Color(0xFF14532D),
              lineHeight = 15.sp
            )
          }
        }
      }
    }

    // Stops Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("পরিদর্শনের ক্রমানুসার (Visit Order)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        Text("${customers.size} টি স্পট অবশিষ্ট", fontSize = 11.sp, color = Color.Gray)
      }
    }

    // Ordered Stops
    itemsIndexed(customers, key = { _, c -> c.fileId }) { index, customer ->
      RouteStopCard(
        stopNumber = index + 1,
        customer = customer,
        isLastStop = index == customers.size - 1,
        onComplete = { onCompleteCustomer(customer) },
        onRemove = { onRemoveFromRoute(customer) },
        onNavigate = {
          val addr = customer.officeAddress.ifBlank { customer.cpvAddress }.ifBlank { customer.companyName }
          onOpenSingleLocationMap(addr)
        },
        onCall = { onCallCustomer(customer.mobile) },
        onWhatsApp = { onWhatsAppCustomer(customer.mobile, customer.customerName) },
        onEdit = { onEditFile(customer.fileId) }
      )
    }
  }
}
}
}

@Composable
private fun RouteStopCard(
  stopNumber: Int,
  customer: CustomerFileEntity,
  isLastStop: Boolean,
  onComplete: () -> Unit,
  onRemove: () -> Unit,
  onNavigate: () -> Unit,
  onCall: () -> Unit,
  onWhatsApp: () -> Unit,
  onEdit: () -> Unit
) {
  val targetAddress = customer.officeAddress.ifBlank { customer.cpvAddress }.ifBlank { "ঠিকানা উল্লেখ নেই" }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.testTag("route_stop_$stopNumber")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Stop Badge + Customer Name + Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Surface(
            shape = CircleShape,
            color = if (stopNumber == 1) Color(0xFF2563EB) else EblNavyPrimary,
            modifier = Modifier.size(26.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("$stopNumber", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = customer.customerName,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = customer.companyName.ifBlank { customer.productType },
              fontSize = 11.5.sp,
              color = Color(0xFF64748B)
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
          ApplicationStatusBadge(customer.applicationStatus)
          IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "রুট থেকে সরান", tint = Color.LightGray, modifier = Modifier.size(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Location / Address Row
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = targetAddress,
            fontSize = 11.5.sp,
            color = Color(0xFF334155),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Pending Documents Alert if any
      if (customer.pendingDocuments.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFFEF2F2),
          border = BorderStroke(0.6.dp, Color(0xFFFECACA)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "পেন্ডিং ডকুমেন্টস: ${customer.pendingDocuments}",
              fontSize = 10.5.sp,
              color = Color(0xFF991B1B),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Communication & Navigation Actions Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Map Navigate Button
        OutlinedButton(
          onClick = onNavigate,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
          border = BorderStroke(1.dp, Color(0xFF93C5FD)),
          modifier = Modifier.weight(1f).height(34.dp)
        ) {
          Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ম্যাপ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Call Button
        OutlinedButton(
          onClick = onCall,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
          border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
          modifier = Modifier.weight(0.9f).height(34.dp)
        ) {
          Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("কল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // WhatsApp Button
        OutlinedButton(
          onClick = onWhatsApp,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
          border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
          modifier = Modifier.weight(1f).height(34.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("WhatsApp", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }

        // Edit / Details Button
        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = "Edit File", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9))
      Spacer(modifier = Modifier.height(8.dp))

      // MASTER ACTION: Mark as Collected and Remove from Route
      Button(
        onClick = onComplete,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp)
          .testTag("btn_complete_stop_$stopNumber")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "✓ কালেকশন সম্পন্ন (রুট আপডেট করুন)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

// -------------------------------------------------------------------------
// COMPONENT 2: CUSTOMER SELECTION & 'WANT TO GO' VIEW
// -------------------------------------------------------------------------
@Composable
private fun CustomerSelectionView(
  eligibleCustomers: List<CustomerFileEntity>,
  selectedIds: List<String>,
  activeRouteCount: Int,
  searchQuery: String,
  categoryFilter: String,
  onSearchQueryChange: (String) -> Unit,
  onCategoryFilterChange: (String) -> Unit,
  onToggleSelectCustomer: (String) -> Unit,
  onSelectAll: () -> Unit,
  onDeselectAll: () -> Unit,
  onWantToGo: () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 76.dp)
    ) {
      // Header & Filter Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search Interested Customers / Address...", fontSize = 11.5.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
          textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val filterOptions = listOf(
            Pair("INTERESTED", "⭐ Interested / Follow-up"),
            Pair("PENDING_DOCS", "⚠️ Pending Documents"),
            Pair("CPV", "📋 CPV Pending"),
            Pair("ALL", "📁 All Active Files")
          )

          filterOptions.forEach { (key, label) ->
            val isSel = categoryFilter == key
            FilterChip(
              selected = isSel,
              onClick = { onCategoryFilterChange(key) },
              label = {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EblNavyPrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Select All / Deselect All Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${eligibleCustomers.size} customers found",
            fontSize = 11.5.sp,
            color = Color(0xFF64748B)
          )

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(
              onClick = onSelectAll,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyPrimary)
            }
            TextButton(
              onClick = onDeselectAll,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("Clear", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
          }
        }
      }

      // Customer List
      if (eligibleCustomers.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("No customers found matching this filter.", color = Color.Gray, fontSize = 13.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          contentPadding = PaddingValues(vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(eligibleCustomers, key = { it.fileId }) { customer ->
            val isSelected = selectedIds.contains(customer.fileId)
            val addr = customer.officeAddress.ifBlank { customer.cpvAddress }.ifBlank { "ঠিকানা পাওয়া যায়নি" }

            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
              ),
              border = BorderStroke(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
              ),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSelectCustomer(customer.fileId) }
                .testTag("select_cust_${customer.fileId}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isSelected,
                  onCheckedChange = { onToggleSelectCustomer(customer.fileId) },
                  colors = CheckboxDefaults.colors(checkedColor = EblNavyPrimary),
                  modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = customer.customerName,
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = EblNavyDark,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    ApplicationStatusBadge(customer.applicationStatus)
                  }

                  Text(
                    text = "${customer.companyName.ifBlank { "Individual" }} • ${customer.productType}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                  )

                  Spacer(modifier = Modifier.height(3.dp))

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = addr,
                      fontSize = 11.sp,
                      color = Color(0xFF475569),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  if (customer.pendingDocuments.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                      text = "বাকি: ${customer.pendingDocuments}",
                      fontSize = 10.sp,
                      color = Color(0xFFDC2626),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Floating Bottom Bar: 'WANT TO GO' ACTION
    Surface(
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
      color = Color.White,
      shadowElevation = 8.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "${selectedIds.size} জন কাস্টমার নির্বাচিত",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Text(
            text = if (activeRouteCount > 0) "+ রানিং রুটে যোগ হবে" else "সহজ গুগল ম্যাপ রুট ও ডিরেকশন তৈরি হবে",
            fontSize = 10.5.sp,
            color = Color.Gray
          )
        }

        Button(
          onClick = onWantToGo,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedIds.isNotEmpty()) Color(0xFF059669) else Color(0xFF94A3B8)
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier.testTag("btn_want_to_go")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Want to Go (রুট প্ল্যান)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------------------
// HELPER FUNCTIONS FOR NAVIGATION & AI CLUSTERING
// -------------------------------------------------------------------------

/**
 * AI Clustered sequence sorter:
 * Clusters customer stops by known Dhaka/Bangladesh area keywords to reduce travel distance.
 */
private fun sortStopsByCluster(customers: List<CustomerFileEntity>): List<CustomerFileEntity> {
  if (customers.size <= 2) return customers

  val areaPriority = listOf(
    "motijheel", "dilkusha", "paltan", "gulistan", "kawran bazar", "karwan bazar",
    "tejgaon", "panthapath", "dhanmondi", "kalabagan", "mohammadpur", "mirpur",
    "banani", "gulshan", "baridhara", "mohakhali", "nikunja", "uttara",
    "chittagong", "agrabad", "nasirabad", "sylhet", "rajshahi", "khulna"
  )

  return customers.sortedBy { c ->
    val combinedText = "${c.officeAddress} ${c.cpvAddress} ${c.companyName}".lowercase()
    val matchIdx = areaPriority.indexOfFirst { area -> combinedText.contains(area) }
    if (matchIdx >= 0) matchIdx else 999
  }
}

private fun openGoogleMapsFullRoute(context: Context, customers: List<CustomerFileEntity>) {
  if (customers.isEmpty()) {
    Toast.makeText(context, "কোনো কাস্টমার পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
    return
  }

  val addresses = customers.map { c ->
    val clean = c.officeAddress.ifBlank { c.cpvAddress }.ifBlank { c.companyName }.trim()
    if (clean.isNotBlank()) clean else "${c.customerName}, Dhaka"
  }

  if (addresses.size == 1) {
    val encoded = Uri.encode(addresses.first())
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "Google Maps অ্যাপ ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
    }
  } else {
    val destination = Uri.encode(addresses.last())
    val waypoints = addresses.dropLast(1).joinToString("|") { Uri.encode(it) }
    val url = "https://www.google.com/maps/dir/?api=1&destination=$destination&waypoints=$waypoints&travelmode=driving"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "Google Maps অ্যাপ ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
    }
  }
}

private fun openSingleLocationOnMap(context: Context, address: String) {
  val clean = address.trim()
  if (clean.isBlank()) {
    Toast.makeText(context, "ঠিকানা খালি!", Toast.LENGTH_SHORT).show()
    return
  }
  val encoded = Uri.encode(clean)
  val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
  val intent = Intent(Intent.ACTION_VIEW, uri)
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "Google Maps ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
  }
}

private fun dialPhoneNumber(context: Context, phone: String) {
  val clean = phone.filter { it.isDigit() || it == '+' }
  if (clean.isBlank()) {
    Toast.makeText(context, "মোবাইল নম্বর পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
    return
  }
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
  try {
    context.startActivity(intent)
  } catch (_: Exception) {}
}

private fun sendWhatsApp(context: Context, phone: String, customerName: String) {
  val clean = phone.filter { it.isDigit() }
  if (clean.isBlank()) {
    Toast.makeText(context, "মোবাইল নম্বর পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
    return
  }
  val formattedNumber = if (clean.startsWith("880")) clean else if (clean.startsWith("0")) "88$clean" else "880$clean"
  val text = "Dear $customerName, Greetings from Eastern Bank PLC (EBL). I am on my way to collect your application documents."
  val uri = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(text)}")
  val intent = Intent(Intent.ACTION_VIEW, uri)
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "WhatsApp ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
  }
}
