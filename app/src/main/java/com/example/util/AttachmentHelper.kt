package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FileAttachmentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SavedAttachmentResult(
  val file: File,
  val fileName: String,
  val fileType: String,
  val fileSizeBytes: Long,
  val storagePath: String,
  val fileUri: String
)

object AttachmentHelper {

  /**
   * Copies any incoming Content or File Uri to persistent app-internal storage.
   * This ensures the file is preserved across sessions and accessible by external intents.
   */
  suspend fun saveUriToInternalStorage(
    context: Context,
    sourceUri: Uri,
    preferredName: String? = null,
    preferredCategory: String = "Document"
  ): SavedAttachmentResult = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver
    
    // Resolve display name
    var resolvedName: String? = preferredName
    var resolvedSize = 0L

    try {
      contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1 && resolvedName == null) {
            resolvedName = cursor.getString(nameIndex)
          }
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (sizeIndex != -1) {
            resolvedSize = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (_: Exception) {}

    // Fallback name if unresolved
    val extension = getExtensionFromUri(context, sourceUri)
    val safeBase = resolvedName?.substringBeforeLast('.')
      ?: "${preferredCategory.replace(" ", "_")}_${System.currentTimeMillis()}"
    val finalName = if (resolvedName != null && resolvedName!!.contains(".")) {
      resolvedName!!
    } else {
      "$safeBase.$extension"
    }

    // Determine MIME type
    val mimeType = contentResolver.getType(sourceUri)
      ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
      ?: if (finalName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"

    // Target internal destination directory
    val attachDir = File(context.filesDir, "attachments").apply { mkdirs() }
    val uniqueFileName = "doc_${System.currentTimeMillis()}_${finalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
    val destinationFile = File(attachDir, uniqueFileName)

    // Copy stream
    try {
      contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(destinationFile).use { output ->
          input.copyTo(output)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    val actualSize = if (destinationFile.exists() && destinationFile.length() > 0) {
      destinationFile.length()
    } else {
      resolvedSize.coerceAtLeast(1024L)
    }

    val fileUri = try {
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destinationFile).toString()
    } catch (_: Exception) {
      Uri.fromFile(destinationFile).toString()
    }

    SavedAttachmentResult(
      file = destinationFile,
      fileName = finalName,
      fileType = mimeType,
      fileSizeBytes = actualSize,
      storagePath = destinationFile.absolutePath,
      fileUri = fileUri
    )
  }

  fun getGoogleDriveThumbnailUrl(url: String): String {
    if (!url.startsWith("http")) return url
    val fileId = extractGoogleDriveFileId(url)
    return if (fileId != null) {
      "https://drive.google.com/thumbnail?id=$fileId&sz=w1200"
    } else {
      url
    }
  }

  fun getGoogleDriveDirectCdnUrl(url: String): String {
    if (!url.startsWith("http")) return url
    val fileId = extractGoogleDriveFileId(url)
    return if (fileId != null) {
      "https://lh3.googleusercontent.com/d/$fileId"
    } else {
      url
    }
  }

  fun getGoogleDriveViewUrl(url: String): String {
    if (!url.startsWith("http")) return url
    val fileId = extractGoogleDriveFileId(url)
    return if (fileId != null) {
      "https://drive.google.com/file/d/$fileId/view?usp=sharing"
    } else {
      url
    }
  }

  fun getGoogleDriveDownloadUrl(url: String): String {
    if (!url.startsWith("http")) return url
    val fileId = extractGoogleDriveFileId(url)
    return if (fileId != null) {
      "https://drive.google.com/uc?export=download&id=$fileId"
    } else {
      url
    }
  }

  fun extractGoogleDriveFileId(url: String): String? {
    if (!url.startsWith("http")) return null
    val regex1 = Regex("/d/([a-zA-Z0-9_-]+)")
    val match1 = regex1.find(url)
    if (match1 != null) return match1.groupValues[1]

    val regex2 = Regex("[?&]id=([a-zA-Z0-9_-]+)")
    val match2 = regex2.find(url)
    if (match2 != null) return match2.groupValues[1]

    return null
  }

  /**
   * Resolves a guaranteed local File for the given attachment entity.
   * If remote, returns cached file if available, or attempts synchronous download on background threads.
   */
  fun resolveFile(context: Context, attachment: FileAttachmentEntity): File {
    // 1. Direct storagePath check
    if (attachment.storagePath.isNotBlank() && !attachment.storagePath.startsWith("http")) {
      val f = File(attachment.storagePath)
      if (f.exists() && f.length() > 0) return f
    }

    // 2. Internal attachments directory check
    val attachDir = File(context.filesDir, "attachments").apply { mkdirs() }
    val matchingFile = attachDir.listFiles()?.firstOrNull {
      (it.name.contains(attachment.fileName, ignoreCase = true) || it.name.contains(attachment.attachmentId)) &&
        !it.name.startsWith("EBL_DEMO_")
    }
    if (matchingFile != null && matchingFile.exists() && matchingFile.length() > 0) {
      return matchingFile
    }

    // 3. Try copying from fileUri if content://
    if (attachment.fileUri.startsWith("content://")) {
      try {
        val uri = Uri.parse(attachment.fileUri)
        val copyDest = File(attachDir, "att_${attachment.attachmentId}_${attachment.fileName}")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(copyDest).use { output -> input.copyTo(output) }
        }
        if (copyDest.exists() && copyDest.length() > 0) return copyDest
      } catch (_: Exception) {}
    }

    // 3.5. Try cached file from remote Google Drive URL
    val remoteCache = File(attachDir, "drive_${attachment.attachmentId}_${attachment.fileName}")
    if (attachment.fileUri.startsWith("http://") || attachment.fileUri.startsWith("https://")) {
      if (remoteCache.exists() && remoteCache.length() > 200) {
        return remoteCache
      }

      // If running on a worker thread, download directly
      if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
        val downloaded = downloadRemoteFileSync(attachment.fileUri, remoteCache)
        if (downloaded && remoteCache.exists() && remoteCache.length() > 200) {
          return remoteCache
        }
      } else {
        // Asynchronously schedule background download so it's ready for future calls
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
          downloadRemoteFileSync(attachment.fileUri, remoteCache)
        }
      }
    }

