// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import org.junit.Test
import java.lang.RuntimeException

class JdkDownloaderIntegrationTest : BasePlatformTestCase() {
  @Test
  fun `test default model can be downloaded and parsed`() {
    lateinit var lastError: Throwable
    repeat(5) {
      val result = runCatching {
        val data = JdkListDownloader.getInstance().downloadForUI(null)
        Assert.assertTrue(data.isNotEmpty())
        Assert.assertTrue(data.all { it.sharedIndexAliases.isNotEmpty() })
      }
      if (result.isSuccess) return
      lastError = result.exceptionOrNull()!!

      if (lastError.message?.startsWith("Failed to download list of available JDKs") == true) {
        Thread.sleep(5000)
      }
      else throw lastError
    }
    throw RuntimeException("Failed to download JDK list within several tries", lastError)
  }

  @Test
  fun `test default model should have JBR`() {
    lateinit var lastError: Throwable
    repeat(5) {
      val result = runCatching {
        val data = JdkListDownloader.getInstance().downloadModelForJdkInstaller(null)
        val jbr = data.filter { it.matchesVendor("jbr") }
        Assert.assertTrue(jbr.isNotEmpty())
      }
      if (result.isSuccess) return
      lastError = result.exceptionOrNull()!!

      if (lastError.message?.startsWith("Failed to download list of available JDKs") == true) {
        Thread.sleep(5000)
      }
      else throw lastError
    }
    throw RuntimeException("Failed to download JDK list within several tries", lastError)
  }

  @Test
  fun `test default model is cached`() {
    lateinit var lastError: Throwable
    repeat(5) {

      val downloader = JdkListDownloader.getInstance()
      val packs = List(10) { runCatching { downloader.downloadForUI(null) }.getOrNull() }.filterNotNull()

      if (packs.size < 3) {
        return@repeat
      }

      //must return cached JdkItem objects
      packs.forEach { p1 ->
        packs.forEach { p2 ->
          Assert.assertEquals(p1.size, p2.size)
          for (i in p1.indices) {
            Assert.assertSame(p1[i], p2[i])
          }
        }
      }
      return
    }
    throw RuntimeException("Failed to download JDK list within several tries", lastError)
  }
}
