package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import kotlin.math.roundToInt

@Composable
fun FloatableRmNavButton(
  onNavigateNewFile: () -> Unit,
  onNavigateMyFiles: () -> Unit,
  onNavigatePendingDocs: () -> Unit,
  onNavigateDashboard: () -> Unit,
  onNavigateDbrChecklist: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }
  var isExpanded by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        .padding(bottom = 72.dp, end = 16.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            offsetX += dragAmount.x
            offsetY += dragAmount.y
          }
        }
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Expandable Quick Navigation Options
        AnimatedVisibility(
          visible = isExpanded,
          enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
          exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Option 1: New File Entry
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = EblNavyDark,
                shadowElevation = 4.dp
              ) {
                Text(
                  text = "New File",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              SmallFloatingActionButton(
                onClick = {
                  isExpanded = false
                  onNavigateNewFile()
                },
                containerColor = EblGold,
                contentColor = EblNavyDark,
                shape = CircleShape,
                modifier = Modifier.testTag("float_btn_new_file")
              ) {
                Icon(Icons.Default.Add, contentDescription = "New File")
              }
            }

            // Option 2: Pending Docs Backlog
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFDC2626),
                shadowElevation = 4.dp
              ) {
                Text(
                  text = "Pending Docs Backlog",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              SmallFloatingActionButton(
                onClick = {
                  isExpanded = false
                  onNavigatePendingDocs()
                },
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("float_btn_pending_docs")
              ) {
                Icon(Icons.Default.Warning, contentDescription = "Pending Docs")
              }
            }

            // Option 3: My Files
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = EblNavyDark,
                shadowElevation = 4.dp
              ) {
                Text(
                  text = "My Files",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              SmallFloatingActionButton(
                onClick = {
                  isExpanded = false
                  onNavigateMyFiles()
                },
                containerColor = EblNavyPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("float_btn_my_files")
              ) {
                Icon(Icons.Default.Folder, contentDescription = "My Files")
              }
            }

            // Option 4: RM Dashboard
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = EblNavyDark,
                shadowElevation = 4.dp
              ) {
                Text(
                  text = "Dashboard",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              SmallFloatingActionButton(
                onClick = {
                  isExpanded = false
                  onNavigateDashboard()
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("float_btn_dashboard")
              ) {
                Icon(Icons.Default.Assessment, contentDescription = "Dashboard")
              }
            }

            // Option 5: DBR & Checklist Tool
            if (onNavigateDbrChecklist != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = EblNavyDark,
                  shadowElevation = 4.dp
                ) {
                  Text(
                    text = "DBR Tool",
                    color = EblGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                SmallFloatingActionButton(
                  onClick = {
                    isExpanded = false
                    onNavigateDbrChecklist()
                  },
                  containerColor = EblGold,
                  contentColor = EblNavyDark,
                  shape = CircleShape,
                  modifier = Modifier.testTag("float_btn_dbr_checklist")
                ) {
                  Icon(Icons.Default.Calculate, contentDescription = "DBR Tool")
                }
              }
            }
          }
        }

        // Main Floatable & Draggable Hub Button
        FloatingActionButton(
          onClick = { isExpanded = !isExpanded },
          containerColor = if (isExpanded) Color(0xFFDC2626) else EblNavyPrimary,
          contentColor = Color.White,
          shape = CircleShape,
          elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
          modifier = Modifier
            .size(54.dp)
            .border(2.dp, EblGold, CircleShape)
            .testTag("draggable_rm_floating_nav_btn")
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Menu,
            contentDescription = "Floatable Nav Menu (Drag to move)",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}
