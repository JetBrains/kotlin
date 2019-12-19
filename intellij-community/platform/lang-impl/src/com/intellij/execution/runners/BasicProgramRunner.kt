// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.ui.RunContentDescriptor
import org.jetbrains.concurrency.resolvedPromise

@Deprecated(message = "Do not use and do not extend. Use DefaultProgramRunner instead.", level = DeprecationLevel.ERROR)
abstract class BasicProgramRunner : ProgramRunner<RunnerSettings> {
  @Throws(ExecutionException::class)
  override fun execute(environment: ExecutionEnvironment, callback: ProgramRunner.Callback?) {
    val state = environment.state ?: return
    startRunProfile(environment, callback) {
      resolvedPromise(doExecute(state, environment))
    }
  }

  @Throws(ExecutionException::class)
  protected open fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    return executeState(state, environment, this)
  }

  override fun getRunnerId() = "Basic"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return DefaultRunExecutor.EXECUTOR_ID == executorId && profile !is RunConfigurationWithSuppressedDefaultRunAction
  }
}