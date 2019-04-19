/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.runners

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

private class DefaultRunProgramRunner : AsyncProgramRunner<RunnerSettings>() {
  override fun getRunnerId() = "defaultRunRunner"

  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    FileDocumentManager.getInstance().saveAllDocuments()
    @Suppress("IfThenToElvis")
    if (state is DebuggableRunProfileState) {
      return state.execute(-1)
        .then {
          it?.let {
            RunContentBuilder(it, environment).showRunContent(environment.contentToReuse)
          }
        }
    }
    else {
      return resolvedPromise(showRunContent(state.execute(environment.executor, this@DefaultRunProgramRunner), environment))
    }
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return DefaultRunExecutor.EXECUTOR_ID == executorId && profile !is RunConfigurationWithSuppressedDefaultRunAction
  }
}
