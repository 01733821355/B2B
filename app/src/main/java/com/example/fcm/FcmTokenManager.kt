package com.example.fcm

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.FcmTokenEntity
import com.example.data.model.UserEntity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FcmTokenManager {
  private const val TAG = "FcmTokenManager"
  private const val PREFS_NAME = "fcm_prefs"
  private const val KEY_CURRENT_TOKEN = "current_fcm_token"

  private val httpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()
  }

  fun initFcm(context: Context, currentUser: UserEntity?) {
    try {
      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
          Log.w(TAG, "Fetching FCM registration token failed", task.exception)
          return@addOnCompleteListener
        }
        val token = task.result ?: return@addOnCompleteListener
        Log.d(TAG, "FCM Registration Token: $token")

        // Persist token in SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_TOKEN, token).apply()

        // Subscribe to broadcast topic
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")

        if (currentUser != null) {
          val role = currentUser.role.trim().lowercase()
          val rmCode = currentUser.rmCode.trim().lowercase()
          if (role.isNotBlank()) {
            FirebaseMessaging.getInstance().subscribeToTopic("role_$role")
          }
          if (rmCode.isNotBlank()) {
            FirebaseMessaging.getInstance().subscribeToTopic("user_$rmCode")
          }

          CoroutineScope(Dispatchers.IO).launch {
            saveAndSyncToken(
              context = context,
              token = token,
              rmCode = currentUser.rmCode,
              userName = currentUser.name,
              role = currentUser.role
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing FCM", e)
    }
  }

  fun onNewToken(context: Context, token: String) {
    Log.d(TAG, "Refreshed FCM token: $token")
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_CURRENT_TOKEN, token).apply()

    CoroutineScope(Dispatchers.IO).launch {
      val db = AppDatabase.getDatabase(context)
      val lastRm = db.appSettingDao().getSetting("last_logged_rm_code")?.settingValue?.trim() ?: ""
      val user = if (lastRm.isNotBlank()) db.userDao().getUser(lastRm) else null

      saveAndSyncToken(
        context = context,
        token = token,
        rmCode = user?.rmCode ?: lastRm.ifBlank { "UNKNOWN" },
        userName = user?.name ?: "EBL Officer",
        role = user?.role ?: "RM"
      )
    }
  }

  suspend fun saveAndSyncToken(
    context: Context,
    token: String,
    rmCode: String,
    userName: String,
    role: String
  ) = withContext(Dispatchers.IO) {
    val db = AppDatabase.getDatabase(context)
    val now = System.currentTimeMillis()
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    val entity = FcmTokenEntity(
      token = token,
      rmCode = rmCode,
      userName = userName,
      role = role,
      deviceModel = deviceModel,
      androidVersion = androidVersion,
      updatedAt = now,
      isSynced = false
    )
    db.fcmTokenDao().insertToken(entity)

    // Sync token to Google Sheets backend
    try {
      val syncStatus = db.appSettingDao().getSyncStatus()
      val webAppUrl = syncStatus?.appsScriptUrl?.ifBlank {
        "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
      } ?: "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"

      val payload = JSONObject().apply {
        put("action", "REGISTER_FCM_TOKEN")
        put("token", token)
        put("rmCode", rmCode)
        put("userName", userName)
        put("role", role)
        put("deviceModel", deviceModel)
        put("androidVersion", androidVersion)
        put("timestamp", com.example.util.DateUtils.formatDateTime(now))
        put("spreadsheetId", syncStatus?.spreadsheetId?.ifBlank { "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI" } ?: "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI")
        put("secretKey", syncStatus?.syncSecretKey?.ifBlank { "ebl_secure_sync_token_2026" } ?: "ebl_secure_sync_token_2026")
      }

      val body = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(webAppUrl).post(body).build()
      httpClient.newCall(request).execute().use { resp ->
        if (resp.isSuccessful) {
          db.fcmTokenDao().markSynced(token)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to register FCM token with backend", e)
    }
  }

  suspend fun sendInstantPush(
    context: Context? = null,
    title: String,
    body: String,
    targetType: String = "ALL", // "ALL", "ROLE", "USER"
    targetValue: String = "",   // e.g. "RM", "ADMIN", or rmCode "104393"
    targetScreen: String? = null,
    targetFileId: String? = null,
    extras: Map<String, String>? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val dbUrl = if (context != null) {
        try {
          AppDatabase.getDatabase(context).appSettingDao().getSyncStatus()?.appsScriptUrl?.ifBlank { null }
        } catch (_: Exception) { null }
      } else null
      val webAppUrl = dbUrl ?: "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"

      val payload = JSONObject().apply {
        put("action", "SEND_FCM_PUSH")
        put("title", title)
        put("body", body)
        put("targetType", targetType.uppercase())
        put("targetValue", targetValue)
        if (!targetScreen.isNullOrBlank()) put("targetScreen", targetScreen)
        if (!targetFileId.isNullOrBlank()) put("targetFileId", targetFileId)
        if (extras != null) {
          val extrasObj = JSONObject()
          extras.forEach { (k, v) -> extrasObj.put(k, v) }
          put("extras", extrasObj)
        }
        put("secretKey", "ebl_secure_sync_token_2026")
      }

      val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder().url(webAppUrl).post(requestBody).build()

      httpClient.newCall(request).execute().use { resp ->
        val respStr = resp.body?.string().orEmpty()
        if (resp.isSuccessful) {
          Result.success("Instant push dispatched successfully.")
        } else {
          Result.failure(Exception("HTTP ${resp.code}: $respStr"))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to send instant push", e)
      Result.failure(e)
    }
  }

  fun getCurrentToken(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CURRENT_TOKEN, "").orEmpty()
  }
}
