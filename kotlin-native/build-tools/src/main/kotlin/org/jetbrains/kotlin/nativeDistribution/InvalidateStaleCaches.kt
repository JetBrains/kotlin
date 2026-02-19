/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import kotlinBuildProperties
import org.gradle.api.DefaultTask
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

@DisableCachingByDefault(because = "only cleans up inside the output directory")
open class InvalidateStaleCaches @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations,
) : DefaultTask() {
    /**
     * compiler-cache-invalidator tool
     */
    @get:Classpath
    protected val tool = objectFactory.fileCollection().apply {
        from(project.configurations.detachedConfiguration(project.dependencies.project(":kotlin-native:tools:compiler-cache-invalidator").apply {
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objectFactory.named(LibraryElements.JAR))
                attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.JAVA_RUNTIME))
            }
        }))
    }

    /**
     * Distribution in which to clean stale caches
     */
    @get:Internal("This task uses only some parts of the Native distribution")
    val distributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    private val distribution = distributionRoot.asNativeDistribution()

    /**
     * Path to the data directory with Native dependencies.
     */
    @get:Input
    @get:Optional
    val dataDirPath = objectFactory.property(String::class).convention(project.kotlinBuildProperties.stringProperty("konan.data.dir").orNull)

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("UNUSED") // Used by Gradle via reflection
    protected val compilerFingerprint = distribution.map { it.compilerFingerprint }

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("UNUSED") // Used by Gradle via reflection
    protected val runtimeFingerprints = objectFactory.fileCollection().apply {
        KonanTarget.predefinedTargets.values.forEach { target ->
            from(distribution.map { it.runtimeFingerprint(target.name) })
        }
    }

    /**
     * Root directory of caches, some of which will be deleted by the tool.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty // The directory may not yet exist (no directory - nothing to clean up)
    @Suppress("UNUSED") // Used by Gradle via reflection
    protected val cachesRoot = distribution.map { it.cachesRoot }

    @get:OutputDirectory
    @Suppress("UNUSED") // Used by Gradle via reflection
    protected val cachesRootAsOutput
        get() = cachesRoot

    @TaskAction
    fun run() {
        execOperations.javaexec {
            classpath(tool)
            mainClass.set("org.jetbrains.kotlin.nativecacheinvalidator.cli.NativeCacheInvalidatorCLI")
            args("--dist=${distribution.get().root.asFile.absolutePath}")
            dataDirPath.orNull?.also {
                args("--data-dir=$it")
            }
            if (logger.isInfoEnabled) {
                args("-v")
            }
        }
    }
}