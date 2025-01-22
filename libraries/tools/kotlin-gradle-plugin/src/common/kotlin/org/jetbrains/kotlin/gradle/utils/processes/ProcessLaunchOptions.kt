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
 * Properties used to launch a process using [ExecHandle].
 *
 * A new instance can be created using [ObjectFactory] and [processLaunchOptions].
 */
interface ProcessLaunchOptions {

    /** The name of the executable to use. */
    val executable: Property<String>

    /** The working directory for the process. */
    val workingDir: DirectoryProperty

    /** The environment variable to use for the process. */
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
                    environment.convention(System.getenv())
                    block()
                }
    }
}
