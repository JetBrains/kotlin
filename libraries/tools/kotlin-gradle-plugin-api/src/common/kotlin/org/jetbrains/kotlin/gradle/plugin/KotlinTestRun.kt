/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.TestFilter

/**
 * A [KotlinExecution] that runs configured tests.
 */
interface KotlinTestRun<out SourceType : KotlinExecution.ExecutionSource> : KotlinExecution<SourceType> {

    /**
     * Configures filtering for executable tests using the provided [configureFilter] configuration.
     */
    fun filter(configureFilter: TestFilter.() -> Unit)

    /**
     * Configures filtering for executable tests using the provided [configureFilter] configuration.
     */
    fun filter(configureFilter: Closure<*>)
}

/**
 * A [KotlinTargetExecution] that executes configured tests in the context of a specific [KotlinTarget].
 */
interface KotlinTargetTestRun<ExecutionSource : KotlinExecution.ExecutionSource> :
    KotlinTestRun<ExecutionSource>,
    KotlinTargetExecution<ExecutionSource>

/**
 * A [KotlinExecution.ExecutionSource] that provides the [classpath] and [testClassesDirs] where JVM test classes can be found.
 */
interface JvmClasspathTestRunSource : KotlinExecution.ExecutionSource {

    /**
     * The part of the classpath where JVM test classes are located for execution.
     */
    val testClassesDirs: FileCollection

    /**
     * The tests classpath.
     *
     * This includes dependencies and/or test framework classes such as JUnit, TestNG, or any other test framework classes.
     */
    val classpath: FileCollection
}

/**
 * Configures the [JvmClasspathTestRunSource].
 */
interface ClasspathTestRunSourceSupport {

    /**
     * Configures values for the [JvmClasspathTestRunSource].
     *
     * Calling this method overwrites any already configured values in the [JvmClasspathTestRunSource].
     *
     * @param [classpath] Tests classpath
     * @param [testClassesDirs] Only the classes from this [FileCollection] are treated as tests.
     */
    fun setExecutionSourceFrom(classpath: FileCollection, testClassesDirs: FileCollection)
}
