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
import org.jetbrains.kotlin.gradle.report.BuildMetricsReporterService
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity
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
        abstract val buildMetricsReporterService: Property<BuildMetricsReporterService>
    }

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val classpathEntryInputDirOrJar = inputArtifact.get().asFile
        val snapshotOutputFile = outputs.file(classpathEntryInputDirOrJar.name.replace('.', '_') + "-snapshot.bin")

        // There are two levels of granularity when taking a snapshot:
        //   - CLASS_LEVEL (coarse-grained): The size of the snapshot is smaller, but we will have coarse-grained classpath changes, which
        //     means more source files will be recompiled.
        //   - CLASS_MEMBER_LEVEL (fine-grained): The size of the snapshot is larger, but we will have fine-grained classpath changes, which
        //     means fewer source files will be recompiled.
        // Therefore, CLASS_LEVEL is suitable for classes that are infrequently changed (e.g., external libraries which are typically
        // stored/transformed inside the Gradle user home, plus a few hard-coded cases below), whereas CLASS_MEMBER_LEVEL is suitable for
        // classes that are frequently changed (e.g., classes produced by the current project).
        val granularity = if (
            classpathEntryInputDirOrJar.startsWith(parameters.gradleUserHomeDir.get().asFile) ||
            classpathEntryInputDirOrJar.name == "android.jar" ||
            classpathEntryInputDirOrJar.name.startsWith("kotlin-compiler-embeddable")
        ) {
            ClassSnapshotGranularity.CLASS_LEVEL
        } else {
            ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
        }
        val buildMetricsReporterService = parameters.buildMetricsReporterService.orNull

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

    private fun doTransform(
        classpathEntryInputDirOrJar: File, snapshotOutputFile: File,
        granularity: ClassSnapshotGranularity, metrics: BuildMetricsReporter
    ) {
        metrics.measure(BuildTime.CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM) {
            val snapshot = ClasspathEntrySnapshotter.snapshot(classpathEntryInputDirOrJar, granularity, metrics)
            metrics.measure(BuildTime.SAVE_CLASSPATH_ENTRY_SNAPSHOT) {
                ClasspathEntrySnapshotExternalizer.saveToFile(snapshotOutputFile, snapshot)
            }
        }
        metrics.addMetric(BuildPerformanceMetric.CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, 1)
        metrics.addMetric(BuildPerformanceMetric.CLASSPATH_ENTRY_SIZE, classpathEntryInputDirOrJar.walk().sumOf { it.length() })
        metrics.addMetric(BuildPerformanceMetric.CLASSPATH_ENTRY_SNAPSHOT_SIZE, snapshotOutputFile.length())
    }
}
