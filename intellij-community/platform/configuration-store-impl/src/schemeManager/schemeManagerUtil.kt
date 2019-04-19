// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.LOG
import com.intellij.openapi.progress.ProcessCanceledException

internal inline fun <T> catchAndLog(file: () -> String, runnable: () -> T): T? {
  try {
    return runnable()
  }
  catch (e: ProcessCanceledException) {
    throw e
  }
  catch (e: Throwable) {
    LOG.error("Cannot read scheme ${file()}", e)
  }
  return null
}

internal fun nameIsMissed(bytes: ByteArray): RuntimeException {
  return RuntimeException("Name is missed:\n${bytes.toString(Charsets.UTF_8)}")
}