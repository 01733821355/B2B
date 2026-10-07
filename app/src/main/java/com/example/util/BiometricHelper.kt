package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

  enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
  }

  fun checkBiometricAvailability(context: Context): BiometricAvailability {
    val biometricManager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.BIOMETRIC_WEAK

    return when (biometricManager.canAuthenticate(authenticators)) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
      else -> BiometricAvailability.UNAVAILABLE
    }
  }

  fun isBiometricReady(context: Context): Boolean {
    return checkBiometricAvailability(context) == BiometricAvailability.AVAILABLE
  }

  fun promptBiometricLogin(
    activity: FragmentActivity,
    title: String = "Fingerprint Login",
    subtitle: String = "Scan your fingerprint to authenticate",
    description: String = "Confirm your identity with biometric security",
    negativeButtonText: String = "Use Password",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        // Do not display error for intentional cancellations
        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
            errorCode != BiometricPrompt.ERROR_CANCELED) {
          onError(errString.toString())
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onError("Fingerprint not recognized. Please try again.")
      }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setDescription(description)
      .setNegativeButtonText(negativeButtonText)
      .setConfirmationRequired(false)
      .build()

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo)
  }
}
