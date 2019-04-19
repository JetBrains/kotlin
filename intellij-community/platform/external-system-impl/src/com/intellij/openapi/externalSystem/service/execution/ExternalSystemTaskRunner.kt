/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.build.BuildView
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.testframework.sm.runner.history.ImportedTestRunnableState
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants

/**
 * @author Vladislav.Soroka
 */
class ExternalSystemTaskRunner : GenericProgramRunner<RunnerSettings>() {

  override fun getRunnerId(): String {
    return ExternalSystemConstants.RUNNER_ID
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return profile is ExternalSystemRunConfiguration && DefaultRunExecutor.EXECUTOR_ID == executorId
  }

  @Throws(ExecutionException::class)
  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    if (state !is ExternalSystemRunConfiguration.MyRunnableState && state !is ImportedTestRunnableState) {
      return null
    }

    val executionResult = state.execute(environment.executor, this) ?: return null
    val runContentDescriptor = RunContentBuilder(executionResult, environment).showRunContent(environment.contentToReuse) ?: return null

    if (state is ImportedTestRunnableState) {
      return runContentDescriptor
    }

    (state as ExternalSystemRunConfiguration.MyRunnableState).setContentDescriptor(runContentDescriptor)
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
