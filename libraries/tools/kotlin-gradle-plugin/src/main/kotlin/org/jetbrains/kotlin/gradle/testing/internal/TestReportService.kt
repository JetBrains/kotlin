/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

typealias TaskError = Pair<String, Error>

abstract class TestReportService : BuildService<BuildServiceParameters.None> {
    private val taskHasFailedTests = mutableMapOf<String, Boolean>()
    private val taskSuppressedFailures = mutableMapOf<String, MutableList<TaskError>>()

    fun testFailed(taskPath: String) {
        taskHasFailedTests[taskPath] = true
    }

    fun hasFailedTests(path: String): Boolean {
        return taskHasFailedTests[path] ?: false
    }

    fun reportFailure(failedTaskPath: String, parentTaskPath: String, failure: Error) {
        taskSuppressedFailures.computeIfAbsent(parentTaskPath) { mutableListOf() }.add(failedTaskPath to failure)
    }

    fun getAggregatedTaskFailures(taskPath: String): List<TaskError> {
        return taskSuppressedFailures[taskPath] ?: emptyList()
    }

    companion object {
        fun registerIfAbsent(gradle: Gradle): Provider<TestReportService> {
            // Use class loader hashcode in case there are multiple class loaders in the same build
            return gradle.sharedServices
                .registerIfAbsent(
                    "${TestReportService::class.java.canonicalName}_${TestReportService::class.java.classLoader.hashCode()}",
                    TestReportService::class.java
                ) {}
        }
    }
}