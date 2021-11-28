/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.getSystemProperty
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable

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
        aggregate.configure {
            it.dependsOn(taskHolder.name)
            it.registerTestTask(taskHolder.get())
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

        val reportName = name

        val aggregate: TaskProvider<KotlinTestReport> = project.registerTask(name) { aggregate ->
            aggregate.description = description
            aggregate.group = JavaBasePlugin.VERIFICATION_GROUP

            aggregate.destinationDir = project.testReportsDir.resolve(reportName)

            val isIdeaActive = project.getSystemProperty("idea.active") != null

            if (isIdeaActive) {
                aggregate.extensions.extraProperties.set("idea.internal.test", true)
            }

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
        return "clean" + taskName.capitalize()
    }

    private val Project.cleanAllTestTask: TaskProvider<*>
        get() {
            val taskName = cleanTaskName(allTestsTask.name)
            return project.locateTask<Task>(taskName)
                ?: project.registerTask<Task>(taskName).also { cleanAllTest ->
                    tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME).dependsOn(cleanAllTest)
                }
        }

    companion object {
        const val PROJECT_EXTENSION_NAME = "kotlinTestRegistry"
    }
}

internal val Project.kotlinTestRegistry: KotlinTestsRegistry
    get() = extensions.getByName(KotlinTestsRegistry.PROJECT_EXTENSION_NAME) as KotlinTestsRegistry