/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask

/**
 * Internal service for creating aggregated test tasks and registering all test tasks.
 * See [KotlinTestReport] for more details about aggregated test tasks.
 */
class KotlinTestsRegistry(val project: Project, val allTestsTaskName: String = "allTests") {
    val allTestsTask: TaskHolder<KotlinTestReport>
        get() = doGetOrCreateAggregatedTestTask(
            name = allTestsTaskName,
            description = "Runs the tests for all targets and create aggregated report"
        ) {
            project.tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME)
                .dependsOn(it.getTaskOrProvider())
        }

    fun registerTestTask(
        taskHolder: TaskHolder<AbstractTestTask>,
        aggregate: KotlinTestReport = allTestsTask.doGetTask()
    ) {
        val project = taskHolder.project

        project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(taskHolder.name)
        project.cleanAllTestTask.dependsOn(cleanTaskName(taskHolder.name))
        aggregate.dependsOn(taskHolder.name)

        taskHolder.configure { task ->
            aggregate.registerTestTask(task)
            ijListenTestTask(task)
        }
    }

    fun getOrCreateAggregatedTestTask(
        name: String,
        description: String,
        parent: TaskHolder<KotlinTestReport>? = allTestsTask
    ): TaskHolder<KotlinTestReport> {
        if (name == parent?.name) return parent

        return doGetOrCreateAggregatedTestTask(name, description, parent?.doGetTask())
    }

    private fun doGetOrCreateAggregatedTestTask(
        name: String,
        description: String,
        parent: KotlinTestReport? = null,
        configure: (TaskHolder<KotlinTestReport>) -> Unit = {}
    ): TaskHolder<KotlinTestReport> {
        val existed = project.locateTask<KotlinTestReport>(name)
        if (existed != null) return existed

        val reportName = name

        val aggregate: TaskHolder<KotlinTestReport> = project.createOrRegisterTask(name) { aggregate ->
            aggregate.description = description
            aggregate.group = JavaBasePlugin.VERIFICATION_GROUP

            aggregate.destinationDir = project.testReportsDir.resolve(reportName)

            if (System.getProperty("idea.active") != null) {
                aggregate.extensions.extraProperties.set("idea.internal.test", true)
            }

            project.gradle.taskGraph.whenReady { graph ->
                aggregate.maybeOverrideReporting(graph)
            }

            parent?.addChild(aggregate)
        }

        parent?.dependsOn(aggregate.getTaskOrProvider())

        configure(aggregate)

        return aggregate
    }

    private fun cleanTaskName(taskName: String): String {
        check(taskName.isNotEmpty())
        return "clean" + taskName.capitalize()
    }

    private val Project.cleanAllTestTask: Task
        get() {
            val taskName = cleanTaskName(allTestsTask.name)
            return tasks.findByName(taskName)
                ?: tasks.create(taskName, Task::class.java).also {
                    tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME).dependsOn(it)
                }
        }

    companion object {
        const val PROJECT_EXTENSION_NAME = "kotlinTestRegistry"
    }
}

internal val Project.kotlinTestRegistry: KotlinTestsRegistry
    get() = extensions.getByName(KotlinTestsRegistry.PROJECT_EXTENSION_NAME) as KotlinTestsRegistry