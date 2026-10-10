package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build

object CallingService {

  private var toneGenerator: ToneGenerator? = null
  private var activeRingtone: Ringtone? = null

  fun playRingtone(context: Context) {
    try {
      stopRingtone()
      val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      if (ringtoneUri != null) {
        activeRingtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)?.apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLooping = true
          }
          play()
        }
      }
      // Also emit dial tone backup in case device is in vibrate/silent
      playDialTone()
    } catch (_: Exception) {
      playDialTone()
    }
  }

  fun stopRingtone() {
    try {
      activeRingtone?.stop()
      activeRingtone = null
    } catch (_: Exception) {}
    stopTone()
  }

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
      stopRingtone()
      if (toneGenerator == null) {
        toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
      }
      toneGenerator?.stopTone()
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
    } catch (_: Exception) {}
  }

  fun playEndCallTone() {
    try {
      stopRingtone()
      if (toneGenerator == null) {
        toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
      }
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
      stopRingtone()
      toneGenerator?.release()
      toneGenerator = null
    } catch (_: Exception) {}
  }
}
