/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.transforms

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.io.File

/** Transform to create a snapshot of a classpath entry (directory or jar). */
@CacheableTransform
abstract class ClasspathEntrySnapshotTransform : TransformAction<ClasspathEntrySnapshotTransform.Parameters> {

    abstract class Parameters : TransformParameters {
        @get:Internal
        abstract val gradleUserHomeDir: DirectoryProperty

        @get:Internal
        abstract val buildMetricsService: Property<BuildMetricsService>
    }

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val classpathEntryInputDirOrJar = inputArtifact.get().asFile
        val snapshotOutputFile = outputs.file(classpathEntryInputDirOrJar.name.replace('.', '_') + "-snapshot.bin")

        val granularity = getClassSnapshotGranularity(classpathEntryInputDirOrJar, parameters.gradleUserHomeDir.get().asFile)

        val buildMetricsReporterService = parameters.buildMetricsService.orNull
        val metricsReporter = buildMetricsReporterService?.let { BuildMetricsReporterImpl() } ?: DoNothingBuildMetricsReporter

        val startTimeMs = System.currentTimeMillis()
        var failureMessage: String? = null
        try {
            doTransform(classpathEntryInputDirOrJar, snapshotOutputFile, granularity, metricsReporter)
        } catch (e: Throwable) {
            failureMessage = e.message
            throw e
        } finally {
            buildMetricsReporterService?.addTransformMetrics(
                transformPath = "${ClasspathEntrySnapshotTransform::class.simpleName} for ${classpathEntryInputDirOrJar.path}",
                transformClass = ClasspathEntrySnapshotTransform::class.java,
                isKotlinTransform = true,
                startTimeMs = startTimeMs,
                totalTimeMs = System.currentTimeMillis() - startTimeMs,
                buildMetrics = metricsReporter.getMetrics(),
                failureMessage = failureMessage
            )
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

    private fun doTransform(
        classpathEntryInputDirOrJar: File, snapshotOutputFile: File,
        granularity: ClassSnapshotGranularity, metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
    ) {
        metrics.measure(GradleBuildTime.CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM) {
            val snapshot = ClasspathEntrySnapshotter.snapshot(classpathEntryInputDirOrJar, granularity, metrics)
            metrics.measure(GradleBuildTime.SAVE_CLASSPATH_ENTRY_SNAPSHOT) {
                ClasspathEntrySnapshotExternalizer.saveToFile(snapshotOutputFile, snapshot)
            }
        }

        metrics.addMetric(GradleBuildPerformanceMetric.CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, 1)
        if (classpathEntryInputDirOrJar.extension.equals("jar", ignoreCase = true)) {
            metrics.addMetric(GradleBuildPerformanceMetric.JAR_CLASSPATH_ENTRY_SIZE, classpathEntryInputDirOrJar.length())
            metrics.addMetric(GradleBuildPerformanceMetric.JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE, snapshotOutputFile.length())
        } else {
            // Only compute the size of the snapshot, not the size of the input directory as walking the file tree has a small overhead
            metrics.addMetric(GradleBuildPerformanceMetric.DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE, snapshotOutputFile.length())
        }
    }
}
