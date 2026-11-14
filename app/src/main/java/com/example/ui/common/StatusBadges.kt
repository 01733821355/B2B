package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApplicationStatusBadge(status: String, modifier: Modifier = Modifier) {
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark
  val (bgColor, textColor) = if (isDark) {
    when (status.lowercase()) {
      "approved" -> Pair(Color(0xFF14532D).copy(alpha = 0.65f), Color(0xFF86EFAC))
      "submitted" -> Pair(Color(0xFF1E3A8A).copy(alpha = 0.65f), Color(0xFF93C5FD))
      "analyst receive", "analyst received" -> Pair(Color(0xFF312E81).copy(alpha = 0.65f), Color(0xFFA5B4FC))
      "collected" -> Pair(Color(0xFF0C4A6E).copy(alpha = 0.65f), Color(0xFF7DD3FC))
      "query" -> Pair(Color(0xFF7C2D12).copy(alpha = 0.65f), Color(0xFFFDBA74))
      "return to source" -> Pair(Color(0xFF7F1D1D).copy(alpha = 0.65f), Color(0xFFFCA5A5))
      "condition" -> Pair(Color(0xFF581C87).copy(alpha = 0.65f), Color(0xFFD8B4FE))
      "stc" -> Pair(Color(0xFF134E4A).copy(alpha = 0.72f), Color(0xFF5EEAD4))
      "declined" -> Pair(Color(0xFF7F1D1D).copy(alpha = 0.72f), Color(0xFFFCA5A5))
      else -> Pair(Color(0xFF1E293B), Color(0xFFCBD5E1))
    }
  } else {
    when (status.lowercase()) {
      "approved" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
      "submitted" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
      "analyst receive", "analyst received" -> Pair(Color(0xFFE0E7FF), Color(0xFF4338CA))
      "collected" -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
      "query" -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
      "return to source" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
      "condition" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
      "stc" -> Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
      "declined" -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
      else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(0.75.dp, textColor.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .background(textColor, CircleShape)
      )
      Text(
        text = if (status.equals("return to source", ignoreCase = true)) "RTS (Return To Source)" else status,
        color = textColor,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
fun ActiveStatusBadge(status: String, modifier: Modifier = Modifier) {
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark
  val (label, bgColor, textColor) = if (isDark) {
    when (status.uppercase()) {
      "Y" -> Triple("Active (Y)", Color(0xFF14532D).copy(alpha = 0.6f), Color(0xFF86EFAC))
      "N" -> Triple("Inactive (N)", Color(0xFF1E293B), Color(0xFFCBD5E1))
      "C" -> Triple("Closed (C)", Color(0xFF7F1D1D).copy(alpha = 0.65f), Color(0xFFFCA5A5))
      else -> Triple(status, Color(0xFF1E293B), Color(0xFFCBD5E1))
    }
  } else {
    when (status.uppercase()) {
      "Y" -> Triple("Active (Y)", Color(0xFFDCFCE7), Color(0xFF166534))
      "N" -> Triple("Inactive (N)", Color(0xFFF1F5F9), Color(0xFF475569))
      "C" -> Triple("Closed (C)", Color(0xFFFEE2E2), Color(0xFF991B1B))
      else -> Triple(status, Color(0xFFF1F5F9), Color(0xFF475569))
    }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(0.75.dp, textColor.copy(alpha = 0.3f))
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
    )
  }
}

@Composable
fun CpvStatusBadge(status: String, modifier: Modifier = Modifier) {
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark
  val (bgColor, textColor) = if (isDark) {
    when (status.lowercase()) {
      "completed" -> Pair(Color(0xFF14532D).copy(alpha = 0.6f), Color(0xFF86EFAC))
      "pending" -> Pair(Color(0xFF78350F).copy(alpha = 0.6f), Color(0xFFFCD34D))
      "failed" -> Pair(Color(0xFF7F1D1D).copy(alpha = 0.65f), Color(0xFFFCA5A5))
      "not required" -> Pair(Color(0xFF1E293B), Color(0xFF94A3B8))
      else -> Pair(Color(0xFF1E293B), Color(0xFFCBD5E1))
    }
  } else {
    when (status.lowercase()) {
      "completed" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
      "pending" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
      "failed" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
      "not required" -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
      else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(0.75.dp, textColor.copy(alpha = 0.3f))
  ) {
    Text(
      text = "CPV: $status",
      color = textColor,
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
    )
  }
}

@Composable
fun AccountStatusBadge(status: String, modifier: Modifier = Modifier) {
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark
  val (label, bgColor, textColor) = if (isDark) {
    when (status.uppercase()) {
      "ACTIVE" -> Triple("Active", Color(0xFF14532D).copy(alpha = 0.65f), Color(0xFF86EFAC))
      "PENDING_APPROVAL" -> Triple("Pending Approval", Color(0xFF78350F).copy(alpha = 0.65f), Color(0xFFFCD34D))
      "INACTIVE" -> Triple("Inactive", Color(0xFF1E293B), Color(0xFFCBD5E1))
      "SUSPENDED" -> Triple("Suspended", Color(0xFF7F1D1D).copy(alpha = 0.65f), Color(0xFFFCA5A5))
      else -> Triple(status, Color(0xFF1E293B), Color(0xFFCBD5E1))
    }
  } else {
    when (status.uppercase()) {
      "ACTIVE" -> Triple("Active", Color(0xFFDCFCE7), Color(0xFF15803D))
      "PENDING_APPROVAL" -> Triple("Pending Approval", Color(0xFFFEF3C7), Color(0xFFB45309))
      "INACTIVE" -> Triple("Inactive", Color(0xFFF1F5F9), Color(0xFF475569))
      "SUSPENDED" -> Triple("Suspended", Color(0xFFFEE2E2), Color(0xFFB91C1C))
      else -> Triple(status, Color(0xFFF1F5F9), Color(0xFF475569))
    }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(0.75.dp, textColor.copy(alpha = 0.3f))
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}
