/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService

/**
 * A parent class for all tasks that use Application Binary Interace (ABI) tools.
 */
@DisableCachingByDefault(because = "Abstract task")
internal abstract class AbiToolsTask : DefaultTask(), UsesClassLoadersCachingBuildService {
    @get:Classpath
    abstract val toolsClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        val files = toolsClasspath.files.toList()
        // get class loader with the ABI Tools implementation from cache
        val classLoader = classLoadersCachingService.get().getClassLoader(files, SharedClassLoaderProvider)
        // get an instance of the ABI Tools and invoke task logic
        val abiTools = AbiTools.getInstance(classLoader)
        runTools(abiTools)
    }

    protected abstract fun runTools(tools: AbiTools)

}
