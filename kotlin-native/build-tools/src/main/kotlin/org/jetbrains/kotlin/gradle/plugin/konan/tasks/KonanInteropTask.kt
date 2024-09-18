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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.AbstractToolConfig
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty
import javax.inject.Inject

private val load0 = Runtime::class.java.getDeclaredMethod("load0", Class::class.java, String::class.java).also {
    it.isAccessible = true
}

private abstract class KonanInteropInProcessAction @Inject constructor() : WorkAction<KonanInteropInProcessAction.Parameters> {
    interface Parameters : WorkParameters {
        val isolatedClassLoadersService: Property<KonanCliRunnerIsolatedClassLoadersService>
        val compilerDistribution: NativeDistributionProperty
        val target: Property<String>
        val args: ListProperty<String>
    }

    override fun execute() {
        val dist = parameters.compilerDistribution.get()
        object : AbstractToolConfig(dist.root.asFile.absolutePath, parameters.target.get(), emptyMap()) {
            override fun loadLibclang() {
                // Load libclang into the system class loader. This is needed to allow developers to make changes
                // in the tooling infrastructure without having to stop the daemon (otherwise libclang might end up
                // loaded in two different class loaders which is not allowed by the JVM).
                load0.invoke(Runtime.getRuntime(), String::class.java, libclang)
            }
        }.prepare()
        KonanCliRunner(
                parameters.isolatedClassLoadersService.get().getClassLoader(parameters.compilerDistribution.get().compilerClasspath.files),
                useArgFile = false,
                toolName = "cinterop",
        ).run(parameters.args.get())
    }
}


private abstract class KonanInteropOutOfProcessAction @Inject constructor(
        private val execOperations: ExecOperations,
) : WorkAction<KonanInteropOutOfProcessAction.Parameters> {
    interface Parameters : WorkParameters {
        val compilerDistribution: NativeDistributionProperty
        val args: ListProperty<String>
    }

    override fun execute() {
        val cinterop = parameters.compilerDistribution.get().cinterop
        execOperations.exec {
            if (PlatformInfo.isWindows()) {
                commandLine("cmd.exe", "/d", "/c", cinterop, *parameters.args.get().toTypedArray())
            } else {
                commandLine(cinterop, *parameters.args.get().toTypedArray())
            }
        }.assertNormalExitValue()
    }
}

/**
 * A task executing cinterop tool with the given args and compiling the stubs produced by this tool.
 */
abstract class KonanInteropTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
        private val layout: ProjectLayout,
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

    @get:Internal
    val compilerDistribution: NativeDistributionProperty = objectFactory.nativeDistributionProperty()

    @get:Input
    val compilerDistributionPath: Provider<String> = compilerDistribution.map { it.root.asFile.absolutePath }

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    private val allowRunningCInteropInProcess = project.kotlinBuildProperties.getBoolean("kotlin.native.allowRunningCinteropInProcess")

    @TaskAction
    fun run() {
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

            add("-Xproject-dir")
            add(layout.projectDirectory.asFile.absolutePath)
        }
        val workQueue = workerExecutor.noIsolation()

        if (allowRunningCInteropInProcess) {
            workQueue.submit(KonanInteropInProcessAction::class.java) {
                this.isolatedClassLoadersService.set(this@KonanInteropTask.isolatedClassLoadersService)
                this.compilerDistribution.set(this@KonanInteropTask.compilerDistribution)
                this.target.set(konanTarget.map { it.visibleName })
                this.args.addAll(args)
            }
        } else {
            workQueue.submit(KonanInteropOutOfProcessAction::class.java) {
                this.compilerDistribution.set(this@KonanInteropTask.compilerDistribution)
                this.args.addAll(args)
            }
        }
    }
}