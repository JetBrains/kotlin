// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.internal.statistic.eventLog.EventLogConfiguration
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.stats.logger.ClientSessionValidator
import com.intellij.stats.logger.EventLoggerWithValidation
import com.intellij.stats.logger.LogFileManager
import java.util.*

class CompletionFileLoggerProvider : Disposable, CompletionLoggerProvider() {
  private val eventLogger = EventLoggerWithValidation(LogFileManager(service()), ClientSessionValidator())

  override fun dispose() {
    eventLogger.dispose()
  }

  override fun newCompletionLogger(): CompletionLogger {
    val installationUID = service<InstallationIdProvider>().installationId()
    val completionUID = UUID.randomUUID().toString()
    val bucket = EventLogConfiguration.bucket.toString()
    return CompletionFileLogger(installationUID.shortedUUID(), completionUID.shortedUUID(), bucket, eventLogger)
  }
}

private fun String.shortedUUID(): String {
  val start = this.lastIndexOf('-')
  if (start > 0 && start + 1 < this.length) {
    return this.substring(start + 1)
  }
  return this
}