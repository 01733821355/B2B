package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RM File Management Suite", appName)
  }

  @Test
  fun `verify password hashing and verification`() {
    val salt = com.example.util.SecurityUtils.generateSalt()
    val hash = com.example.util.SecurityUtils.hashPassword("secret123", salt)
    val isValid = com.example.util.SecurityUtils.verifyPassword("secret123", salt, hash)
    val isWrong = com.example.util.SecurityUtils.verifyPassword("wrongpass", salt, hash)
    assertEquals(true, isValid)
    assertEquals(false, isWrong)
  }
}
