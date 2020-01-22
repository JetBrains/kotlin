// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.concurrency.resolvedPromise

abstract class DefaultProgramRunner : ProgramRunner<RunnerSettings> {
  @Throws(ExecutionException::class)
  final override fun execute(environment: ExecutionEnvironment, callback: ProgramRunner.Callback?) {
    val state = environment.state ?: return
    ExecutionManager.getInstance(environment.project).startRunProfile(environment, callback, {
      resolvedPromise(doExecute(state, environment))
    })
  }

  @Throws(ExecutionException::class)
  protected open fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    return executeState(state, environment, this)
  }
}

fun executeState(state: RunProfileState, environment: ExecutionEnvironment, runner: ProgramRunner<*>): RunContentDescriptor? {
  FileDocumentManager.getInstance().saveAllDocuments()
  return showRunContent(state.execute(environment.executor, runner), environment)
}

fun showRunContent(executionResult: ExecutionResult?, environment: ExecutionEnvironment): RunContentDescriptor? {
  return executionResult?.let { RunContentBuilder(it, environment).showRunContent(environment.contentToReuse) }
}
