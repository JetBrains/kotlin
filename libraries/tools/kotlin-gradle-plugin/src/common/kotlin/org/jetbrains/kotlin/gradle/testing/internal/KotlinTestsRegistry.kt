/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinTestReportCompatibilityHelper
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.readSystemPropertyAtConfigurationTime
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

/**
 * Internal service for creating aggregated test tasks and registering all test tasks.
 * See [KotlinTestReport] for more details about aggregated test tasks.
 */
class KotlinTestsRegistry(val project: Project, val allTestsTaskName: String = "allTests") {
    val allTestsTask: TaskProvider<KotlinTestReport>
        get() = doGetOrCreateAggregatedTestTask(
            name = allTestsTaskName,
            description = "Runs the tests for all targets and create aggregated report"
        ).also {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(it)
        }

    fun registerTestTask(
        taskHolder: TaskProvider<out AbstractTestTask>,
        aggregate: TaskProvider<KotlinTestReport> = allTestsTask
    ) {
        project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(taskHolder)
        project.cleanAllTestTask.configure { it.dependsOn(cleanTaskName(taskHolder.name)) }
        val testReportService = TestReportService.registerIfAbsent(project)
        aggregate.configure {
            it.dependsOn(taskHolder.name)
            it.registerTestTask(taskHolder.get())
        }
        taskHolder.configure { task ->
            task.usesService(testReportService)
            task.outputs.upToDateWhen {
                val hasFailedPreviously = testReportService.get().hasTestTaskFailedPreviously(it.path)
                if (hasFailedPreviously) {
                    task.logger.kotlinInfo("Marking $it as not up-to-date because the task has failed previously")
                }
                !hasFailedPreviously
            }
        }

        taskHolder.configure { task ->
            ijListenTestTask(task)
        }
    }

    fun getOrCreateAggregatedTestTask(
        name: String,
        description: String,
        parent: TaskProvider<KotlinTestReport>? = allTestsTask
    ): TaskProvider<KotlinTestReport> {
        if (name == parent?.name) return parent

        return doGetOrCreateAggregatedTestTask(name, description, parent)
    }

    private fun doGetOrCreateAggregatedTestTask(
        name: String,
        description: String,
        parent: TaskProvider<KotlinTestReport>? = null,
        configure: (TaskProvider<KotlinTestReport>) -> Unit = {}
    ): TaskProvider<KotlinTestReport> {
        val existed = project.locateTask<KotlinTestReport>(name)
        if (existed != null) return existed

        val testReportService = TestReportService.registerIfAbsent(project)
        val aggregate: TaskProvider<KotlinTestReport> = project.registerTask(name) { aggregate ->
            aggregate.description = description
            aggregate.group = JavaBasePlugin.VERIFICATION_GROUP

            val compatibilityHelper = project
                .variantImplementationFactory<KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory>()
                .getInstance(project.objects)

            compatibilityHelper.setDestinationDirectory(aggregate, project.testReportsDir.map { it.dir(name) })

            val isIdeaActive = project.readSystemPropertyAtConfigurationTime("idea.active").isPresent

            if (isIdeaActive) {
                aggregate.extensions.extraProperties.set("idea.internal.test", true)
            }
            aggregate.htmlReportFile.value(compatibilityHelper.getDestinationDirectory(aggregate).file("index.html")).disallowChanges()
            aggregate.testReportServiceProvider.value(testReportService).finalizeValueOnRead()
            aggregate.testReportCompatibilityHelper.value(compatibilityHelper).finalizeValueOnRead()

            project.gradle.taskGraph.whenReady { graph ->
                aggregate.maybeOverrideReporting(graph)
            }
        }

        parent?.configure {
            it.addChild(aggregate)
            it.dependsOn(aggregate)
        }

        configure(aggregate)

        return aggregate
    }

    private fun cleanTaskName(taskName: String): String {
        check(taskName.isNotEmpty())
        return "clean" + taskName.capitalizeAsciiOnly()
    }

    private val Project.cleanAllTestTask: TaskProvider<*>
        get() {
            val taskName = cleanTaskName(allTestsTask.name)
            return project.locateOrRegisterTask<Task>(taskName) { cleanAllTest ->
                cleanAllTest.group = BasePlugin.BUILD_GROUP
                cleanAllTest.description = "Deletes all the test results."
            }.also { cleanAllTest ->
                tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME).dependsOn(cleanAllTest)
            }
        }

    companion object {
        const val PROJECT_EXTENSION_NAME = "kotlinTestRegistry"
    }
}

internal val Project.kotlinTestRegistry: KotlinTestsRegistry
    get() = extensions.getByName(KotlinTestsRegistry.PROJECT_EXTENSION_NAME) as KotlinTestsRegistry