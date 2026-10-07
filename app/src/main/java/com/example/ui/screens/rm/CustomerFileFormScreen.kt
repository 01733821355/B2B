package com.example.ui.screens.rm

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import com.example.ui.common.AttachmentViewerDialog
import com.example.util.AttachmentHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.UserEntity
import com.example.ui.common.CpvStatusBadge
import com.example.ui.common.VoiceInputField
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils
import com.example.util.LocationHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerFileFormScreen(
  viewModel: AppViewModel,
  editFileId: String?,
  currentUser: UserEntity,
  onCancel: () -> Unit,
  onSaveSuccess: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var isDetectingLocation by remember { mutableStateOf(false) }
  var locationDetectionSuccess by remember { mutableStateOf(false) }

  var existingFile by remember { mutableStateOf<CustomerFileEntity?>(null) }
  var isLoaded by remember { mutableStateOf(editFileId == null) }

  // Form Fields
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var fileId by remember { mutableStateOf(editFileId ?: SecurityUtils.generateFileId(currentUser.rmCode)) }

  var selectedCategoryForUpload by remember { mutableStateOf("NID") }

  val statusPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    coroutineScope.launch {
      uris.forEach { uri ->
        val saved = AttachmentHelper.saveUriToInternalStorage(
          context = context,
          sourceUri = uri,
          preferredCategory = "Status Update Photo"
        )
        viewModel.addAttachment(
          fileId = fileId,
          category = "Status Update Photo",
          fileName = saved.fileName,
          fileType = saved.fileType,
          fileSizeBytes = saved.fileSizeBytes,
          fileUri = saved.fileUri
        )
      }
    }
  }

  val cpvMultiPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    coroutineScope.launch {
      uris.forEach { uri ->
        val saved = AttachmentHelper.saveUriToInternalStorage(
          context = context,
          sourceUri = uri,
          preferredCategory = "CPV Photo"
        )
        viewModel.addAttachment(
          fileId = fileId,
          category = "CPV Photo",
          fileName = saved.fileName,
          fileType = saved.fileType,
          fileSizeBytes = saved.fileSizeBytes,
          fileUri = saved.fileUri
        )
      }
    }
  }

  val othersPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    coroutineScope.launch {
      uris.forEach { uri ->
        val saved = AttachmentHelper.saveUriToInternalStorage(
          context = context,
          sourceUri = uri,
          preferredCategory = "Other Document"
        )
        viewModel.addAttachment(
          fileId = fileId,
          category = "Other Document",
          fileName = saved.fileName,
          fileType = saved.fileType,
          fileSizeBytes = saved.fileSizeBytes,
          fileUri = saved.fileUri
        )
      }
    }
  }

  val generalDocumentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    coroutineScope.launch {
      uris.forEach { uri ->
        val saved = AttachmentHelper.saveUriToInternalStorage(
          context = context,
          sourceUri = uri,
          preferredCategory = selectedCategoryForUpload
        )
        viewModel.addAttachment(
          fileId = fileId,
          category = selectedCategoryForUpload,
          fileName = saved.fileName,
          fileType = saved.fileType,
          fileSizeBytes = saved.fileSizeBytes,
          fileUri = saved.fileUri
        )
      }
    }
  }

  val customCategoryPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    coroutineScope.launch {
      uris.forEach { uri ->
        val saved = AttachmentHelper.saveUriToInternalStorage(
          context = context,
          sourceUri = uri,
          preferredCategory = selectedCategoryForUpload
        )
        viewModel.addAttachment(
          fileId = fileId,
          category = selectedCategoryForUpload,
          fileName = saved.fileName,
          fileType = saved.fileType,
          fileSizeBytes = saved.fileSizeBytes,
          fileUri = saved.fileUri
        )
      }
    }
  }
  var customerName by remember { mutableStateOf("") }
  var companyName by remember { mutableStateOf("") }
  var officeAddress by remember { mutableStateOf("") }
  var mobile by remember { mutableStateOf("") }
  var altMobile by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var ccNumber by remember { mutableStateOf("") }
  var assignedRmCode by remember { mutableStateOf(currentUser.rmCode) }
  val allRms by viewModel.allRms.collectAsState()
  var rmDropdownExpanded by remember { mutableStateOf(false) }
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (isGranted) {
      coroutineScope.launch {
        isDetectingLocation = true
        try {
          val result = LocationHelper.getCurrentLocation(context)
          officeAddress = result.address
          locationDetectionSuccess = true
          viewModel.updateUserLocation(
            rmCode = currentUser.rmCode,
            lat = result.latitude,
            lng = result.longitude,
            address = result.address,
            sourceAction = "GPS_AUTO_DETECT",
            fileId = fileId
          )
        } catch (e: Exception) {
          errorMessage = "Location error: ${e.message}"
        } finally {
          isDetectingLocation = false
        }
      }
    } else {
      errorMessage = "Location permission is required to auto-detect current address."
    }
  }

  fun triggerLocationDetection() {
    if (LocationHelper.hasLocationPermission(context)) {
      coroutineScope.launch {
        isDetectingLocation = true
        try {
          val result = LocationHelper.getCurrentLocation(context)
          officeAddress = result.address
          locationDetectionSuccess = true
          viewModel.updateUserLocation(
            rmCode = currentUser.rmCode,
            lat = result.latitude,
            lng = result.longitude,
            address = result.address,
            sourceAction = "GPS_AUTO_DETECT",
            fileId = fileId
          )
        } catch (e: Exception) {
          errorMessage = "Location error: ${e.message}"
        } finally {
          isDetectingLocation = false
        }
      }
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  val dynamicProductTypes by viewModel.productTypes.collectAsState()
  val dynamicPendingDocOptions by viewModel.pendingDocsOptions.collectAsState()

  val productOptions = if (dynamicProductTypes.isNotEmpty()) dynamicProductTypes else listOf("Credit Card", "B2B", "Corporate Card", "Split", "Limit Enhancement")
  var selectedProductType by remember { mutableStateOf(productOptions.first()) }
  var productExpanded by remember { mutableStateOf(false) }

  // Includes "Analyst Receive" immediately after "Collected", "Submitted" as requested!
  val statusOptions = listOf("Collected", "Submitted", "Analyst Receive", "Approved", "Declined", "Query", "Return to Source", "Condition", "STC")
  var selectedApplicationStatus by remember { mutableStateOf("Collected") }
  var statusExpanded by remember { mutableStateOf(false) }

  val activeOptions = listOf("Y", "N", "C")
  var selectedActiveStatus by remember { mutableStateOf("N") }
  var activeExpanded by remember { mutableStateOf(false) }

  val allPendingDocOptions = if (dynamicPendingDocOptions.isNotEmpty()) dynamicPendingDocOptions else listOf(
    "NID", "TIN", "Office ID", "Salary Certificate",
    "Account Statement (6 Months)", "BIN", "Trade License 2024-25",
    "Trade License 2025-26", "Trade License 2026-27",
    "Loan Certificate", "Card Statement (Month)", "Card Copy"
  )
  val selectedPendingDocs = remember { mutableStateListOf<String>() }

  var remarks by remember { mutableStateOf("") }

  // CPV Fields
  val cpvStatusOptions = listOf("Pending", "Completed", "Failed", "Not Required")
  var selectedCpvStatus by remember { mutableStateOf("Pending") }
  var cpvExpanded by remember { mutableStateOf(false) }
  var cpvDate by remember { mutableStateOf(DateUtils.formatIsoDate(DateUtils.currentDhakaMillis())) }
  var cpvAddress by remember { mutableStateOf("") }
  var cpvRemarks by remember { mutableStateOf("") }

  // Attachments State
  val attachmentsFlow = remember(fileId) { viewModel.eblRepository.getAttachmentsForFileFlow(fileId) }
  val attachments by attachmentsFlow.collectAsState(initial = emptyList())
  var showAddAttachmentDialog by remember { mutableStateOf(false) }
  var previewAttachment by remember { mutableStateOf<FileAttachmentEntity?>(null) }

  var isSaving by remember { mutableStateOf(false) }

  // Load existing file data if editing
  LaunchedEffect(editFileId) {
    if (editFileId != null) {
      val file = viewModel.eblRepository.getFileById(editFileId)
      if (file != null) {
        existingFile = file
        fileId = file.fileId
        customerName = file.customerName
        companyName = file.companyName
        officeAddress = file.officeAddress
        mobile = file.mobile
        altMobile = file.altMobile
        email = file.email
        ccNumber = file.ccNumber
        assignedRmCode = file.assignedRmCode
        selectedProductType = file.productType
        selectedApplicationStatus = file.applicationStatus
        selectedActiveStatus = file.activeStatus
        selectedPendingDocs.clear()
        if (file.pendingDocuments.isNotBlank()) {
          selectedPendingDocs.addAll(file.pendingDocuments.split(",").filter { it.isNotBlank() })
        }
        remarks = file.remarks
        selectedCpvStatus = file.cpvStatus
        cpvDate = file.cpvDate
        cpvAddress = file.cpvAddress
        cpvRemarks = file.cpvRemarks
      }
      isLoaded = true
    }
  }

  fun handleSave() {
    if (customerName.isBlank()) {
      errorMessage = "Customer Name is required."
      return
    }
    if (mobile.isBlank()) {
      errorMessage = "Mobile number is required."
      return
    }
    if (companyName.isBlank()) {
      errorMessage = "Office / Company name is required."
      return
    }

    isSaving = true
    errorMessage = null

    coroutineScope.launch {
      var currentLat: Double? = null
      var currentLng: Double? = null
      var currentAddr: String? = null

      // Automatically capture RM's location when creating or submitting file
      if (LocationHelper.hasLocationPermission(context)) {
        try {
          val loc = LocationHelper.getCurrentLocation(context)
          currentLat = loc.latitude
          currentLng = loc.longitude
          currentAddr = loc.address
        } catch (_: Exception) {}
      }

      viewModel.saveCustomerFile(
        fileId = fileId,
        customerName = customerName,
        companyName = companyName,
        officeAddress = officeAddress,
        mobile = mobile,
        altMobile = altMobile,
        email = email,
        productType = selectedProductType,
        applicationStatus = selectedApplicationStatus,
        activeStatus = selectedActiveStatus,
        assignedRmCode = assignedRmCode,
        ccNumber = ccNumber,
        pendingDocuments = selectedPendingDocs.toList(),
        remarks = remarks,
        cpvStatus = selectedCpvStatus,
        cpvDate = cpvDate,
        cpvAddress = cpvAddress,
        cpvRemarks = cpvRemarks,
        submissionLatitude = currentLat,
        submissionLongitude = currentLng,
        submissionAddress = currentAddr
      ) { success, targetId ->
        isSaving = false
        if (success && targetId != null) {
          viewModel.resetFilters()
          onSaveSuccess(targetId)
        } else {
          errorMessage = "Failed to save customer file. Please check permissions."
        }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("customer_file_form")
  ) {
    // Top Title & Notice
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = EblNavyDark)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (editFileId == null) "New Customer File Entry" else "Edit Customer File",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "CC-Number: ${if (ccNumber.isNotBlank()) ccNumber else fileId} | RM: $assignedRmCode",
            fontSize = 12.sp,
            color = EblGold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (errorMessage != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Text(text = errorMessage!!, color = Color(0xFFB91C1C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
      }
      Spacer(modifier = Modifier.height(14.dp))
    }

    // SECTION A: Customer Information
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "A. Customer & Account Details",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        // CC-Number is the primary identifier
        VoiceInputField(
          value = ccNumber,
          onValueChange = { ccNumber = it },
          label = "CC-Number (Account / Reference No.) *",
          placeholder = "e.g. 4532-8901-2345-6789 or CC-9982",
          leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = EblNavyPrimary) },
          testTag = "form_cc_number"
        )

        // Admin & Mentor RM Reassignment Control
        if (currentUser.role == "ADMIN" || currentUser.role == "MENTOR") {
          Spacer(modifier = Modifier.height(10.dp))
          ExposedDropdownMenuBox(
            expanded = rmDropdownExpanded,
            onExpandedChange = { rmDropdownExpanded = !rmDropdownExpanded }
          ) {
            val assignedRmUser = allRms.find { it.rmCode.equals(assignedRmCode, ignoreCase = true) }
            val rmDisplayName = if (assignedRmUser != null) {
              "${assignedRmUser.name} (${assignedRmUser.rmCode})"
            } else {
              "RM: $assignedRmCode"
            }

            OutlinedTextField(
              value = rmDisplayName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Assigned RM Officer * (Admin/Mentor Control)") },
              leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = EblNavyPrimary) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rmDropdownExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_assigned_rm"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EblNavyPrimary,
                focusedContainerColor = Color(0xFFEFF6FF)
              )
            )

            ExposedDropdownMenu(
              expanded = rmDropdownExpanded,
              onDismissRequest = { rmDropdownExpanded = false }
            ) {
              allRms.forEach { rm ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(text = "${rm.name} (${rm.rmCode})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text(text = "Mobile: ${rm.mobile.ifBlank { "N/A" }} • ${rm.accountStatus}", fontSize = 11.sp, color = Color.Gray)
                    }
                  },
                  onClick = {
                    assignedRmCode = rm.rmCode
                    rmDropdownExpanded = false
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        VoiceInputField(
          value = customerName,
          onValueChange = { customerName = it; errorMessage = null },
          label = "Customer Full Name *",
          placeholder = "e.g. Kazi Mahbubur Rahman",
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EblNavyPrimary) },
          testTag = "form_customer_name"
        )

        Spacer(modifier = Modifier.height(10.dp))

        VoiceInputField(
          value = companyName,
          onValueChange = { companyName = it; errorMessage = null },
          label = "Office / Company Name *",
          placeholder = "e.g. Square Pharmaceuticals Ltd",
          testTag = "form_company_name"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Office Address with Track My Location Button
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Office Address",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = EblNavyDark
            )

            // Track My Location button requested by user
            Button(
              onClick = { triggerLocationDetection() },
              enabled = !isDetectingLocation,
              colors = ButtonDefaults.buttonColors(
                containerColor = EblNavyPrimary,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_track_my_location")
            ) {
              if (isDetectingLocation) {
                CircularProgressIndicator(
                  modifier = Modifier.size(13.dp),
                  color = Color.White,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tracking...", fontSize = 11.sp)
              } else {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = "Track Location",
                  modifier = Modifier.size(14.dp),
                  tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("📍 Track My Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          VoiceInputField(
            value = officeAddress,
            onValueChange = {
              officeAddress = it
              locationDetectionSuccess = false
            },
            label = "Office Address (or use Track My Location)",
            placeholder = "e.g. Square Centre, 48 Mohakhali C/A, Dhaka",
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EblNavyPrimary) },
            testTag = "form_office_address"
          )

          if (locationDetectionSuccess) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "✓ Current GPS location auto-added to address",
              fontSize = 11.sp,
              color = Color(0xFF059669),
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          VoiceInputField(
            value = mobile,
            onValueChange = { mobile = it; errorMessage = null },
            label = "Mobile Number *",
            placeholder = "017XXXXXXXX",
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EblNavyPrimary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
            testTag = "form_mobile"
          )
          VoiceInputField(
            value = altMobile,
            onValueChange = { altMobile = it },
            label = "Alt Mobile",
            placeholder = "018XXXXXXXX",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
            testTag = "form_alt_mobile"
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        VoiceInputField(
          value = email,
          onValueChange = { email = it },
          label = "Email Address (Optional)",
          placeholder = "customer@domain.com",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          testTag = "form_email"
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION B, C, D: Product, Application Status, Active Status
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "B, C, D. Product & Status Classification",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Product Type
        ExposedDropdownMenuBox(
          expanded = productExpanded,
          onExpandedChange = { productExpanded = !productExpanded },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = selectedProductType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Product Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("form_product_type")
          )
          ExposedDropdownMenu(
            expanded = productExpanded,
            onDismissRequest = { productExpanded = false }
          ) {
            productOptions.forEach { opt ->
              DropdownMenuItem(
                text = { Text(opt) },
                onClick = {
                  selectedProductType = opt
                  productExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Application Status
        ExposedDropdownMenuBox(
          expanded = statusExpanded,
          onExpandedChange = { statusExpanded = !statusExpanded },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = selectedApplicationStatus,
            onValueChange = {},
            readOnly = true,
            label = { Text("Application Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("form_app_status")
          )
          ExposedDropdownMenu(
            expanded = statusExpanded,
            onDismissRequest = { statusExpanded = false }
          ) {
            statusOptions.forEach { opt ->
              DropdownMenuItem(
                text = { Text(opt) },
                onClick = {
                  selectedApplicationStatus = opt
                  statusExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Update Photos Upload Box
        val statusPhotos = attachments.filter { it.category == "Status Update Photo" }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFFF0F9FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Status Update Photos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
              }
              Text(
                text = if (statusPhotos.isEmpty()) "Optional photo proofs" else "✓ ${statusPhotos.size} photo(s)",
                fontSize = 11.sp,
                color = if (statusPhotos.isEmpty()) Color.Gray else Color(0xFF0284C7),
                fontWeight = if (statusPhotos.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Upload sanction letter, query slips, client receipts, or verification photos for status '$selectedApplicationStatus'.",
              fontSize = 10.sp,
              color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
              onClick = {
                statusPhotoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("upload_status_photos_btn")
            ) {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Upload Status Photo(s) [Multiple]", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            if (statusPhotos.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                statusPhotos.forEach { photo ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7DD3FC)),
                    modifier = Modifier.clickable { previewAttachment = photo }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(photo.fileName.take(16) + if (photo.fileName.length > 16) "..." else "", fontSize = 10.sp)
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp).clickable {
                          viewModel.deleteAttachment(photo.attachmentId, fileId)
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Status
        ExposedDropdownMenuBox(
          expanded = activeExpanded,
          onExpandedChange = { activeExpanded = !activeExpanded },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = when (selectedActiveStatus) {
              "Y" -> "Active (Y)"
              "N" -> "Inactive (N)"
              "C" -> "Cancelled / Closed (C)"
              else -> selectedActiveStatus
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Status (Y/N/C)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("form_active_status")
          )
          ExposedDropdownMenu(
            expanded = activeExpanded,
            onDismissRequest = { activeExpanded = false }
          ) {
            activeOptions.forEach { opt ->
              val label = when (opt) {
                "Y" -> "Active (Y)"
                "N" -> "Inactive (N)"
                "C" -> "Cancelled / Closed (C)"
                else -> opt
              }
              DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                  selectedActiveStatus = opt
                  activeExpanded = false
                }
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION E: Pending Documents Checklist
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "E. Pending Documents Checklist",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Text(
            text = "${selectedPendingDocs.size} Pending",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selectedPendingDocs.isNotEmpty()) Color(0xFFC2410C) else Color(0xFF15803D)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Select all documents that are currently missing or required from customer:",
          fontSize = 12.sp,
          color = Color.Gray
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          allPendingDocOptions.forEach { docName ->
            val isChecked = selectedPendingDocs.contains(docName)
            FilterChip(
              selected = isChecked,
              onClick = {
                if (isChecked) {
                  selectedPendingDocs.remove(docName)
                } else {
                  selectedPendingDocs.add(docName)
                }
              },
              label = { Text(docName, fontSize = 11.sp) },
              leadingIcon = if (isChecked) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              modifier = Modifier.testTag("doc_chip_${docName.take(6)}")
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION F: Remarks
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "F. Remarks / Requirements",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Spacer(modifier = Modifier.height(10.dp))
        VoiceInputField(
          value = remarks,
          onValueChange = { remarks = it },
          label = "Remarks & Follow-up Notes",
          placeholder = "Enter customer notes, missing documents, query justifications, or requirements...",
          singleLine = false,
          maxLines = 5,
          testTag = "form_remarks"
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION: ADDITIONAL SUPPORTING DOCUMENTS
    Text(
      text = "Additional Supporting Documents",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = EblNavyDark
    )
    Text(
      text = "Attach client KYC papers, trade licenses, salary slips, or other relevant files.",
      fontSize = 11.sp,
      color = Color.Gray
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Supporting Documents (Green Card)
    val otherDocs = attachments.filter { it.category != "Status Update Photo" && it.category != "CPV Photo" }
    Card(
      modifier = Modifier.fillMaxWidth().testTag("upload_others_doc"),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF059669), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Upload Supporting Documents",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = Color(0xFF064E3B)
        )
        Text(
          text = if (otherDocs.isEmpty()) "Attach NID, Trade License, Bank Statements, PDF/Word [Multiple]" else "✓ ${otherDocs.size} Document(s) Attached (Tap to Add More)",
          fontSize = 11.sp,
          color = Color(0xFF059669),
          fontWeight = if (otherDocs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              selectedCategoryForUpload = "General Document"
              generalDocumentPickerLauncher.launch(arrayOf("*/*"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            modifier = Modifier.weight(1f).height(38.dp)
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("PDF / ফাইল", fontSize = 11.sp)
          }

          Button(
            onClick = {
              othersPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.weight(1f).height(38.dp)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ছবি / ফটো", fontSize = 11.sp)
          }
        }

        if (otherDocs.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            otherDocs.forEach { doc ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7)),
                modifier = Modifier.clickable { previewAttachment = doc }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    if (doc.fileType.contains("pdf")) Icons.Default.Description else Icons.Default.Image,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = doc.fileName.take(14) + if (doc.fileName.length > 14) "..." else "",
                    fontSize = 10.sp,
                    color = Color(0xFF065F46)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp).clickable {
                      viewModel.deleteAttachment(doc.attachmentId, fileId)
                    }
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION H: CPV (CONTACT POINT VERIFICATION) STATUS - MATCHING SCREENSHOT
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
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
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "H. CPV (CONTACT POINT VERIFICATION) STATUS",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CPV Status Dropdown
        ExposedDropdownMenuBox(
          expanded = cpvExpanded,
          onExpandedChange = { cpvExpanded = !cpvExpanded },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = selectedCpvStatus,
            onValueChange = {},
            readOnly = true,
            label = { Text("CPV Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cpvExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("form_cpv_status")
          )
          ExposedDropdownMenu(
            expanded = cpvExpanded,
            onDismissRequest = { cpvExpanded = false }
          ) {
            cpvStatusOptions.forEach { opt ->
              DropdownMenuItem(
                text = { Text(opt) },
                onClick = {
                  selectedCpvStatus = opt
                  cpvExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CPV Verification Date
        VoiceInputField(
          value = cpvDate,
          onValueChange = { cpvDate = it },
          label = "CPV Verification Date",
          placeholder = "e.g. 2026-10-15",
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EblNavyPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          testTag = "form_cpv_date"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // CPV Verified Address with Auto-Detect Location Button
        VoiceInputField(
          value = cpvAddress,
          onValueChange = { cpvAddress = it },
          label = "CPV Verified Address",
          placeholder = "Physical address verified in person (or click Auto-Detect)...",
          leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red) },
          trailingIcon = {
            TextButton(
              onClick = {
                if (LocationHelper.hasLocationPermission(context)) {
                  coroutineScope.launch {
                    try {
                      val loc = LocationHelper.getCurrentLocation(context)
                      cpvAddress = loc.address
                    } catch (_: Exception) {}
                  }
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                  )
                }
              }
            ) {
              Text("Auto-Detect", fontSize = 11.sp, color = Color(0xFF2563EB))
            }
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "form_cpv_address"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // CPV Remarks & Field Verification Notes
        VoiceInputField(
          value = cpvRemarks,
          onValueChange = { cpvRemarks = it },
          label = "CPV Remarks & Field Verification Notes",
          placeholder = "Officer remarks on premises, neighboring inquiries...",
          singleLine = false,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
          testTag = "form_cpv_remarks"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Two Action Buttons matching screenshot: Upload CPV Photo & Upload CPV Report / Doc
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              cpvMultiPhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("upload_cpv_photo_btn")
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Upload CPV Photos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }

          OutlinedButton(
            onClick = { showAddAttachmentDialog = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("upload_cpv_doc_btn")
          ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Upload CPV Doc", fontSize = 11.sp)
          }
        }

        // CPV Photos Preview Flow
        val cpvPhotosInCard = attachments.filter { it.category == "CPV Photo" }
        if (cpvPhotosInCard.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "✓ ${cpvPhotosInCard.size} Verification Photo(s) Attached:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7E22CE)
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            cpvPhotosInCard.forEach { photo ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF5FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)),
                modifier = Modifier.clickable { previewAttachment = photo }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(photo.fileName.take(16) + if (photo.fileName.length > 16) "..." else "", fontSize = 10.sp, color = Color(0xFF581C87))
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp).clickable {
                      viewModel.deleteAttachment(photo.attachmentId, fileId)
                    }
                  )
                }
              }
            }
          }
        }
      }
    }

    // Attachments Master List
    if (attachments.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Attached Files (${attachments.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EblNavyDark)
          Spacer(modifier = Modifier.height(8.dp))
          attachments.forEach { att ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .clickable { previewAttachment = att }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (att.fileType.contains("pdf")) Icons.Default.Description else Icons.Default.Image,
                contentDescription = null,
                tint = EblNavyPrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(att.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${att.category} • ${(att.fileSizeBytes / 1024)} KB", fontSize = 10.sp, color = Color.Gray)
              }
              // Quick View button
              IconButton(onClick = { previewAttachment = att }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = "View", tint = EblNavyPrimary, modifier = Modifier.size(16.dp))
              }
              // Quick Download button
              IconButton(onClick = { AttachmentHelper.downloadAttachment(context, att) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
              }
              IconButton(onClick = { viewModel.deleteAttachment(att.attachmentId, fileId) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action Buttons: Save & Cancel
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Cancel")
      }

      if (editFileId != null) {
        OutlinedButton(
          onClick = { showDeleteConfirmDialog = true },
          modifier = Modifier.height(48.dp).testTag("btn_form_delete_record"),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFDC2626))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete", color = Color(0xFFDC2626))
        }
      }

      Button(
        onClick = { handleSave() },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.weight(1.2f).height(48.dp).testTag("btn_save_customer_file"),
        shape = RoundedCornerShape(8.dp)
      ) {
        if (isSaving) {
          Text("Saving...")
        } else {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (editFileId == null) "Submit File" else "Update Record", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
  }

  // Delete Confirmation Dialog for Form
  if (showDeleteConfirmDialog && editFileId != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Delete Customer Record", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
      text = {
        Text("Are you sure you want to delete customer file '${customerName.ifBlank { "Record" }}' ($editFileId)? This will remove it and sync deletion with Google Sheets.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteFile(editFileId)
            showDeleteConfirmDialog = false
            onCancel()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          modifier = Modifier.testTag("btn_confirm_form_delete")
        ) {
          Text("Delete Record")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog to Add Attachment
  if (showAddAttachmentDialog) {
    var catInput by remember { mutableStateOf("NID") }
    var fileNameInput by remember { mutableStateOf("") }
    val categories = listOf("NID", "Account Statement", "Office ID", "Salary Certificate", "CPV Photo", "Trade License", "General")

    AlertDialog(
      onDismissRequest = { showAddAttachmentDialog = false },
      title = { Text("Upload Customer Document", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Select Document Category:", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.forEach { c ->
              FilterChip(
                selected = catInput == c,
                onClick = { catInput = c },
                label = { Text(c, fontSize = 10.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text("Choose File Source from Device:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
          Spacer(modifier = Modifier.height(8.dp))

          // 1. Pick PDF / Word / Any Document from Phone Storage
          Button(
            onClick = {
              selectedCategoryForUpload = catInput
              showAddAttachmentDialog = false
              generalDocumentPickerLauncher.launch(arrayOf("*/*"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            modifier = Modifier.fillMaxWidth().testTag("btn_pick_document_file")
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("PDF / Word / ফাইল নির্বাচন করুন", fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2. Pick Photos from Gallery
          Button(
            onClick = {
              selectedCategoryForUpload = catInput
              showAddAttachmentDialog = false
              customCategoryPhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth().testTag("btn_pick_gallery_photo")
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ছবি / গ্যালারি থেকে নির্বাচন করুন", fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text("বা ম্যানুয়ালি নাম লিখুন:", fontSize = 11.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(4.dp))
          VoiceInputField(
            value = fileNameInput,
            onValueChange = { fileNameInput = it },
            label = "Document Note / Name",
            placeholder = "e.g. NID_Front_Back.pdf",
            modifier = Modifier.fillMaxWidth(),
            testTag = "input_attachment_name"
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val name = if (fileNameInput.isBlank()) "${catInput}_doc.pdf" else fileNameInput.trim()
            val isPdf = name.endsWith(".pdf", ignoreCase = true)
            viewModel.addAttachment(
              fileId = fileId,
              category = catInput,
              fileName = name,
              fileType = if (isPdf) "application/pdf" else "image/jpeg",
              fileSizeBytes = (200..1200).random() * 1024L,
              fileUri = ""
            )
            showAddAttachmentDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
        ) {
          Text("সংরক্ষণ করুন")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddAttachmentDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Full-featured Preview & Download Attachment Dialog
  val currentPreview = previewAttachment
  if (currentPreview != null) {
    AttachmentViewerDialog(
      attachment = currentPreview,
      onDismiss = { previewAttachment = null },
      onDelete = {
        viewModel.deleteAttachment(currentPreview.attachmentId, fileId)
        previewAttachment = null
      }
    )
  }
}
