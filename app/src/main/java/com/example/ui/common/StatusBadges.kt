package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ApplicationStatusBadge(status: String, modifier: Modifier = Modifier) {
  val (bgColor, textColor) = when (status.lowercase()) {
    "approved" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
    "submitted" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
    "collected" -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
    "query" -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
    "return to source" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
    "condition" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
    "stc" -> Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
    "declined" -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
    else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = if (status.equals("return to source", ignoreCase = true)) "RTS (Return To Source)" else status,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun ActiveStatusBadge(status: String, modifier: Modifier = Modifier) {
  val (label, bgColor, textColor) = when (status.uppercase()) {
    "Y" -> Triple("Active (Y)", Color(0xFFDCFCE7), Color(0xFF166534))
    "N" -> Triple("Inactive (N)", Color(0xFFF1F5F9), Color(0xFF475569))
    "C" -> Triple("Closed (C)", Color(0xFFFEE2E2), Color(0xFF991B1B))
    else -> Triple(status, Color(0xFFF1F5F9), Color(0xFF475569))
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun CpvStatusBadge(status: String, modifier: Modifier = Modifier) {
  val (bgColor, textColor) = when (status.lowercase()) {
    "completed" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
    "pending" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
    "failed" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
    "not required" -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
    else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = "CPV: $status",
      color = textColor,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun AccountStatusBadge(status: String, modifier: Modifier = Modifier) {
  val (label, bgColor, textColor) = when (status.uppercase()) {
    "ACTIVE" -> Triple("Active", Color(0xFFDCFCE7), Color(0xFF15803D))
    "PENDING_APPROVAL" -> Triple("Pending Approval", Color(0xFFFEF3C7), Color(0xFFB45309))
    "INACTIVE" -> Triple("Inactive", Color(0xFFF1F5F9), Color(0xFF475569))
    "SUSPENDED" -> Triple("Suspended", Color(0xFFFEE2E2), Color(0xFFB91C1C))
    else -> Triple(status, Color(0xFFF1F5F9), Color(0xFF475569))
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false
    )
  }
}
