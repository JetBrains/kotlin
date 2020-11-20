/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.invocation.Gradle
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskOperationDescriptor
import java.io.File
import java.util.*

class KotlinBuildReporterListener(val gradle: Gradle, val perfReportFile: File) : OperationCompletionListener, AutoCloseable {

    private val kotlinTaskTimeNs = HashMap<String, Long>()
    internal var allTasksTimeNs: Long = 0L

    override fun onFinish(event: FinishEvent?) {
        val descriptor = event?.descriptor
        if (descriptor is TaskOperationDescriptor) {
            val taskPath = descriptor.taskPath
            val executionTime = event.result.endTime - event.result.startTime
            allTasksTimeNs += executionTime
            kotlinTaskTimeNs[taskPath] = executionTime
        }
    }

    override fun close() {
        KotlinBuildReporterHandler().buildFinished(gradle, perfReportFile, kotlinTaskTimeNs, allTasksTimeNs)
    }
}