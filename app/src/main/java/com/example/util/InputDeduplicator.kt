package com.example.util

import android.os.SystemClock

/**
 * Utility to eliminate duplicate characters ("double typing") caused by web streaming emulators,
 * hardware keyboard dual-event bridges (where host KeyDown event and guest IME commitText fire together),
 * or rapid text input race conditions in Jetpack Compose.
 */
class InputDeduplicator(private val rapidThresholdMs: Long = 65L) {
  private var lastInputTime: Long = 0L
  private var lastInputChar: Char? = null

  /**
   * Filters the incoming string from onValueChange against the previous string.
   * If an identical character was added within [rapidThresholdMs] or a single keypress
   * resulted in duplicate identical characters at the cursor, it deduplicates cleanly.
   */
  fun filter(oldText: String, newText: String): String {
    val now = SystemClock.uptimeMillis()
    val oldLen = oldText.length
    val newLen = newText.length

    // 1. Deletions or identical text: Always accept
    if (newLen <= oldLen) {
      lastInputChar = null
      lastInputTime = now
      return newText
    }

    // 2. Dual-character burst in a single event: e.g. typing "a" produces "aa" directly (newLen == oldLen + 2)
    if (newLen == oldLen + 2) {
      var diffIdx = 0
      while (diffIdx < oldLen && oldText[diffIdx] == newText[diffIdx]) {
        diffIdx++
      }
      val char1 = newText[diffIdx]
      val char2 = newText[diffIdx + 1]
      if (char1 == char2) {
        // Collapse the duplicate character
        val collapsed = newText.removeRange(diffIdx, diffIdx + 1)
        lastInputChar = char1
        lastInputTime = now
        return collapsed
      }
    }

    // 3. Sequential rapid dual-event: Event 1 added char, Event 2 (< 65ms later) adds the exact same char
    if (newLen == oldLen + 1) {
      var diffIdx = 0
      while (diffIdx < oldLen && oldText[diffIdx] == newText[diffIdx]) {
        diffIdx++
      }
      val insertedChar = newText[diffIdx]
      val timeDiff = now - lastInputTime

      if (lastInputChar != null && insertedChar == lastInputChar && timeDiff < rapidThresholdMs) {
        // Emulator dual-event race (KeyDown + IME commitText) -> Swallow duplicate
        return oldText
      }

      lastInputChar = insertedChar
      lastInputTime = now
      return newText
    }

    // 4. Multi-character paste (> 2 characters) or multiple different characters: Accept
    lastInputChar = null
    lastInputTime = now
    return newText
  }

  fun reset() {
    lastInputTime = 0L
    lastInputChar = null
  }
}
