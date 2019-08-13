// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

abstract class DefaultProgramRunner : GenericProgramRunner<RunnerSettings>() {
  @Throws(ExecutionException::class)
  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    return executeState(state, environment, this)
  }
}

fun executeState(state: RunProfileState, env: ExecutionEnvironment, runner: ProgramRunner<*>): RunContentDescriptor? {
  FileDocumentManager.getInstance().saveAllDocuments()
  return showRunContent(state.execute(env.executor, runner), env)
}

fun showRunContent(executionResult: ExecutionResult?, environment: ExecutionEnvironment): RunContentDescriptor? {
  return executionResult?.let { RunContentBuilder(it, environment).showRunContent(environment.contentToReuse) }
}
