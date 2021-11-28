/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

enum class KotlinCompilerExecutionStrategy(
    /**
     * Value that should be passed for `kotlin.compiler.execution.strategy` Gradle or system property to choose the strategy
     */
    val propertyValue: String
) {
    /**
     * Execute Kotlin compiler in its own daemon. Default strategy.
     *
     * Daemon may be shared across multiple compile tasks if it's considered compatible
     */
    DAEMON("daemon"),

    /**
     * Execute Kotlin compiler inside the Gradle process
     *
     * Note: currently this strategy doesn't support incremental compilation
     */
    IN_PROCESS("in-process"),

    /**
     * Execute Kotlin compiler in a new forked process for each compilation
     *
     * Note: currently this strategy doesn't support incremental compilation
     */
    OUT_OF_PROCESS("out-of-process"),
    ;

    companion object {
        fun fromProperty(value: String?) =
            if (value == null) {
                DAEMON
            } else {
                values().find { it.propertyValue.equals(value, ignoreCase = true) }
                    ?: error("Unknown value '$value' is passed for Kotlin compiler execution strategy")
            }
    }
}

/**
 * Task is using Kotlin daemon to run compilation.
 */
interface CompileUsingKotlinDaemon : Task {
    /**
     * Provides JVM arguments to Kotlin daemon, default is `null` if "kotlin.daemon.jvmargs" property is not set.
     */
    @get:Internal
    val kotlinDaemonJvmArguments: ListProperty<String>

    /**
     * Defines compiler execution strategy, see docs for [KotlinCompilerExecutionStrategy] for more details
     */
    @get:Internal
    val compilerExecutionStrategy: Property<KotlinCompilerExecutionStrategy>
}
