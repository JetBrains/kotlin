/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.GradleException
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.io.File

open class AggregateTestReport : AbstractTask() {
    @Internal
    val testTasks = mutableListOf<AbstractTestTask>()

    /**
     * Returns the set of binary test results to include in the report.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val testResultDirs: Collection<File>
        @PathSensitive(PathSensitivity.NONE)
        @InputFiles
        @SkipWhenEmpty
        get() {
            val dirs = mutableListOf<File>()
            testTasks.forEach {
                @Suppress("UnstableApiUsage")
                dirs.add(it.binResultsDir)
            }
            return dirs
        }

    @Input
    var ignoreFailures: Boolean = false

    private var hasFailedTests = false

    private val failedTestsListener = object : TestListener {
        override fun beforeTest(testDescriptor: TestDescriptor) {
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        }

        override fun beforeSuite(suite: TestDescriptor) {
        }

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.failedTestCount > 0) {
                hasFailedTests = true
            }
        }
    }

    @Input
    var checkFailedTests: Boolean = false

    fun registerTestTask(task: AbstractTestTask) {
        testTasks.add(task)
        task.addTestListener(failedTestsListener)
    }

    open fun configureReportsConvention(name: String) {

    }

    @Internal
    protected open val htmlReportUrl: String? = null

    protected fun checkFailedTests() {
        if (checkFailedTests && hasFailedTests) {
            val message = StringBuilder("There were failing tests.")

            val reportUrl = htmlReportUrl
            if (reportUrl != null) {
                message.append(" See the report at: $reportUrl")
            }

            if (ignoreFailures) {
                logger.warn(message.toString())
            } else {
                throw GradleException(message.toString())
            }
        }
    }

    open fun overrideReporting(task: AbstractTestTask) {
        task.ignoreFailures = true
        checkFailedTests = true
        ignoreFailures = false
    }
}
