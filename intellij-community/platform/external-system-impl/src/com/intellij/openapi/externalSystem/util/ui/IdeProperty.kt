// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import com.intellij.openapi.application.ApplicationManager

open class IdeProperty<T>(initial: () -> T) : Property<T>(initial) {
  override fun assertIsExecutionThread() {
    ApplicationManager.getApplication().assertIsDispatchThread()
  }

  override fun invokeLater(action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(action)
  }
}