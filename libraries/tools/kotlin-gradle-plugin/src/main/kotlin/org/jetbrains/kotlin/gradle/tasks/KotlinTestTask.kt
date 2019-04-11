/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.process.internal.ExecHandleFactory
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.testing.TestsGrouping
import org.jetbrains.kotlin.gradle.utils.injected
import java.io.File
import javax.inject.Inject

abstract class KotlinTestTask : AbstractTestTask() {
    @Input
    var testsGrouping: TestsGrouping =
        TestsGrouping.root

    @Input
    @Optional
    var targetName: String? = null

    @Input
    var excludes = mutableSetOf<String>()

    @Suppress("UnstableApiUsage")
    protected val filterExt: DefaultTestFilter
        @Internal get() = filter as DefaultTestFilter

    init {
        filterExt.isFailOnNoMatchingTests = false
    }

    protected val includePatterns: Set<String>
        @Internal get() = filterExt.includePatterns + filterExt.commandLineIncludePatterns

    protected val excludePatterns: Set<String>
        @Internal get() = excludes

    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    override fun createTestExecuter() = TCServiceMessagesTestExecutor(
        execHandleFactory,
        buildOperationExecutor
    )

    companion object {
        @Suppress("UnstableApiUsage")
        private val Project.testResults: File
            get() = project.buildDir.resolve(TestingBasePlugin.TEST_RESULTS_DIR_NAME)

        private val Project.reportsDir: File
            get() = project.extensions.getByType(ReportingExtension::class.java).baseDir

        @Suppress("UnstableApiUsage")
        private val Project.testReports: File
            get() = reportsDir.resolve(TestingBasePlugin.TESTS_DIR_NAME)

        internal fun configureConventions(task: KotlinTestTask) {
            val htmlReport = DslObject(task.reports.html)
            val xmlReport = DslObject(task.reports.junitXml)

            xmlReport.conventionMapping.map("destination") { task.project.testResults.resolve(task.name) }
            htmlReport.conventionMapping.map("destination") { task.project.testReports.resolve(task.name) }
            task.conventionMapping.map("binResultsDir") { task.project.testResults.resolve(task.name + "/binary") }
        }
    }
}
