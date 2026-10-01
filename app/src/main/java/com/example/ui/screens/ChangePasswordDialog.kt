package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblNavyPrimary

@Composable
fun ChangePasswordDialog(
  isForced: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
  var oldPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  var showOld by remember { mutableStateOf(false) }
  var showNew by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = {
      if (!isForced) onDismiss()
    },
    title = {
      Text(
        text = if (isForced) "Password Update Required" else "Change Password",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (isForced) {
          Text(
            text = "For security compliance, you must set a new personal password before accessing the system.",
            fontSize = 12.sp,
            color = Color(0xFFB45309),
            modifier = Modifier.padding(bottom = 8.dp)
          )
        }

        if (errorMessage != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Text(errorMessage!!, color = Color(0xFFB91C1C), fontSize = 12.sp)
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
          value = oldPassword,
          onValueChange = { oldPassword = it; errorMessage = null },
          label = { Text("Current Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showOld = !showOld }) {
              Icon(
                imageVector = if (showOld) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null
              )
            }
          },
          visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth().testTag("change_pwd_old")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it; errorMessage = null },
          label = { Text("New Password (min 6 chars)") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showNew = !showNew }) {
              Icon(
                imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null
              )
            }
          },
          visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth().testTag("change_pwd_new")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; errorMessage = null },
          label = { Text("Confirm New Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth().testTag("change_pwd_confirm")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (oldPassword.isBlank() || newPassword.isBlank()) {
            errorMessage = "Please enter all fields."
            return@Button
          }
          if (newPassword != confirmPassword) {
            errorMessage = "New passwords do not match."
            return@Button
          }
          if (newPassword.length < 6) {
            errorMessage = "New password must be at least 6 characters."
            return@Button
          }
          isSubmitting = true
          onSubmit(oldPassword, newPassword) { success, err ->
            isSubmitting = false
            if (success) {
              onDismiss()
            } else {
              errorMessage = err ?: "Failed to change password."
            }
          }
        },
        enabled = !isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
        modifier = Modifier.testTag("change_pwd_submit")
      ) {
        Text("Update Password")
      }
    },
    dismissButton = {
      if (!isForced) {
        OutlinedButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    }
  )
}
