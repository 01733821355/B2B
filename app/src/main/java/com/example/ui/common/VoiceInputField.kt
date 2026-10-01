package com.example.ui.common

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblNavyPrimary
import java.util.Locale

@Composable
fun VoiceInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  leadingIcon: @Composable (() -> Unit)? = null,
  singleLine: Boolean = true,
  maxLines: Int = 1,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  isError: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = ""
) {
  val context = LocalContext.current

  val speechLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spokenText.isNullOrBlank()) {
        val updated = if (value.isBlank()) spokenText else "$value $spokenText"
        onValueChange(updated)
      }
    }
  }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, fontSize = 12.sp) },
    placeholder = if (placeholder.isNotBlank()) { { Text(placeholder, fontSize = 12.sp) } } else null,
    leadingIcon = leadingIcon,
    trailingIcon = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (value.isNotEmpty() && !readOnly) {
          IconButton(
            onClick = { onValueChange("") },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Clear, contentDescription = "Clear text", tint = Color.Gray, modifier = Modifier.size(16.dp))
          }
        }
        if (!readOnly && enabled) {
          IconButton(
            onClick = {
              try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                  putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                  putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                  putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to input text...")
                }
                speechLauncher.launch(intent)
              } catch (e: Exception) {
                Toast.makeText(context, "Voice input not supported on this device: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.size(32.dp).testTag("${testTag}_voice_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Voice Input",
              tint = EblNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    },
    singleLine = singleLine,
    maxLines = maxLines,
    enabled = enabled,
    readOnly = readOnly,
    isError = isError,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    modifier = modifier
      .fillMaxWidth()
      .testTag(testTag)
  )
}
