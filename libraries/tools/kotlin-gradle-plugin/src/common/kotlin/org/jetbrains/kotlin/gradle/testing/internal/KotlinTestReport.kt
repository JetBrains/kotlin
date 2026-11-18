/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestReport
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.toUri

/**
 * Aggregates tests reports for kotlin test tasks added by [registerTestTask]
 * into one consolidated HTML report.
 *
 * The task is report-only: individual test tasks fail the build on their own
 * failures, as with Gradle's native test tasks. Consequently, this task is
 * skipped when any of the aggregated test tasks fail unless `--continue` is
 * used or the failing tasks opt out via `ignoreFailures`, in which case no
 * consolidated report is generated (same as with Gradle's Test Report
 * Aggregation plugin).
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
 */
@DisableCachingByDefault
abstract class KotlinTestReport : TestReport() {
    @Transient
    @Internal
    val testTasks = mutableListOf<AbstractTestTask>()

    @Transient
    private var parent: KotlinTestReport? = null

    @Internal
    @Transient
    val children = mutableListOf<TaskProvider<KotlinTestReport>>()

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
        testResults.from(task.binaryResultsDirectory)
    }

    @get:Internal
    abstract val htmlReportFile: RegularFileProperty

    @Suppress("unused")
    @Deprecated("Use `htmlReportFile` instead", ReplaceWith("htmlReportFile"))
    @get:Internal
    open val htmlReportUrl: String?
        get() = htmlReportFile.orNull?.toUri().toString()
}
