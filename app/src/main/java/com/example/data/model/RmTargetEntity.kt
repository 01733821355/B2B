package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rm_targets")
data class RmTargetEntity(
  @PrimaryKey val rmCode: String,
  val creditCardTarget: Int = 20,
  val corporateCardTarget: Int = 10,
  val b2bTarget: Int = 15,
  val updatedAt: Long = System.currentTimeMillis(),
  val targetYear: Int = 2026,
  val activeQuarter: String = "Q4",
  val q1CreditCardTarget: Int = 20,
  val q1CorporateCardTarget: Int = 10,
  val q1B2bTarget: Int = 15,
  val q2CreditCardTarget: Int = 20,
  val q2CorporateCardTarget: Int = 10,
  val q2B2bTarget: Int = 15,
  val q3CreditCardTarget: Int = 20,
  val q3CorporateCardTarget: Int = 10,
  val q3B2bTarget: Int = 15,
  val q4CreditCardTarget: Int = 20,
  val q4CorporateCardTarget: Int = 10,
  val q4B2bTarget: Int = 15
) {
  fun getTargetForQuarter(quarter: String): Triple<Int, Int, Int> {
    return when (quarter.trim().uppercase()) {
      "Q1" -> Triple(q1CreditCardTarget, q1CorporateCardTarget, q1B2bTarget)
      "Q2" -> Triple(q2CreditCardTarget, q2CorporateCardTarget, q2B2bTarget)
      "Q3" -> Triple(q3CreditCardTarget, q3CorporateCardTarget, q3B2bTarget)
      "Q4" -> Triple(q4CreditCardTarget, q4CorporateCardTarget, q4B2bTarget)
      else -> Triple(creditCardTarget, corporateCardTarget, b2bTarget)
    }
  }

  fun getEffectiveCcTarget(quarter: String? = null): Int {
    val q = quarter ?: activeQuarter
    return when (q.trim().uppercase()) {
      "Q1" -> q1CreditCardTarget
      "Q2" -> q2CreditCardTarget
      "Q3" -> q3CreditCardTarget
      "Q4" -> q4CreditCardTarget
      else -> creditCardTarget
    }
  }

  fun getEffectiveCorpTarget(quarter: String? = null): Int {
    val q = quarter ?: activeQuarter
    return when (q.trim().uppercase()) {
      "Q1" -> q1CorporateCardTarget
      "Q2" -> q2CorporateCardTarget
      "Q3" -> q3CorporateCardTarget
      "Q4" -> q4CorporateCardTarget
      else -> corporateCardTarget
    }
  }

  fun getEffectiveB2bTarget(quarter: String? = null): Int {
    val q = quarter ?: activeQuarter
    return when (q.trim().uppercase()) {
      "Q1" -> q1B2bTarget
      "Q2" -> q2B2bTarget
      "Q3" -> q3B2bTarget
      "Q4" -> q4B2bTarget
      else -> b2bTarget
    }
  }
}
