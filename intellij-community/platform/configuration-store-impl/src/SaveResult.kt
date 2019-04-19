// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.impl.stores.SaveSessionAndFile
import com.intellij.util.SmartList
import com.intellij.util.lang.CompoundRuntimeException

internal class SaveResult {
  companion object {
    val EMPTY = SaveResult()
  }

  private val errors: MutableList<Throwable> = SmartList()
  val readonlyFiles: MutableList<SaveSessionAndFile> = SmartList()

  @Suppress("MemberVisibilityCanBePrivate")
  var isChanged = false

  @Synchronized
  fun addError(error: Throwable) {
    errors.add(error)
  }

  @Synchronized
  fun addReadOnlyFile(info: SaveSessionAndFile) {
    readonlyFiles.add(info)
  }

  fun addErrors(list: List<Throwable>) {
    if (list.isEmpty()) {
      return
    }

    synchronized(this) {
      errors.addAll(list)
    }
  }

  @Synchronized
  fun appendTo(saveResult: SaveResult) {
    if (this === EMPTY) {
      return
    }

    synchronized(saveResult) {
      saveResult.errors.addAll(errors)
      saveResult.readonlyFiles.addAll(readonlyFiles)

      if (isChanged) {
        saveResult.isChanged = isChanged
      }
    }
  }

  @Synchronized
  fun throwIfErrored() {
    CompoundRuntimeException.throwIfNotEmpty(errors)
  }
}