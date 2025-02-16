/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
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
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty
import java.io.File
import javax.inject.Inject

private abstract class KonanCompileAction : WorkAction<KonanCompileAction.Parameters> {
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

open class KotlinSourceDirectorySet @Inject constructor(
        private val _name: String,
        objectFactory: ObjectFactory,
        private val layout: ProjectLayout,
) : Named {
    @Input
    override fun getName() = _name

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // manually computed: relativePaths
    protected val sources: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val relativePaths = sources.elements.map { files ->
        val base = layout.projectDirectory
        files.map {
            it.asFile.toRelativeString(base.asFile)
        }
    }

    /**
     * Add source directory. Accepts the same format as [org.gradle.api.Project.files].
     */
    fun srcDir(dir: Any) {
        sources.from(layout.files(dir).asFileTree.matching {
            include("**/*.kt")
        })
    }

    @get:Internal("handled by sources")
    internal val files: Set<File>
        get() = sources.files
}

/**
 * A task compiling the target library using Kotlin/Native compiler
 */
@CacheableTask
open class KonanCompileTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    val extraOpts: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @get:Internal("Depends only upon the compiler classpath, because compiles into klib only")
    val compilerDistribution: NativeDistributionProperty = objectFactory.nativeDistributionProperty()

    @get:Classpath
    protected val compilerClasspath = compilerDistribution.map { it.compilerClasspath }

    @get:Nested
    val sourceSets: NamedDomainObjectContainer<KotlinSourceDirectorySet> = objectFactory.domainObjectContainer(KotlinSourceDirectorySet::class.java)

    @get:ServiceReference
    protected val isolatedClassLoadersService = project.gradle.sharedServices.registerIsolatedClassLoadersServiceIfAbsent()

    @TaskAction
    fun run() {
        outputDirectory.get().asFile.prepareAsOutput()

        val args = buildList {
            add("-nopack")
            add("-Xmulti-platform")
            add("-output")
            add(outputDirectory.asFile.get().canonicalPath)
            add("-produce")
            add("library")

            addAll(extraOpts.get())
            add(sourceSets.joinToString(",", prefix = "-Xfragments=") { it.name })

            val fragmentSources = sequence {
                for (s in sourceSets) {
                    for (f in s.files) {
                        yield("${s.name}:${f.absolutePath}")
                    }
                }
            }
            add(fragmentSources.joinToString(",", prefix = "-Xfragment-sources="))

            sourceSets.flatMap { it.files }.mapTo(this) { it.absolutePath }
        }

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanCompileAction::class.java) {
            this.isolatedClassLoaderService.set(this@KonanCompileTask.isolatedClassLoadersService)
            this.compilerClasspath.from(this@KonanCompileTask.compilerClasspath)
            this.args.addAll(args)
        }
    }
}
