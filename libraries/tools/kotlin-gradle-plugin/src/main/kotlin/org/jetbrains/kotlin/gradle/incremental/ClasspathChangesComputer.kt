/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.incremental.*
import java.io.File
import org.jetbrains.kotlin.gradle.incremental.ChangesCollectorResult.Success
import org.jetbrains.kotlin.gradle.incremental.ChangesCollectorResult.Failure
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import java.util.*

/** Computes [ClasspathChanges] between two [ClasspathSnapshot]s .*/
object ClasspathChangesComputer {

    fun getChanges(current: ClasspathSnapshot, previous: ClasspathSnapshot): ClasspathChanges {
        val changesCollector = ChangesCollector()
        return when (collectClasspathChanges(current, previous, changesCollector)) {
            Success -> {
                val (lookupSymbols, fqNames, _) = changesCollector.getDirtyData(emptyList(), NoOpBuildReporter)
                ClasspathChanges.Available(lookupSymbols.toList(), fqNames.toList())
            }
            is Failure -> ClasspathChanges.NotAvailable.UnableToCompute
        }
    }

    private fun collectClasspathChanges(
        current: ClasspathSnapshot,
        previous: ClasspathSnapshot,
        changesCollector: ChangesCollector
    ): ChangesCollectorResult {
        if (current.classpathEntrySnapshots.size != previous.classpathEntrySnapshots.size) {
            return Failure.AddedRemovedClasspathEntries
        }

        for (index in current.classpathEntrySnapshots.indices) {
            val result = collectClasspathEntryChanges(
                current.classpathEntrySnapshots[index],
                previous.classpathEntrySnapshots[index],
                changesCollector
            )
            if (result is Failure) {
                return result
            }
        }
        return Success
    }

    private fun collectClasspathEntryChanges(
        current: ClasspathEntrySnapshot,
        previous: ClasspathEntrySnapshot,
        changesCollector: ChangesCollector
    ): ChangesCollectorResult {
        if (current.classSnapshots.size != previous.classSnapshots.size) {
            return Failure.AddedRemovedClasses
        }

        for (key in current.classSnapshots.keys) {
            val currentSnapshot = current.classSnapshots[key]!!
            val previousSnapshot = previous.classSnapshots[key] ?: return Failure.AddedRemovedClasses
            if (currentSnapshot !is KotlinClassSnapshot || previousSnapshot !is KotlinClassSnapshot) {
                return Failure.NotYetImplemented
            }
            // TODO: Store results in changesCollector
            collectKotlinClassChanges(currentSnapshot, previousSnapshot)
        }
        return Success
    }

    @TestOnly
    internal fun collectKotlinClassChanges(current: KotlinClassSnapshot, previous: KotlinClassSnapshot): DirtyData {
        // TODO Create IncrementalJvmCache early once and reuse it here
        val workingDir =
            FileUtil.createTempDirectory(this::class.java.simpleName, "_WorkingDir_${UUID.randomUUID()}", /* deleteOnExit */ true)
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToCanonicalPathConverter)

        // Store previous snapshot in incrementalJvmCache, the returned ChangesCollector result is not used.
        incrementalJvmCache.saveClassToCache(
            kotlinClassInfo = previous.classInfo,
            sourceFiles = null,
            changesCollector = ChangesCollector()
        )

        // Compute changes between the current snapshot and the previously stored snapshot, and store the result in changesCollector.
        val changesCollector = ChangesCollector()
        incrementalJvmCache.saveClassToCache(
            kotlinClassInfo = current.classInfo,
            sourceFiles = null,
            changesCollector = changesCollector
        )

        workingDir.deleteRecursively()
        return changesCollector.getDirtyData(listOf(incrementalJvmCache), NoOpBuildReporter.NoOpICReporter)
    }
}

private sealed class ChangesCollectorResult {

    object Success : ChangesCollectorResult()

    sealed class Failure : ChangesCollectorResult() {
        // TODO: Handle these cases
        object AddedRemovedClasspathEntries : Failure()
        object AddedRemovedClasses : Failure()
        object NotYetImplemented : Failure()
    }
}

private object NoOpBuildReporter : BuildReporter(NoOpICReporter, NoOpBuildMetricsReporter) {

    object NoOpICReporter : ICReporter {
        override fun report(message: () -> String) {}
        override fun reportVerbose(message: () -> String) {}
        override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {}
        override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {}
        override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {}
        override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {}
    }

    object NoOpBuildMetricsReporter : BuildMetricsReporter {
        override fun startMeasure(time: BuildTime, startNs: Long) {}
        override fun endMeasure(time: BuildTime, endNs: Long) {}
        override fun addTimeMetric(metric: BuildTime, durationMs: Long) {}
        override fun addMetric(metric: BuildPerformanceMetric, value: Long) {}
        override fun addAttribute(attribute: BuildAttribute) {}
        override fun getMetrics(): BuildMetrics = BuildMetrics()
        override fun addMetrics(metrics: BuildMetrics?) {}
    }
}
