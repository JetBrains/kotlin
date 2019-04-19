// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.openapi.Disposable
import com.intellij.stats.logger.ClientSessionValidator
import com.intellij.stats.logger.EventLoggerWithValidation
import com.intellij.stats.logger.LogFileManager
import com.intellij.stats.storage.FilePathProvider
import java.util.*

class CompletionFileLoggerProvider(filePathProvider: FilePathProvider, private val installationIdProvider: InstallationIdProvider) : Disposable, CompletionLoggerProvider() {
  private val eventLogger = EventLoggerWithValidation(LogFileManager(filePathProvider), ClientSessionValidator())

  override fun dispose() {
    eventLogger.dispose()
  }

  override fun newCompletionLogger(): CompletionLogger {
    val installationUID = installationIdProvider.installationId()
    val completionUID = UUID.randomUUID().toString()
    return CompletionFileLogger(installationUID.shortedUUID(), completionUID.shortedUUID(), eventLogger)
  }
}

private fun String.shortedUUID(): String {
  val start = this.lastIndexOf('-')
  if (start > 0 && start + 1 < this.length) {
    return this.substring(start + 1)
  }
  return this
}