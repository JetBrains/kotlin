/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskBuildMetrics
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers

open class TaskCachesManagement {
    fun cleanTaskExecutionCaches() {
        TaskLoggers.clear()
        TaskExecutionResults.clear()
        TaskBuildMetrics.clear()
    }
}