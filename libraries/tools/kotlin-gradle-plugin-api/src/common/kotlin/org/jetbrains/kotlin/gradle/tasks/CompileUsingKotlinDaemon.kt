/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

/**
 * The available Kotlin compilation execution strategies in Gradle.
 *
 * @property propertyValue value that should be passed for `kotlin.compiler.execution.strategy` Gradle property to choose the strategy
 */
enum class KotlinCompilerExecutionStrategy(
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

    /**
     * @suppress
     */
    companion object {
        /**
         * @suppress
         */
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
 * Represents a Kotlin task that uses the Kotlin daemon to compile.
 */
interface CompileUsingKotlinDaemon : Task {
    /**
     * Provides JVM arguments to the Kotlin daemon. The default is `null` if the `kotlin.daemon.jvmargs` property is not set.
     */
    @get:Internal
    val kotlinDaemonJvmArguments: ListProperty<String>

    /**
     * Defines the compiler execution strategy, see docs for [KotlinCompilerExecutionStrategy] for more details.
     *
     * @see [KotlinCompilerExecutionStrategy]
     */
    @get:Internal
    val compilerExecutionStrategy: Property<KotlinCompilerExecutionStrategy>

    /**
     * Defines whether task execution should fail when [compilerExecutionStrategy] is set to [KotlinCompilerExecutionStrategy.DAEMON]
     * and compilation via the Kotlin daemon is not possible. If set to true, then compilation is retried without the daemon.
     *
     * Default: `true`
     */
    @get:Internal
    val useDaemonFallbackStrategy: Property<Boolean>
}
