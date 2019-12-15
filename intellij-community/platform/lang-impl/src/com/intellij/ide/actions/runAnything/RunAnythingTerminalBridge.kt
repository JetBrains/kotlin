// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandExecutionProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingRecentProjectProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.terminal.TerminalShellCommandHandler

class RunAnythingTerminalBridge : TerminalShellCommandHandler {
  override fun matches(project: Project, workingDirectory: String?, localSession: Boolean, command: String): Boolean {
    return RunAnythingProvider.EP_NAME.extensionList
      .filter { checkForCLI(it) }
      .any { provider -> provider.findMatchingValue(createDataContext(project, localSession, workingDirectory), command) != null }
  }

  override fun execute(project: Project, workingDirectory: String?, localSession: Boolean, command: String): Boolean {
    return RunAnythingProvider.EP_NAME.extensionList
      .filter { checkForCLI(it) }
      .any { provider ->
        val dataContext = createDataContext(project, localSession, workingDirectory)
        provider.findMatchingValue(dataContext, command)?.let { provider.execute(dataContext, it); return true } ?: false
      }
  }

  companion object {
    private fun createDataContext(project: Project, localSession: Boolean, workingDirectory: String?): DataContext {
      return SimpleDataContext.getSimpleContext(
        mutableMapOf<String, Any?>()
          .also {
            it[CommonDataKeys.PROJECT.name] = project
            it[CommonDataKeys.VIRTUAL_FILE.name] =
              if (localSession && workingDirectory != null) LocalFileSystem.getInstance().findFileByPath(workingDirectory) else null
          }, null)
    }

    private fun checkForCLI(it: RunAnythingProvider<*>?) = it !is RunAnythingCommandExecutionProvider && it !is RunAnythingRecentProjectProvider
  }
}