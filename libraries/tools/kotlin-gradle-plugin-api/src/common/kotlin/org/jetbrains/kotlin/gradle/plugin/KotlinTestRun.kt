/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.TestFilter

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface KotlinTestRun<out SourceType : KotlinExecution.ExecutionSource> : KotlinExecution<SourceType> {
    fun filter(configureFilter: TestFilter.() -> Unit)

    fun filter(configureFilter: Closure<*>)
}

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface KotlinTargetTestRun<ExecutionSource : KotlinExecution.ExecutionSource> :
    KotlinTestRun<ExecutionSource>,
    KotlinTargetExecution<ExecutionSource>

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface JvmClasspathTestRunSource : KotlinExecution.ExecutionSource {
    val testClassesDirs: FileCollection
    val classpath: FileCollection
}

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface ClasspathTestRunSourceSupport {
    /**
     * Select the exact [classpath] to run the tests from.
     *
     * Only the classes from [testClasses] will be treated as tests.
     *
     * This overrides other [KotlinExecution.executionSource] selection options.
     */
    fun setExecutionSourceFrom(classpath: FileCollection, testClassesDirs: FileCollection)
}
