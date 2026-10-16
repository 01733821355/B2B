package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AgeCalculatorHelper {

  data class ParsedDateResult(
    val day: Int,
    val month: Int, // 1 - 12
    val year: Int,
    val formattedSlash: String, // e.g. "25/10/1997"
    val formattedPretty: String, // e.g. "25 October 1997 (Saturday)"
    val rawInterpreted: String
  )

  data class AgeCalculation(
    val years: Int,
    val months: Int,
    val days: Int,
    val nextBirthdayMonths: Int,
    val nextBirthdayDays: Int,
    val nextBirthdayDayOfWeek: String,
    val totalDays: Long,
    val totalWeeks: Long,
    val totalMonths: Long,
    val formattedBirthDate: String,
    val formattedAsOnDate: String,
    val creditCardEligibility: String,
    val loanEligibility: String,
    val isEligibleCreditCard: Boolean,
    val isEligibleLoan: Boolean,
    val fullSummaryText: String
  )

  /**
   * Convert Bengali digits '০'..'৯' to standard ASCII '0'..'9'.
   */
  fun normalizeBengaliDigits(input: String): String {
    val sb = StringBuilder()
    for (ch in input) {
      if (ch in '০'..'৯') {
        sb.append((ch - '০' + '0'.code).toChar())
      } else {
        sb.append(ch)
      }
    }
    return sb.toString()
  }

  /**
   * Parses virtually ANY date format into a validated day, month (1..12), and 4-digit year.
   * Examples supported:
   * - "25 10 1997" -> 25/10/1997
   * - "25 10 97" -> 25/10/1997 (2-digit year conversion: 97 -> 1997, 05 -> 2005)
   * - "25/10/1997", "25-10-1997", "25.10.1997", "25, 10, 1997"
   * - "25/10/97", "25-10-97", "25.10.97"
   * - "25101997" (8 digits) -> 25/10/1997
   * - "251097" (6 digits) -> 25/10/1997
   * - "1997-10-25", "1997 10 25", "1997/10/25"
   * - "25 Oct 1997", "25 October 97"
   * - "২৫ ১০ ১৯৯৭", "২৫ ১০ ৯৭"
   */
  fun parseAnyDate(raw: String): ParsedDateResult? {
    if (raw.isBlank()) return null

    var cleaned = normalizeBengaliDigits(raw.trim()).lowercase(Locale.ENGLISH)

    // Replace textual months with numeric markers (1..12)
    val monthsMap = listOf(
      "september" to " 9 ", "sept" to " 9 ", "sep" to " 9 ",
      "january" to " 1 ", "jan" to " 1 ",
      "february" to " 2 ", "feb" to " 2 ",
      "march" to " 3 ", "mar" to " 3 ",
      "april" to " 4 ", "apr" to " 4 ",
      "may" to " 5 ",
      "june" to " 6 ", "jun" to " 6 ",
      "july" to " 7 ", "jul" to " 7 ",
      "august" to " 8 ", "aug" to " 8 ",
      "october" to " 10 ", "oct" to " 10 ",
      "november" to " 11 ", "nov" to " 11 ",
      "december" to " 12 ", "dec" to " 12 "
    )

    for ((monthName, numStr) in monthsMap) {
      if (cleaned.contains(monthName)) {
        cleaned = cleaned.replace(monthName, numStr)
        break
      }
    }

    // Extract all numeric segments
    val nonDigitsReplaced = cleaned.replace(Regex("[^0-9]+"), " ").trim()
    if (nonDigitsReplaced.isBlank()) return null

    val tokens = nonDigitsReplaced.split(Regex("\\s+")).filter { it.isNotBlank() }

    var day = 0
    var month = 0
    var year = 0

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentTwoDigitYear = currentYear % 100 // e.g. 26 for 2026

    fun resolveTwoDigitYear(yy: Int): Int {
      return if (yy <= currentTwoDigitYear) {
        2000 + yy
      } else {
        1900 + yy
      }
    }

    if (tokens.size == 1) {
      val single = tokens[0]
      when (single.length) {
        8 -> {
          // Check if ISO YYYYMMDD
          val prefix4 = single.substring(0, 4).toIntOrNull() ?: 0
          if (prefix4 in 1900..currentYear) {
            year = prefix4
            month = single.substring(4, 6).toIntOrNull() ?: 0
            day = single.substring(6, 8).toIntOrNull() ?: 0
          } else {
            // DDMMYYYY
            day = single.substring(0, 2).toIntOrNull() ?: 0
            month = single.substring(2, 4).toIntOrNull() ?: 0
            year = single.substring(4, 8).toIntOrNull() ?: 0
          }
        }
        6 -> {
          // DDMMYY (e.g. 251097)
          day = single.substring(0, 2).toIntOrNull() ?: 0
          month = single.substring(2, 4).toIntOrNull() ?: 0
          val yy = single.substring(4, 6).toIntOrNull() ?: 0
          year = resolveTwoDigitYear(yy)
        }
        4 -> {
          // Just a year e.g. 1997
          val yCandidate = single.toIntOrNull() ?: 0
          if (yCandidate in 1900..currentYear) {
            year = yCandidate
            month = 1
            day = 1
          } else {
            return null
          }
        }
        else -> return null
      }
    } else if (tokens.size == 2) {
      val t1 = tokens[0].toIntOrNull() ?: return null
      val t2 = tokens[1].toIntOrNull() ?: return null
      if (t2 > 1000) {
        // MM YYYY or DD YYYY
        month = t1.coerceIn(1, 12)
        year = t2
        day = 1
      } else {
        return null
      }
    } else if (tokens.size >= 3) {
      val n1 = tokens[0].toIntOrNull() ?: return null
      val n2 = tokens[1].toIntOrNull() ?: return null
      val n3 = tokens[2].toIntOrNull() ?: return null

      if (n1 > 1000) {
        // YYYY MM DD (e.g. 1997 10 25)
        year = n1
        month = n2
        day = n3
      } else {
        // n3 is the Year part (e.g. 1997 or 97)
        if (n3 > 1000) {
          year = n3
        } else {
          // 2-digit year (e.g. 97 -> 1997, 05 -> 2005)
          year = resolveTwoDigitYear(n3)
        }

        // Determine which of n1 and n2 is Day and Month
        if (n1 > 12 && n2 <= 12) {
          // n1 must be Day (e.g. 25 10 1997)
          day = n1
          month = n2
        } else if (n2 > 12 && n1 <= 12) {
          // n2 must be Day (e.g. 10 25 1997 - US format)
          month = n1
          day = n2
        } else {
          // Default standard in Bangladesh / UK: DD / MM / YYYY
          day = n1
          month = n2
        }
      }
    } else {
      return null
    }

    // Validation
    if (year !in 1900..currentYear) return null
    if (month !in 1..12) return null

    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    if (day !in 1..maxDay) return null

    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val slashFormat = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
    val sdfPretty = SimpleDateFormat("dd MMMM yyyy (EEEE)", Locale.US)
    val prettyFormat = sdfPretty.format(cal.time)

    return ParsedDateResult(
      day = day,
      month = month,
      year = year,
      formattedSlash = slashFormat,
      formattedPretty = prettyFormat,
      rawInterpreted = "Day: $day, Month: $month, Year: $year"
    )
  }

  /**
   * Calculates exact age in Years, Months, Days as of a target date (default = Today).
   */
  fun calculateAge(birth: ParsedDateResult, asOn: Calendar = Calendar.getInstance()): AgeCalculation {
    val birthCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, birth.year)
      set(Calendar.MONTH, birth.month - 1)
      set(Calendar.DAY_OF_MONTH, birth.day)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val asOnCal = Calendar.getInstance().apply {
      timeInMillis = asOn.timeInMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    var years = asOnCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
    var months = asOnCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)
    var days = asOnCal.get(Calendar.DAY_OF_MONTH) - birthCal.get(Calendar.DAY_OF_MONTH)

    if (days < 0) {
      val tempCal = asOnCal.clone() as Calendar
      tempCal.add(Calendar.MONTH, -1)
      val maxDaysInPrevMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
      days += maxDaysInPrevMonth
      months--
    }

    if (months < 0) {
      months += 12
      years--
    }

    // Next Birthday calculation
    val nextBday = Calendar.getInstance().apply {
      set(Calendar.YEAR, asOnCal.get(Calendar.YEAR))
      set(Calendar.MONTH, birth.month - 1)
      set(Calendar.DAY_OF_MONTH, birth.day)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (nextBday.before(asOnCal)) {
      nextBday.add(Calendar.YEAR, 1)
    }

    var nbMonths = nextBday.get(Calendar.MONTH) - asOnCal.get(Calendar.MONTH)
    var nbDays = nextBday.get(Calendar.DAY_OF_MONTH) - asOnCal.get(Calendar.DAY_OF_MONTH)

    if (nbDays < 0) {
      val tempCal = nextBday.clone() as Calendar
      tempCal.add(Calendar.MONTH, -1)
      val maxPrev = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
      nbDays += maxPrev
      nbMonths--
    }
    if (nbMonths < 0) {
      nbMonths += 12
    }

    val dayOfWeekFmt = SimpleDateFormat("EEEE", Locale.US)
    val nextBdayDOW = dayOfWeekFmt.format(nextBday.time)

    // Total counts
    val diffMillis = (asOnCal.timeInMillis - birthCal.timeInMillis).coerceAtLeast(0L)
    val totalDays = diffMillis / (24L * 60 * 60 * 1000)
    val totalWeeks = totalDays / 7
    val totalMonths = (years * 12L) + months

    val sdfPretty = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    val formattedAsOn = sdfPretty.format(asOnCal.time)

    // EBL Credit Card & Retail Loan Eligibility Criteria:
    // Credit Card: 21 - 65 years (Salaried: 21-60, Business: 21-65)
    // Personal Loan: 21 - 60 years
    // Auto Loan: 21 - 65 years
    // Home Loan: 21 - 70 years
    val isEligibleCC: Boolean
    val ccEligibilityText: String
    val isEligibleLoan: Boolean
    val loanEligibilityText: String

    when {
      years < 21 -> {
        isEligibleCC = false
        ccEligibilityText = "🚫 Underage (ক্রেডিট কার্ডের জন্য ন্যূনতম ২১ বছর বয়স বাধ্যতামূলক)"
        isEligibleLoan = false
        loanEligibilityText = "🚫 Underage (রিটেল লোনের জন্য ন্যূনতম ২১ বছর বয়স বাধ্যতামূলক)"
      }
      years in 21..60 -> {
        isEligibleCC = true
        ccEligibilityText = "✓ সম্পূর্ণ যোগ্য (Eligible for Salaried & Business Credit Card, 21-60 Years)"
        isEligibleLoan = true
        loanEligibilityText = "✓ সম্পূর্ণ যোগ্য (Eligible for Personal, Auto & Home Loan, 21-60 Years)"
      }
      years in 61..65 -> {
        isEligibleCC = true
        ccEligibilityText = "✓ বিজনেস পারসন ক্রেডিট কার্ড যোগ্য (Business Person Credit Card, Max 65 Years)"
        isEligibleLoan = true
        loanEligibilityText = "✓ অটো ও হোম লোন যোগ্য (Auto Loan max 65 yrs, Home Loan max 70 yrs)"
      }
      years in 66..70 -> {
        isEligibleCC = false
        ccEligibilityText = "⚠️ ৬৫+ বছর (Senior Citizen: ক্রেডিট কার্ড প্রযোজ্য নয়)"
        isEligibleLoan = true
        loanEligibilityText = "✓ শুধুমাত্র হোম লোন যোগ্য (Home Loan up to 70 yrs subject to income)"
      }
      else -> {
        isEligibleCC = false
        ccEligibilityText = "🚫 ৭০+ বছর (Senior Citizen: ক্রেডিট কার্ড প্রযোজ্য নয়)"
        isEligibleLoan = false
        loanEligibilityText = "🚫 ৭০+ বছর (রিটেল লোন প্রযোজ্য নয়, কো-অ্যাপ্লিক্যান্ট/ওয়ারিশান বিবেচনা)"
      }
    }

    val summaryBuilder = StringBuilder()
    summaryBuilder.appendLine("🎂 EBL Age & Eligibility Assessment")
    summaryBuilder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    summaryBuilder.appendLine("• Date of Birth: ${birth.formattedSlash} (${birth.formattedPretty})")
    summaryBuilder.appendLine("• Calculated As On: $formattedAsOn")
    summaryBuilder.appendLine("• Exact Age: $years Years, $months Months, $days Days")
    summaryBuilder.appendLine("• Next Birthday In: $nbMonths Months, $nbDays Days ($nextBdayDOW)")
    summaryBuilder.appendLine("• Total Lived: $totalDays Days | $totalWeeks Weeks | $totalMonths Months")
    summaryBuilder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    summaryBuilder.appendLine("💳 Credit Card Status: $ccEligibilityText")
    summaryBuilder.appendLine("🏦 Retail Loan Status: $loanEligibilityText")

    return AgeCalculation(
      years = years,
      months = months,
      days = days,
      nextBirthdayMonths = nbMonths,
      nextBirthdayDays = nbDays,
      nextBirthdayDayOfWeek = nextBdayDOW,
      totalDays = totalDays,
      totalWeeks = totalWeeks,
      totalMonths = totalMonths,
      formattedBirthDate = birth.formattedSlash,
      formattedAsOnDate = formattedAsOn,
      creditCardEligibility = ccEligibilityText,
      loanEligibility = loanEligibilityText,
      isEligibleCreditCard = isEligibleCC,
      isEligibleLoan = isEligibleLoan,
      fullSummaryText = summaryBuilder.toString()
    )
  }
}
