/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.incremental.*
import java.io.File
import org.jetbrains.kotlin.gradle.incremental.ChangesCollectorResult.Success
import org.jetbrains.kotlin.gradle.incremental.ChangesCollectorResult.Failure

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
            if (result is Failure) {
                return result
            }
        }
        return Success
    }

    @Suppress("UNUSED_PARAMETER")
    private fun collectClassChanges(
        current: ClassSnapshot,
        previous: ClassSnapshot,
        changesCollector: ChangesCollector
    ): ChangesCollectorResult {
        return Failure.NotYetImplemented
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
        override fun startMeasure(metric: BuildTime, startNs: Long) {}
        override fun endMeasure(metric: BuildTime, endNs: Long) {}
        override fun addAttribute(attribute: BuildAttribute) {}
        override fun getMetrics(): BuildMetrics = BuildMetrics()
        override fun addMetrics(metrics: BuildMetrics?) {}
    }
}
