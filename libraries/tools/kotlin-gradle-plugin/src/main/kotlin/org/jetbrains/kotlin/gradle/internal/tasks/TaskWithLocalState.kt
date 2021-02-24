/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.gradle.utils.outputsCompatible

internal interface TaskWithLocalState : Task {
    fun localStateDirectories(): FileCollection

    @get:Internal
    val metrics: BuildMetricsReporter
}

internal fun TaskWithLocalState.allOutputFiles(): FileCollection =
    outputs.files + localStateDirectories()
