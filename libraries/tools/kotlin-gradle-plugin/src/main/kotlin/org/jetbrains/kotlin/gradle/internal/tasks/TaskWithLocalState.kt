/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter

internal interface TaskWithLocalState : Task {
    @get:Internal
    val localStateDirectories: ConfigurableFileCollection

    @get:Internal
    val metrics: Property<BuildMetricsReporter>
}

internal fun TaskWithLocalState.allOutputFiles(): FileCollection =
    outputs.files + localStateDirectories
