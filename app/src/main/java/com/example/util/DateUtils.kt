package com.example.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
  val DHAKA_ZONE: ZoneId = ZoneId.of("Asia/Dhaka")

  private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
  private val DISPLAY_FORMATTER_WITH_SEC = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a")
  private val DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy")
  private val TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a")
  private val ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  fun currentDhakaMillis(): Long {
    return Instant.now().toEpochMilli()
  }

  fun currentDhakaDate(): LocalDate {
    return LocalDate.now(DHAKA_ZONE)
  }

  fun formatDateTime(millis: Long?): String {
    if (millis == null || millis <= 0) return "N/A"
    return try {
      val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DHAKA_ZONE)
      ldt.format(DISPLAY_FORMATTER)
    } catch (_: Exception) {
      "Invalid Date"
    }
  }

  fun formatDateTimeWithSeconds(millis: Long?): String {
    if (millis == null || millis <= 0) return "N/A"
    return try {
      val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DHAKA_ZONE)
      ldt.format(DISPLAY_FORMATTER_WITH_SEC)
    } catch (_: Exception) {
      "Invalid Date"
    }
  }

  fun formatTimeOnly(millis: Long?): String {
    if (millis == null || millis <= 0) return "N/A"
    return try {
      val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DHAKA_ZONE)
      ldt.format(TIME_ONLY_FORMATTER)
    } catch (_: Exception) {
      "Invalid Date"
    }
  }

  fun formatDateOnly(millis: Long?): String {
    if (millis == null || millis <= 0) return "N/A"
    return try {
      val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DHAKA_ZONE)
      ldt.format(DATE_ONLY_FORMATTER)
    } catch (_: Exception) {
      "Invalid Date"
    }
  }

  fun formatIsoDate(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    return try {
      val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DHAKA_ZONE)
      ldt.format(ISO_DATE_FORMATTER)
    } catch (_: Exception) {
      ""
    }
  }

  /**
   * Defines week as Saturday through Friday (default banking week in Bangladesh).
   */
  fun getWeekRange(date: LocalDate, startDayOfWeek: DayOfWeek = DayOfWeek.SATURDAY): Pair<Long, Long> {
    // find the previous or same start day
    var start = date
    while (start.dayOfWeek != startDayOfWeek) {
      start = start.minusDays(1)
    }
    val end = start.plusDays(6)

    val startMillis = start.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
    val endMillis = end.atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
    return Pair(startMillis, endMillis)
  }

  fun getTodayRange(): Pair<Long, Long> {
    val today = currentDhakaDate()
    val startMillis = today.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
    val endMillis = today.atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
    return Pair(startMillis, endMillis)
  }

  fun getThisWeekRange(): Pair<Long, Long> {
    return getWeekRange(currentDhakaDate())
  }

  fun getLastWeekRange(): Pair<Long, Long> {
    val lastWeekDate = currentDhakaDate().minusWeeks(1)
    return getWeekRange(lastWeekDate)
  }

  fun getThisMonthRange(): Pair<Long, Long> {
    val today = currentDhakaDate()
    val start = today.with(TemporalAdjusters.firstDayOfMonth())
    val end = today.with(TemporalAdjusters.lastDayOfMonth())
    val startMillis = start.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
    val endMillis = end.atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
    return Pair(startMillis, endMillis)
  }

  fun getLastMonthRange(): Pair<Long, Long> {
    val prevMonth = currentDhakaDate().minusMonths(1)
    val start = prevMonth.with(TemporalAdjusters.firstDayOfMonth())
    val end = prevMonth.with(TemporalAdjusters.lastDayOfMonth())
    val startMillis = start.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
    val endMillis = end.atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
    return Pair(startMillis, endMillis)
  }

  enum class TimeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    ALL_TIME("All Time")
  }

  fun matchesTimeFilter(timestamp: Long, filter: TimeFilter): Boolean {
    return when (filter) {
      TimeFilter.ALL_TIME -> true
      TimeFilter.TODAY -> {
        val (start, end) = getTodayRange()
        timestamp in start..end
      }
      TimeFilter.THIS_WEEK -> {
        val (start, end) = getThisWeekRange()
        timestamp in start..end
      }
      TimeFilter.LAST_WEEK -> {
        val (start, end) = getLastWeekRange()
        timestamp in start..end
      }
      TimeFilter.THIS_MONTH -> {
        val (start, end) = getThisMonthRange()
        timestamp in start..end
      }
      TimeFilter.LAST_MONTH -> {
        val (start, end) = getLastMonthRange()
        timestamp in start..end
      }
    }
  }
}
