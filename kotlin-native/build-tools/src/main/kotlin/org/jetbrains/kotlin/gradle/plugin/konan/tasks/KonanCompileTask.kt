/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliRunnerIsolatedClassLoadersService
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

/**
 * A task compiling the target library using Kotlin/Native compiler
 */
@CacheableTask
abstract class KonanCompileTask @Inject constructor(
        private val fileOperations: FileOperations,
        private val execOperations: ExecOperations,
        private val objectFactory: ObjectFactory,
) : DefaultTask() {
    init {
        this.notCompatibleWithConfigurationCache("project references stored")
    }

    // Changing the compiler version must rebuild the library.
    @get:Input
    protected val buildNumber = project.properties["kotlinVersion"] ?: error("kotlinVersion property is not specified in the project")

    @get:Input
    abstract val konanTarget: Property<KonanTarget>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val extraOpts: ListProperty<String>

    @get:Input
    abstract val compilerDistributionPath: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceSets: NamedDomainObjectContainer<SourceDirectorySet> = objectFactory.domainObjectContainer(SourceDirectorySet::class.java) {
        objectFactory.sourceDirectorySet(it, it).apply {
            filter.include("**/*.kt")
        }
    }

    @get:Internal
    val isolatedClassLoadersService = KonanCliRunnerIsolatedClassLoadersService.attachingToTask(this)

    @TaskAction
    fun run() {
        val toolRunner = KonanCliCompilerRunner(fileOperations, execOperations, logger, isolatedClassLoadersService, compilerDistributionPath.get())

        outputDirectory.asFile.get().mkdirs()
        val args = buildList {
            add("-nopack")
            add("-Xmulti-platform")
            add("-output")
            add(outputDirectory.asFile.get().canonicalPath)
            add("-produce")
            add("library")
            add("-target")
            add(konanTarget.get().visibleName)

            addAll(extraOpts.get())
            add(sourceSets.joinToString(",", prefix = "-Xfragments=") { it.name })

            val fragmentSources = sequence {
                for (s in sourceSets) {
                    for (f in s.files) {
                        yield("${s.name}:${f.absolutePath}")
                    }
                }
            }
            add(fragmentSources.joinToString(",", prefix="-Xfragment-sources="))

            sourceSets.flatMap { it.files }.mapTo(this) { it.absolutePath }
        }
        toolRunner.run(args)
    }
}
