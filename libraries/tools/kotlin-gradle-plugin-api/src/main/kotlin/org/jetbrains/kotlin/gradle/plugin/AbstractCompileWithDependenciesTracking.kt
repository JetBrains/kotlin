package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.tasks.compile.AbstractCompile

abstract class AbstractCompileWithDependenciesTracking : AbstractCompile() {
    open fun isDependentTaskOutOfDate(task: Task): Boolean = false
}

