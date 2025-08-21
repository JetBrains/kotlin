/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
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
     * The environment variables to use when launching the process.
     *
     * Note that the processes may be launched using
     * [org.gradle.process.ExecOperations].
     * If so, then when no environment variables are provided then
     * [org.gradle.process.ExecOperations]
     * will copy all values from [System.getenv].
     * Otherwise, if _any_ environment variables are set, then _no_ values from
     * [System.getenv] be used.
     * This may result in the `PATH` environment variable being unset.
     */
    val environment: MapProperty<String, String>

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
                    block()
                }
    }
}
