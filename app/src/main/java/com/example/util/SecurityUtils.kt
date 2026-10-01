package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityUtils {
  private val random = SecureRandom()

  fun generateSalt(length: Int = 16): String {
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPassword(password: String, salt: String): String {
    val input = "$salt:$password:SECURE_OPERATIONS_SALT_2026"
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
    val computed = hashPassword(password, salt)
    return computed.equals(expectedHash, ignoreCase = true)
  }

  fun generateFileId(rmCode: String, sequence: Int = (1000..9999).random()): String {
    val cleanRm = rmCode.takeLast(4)
    val randomSuffix = (100..999).random()
    return "DOC-2026-$cleanRm-$randomSuffix"
  }

  fun generateUniqueId(): String {
    return UUID.randomUUID().toString()
  }

  fun generateTemporaryPassword(): String {
    val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val lower = "abcdefghijkmnopqrstuvwxyz"
    val numbers = "23456789"
    val special = "#$@!"
    
    val p1 = upper.random()
    val p2 = lower.random()
    val p3 = numbers.random()
    val p4 = special.random()
    val remaining = (1..4).map { (upper + lower + numbers).random() }.joinToString("")
    return "$p1$p2$p3$p4$remaining"
  }

  fun isPasswordValid(password: String): Pair<Boolean, String?> {
    if (password.length < 6) {
      return Pair(false, "Password must be at least 6 characters long.")
    }
    return Pair(true, null)
  }
}
