// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.stats.storage.FilePathProvider
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.replaceService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.util.*
import java.util.concurrent.TimeUnit

class FileLoggerTest : HeavyPlatformTestCase() {
  private lateinit var dir: File
  private lateinit var logFile: File

  private lateinit var pathProvider: FilePathProvider

  override fun setUp() {
    super.setUp()
    dir = createTempDirectory()
    logFile = File(dir, "unique_1")

    pathProvider = mock(FilePathProvider::class.java).apply {
      `when`(getStatsDataDirectory()).thenReturn(dir)
      `when`(getUniqueFile()).thenReturn(logFile)
    }

    CompletionTrackerInitializer.isEnabledInTests = true
  }

  override fun tearDown() {
    CompletionTrackerInitializer.isEnabledInTests = false
    try {
      dir.deleteRecursively()
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun testLogging() {
    val fileLengthBefore = logFile.length()
    val uidProvider = mock(InstallationIdProvider::class.java).apply {
      `when`(installationId()).thenReturn(UUID.randomUUID().toString())
    }

    ApplicationManager.getApplication().replaceService(FilePathProvider::class.java, pathProvider, testRootDisposable)
    ApplicationManager.getApplication().replaceService(InstallationIdProvider::class.java, uidProvider, testRootDisposable)

    val loggerProvider = CompletionFileLoggerProvider()

    val logger = loggerProvider.newCompletionLogger()

    val editorMock = mock(Editor::class.java).apply {
      `when`(caretModel).thenReturn(mock(CaretModel::class.java))
    }

    val lookup = mock(LookupImpl::class.java).apply {
      `when`(getRelevanceObjects(ArgumentMatchers.any(), ArgumentMatchers.anyBoolean())).thenReturn(emptyMap())
      `when`(items).thenReturn(emptyList())
      `when`(psiFile).thenReturn(null)
      `when`(editor).thenReturn(editorMock)
    }

    val watchService = FileSystems.getDefault().newWatchService()
    val key = dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)

    logger.completionStarted(lookup, true, 2, System.currentTimeMillis())

    logger.completionCancelled(Fixtures.performance, System.currentTimeMillis())
    loggerProvider.dispose()

    var attemps = 0
    while (!logFile.exists() && attemps < 5) {
      watchService.poll(15, TimeUnit.SECONDS)
      attemps += 1
    }

    key.cancel()
    watchService.close()
    assertThat(logFile.length()).isGreaterThan(fileLengthBefore)
  }
}