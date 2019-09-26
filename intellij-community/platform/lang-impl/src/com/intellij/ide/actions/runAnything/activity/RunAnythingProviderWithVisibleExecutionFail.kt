// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ui.Messages

abstract class RunAnythingProviderWithVisibleExecutionFail<V> : RunAnythingProviderBase<V>() {

  protected abstract fun runAnything(dataContext: DataContext, value: V): Boolean

  private fun notifyExecutionFail(dataContext: DataContext, value: V) {
    Messages.showWarningDialog(
      fetchProject(dataContext),
      IdeBundle.message("run.anything.notification.warning.content", getCommand(value)),
      IdeBundle.message("run.anything.notification.warning.title")
    )
  }

  override fun execute(dataContext: DataContext, value: V) {
    try {
      if (!runAnything(dataContext, value)) {
        notifyExecutionFail(dataContext, value)
      }
    }
    catch (ex: Throwable) {
      notifyExecutionFail(dataContext, value)
      throw ex
    }
  }
}