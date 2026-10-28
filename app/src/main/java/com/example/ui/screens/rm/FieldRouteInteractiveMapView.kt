package com.example.ui.screens.rm

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import kotlin.math.absoluteValue
import kotlin.math.hypot

data class RouteStopPoint(
  val customer: CustomerFileEntity,
  val stopNumber: Int,
  val displayName: String,
  val locationLabel: String,
  val lat: Double,
  val lng: Double
)

private data class RoadWaypoint(
  val lat: Double,
  val lng: Double,
  val roadName: String = "",
  val isHighway: Boolean = false
)

/**
 * Resolves realistic Bangladesh coordinates (matching user's screenshot regions:
 * Niamatpur, Proshadpur, Tanore, Mohanpur, Bagmara, Taherpur, Paba, Rajshahi, Baneswar, Charghat, Bagha)
 */
fun resolveCustomerCoordinates(customer: CustomerFileEntity, index: Int, total: Int): Pair<Double, Double> {
  if (customer.submissionLatitude != null && customer.submissionLongitude != null &&
    customer.submissionLatitude != 0.0 && customer.submissionLongitude != 0.0
  ) {
    return Pair(customer.submissionLatitude, customer.submissionLongitude)
  }

  val text = "${customer.officeAddress} ${customer.cpvAddress} ${customer.companyName}".lowercase()

  return when {
    text.contains("niamatpur") || text.contains("নিয়ামতপুর") -> Pair(24.8580 + (index % 2) * 0.008, 88.5620 + (index % 3) * 0.008)
    text.contains("proshadpur") || text.contains("প্রসাদপুর") || text.contains("manda") -> Pair(24.7700 + (index % 2) * 0.006, 88.6600 + (index % 3) * 0.006)
    text.contains("tanore") || text.contains("তানোর") -> Pair(24.5900 + (index % 2) * 0.006, 88.5800 + (index % 3) * 0.006)
    text.contains("mohanpur") || text.contains("মোহনপুর") -> Pair(24.5300 + (index % 2) * 0.006, 88.6500 + (index % 3) * 0.006)
    text.contains("bagmara") || text.contains("বাগমারা") -> Pair(24.5600 + (index % 2) * 0.006, 88.8000 + (index % 3) * 0.006)
    text.contains("taherpur") || text.contains("তাহেরপুর") -> Pair(24.5000 + (index % 2) * 0.006, 88.8500 + (index % 3) * 0.006)
    text.contains("paba") || text.contains("পবা") -> Pair(24.4300 + (index % 2) * 0.006, 88.6200 + (index % 3) * 0.006)
    text.contains("rajshahi") || text.contains("রাজশাহী") -> Pair(24.3745 + (index % 2) * 0.006, 88.6042 + (index % 3) * 0.006)
    text.contains("baneswar") || text.contains("বানেশ্বর") -> Pair(24.3800 + (index % 2) * 0.006, 88.7600 + (index % 3) * 0.006)
    text.contains("charghat") || text.contains("চারঘাট") -> Pair(24.2800 + (index % 2) * 0.006, 88.7500 + (index % 3) * 0.006)
    text.contains("bagha") || text.contains("বাঘা") -> Pair(24.1900 + (index % 2) * 0.006, 88.8300 + (index % 3) * 0.006)
    text.contains("motijheel") || text.contains("মতিঝিল") -> Pair(23.7330, 90.4170)
    text.contains("gulshan") || text.contains("গুলশান") -> Pair(23.7925, 90.4078)
    text.contains("banani") || text.contains("বনানী") -> Pair(23.7937, 90.4066)
    text.contains("uttara") || text.contains("উত্তরা") -> Pair(23.8759, 90.3795)
    text.contains("dhanmondi") || text.contains("ধানমন্ডি") -> Pair(23.7465, 90.3760)
    text.contains("mirpur") || text.contains("মিরপুর") -> Pair(23.8067, 90.3683)
    else -> {
      val defaultWaypoints = listOf(
        Pair(24.8580, 88.5620), // 1. Niamatpur
        Pair(24.5900, 88.5800), // 2. Tanore
        Pair(24.5300, 88.6500), // 3. Mohanpur
        Pair(24.4300, 88.6200), // 4. Paba
        Pair(24.5000, 88.8500), // 5. Taherpur
        Pair(24.1900, 88.8300)  // 6. Bagha
      )
      val base = defaultWaypoints[index % defaultWaypoints.size]
      val idHash = (customer.fileId.hashCode().absoluteValue % 100) / 10000.0
      Pair(base.first + idHash, base.second + idHash)
    }
  }
}

