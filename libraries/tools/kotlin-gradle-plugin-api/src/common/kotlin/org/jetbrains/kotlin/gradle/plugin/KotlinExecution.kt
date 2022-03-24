/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution.ExecutionSource

/**
 * An execution of Kotlin code. Executions in a single family (e.g. test runs) are distinguished by [Named.getName].
 * Names may not be unique across different execution families, such as test runs of different targets.
 *
 * A particular kind of execution is a [KotlinTestRun] which runs tests.
 */
interface KotlinExecution<out SourceType : ExecutionSource> : Named {
    interface ExecutionSource

    /**
     * The source of the executable code that this execution runs from. It is usually set via members of  execution source support
     * interfaces, such as [CompilationExecutionSourceSupport] or [ClasspathTestRunSourceSupport], or `setExecutionSourceFrom*` functions.
     */
    val executionSource: SourceType
}

interface KotlinTargetExecution<out SourceType : ExecutionSource> : KotlinExecution<SourceType> {
    val target: KotlinTarget
}

/**
 * An execution source that is produced by a [compilation].
 *
 * See also: [CompilationExecutionSourceSupport].
 */
interface CompilationExecutionSource<CompilationType : KotlinCompilation<*>> : ExecutionSource {
    val compilation: CompilationType
}

interface CompilationExecutionSourceSupport<in T : KotlinCompilationToRunnableFiles<*>> {
    /**
     * Select a compilation to run the execution from.
     *
     * The [compilation]'s [KotlinCompilationToRunnableFiles.runtimeDependencyFiles]
     * will be treated as runtime dependencies, and its [output] as runnable files.
     *
     * This overrides other [KotlinExecution.executionSource] selection options.
     */
    fun setExecutionSourceFrom(compilation: T)
}

interface ExecutionTaskHolder<T : Task> {
    val executionTask: TaskProvider<T>
}