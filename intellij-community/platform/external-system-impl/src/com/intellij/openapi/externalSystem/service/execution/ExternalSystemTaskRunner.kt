// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.build.BuildView
import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.testframework.HistoryTestRunnableState
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import org.jetbrains.concurrency.resolvedPromise

internal class ExternalSystemTaskRunner : ProgramRunner<RunnerSettings> {
  override fun getRunnerId() = ExternalSystemConstants.RUNNER_ID

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is ExternalSystemRunConfiguration && DefaultRunExecutor.EXECUTOR_ID == executorId
  }

  override fun execute(environment: ExecutionEnvironment) {
    val state = environment.state ?: return
    ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
      resolvedPromise(doExecute(state, environment))
    }
  }

  private fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    if (state !is ExternalSystemRunnableState && state !is HistoryTestRunnableState) {
      return null
    }

    val executionResult = state.execute(environment.executor, this) ?: return null
    val runContentDescriptor = RunContentBuilder(executionResult, environment).showRunContent(environment.contentToReuse) ?: return null

    if (state is HistoryTestRunnableState) {
      return runContentDescriptor
    }

    (state as ExternalSystemRunnableState).setContentDescriptor(runContentDescriptor)
    if (executionResult.executionConsole is BuildView) {
      return runContentDescriptor
    }

    val descriptor = object : RunContentDescriptor(runContentDescriptor.executionConsole, runContentDescriptor.processHandler,
                                                   runContentDescriptor.component, runContentDescriptor.displayName,
                                                   runContentDescriptor.icon, null,
                                                   runContentDescriptor.restartActions) {
      override fun isHiddenContent(): Boolean = true
    }
    descriptor.runnerLayoutUi = runContentDescriptor.runnerLayoutUi
    return descriptor
  }
}
