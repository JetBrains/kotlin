/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import kotlinBuildProperties
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * A task executing cinterop tool with the given args and compiling the stubs produced by this tool.
 */
abstract class KonanInteropTask @Inject constructor(
        private val workerExecutor: WorkerExecutor,
        private val layout: ProjectLayout,
        private val fileOperations: FileOperations,
        private val execOperations: ExecOperations,
): DefaultTask() {
    @get:Input
    abstract val konanTarget: Property<KonanTarget>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val klibFiles: ConfigurableFileCollection

    @get:Input
    abstract val extraOpts: ListProperty<String>

    @get:InputFile
    abstract val defFile: RegularFileProperty

    @get:Input
    abstract val compilerOpts: ListProperty<String>

    @get:Input
    abstract val compilerDistributionPath: Property<String>

    internal interface RunToolParameters: WorkParameters {
        var taskName: String
        var args: List<String>
    }

    internal abstract class RunTool @Inject constructor() : WorkAction<RunToolParameters> {
        override fun execute() {
            val toolRunner = interchangeBox.remove(parameters.taskName) ?: error(":(")
            toolRunner.run(parameters.args)
        }
    }

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    private val allowRunningCInteropInProcess = project.kotlinBuildProperties.getBoolean("kotlin.native.allowRunningCinteropInProcess")

    @TaskAction
    fun run() {
        val interopRunner = KonanCliInteropRunner(
                fileOperations,
                execOperations,
                logger,
                layout,
                isolatedClassLoadersService.get(),
                compilerDistributionPath.get(),
                konanTarget.get(),
                allowRunningCInteropInProcess
        )

        outputDirectory.get().asFile.prepareAsOutput()

        val args = buildList {
            add("-nopack")
            add("-o")
            add(outputDirectory.asFile.get().canonicalPath)
            add("-target")
            add(konanTarget.get().visibleName)
            add("-def")
            add(defFile.asFile.get().canonicalPath)

            compilerOpts.get().forEach {
                add("-compiler-option")
                add(it)
            }

            klibFiles.forEach {
                add("-library")
                add(it.canonicalPath)
            }

            addAll(extraOpts.get())
        }
        val workQueue = workerExecutor.noIsolation()
        interchangeBox[this.path] = interopRunner
        workQueue.submit(RunTool::class.java) {
            taskName = path
            this.args = args
        }
    }
}

internal val interchangeBox = ConcurrentHashMap<String, KonanCliInteropRunner>()
