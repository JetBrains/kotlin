/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.GradleException
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.testing.KotlinTestRunnerListener
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinTestReportCompatibilityHelper
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.toUri

/**
 * Aggregates tests reports for kotlin test tasks added by [registerTestTask].
 *
 * Individual test tasks will not fail build if this task will be executed,
 * also individual html and xml reports will replaced by one consolidated html report.
 * This behavior can be disabled by setting `kotlin.tests.individualTaskReports` property
 * to true.
 *
 * Aggregate test reports may form hierarchy, for example:
 *  - allTest // aggregates all tests
 *      - jvmTest
 *      - nativeTest // aggregates all native tests
 *          - macos64Test
 *          - linux64Test
 *          - mingw64
 *      - jsTests // aggregates all js tests
 *          - browserTest
 *          - nodejsTest
 *
 * In this case, only topmost aggregate test task will override reporting,
 * event if child tasks will be executed.
 */
@DisableCachingByDefault
abstract class KotlinTestReport : TestReport(), UsesTestReportService {
    @Transient
    @Internal
    val testTasks = mutableListOf<AbstractTestTask>()

    @Transient
    private var parent: KotlinTestReport? = null
    private val parentPaths = project.provider {
        computeAllParentTasksPaths()
    }

    @Internal
    @Transient
    val children = mutableListOf<TaskProvider<KotlinTestReport>>()

    @get:Input
    val overrideReporting: Boolean by lazy {
        !PropertiesProvider(project).individualTaskReports
    }

    @Input
    var checkFailedTests: Boolean = false

    @Input
    var ignoreFailures: Boolean = false

    private val testReportService
        get() = testReportServiceProvider.get()

    private val hasFailedTests: Boolean
        get() = testReportService.hasFailedTests(path)

    @get:Internal
    internal abstract val testReportCompatibilityHelper: Property<KotlinTestReportCompatibilityHelper>

    private fun computeAllParentTasksPaths(): List<String> {
        val allParents = mutableListOf<String>()
        var cur: KotlinTestReport? = this
        while (cur != null) {
            allParents.add(cur.path)
            cur = cur.parent
        }
        return allParents
    }

    fun addChild(childProvider: TaskProvider<KotlinTestReport>) {
        val child = childProvider.get()

        check(child.parent == null) { "$child already registers as child of ${child.parent}" }
        child.parent = this

        children.add(childProvider)
        reportOnChildTasks(childProvider)
    }

    private fun reportOnChildTasks(childProvider: TaskProvider<KotlinTestReport>) {
        val child = childProvider.get()

        child.testTasks.forEach {
            reportOn(it)
        }
        child.children.forEach {
            reportOnChildTasks(it)
        }
    }

    fun registerTestTask(task: AbstractTestTask) {
        testTasks.add(task)

        task.addTestListener(FailedTestListener(task.path, parentPaths, testReportServiceProvider))
        if (task is KotlinTest) {
            val listener = SuppressedTestRunningFailureListener(parentPaths, task.path, testReportServiceProvider)
            task.addRunListener(listener)
        }
        reportOn(task)

        addToParents(task)
    }

    private fun addToParents(task: AbstractTestTask) {
        val parent = parent
        if (parent != null) {
            parent.reportOn(task)
            parent.addToParents(task)
        }
    }

    private fun reportOn(task: AbstractTestTask) {
        testReportCompatibilityHelper.get().addTestResultsFrom(this, task)
    }

    @get:Internal
    abstract val htmlReportFile: RegularFileProperty

    @Suppress("unused")
    @Deprecated("Use `htmlReportFile` instead", ReplaceWith("htmlReportFile"))
    @get:Internal
    open val htmlReportUrl: String?
        get() = htmlReportFile.orNull?.toUri().toString()

    @TaskAction
    fun checkFailedTests() {
        if (checkFailedTests) {
            checkSuppressedRunningFailures()
            if (hasFailedTests) {
                if (ignoreFailures) {
                    logger.warn(getFailingTestsMessage())
                } else {
                    throw GradleException(getFailingTestsMessage())
                }
            }
        }
    }

    private fun getFailingTestsMessage(): String {
        val message = StringBuilder("There were failing tests.")

        if (htmlReportFile.isPresent) {
            message.append(" See the report at: ${htmlReportFile.get().toUri()}")
        }
        return message.toString()
    }

    private fun checkSuppressedRunningFailures() {
        val taskFailures = testReportService.getAggregatedTaskFailures(path)
        if (taskFailures.isNotEmpty()) {
            val allErrors = mutableListOf<Error>()
            val msg = buildString {
                appendLine("Failed to execute all tests:")
                taskFailures.groupBy { it.first }.forEach { (path, errors) ->
                    append(path)
                    append(": ")
                    var first = true
                    errors.forEach { (_, error) ->
                        allErrors.add(error)
                        append(error.message)
                        if (first) first = false else appendLine()
                    }
                }

                if (hasFailedTests) {
                    val failedTestsMessage = getFailingTestsMessage()
                    if (ignoreFailures) {
                        logger.warn(getFailingTestsMessage())
                    } else {
                        allErrors.add(Error(failedTestsMessage))
                        appendLine("Also: $failedTestsMessage")
                    }
                }
            }

            throw MultiCauseException(msg, allErrors)
        }
    }

    fun maybeOverrideReporting(graph: TaskExecutionGraph) {
        if (!overrideReporting) return
        if (!graph.hasTask(this)) return

        // only topmost aggregate should override reporting
        var parent = parent
        while (parent != null) {
            if (parent.overrideReporting && graph.hasTask(parent)) return
            parent = parent.parent
        }

        overrideReporting()
    }

    private fun overrideReporting() {
        ignoreFailures = false
        checkFailedTests = true

        disableIndividualTestTaskFailing()
    }

    private fun disableIndividualTestTaskFailing() {
        testTasks.forEach { task ->
            task.ignoreFailures = true
            if (task is KotlinTest) {
                task.ignoreRunFailures = true
            }
        }

        children.forEach { child ->
            child.configure {
                it.checkFailedTests = false
                it.disableIndividualTestTaskFailing()
            }
        }
    }

    private class SuppressedTestRunningFailureListener(
        private val allListenedTaskParentsPaths: Provider<List<String>>,
        private val failedTaskPath: String,
        private val testReportServiceProvider: Provider<TestReportService>
    ) : KotlinTestRunnerListener {
        override fun runningFailure(failure: Error) {
            allListenedTaskParentsPaths.get().forEach {
                testReportServiceProvider.get().reportFailure(failedTaskPath, it, failure)
            }
        }
    }

    private class FailedTestListener(
        private val testTaskPath: String,
        private val allListenedTaskParentsPaths: Provider<List<String>>,
        private val testReportServiceProvider: Provider<TestReportService>
    ) : TestListener {
        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            reportFailure(result)
        }

        override fun beforeSuite(suite: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            reportFailure(result)
        }

        private fun reportFailure(result: TestResult) {
            if (result.failedTestCount > 0) {
                allListenedTaskParentsPaths.get().forEach {
                    testReportServiceProvider.get().testFailed(it, testTaskPath)
                }
            }
        }
    }
}
