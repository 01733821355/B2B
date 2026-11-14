package com.example.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblGold
import com.example.ui.theme.LocalAppTheme
import com.example.ui.viewmodel.Screen

enum class Nav3DIconType {
  DASHBOARD,
  NEW_FILE,
  MY_FILES,
  DATABASE,
  REPORTS,
  SHEETS,
  MORE
}

data class Nav3DItemSpec(
  val label: String,
  val iconType: Nav3DIconType,
  val selected: Boolean,
  val testTag: String,
  val onClick: () -> Unit
)

/**
 * Unclipped floating 3D Realistic Bottom Navigation Dock.
 * Places WindowInsets.navigationBars cleanly BELOW the 74.dp interactive row so icons and labels
 * are NEVER squished or cut in half on gesture-nav or 3-button Android devices.
 */
@Composable
fun Realistic3DBottomBar(
  userRole: String,
  currentScreen: Screen,
  onSelectScreen: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentTheme = LocalAppTheme.current
  val isDark = currentTheme.isDark

  val items = remember(userRole, currentScreen) {
    when (userRole) {
      "ADMIN" -> listOf(
        Nav3DItemSpec(
          label = "Dashboard",
          iconType = Nav3DIconType.DASHBOARD,
          selected = currentScreen is Screen.AdminDashboard,
          testTag = "nav_tab_dashboard",
          onClick = { onSelectScreen(Screen.AdminDashboard) }
        ),
        Nav3DItemSpec(
          label = "New File",
          iconType = Nav3DIconType.NEW_FILE,
          selected = currentScreen is Screen.CustomerForm && currentScreen.editFileId == null,
          testTag = "nav_tab_new_file",
          onClick = { onSelectScreen(Screen.CustomerForm(null)) }
        ),
        Nav3DItemSpec(
          label = "Database",
          iconType = Nav3DIconType.DATABASE,
          selected = currentScreen is Screen.GlobalDatabase,
          testTag = "nav_tab_database",
          onClick = { onSelectScreen(Screen.GlobalDatabase) }
        ),
        Nav3DItemSpec(
          label = "Reports",
          iconType = Nav3DIconType.REPORTS,
          selected = currentScreen is Screen.Reports,
          testTag = "nav_tab_reports",
          onClick = { onSelectScreen(Screen.Reports) }
        ),
        Nav3DItemSpec(
          label = "More",
          iconType = Nav3DIconType.MORE,
          selected = currentScreen is Screen.More,
          testTag = "nav_tab_more",
          onClick = { onSelectScreen(Screen.More) }
        )
      )
      "MENTOR" -> listOf(
        Nav3DItemSpec(
          label = "Console",
          iconType = Nav3DIconType.DASHBOARD,
          selected = currentScreen is Screen.MentorDashboard,
          testTag = "nav_tab_console",
          onClick = { onSelectScreen(Screen.MentorDashboard) }
        ),
        Nav3DItemSpec(
          label = "New File",
          iconType = Nav3DIconType.NEW_FILE,
          selected = currentScreen is Screen.CustomerForm && currentScreen.editFileId == null,
          testTag = "nav_tab_new_file",
          onClick = { onSelectScreen(Screen.CustomerForm(null)) }
        ),
        Nav3DItemSpec(
          label = "Database",
          iconType = Nav3DIconType.DATABASE,
          selected = currentScreen is Screen.GlobalDatabase,
          testTag = "nav_tab_database",
          onClick = { onSelectScreen(Screen.GlobalDatabase) }
        ),
        Nav3DItemSpec(
          label = "Sheets",
          iconType = Nav3DIconType.SHEETS,
          selected = currentScreen is Screen.GoogleSheetsSync,
          testTag = "nav_tab_sheets",
          onClick = { onSelectScreen(Screen.GoogleSheetsSync) }
        ),
        Nav3DItemSpec(
          label = "More",
          iconType = Nav3DIconType.MORE,
          selected = currentScreen is Screen.More,
          testTag = "nav_tab_more",
          onClick = { onSelectScreen(Screen.More) }
        )
      )
      else -> listOf(
        Nav3DItemSpec(
          label = "Dashboard",
          iconType = Nav3DIconType.DASHBOARD,
          selected = currentScreen is Screen.RmDashboard,
          testTag = "nav_tab_dashboard",
          onClick = { onSelectScreen(Screen.RmDashboard) }
        ),
        Nav3DItemSpec(
          label = "New File",
          iconType = Nav3DIconType.NEW_FILE,
          selected = currentScreen is Screen.CustomerForm && currentScreen.editFileId == null,
          testTag = "nav_tab_new_file",
          onClick = { onSelectScreen(Screen.CustomerForm(null)) }
        ),
        Nav3DItemSpec(
          label = "My Files",
          iconType = Nav3DIconType.MY_FILES,
          selected = currentScreen is Screen.CustomerList,
          testTag = "nav_tab_my_files",
          onClick = { onSelectScreen(Screen.CustomerList) }
        ),
        Nav3DItemSpec(
          label = "Reports",
          iconType = Nav3DIconType.REPORTS,
          selected = currentScreen is Screen.Reports,
          testTag = "nav_tab_reports",
          onClick = { onSelectScreen(Screen.Reports) }
        ),
        Nav3DItemSpec(
          label = "More",
          iconType = Nav3DIconType.MORE,
          selected = currentScreen is Screen.More,
          testTag = "nav_tab_more",
          onClick = { onSelectScreen(Screen.More) }
        )
      )
    }
  }

  Surface(
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
    shadowElevation = 18.dp,
    tonalElevation = 6.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Metallic top 3D rim accent
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(
            Brush.horizontalGradient(
              listOf(
                Color.Transparent,
                currentTheme.tertiary.copy(alpha = 0.7f),
                currentTheme.primary.copy(alpha = 0.6f),
                currentTheme.tertiary.copy(alpha = 0.7f),
                Color.Transparent
              )
            )
          )
      )

      // Full-height non-clipped interactive navigation row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(74.dp)
          .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        items.forEach { item ->
          Realistic3DNavItemButton(
            spec = item,
            accentColor = currentTheme.primary,
            goldAccent = currentTheme.tertiary,
            isDark = isDark,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Dedicated bottom system inset spacer BELOW the icon row so icons are never cut off
      Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
  }
}

@Composable
private fun Realistic3DNavItemButton(
  spec: Nav3DItemSpec,
  accentColor: Color,
  goldAccent: Color,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val iconScale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.88f
      spec.selected -> 1.14f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "nav3d_scale"
  )

  val liftOffset by animateDpAsState(
    targetValue = if (spec.selected) (-3).dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "nav3d_lift"
  )

  val labelColor by animateColorAsState(
    targetValue = if (spec.selected) {
      if (isDark) goldAccent else accentColor
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant
    },
    animationSpec = tween(durationMillis = 200),
    label = "nav3d_label"
  )

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(color = accentColor),
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          spec.onClick()
        }
      )
      .padding(vertical = 4.dp)
      .testTag(spec.testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .offset(y = liftOffset)
        .graphicsLayer {
          scaleX = iconScale
          scaleY = iconScale
        },
      contentAlignment = Alignment.Center
    ) {
      if (spec.selected) {
        Box(
          modifier = Modifier
            .size(width = 48.dp, height = 36.dp)
            .background(
              Brush.radialGradient(
                listOf(
                  accentColor.copy(alpha = if (isDark) 0.38f else 0.18f),
                  Color.Transparent
                )
              ),
              RoundedCornerShape(14.dp)
            )
        )
      }
      Realistic3DNavIcon(
        type = spec.iconType,
        selected = spec.selected,
        accentColor = accentColor,
        modifier = Modifier.size(34.dp)
      )
    }

    Spacer(modifier = Modifier.height(3.dp))

    Text(
      text = spec.label,
      fontSize = if (spec.selected) 10.5.sp else 9.5.sp,
      fontWeight = if (spec.selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = labelColor,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Custom multi-layered 3D beveled ceramic & metallic icon rendered via Compose Canvas.
 * Every icon includes a 3D bottom extrusion shadow, rich linear gradient body, internal 3D subjects,
 * and a glossy top-left specular glass reflection.
 */
@Composable
fun Realistic3DNavIcon(
  type: Nav3DIconType,
  selected: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier.size(34.dp)
) {
  val (topGrad, bottomGrad, rimColor) = when (type) {
    Nav3DIconType.DASHBOARD -> Triple(
      if (selected) Color(0xFF2563EB) else Color(0xFF334155),
      if (selected) Color(0xFF0A1F44) else Color(0xFF1E293B),
      Color(0xFF061126)
    )
    Nav3DIconType.NEW_FILE -> Triple(
      if (selected) Color(0xFF10B981) else Color(0xFF0F766E),
      if (selected) Color(0xFF047857) else Color(0xFF115E59),
      Color(0xFF022C22)
    )
    Nav3DIconType.MY_FILES -> Triple(
      if (selected) Color(0xFFF59E0B) else Color(0xFFD97706),
      if (selected) Color(0xFFB45309) else Color(0xFF92400E),
      Color(0xFF451A03)
    )
    Nav3DIconType.DATABASE -> Triple(
      if (selected) Color(0xFF6366F1) else Color(0xFF4338CA),
      if (selected) Color(0xFF3730A3) else Color(0xFF312E81),
      Color(0xFF1E1B4B)
    )
    Nav3DIconType.REPORTS -> Triple(
      if (selected) Color(0xFF0284C7) else Color(0xFF0369A1),
      if (selected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
      Color(0xFF0C1938)
    )
    Nav3DIconType.SHEETS -> Triple(
      if (selected) Color(0xFF16A34A) else Color(0xFF15803D),
      if (selected) Color(0xFF14532D) else Color(0xFF064E3B),
      Color(0xFF052E16)
    )
    Nav3DIconType.MORE -> Triple(
      if (selected) Color(0xFF7C3AED) else Color(0xFF475569),
      if (selected) Color(0xFF4C1D95) else Color(0xFF1E293B),
      Color(0xFF1E1038)
    )
  }

  val iconAlpha = if (selected) 1f else 0.86f

  Box(
    modifier = modifier
      .shadow(
        elevation = if (selected) 7.dp else 3.dp,
        shape = RoundedCornerShape(11.dp),
        ambientColor = accentColor,
        spotColor = accentColor
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
      val bevelDepth = 2.6.dp.toPx()

      // 1. 3D Extruded Bottom Rim
      drawRoundRect(
        color = rimColor.copy(alpha = iconAlpha),
        topLeft = Offset(0f, bevelDepth),
        size = Size(w, h - bevelDepth),
        cornerRadius = corner
      )

      // 2. Main 3D Metallic/Ceramic Body
      drawRoundRect(
        brush = Brush.verticalGradient(listOf(topGrad, bottomGrad)),
        topLeft = Offset.Zero,
        size = Size(w, h - bevelDepth),
        cornerRadius = corner,
        alpha = iconAlpha
      )

      // 3. Top Gloss Specular Reflection Arc
      drawRoundRect(
        brush = Brush.verticalGradient(
          listOf(Color.White.copy(alpha = 0.42f), Color.Transparent)
        ),
        topLeft = Offset(w * 0.08f, h * 0.05f),
        size = Size(w * 0.84f, (h - bevelDepth) * 0.44f),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
      )

      // 4. Inner 3D Subject Artwork
      val bodyH = h - bevelDepth
      when (type) {
        Nav3DIconType.DASHBOARD -> {
          // 3D Bento Grid Tiles inside medallion
          drawRoundRect(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = Offset(w * 0.22f, bodyH * 0.22f),
            size = Size(w * 0.24f, bodyH * 0.28f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
          )
          drawRoundRect(
            color = EblGold,
            topLeft = Offset(w * 0.54f, bodyH * 0.22f),
            size = Size(w * 0.24f, bodyH * 0.20f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
          )
          drawRoundRect(
            color = Color(0xFF93C5FD),
            topLeft = Offset(w * 0.22f, bodyH * 0.57f),
            size = Size(w * 0.24f, bodyH * 0.21f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
          )
          drawRoundRect(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = Offset(w * 0.54f, bodyH * 0.49f),
            size = Size(w * 0.24f, bodyH * 0.29f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
          )
        }

        Nav3DIconType.NEW_FILE -> {
          // 3D Card + Glowing Raised "+" Jewel
          drawRoundRect(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = Offset(w * 0.20f, bodyH * 0.26f),
            size = Size(w * 0.60f, bodyH * 0.48f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          // Mini gold chip on card
          drawRoundRect(
            color = EblGold,
            topLeft = Offset(w * 0.26f, bodyH * 0.36f),
            size = Size(w * 0.14f, bodyH * 0.12f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
          )
          // Bold teal "+"
          val stroke = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round)
          val cx = w * 0.61f
          val cy = bodyH * 0.54f
          val arm = w * 0.11f
          drawLine(Color(0xFF047857), Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = stroke.width, cap = StrokeCap.Round)
          drawLine(Color(0xFF047857), Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }

        Nav3DIconType.MY_FILES -> {
          // 3D Folder with white document pages sticking out
          drawRoundRect(
            color = Color.White.copy(alpha = 0.96f),
            topLeft = Offset(w * 0.23f, bodyH * 0.22f),
            size = Size(w * 0.54f, bodyH * 0.30f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
          )
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            topLeft = Offset(w * 0.18f, bodyH * 0.34f),
            size = Size(w * 0.64f, bodyH * 0.44f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          // Folder clasp line
          drawLine(
            color = Color(0xFFB45309),
            start = Offset(w * 0.35f, bodyH * 0.56f),
            end = Offset(w * 0.65f, bodyH * 0.56f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
          )
        }

        Nav3DIconType.DATABASE -> {
          // 3D Stacked Glowing Database Platters
          val pW = w * 0.58f
          val pH = bodyH * 0.17f
          val left = w * 0.21f
          listOf(0.22f, 0.42f, 0.62f).forEachIndexed { idx, yRatio ->
            drawRoundRect(
              color = if (idx == 0) Color.White else Color(0xFFE0E7FF),
              topLeft = Offset(left, bodyH * yRatio),
              size = Size(pW, pH),
              cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            // Status LED dot on each platter
            drawCircle(
              color = Color(0xFF34D399),
              radius = 1.6.dp.toPx(),
              center = Offset(left + pW * 0.18f, bodyH * yRatio + pH / 2f)
            )
          }
        }

        Nav3DIconType.REPORTS -> {
          // 3D Glossy Document Sheet + Colorful Rising Analytics Bars
          drawRoundRect(
            color = Color.White.copy(alpha = 0.96f),
            topLeft = Offset(w * 0.21f, bodyH * 0.18f),
            size = Size(w * 0.58f, bodyH * 0.64f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          val baseY = bodyH * 0.72f
          drawRoundRect(
            color = Color(0xFF2563EB),
            topLeft = Offset(w * 0.30f, bodyH * 0.52f),
            size = Size(w * 0.09f, baseY - bodyH * 0.52f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
          )
          drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(w * 0.455f, bodyH * 0.40f),
            size = Size(w * 0.09f, baseY - bodyH * 0.40f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
          )
          drawRoundRect(
            color = EblGold,
            topLeft = Offset(w * 0.61f, bodyH * 0.30f),
            size = Size(w * 0.09f, baseY - bodyH * 0.30f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
          )
        }

        Nav3DIconType.SHEETS -> {
          // 3D Spreadsheet Tablet Grid
          drawRoundRect(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = Offset(w * 0.21f, bodyH * 0.21f),
            size = Size(w * 0.58f, bodyH * 0.58f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          val gridC = Color(0xFF15803D)
          drawLine(gridC, Offset(w * 0.21f, bodyH * 0.41f), Offset(w * 0.79f, bodyH * 0.41f), 1.4.dp.toPx())
          drawLine(gridC, Offset(w * 0.21f, bodyH * 0.60f), Offset(w * 0.79f, bodyH * 0.60f), 1.4.dp.toPx())
          drawLine(gridC, Offset(w * 0.48f, bodyH * 0.21f), Offset(w * 0.48f, bodyH * 0.79f), 1.4.dp.toPx())
        }

        Nav3DIconType.MORE -> {
          // 4 Raised 3D Gem Spheres / Cubes
          val r = w * 0.135f
          drawCircle(Color.White.copy(alpha = 0.96f), r, Offset(w * 0.34f, bodyH * 0.34f))
          drawCircle(EblGold, r, Offset(w * 0.66f, bodyH * 0.34f))
          drawCircle(Color(0xFF34D399), r, Offset(w * 0.34f, bodyH * 0.66f))
          drawCircle(Color(0xFF93C5FD), r, Offset(w * 0.66f, bodyH * 0.66f))
        }
      }

      // 5. Crisp Glass Inner Rim Highlight
      drawRoundRect(
        color = Color.White.copy(alpha = if (selected) 0.45f else 0.22f),
        topLeft = Offset.Zero,
        size = Size(w, bodyH),
        cornerRadius = corner,
        style = Stroke(width = 0.9.dp.toPx())
      )
    }
  }
}
