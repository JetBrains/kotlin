/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.io.*
import java.util.concurrent.ConcurrentHashMap

internal typealias TaskError = Pair<String, Error>

internal interface UsesTestReportService : Task {
    @get:Internal
    val testReportServiceProvider: Property<TestReportService>
}

/**
 * A build service required for correct test failures detection in [KotlinTestReport] as it requires cross-task interaction.
 */
internal abstract class TestReportService : BuildService<TestReportService.TestReportServiceParameters>, AutoCloseable {
    internal interface TestReportServiceParameters : BuildServiceParameters {
        val testTasksStateFile: RegularFileProperty
    }

    private val log = Logging.getLogger(this.javaClass)
    private val previouslyFailedTestTasks = readPreviouslyFailedTasks()
    private val reportHasFailedTests = ConcurrentHashMap<String, Boolean>()
    private val testTaskSuppressedFailures = ConcurrentHashMap<String, MutableList<TaskError>>()

    /**
     * Marks [KotlinTestReport] with [reportTaskPath] as a report containing failed tests during the build.
     * [testTaskPath] is a path of the actual test task with failed tests.
     */
    fun testFailed(reportTaskPath: String, testTaskPath: String) {
        reportHasFailedTests[reportTaskPath] = true
        previouslyFailedTestTasks.add(testTaskPath)
    }

    /**
     * Checks whether [KotlinTestReport] defined by [path] contains any children test tasks that failed during the build
     */
    fun hasFailedTests(path: String): Boolean {
        return reportHasFailedTests[path] ?: false
    }

    /**
     * Reports a test task execution failure (not test failure).
     * @param failedTaskPath is a path of the failed test task
     * @param parentTaskPath is a path of a [KotlinTestReport] that the task reports to
     */
    fun reportFailure(failedTaskPath: String, parentTaskPath: String, failure: Error) {
        testTaskSuppressedFailures.computeIfAbsent(parentTaskPath) { mutableListOf() }.add(failedTaskPath to failure)
    }

    /**
     * Returns all the test task execution failures (not test failures) related to the [KotlinTestReport] defined by [taskPath]
     */
    fun getAggregatedTaskFailures(taskPath: String): List<TaskError> {
        return testTaskSuppressedFailures[taskPath] ?: emptyList()
    }

    override fun close() {
        writePreviouslyFailedTasks()
    }

    /**
     * Checks whether the test task defined by [path] had failed previously (doesn't matter if it's caused by failed test or any runtime problem).
     * This function is not idempotent as it resets the task's failed state.
     */
    fun hasTestTaskFailedPreviously(path: String) = previouslyFailedTestTasks.remove(path)

    private val binaryStateFile: File
        get() = parameters.testTasksStateFile.get().asFile

    private fun readPreviouslyFailedTasks(): MutableSet<String> {
        val failedTasksSet: MutableSet<String> = ConcurrentHashMap.newKeySet()
        if (!binaryStateFile.exists()) return failedTasksSet
        try {
            ObjectInputStream(FileInputStream(binaryStateFile)).use {
                @Suppress("UNCHECKED_CAST")
                failedTasksSet.addAll(it.readObject() as Set<String>)
            }
        } catch (e: Exception) {
            log.error("Cannot read test tasks state from $binaryStateFile", e)
        }
        return failedTasksSet
    }

    private fun writePreviouslyFailedTasks() {
        previouslyFailedTestTasks += testTaskSuppressedFailures.values.flatMap { taskErrors -> taskErrors.map { (taskPath, _) -> taskPath } }
        binaryStateFile.parentFile.mkdirs()
        try {
            ObjectOutputStream(FileOutputStream(binaryStateFile)).use {
                it.writeObject(previouslyFailedTestTasks.toSet())
            }
        } catch (e: Exception) {
            log.error("Cannot store test tasks state into $binaryStateFile", e)
        }
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<TestReportService> =
            project.gradle.sharedServices
                .registerIfAbsent(
                    "${TestReportService::class.java.canonicalName}_${project.path}",
                    TestReportService::class.java
                ) { spec ->
                    spec.parameters.testTasksStateFile.set(project.layout.buildDirectory.file("test-results/kotlin-test-tasks-state.bin"))
                }.also { serviceProvider ->
                    SingleActionPerProject.run(project, UsesTestReportService::class.java.name) {
                        project.tasks.withType<UsesTestReportService>().configureEach { task ->
                            task.usesService(serviceProvider)
                        }
                    }
                }
    }
}