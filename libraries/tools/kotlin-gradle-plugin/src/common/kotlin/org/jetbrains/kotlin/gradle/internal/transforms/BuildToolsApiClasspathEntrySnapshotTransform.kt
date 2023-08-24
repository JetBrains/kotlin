/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.transforms

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.compilerRunner.btapi.SharedApiClassesClassLoaderProvider
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import java.io.File

/** Transform to create a snapshot of a classpath entry (directory or jar). */
@CacheableTransform
@OptIn(ExperimentalBuildToolsApi::class)
abstract class BuildToolsApiClasspathEntrySnapshotTransform : TransformAction<BuildToolsApiClasspathEntrySnapshotTransform.Parameters> {

    abstract class Parameters : TransformParameters {
        @get:Internal
        abstract val gradleUserHomeDir: DirectoryProperty

        @get:Internal
        internal abstract val classLoadersCachingService: Property<ClassLoadersCachingBuildService>

        @get:Classpath
        internal abstract val classpath: ConfigurableFileCollection
    }

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val classpathEntryInputDirOrJar = inputArtifact.get().asFile
        val snapshotOutputFile = outputs.file(classpathEntryInputDirOrJar.name.replace('.', '_') + "-snapshot.bin")

        val granularity = getClassSnapshotGranularity(classpathEntryInputDirOrJar, parameters.gradleUserHomeDir.get().asFile)

        try {
            val classLoader = parameters.classLoadersCachingService.get()
                .getClassLoader(parameters.classpath.toList(), SharedApiClassesClassLoaderProvider)
            val compilationService = CompilationService.loadImplementation(classLoader)
            val snapshot = compilationService.calculateClasspathSnapshot(classpathEntryInputDirOrJar, granularity)
            snapshot.saveSnapshot(snapshotOutputFile)
        } catch (e: Throwable) {
            throw e
        }
    }

    /**
     * Determines the [ClassSnapshotGranularity] when taking a snapshot of the given [classpathEntryDirOrJar].
     *
     * As mentioned in [ClassSnapshotGranularity]'s kdoc, we will take [CLASS_LEVEL] snapshots for classes that are infrequently changed
     * (e.g., external libraries which are typically stored/transformed inside the Gradle user home, or a few hard-coded cases), and take
     * [CLASS_MEMBER_LEVEL] snapshots for the others.
     */
    private fun getClassSnapshotGranularity(classpathEntryDirOrJar: File, gradleUserHomeDir: File): ClassSnapshotGranularity {
        return if (
            classpathEntryDirOrJar.startsWith(gradleUserHomeDir) ||
            classpathEntryDirOrJar.name == "android.jar"
        ) CLASS_LEVEL
        else CLASS_MEMBER_LEVEL
    }
}
