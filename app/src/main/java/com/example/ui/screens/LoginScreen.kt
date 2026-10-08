package com.example.ui.screens

import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.common.VoiceInputField
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  appCustomName: String = "RM File Management Suite",
  lastLoggedRmCode: String = "",
  onLogin: (String, String, Double?, Double?, String?, (Boolean, String?) -> Unit) -> Unit,
  onBiometricLogin: ((String, Double?, Double?, String?, (Boolean, String?) -> Unit) -> Unit)? = null,
  isBiometricEnabled: ((String) -> Boolean)? = null,
  isPasswordVerified: ((String) -> Boolean)? = null,
  modifier: Modifier = Modifier
) {
  var usernameInput by remember { mutableStateOf(lastLoggedRmCode) }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(lastLoggedRmCode) {
    if (usernameInput.isBlank() && lastLoggedRmCode.isNotBlank()) {
      usernameInput = lastLoggedRmCode
    }
  }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  fun doLogin() {
    if (usernameInput.isBlank() || passwordInput.isBlank()) {
      errorMessage = "Please enter both RM Code / Username and password."
      return
    }
    isLoading = true
    errorMessage = null

    coroutineScope.launch {
      var lat: Double? = null
      var lng: Double? = null
      var addr: String? = null

      // Automatically capture RM's location upon login for Mentor Live Google Maps tracking
      if (LocationHelper.hasLocationPermission(context)) {
        try {
          val loc = LocationHelper.getCurrentLocation(context)
          lat = loc.latitude
          lng = loc.longitude
          addr = loc.address
        } catch (_: Exception) {}
      }

      onLogin(usernameInput.trim(), passwordInput.trim(), lat, lng, addr) { success, err ->
        isLoading = false
        if (!success) {
          errorMessage = err ?: "Login failed. Please check your credentials."
        }
      }
    }
  }

  fun doBiometricLogin() {
    val targetCode = usernameInput.ifBlank { lastLoggedRmCode }.trim().uppercase()
    if (targetCode.isBlank()) {
      errorMessage = "Please enter your RM Code first."
      return
    }

    val isBioOn = isBiometricEnabled?.invoke(targetCode) == true
    val isPassOk = isPasswordVerified?.invoke(targetCode) == true

    if (!isPassOk && !isBioOn) {
      errorMessage = "প্রথমবার ইউজারনেম ও পাসওয়ার্ড দিয়ে লগইন বাধ্যতামূলক। একবার পাসওয়ার্ড দিয়ে লগইন করলে পরবর্তী সকল সময়ে সরাসরি ফিঙ্গারপ্রিন্ট দিয়ে ঢুকতে পারবেন। (Please sign in with username & password for the first time. Fingerprint will be enabled for subsequent logins.)"
      return
    }

    val activity = generateSequence(context) { ctx ->
      if (ctx is ContextWrapper) ctx.baseContext else null
    }.filterIsInstance<FragmentActivity>().firstOrNull()

    if (activity == null) {
      errorMessage = "Biometric prompt requires an active activity."
      return
    }

    val availability = BiometricHelper.checkBiometricAvailability(context)
    when (availability) {
      BiometricHelper.BiometricAvailability.NO_HARDWARE -> {
        errorMessage = "Fingerprint sensor is not available on this device."
        return
      }
      BiometricHelper.BiometricAvailability.NONE_ENROLLED -> {
        errorMessage = "No fingerprint enrolled. Please enroll a fingerprint in Android Settings."
        return
      }
      BiometricHelper.BiometricAvailability.UNAVAILABLE -> {
        errorMessage = "Fingerprint biometric sensor is currently unavailable."
        return
      }
      BiometricHelper.BiometricAvailability.AVAILABLE -> {}
    }

    isLoading = true
    errorMessage = null

    BiometricHelper.promptBiometricLogin(
      activity = activity,
      title = "EBL Fingerprint Sign In",
      subtitle = "Authenticate RM $targetCode",
      description = "Scan your registered fingerprint to enter the portal",
      negativeButtonText = "Use Password",
      onSuccess = {
        coroutineScope.launch {
          var lat: Double? = null
          var lng: Double? = null
          var addr: String? = null
          if (LocationHelper.hasLocationPermission(context)) {
            try {
              val loc = LocationHelper.getCurrentLocation(context)
              lat = loc.latitude
              lng = loc.longitude
              addr = loc.address
            } catch (_: Exception) {}
          }
          if (onBiometricLogin != null) {
            onBiometricLogin(targetCode, lat, lng, addr) { success, err ->
              isLoading = false
              if (!success) {
                errorMessage = err ?: "Fingerprint login failed."
              }
            }
          } else {
            isLoading = false
          }
        }
      },
      onError = { err ->
        isLoading = false
        errorMessage = err
      }
    )
  }

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // Professional Corporate Team Members in Suits Background Image
    Image(
      painter = painterResource(id = R.drawable.img_corporate_team),
      contentDescription = "Professional Team Background",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // Elegant Dark Gradient Overlay for contrast and readability
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x66091424),
              Color(0x880B1B30),
              Color(0xB3050D18)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Golden Brand Logo Icon
      Box(
        modifier = Modifier
          .size(76.dp)
          .background(EblGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = EblNavyDark,
          modifier = Modifier.size(42.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Team / App Name configured by Mentor
      Text(
        text = appCustomName,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Executive Operations & Field Force Team",
        color = EblGold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Sign In to Portal",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Text(
            text = "Enter your assigned RM Code or Admin username",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Error Message Alert
          if (errorMessage != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
              Text(
                text = errorMessage!!,
                color = Color(0xFFB91C1C),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          // Voice-Enabled RM Code / Username
          VoiceInputField(
            value = usernameInput,
            onValueChange = {
              usernameInput = it
              errorMessage = null
            },
            label = "RM Code / Username",
            placeholder = "e.g. 104393, 12345, or Admin0",
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = "Username", tint = EblNavyPrimary)
            },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            testTag = "login_username_input"
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Password
          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              errorMessage = null
            },
            label = { Text("Password") },
            placeholder = { Text("Enter your account password") },
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = "Password", tint = EblNavyPrimary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  tint = Color.Gray
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                doLogin()
              }
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_password_input")
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Login Button
          Button(
            onClick = { doLogin() },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
              containerColor = EblNavyPrimary,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("login_submit_btn")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Authenticating...", fontSize = 14.sp)
            } else {
              Text(
                text = "Sign In",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Biometric Fingerprint Login Section
          if (onBiometricLogin != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
              Text(
                text = "OR BIOMETRIC",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
              )
              HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
              onClick = { doBiometricLogin() },
              enabled = !isLoading,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.5.dp, EblGold),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFFFFBEB),
                contentColor = EblNavyDark
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_biometric_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint Login",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.Start) {
                Text(
                  text = "Fingerprint Sign In",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                val target = usernameInput.ifBlank { lastLoggedRmCode }.trim().uppercase()
                val isBioActive = target.isNotBlank() && (isBiometricEnabled?.invoke(target) == true)
                if (target.isNotBlank()) {
                  Text(
                    text = if (isBioActive) "✓ ফিঙ্গারপ্রিন্ট প্রস্তুত ($target) - ট্যাপ করুন" else "প্রথমবার পাসওয়ার্ড দিয়ে লগইন করুন (এরপর সক্রিয় হবে)",
                    fontSize = 10.sp,
                    color = if (isBioActive) Color(0xFF059669) else Color(0xFFB45309),
                    fontWeight = if (isBioActive) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Footer security notice
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Secure",
          tint = EblGold,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Protected by End-to-End Security Encryption",
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 11.sp
        )
      }
      Text(
        text = "Asia/Dhaka GMT+6 Session Logging Enabled",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 10.sp
      )
    }
  }
}
