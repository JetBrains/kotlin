/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.tasks.testing.DefaultTestTaskReports
import org.gradle.api.internal.tasks.testing.junit.result.*
import org.gradle.api.internal.tasks.testing.report.DefaultTestReport
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.internal.concurrent.CompositeStoppable.stoppable
import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.remote.internal.inet.InetAddressFactory
import org.jetbrains.kotlin.gradle.utils.injected
import java.util.*
import javax.inject.Inject

@Suppress("UnstableApiUsage")
open class AggregateTestReportGradle5 : AggregateTestReport() {
    @Internal
    val reports: DefaultTestTaskReports

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

    override fun configureReportsConvention(name: String) {
        reports.configureConventions(project, name)
    }

    override val htmlReportUrl: String?
        get() =
            if (reports.html.isEnabled) ConsoleRenderer().asClickableFileUrl(reports.html.destination.resolve("index.html"))
            else null

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
                logger.info("{} - no binary test results found in dirs: {}.", path, testResultDirs)
                didWork = false
            }
        } finally {
            stoppable(resultsProvider).stop()
        }

        checkFailedTests()
    }

    private fun createAggregateProvider(): TestResultsProvider {
        val resultsProviders = LinkedList<TestResultsProvider>()
        try {
            val resultDirs = testResultDirs
            return if (resultDirs.size == 1) BinaryResultBackedTestResultsProvider(resultDirs.single())
            else AggregateTestResultsProvider(resultDirs.mapTo(resultsProviders) { BinaryResultBackedTestResultsProvider(it) })
        } catch (e: RuntimeException) {
            stoppable(resultsProviders).stop()
            throw e
        }
    }

    override fun overrideReporting(task: AbstractTestTask) {
        task.ignoreFailures = true

        @Suppress("UnstableApiUsage")
        task.reports.html.isEnabled = false

        @Suppress("UnstableApiUsage")
        task.reports.junitXml.isEnabled = false

        checkFailedTests = true
        ignoreFailures = false
    }
}