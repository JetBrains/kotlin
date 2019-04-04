/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.tasks.testing.DefaultTestTaskReports
import org.gradle.api.internal.tasks.testing.junit.result.*
import org.gradle.api.internal.tasks.testing.report.DefaultTestReport
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.internal.concurrent.CompositeStoppable.stoppable
import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.remote.internal.inet.InetAddressFactory
import org.jetbrains.kotlin.gradle.utils.injected
import java.util.*
import javax.inject.Inject

@Suppress("UnstableApiUsage")
open class AggregateTestReport : DefaultTask() {
    @Internal
    val testTasks = mutableListOf<AbstractTestTask>()

    /**
     * Returns the set of binary test results to include in the report.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val testResultDirs: FileCollection
        @PathSensitive(PathSensitivity.NONE)
        @InputFiles
        @SkipWhenEmpty
        get() {
            val dirs = UnionFileCollection()
            testTasks.forEach {
                dirs.addToUnion(project.files(it.binResultsDir).builtBy(it))
            }
            return dirs
        }

    @Input
    var ignoreFailures: Boolean = false

    @Nested
    val reports: TestTaskReports

    protected open val instantiator: Instantiator
        @Inject get() = injected

    protected open val collectionCallbackActionDecorator: CollectionCallbackActionDecorator
        @Inject get() = injected

    protected open val buildOperationExecutor: BuildOperationExecutor
        @Inject get() = injected

    protected open val inetAddressFactory: InetAddressFactory
        @Inject get() = injected

    init {
        @Suppress("LeakingThis")
        reports = instantiator.newInstance(DefaultTestTaskReports::class.java, this, collectionCallbackActionDecorator)
        reports.junitXml.isEnabled = true
        reports.html.isEnabled = true
    }

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

    internal var checkFailedTests: Boolean = false

    fun registerTestTask(task: AbstractTestTask) {
        testTasks.add(task)
        task.addTestListener(failedTestsListener)
    }

    @TaskAction
    fun generateReport() {
        val resultsProvider = createAggregateProvider()
        try {
            if (resultsProvider.isHasResults) {
                val junitXml = reports.junitXml
                if (junitXml.isEnabled) {
                    Binary2JUnitXmlReportGenerator(
                        junitXml.destination,
                        resultsProvider,
                        when {
                            junitXml.isOutputPerTestCase -> TestOutputAssociation.WITH_TESTCASE
                            else -> TestOutputAssociation.WITH_SUITE
                        },
                        buildOperationExecutor,
                        inetAddressFactory.hostname
                    ).generate()
                }

                val html = reports.html
                if (html.isEnabled) {
                    val testReport = DefaultTestReport(buildOperationExecutor)
                    testReport.generateReport(resultsProvider, html.destination)
                }
            } else {
                logger.info("{} - no binary test results found in dirs: {}.", path, testResultDirs.files)
                didWork = false
            }
        } finally {
            stoppable(resultsProvider).stop()
        }

        if (checkFailedTests && hasFailedTests) {
            val message = StringBuilder("There were failing tests.")

            if (reports.html.isEnabled) {
                val reportUrl = ConsoleRenderer().asClickableFileUrl(reports.html.destination.resolve("index.html"))
                message.append(" See the report at: $reportUrl")
            }

            if (ignoreFailures) {
                logger.warn(message.toString())
            } else {
                throw GradleException(message.toString())
            }
        }
    }

    private fun createAggregateProvider(): TestResultsProvider {
        val resultsProviders = LinkedList<TestResultsProvider>()
        try {
            val resultDirs = testResultDirs
            return if (resultDirs.files.size == 1) BinaryResultBackedTestResultsProvider(resultDirs.singleFile)
            else AggregateTestResultsProvider(resultDirs.mapTo(resultsProviders) { BinaryResultBackedTestResultsProvider(it) })
        } catch (e: RuntimeException) {
            stoppable(resultsProviders).stop()
            throw e
        }
    }
}