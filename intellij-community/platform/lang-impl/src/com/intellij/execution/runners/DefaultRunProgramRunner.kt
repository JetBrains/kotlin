// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.concurrency.resolvedPromise

private class DefaultRunProgramRunner : ProgramRunner<RunnerSettings> {
  override fun getRunnerId() = "defaultRunRunner"

  @Throws(ExecutionException::class)
  override fun execute(environment: ExecutionEnvironment, callback: ProgramRunner.Callback?) {
    val state = environment.state ?: return
    ExecutionManager.getInstance(environment.project).startRunProfile(environment, callback, {
        FileDocumentManager.getInstance().saveAllDocuments()
        if (state is DebuggableRunProfileState) {
          state.execute(-1)
            .then {
              it?.let {
                RunContentBuilder(it, environment).showRunContent(environment.contentToReuse)
              }
            }
        }
        else {
          resolvedPromise(showRunContent(state.execute(environment.executor, this@DefaultRunProgramRunner), environment))
        }
      })
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return DefaultRunExecutor.EXECUTOR_ID == executorId && profile !is RunConfigurationWithSuppressedDefaultRunAction
  }
}
