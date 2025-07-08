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
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliRunnerIsolatedClassLoadersService
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.gradle.plugin.konan.registerIsolatedClassLoadersServiceIfAbsent
import org.jetbrains.kotlin.gradle.plugin.konan.runKonanTool
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.nativeDistribution.BuiltNativeDistribution
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
        private val objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    open class Dependency @Inject constructor(
            objectFactory: ObjectFactory,
    ) {
        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val klib: DirectoryProperty = objectFactory.directoryProperty()

        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val cache: DirectoryProperty = objectFactory.directoryProperty()
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val klib: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    val target: Property<String> = objectFactory.property(String::class.java)

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:Nested
    val compilerDistribution: Property<BuiltNativeDistribution> = objectFactory.property(BuiltNativeDistribution::class)

    @get:Nested
    protected val dependencies = objectFactory.listProperty(Dependency::class)

    fun dependency(configure: Dependency.() -> Unit) {
        dependencies.add(objectFactory.newInstance<Dependency>().apply { configure() })
    }

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
            dependencies.get().forEach {
                val klib = it.klib.get().asFile.absolutePath
                add("-l")
                add(klib)
                add("-Xcached-library=$klib,${it.cache.get().asFile.absolutePath}")
            }
            PlatformManager(compilerDistribution.get().dist.root.asFile.absolutePath).apply {
                addAll(platform(targetByName(target.get())).additionalCacheFlags)
            }
        }
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanCacheAction::class.java) {
            this.isolatedClassLoaderService.set(this@KonanCacheTask.isolatedClassLoadersService)
            this.compilerClasspath.from(this@KonanCacheTask.compilerDistribution.get().dist.compilerClasspath)
            this.args.addAll(args)
        }
    }
}
