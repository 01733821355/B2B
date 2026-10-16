package com.example

import com.example.util.AgeCalculatorHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAgeCalculator_anyDateFormat() {
    // 1. "25 10 1997" -> 25/10/1997
    val r1 = AgeCalculatorHelper.parseAnyDate("25 10 1997")
    assertNotNull(r1)
    assertEquals(25, r1!!.day)
    assertEquals(10, r1.month)
    assertEquals(1997, r1.year)
    assertEquals("25/10/1997", r1.formattedSlash)

    // 2. "25 10 97" -> 25/10/1997 (2-digit year conversion: 97 -> 1997)
    val r2 = AgeCalculatorHelper.parseAnyDate("25 10 97")
    assertNotNull(r2)
    assertEquals(25, r2!!.day)
    assertEquals(10, r2.month)
    assertEquals(1997, r2.year)
    assertEquals("25/10/1997", r2.formattedSlash)

    // 3. Slashes: "25/10/1997" and "25/10/97"
    val r3 = AgeCalculatorHelper.parseAnyDate("25/10/1997")
    assertNotNull(r3)
    assertEquals("25/10/1997", r3!!.formattedSlash)

    val r4 = AgeCalculatorHelper.parseAnyDate("25/10/97")
    assertNotNull(r4)
    assertEquals("25/10/1997", r4!!.formattedSlash)

    // 4. Dashes: "25-10-1997" and "25-10-97"
    val r5 = AgeCalculatorHelper.parseAnyDate("25-10-97")
    assertNotNull(r5)
    assertEquals("25/10/1997", r5!!.formattedSlash)

    // 5. Continuous 8-digit and 6-digit: "25101997", "251097"
    val r6 = AgeCalculatorHelper.parseAnyDate("25101997")
    assertNotNull(r6)
    assertEquals("25/10/1997", r6!!.formattedSlash)

    val r7 = AgeCalculatorHelper.parseAnyDate("251097")
    assertNotNull(r7)
    assertEquals("25/10/1997", r7!!.formattedSlash)

    // 6. Bengali numerals: "২৫ ১০ ৯৭"
    val r8 = AgeCalculatorHelper.parseAnyDate("২৫ ১০ ৯৭")
    assertNotNull(r8)
    assertEquals("25/10/1997", r8!!.formattedSlash)

    // 7. Age calculation check
    val asOn = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.OCTOBER)
      set(Calendar.DAY_OF_MONTH, 15)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val calc = AgeCalculatorHelper.calculateAge(r1, asOn)
    assertEquals(28, calc.years)
    assertEquals(11, calc.months)
    assertEquals(20, calc.days)
    assertTrue(calc.isEligibleCreditCard)
    assertTrue(calc.isEligibleLoan)
  }
}
