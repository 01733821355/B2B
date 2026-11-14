package com.example.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppTheme

enum class CardNetwork {
  VISA,
  MASTERCARD,
  AMEX,
  UNIONPAY,
  DINERS
}

fun resolveCardNetwork(productType: String, ccNumber: String = ""): CardNetwork {
  val trimmedCc = ccNumber.filter { it.isDigit() }
  when {
    trimmedCc.startsWith("4") -> return CardNetwork.VISA
    trimmedCc.startsWith("5") || trimmedCc.startsWith("2") -> return CardNetwork.MASTERCARD
    trimmedCc.startsWith("34") || trimmedCc.startsWith("37") -> return CardNetwork.AMEX
    trimmedCc.startsWith("30") || trimmedCc.startsWith("36") || trimmedCc.startsWith("38") -> return CardNetwork.DINERS
    trimmedCc.startsWith("62") -> return CardNetwork.UNIONPAY
  }
  val p = productType.lowercase()
  return when {
    p.contains("corp") || p.contains("master") || p.contains("world") || p.contains("titanium") -> CardNetwork.MASTERCARD
    p.contains("b2b") || p.contains("payroll") || p.contains("union") -> CardNetwork.UNIONPAY
    p.contains("amex") || p.contains("platinum") -> CardNetwork.AMEX
    p.contains("diners") || p.contains("split") -> CardNetwork.DINERS
    else -> CardNetwork.VISA
  }
}

/**
 * Photorealistic 3D brushed-gold metallic EMV smart chip.
 */
@Composable
fun EmvGoldChip(modifier: Modifier = Modifier.size(width = 34.dp, height = 25.dp)) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

    // 3D base shadow rim
    drawRoundRect(
      color = Color(0xFF78350F).copy(alpha = 0.65f),
      topLeft = Offset(0f, 1.5.dp.toPx()),
      size = Size(w, h),
      cornerRadius = corner
    )

    // Brushed metallic gold body
    drawRoundRect(
      brush = Brush.linearGradient(
        colors = listOf(
          Color(0xFFFEF08A),
          Color(0xFFF59E0B),
          Color(0xFFFDE68A),
          Color(0xFFD97706)
        ),
        start = Offset.Zero,
        end = Offset(w, h)
      ),
      topLeft = Offset.Zero,
      size = Size(w, h),
      cornerRadius = corner
    )

    // Silicon contact grid lines
    val lineColor = Color(0xFF92400E).copy(alpha = 0.7f)
    val strokeW = 1.dp.toPx()
    drawLine(lineColor, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeW)
    drawLine(lineColor, Offset(w * 0.67f, 0f), Offset(w * 0.67f, h), strokeW)
    drawLine(lineColor, Offset(0f, h * 0.35f), Offset(w * 0.33f, h * 0.35f), strokeW)
    drawLine(lineColor, Offset(0f, h * 0.65f), Offset(w * 0.33f, h * 0.65f), strokeW)
    drawLine(lineColor, Offset(w * 0.67f, h * 0.35f), Offset(w, h * 0.35f), strokeW)
    drawLine(lineColor, Offset(w * 0.67f, h * 0.65f), Offset(w, h * 0.65f), strokeW)
    drawRoundRect(
      color = lineColor,
      topLeft = Offset(w * 0.33f, h * 0.24f),
      size = Size(w * 0.34f, h * 0.52f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
      style = Stroke(width = strokeW)
    )

    // Specular edge rim
    drawRoundRect(
      color = Color.White.copy(alpha = 0.55f),
      topLeft = Offset.Zero,
      size = Size(w, h),
      cornerRadius = corner,
      style = Stroke(width = 0.8.dp.toPx())
    )
  }
}

/**
 * Contactless NFC wave icon.
 */
@Composable
fun ContactlessNfcMark(
  tint: Color = Color.White.copy(alpha = 0.85f),
  modifier: Modifier = Modifier.size(18.dp)
) {
  Canvas(modifier = modifier) {
    val cx = size.width * 0.15f
    val cy = size.height * 0.5f
    val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
    listOf(0.28f, 0.52f, 0.78f).forEach { ratio ->
      val r = size.width * ratio
      drawArc(
        color = tint,
        startAngle = -42f,
        sweepAngle = 84f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.35f, cy - r * 0.5f),
        size = Size(r * 0.7f, r),
        style = stroke
      )
    }
  }
}

/**
 * Crisp card network logo emblem (VISA / Mastercard / AMEX / UnionPay / Diners).
 */
