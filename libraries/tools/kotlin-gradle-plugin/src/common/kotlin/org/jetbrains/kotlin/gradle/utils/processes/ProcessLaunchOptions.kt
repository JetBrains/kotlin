/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import java.io.File

/**
 * Properties used to launch a process using [ExecAsyncHandle].
 *
 * A new instance can be created using [ObjectFactory] and [processLaunchOptions].
 */
interface ProcessLaunchOptions {

    /** The name of the executable to use. */
    val executable: Property<String>

    /** The working directory for the process. */
    val workingDir: DirectoryProperty

    /**
     * Custom environment variables to use when launching the process.
     *
     * If [includeSystemEnvironment] is enabled then these values will override
     * any system environment variables.
     *
     * Use [ProcessLaunchOptions.Companion.computeEnvironment] to compute the final environment
     * to use when launching the process.
     */
    val customEnvironment: MapProperty<String, String>

    /**
     * If `true` use the current System environment ([System.getenv]) when launching the external process.
     *
     * The values can be overridden using [customEnvironment].
     *
     * Defaults to `false`.
     */
    val includeSystemEnvironment: Property<Boolean>

    companion object {
        /**
         * Build a new [ProcessLaunchOptions] instance.
         */
        fun ObjectFactory.processLaunchOptions(
            block: ProcessLaunchOptions.() -> Unit = {},
        ): ProcessLaunchOptions =
            newInstance<ProcessLaunchOptions>()
                .apply {
                    workingDir.convention(directoryProperty().fileValue(File(".")))
                    includeSystemEnvironment.convention(false)
                    block()
                }

        /**
         * Compute the final environment to use when launching the process.
         *
         * **NOTE** Only use this function in the execution phase.
         * It calls [System.getenv], which if used in the configuration phase
         * Gradle will register _all_ environment variables as configuration cache inputs.
         */
        internal fun ProcessLaunchOptions.computeEnvironment(): Map<String, String?> =
            buildMap {
                if (includeSystemEnvironment.get()) {
                    putAll(System.getenv().mapValues { (_, v) -> v ?: "" })
                }
                putAll(
                    customEnvironment.orNull.orEmpty()
                )
            }

        internal fun ExecSpec.configure(options: ProcessLaunchOptions) {
            executable = options.executable.get()
            workingDir = options.workingDir.orNull?.asFile
            environment = options.computeEnvironment()
        }
    }
}
