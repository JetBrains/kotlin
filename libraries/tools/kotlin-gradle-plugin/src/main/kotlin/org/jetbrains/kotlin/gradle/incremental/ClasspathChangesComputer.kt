/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.util.io.FileUtil
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
            val result = collectClassChanges(currentSnapshot, previousSnapshot, changesCollector)
            if (result !is Success) {
                return result
            }
        }
        return Success
    }

    private fun collectClassChanges(
        current: ClassSnapshot,
        previous: ClassSnapshot,
        @Suppress("UNUSED_PARAMETER") changesCollector: ChangesCollector
    ): ChangesCollectorResult {
        if (current is JavaClassSnapshot && previous is JavaClassSnapshot &&
            (current !is RegularJavaClassSnapshot || previous !is RegularJavaClassSnapshot)
        ) {
            return Failure.NotYetImplemented
        }
        // TODO: Store results in changesCollector and return SUCCESS here
        computeClassChanges(current, previous)
        return Failure.NotYetImplemented
    }

    @VisibleForTesting
    internal fun computeClassChanges(current: ClassSnapshot, previous: ClassSnapshot): DirtyData {
        // TODO Create IncrementalJvmCache early once and reuse it here
        val workingDir =
            FileUtil.createTempDirectory(this::class.java.simpleName, "_WorkingDir_${UUID.randomUUID()}", /* deleteOnExit */ true)
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToCanonicalPathConverter)
        val changesCollector = ChangesCollector()

        when {
            current is KotlinClassSnapshot && previous is KotlinClassSnapshot ->
                collectKotlinClassChanges(current, previous, incrementalJvmCache, changesCollector)
            current is JavaClassSnapshot && previous is JavaClassSnapshot ->
                collectJavaClassChanges(current, previous, incrementalJvmCache, changesCollector)
            else -> {
                // TODO: Handle current is KotlinClassSnapshot && previous is JavaClassSnapshot, and vice versa
                error("Incompatible types: ${current.javaClass.name} vs. ${previous.javaClass.name}")
            }
        }

        workingDir.deleteRecursively()
        return changesCollector.getDirtyData(listOf(incrementalJvmCache), NoOpBuildReporter.NoOpICReporter)
    }

    private fun collectKotlinClassChanges(
        current: KotlinClassSnapshot,
        previous: KotlinClassSnapshot,
        incrementalJvmCache: IncrementalJvmCache,
        changesCollector: ChangesCollector
    ) {
        // Store previous snapshot in incrementalJvmCache, the returned ChangesCollector result is not used.
        incrementalJvmCache.saveClassToCache(
            kotlinClassInfo = previous.classInfo,
            sourceFiles = null,
            changesCollector = ChangesCollector()
        )
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)

        // Compute changes between the current snapshot and the previously stored snapshot, and store the result in changesCollector.
        incrementalJvmCache.saveClassToCache(
            kotlinClassInfo = current.classInfo,
            sourceFiles = null,
            changesCollector = changesCollector
        )
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)
    }

    private fun collectJavaClassChanges(
        current: JavaClassSnapshot,
        previous: JavaClassSnapshot,
        incrementalJvmCache: IncrementalJvmCache,
        changesCollector: ChangesCollector
    ) {
        // Store previous snapshot in incrementalJvmCache, the returned ChangesCollector result is not used.
        val previousSnapshot = (previous as RegularJavaClassSnapshot).serializedJavaClass // TODO Handle unsafe cast
        incrementalJvmCache.saveJavaClassProto(/* source */ null, previousSnapshot, ChangesCollector())
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)

        // Compute changes between the current snapshot and the previously stored snapshot, and store the result in changesCollector.
        val currentSnapshot = (current as RegularJavaClassSnapshot).serializedJavaClass
        incrementalJvmCache.saveJavaClassProto(/* source */ null, currentSnapshot, changesCollector)
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)
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
