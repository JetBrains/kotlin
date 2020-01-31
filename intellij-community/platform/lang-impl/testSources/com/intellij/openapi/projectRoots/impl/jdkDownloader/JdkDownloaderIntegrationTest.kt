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
    run {
      repeat(5) {
        val result = runCatching {
          val data = JdkListDownloader.downloadForUI(null)
          Assert.assertTrue(data.isNotEmpty())
        }
        if (result.isSuccess) return
        lastError = result.exceptionOrNull()!!

        if (lastError.message?.startsWith("Failed to download list of available JDKs") == true) {
          Thread.sleep(5000)
        }
        else throw lastError
      }
    }
    throw RuntimeException("Failed to download JDK list within several tries", lastError)
  }
}
