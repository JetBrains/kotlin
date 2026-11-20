/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.process.ExecOperations
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.testing.KotlinTestRunnerListener
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import javax.inject.Inject

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class KotlinTest
@Inject
internal constructor(
    private val execOps: ExecOperations,
) : AbstractTestTask() {

    @Input
    @Optional
    var targetName: String? = null

    protected val filterExt: DefaultTestFilter
        @Internal get() = filter as DefaultTestFilter

    init {
        filterExt.isFailOnNoMatchingTests = false
    }

    val includePatterns: Set<String>
        @Input get() = filterExt.includePatterns + filterExt.commandLineIncludePatterns

    val excludePatterns: Set<String>
        @Input get() = filterExt.excludePatterns

    private val runListeners = mutableListOf<KotlinTestRunnerListener>()

    @Internal
    var ignoreRunFailures: Boolean = false

    /**
     * Whether test failures are prevented from failing this task: set by the
     * user directly (ignoreFailures), or set by the aggregated test report
     * task, which sets both ignoreFailures and ignoreRunFailures on
     * individual test tasks whenever an aggregate is in the task graph (see
     * KotlinTestReport.disableIndividualTestTaskFailing).
     *
     * Tracked as a task input to keep build cache entries of suppressed
     * executions apart from entries of honest executions: a suppressed
     * execution may succeed despite failing tests, so its entry may only be
     * restored by another suppressed execution, in which KotlinTestReport
     * re-checks the recorded results via [failedTestsCountFile]. An honest
     * execution can then never restore results containing test failures.
     */
    @get:Input
    @Suppress("UNUSED") // Gradle input
    internal val failuresAreIgnored: Boolean
        get() = ignoreFailures || ignoreRunFailures

    /**
     * Total number of failed tests of this task's execution, persisted as a
     * task output so that it is stored in and restored from the build cache
     * together with the binary results. KotlinTestReport reads it to detect
     * test failures in results restored from cache, where test listeners do
     * not run.
     */
    @get:OutputFile
    internal abstract val failedTestsCountFile: RegularFileProperty

    init {
        failedTestsCountFile
            .convention(project.layout.buildDirectory.file("${TestingBasePlugin.TEST_RESULTS_DIR_NAME}/$name/failed-tests-count.txt"))
            .finalizeValueOnRead()

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                // Only the root suite accumulates the failures of the whole run:
                if (suite.parent != null) return
                val countFile = failedTestsCountFile.get().asFile
                countFile.parentFile.mkdirs()
                countFile.writeText(result.failedTestCount.toString())
            }

            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        })
    }

    fun addRunListener(listener: KotlinTestRunnerListener) {
        runListeners.add(listener)
    }

    private val ignoreTcsmOverflow = PropertiesProvider(project).ignoreTcsmOverflow

    // This method is called on execution time
    override fun createTestExecuter(): TestExecuter<*> =
        TCServiceMessagesTestExecutor(
            description = path,
            runListeners = runListeners,
            ignoreTcsmOverflow = ignoreTcsmOverflow.get(),
            ignoreRunFailures = ignoreRunFailures,
            execOps = execOps,
        )
}
