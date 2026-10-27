package com.example.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
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

  fun formatDate(millis: Long?): String {
    return formatDateOnly(millis)
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

  fun normalizeDigits(input: String): String {
    return input
      .replace('০', '0').replace('১', '1').replace('২', '2').replace('৩', '3').replace('৪', '4')
      .replace('৫', '5').replace('৬', '6').replace('৭', '7').replace('৮', '8').replace('৯', '9')
  }

  fun parseLocalDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    val clean = normalizeDigits(dateStr.trim())
    val datePatterns = listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy", "yyyy-MM-dd", "yyyy-M-d", "dd MMM yyyy", "d MMM yyyy")
    for (p in datePatterns) {
      try {
        val f = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(p).toFormatter(java.util.Locale.ENGLISH)
        return LocalDate.parse(clean, f)
      } catch (_: Exception) {}
    }
    return null
  }

  fun parseDeadlineMillis(closeTime: String?, targetDate: String = ""): Long? {
    if (closeTime.isNullOrBlank()) return null
    var clean = normalizeDigits(closeTime.trim())
    clean.toLongOrNull()?.let { return it }

    // Normalize am/pm: insert space between number and AM/PM if missing, e.g. "6:00pm" -> "6:00 PM"
    clean = clean.replace("(?i)(\\d+)(am|pm)".toRegex(), "$1 $2")
    clean = clean.replace("(?i)am".toRegex(), "AM").replace("(?i)pm".toRegex(), "PM")

    // 1. Try full date-time parse
    val fullParse = parseDateTime(clean)
    if (fullParse != null) {
      val hasTime = clean.contains(":") || clean.contains("AM") || clean.contains("PM")
      return if (!hasTime) fullParse + 86399000L else fullParse
    }

    // 2. Try time-only parse (e.g. "06:00 PM", "6:00 PM", "18:00", "6 PM")
    val timePatterns = listOf(
      "h:mm:ss a", "hh:mm:ss a", "h:mm a", "hh:mm a",
      "H:mm:ss", "HH:mm:ss", "H:mm", "HH:mm",
      "h a", "hh a"
    )
    for (tp in timePatterns) {
      try {
        val tf = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(tp).toFormatter(java.util.Locale.ENGLISH)
        val lt = LocalTime.parse(clean, tf)
        val baseDate = if (targetDate.isNotBlank()) parseLocalDate(targetDate) ?: LocalDate.now(DHAKA_ZONE)
                       else LocalDate.now(DHAKA_ZONE)
        return baseDate.atTime(lt).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
      } catch (_: Exception) {}
    }

    return null
  }

  fun parseDateTime(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    var trimmed = normalizeDigits(dateStr.trim())
    trimmed.toLongOrNull()?.let { return it }

    // Normalize am/pm
    trimmed = trimmed.replace("(?i)(\\d+)(am|pm)".toRegex(), "$1 $2")
    trimmed = trimmed.replace("(?i)am".toRegex(), "AM").replace("(?i)pm".toRegex(), "PM")

    // Strip JS timezone name in parentheses if present:
    if (trimmed.contains("(") && trimmed.contains(")")) {
      trimmed = trimmed.substringBefore("(").trim()
    }

    // Try Instant parse (e.g. ISO 8601 with Z or offset)
    try {
      return Instant.parse(trimmed).toEpochMilli()
    } catch (_: Exception) {}

    // Try standard JS Date format
    try {
      val jsFormatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", java.util.Locale.ENGLISH)
      return java.time.ZonedDateTime.parse(trimmed, jsFormatter).toInstant().toEpochMilli()
    } catch (_: Exception) {}

    try {
      val jsFormatterNoSec = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm 'GMT'Z", java.util.Locale.ENGLISH)
      return java.time.ZonedDateTime.parse(trimmed, jsFormatterNoSec).toInstant().toEpochMilli()
    } catch (_: Exception) {}

    val patterns = listOf(
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd HH:mm",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd'T'HH:mm:ss.SSS",
      "yyyy-MM-dd'T'HH:mm",
      "dd/MM/yyyy HH:mm:ss",
      "dd/MM/yyyy HH:mm",
      "dd/MM/yyyy hh:mm a",
      "dd/MM/yyyy h:mm a",
      "d/M/yyyy HH:mm:ss",
      "d/M/yyyy HH:mm",
      "d/M/yyyy h:mm a",
      "d/M/yyyy hh:mm a",
      "dd-MM-yyyy HH:mm:ss",
      "dd-MM-yyyy HH:mm",
      "dd-MM-yyyy hh:mm a",
      "dd-MM-yyyy h:mm a",
      "d-M-yyyy HH:mm",
      "d-M-yyyy h:mm a",
      "dd MMM yyyy, hh:mm a",
      "dd MMM yyyy, h:mm a",
      "dd MMM yyyy, hh:mm:ss a",
      "dd/MM/yyyy",
      "d/M/yyyy",
      "dd-MM-yyyy",
      "d-M-yyyy",
      "yyyy-MM-dd",
      "yyyy-M-d"
    )
    for (p in patterns) {
      try {
        val formatter = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(p).toFormatter(java.util.Locale.ENGLISH)
        if (p == "yyyy-MM-dd" || p == "yyyy-M-d" || p == "dd/MM/yyyy" || p == "d/M/yyyy" || p == "dd-MM-yyyy" || p == "d-M-yyyy") {
          val ld = LocalDate.parse(trimmed, formatter)
          return ld.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
        } else {
          val ldt = LocalDateTime.parse(trimmed, formatter)
          return ldt.atZone(DHAKA_ZONE).toInstant().toEpochMilli()
        }
      } catch (_: Exception) {}
    }
    return null
  }

  fun isEventExpired(closeTime: String, targetDate: String = ""): Boolean {
    val now = currentDhakaMillis()
    if (closeTime.isNotBlank()) {
      val parsedClose = parseDeadlineMillis(closeTime, targetDate)
      if (parsedClose != null && parsedClose > 0L) {
        return now >= parsedClose
      }
    }
    if (targetDate.isNotBlank()) {
      val parsedTarget = parseDateTime(targetDate)
      if (parsedTarget != null && parsedTarget > 0L) {
        val expiryTime = if (targetDate.trim().length <= 10) parsedTarget + 86399000L else parsedTarget
        return now >= expiryTime
      }
    }
    return false
  }

  enum class TimeOfDayPeriod(val label: String, val banglaLabel: String, val greeting: String, val emoji: String) {
    MORNING("Morning", "সকাল", "Good Morning (শুভ সকাল)", "🌅"),
    AFTERNOON("Afternoon", "দুপুর", "Good Afternoon (শুভ দুপুর)", "☀️"),
    EVENING("Evening", "সন্ধ্যা", "Good Evening (শুভ সন্ধ্যা)", "🌇"),
    NIGHT("Night", "রাত", "Good Night (শুভ রাত্রি)", "🌙")
  }

  fun getCurrentTimeOfDayPeriod(): TimeOfDayPeriod {
    val ldt = LocalDateTime.now(DHAKA_ZONE)
    val hour = ldt.hour
    return when (hour) {
      in 5..11 -> TimeOfDayPeriod.MORNING
      in 12..16 -> TimeOfDayPeriod.AFTERNOON
      in 17..19 -> TimeOfDayPeriod.EVENING
      else -> TimeOfDayPeriod.NIGHT
    }
  }

  fun getCurrentQuarter(): String {
    val month = LocalDate.now(DHAKA_ZONE).monthValue
    return when (month) {
      in 1..3 -> "Q1"
      in 4..6 -> "Q2"
      in 7..9 -> "Q3"
      else -> "Q4"
    }
  }

  fun getQuarterRange(quarter: String, year: Int = LocalDate.now(DHAKA_ZONE).year): Pair<Long, Long> {
    val cleanQ = quarter.trim().uppercase()
    val (startMonth, startDay, endMonth, endDay) = when (cleanQ) {
      "Q1" -> listOf(1, 1, 3, 31)
      "Q2" -> listOf(4, 1, 6, 30)
      "Q3" -> listOf(7, 1, 9, 30)
      else -> listOf(10, 1, 12, 31)
    }
    val start = LocalDate.of(year, startMonth, startDay).atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
    val end = LocalDate.of(year, endMonth, endDay).atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
    return Pair(start, end)
  }

  fun getQuarterDateLabel(quarter: String, year: Int = LocalDate.now(DHAKA_ZONE).year): String {
    return when (quarter.trim().uppercase()) {
      "Q1" -> "Q1 $year (01 Jan - 31 Mar $year)"
      "Q2" -> "Q2 $year (01 Apr - 30 Jun $year)"
      "Q3" -> "Q3 $year (01 Jul - 30 Sep $year)"
      else -> "Q4 $year (01 Oct - 31 Dec $year)"
    }
  }

  fun parseStartOfDayMillis(dateStr: String): Long? {
    val trimmed = dateStr.trim()
    val parsed = parseDateTime(trimmed) ?: return null
    val ld = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed), DHAKA_ZONE).toLocalDate()
    return ld.atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
  }

  fun parseEndOfDayMillis(dateStr: String): Long? {
    val trimmed = dateStr.trim()
    val parsed = parseDateTime(trimmed) ?: return null
    val ld = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed), DHAKA_ZONE).toLocalDate()
    return ld.atTime(23, 59, 59, 999_000_000).atZone(DHAKA_ZONE).toInstant().toEpochMilli()
  }
}