@Composable
fun CardNetworkLogoBadge(
  network: CardNetwork,
  compact: Boolean = false,
  modifier: Modifier = Modifier
) {
  val height = if (compact) 19.dp else 24.dp
  when (network) {
    CardNetwork.VISA -> {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF0A1F44),
        border = BorderStroke(0.75.dp, Color(0xFFF59E0B).copy(alpha = 0.75f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(height)
      ) {
        Box(
          modifier = Modifier
            .background(
              Brush.linearGradient(listOf(Color(0xFF1A3A7E), Color(0xFF0D1E46)))
            )
            .padding(horizontal = if (compact) 6.dp else 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(width = 4.dp, height = if (compact) 9.dp else 11.dp)
                .background(Color(0xFFF59E0B), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "VISA",
              color = Color.White,
              fontSize = if (compact) 9.5.sp else 11.5.sp,
              fontWeight = FontWeight.Black,
              fontStyle = FontStyle.Italic,
              letterSpacing = 0.8.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }

    CardNetwork.MASTERCARD -> {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF111827),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(height)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = if (compact) 5.dp else 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Canvas(modifier = Modifier.size(width = if (compact) 20.dp else 24.dp, height = if (compact) 12.dp else 15.dp)) {
            val r = size.height / 2f
            drawCircle(
              color = Color(0xFFEB001B),
              radius = r,
              center = Offset(r, size.height / 2f)
            )
            drawCircle(
              color = Color(0xFFF79E1B).copy(alpha = 0.92f),
              radius = r,
              center = Offset(size.width - r, size.height / 2f)
            )
          }
          if (!compact) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "MC",
              color = Color.White.copy(alpha = 0.92f),
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }

    CardNetwork.AMEX -> {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF006FCF),
        border = BorderStroke(0.75.dp, Color(0xFF93C5FD).copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(height)
      ) {
        Box(
          modifier = Modifier
            .background(Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))))
            .padding(horizontal = if (compact) 5.dp else 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "AMEX",
            color = Color.White,
            fontSize = if (compact) 8.5.sp else 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }

    CardNetwork.UNIONPAY -> {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(height)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = if (compact) 5.dp else 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Canvas(modifier = Modifier.size(width = if (compact) 16.dp else 20.dp, height = if (compact) 11.dp else 13.dp)) {
            val h = size.height
            val w = size.width
            val redPath = Path().apply {
              moveTo(w * 0.1f, 0f); lineTo(w * 0.42f, 0f); lineTo(w * 0.30f, h); lineTo(0f, h); close()
            }
            val bluePath = Path().apply {
              moveTo(w * 0.40f, 0f); lineTo(w * 0.72f, 0f); lineTo(w * 0.60f, h); lineTo(w * 0.28f, h); close()
            }
            val tealPath = Path().apply {
              moveTo(w * 0.70f, 0f); lineTo(w, 0f); lineTo(w * 0.88f, h); lineTo(w * 0.58f, h); close()
            }
            drawPath(redPath, Color(0xFFDC2626))
            drawPath(bluePath, Color(0xFF1D4ED8))
            drawPath(tealPath, Color(0xFF0D9488))
          }
          if (!compact) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "UnionPay",
              color = Color.White,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }

    CardNetwork.DINERS -> {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(0.75.dp, Color(0xFF94A3B8).copy(alpha = 0.65f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(height)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = if (compact) 5.dp else 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Canvas(modifier = Modifier.size(if (compact) 11.dp else 13.dp)) {
            drawCircle(Color.White, radius = size.minDimension / 2f, style = Stroke(width = 1.6.dp.toPx()))
            drawLine(
              Color.White,
              start = Offset(size.width / 2f, 1.dp.toPx()),
              end = Offset(size.width / 2f, size.height - 1.dp.toPx()),
              strokeWidth = 1.6.dp.toPx()
            )
          }
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (compact) "DC" else "Diners",
            color = Color.White,
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}

/**
 * Miniature 3D Banking Card Badge matched to Product Type (Credit Card, Corporate Card, B2B, Split).
 */
@Composable
fun ProductCardMiniBadge(
  productType: String,
  ccNumber: String = "",
  modifier: Modifier = Modifier
) {
  val network = remember(productType, ccNumber) { resolveCardNetwork(productType, ccNumber) }
  val p = productType.lowercase()
  val gradientColors = when {
    p.contains("corp") -> listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    p.contains("b2b") || p.contains("payroll") -> listOf(Color(0xFF065F46), Color(0xFF064E3B))
    p.contains("limit") || p.contains("split") -> listOf(Color(0xFF4C1D95), Color(0xFF2E1065))
    else -> listOf(Color(0xFF1E3A8A), Color(0xFF0A1F44))
  }
  val rimColor = when {
    p.contains("corp") -> EblGold
    p.contains("b2b") || p.contains("payroll") -> Color(0xFF34D399)
    p.contains("limit") || p.contains("split") -> Color(0xFFC4B5FD)
    else -> Color(0xFF60A5FA)
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color.Transparent,
    shadowElevation = 3.dp,
    border = BorderStroke(0.85.dp, rimColor.copy(alpha = 0.65f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .background(Brush.horizontalGradient(gradientColors))
        .padding(horizontal = 7.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      // Tiny realistic EMV gold square
      Box(
        modifier = Modifier
          .size(width = 11.dp, height = 8.dp)
          .background(
            Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706))),
            RoundedCornerShape(2.dp)
          )
      )
      Text(
        text = productType.ifBlank { "Credit Card" },
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      CardNetworkLogoBadge(network = network, compact = true)
    }
  }
}

/**
 * Strip showing official Banking Card Network logos (VISA, Mastercard, AMEX, UnionPay, Diners).
 */
@Composable
fun CardNetworksShowcaseStrip(
  modifier: Modifier = Modifier,
  darkPill: Boolean = true
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (darkPill) Color.Black.copy(alpha = 0.36f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(
      0.85.dp,
      if (darkPill) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    ),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
      EmvGoldChip(modifier = Modifier.size(width = 24.dp, height = 18.dp))
      CardNetworkLogoBadge(CardNetwork.VISA, compact = false)
      CardNetworkLogoBadge(CardNetwork.MASTERCARD, compact = false)
      CardNetworkLogoBadge(CardNetwork.AMEX, compact = false)
      CardNetworkLogoBadge(CardNetwork.UNIONPAY, compact = false)
      CardNetworkLogoBadge(CardNetwork.DINERS, compact = false)
    }
  }
}

/**
 * Realistic 3D Interactive Banking Card Header for CustomerDetailDialog.
 * Tapping flips/tilts smoothly between Front (Cardholder & CC) and Back (Limit, Analyst, Office) views.
 */
@Composable
fun Realistic3DBankCardPreview(
  file: CustomerFileEntity,
  modifier: Modifier = Modifier
) {
  var showBackSide by remember { mutableStateOf(false) }
  val flipRotation by animateFloatAsState(
    targetValue = if (showBackSide) 180f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "3d_card_flip"
  )
  val isBackVisible = flipRotation > 90f

  val network = remember(file.productType, file.ccNumber) {
    resolveCardNetwork(file.productType, file.ccNumber)
  }

  val p = file.productType.lowercase()
  val cardGradient = when {
    p.contains("corp") -> listOf(Color(0xFF1F2937), Color(0xFF0F172A), Color(0xFF334155))
    p.contains("b2b") || p.contains("payroll") -> listOf(Color(0xFF047857), Color(0xFF065F46), Color(0xFF022C22))
    p.contains("limit") || p.contains("split") -> listOf(Color(0xFF5B21B6), Color(0xFF3B0764), Color(0xFF1E1B4B))
    else -> listOf(Color(0xFF1D4ED8), Color(0xFF0F2952), Color(0xFF071329))
  }
  val accentGold = EblGold

  val displayNumber = remember(file.ccNumber, file.fileId) {
    val clean = file.ccNumber.trim()
    when {
      clean.length >= 4 -> clean
      else -> file.fileId
    }
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    border = BorderStroke(
      1.2.dp,
      Brush.linearGradient(listOf(accentGold.copy(alpha = 0.85f), Color.White.copy(alpha = 0.28f), accentGold.copy(alpha = 0.45f)))
    ),
    modifier = modifier
      .fillMaxWidth()
      .height(176.dp)
      .graphicsLayer {
        rotationY = flipRotation
        cameraDistance = 12f * density
      }
      .interactiveBounce(pressedScale = 0.97f, onClick = { showBackSide = !showBackSide })
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(cardGradient))
        .background(rememberShimmerBrush(Color.White.copy(alpha = 0.09f)))
    ) {
      // Decorative metallic concentric globe/arcs on card background
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          color = Color.White.copy(alpha = 0.05f),
          radius = size.height * 0.85f,
          center = Offset(size.width * 0.9f, size.height * 0.15f)
        )
        drawCircle(
          color = accentGold.copy(alpha = 0.06f),
          radius = size.height * 0.55f,
          center = Offset(size.width * 0.82f, size.height * 0.25f)
        )
      }

      if (!isBackVisible) {
        // FRONT FACE
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "B2B PLATINUM SUITE",
                color = accentGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
              )
              Text(
                text = file.productType.ifBlank { "Credit Card" }.uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
            CardNetworkLogoBadge(network = network, compact = false)
          }

          // Chip + Contactless + Status
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              EmvGoldChip(modifier = Modifier.size(width = 36.dp, height = 26.dp))
              Spacer(modifier = Modifier.width(8.dp))
              ContactlessNfcMark(tint = Color.White.copy(alpha = 0.82f))
            }
            ApplicationStatusBadge(file.applicationStatus)
          }

          // Embossed Number + Cardholder & Tap Hint
          Column {
            Text(
              text = displayNumber,
              color = Color.White,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "CARDHOLDER",
                  color = Color.White.copy(alpha = 0.58f),
                  fontSize = 8.sp,
                  letterSpacing = 1.sp
                )
                Text(
                  text = file.customerName.uppercase(),
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "RM ${file.assignedRmCode}",
                  color = accentGold,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Tap card to flip ↻",
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 8.5.sp
                )
              }
            }
          }
        }
      } else {
        // BACK FACE (mirrored rotationY = 180f so text renders left-to-right cleanly)
        Column(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationY = 180f },
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Spacer(modifier = Modifier.height(10.dp))
          // Realistic dark magnetic stripe
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(34.dp)
              .background(Color(0xFF030712))
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Signature / Security strip
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "LIMIT: ${file.limit.ifBlank { "Standard Review" }}",
                color = EblNavyDark,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Surface(shape = RoundedCornerShape(4.dp), color = EblNavyPrimary) {
                Text(
                  text = "CVV • ${file.activeStatus.uppercase()}",
                  color = accentGold,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "ANALYST: ${file.analyst.ifBlank { "Unassigned" }}",
                  color = Color.White,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${file.companyName} • CPV: ${file.cpvStatus}",
                  color = Color.White.copy(alpha = 0.78f),
                  fontSize = 9.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              CardNetworkLogoBadge(network = network, compact = true)
            }
          }
        }
      }
    }
  }
}