    return remoteCache
  }

  suspend fun resolveFileAsync(context: Context, attachment: FileAttachmentEntity): File? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val attachDir = File(context.filesDir, "attachments").apply { mkdirs() }
      if (attachment.storagePath.isNotBlank() && !attachment.storagePath.startsWith("http")) {
        val f = File(attachment.storagePath)
        if (f.exists() && f.length() > 0) return@withContext f
      }

      val remoteCache = File(attachDir, "drive_${attachment.attachmentId}_${attachment.fileName}")
      if (remoteCache.exists() && remoteCache.length() > 200) {
        return@withContext remoteCache
      }

      if (attachment.fileUri.startsWith("content://")) {
        try {
          val uri = Uri.parse(attachment.fileUri)
          val copyDest = File(attachDir, "att_${attachment.attachmentId}_${attachment.fileName}")
          context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(copyDest).use { output -> input.copyTo(output) }
          }
          if (copyDest.exists() && copyDest.length() > 0) return@withContext copyDest
        } catch (_: Exception) {}
      }

      if (attachment.fileUri.startsWith("http://") || attachment.fileUri.startsWith("https://")) {
        val ok = downloadRemoteFileSync(attachment.fileUri, remoteCache)
        if (ok && remoteCache.exists() && remoteCache.length() > 200) {
          return@withContext remoteCache
        }
      }

      val fallback = resolveFile(context, attachment)
      if (fallback.exists() && fallback.length() > 0) fallback else null
    }

  fun downloadRemoteFileSync(urlStr: String, destFile: File): Boolean {
    val fileId = extractGoogleDriveFileId(urlStr)
    val isPdf = destFile.name.endsWith(".pdf", ignoreCase = true)
    val candidateUrls = mutableListOf<String>()

    if (fileId != null) {
      if (!isPdf) {
        candidateUrls.add("https://lh3.googleusercontent.com/d/$fileId")
        candidateUrls.add("https://drive.google.com/thumbnail?id=$fileId&sz=w2000")
      }
      candidateUrls.add("https://drive.google.com/uc?export=download&id=$fileId")
      candidateUrls.add("https://docs.google.com/uc?id=$fileId&export=download")
    }
    candidateUrls.add(urlStr)

    for (targetUrl in candidateUrls) {
      try {
        val url = java.net.URL(targetUrl)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")

        var responseCode = conn.responseCode
        var finalConn = conn
        var redirects = 0
        while (responseCode in 300..399 && redirects < 5) {
          redirects++
          val newUrl = finalConn.getHeaderField("Location") ?: break
          finalConn = java.net.URL(newUrl).openConnection() as java.net.HttpURLConnection
          finalConn.connectTimeout = 15000
          finalConn.readTimeout = 30000
          finalConn.instanceFollowRedirects = true
          finalConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
          responseCode = finalConn.responseCode
        }

        if (responseCode in 200..299) {
          val tempFile = File(destFile.parentFile, "temp_${System.currentTimeMillis()}_${destFile.name}")
          finalConn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
          }

          if (tempFile.exists() && tempFile.length() > 200) {
            // Verify it is not an HTML error/auth landing page
            val headerBytes = ByteArray(100)
            val bytesRead = tempFile.inputStream().use { it.read(headerBytes) }
            val headerStr = String(headerBytes, 0, bytesRead.coerceAtLeast(0)).trim().lowercase()
            if (!headerStr.startsWith("<!doctype") && !headerStr.startsWith("<html")) {
              tempFile.copyTo(destFile, overwrite = true)
              tempFile.delete()
              return true
            }
          }
          tempFile.delete()
        }
      } catch (_: Exception) {}
    }
    return false
  }

  /**
   * Opens the attachment using external system viewer (Adobe, Drive, Photos, etc.)
   */
  fun viewAttachment(context: Context, attachment: FileAttachmentEntity) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      val file = resolveFileAsync(context, attachment)
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        if (file != null && file.exists() && file.length() > 0) {
          viewAttachmentWithFile(context, file, attachment)
        } else {
          // If local download could not complete, open Google Drive directly in browser
          val driveLink = getGoogleDriveViewUrl(attachment.fileUri)
          if (driveLink.startsWith("http")) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(driveLink)).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
          } else {
            val isLocalOnly = attachment.fileUri.startsWith("content://") ||
              attachment.fileUri.startsWith("/data/") ||
              (!attachment.fileUri.startsWith("http") && attachment.fileUri.isNotBlank()) ||
              attachment.storagePath.startsWith("/data/")
            val msg = if (isLocalOnly) {
              "ফাইলটি এখনও ক্লাউডে (Google Drive) আপলোড হয়নি, এটি আপলোডকারীর নিজস্ব ডিভাইসে সংরক্ষিত ছিল। Apps Script-এ Drive পারমিশন দিয়ে ফাইলটি পুনরায় আপলোড করুন।"
            } else {
              "ডকুমেন্টটি ডাউনলোড করা যায়নি। অনুগ্রহ করে ইন্টারনেট সংযোগ চেক করুন।"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  fun viewAttachmentWithFile(context: Context, file: File, attachment: FileAttachmentEntity) {
    try {
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val mime = attachment.fileType.ifBlank {
        if (file.name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*"
      }

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(intent, "Open '${attachment.fileName}' with").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Direct open failed, opening share menu...", Toast.LENGTH_SHORT).show()
      shareAttachment(context, attachment)
    }
  }

  fun downloadAttachment(context: Context, attachment: FileAttachmentEntity): Boolean {
    CoroutineScope(Dispatchers.IO).launch {
      downloadAttachmentAsync(context, attachment)
    }
    return true
  }

  suspend fun downloadAttachmentAsync(context: Context, attachment: FileAttachmentEntity): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val sourceFile = resolveFileAsync(context, attachment)
        if (sourceFile == null || !sourceFile.exists()) {
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "ডকুমেন্টটি ডাউনলোড করা যায়নি বা ফাইল পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
          }
          return@withContext false
        }
        val fileName = attachment.fileName.ifBlank { "Document_${System.currentTimeMillis()}" }
        val mime = attachment.fileType.ifBlank { "application/octet-stream" }

        var success = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EBL_Documents")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }

          val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
          if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
              sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            success = true
          }
        }

        if (!success) {
          val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          val eblFolder = File(publicDownloads, "EBL_Documents").apply { mkdirs() }
          val destFile = File(eblFolder, fileName)
          sourceFile.inputStream().use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
          }
          success = destFile.exists() && destFile.length() > 0
        }

        withContext(Dispatchers.Main) {
          Toast.makeText(
            context,
            "ডকুমেন্ট ডাউনলোড সফল হয়েছে!\nফাইল: $fileName\nপাথ: Downloads/EBL_Documents",
            Toast.LENGTH_LONG
          ).show()
        }
        true
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(
            context,
            "ডাউনলোডে সমস্যা হয়েছে। ফাইলটি শেয়ার অপশন দিয়ে সেভ করতে পারেন।",
            Toast.LENGTH_LONG
          ).show()
          shareAttachment(context, attachment)
        }
        false
      }
    }


  /**
   * Shares the attachment with any installed app (WhatsApp, Gmail, Drive, etc.)
   */
  fun shareAttachment(context: Context, attachment: FileAttachmentEntity) {
    try {
      val file = resolveFile(context, attachment)
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val mime = attachment.fileType.ifBlank { "*/*" }

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "EBL Document: ${attachment.fileName}")
        putExtra(Intent.EXTRA_TEXT, "EBL Customer Document: ${attachment.fileName}\nCategory: ${attachment.category}\nFile ID: ${attachment.fileId}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(shareIntent, "Share '${attachment.fileName}' via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Unable to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
  }

  private fun getExtensionFromUri(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri)
    if (mime != null) {
      val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
      if (!ext.isNullOrBlank()) return ext
    }
    val path = uri.path ?: ""
    return if (path.contains(".")) path.substringAfterLast('.') else "jpg"
  }

  /**
   * Encodes a local file to Base64 for Google Drive / Google Sheets uploading.
   * If it's an image, downscales to maxDimension (e.g. 1600px) and compresses to JPEG 85%
   * to ensure rapid transmission. For PDFs/docs up to 10MB, reads raw bytes.
   */
  fun fileToBase64(file: File, maxDimension: Int = 1600): String? {
    if (!file.exists() || file.length() == 0L) return null
    return try {
      val nameLower = file.name.lowercase()
      val isImage = listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp").any { nameLower.endsWith(it) }

      if (isImage) {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var sampleSize = 1
        var w = boundsOptions.outWidth
        var h = boundsOptions.outHeight
        while (w > maxDimension || h > maxDimension) {
          sampleSize *= 2
          w /= 2
          h /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

        val finalBmp = if (bmp.width > maxDimension || bmp.height > maxDimension) {
          val ratio = bmp.width.toFloat() / bmp.height.toFloat()
          val targetW: Int
          val targetH: Int
          if (bmp.width > bmp.height) {
            targetW = maxDimension
            targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
          } else {
            targetH = maxDimension
            targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
          }
          Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
        } else bmp

        val baos = ByteArrayOutputStream()
        finalBmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
      } else {
        if (file.length() > 10 * 1024 * 1024) return null // Max 10MB for documents
        val bytes = file.readBytes()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
