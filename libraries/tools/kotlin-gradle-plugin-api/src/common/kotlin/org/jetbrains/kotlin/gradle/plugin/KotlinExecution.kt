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
 * Represents the execution of Kotlin code, such as tests.
 *
 * Executions within a single family, like test runs, are distinguished by [Named.getName].
 * Names don't have to be globally unique across different execution families.
 * For example, test runs of different targets can have the same name.
 *
 * [KotlinTestRun] is a specific type of execution that runs tests.
 */
interface KotlinExecution<out SourceType : ExecutionSource> : Named {

    /**
     * Represents an execution source that provides the necessary inputs to run the execution.
     */
    interface ExecutionSource

    /**
     * The source of the executable code that this execution runs.
     *
     * It is typically set via members of [ExecutionSource] support interfaces,
     * such as [CompilationExecutionSourceSupport] or [ClasspathTestRunSourceSupport].
     */
    val executionSource: SourceType
}

/**
 * Represents an execution in the scope of a [KotlinTarget].
 */
interface KotlinTargetExecution<out SourceType : ExecutionSource> : KotlinExecution<SourceType> {

    /**
     * The [KotlinTarget] that this execution belongs to.
     */
    val target: KotlinTarget
}

/**
 * An execution source produced by a [compilation].
 *
 * @see [CompilationExecutionSourceSupport].
 */
interface CompilationExecutionSource<CompilationType : KotlinCompilation<*>> : ExecutionSource {

    /**
     * The [KotlinCompilation] that this [ExecutionSource] belongs to.
     */
    val compilation: CompilationType
}

/**
 * Provides methods to set a [KotlinCompilation] as an [ExecutionSource].
 */
@Suppress("DEPRECATION_ERROR")
interface CompilationExecutionSourceSupport<in T : KotlinCompilationToRunnableFiles<*>> {

    /**
     * Selects the compilation to run the execution from.
     *
     * The [compilation]'s [KotlinCompilation.runtimeDependencyFiles]
     * are treated as runtime dependencies, while its [KotlinCompilation.output] is treated as runnable files.
     *
     * Calling this method overwrites an already configured [KotlinExecution.executionSource].
     */
    fun setExecutionSourceFrom(compilation: T)
}

/**
 * Provides a reference to the Gradle task that executes [KotlinExecution].
 */
interface ExecutionTaskHolder<T : Task> {

    /**
     * Provides the Gradle task that executes the [KotlinExecution].
     */
    val executionTask: TaskProvider<T>
}