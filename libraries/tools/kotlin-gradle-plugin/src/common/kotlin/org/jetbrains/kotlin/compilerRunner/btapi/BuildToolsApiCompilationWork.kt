/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import java.io.File

internal abstract class BuildToolsApiCompilationWork : WorkAction<BuildToolsApiCompilationWork.BuildToolsApiCompilationParameters> {
    internal interface BuildToolsApiCompilationParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter>
    }

    private val workArguments
        get() = parameters.compilerWorkArguments.get()

    override fun execute() {
        println("I'm simulating compilation, the compilation classpath would be ${workArguments.compilerFullClasspath}")
    }
}