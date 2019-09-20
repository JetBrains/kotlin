// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.openapi.externalSystem.model.task.TaskData
import org.jetbrains.plugins.gradle.execution.TaskOption.ArgumentType.CLASS
import org.jetbrains.plugins.gradle.execution.TaskOption.ArgumentType.NOTHING

class GradleCommandLineTaskOptionsProvider {
  fun getTaskOptions(task: TaskData) = when {
    task.isTest -> listOf(TaskOption("--tests", CLASS))
    task.name == "tasks" -> listOf(TaskOption("--all", NOTHING))
    else -> emptyList()
  }
}

class TaskOption(val name: String, val argumentTypes: Set<ArgumentType>) {
  enum class ArgumentType { NOTHING, CLASS }

  constructor(name: String, vararg argumentTypes: ArgumentType) : this(name, argumentTypes.toSet())
}