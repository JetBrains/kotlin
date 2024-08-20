/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.compilerRunner.addBuildMetricsForTaskAction
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.utils.kotlinMetadataDir
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class CopyCommonizeCInteropForIdeTask @Inject constructor(
    commonizeCInteropTask: TaskProvider<CInteropCommonizerTask>
) : AbstractCInteropCommonizerTask() {

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val cInteropCommonizerTaskOutputDirectories: Provider<Set<File>> =
        commonizeCInteropTask.map { it.allOutputDirectories }

    @get:OutputDirectory
    override val outputDirectory: File = project.kotlinMetadataDir()
        .resolve("commonizer")
        .resolve(project.path.removePrefix(":").replace(":", "/"))

    @get:Internal
    val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    private val allInteropGroups: MutableList<Pair<CInteropCommonizerGroup, File>> = mutableListOf()

    init {
        val commonizerTask = commonizeCInteropTask.get()
        project.launch {
            commonizerTask.allInteropGroups
                .await()
                .forEach { group ->
                    allInteropGroups += group to commonizerTask.outputDirectory(group)
                }
        }
    }

    @TaskAction
    protected fun copy() {
        val metricReporter = metrics.get()
        addBuildMetricsForTaskAction(metricsReporter = metricReporter, languageVersion = null) {
            outputDirectory.mkdirs()
            for ((group, source) in allInteropGroups) {
                if (!source.exists()) continue
                val target = outputDirectory(group)
                if (target.exists()) target.deleteRecursively()
                source.copyRecursively(target, true)
            }
        }
    }
}
