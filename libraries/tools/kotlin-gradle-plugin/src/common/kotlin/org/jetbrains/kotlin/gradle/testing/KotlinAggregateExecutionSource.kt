/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import groovy.lang.Closure
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.execution.KotlinAggregatingExecution
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution.ExecutionSource
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface KotlinAggregatingTestRun<S : ExecutionSource, A : KotlinAggregateExecutionSource<S>, T : KotlinTestRun<S>> :
    KotlinAggregatingExecution<S, A, T>,
    KotlinTestRun<A> {

    override fun filter(configureFilter: TestFilter.() -> Unit) {
        configureAllExecutions { filter(configureFilter) }
    }
}

abstract class KotlinTaskTestRun<S : ExecutionSource, T : AbstractTestTask>(
    private val testRunName: String,
    override val target: KotlinTarget
) : KotlinTargetTestRun<S>, ExecutionTaskHolder<T> {

    override fun getName(): String =
        testRunName

    override lateinit var executionTask: TaskProvider<T>
        internal set

    override fun filter(configureFilter: TestFilter.() -> Unit) {
        executionTask.configure { task -> configureFilter(task.filter) }
    }

    override fun filter(configureFilter: Closure<*>) = filter { target.project.configure(this, configureFilter) }
}

internal val KotlinTargetTestRun<*>.testTaskName: String
    get() = lowerCamelCaseName(
        target.disambiguationClassifier,
        name.takeIf { it != KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME }?.removeSuffix("Test"),
        AbstractKotlinTargetConfigurator.testTaskNameSuffix
    )

internal fun requireCompilationOfTarget(compilation: KotlinCompilation<*>, target: KotlinTarget) {
    require(compilation.target === target) {
        "Expected a compilation of target ${target.name}, " +
                "got the compilation ${compilation.name} of target ${compilation.target.name}"
    }
}

abstract class KotlinReportAggregatingTestRun<E : ExecutionSource, A : KotlinAggregateExecutionSource<E>, T : KotlinTestRun<E>>(
    val testRunName: String
) : KotlinAggregatingTestRun<E, A, T>,
    ExecutionTaskHolder<KotlinTestReport> {

    override fun getName() = testRunName

    override lateinit var executionTask: TaskProvider<KotlinTestReport>

    override fun filter(configureFilter: TestFilter.() -> Unit) = configureAllExecutions { filter(configureFilter) }
}