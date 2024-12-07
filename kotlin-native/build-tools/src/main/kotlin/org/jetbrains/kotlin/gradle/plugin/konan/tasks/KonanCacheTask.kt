/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliRunnerIsolatedClassLoadersService
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.gradle.plugin.konan.registerIsolatedClassLoadersServiceIfAbsent
import org.jetbrains.kotlin.gradle.plugin.konan.runKonanTool
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty
import javax.inject.Inject

private abstract class KonanCacheAction : WorkAction<KonanCacheAction.Parameters> {
    interface Parameters : WorkParameters {
        val isolatedClassLoaderService: Property<KonanCliRunnerIsolatedClassLoadersService>
        val compilerClasspath: ConfigurableFileCollection
        val args: ListProperty<String>
    }

    override fun execute() = with(parameters) {
        isolatedClassLoaderService.get().getClassLoader(compilerClasspath.files).runKonanTool(
                toolName = "konanc",
                args = args.get(),
                useArgFile = true,
        )
    }
}

@CacheableTask
open class KonanCacheTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val klib: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    val target: Property<String> = objectFactory.property(String::class.java)

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:Internal("Depends upon the compiler classpath, native libraries (used by codegen) and konan.properties (compilation flags + dependencies)")
    val compilerDistribution: NativeDistributionProperty = objectFactory.nativeDistributionProperty()

    @get:Classpath
    protected val compilerClasspath = compilerDistribution.map { it.compilerClasspath }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val codegenLibs = compilerDistribution.map { it.nativeLibs }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val konanProperties = compilerDistribution.map { it.konanProperties }

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    @TaskAction
    fun compile() {
        outputDirectory.get().asFile.prepareAsOutput()

        val args = buildList {
            add("-g")
            add("-target")
            add(target.get())
            add("-produce")
            add("static_cache")
            add("-Xadd-cache=${klib.get().asFile.absolutePath}")
            add("-Xcache-directory=${outputDirectory.get().asFile.parentFile.absolutePath}")
            PlatformManager(compilerDistribution.get().root.asFile.absolutePath).apply {
                addAll(platform(targetByName(target.get())).additionalCacheFlags)
            }
        }
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanCacheAction::class.java) {
            this.isolatedClassLoaderService.set(this@KonanCacheTask.isolatedClassLoadersService)
            this.compilerClasspath.from(this@KonanCacheTask.compilerClasspath)
            this.args.addAll(args)
        }
    }
}