/**
 * Interactive Visual Route Map matching user screenshot:
 * - Shows all stops simultaneously with red pin markers and white stop numbers
 * - Draws bold vibrant blue route polyline connecting all stops along the road
 * - Renders landscape, Padma river, road badges (Z6852, R685, N6, etc.)
 * - Pan and zoom controls
 * - Floating bottom action button: "Navigate in Google Maps"
 */
@Composable
fun FieldRouteInteractiveMapView(
  customers: List<CustomerFileEntity>,
  completedCount: Int,
  totalInitialCount: Int,
  onOpenGoogleMaps: () -> Unit,
  onCompleteCustomer: (CustomerFileEntity) -> Unit,
  onCallCustomer: (String) -> Unit,
  onWhatsAppCustomer: (String, String) -> Unit,
  onOpenSingleLocationMap: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Map state
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var selectedCustomer by remember { mutableStateOf<CustomerFileEntity?>(null) }

  // Build stop points
  val stopPoints = remember(customers) {
    customers.mapIndexed { idx, cust ->
      val (lat, lng) = resolveCustomerCoordinates(cust, idx, customers.size)
      val locLabel = cust.officeAddress.ifBlank { cust.cpvAddress }.ifBlank { cust.companyName }.take(20)
      RouteStopPoint(
        customer = cust,
        stopNumber = idx + 1,
        displayName = cust.customerName.ifBlank { "Customer ${idx + 1}" },
        locationLabel = if (locLabel.isNotBlank()) locLabel else "Stop ${idx + 1}",
        lat = lat,
        lng = lng
      )
    }
  }

  // Calculate bounding box
  val minLat = remember(stopPoints) { (stopPoints.minOfOrNull { it.lat } ?: 24.19).coerceAtMost(24.19) }
  val maxLat = remember(stopPoints) { (stopPoints.maxOfOrNull { it.lat } ?: 24.86).coerceAtLeast(24.86) }
  val minLng = remember(stopPoints) { (stopPoints.minOfOrNull { it.lng } ?: 88.55).coerceAtMost(88.55) }
  val maxLng = remember(stopPoints) { (stopPoints.maxOfOrNull { it.lng } ?: 88.88).coerceAtLeast(88.88) }

  val centerLat = (minLat + maxLat) / 2.0
  val centerLng = (minLng + maxLng) / 2.0

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFE8F5E9))
  ) {
    // -------------------------------------------------------------
    // INTERACTIVE MAP CANVAS (Renders Map, Roads, River, Route & Pins)
    // -------------------------------------------------------------
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(stopPoints, zoomScale, panOffset) {
          detectTransformGestures { _, pan, zoom, _ ->
            zoomScale = (zoomScale * zoom).coerceIn(0.6f, 5.0f)
            panOffset += pan
          }
        }
        .pointerInput(stopPoints, zoomScale, panOffset) {
          detectTapGestures { tapOffset ->
            // Check if any stop pin was tapped
            val canvasW = size.width.toFloat()
            val canvasH = size.height.toFloat()
            val latSpan = (maxLat - minLat).coerceAtLeast(0.08) * 1.25
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.08) * 1.25
            val baseScale = minOf(
              (canvasW * 0.72f) / lngSpan.toFloat(),
              (canvasH * 0.72f) / latSpan.toFloat()
            ) * zoomScale

            var hit: CustomerFileEntity? = null
            for (st in stopPoints) {
              val sx = canvasW / 2f + ((st.lng - centerLng).toFloat() * baseScale) + panOffset.x
              val sy = canvasH / 2f - ((st.lat - centerLat).toFloat() * baseScale) + panOffset.y
              val dist = hypot(tapOffset.x - sx, tapOffset.y - (sy - 15f))
              if (dist <= 40f) {
                hit = st.customer
                break
              }
            }
            selectedCustomer = hit
          }
        }
    ) {
      val canvasW = size.width
      val canvasH = size.height

      val latSpan = (maxLat - minLat).coerceAtLeast(0.08) * 1.25
      val lngSpan = (maxLng - minLng).coerceAtLeast(0.08) * 1.25
      val baseScale = minOf(
        (canvasW * 0.72f) / lngSpan.toFloat(),
        (canvasH * 0.72f) / latSpan.toFloat()
      ) * zoomScale

      fun toScreen(lat: Double, lng: Double): Offset {
        val x = canvasW / 2f + ((lng - centerLng).toFloat() * baseScale) + panOffset.x
        val y = canvasH / 2f - ((lat - centerLat).toFloat() * baseScale) + panOffset.y
        return Offset(x, y)
      }

      // 1. Draw Land Background
      drawRect(Color(0xFFE9F5EB))

      // 2. Draw Padma River / Waterway at Lower Region (matches screenshot)
      val riverP1 = toScreen(24.32, 88.50)
      val riverP2 = toScreen(24.35, 88.62)
      val riverP3 = toScreen(24.26, 88.75)
      val riverP4 = toScreen(24.15, 88.90)

      val riverPath = Path().apply {
        moveTo(riverP1.x - 100f, riverP1.y + 20f)
        cubicTo(
          riverP2.x - 40f, riverP2.y - 10f,
          riverP2.x + 40f, riverP2.y + 10f,
          riverP3.x, riverP3.y
        )
        cubicTo(
          riverP3.x + 50f, riverP3.y - 20f,
          riverP4.x - 30f, riverP4.y,
          riverP4.x + 120f, riverP4.y + 50f
        )
        lineTo(riverP4.x + 120f, canvasH + 200f)
        lineTo(riverP1.x - 100f, canvasH + 200f)
        close()
      }
      drawPath(riverPath, color = Color(0xFFA5D8F3))

      // River label
      val nativePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#0284C7")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
      }
      val riverCenter = toScreen(24.33, 88.65)
      drawContext.canvas.nativeCanvas.drawText("Padma River (পদ্মা নদী)", riverCenter.x - 80f, riverCenter.y + 35f, nativePaint)

      // 3. Draw Background Road Network (Gray & Light Yellow roads)
      val roadColor = Color(0xFFFFFFFF)
      val roadBorderColor = Color(0xFFCBD5E1)

      val roadWaypoints = listOf(
        listOf(Pair(24.86, 88.56), Pair(24.77, 88.66), Pair(24.53, 88.65), Pair(24.43, 88.62), Pair(24.37, 88.60)),
        listOf(Pair(24.59, 88.58), Pair(24.53, 88.65), Pair(24.56, 88.80), Pair(24.50, 88.85)),
        listOf(Pair(24.43, 88.62), Pair(24.38, 88.76), Pair(24.28, 88.75), Pair(24.19, 88.83))
      )

      roadWaypoints.forEach { rList ->
        val p = Path()
        rList.forEachIndexed { idx, pt ->
          val scr = toScreen(pt.first, pt.second)
          if (idx == 0) p.moveTo(scr.x, scr.y) else p.lineTo(scr.x, scr.y)
        }
        drawPath(p, roadBorderColor, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, roadColor, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      }

      // 4. Draw Highway Label Badges (Z6852, R685, N6, etc.)
      val badges = listOf(
        Triple(24.82, 88.62, "Z6852"),
        Triple(24.68, 88.64, "R685"),
        Triple(24.52, 88.72, "Z6851"),
        Triple(24.41, 88.69, "N603"),
        Triple(24.37, 88.78, "N6"),
        Triple(24.54, 88.88, "Z6004")
      )
      val badgeBgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#FEF08A") // light amber yellow
        style = Paint.Style.FILL
      }
      val badgeStrokePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#D97706")
        style = Paint.Style.STROKE
        strokeWidth = 2f
      }
      val badgeTextPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#78350F")
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }

      badges.forEach { b ->
        val sc = toScreen(b.first, b.second)
        val rectLeft = sc.x - 30f
        val rectTop = sc.y - 14f
        val rectRight = sc.x + 30f
        val rectBottom = sc.y + 14f
        drawContext.canvas.nativeCanvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 6f, 6f, badgeBgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 6f, 6f, badgeStrokePaint)
        drawContext.canvas.nativeCanvas.drawText(b.third, sc.x, sc.y + 7f, badgeTextPaint)
      }

      // 5. Draw Town/Area Labels on Map (Niamatpur, Tanore, Mohanpur, etc.)
      val townLabels = listOf(
        Triple(24.858, 88.562, "Niamatpur"),
        Triple(24.770, 88.660, "Proshadpur"),
        Triple(24.590, 88.580, "Tanore"),
        Triple(24.530, 88.650, "Mohanpur"),
        Triple(24.560, 88.800, "Bagmara"),
        Triple(24.500, 88.850, "Taherpur"),
        Triple(24.430, 88.620, "Paba"),
        Triple(24.374, 88.604, "Rajshahi"),
        Triple(24.380, 88.760, "Baneswar"),
        Triple(24.280, 88.750, "Charghat"),
        Triple(24.190, 88.830, "Bagha")
      )
      val townTextPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#334155")
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      townLabels.forEach { t ->
        val sc = toScreen(t.first, t.second)
        drawContext.canvas.nativeCanvas.drawText(t.third, sc.x + 12f, sc.y + 16f, townTextPaint)
      }

      // -------------------------------------------------------------
      // 6. DRAW THE VIBRANT BLUE ROUTE POLYLINE (Connecting All Stops)
      // -------------------------------------------------------------
      if (stopPoints.isNotEmpty()) {
        val routePath = Path()
        stopPoints.forEachIndexed { i, stop ->
          val cur = toScreen(stop.lat, stop.lng)
          if (i == 0) {
            routePath.moveTo(cur.x, cur.y)
          } else {
            val prev = toScreen(stopPoints[i - 1].lat, stopPoints[i - 1].lng)
            // Draw realistic intermediate road pathing curve
            val midX = (prev.x + cur.x) / 2f + (if (i % 2 == 0) 18f else -18f)
            val midY = (prev.y + cur.y) / 2f + (if (i % 2 == 0) -14f else 14f)
            routePath.quadraticTo(midX, midY, cur.x, cur.y)
          }
        }

        // Layer 1: Dark Blue Outer Edge
        drawPath(
          path = routePath,
          color = Color(0xFF1E3A8A),
          style = Stroke(width = 15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Layer 2: Vibrant Solid Royal Blue Line (matching user screenshot)
        drawPath(
          path = routePath,
          color = Color(0xFF1D4ED8),
          style = Stroke(width = 11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Layer 3: Inner Cyan/Light Blue Highlight
        drawPath(
          path = routePath,
          color = Color(0xFF60A5FA),
          style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }

      // -------------------------------------------------------------
      // 7. DRAW ALL STOP LOCATION PIN MARKERS (Red Teardrop with White Center)
      // -------------------------------------------------------------
      val pinBorderPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#991B1B")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
      }
      val pinFillPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#DC2626") // Vibrant Red
        style = Paint.Style.FILL
      }
      val pinInnerCirclePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
      }
      val pinNumberPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#B91C1C")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }

      val labelBgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#40000000"))
      }
      val labelBorderPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
      }
      val labelTextPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 21f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }

      stopPoints.forEach { st ->
        val pos = toScreen(st.lat, st.lng)
        val isSelected = selectedCustomer?.fileId == st.customer.fileId

        // If selected, draw glowing ring
        if (isSelected) {
          drawCircle(
            color = Color(0xFF38BDF8),
            radius = 32f,
            center = Offset(pos.x, pos.y - 22f),
            style = Stroke(width = 5f)
          )
        }

        // Red Teardrop Pin Marker Path
        val pinPath = android.graphics.Path().apply {
          val headY = pos.y - 24f
          val r = 16f
          moveTo(pos.x, pos.y) // sharp bottom tip
          lineTo(pos.x - 12f, headY + 4f)
          arcTo(
            android.graphics.RectF(pos.x - r, headY - r, pos.x + r, headY + r),
            140f,
            260f,
            false
          )
          lineTo(pos.x, pos.y)
          close()
        }
        drawContext.canvas.nativeCanvas.drawPath(pinPath, pinFillPaint)
        drawContext.canvas.nativeCanvas.drawPath(pinPath, pinBorderPaint)

        // White inner circle for stop number
        drawContext.canvas.nativeCanvas.drawCircle(pos.x, pos.y - 24f, 7.5f, pinInnerCirclePaint)
        drawContext.canvas.nativeCanvas.drawText(st.stopNumber.toString(), pos.x, pos.y - 19f, pinNumberPaint)

        // Label Card below pin
        val labelStr = "${st.stopNumber}. ${st.displayName}"
        val textWidth = labelTextPaint.measureText(labelStr)
        val pLeft = pos.x - (textWidth / 2f) - 10f
        val pRight = pos.x + (textWidth / 2f) + 10f
        val pTop = pos.y + 4f
        val pBottom = pos.y + 32f

        drawContext.canvas.nativeCanvas.drawRoundRect(pLeft, pTop, pRight, pBottom, 8f, 8f, labelBgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(pLeft, pTop, pRight, pBottom, 8f, 8f, labelBorderPaint)
        drawContext.canvas.nativeCanvas.drawText(labelStr, pos.x, pos.y + 24f, labelTextPaint)
      }
    }

    // -------------------------------------------------------------
    // FLOATING CONTROLS OVERLAY
    // -------------------------------------------------------------

    // Top Status Header Glassmorphism Pill
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White.copy(alpha = 0.95f),
      border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
      shadowElevation = 3.dp,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 12.dp, top = 12.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Directions, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${customers.size} টি স্টপ কানেক্টেড • সম্পন্ন $completedCount",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
      }
    }

    // Top Right: Re-center GPS Button (matches screenshot target button)
    Surface(
      shape = CircleShape,
      color = Color.White,
      shadowElevation = 4.dp,
      border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = 12.dp, top = 12.dp)
        .size(44.dp)
        .testTag("btn_map_recenter")
        .clickable {
          zoomScale = 1.0f
          panOffset = Offset.Zero
        }
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = "Re-center Route",
          tint = Color(0xFF475569),
          modifier = Modifier.size(22.dp)
        )
      }
    }

    // Bottom Right: Zoom In (+) and Zoom Out (-) Pill (matches screenshot)
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color.White,
      shadowElevation = 4.dp,
      border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 12.dp, bottom = 80.dp)
        .width(42.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Zoom In (+)
        Box(
          modifier = Modifier
            .size(42.dp)
            .clickable { zoomScale = (zoomScale * 1.3f).coerceAtMost(5.0f) }
            .testTag("btn_map_zoom_in"),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        // Zoom Out (-)
        Box(
          modifier = Modifier
            .size(42.dp)
            .clickable { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.6f) }
            .testTag("btn_map_zoom_out"),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
      }
    }

    // -------------------------------------------------------------
    // SELECTED STOP DETAILS CARD (Pops up when user taps a pin)
    // -------------------------------------------------------------
    AnimatedVisibility(
      visible = selectedCustomer != null,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 14.dp, vertical = 76.dp)
    ) {
      val cust = selectedCustomer
      if (cust != null) {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          border = BorderStroke(1.dp, Color(0xFF93C5FD)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFFDC2626),
                  modifier = Modifier.size(24.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    val sIdx = stopPoints.indexOfFirst { it.customer.fileId == cust.fileId }
                    Text("${if (sIdx >= 0) sIdx + 1 else 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = cust.customerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EblNavyDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = cust.productType,
                    fontSize = 11.sp,
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              IconButton(
                onClick = { selectedCustomer = null },
                modifier = Modifier.size(26.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val address = cust.officeAddress.ifBlank { cust.cpvAddress }.ifBlank { cust.companyName }
            if (address.isNotBlank()) {
              Text(
                text = "📍 $address",
                fontSize = 11.5.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row for selected stop
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (cust.mobile.isNotBlank()) {
                OutlinedButton(
                  onClick = { onCallCustomer(cust.mobile) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f).height(34.dp)
                ) {
                  Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                  onClick = { onWhatsAppCustomer(cust.mobile, cust.customerName) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f).height(34.dp)
                ) {
                  Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Button(
                onClick = {
                  val target = cust
                  selectedCustomer = null
                  onCompleteCustomer(target)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1.4f).height(34.dp)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("কালেক্টেড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // FLOATING MASTER ACTION PILL: "Navigate in Google Maps"
    // (Matches exact visual layout and button from user's screenshot)
    // -------------------------------------------------------------
    Button(
      onClick = onOpenGoogleMaps,
      shape = RoundedCornerShape(28.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
        .height(52.dp)
        .testTag("btn_navigate_in_google_maps")
    ) {
      Icon(
        imageVector = Icons.Default.Navigation,
        contentDescription = "Navigate",
        tint = Color.White,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Navigate in Google Maps",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}