/**
 * Interactive 3D Banking Card Portfolio Deck for Dashboards showing live counts across Card Tiers.
 */
@Composable
fun CardProductPortfolioStrip(
  files: List<CustomerFileEntity>,
  onCardClick: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val creditCount = remember(files) {
    files.count { !it.isDeleted && it.productType.contains("Credit", ignoreCase = true) }
  }
  val corpCount = remember(files) {
    files.count { !it.isDeleted && it.productType.contains("Corp", ignoreCase = true) }
  }
  val b2bCount = remember(files) {
    files.count {
      !it.isDeleted &&
        !it.productType.contains("Credit", ignoreCase = true) &&
        !it.productType.contains("Corp", ignoreCase = true)
    }
  }
  val isDark = LocalAppTheme.current.isDark

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Card Product Portfolio",
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
      )
      CardNetworkLogoBadge(network = CardNetwork.VISA, compact = true)
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Portfolio3DMiniCard(
        tierTitle = "Credit Card",
        tierSubtitle = "Retail & Classic",
        count = creditCount,
        network = CardNetwork.VISA,
        gradient = listOf(Color(0xFF1D4ED8), Color(0xFF0A1F44)),
        accent = EblGold,
        onClick = { onCardClick?.invoke("Credit Card") }
      )
      Portfolio3DMiniCard(
        tierTitle = "Corporate Card",
        tierSubtitle = "Executive World",
        count = corpCount,
        network = CardNetwork.MASTERCARD,
        gradient = listOf(Color(0xFF334155), Color(0xFF0F172A)),
        accent = Color(0xFFFBBF24),
        onClick = { onCardClick?.invoke("Corporate Card") }
      )
      Portfolio3DMiniCard(
        tierTitle = "B2B & Payroll",
        tierSubtitle = "Limit / Split / Corp",
        count = b2bCount,
        network = CardNetwork.UNIONPAY,
        gradient = listOf(Color(0xFF0F766E), Color(0xFF064E3B)),
        accent = Color(0xFF6EE7B7),
        onClick = { onCardClick?.invoke("B2B") }
      )
    }
  }
}

@Composable
private fun Portfolio3DMiniCard(
  tierTitle: String,
  tierSubtitle: String,
  count: Int,
  network: CardNetwork,
  gradient: List<Color>,
  accent: Color,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    modifier = Modifier
      .width(164.dp)
      .height(98.dp)
      .interactiveBounce(pressedScale = 0.95f, onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(gradient))
        .background(rememberShimmerBrush(Color.White.copy(alpha = 0.07f)))
        .padding(11.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          EmvGoldChip(modifier = Modifier.size(width = 22.dp, height = 16.dp))
          CardNetworkLogoBadge(network = network, compact = true)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = tierTitle,
              color = Color.White,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = tierSubtitle,
              color = accent.copy(alpha = 0.9f),
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1
            )
          }
          AnimatedCountText(
            targetCount = count,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = JetBrainsMonoFontFamily
          )
        }
      }
    }
  }
}
