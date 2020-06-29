// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingRecentProjectProvider
import com.intellij.internal.statistic.collectors.fus.TerminalFusAwareHandler
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.terminal.TerminalShellCommandHandler
import com.intellij.terminal.TerminalExecutorAction

class RunAnythingTerminalBridge : TerminalShellCommandHandler, TerminalFusAwareHandler {
  override fun matches(project: Project, workingDirectory: String?, localSession: Boolean, command: String): Boolean {
    val dataContext = createDataContext(project, localSession, workingDirectory)
    return RunAnythingProvider.EP_NAME.extensionList
      .filter { checkForCLI(it) }
      .any { provider -> provider.findMatchingValue(dataContext, command) != null }
  }

  override fun execute(project: Project, workingDirectory: String?, localSession: Boolean, command: String, executorAction: TerminalExecutorAction): Boolean {
    val dataContext = createDataContext(project, localSession, workingDirectory, executorAction)
    return RunAnythingProvider.EP_NAME.extensionList
      .filter { checkForCLI(it) }
      .any { provider ->
        provider.findMatchingValue(dataContext, command)?.let { provider.execute(dataContext, it); return true } ?: false
      }
  }

  companion object {
    private fun createDataContext(project: Project, localSession: Boolean, workingDirectory: String?, action: TerminalExecutorAction? = null): DataContext {
      return SimpleDataContext.getSimpleContext(
        mutableMapOf<String, Any?>()
          .also {
            it[CommonDataKeys.PROJECT.name] = project
            val virtualFile =
              if (localSession && workingDirectory != null) LocalFileSystem.getInstance().findFileByPath(workingDirectory) else null
            if (virtualFile != null) {
              it[CommonDataKeys.VIRTUAL_FILE.name] = virtualFile
              it[RunAnythingProvider.EXECUTING_CONTEXT.name] = RunAnythingContext.RecentDirectoryContext(virtualFile.path)
            }
            it[RunAnythingAction.EXECUTOR_KEY.name] = action?.executor
          }, null)
    }

    private fun checkForCLI(it: RunAnythingProvider<*>?) = it !is RunAnythingCommandProvider
                                                           && it !is RunAnythingRecentProjectProvider
                                                           && it !is RunAnythingRunConfigurationProvider
  }

  override fun fillData(project: Project, workingDirectory: String?, localSession: Boolean, command: String, data: FeatureUsageData) {
    val dataContext = createDataContext(project, localSession, workingDirectory)
    val runAnythingProvider = RunAnythingProvider.EP_NAME.extensionList
      .filter { checkForCLI(it) }.ifEmpty { return }.first { provider -> provider.findMatchingValue(dataContext, command) != null }

    data.addData("runAnythingProvider", runAnythingProvider::class.java.simpleName)
  }
}