/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.util.HashSet

internal class TaskExecutionResult(
    val buildMetrics: BuildMetrics<BuildTimeMetric, BuildPerformanceMetric>,
    val taskInfo: TaskExecutionInfo = TaskExecutionInfo(),
    val icLogLines: List<String> = emptyList()
)

internal class TaskExecutionInfo(
    val kotlinLanguageVersion: KotlinVersion? = null,
    val changedFiles: SourcesChanges? = null,
    val compilerArguments: Array<String> = emptyArray(),
    val tags: Set<StatTag> = emptySet(),
)

internal fun IncrementalCompilationEnvironment.collectIcTags(): Set<StatTag> = buildSet {
    if (icFeatures.withAbiSnapshot) {
        add(StatTag.ABI_SNAPSHOT)
    }
    if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
        add(StatTag.ARTIFACT_TRANSFORM)
    }
}
