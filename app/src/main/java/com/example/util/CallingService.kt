package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator

object CallingService {

  private var toneGenerator: ToneGenerator? = null

  fun playDialTone() {
    try {
      if (toneGenerator == null) {
        toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
      }
      toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 3000)
    } catch (_: Exception) {}
  }

  fun playConnectedTone() {
    try {
      toneGenerator?.stopTone()
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
    } catch (_: Exception) {}
  }

  fun playEndCallTone() {
    try {
      toneGenerator?.stopTone()
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
    } catch (_: Exception) {}
  }

  fun stopTone() {
    try {
      toneGenerator?.stopTone()
    } catch (_: Exception) {}
  }

  fun release() {
    try {
      toneGenerator?.release()
      toneGenerator = null
    } catch (_: Exception) {}
  }
}
